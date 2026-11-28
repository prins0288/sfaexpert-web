package in.opt.sfa.tenant.mailbox;

import in.opt.sfa.exception.ForbiddenException;
import in.opt.sfa.tenant.team.TeamScope;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Part;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/** Reads from the cache: folders, the list with search / filter / paging, the mail view (the body comes from the server the first time it is opened), attachment download. Every query is bound to the signed-in employee's account. */
@Service
public class MailReadService {
    private static final Pattern LOCAL_IMG = Pattern.compile("^data:image/(png|jpeg|jpg|gif|webp);base64,[A-Za-z0-9+/=]+$");
    private static final Pattern REMOTE_IMG = Pattern.compile("^(https?://[^\\s\"'<>]+|data:image/(png|jpeg|jpg|gif|webp);base64,[A-Za-z0-9+/=]+)$");
    private static final Pattern HREF = Pattern.compile("(?i)^(https?://|mailto:)[^\\s\"'<>]*$");
    private static final Pattern STYLE = Pattern.compile("(?i)^[a-z\\-:;#%.,()\\s0-9\"']{0,400}$");

    private static PolicyFactory policy(boolean remoteImages) {
        return new HtmlPolicyBuilder().allowElements("p", "div", "span", "br", "b", "strong", "i", "em", "u", "s", "ul", "ol", "li", "blockquote", "pre", "code", "h1", "h2", "h3", "h4", "h5", "h6", "table", "thead", "tbody", "tfoot", "tr", "td", "th", "hr", "a", "img", "font", "center", "small", "sub", "sup")
                .allowAttributes("style").matching(STYLE).globally().allowAttributes("align", "valign", "width", "height", "colspan", "rowspan", "border", "cellpadding", "cellspacing", "bgcolor", "color", "size", "face").globally()
                .allowAttributes("href").matching(HREF).onElements("a").allowStandardUrlProtocols().allowUrlProtocols("mailto", "data").requireRelNofollowOnLinks().allowAttributes("src").matching(remoteImages ? REMOTE_IMG : LOCAL_IMG).onElements("img").allowAttributes("alt").onElements("img").toFactory();
    }
    static String sanitizeForView(String html, boolean remoteImages) { return (remoteImages ? WITH_REMOTE : NO_REMOTE).sanitize(html); }
    private static final PolicyFactory NO_REMOTE = policy(false), WITH_REMOTE = policy(true);
    private static final Pattern REMOTE_SRC = Pattern.compile("(?i)<img\\b[^>]*\\bsrc\\s*=\\s*[\"']?\\s*https?:");

    private final JdbcTemplate jdbc;
    private final MailAccountService accounts;
    private final MailBodyService bodies;

    public MailReadService(JdbcTemplate jdbc, MailAccountService accounts, MailBodyService bodies) { this.jdbc = jdbc; this.accounts = accounts; this.bodies = bodies; }

    private static String me() { String m = TeamScope.me(); if (m == null) throw new ForbiddenException("Sign in again"); return m; }
    private Long accountId() { accounts.requireExternal(); return accountIdRaw(); }
    private Long accountIdRaw() { return jdbc.queryForList("SELECT id FROM mail_account WHERE emp_id = ?", Long.class, me()).stream().findFirst().orElse(null); }
    private void seen(long accId) { jdbc.update("UPDATE mail_account SET last_seen_at = NOW() WHERE id = ?", accId); }

    public Map<String, Object> folders() {
        Map<String, Object> out = new LinkedHashMap<>(); out.put("mode", accounts.mode());
        if (!accounts.externalOn()) { out.put("account", false); out.put("folders", List.of()); return out; }
        Long acc = accountId();
        if (acc == null) { out.put("account", false); out.put("folders", List.of()); return out; }
        seen(acc);
        out.put("account", true);
        out.put("folders", jdbc.queryForList("SELECT id, name, role, unread_count AS unread, total_count AS total FROM mail_folder WHERE account_id = ? ORDER BY sort_order, name", acc));
        out.put("outbox", jdbc.queryForObject("SELECT COUNT(*) FROM mail_outbox WHERE account_id = ? AND status IN ('QUEUED','SENDING','FAILED','UNKNOWN')", Integer.class, acc));
        Map<String, Object> a = jdbc.queryForMap("SELECT email, sync_state AS syncState, last_sync_at AS lastSyncAt, last_sync_error AS lastSyncError, is_enabled AS enabled FROM mail_account WHERE id = ?", acc); out.put("info", a);
        return out;
    }

