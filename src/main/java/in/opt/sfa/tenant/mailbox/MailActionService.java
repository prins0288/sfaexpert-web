package in.opt.sfa.tenant.mailbox;

import in.opt.sfa.exception.ForbiddenException;
import in.opt.sfa.tenant.team.TeamScope;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.eclipse.angus.mail.imap.IMAPFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * What the user does with mails: read / unread, star, move, archive, spam, delete, restore, permanent delete, drafts. The server is changed FIRST - if it refuses nothing changes in the cache.
 * Reply / Reply all / Forward are prepared here (recipients, "Re:" / "Fwd:", quoted text, attachments of a forward) so the screen only has to show them.
 */
@Service
public class MailActionService {
    private static final Logger log = LoggerFactory.getLogger(MailActionService.class);
    private static final int MAX_IDS = 200;

    private final JdbcTemplate jdbc;
    private final MailAccountService accounts;

    public MailActionService(JdbcTemplate jdbc, MailAccountService accounts) { this.jdbc = jdbc; this.accounts = accounts; }

    private static String me() { String m = TeamScope.me(); if (m == null) throw new ForbiddenException("Sign in again"); return m; }
    private MailAccountService.Account account() { return accounts.load(me()).orElseThrow(() -> new IllegalStateException("Set up your mail account first (Mail Settings)")); }

    private record Row(long id, long folderId, String path, String role, long uid) { }

    private List<Row> own(MailAccountService.Account acc, List<?> ids) {
        if (ids == null || ids.isEmpty()) throw new IllegalArgumentException("Choose at least one mail");
        if (ids.size() > MAX_IDS) throw new IllegalArgumentException("Too many mails at once (limit " + MAX_IDS + ")");
        List<Row> out = new ArrayList<>();
        for (Object o : ids) {
            long id = Long.parseLong(String.valueOf(o));
            Map<String, Object> r = jdbc.queryForList("SELECT m.id, m.folder_id, m.uid, f.path, f.role FROM mail_message m JOIN mail_folder f ON f.id = m.folder_id WHERE m.id = ? AND m.account_id = ?", id, acc.id()).stream().findFirst().orElseThrow(() -> new ForbiddenException("This mail is not yours"));
            out.add(new Row(id, ((Number) r.get("folder_id")).longValue(), (String) r.get("path"), (String) r.get("role"), ((Number) r.get("uid")).longValue()));
        }
        return out;
    }

    /** The folder a special action goes to: the server's folder of that role; for Archive / Spam it is created when the server has none. */
    private Map<String, Object> roleFolder(Store st, MailAccountService.Account acc, String role) throws Exception {
        List<Map<String, Object>> l = jdbc.queryForList("SELECT * FROM mail_folder WHERE account_id = ? AND role = ? ORDER BY id LIMIT 1", acc.id(), role);
        if (!l.isEmpty()) return l.get(0);
        if (!List.of("ARCHIVE", "SPAM").contains(role)) throw new IllegalStateException("This mail server has no " + role.toLowerCase() + " folder");
        String name = "ARCHIVE".equals(role) ? "Archive" : "Junk"; Folder f = st.getFolder(name); if (!f.exists()) f.create(Folder.HOLDS_MESSAGES);
        jdbc.update("INSERT INTO mail_folder (account_id, name, path, role, sort_order) VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE role = VALUES(role)", acc.id(), name, name, role, ImapFolders.order(role));
        return jdbc.queryForMap("SELECT * FROM mail_folder WHERE account_id = ? AND path = ?", acc.id(), name);
    }

