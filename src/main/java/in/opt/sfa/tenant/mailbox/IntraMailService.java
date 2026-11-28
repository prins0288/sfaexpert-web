package in.opt.sfa.tenant.mailbox;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.opt.sfa.exception.ForbiddenException;
import in.opt.sfa.tenant.team.TeamScope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Intramail: messages between the users of the application, kept in the database. A message goes to users, to groups, or to all employees; every receiver has his own read / star / delete state.
 * Sending to ALL employees or to a company group is for the admin and the managers (circulars); everyone can write to users and to his own groups.
 */
@Service
public class IntraMailService {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int MAX_RECIPIENTS = 5000;

    private final JdbcTemplate jdbc;
    private final MailAttachmentStore files;
    private final MailAccountService accounts;
    private final RateLimiter limiter;

    public IntraMailService(JdbcTemplate jdbc, MailAttachmentStore files, MailAccountService accounts, RateLimiter limiter) { this.jdbc = jdbc; this.files = files; this.accounts = accounts; this.limiter = limiter; }

    private static String me() { String m = TeamScope.me(); if (m == null) throw new ForbiddenException("Sign in again"); return m; }
    private static boolean canBroadcast() { return TeamScope.isAdmin() || TeamScope.isManager(); }
    private void tables() { if (!accounts.intraOn()) throw new IllegalStateException("Your company uses external mail only - Intramail is switched off by the administrator");  try { jdbc.queryForObject("SELECT COUNT(*) FROM intra_message WHERE 1 = 0", Integer.class); } catch (org.springframework.dao.DataAccessException e) { throw new IllegalStateException("The intramail tables are missing - run migration 136_intramail.sql"); } }