    /** The list of one folder (or the starred mails of the account when folderId is 0), newest first. */
    public Map<String, Object> list(Long folderId, int page, int size, String q, String from, String subject, String dateFrom, String dateTo, boolean unread, boolean attach, boolean starred) {
        Long acc = accountId(); if (acc == null) throw new IllegalStateException("Set up your mail account first (Mail Settings)"); seen(acc);
        size = Math.max(5, Math.min(size, 100)); page = Math.max(0, page);
        StringBuilder w = new StringBuilder(" WHERE m.account_id = ?"); List<Object> a = new ArrayList<>(); a.add(acc);
        if (folderId != null && folderId > 0) { if (jdbc.queryForObject("SELECT COUNT(*) FROM mail_folder WHERE id = ? AND account_id = ?", Integer.class, folderId, acc) == 0) throw new ForbiddenException("This folder is not yours"); w.append(" AND m.folder_id = ?"); a.add(folderId); }
        if (q != null && !q.isBlank()) { String like = "%" + esc(q.trim()) + "%"; w.append(" AND (m.subject LIKE ? OR m.from_addr LIKE ? OR m.from_name LIKE ? OR m.to_addrs LIKE ? OR m.snippet LIKE ?)"); for (int i = 0; i < 5; i++) a.add(like); }
        if (from != null && !from.isBlank()) { String like = "%" + esc(from.trim()) + "%"; w.append(" AND (m.from_addr LIKE ? OR m.from_name LIKE ?)"); a.add(like); a.add(like); }
        if (subject != null && !subject.isBlank()) { w.append(" AND m.subject LIKE ?"); a.add("%" + esc(subject.trim()) + "%"); }
        if (dateFrom != null && !dateFrom.isBlank()) { w.append(" AND m.sent_at >= ?"); a.add(java.sql.Date.valueOf(java.time.LocalDate.parse(dateFrom))); }
        if (dateTo != null && !dateTo.isBlank()) { w.append(" AND m.sent_at < ?"); a.add(java.sql.Date.valueOf(java.time.LocalDate.parse(dateTo).plusDays(1))); }
        if (unread) w.append(" AND m.is_read = 0"); if (attach) w.append(" AND m.has_attach = 1"); if (starred) w.append(" AND m.is_starred = 1");
        int total = jdbc.queryForObject("SELECT COUNT(*) FROM mail_message m" + w, Integer.class, a.toArray());
        List<Object> a2 = new ArrayList<>(a); a2.add(size); a2.add(page * size);
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT m.id, m.folder_id AS folderId, m.from_addr AS fromAddr, m.from_name AS fromName, m.to_addrs AS toAddrs, m.subject, m.sent_at AS sentAt, m.has_attach AS hasAttach, m.is_read AS isRead, m.is_starred AS isStarred, m.snippet FROM mail_message m" + w + " ORDER BY m.sent_at DESC, m.id DESC LIMIT ? OFFSET ?", a2.toArray());
        Map<String, Object> out = new LinkedHashMap<>(); out.put("rows", rows); out.put("total", total); out.put("page", page); out.put("size", size);
        if (folderId != null && folderId > 0) out.put("canLoadMore", jdbc.queryForObject("SELECT COUNT(*) FROM mail_folder WHERE id = ? AND lowest_uid > 1", Integer.class, folderId) > 0);
        return out;
    }
    private static String esc(String s) { return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_"); }

    /** One mail with its body (fetched from the server on the first open) and a safe HTML. Remote pictures stay blocked unless images = true. */
    public Map<String, Object> view(long id, boolean images) throws Exception {
        Long acc = accountId(); if (acc == null) throw new IllegalStateException("Set up your mail account first (Mail Settings)");
        Map<String, Object> m = jdbc.queryForList("SELECT m.*, f.role AS folderRole, f.name AS folderName FROM mail_message m JOIN mail_folder f ON f.id = m.folder_id WHERE m.id = ? AND m.account_id = ?", id, acc).stream().findFirst().orElseThrow(() -> new ForbiddenException("This mail is not yours"));
        if (!flag(m.get("body_fetched"))) { bodies.fetch(accounts.load(me()).orElseThrow(), id); m = jdbc.queryForMap("SELECT m.*, f.role AS folderRole, f.name AS folderName FROM mail_message m JOIN mail_folder f ON f.id = m.folder_id WHERE m.id = ?", id); }
        String html = (String) m.get("body_html"); boolean remote = html != null && REMOTE_SRC.matcher(html).find();
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("id", id); o.put("folderId", m.get("folder_id")); o.put("folderRole", m.get("folderRole")); o.put("subject", m.get("subject")); o.put("fromAddr", m.get("from_addr")); o.put("fromName", m.get("from_name")); o.put("to", m.get("to_addrs")); o.put("cc", m.get("cc_addrs")); o.put("bcc", m.get("bcc_addrs"));
        o.put("sentAt", m.get("sent_at")); o.put("isRead", m.get("is_read")); o.put("isStarred", m.get("is_starred")); o.put("messageId", m.get("message_id"));
        o.put("html", html == null ? null : sanitizeForView(html, images)); o.put("text", m.get("body_text")); o.put("remoteImagesBlocked", remote && !images);
        o.put("attachments", jdbc.queryForList("SELECT part_no AS part, filename AS name, content_type AS type, size_bytes AS size, is_inline AS inline FROM mail_attachment WHERE message_id = ? AND is_inline = 0 ORDER BY part_no", id));
        return o;
    }
    private static boolean flag(Object o) { return Boolean.TRUE.equals(o) || (o instanceof Number n && n.intValue() == 1); }

    public record Download(String name, String type, byte[] data) { }

    public Download attachment(long id, int part) throws Exception {
        Long acc = accountId(); if (acc == null) throw new IllegalStateException("Set up your mail account first (Mail Settings)");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM mail_attachment a JOIN mail_message m ON m.id = a.message_id WHERE a.message_id = ? AND a.part_no = ? AND m.account_id = ?", Integer.class, id, part, acc) == 0) throw new ForbiddenException("This attachment is not yours");
        ImapAttachment.Loaded l = ImapAttachment.load(accounts, accounts.load(me()).orElseThrow(), id, part, accounts.maxAttachmentMb() * 1024L * 1024L * 2, jdbc);
        return new Download(l.name(), l.type(), l.data());
    }
}