    /** body: ids, action = READ | UNREAD | STAR | UNSTAR | MOVE (targetFolderId) | ARCHIVE | SPAM | NOT_SPAM | DELETE | RESTORE | PURGE */
    public Map<String, Object> act(Map<String, Object> b) throws Exception {
        MailAccountService.Account acc = account(); String action = String.valueOf(b.get("action")).toUpperCase(); List<Row> rows = own(acc, (List<?>) b.get("ids"));
        try (Store st = ImapConnect.open(accounts, acc)) {
            switch (action) {
                case "READ", "UNREAD", "STAR", "UNSTAR" -> flags(st, rows, action);
                case "MOVE" -> { Map<String, Object> t = jdbc.queryForList("SELECT * FROM mail_folder WHERE id = ? AND account_id = ?", Long.parseLong(String.valueOf(b.get("targetFolderId"))), acc.id()).stream().findFirst().orElseThrow(() -> new ForbiddenException("This folder is not yours")); move(st, acc, rows, t); }
                case "ARCHIVE" -> move(st, acc, rows, roleFolder(st, acc, "ARCHIVE"));
                case "SPAM" -> move(st, acc, rows, roleFolder(st, acc, "SPAM"));
                case "NOT_SPAM", "RESTORE" -> move(st, acc, rows, roleFolder(st, acc, "INBOX"));
                case "DELETE" -> { if (rows.stream().anyMatch(r -> "TRASH".equals(r.role()))) throw new IllegalArgumentException("These mails are already in Trash - use permanent delete"); move(st, acc, rows, roleFolder(st, acc, "TRASH")); }
                case "PURGE" -> purge(st, rows);
                default -> throw new IllegalArgumentException("Unknown action");
            }
        }
        for (long fid : rows.stream().map(Row::folderId).distinct().toList()) recount(fid);
        return Map.of("done", rows.size());
    }

    private void flags(Store st, List<Row> rows, String action) throws Exception {
        Flags.Flag fl = action.endsWith("READ") ? Flags.Flag.SEEN : Flags.Flag.FLAGGED; boolean on = "READ".equals(action) || "STAR".equals(action);
        for (Map.Entry<String, List<Row>> e : byFolder(rows).entrySet()) {
            Folder f = st.getFolder(e.getKey()); f.open(Folder.READ_WRITE);
            try { Message[] ms = ((UIDFolder) f).getMessagesByUID(e.getValue().stream().mapToLong(Row::uid).toArray()); List<Message> ok = new ArrayList<>(); for (Message m : ms) if (m != null) ok.add(m); if (!ok.isEmpty()) f.setFlags(ok.toArray(new Message[0]), new Flags(fl), on); }
            finally { try { f.close(false); } catch (Exception ignore) { /* closing */ } }
        }
        for (Row r : rows) jdbc.update(fl == Flags.Flag.SEEN ? "UPDATE mail_message SET is_read = ? WHERE id = ?" : "UPDATE mail_message SET is_starred = ? WHERE id = ?", on ? 1 : 0, r.id());
    }

    private void move(Store st, MailAccountService.Account acc, List<Row> rows, Map<String, Object> target) throws Exception {
        long targetId = ((Number) target.get("id")).longValue(); String targetPath = (String) target.get("path");
        Folder dest = st.getFolder(targetPath);
        for (Map.Entry<String, List<Row>> e : byFolder(rows).entrySet()) {
            if (e.getKey().equals(targetPath)) continue;
            IMAPFolder src = (IMAPFolder) st.getFolder(e.getKey()); src.open(Folder.READ_WRITE);
            try {
                Map<Long, Row> byUid = new HashMap<>(); for (Row r : e.getValue()) byUid.put(r.uid(), r);
                Message[] ms = src.getMessagesByUID(byUid.keySet().stream().mapToLong(Long::longValue).toArray()); List<Message> ok = new ArrayList<>(); for (Message m : ms) if (m != null) ok.add(m);
                if (ok.isEmpty()) { for (Row r : e.getValue()) jdbc.update("DELETE FROM mail_message WHERE id = ?", r.id()); continue; }
                Message[] arr = ok.toArray(new Message[0]);
                org.eclipse.angus.mail.imap.AppendUID[] au = src.copyUIDMessages(arr, dest);   // UIDPLUS gives the new UIDs; null when the server has no UIDPLUS
                src.setFlags(arr, new Flags(Flags.Flag.DELETED), true); try { src.expunge(arr); } catch (Exception ex) { src.expunge(); }
                for (int i = 0; i < ok.size(); i++) {
                    Row r = byUid.get(src.getUID(ok.get(i)));
                    if (au != null && au[i] != null && r != null) jdbc.update("INSERT IGNORE INTO mail_message (account_id, folder_id, uid, message_id, in_reply_to, refs, from_addr, from_name, to_addrs, cc_addrs, bcc_addrs, subject, sent_at, size_bytes, has_attach, is_read, is_starred, is_draft, snippet, body_text, body_html, body_fetched) SELECT account_id, ?, ?, message_id, in_reply_to, refs, from_addr, from_name, to_addrs, cc_addrs, bcc_addrs, subject, sent_at, size_bytes, has_attach, is_read, is_starred, is_draft, snippet, body_text, body_html, body_fetched FROM mail_message WHERE id = ?", targetId, au[i].uid, r.id());
                    if (r != null) jdbc.update("DELETE FROM mail_message WHERE id = ?", r.id());   // the copy in the new folder is re-read by the next sync when the server gave no UID
                }
            } finally { try { src.close(false); } catch (Exception ignore) { /* closing */ } }
        }
        recount(targetId);
    }