    // ------------------------------------------------------------------ people and groups
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> people(String q) {
        tables(); List<Map<String, Object>> out = new ArrayList<>(); String t = q == null ? "" : q.trim();
        if (canBroadcast() && (t.isEmpty() || "all employees".contains(t.toLowerCase()) || "everyone".contains(t.toLowerCase()))) out.add(Map.of("type", "ALL", "id", "ALL", "label", "All employees"));
        String like = "%" + t.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        for (Map<String, Object> g : jdbc.queryForList("SELECT id, name, owner_emp_id FROM intra_group WHERE (owner_emp_id IS NULL OR owner_emp_id = ?) AND name LIKE ? ORDER BY name LIMIT 8", me(), like)) {
            if (g.get("owner_emp_id") == null && !canBroadcast()) continue;
            out.add(Map.of("type", "GROUP", "id", g.get("id"), "label", g.get("name") + (g.get("owner_emp_id") == null ? " (company group)" : " (my group)")));
        }
        if (t.length() >= 1) for (Map<String, Object> e : jdbc.queryForList("SELECT emp_id, emp_code, emp_name FROM emp_detail WHERE is_active = 1 AND COALESCE(is_vacant, 0) = 0 AND (emp_name LIKE ? OR emp_code LIKE ?) ORDER BY emp_name LIMIT 15", like, like))
            out.add(Map.of("type", "USER", "id", e.get("emp_id"), "label", e.get("emp_name") + " (" + e.get("emp_code") + ")"));
        return out;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> groups() {
        tables();
        List<Map<String, Object>> gs = jdbc.queryForList("SELECT id, name, owner_emp_id IS NULL AS company FROM intra_group WHERE owner_emp_id IS NULL OR owner_emp_id = ? ORDER BY company DESC, name", me());
        for (Map<String, Object> g : gs) g.put("members", jdbc.queryForList("SELECT e.emp_id AS empId, e.emp_name AS name, e.emp_code AS code FROM intra_group_member m JOIN emp_detail e ON e.emp_id = m.emp_id WHERE m.group_id = ? ORDER BY e.emp_name", g.get("id")));
        return gs;
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    @SuppressWarnings("unchecked")
    public Map<String, Object> saveGroup(Map<String, Object> b) {
        tables(); boolean company = Boolean.TRUE.equals(b.get("company")) || "true".equalsIgnoreCase(String.valueOf(b.get("company")));
        if (company && !TeamScope.isAdmin()) throw new ForbiddenException("Only the admin makes company groups");
        String name = b.get("name") == null ? "" : b.get("name").toString().trim(); if (name.isEmpty() || name.length() > 100) throw new IllegalArgumentException("Give the group a name (up to 100 characters)");
        List<String> emps = new ArrayList<>(); if (b.get("empIds") instanceof List<?> l) for (Object o : l) emps.add(o.toString());
        Long id = b.get("id") == null || b.get("id").toString().isBlank() ? null : Long.valueOf(b.get("id").toString());
        String owner = company ? null : me();
        if (id == null) { try { jdbc.update("INSERT INTO intra_group (name, owner_emp_id) VALUES (?,?)", name, owner); } catch (org.springframework.dao.DuplicateKeyException e) { throw new IllegalArgumentException("A group with this name already exists"); } id = jdbc.queryForObject("SELECT id FROM intra_group WHERE name = ? AND " + (owner == null ? "owner_emp_id IS NULL" : "owner_emp_id = '" + owner.replace("'", "") + "'"), Long.class, name); }
        else {
            Map<String, Object> g = jdbc.queryForList("SELECT owner_emp_id FROM intra_group WHERE id = ?", id).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Group not found"));
            if (g.get("owner_emp_id") == null ? !TeamScope.isAdmin() : !me().equals(g.get("owner_emp_id"))) throw new ForbiddenException("This group is not yours");
            jdbc.update("UPDATE intra_group SET name = ? WHERE id = ?", name, id);
        }
        jdbc.update("DELETE FROM intra_group_member WHERE group_id = ?", id);
        for (String e : new LinkedHashSet<>(emps)) jdbc.update("INSERT IGNORE INTO intra_group_member (group_id, emp_id) SELECT ?, emp_id FROM emp_detail WHERE emp_id = ? AND is_active = 1", id, e);
        return Map.of("id", id);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void deleteGroup(long id) {
        Map<String, Object> g = jdbc.queryForList("SELECT owner_emp_id FROM intra_group WHERE id = ?", id).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Group not found"));
        if (g.get("owner_emp_id") == null ? !TeamScope.isAdmin() : !me().equals(g.get("owner_emp_id"))) throw new ForbiddenException("This group is not yours");
        jdbc.update("DELETE FROM intra_group WHERE id = ?", id);
    }

    // ------------------------------------------------------------------ send
    private static List<String> strs(Object o) { List<String> l = new ArrayList<>(); if (o instanceof List<?> x) for (Object v : x) l.add(String.valueOf(v)); return l; }

    /** body: to / cc / bcc = [{type USER|GROUP|ALL, id}], subject, html, attachments [{path,name,size,type} | {fwdAtt}], parentId, draftId. */
    @Transactional(transactionManager = "tenantTransactionManager")
    @SuppressWarnings("unchecked")
    public Map<String, Object> send(Map<String, Object> b) throws Exception {
        tables(); String me = me(); limiter.check("intrasend:" + me, 30, 10 * 60_000, "You are sending too fast - wait a few minutes");
        Map<String, String> kindOf = new LinkedHashMap<>(); Map<String, String> viaOf = new HashMap<>(); List<String> labels = new ArrayList<>(); boolean all = false;
        for (String kind : List.of("to", "cc", "bcc")) {
            if (!(b.get(kind) instanceof List<?> l)) continue;
            for (Object o : l) {
                Map<String, Object> r = (Map<String, Object>) o; String type = String.valueOf(r.get("type")).toUpperCase(), id = String.valueOf(r.get("id")); String k = kind.toUpperCase();
                List<String> emps;
                switch (type) {
                    case "ALL" -> { if (!canBroadcast()) throw new ForbiddenException("Only the admin and the managers can write to all employees"); emps = jdbc.queryForList("SELECT emp_id FROM emp_detail WHERE is_active = 1 AND COALESCE(is_vacant, 0) = 0", String.class); all = true; if ("TO".equals(k)) labels.add("All employees"); }
                    case "GROUP" -> { Map<String, Object> g = jdbc.queryForList("SELECT name, owner_emp_id FROM intra_group WHERE id = ?", Long.parseLong(id)).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Group not found")); if (g.get("owner_emp_id") == null ? !canBroadcast() : !me.equals(g.get("owner_emp_id"))) throw new ForbiddenException("You cannot write to this group");
                        emps = jdbc.queryForList("SELECT m.emp_id FROM intra_group_member m JOIN emp_detail e ON e.emp_id = m.emp_id WHERE m.group_id = ? AND e.is_active = 1", String.class, Long.parseLong(id)); if ("TO".equals(k)) labels.add(String.valueOf(g.get("name"))); }
                    case "USER" -> { Map<String, Object> e = jdbc.queryForList("SELECT emp_id, emp_name FROM emp_detail WHERE emp_id = ? AND is_active = 1", id).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Employee not found")); emps = List.of(id); if ("TO".equals(k)) labels.add(String.valueOf(e.get("emp_name"))); }
                    default -> throw new IllegalArgumentException("Unknown recipient type");
                }
                for (String e : emps) { String old = kindOf.get(e); if (old == null || rank(k) < rank(old)) { kindOf.put(e, k); viaOf.put(e, type); } }
            }
        }
        if (kindOf.isEmpty()) throw new IllegalArgumentException("Add at least one recipient");
        if (kindOf.size() > MAX_RECIPIENTS) throw new IllegalArgumentException("Too many recipients");
        String subject = b.get("subject") == null ? "" : b.get("subject").toString().trim(); if (subject.length() > 400) throw new IllegalArgumentException("The subject is too long");
        String html = MailSendService.COMPOSE_POLICY.sanitize(b.get("html") == null ? "" : b.get("html").toString()); if (html.length() > 5_000_000) throw new IllegalArgumentException("The message is too big");
        if (subject.isEmpty() && MimeBuilder.htmlToText(html).isEmpty()) throw new IllegalArgumentException("Write a subject or a message");
        List<Map<String, Object>> atts = checkAttachments(me, b.get("attachments"));
        String label = String.join(", ", labels.size() > 4 ? labels.subList(0, 4) : labels) + (labels.size() > 4 ? " +" + (labels.size() - 4) : "");
        Long parent = b.get("parentId") == null || b.get("parentId").toString().isBlank() ? null : Long.valueOf(b.get("parentId").toString());
        Long draft = b.get("draftId") == null || b.get("draftId").toString().isBlank() ? null : Long.valueOf(b.get("draftId").toString());
        long id;
        if (draft != null && jdbc.queryForObject("SELECT COUNT(*) FROM intra_message WHERE id = ? AND sender_emp_id = ? AND is_draft = 1", Integer.class, draft, me) == 1) {
            id = draft; jdbc.update("UPDATE intra_message SET subject = ?, body_html = ?, body_text = ?, parent_id = ?, is_draft = 0, draft_json = NULL, has_attach = ?, to_label = ?, sent_at = NOW() WHERE id = ?", subject, html, MimeBuilder.htmlToText(html), parent, atts.isEmpty() ? 0 : 1, label, id);
            jdbc.update("DELETE FROM intra_attachment WHERE message_id = ?", id);
        } else {
            jdbc.update("INSERT INTO intra_message (sender_emp_id, subject, body_html, body_text, parent_id, has_attach, to_label, sent_at) VALUES (?,?,?,?,?,?,?,NOW())", me, subject, html, MimeBuilder.htmlToText(html), parent, atts.isEmpty() ? 0 : 1, label);
            id = jdbc.queryForObject("SELECT MAX(id) FROM intra_message WHERE sender_emp_id = ?", Long.class, me);
        }
        for (Map<String, Object> a : atts) jdbc.update("INSERT INTO intra_attachment (message_id, filename, content_type, size_bytes, path) VALUES (?,?,?,?,?)", id, a.get("name"), a.get("type"), a.get("size"), a.get("path"));
        List<Object[]> rows = new ArrayList<>(); for (Map.Entry<String, String> e : kindOf.entrySet()) rows.add(new Object[]{id, e.getKey(), e.getValue(), viaOf.get(e.getKey())});
        jdbc.batchUpdate("INSERT IGNORE INTO intra_recipient (message_id, emp_id, kind, via) VALUES (?,?,?,?)", rows);
        return Map.of("id", id, "recipients", kindOf.size());
    }
    private static int rank(String k) { return "TO".equals(k) ? 0 : "CC".equals(k) ? 1 : 2; }

    /** An uploaded file must be mine; a forwarded one must belong to a message I may see. */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> checkAttachments(String me, Object raw) {
        List<Map<String, Object>> out = new ArrayList<>(); long total = 0, max = accounts.maxAttachmentMb() * 1024L * 1024L;
        if (raw instanceof List<?> l) for (Object o : l) {
            Map<String, Object> a = (Map<String, Object>) o; Map<String, Object> k = new LinkedHashMap<>();
            if (a.get("fwdAtt") != null) {
                Map<String, Object> r = jdbc.queryForList("SELECT a.* FROM intra_attachment a WHERE a.id = ?", Long.parseLong(String.valueOf(a.get("fwdAtt")))).stream().findFirst().orElseThrow(() -> new ForbiddenException("This attachment is not yours"));
                canSee(((Number) r.get("message_id")).longValue(), me); k.put("path", r.get("path")); k.put("name", r.get("filename")); k.put("type", r.get("content_type")); k.put("size", r.get("size_bytes"));
            } else {
                String path = String.valueOf(a.get("path")); if (!files.owns(me, path)) throw new ForbiddenException("This attachment is not yours");
                k.put("path", path); k.put("name", MailAttachmentStore.safeName(String.valueOf(a.get("name")))); k.put("type", a.get("type")); k.put("size", a.get("size") == null ? 0 : Long.parseLong(String.valueOf(a.get("size"))));
            }
            total += ((Number) k.get("size")).longValue(); out.add(k);
        }
        if (total > max) throw new IllegalArgumentException("The attachments are bigger than the limit of " + accounts.maxAttachmentMb() + " MB");
        return out;
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveDraft(Map<String, Object> b) throws Exception {
        tables(); String me = me(); String html = MailSendService.COMPOSE_POLICY.sanitize(b.get("html") == null ? "" : b.get("html").toString());
        String subject = b.get("subject") == null ? "" : b.get("subject").toString().trim(); if (subject.length() > 400) subject = subject.substring(0, 400);
        String dj = JSON.writeValueAsString(Map.of("to", b.getOrDefault("to", List.of()), "cc", b.getOrDefault("cc", List.of()), "bcc", b.getOrDefault("bcc", List.of())));
        Long id = b.get("draftId") == null || b.get("draftId").toString().isBlank() ? null : Long.valueOf(b.get("draftId").toString());
        if (id != null && jdbc.queryForObject("SELECT COUNT(*) FROM intra_message WHERE id = ? AND sender_emp_id = ? AND is_draft = 1", Integer.class, id, me) == 1) jdbc.update("UPDATE intra_message SET subject = ?, body_html = ?, body_text = ?, draft_json = ? WHERE id = ?", subject, html, MimeBuilder.htmlToText(html), dj, id);
        else { jdbc.update("INSERT INTO intra_message (sender_emp_id, subject, body_html, body_text, is_draft, draft_json) VALUES (?,?,?,?,1,?)", me, subject, html, MimeBuilder.htmlToText(html), dj); id = jdbc.queryForObject("SELECT MAX(id) FROM intra_message WHERE sender_emp_id = ? AND is_draft = 1", Long.class, me); }
        return Map.of("draftId", id, "saved", true);
    }

    // ------------------------------------------------------------------ read
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> folders() {
        String me = me(); Map<String, Object> o = new LinkedHashMap<>();
        if (!accounts.intraOn()) { o.put("inbox", 0); o.put("drafts", 0); o.put("ready", false); o.put("off", true); return o; }
        try {
            o.put("inbox", jdbc.queryForObject("SELECT COUNT(*) FROM intra_recipient r JOIN intra_message m ON m.id = r.message_id WHERE r.emp_id = ? AND r.deleted = 0 AND r.is_read = 0 AND m.is_draft = 0", Integer.class, me));
            o.put("drafts", jdbc.queryForObject("SELECT COUNT(*) FROM intra_message WHERE sender_emp_id = ? AND is_draft = 1", Integer.class, me));
            o.put("ready", true);
        } catch (org.springframework.dao.DataAccessException e) { o.put("inbox", 0); o.put("drafts", 0); o.put("ready", false); }
        return o;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> list(String folder, int page, int size, String q, boolean unread) {
        tables(); String me = me(); size = Math.max(5, Math.min(size, 100)); page = Math.max(0, page); String like = q == null || q.isBlank() ? null : "%" + q.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        String from, where; List<Object> a = new ArrayList<>(); a.add(me);
        switch (folder == null ? "inbox" : folder) {
            case "sent" -> { from = "FROM intra_message m JOIN emp_detail s ON s.emp_id = m.sender_emp_id"; where = "WHERE m.sender_emp_id = ? AND m.is_draft = 0 AND m.sender_deleted = 0"; }
            case "drafts" -> { from = "FROM intra_message m JOIN emp_detail s ON s.emp_id = m.sender_emp_id"; where = "WHERE m.sender_emp_id = ? AND m.is_draft = 1"; }
            case "trash" -> { from = "FROM intra_recipient r JOIN intra_message m ON m.id = r.message_id JOIN emp_detail s ON s.emp_id = m.sender_emp_id"; where = "WHERE r.emp_id = ? AND r.deleted = 1 AND m.is_draft = 0"; }
            case "starred" -> { from = "FROM intra_recipient r JOIN intra_message m ON m.id = r.message_id JOIN emp_detail s ON s.emp_id = m.sender_emp_id"; where = "WHERE r.emp_id = ? AND r.deleted = 0 AND r.is_starred = 1 AND m.is_draft = 0"; }
            default -> { from = "FROM intra_recipient r JOIN intra_message m ON m.id = r.message_id JOIN emp_detail s ON s.emp_id = m.sender_emp_id"; where = "WHERE r.emp_id = ? AND r.deleted = 0 AND m.is_draft = 0"; }
        }
        boolean rcpt = from.contains("intra_recipient"); StringBuilder w = new StringBuilder(where);
        if (like != null) { w.append(" AND (m.subject LIKE ? OR s.emp_name LIKE ? OR m.body_text LIKE ?)"); a.add(like); a.add(like); a.add(like); }
        if (unread && rcpt) w.append(" AND r.is_read = 0");
        int total = jdbc.queryForObject("SELECT COUNT(*) " + from + " " + w, Integer.class, a.toArray());
        List<Object> a2 = new ArrayList<>(a); a2.add(size); a2.add(page * size);
        String sel = "SELECT m.id, m.subject, COALESCE(m.sent_at, m.created_at) AS sentAt, s.emp_name AS fromName, m.to_label AS toLabel, m.has_attach AS hasAttach, " + (rcpt ? "r.is_read AS isRead, r.is_starred AS isStarred" : "1 AS isRead, 0 AS isStarred") + ", SUBSTRING(m.body_text, 1, 160) AS snippet ";
        List<Map<String, Object>> rows = jdbc.queryForList(sel + from + " " + w + " ORDER BY COALESCE(m.sent_at, m.created_at) DESC, m.id DESC LIMIT ? OFFSET ?", a2.toArray());
        return Map.of("rows", rows, "total", total, "page", page, "size", size);
    }

    private Map<String, Object> canSee(long id, String me) {
        Map<String, Object> m = jdbc.queryForList("SELECT m.*, s.emp_name AS fromName FROM intra_message m JOIN emp_detail s ON s.emp_id = m.sender_emp_id WHERE m.id = ?", id).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Message not found"));
        boolean sender = me.equals(m.get("sender_emp_id")); boolean rcpt = jdbc.queryForObject("SELECT COUNT(*) FROM intra_recipient WHERE message_id = ? AND emp_id = ? AND deleted < 2", Integer.class, id, me) > 0;
        if (!sender && !rcpt) throw new ForbiddenException("This message is not yours"); if (!sender && "1".equals(String.valueOf(m.get("is_draft")))) throw new ForbiddenException("This message is not yours");
        m.put("_sender", sender); return m;
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> view(long id) {
        String me = me(); Map<String, Object> m = canSee(id, me); boolean sender = (boolean) m.get("_sender");
        if (!sender) jdbc.update("UPDATE intra_recipient SET is_read = 1, read_at = COALESCE(read_at, NOW()) WHERE message_id = ? AND emp_id = ?", id, me);
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("id", id); o.put("subject", m.get("subject")); o.put("fromName", m.get("fromName")); o.put("fromId", m.get("sender_emp_id")); o.put("sentAt", m.get("sent_at") != null ? m.get("sent_at") : m.get("created_at")); o.put("isDraft", m.get("is_draft")); o.put("toLabel", m.get("to_label"));
        o.put("html", m.get("body_html") == null ? null : MailReadService.sanitizeForView((String) m.get("body_html"), false)); o.put("text", m.get("body_text")); o.put("parentId", m.get("parent_id")); o.put("mine", sender);
        int n = jdbc.queryForObject("SELECT COUNT(*) FROM intra_recipient WHERE message_id = ?", Integer.class, id);
        if (n <= 50) o.put("recipients", jdbc.queryForList("SELECT e.emp_name AS name, r.kind FROM intra_recipient r JOIN emp_detail e ON e.emp_id = r.emp_id WHERE r.message_id = ? AND (r.kind <> 'BCC' OR ? ) ORDER BY FIELD(r.kind,'TO','CC','BCC'), e.emp_name", id, sender)); else o.put("recipientCount", n);
        if (sender && "1".equals(String.valueOf(m.get("is_draft"))) && m.get("draft_json") != null) { try { o.put("draft", JSON.readValue((String) m.get("draft_json"), new TypeReference<Map<String, Object>>() { })); } catch (Exception e) { /* old draft */ } }
        if (sender && n > 0) o.put("readCount", jdbc.queryForObject("SELECT COUNT(*) FROM intra_recipient WHERE message_id = ? AND is_read = 1", Integer.class, id));
        o.put("attachments", jdbc.queryForList("SELECT id, filename AS name, content_type AS type, size_bytes AS size FROM intra_attachment WHERE message_id = ? ORDER BY id", id));
        o.put("isStarred", sender ? 0 : jdbc.queryForObject("SELECT is_starred FROM intra_recipient WHERE message_id = ? AND emp_id = ?", Integer.class, id, me));
        return o;
    }

    public record Download(String name, String type, byte[] data) { }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Download attachment(long msgId, long attId) throws Exception {
        String me = me(); canSee(msgId, me);
        Map<String, Object> a = jdbc.queryForList("SELECT * FROM intra_attachment WHERE id = ? AND message_id = ?", attId, msgId).stream().findFirst().orElseThrow(() -> new ForbiddenException("This attachment is not yours"));
        return new Download((String) a.get("filename"), a.get("content_type") == null ? "application/octet-stream" : (String) a.get("content_type"), files.read((String) a.get("path")));
    }

    /** ids, action = READ | UNREAD | STAR | UNSTAR | DELETE | RESTORE | PURGE. In Sent a delete hides it for the sender only; a draft is removed for good. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> act(Map<String, Object> b) {
        tables(); String me = me(); String action = String.valueOf(b.get("action")).toUpperCase(); List<?> ids = (List<?>) b.get("ids");
        if (ids == null || ids.isEmpty() || ids.size() > 500) throw new IllegalArgumentException("Choose at least one message");
        int done = 0;
        for (Object o : ids) {
            long id = Long.parseLong(String.valueOf(o)); Map<String, Object> m = canSee(id, me); boolean sender = (boolean) m.get("_sender"), draft = "1".equals(String.valueOf(m.get("is_draft")));
            if (draft) { if (List.of("DELETE", "PURGE").contains(action)) { jdbc.update("DELETE FROM intra_message WHERE id = ?", id); done++; } continue; }
            switch (action) {
                case "READ" -> done += jdbc.update("UPDATE intra_recipient SET is_read = 1, read_at = COALESCE(read_at, NOW()) WHERE message_id = ? AND emp_id = ?", id, me);
                case "UNREAD" -> done += jdbc.update("UPDATE intra_recipient SET is_read = 0 WHERE message_id = ? AND emp_id = ?", id, me);
                case "STAR" -> done += jdbc.update("UPDATE intra_recipient SET is_starred = 1 WHERE message_id = ? AND emp_id = ?", id, me);
                case "UNSTAR" -> done += jdbc.update("UPDATE intra_recipient SET is_starred = 0 WHERE message_id = ? AND emp_id = ?", id, me);
                case "DELETE" -> { int n = jdbc.update("UPDATE intra_recipient SET deleted = 1 WHERE message_id = ? AND emp_id = ? AND deleted = 0", id, me); if (n == 0 && sender) n = jdbc.update("UPDATE intra_message SET sender_deleted = 1 WHERE id = ?", id); done += n; }
                case "RESTORE" -> done += jdbc.update("UPDATE intra_recipient SET deleted = 0 WHERE message_id = ? AND emp_id = ? AND deleted = 1", id, me);
                case "PURGE" -> done += jdbc.update("UPDATE intra_recipient SET deleted = 2 WHERE message_id = ? AND emp_id = ? AND deleted = 1", id, me);
                default -> throw new IllegalArgumentException("Unknown action");
            }
            if (!sender || jdbc.queryForObject("SELECT COUNT(*) FROM intra_recipient WHERE message_id = ? AND deleted < 2", Integer.class, id) == 0) cleanup(id);
        }
        return Map.of("done", done);
    }

    /** A message that nobody can see any more (sender removed it, every receiver deleted it for good) is dropped with its files. */
    private void cleanup(long id) {
        Map<String, Object> m = jdbc.queryForList("SELECT sender_deleted, is_draft FROM intra_message WHERE id = ?", id).stream().findFirst().orElse(null); if (m == null) return;
        boolean gone = "1".equals(String.valueOf(m.get("sender_deleted"))) || Boolean.TRUE.equals(m.get("sender_deleted"));
        if (gone && jdbc.queryForObject("SELECT COUNT(*) FROM intra_recipient WHERE message_id = ? AND deleted < 2", Integer.class, id) == 0) jdbc.update("DELETE FROM intra_message WHERE id = ?", id);
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> prepare(long id, String mode) {
        String me = me(); Map<String, Object> m = canSee(id, me); boolean fwd = "FORWARD".equalsIgnoreCase(mode);
        String subject = String.valueOf(m.get("subject") == null ? "" : m.get("subject")), prefix = fwd ? "Fwd: " : "Re: ";
        List<Map<String, Object>> to = new ArrayList<>(), cc = new ArrayList<>();
        if (!fwd) {
            String senderId = (String) m.get("sender_emp_id");
            if (!me.equals(senderId)) to.add(Map.of("type", "USER", "id", senderId, "label", String.valueOf(m.get("fromName"))));
            if ("REPLYALL".equalsIgnoreCase(mode) && jdbc.queryForObject("SELECT COUNT(*) FROM intra_recipient WHERE message_id = ?", Integer.class, id) <= 50)
                for (Map<String, Object> r : jdbc.queryForList("SELECT r.emp_id, e.emp_name FROM intra_recipient r JOIN emp_detail e ON e.emp_id = r.emp_id WHERE r.message_id = ? AND r.kind <> 'BCC' AND r.emp_id <> ?", id, me)) { Map<String, Object> p = Map.of("type", "USER", "id", r.get("emp_id"), "label", r.get("emp_name")); if (!senderId.equals(r.get("emp_id"))) cc.add(p); }
            if (to.isEmpty() && !me.equals(senderId)) throw new IllegalArgumentException("Nobody to reply to");
        }
        String head = fwd ? "<br><br>---------- Forwarded message ----------<br>From: " + esc(String.valueOf(m.get("fromName"))) + "<br>Subject: " + esc(subject) + "<br><br>" : "<br><br>" + esc(String.valueOf(m.get("fromName"))) + " wrote:<br>";
        String inner = m.get("body_html") == null ? "" : MailSendService.COMPOSE_POLICY.sanitize((String) m.get("body_html"));
        Map<String, Object> out = new LinkedHashMap<>(); out.put("to", to); out.put("cc", cc); out.put("subject", subject.regionMatches(true, 0, prefix, 0, prefix.length()) ? subject : prefix + subject); out.put("html", head + (fwd ? inner : "<blockquote>" + inner + "</blockquote>")); out.put("parentId", id);
        out.put("attachments", fwd ? jdbc.queryForList("SELECT id AS fwdAtt, filename AS name, size_bytes AS size FROM intra_attachment WHERE message_id = ?", id) : List.of());
        return out;
    }
    private static String esc(String s) { return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
}