    private void purge(Store st, List<Row> rows) throws Exception {
        if (rows.stream().anyMatch(r -> !List.of("TRASH", "DRAFTS", "SPAM").contains(r.role()))) throw new IllegalArgumentException("Permanent delete works only on Trash, Spam and Drafts - delete the mail first");
        for (Map.Entry<String, List<Row>> e : byFolder(rows).entrySet()) {
            Folder f = st.getFolder(e.getKey()); f.open(Folder.READ_WRITE);
            try { Message[] ms = ((UIDFolder) f).getMessagesByUID(e.getValue().stream().mapToLong(Row::uid).toArray()); List<Message> ok = new ArrayList<>(); for (Message m : ms) if (m != null) ok.add(m);
                if (!ok.isEmpty()) { Message[] arr = ok.toArray(new Message[0]); f.setFlags(arr, new Flags(Flags.Flag.DELETED), true); try { ((IMAPFolder) f).expunge(arr); } catch (Exception ex) { f.expunge(); } } }
            finally { try { f.close(false); } catch (Exception ignore) { /* closing */ } }
        }
        for (Row r : rows) jdbc.update("DELETE FROM mail_message WHERE id = ?", r.id());
    }

    private static Map<String, List<Row>> byFolder(List<Row> rows) { Map<String, List<Row>> m = new LinkedHashMap<>(); for (Row r : rows) m.computeIfAbsent(r.path(), k -> new ArrayList<>()).add(r); return m; }
    private void recount(long folderId) { jdbc.update("UPDATE mail_folder SET unread_count = (SELECT COUNT(*) FROM mail_message WHERE folder_id = ? AND is_read = 0), total_count = (SELECT COUNT(*) FROM mail_message WHERE folder_id = ?) WHERE id = ?", folderId, folderId, folderId); }

    // ------------------------------------------------------------------ drafts
    /** Save (or replace) a draft on the server's Drafts folder. body: draftId (the local id of the previous save, replaced), to, cc, bcc, subject, html, replyTo. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> saveDraft(Map<String, Object> b) throws Exception {
        MailAccountService.Account acc = account();
        List<String> to = lenient(b.get("to")), cc = lenient(b.get("cc")), bcc = lenient(b.get("bcc"));
        String subject = b.get("subject") == null ? "" : b.get("subject").toString().replaceAll("[\\r\\n]+", " ").trim(); String html = MailSendService.COMPOSE_POLICY.sanitize(b.get("html") == null ? "" : b.get("html").toString());
        MimeBuilder.Out o = new MimeBuilder.Out(acc.email(), acc.displayName(), to, cc, bcc, subject, html, MimeBuilder.htmlToText(html), MimeBuilder.newMessageId(acc.email()), null, null, List.of());
        MimeMessage msg = MimeBuilder.build(Session.getInstance(new Properties()), o);
        msg.setFlag(Flags.Flag.DRAFT, true); msg.setFlag(Flags.Flag.SEEN, true);
        long newId = 0;
        try (Store st = ImapConnect.open(accounts, acc)) {
            Map<String, Object> df = roleFolderOrNull(acc, "DRAFTS"); if (df == null) throw new IllegalStateException("This mail server has no Drafts folder");
            IMAPFolder f = (IMAPFolder) st.getFolder((String) df.get("path")); f.open(Folder.READ_WRITE);
            try {
                org.eclipse.angus.mail.imap.AppendUID[] au = f.appendUIDMessages(new Message[]{msg});
                long fid = ((Number) df.get("id")).longValue();
                if (au != null && au.length > 0 && au[0] != null) {
                    jdbc.update("INSERT IGNORE INTO mail_message (account_id, folder_id, uid, message_id, from_addr, from_name, to_addrs, cc_addrs, bcc_addrs, subject, sent_at, is_read, is_draft, snippet, body_text, body_html, body_fetched) VALUES (?,?,?,?,?,?,?,?,?,?,NOW(),1,1,?,?,?,1)",
                            acc.id(), fid, au[0].uid, o.messageId(), acc.email(), acc.displayName(), String.join(", ", to), cc.isEmpty() ? null : String.join(", ", cc), bcc.isEmpty() ? null : String.join(", ", bcc), subject, abbreviate(o.text()), o.text(), html);
                    newId = jdbc.queryForObject("SELECT id FROM mail_message WHERE folder_id = ? AND uid = ?", Long.class, fid, au[0].uid);
                }
                if (b.get("draftId") != null && !String.valueOf(b.get("draftId")).isBlank()) {   // the earlier save of the same draft goes away
                    long old = Long.parseLong(String.valueOf(b.get("draftId")));
                    List<Row> prev = own(acc, List.of(old)); if (prev.get(0).role().equals("DRAFTS")) purge(st, prev);
                }
                recount(fid);
            } finally { try { f.close(false); } catch (Exception ignore) { /* closing */ } }
        }
        return Map.of("draftId", newId, "saved", true);
    }
    private static String abbreviate(String t) { String s = t == null ? "" : t.replaceAll("\\s+", " ").trim(); return s.length() > 200 ? s.substring(0, 200) : s; }
    private Map<String, Object> roleFolderOrNull(MailAccountService.Account acc, String role) { return jdbc.queryForList("SELECT * FROM mail_folder WHERE account_id = ? AND role = ? ORDER BY id LIMIT 1", acc.id(), role).stream().findFirst().orElse(null); }
    /** A draft may hold half-written addresses: they are kept as typed (only line breaks are refused). */
    private static List<String> lenient(Object o) {
        List<String> out = new ArrayList<>();
        if (o instanceof List<?> l) for (Object x : l) add(out, String.valueOf(x)); else if (o != null) add(out, o.toString());
        return out;
    }
    private static void add(List<String> out, String s) { for (String p : s.split("[;,\\n]")) { String t = p.trim(); if (!t.isEmpty() && t.indexOf('\r') < 0 && t.contains("@")) out.add(t); } }

    // ------------------------------------------------------------------ reply / forward
    /** mode = REPLY | REPLYALL | FORWARD: the recipients, subject, quoted body and (forward) the attachments to put on the compose screen. */
    public Map<String, Object> prepare(long id, String mode) throws Exception {
        MailAccountService.Account acc = account(); Map<String, Object> m = jdbc.queryForList("SELECT * FROM mail_message WHERE id = ? AND account_id = ?", id, acc.id()).stream().findFirst().orElseThrow(() -> new ForbiddenException("This mail is not yours"));
        if (!"1".equals(String.valueOf(m.get("body_fetched"))) && !Boolean.TRUE.equals(m.get("body_fetched"))) { new MailBodyService(jdbc, accounts).fetch(acc, id); m = jdbc.queryForMap("SELECT * FROM mail_message WHERE id = ?", id); }
        String subject = String.valueOf(m.get("subject") == null ? "" : m.get("subject")); boolean fwd = "FORWARD".equalsIgnoreCase(mode);
        String prefix = fwd ? "Fwd: " : "Re: "; String subj = subject.regionMatches(true, 0, prefix, 0, prefix.length()) ? subject : prefix + subject;
        String from = m.get("from_addr") == null ? "" : (m.get("from_name") == null || ((String) m.get("from_name")).isBlank() ? (String) m.get("from_addr") : "\"" + String.valueOf(m.get("from_name")).replace("\"", "") + "\" <" + m.get("from_addr") + ">");
        List<String> to = new ArrayList<>(), cc = new ArrayList<>();
        if (!fwd) {
            to.add(from);
            if ("REPLYALL".equalsIgnoreCase(mode)) {
                Set<String> seen = new HashSet<>(); seen.add(acc.email().toLowerCase()); if (m.get("from_addr") != null) seen.add(((String) m.get("from_addr")).toLowerCase());
                for (String col : List.of("to_addrs", "cc_addrs")) for (String a : split((String) m.get(col))) { String bare = bare(a); if (bare != null && seen.add(bare.toLowerCase())) cc.add(a); }
            }
        }
        String when = m.get("sent_at") == null ? "" : String.valueOf(m.get("sent_at")).replace('T', ' ');
        String head = fwd ? "<br><br>---------- Forwarded message ----------<br>From: " + esc(from) + "<br>Date: " + esc(when) + "<br>Subject: " + esc(subject) + "<br>To: " + esc(String.valueOf(m.get("to_addrs") == null ? "" : m.get("to_addrs"))) + "<br><br>"
                : "<br><br>On " + esc(when) + ", " + esc(from) + " wrote:<br>";
        String inner = m.get("body_html") != null ? MailSendService.COMPOSE_POLICY.sanitize(String.valueOf(m.get("body_html"))) : esc(String.valueOf(m.get("body_text") == null ? "" : m.get("body_text"))).replace("\n", "<br>");
        Map<String, Object> out = new LinkedHashMap<>(); out.put("to", to); out.put("cc", cc); out.put("subject", subj); out.put("html", head + (fwd ? inner : "<blockquote>" + inner + "</blockquote>")); out.put("replyTo", fwd ? null : id);
        out.put("attachments", fwd ? jdbc.queryForList("SELECT message_id AS fwd, part_no AS part, filename AS name, size_bytes AS size FROM mail_attachment WHERE message_id = ? AND is_inline = 0 ORDER BY part_no", id) : List.of());
        return out;
    }
    private static List<String> split(String s) { return s == null || s.isBlank() ? List.of() : Arrays.stream(s.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")).map(String::trim).filter(x -> !x.isEmpty()).toList(); }
    private static String bare(String a) { try { return new InternetAddress(a).getAddress(); } catch (Exception e) { return null; } }
    private static String esc(String s) { return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }

    // ------------------------------------------------------------------ contacts
    /** Address suggestions: people I mailed or got mail from, and the employees of the company (official email). */
    public List<Map<String, Object>> contacts(String q) {
        Long acc = jdbc.queryForList("SELECT id FROM mail_account WHERE emp_id = ?", Long.class, me()).stream().findFirst().orElse(null);
        String like = "%" + (q == null ? "" : q.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")) + "%"; if (q == null || q.trim().length() < 2) return List.of();
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        if (acc != null) {
            for (Map<String, Object> r : jdbc.queryForList("SELECT from_addr AS email, MAX(from_name) AS name, COUNT(*) AS n FROM mail_message WHERE account_id = ? AND from_addr IS NOT NULL AND (from_addr LIKE ? OR from_name LIKE ?) GROUP BY from_addr ORDER BY n DESC LIMIT 8", acc, like, like)) out.putIfAbsent(String.valueOf(r.get("email")).toLowerCase(), Map.of("email", r.get("email"), "name", r.get("name") == null ? "" : r.get("name"), "source", "mail"));
        }
        for (Map<String, Object> r : jdbc.queryForList("SELECT official_email AS email, emp_name AS name FROM emp_detail WHERE is_active = 1 AND official_email IS NOT NULL AND official_email <> '' AND (official_email LIKE ? OR emp_name LIKE ?) ORDER BY emp_name LIMIT 8", like, like)) out.putIfAbsent(String.valueOf(r.get("email")).toLowerCase(), Map.of("email", r.get("email"), "name", r.get("name") == null ? "" : r.get("name"), "source", "company"));
        return new ArrayList<>(out.values()).subList(0, Math.min(10, out.size()));
    }
}
