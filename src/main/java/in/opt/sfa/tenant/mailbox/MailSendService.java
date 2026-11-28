package in.opt.sfa.tenant.mailbox;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.opt.sfa.exception.ForbiddenException;
import in.opt.sfa.tenant.team.TeamScope;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.MimeMessage;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Sending: the screen only QUEUES a mail (status QUEUED); a worker sends it. The Message-ID is made once when it is queued, a mail is claimed with one atomic UPDATE (two workers can never take the same mail),
 * a temporary failure is retried a few times, and when it is not known whether the server took the mail (UNKNOWN) it is NEVER sent again by itself - the user decides.
 */
@Service
public class MailSendService {
    private static final Logger log = LoggerFactory.getLogger(MailSendService.class);
    private static final int MAX_ATTEMPTS = 3, MAX_RECIPIENTS = 50;
    private static final long[] BACKOFF_MIN = {1, 5, 15};
    private static final ObjectMapper JSON = new ObjectMapper();

    /** What a user writes in the editor: formatting, lists, links and https images - nothing that runs. */
    static final PolicyFactory COMPOSE_POLICY = new HtmlPolicyBuilder().allowElements("p", "div", "span", "br", "b", "strong", "i", "em", "u", "s", "ul", "ol", "li", "blockquote", "pre", "code", "h1", "h2", "h3", "h4", "table", "thead", "tbody", "tr", "td", "th", "hr", "a", "img", "font")
            .allowAttributes("style").matching(java.util.regex.Pattern.compile("(?i)^[a-z\\-:;#%.,()\\s0-9\"']{0,300}$")).onElements("p", "div", "span", "td", "th", "table", "font", "blockquote")
            .allowAttributes("href").onElements("a").allowAttributes("src", "alt", "width", "height").onElements("img").allowAttributes("color", "size", "face").onElements("font")
            .allowStandardUrlProtocols().allowUrlProtocols("mailto").requireRelNofollowOnLinks().toFactory();

    private final JdbcTemplate jdbc;
    private final MailAccountService accounts;
    private final MailAttachmentStore files;
    private final RateLimiter limiter;
    private final TenantRunner tenants;

    public MailSendService(JdbcTemplate jdbc, MailAccountService accounts, MailAttachmentStore files, RateLimiter limiter, TenantRunner tenants) { this.jdbc = jdbc; this.accounts = accounts; this.files = files; this.limiter = limiter; this.tenants = tenants; }

    private static String me() { String m = TeamScope.me(); if (m == null) throw new ForbiddenException("Sign in again"); return m; }
    private static String csv(List<String> l) { return l == null || l.isEmpty() ? null : String.join(", ", l); }
    @SuppressWarnings("unchecked") private static List<String> list(Object o) { if (o instanceof List<?> l) return l.stream().map(String::valueOf).toList(); if (o instanceof String s && !s.isBlank()) return List.of(s); return List.of(); }

    private MailAccountService.Account myAccount() {
        MailAccountService.Account a = accounts.load(me()).orElseThrow(() -> new IllegalStateException("Set up your mail account first (Mail Settings)"));
        if (!a.enabled()) throw new IllegalStateException("Your mail account is switched off"); if (a.password() == null) throw new IllegalStateException("Enter the mail password in Mail Settings");
        return a;
    }

    // ------------------------------------------------------------------ queue
    /** Queue a mail. body: to, cc, bcc (lists or comma text), subject, html, attachments [{path,name,size,type} uploaded | {fwd: localMessageId, part: n}], replyTo (local message id: sets In-Reply-To / References). */
    @Transactional(transactionManager = "tenantTransactionManager")
    @SuppressWarnings("unchecked")
    public Map<String, Object> enqueue(Map<String, Object> b) throws Exception {
        MailAccountService.Account acc = myAccount(); String emp = me();
        limiter.check("mailsend:" + emp, 40, 10 * 60_000, "You are sending too fast - wait a few minutes");
        List<String> to = MimeBuilder.addresses(splitList(b.get("to")), "To"), cc = MimeBuilder.addresses(splitList(b.get("cc")), "Cc"), bcc = MimeBuilder.addresses(splitList(b.get("bcc")), "Bcc");
        if (to.isEmpty() && cc.isEmpty() && bcc.isEmpty()) throw new IllegalArgumentException("Add at least one recipient");
        if (to.size() + cc.size() + bcc.size() > MAX_RECIPIENTS) throw new IllegalArgumentException("Too many recipients (limit " + MAX_RECIPIENTS + ")");
        String subject = b.get("subject") == null ? "" : b.get("subject").toString().trim(); MimeBuilder.noCrlf(subject, "Subject"); if (subject.length() > 400) throw new IllegalArgumentException("The subject is too long");
        String html = COMPOSE_POLICY.sanitize(b.get("html") == null ? "" : b.get("html").toString());
        if (html.length() > 5_000_000) throw new IllegalArgumentException("The mail text is too big");
        String sig = acc.signature() == null || acc.signature().isBlank() || Boolean.FALSE.equals(b.get("withSignature")) ? "" : "<br>-- <br>" + COMPOSE_POLICY.sanitize(acc.signature().replace("\n", "<br>"));
        html = html + sig;
        // attachments: an uploaded file is accepted only when it is mine; a forwarded one only from my own message
        long total = 0; int maxBytes = accounts.maxAttachmentMb() * 1024 * 1024; List<Map<String, Object>> atts = new ArrayList<>();
        if (b.get("attachments") instanceof List<?> al) for (Object o : al) {
            Map<String, Object> a = (Map<String, Object>) o; Map<String, Object> keep = new LinkedHashMap<>();
            if (a.get("fwd") != null) {
                long mid = Long.parseLong(String.valueOf(a.get("fwd"))); int part = Integer.parseInt(String.valueOf(a.get("part")));
                Map<String, Object> row = jdbc.queryForList("SELECT a.filename, a.content_type, a.size_bytes FROM mail_attachment a JOIN mail_message m ON m.id = a.message_id WHERE a.message_id = ? AND a.part_no = ? AND m.account_id = ?", mid, part, acc.id()).stream().findFirst().orElseThrow(() -> new ForbiddenException("This attachment is not yours"));
                keep.put("fwd", mid); keep.put("part", part); keep.put("name", row.get("filename")); keep.put("size", row.get("size_bytes")); total += row.get("size_bytes") == null ? 0 : ((Number) row.get("size_bytes")).longValue();
            } else {
                String path = String.valueOf(a.get("path")); if (!files.owns(emp, path)) throw new ForbiddenException("This attachment is not yours");
                keep.put("path", path); keep.put("name", MailAttachmentStore.safeName(String.valueOf(a.get("name")))); keep.put("type", a.get("type")); long sz = a.get("size") == null ? 0 : Long.parseLong(String.valueOf(a.get("size"))); keep.put("size", sz); total += sz;
            }
            atts.add(keep);
        }
        if (total > maxBytes) throw new IllegalArgumentException("The attachments are bigger than the limit of " + accounts.maxAttachmentMb() + " MB");
        String inReplyTo = null, refs = null;
        if (b.get("replyTo") != null && !String.valueOf(b.get("replyTo")).isBlank()) {
            Map<String, Object> o = jdbc.queryForList("SELECT message_id, refs FROM mail_message WHERE id = ? AND account_id = ?", Long.parseLong(String.valueOf(b.get("replyTo"))), acc.id()).stream().findFirst().orElseThrow(() -> new ForbiddenException("This mail is not yours"));
            inReplyTo = (String) o.get("message_id"); String r = (String) o.get("refs"); refs = ((r == null ? "" : r.trim() + " ") + (inReplyTo == null ? "" : inReplyTo)).trim(); if (refs.isEmpty()) refs = null;
        }
        String mid = MimeBuilder.newMessageId(acc.email());
        jdbc.update("INSERT INTO mail_outbox (account_id, status, message_id, in_reply_to, refs, to_addrs, cc_addrs, bcc_addrs, subject, body_html, body_text, attachments_json, next_attempt_at) VALUES (?,'QUEUED',?,?,?,?,?,?,?,?,?,?,NOW())",
                acc.id(), mid, inReplyTo, refs, csv(to) == null ? "" : csv(to), csv(cc), csv(bcc), subject, html, MimeBuilder.htmlToText(html), atts.isEmpty() ? null : JSON.writeValueAsString(atts));
        long id = jdbc.queryForObject("SELECT id FROM mail_outbox WHERE account_id = ? AND message_id = ?", Long.class, acc.id(), mid);
        return Map.of("id", id, "status", "QUEUED");
    }

    private static List<String> splitList(Object o) {
        List<String> l = list(o); List<String> out = new ArrayList<>();
        for (String s : l) for (String p : s.split("[;\\n]")) if (!p.isBlank()) out.add(p.trim());
        return out;
    }

    /** My outbox (what is waiting, failed or unsure). */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> outbox() {
        return jdbc.queryForList("SELECT o.id, o.status, o.to_addrs AS toAddrs, o.subject, o.attempts, o.last_error AS lastError, o.created_at AS createdAt, o.sent_at AS sentAt FROM mail_outbox o JOIN mail_account a ON a.id = o.account_id WHERE a.emp_id = ? AND o.status <> 'SENT' ORDER BY o.id DESC LIMIT 100", me());
    }

    private long ownOutbox(long id) { return jdbc.queryForList("SELECT o.id FROM mail_outbox o JOIN mail_account a ON a.id = o.account_id WHERE o.id = ? AND a.emp_id = ?", Long.class, id, me()).stream().findFirst().orElseThrow(() -> new ForbiddenException("This mail is not yours")); }

    /** FAILED: the user may try again. UNKNOWN: the user decides - "send again" (a duplicate is possible) or "it was sent". */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> resolve(long id, String action) {
        ownOutbox(id);
        switch (action) {
            case "RETRY" -> { int n = jdbc.update("UPDATE mail_outbox SET status = 'QUEUED', attempts = 0, next_attempt_at = NOW(), last_error = NULL WHERE id = ? AND status IN ('FAILED','UNKNOWN')", id); if (n == 0) throw new IllegalStateException("This mail is not in a state that can be sent again"); }
            case "SENT" -> { int n = jdbc.update("UPDATE mail_outbox SET status = 'SENT', sent_at = NOW(), copy_saved = 2 WHERE id = ? AND status = 'UNKNOWN'", id); if (n == 0) throw new IllegalStateException("Only a mail of unknown state can be marked as sent"); }
            case "DISCARD" -> { jdbc.update("DELETE FROM mail_outbox WHERE id = ? AND status IN ('QUEUED','FAILED','UNKNOWN')", id); }
            default -> throw new IllegalArgumentException("Unknown action");
        }
        return Map.of("ok", true);
    }

    // ------------------------------------------------------------------ worker
    /** Called by the scheduler for every tenant. */
    public void processTenant() {
        // a mail that stayed SENDING for 10 minutes (the application stopped while sending): we cannot know if it went - UNKNOWN, never resent by itself
        jdbc.update("UPDATE mail_outbox SET status = 'UNKNOWN', last_error = 'The application stopped while this mail was being sent - check your Sent folder before sending again.' WHERE status = 'SENDING' AND claimed_at < NOW() - INTERVAL 10 MINUTE");
        List<Long> due = jdbc.queryForList("SELECT id FROM mail_outbox WHERE status = 'QUEUED' AND (next_attempt_at IS NULL OR next_attempt_at <= NOW()) ORDER BY id LIMIT 20", Long.class);
        for (Long id : due) try { processOne(id); } catch (Exception e) { log.warn("Mail outbox {} failed: {}", id, e.getMessage()); }
        saveSentCopies();
    }

    /** Claim and send one mail. Returns the final status (or null when another worker had it). */
    public String processOne(long id) throws Exception {
        int claimed = jdbc.update("UPDATE mail_outbox SET status = 'SENDING', claimed_at = NOW(), attempts = attempts + 1 WHERE id = ? AND status = 'QUEUED'", id);
        if (claimed == 0) return null;   // someone else has it - never two senders for one mail
        Map<String, Object> o = jdbc.queryForMap("SELECT * FROM mail_outbox WHERE id = ?", id);
        MailAccountService.Account acc = accounts.loadById(((Number) o.get("account_id")).longValue()).orElse(null);
        if (acc == null || !acc.enabled() || acc.password() == null) return finish(id, "FAILED", "The mail account is missing, switched off or has no password.", 0);
        MimeMessage msg;
        try { msg = MimeBuilder.build(Session.getInstance(new Properties()), outOf(acc, o)); }
        catch (Exception e) { return finish(id, "FAILED", "The mail could not be prepared: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()), 0); }
        try { HostGuard.check(acc.smtp().host(), acc.smtp().port(), accounts.internalHosts()); }   // checked again at send time, not only when the account was saved
        catch (IllegalArgumentException e) { return finish(id, "FAILED", e.getMessage(), 0); }
        SmtpSender.Outcome r = SmtpSender.send(acc.smtp(), acc.username(), acc.password(), msg);
        int attempts = ((Number) o.get("attempts")).intValue() + 1;
        return switch (r.result()) {
            case SENT -> { jdbc.update("UPDATE mail_outbox SET status = 'SENT', sent_at = NOW(), last_error = NULL, copy_saved = ? WHERE id = ?", MailSendService.sentCopyNeeded(acc) ? 0 : 2, id); yield "SENT"; }
            case RETRY -> attempts >= MAX_ATTEMPTS ? finish(id, "FAILED", r.message(), 0) : requeue(id, r.message(), BACKOFF_MIN[Math.min(attempts - 1, BACKOFF_MIN.length - 1)]);
            case REJECTED -> finish(id, "FAILED", r.message(), 0);
            case UNKNOWN -> finish(id, "UNKNOWN", r.message(), 0);
        };
    }

    static boolean sentCopyNeeded(MailAccountService.Account acc) { return !ImapFolders.serverSavesSent(acc.smtp().host()); }
    private String finish(long id, String status, String msg, int x) { jdbc.update("UPDATE mail_outbox SET status = ?, last_error = ? WHERE id = ?", status, msg == null ? null : msg.substring(0, Math.min(390, msg.length())), id); return status; }
    private String requeue(long id, String msg, long minutes) { jdbc.update("UPDATE mail_outbox SET status = 'QUEUED', last_error = ?, next_attempt_at = NOW() + INTERVAL ? MINUTE WHERE id = ?", msg == null ? null : msg.substring(0, Math.min(390, msg.length())), minutes, id); return "QUEUED"; }

    private MimeBuilder.Out outOf(MailAccountService.Account acc, Map<String, Object> o) throws Exception {
        List<MimeBuilder.Attachment> atts = new ArrayList<>();
        if (o.get("attachments_json") != null) for (Map<String, Object> a : JSON.readValue((String) o.get("attachments_json"), new TypeReference<List<Map<String, Object>>>() { })) {
            if (a.get("path") != null) atts.add(new MimeBuilder.Attachment(String.valueOf(a.get("name")), a.get("type") == null ? null : String.valueOf(a.get("type")), files.read(String.valueOf(a.get("path")))));
            else atts.add(forwarded(acc, Long.parseLong(String.valueOf(a.get("fwd"))), Integer.parseInt(String.valueOf(a.get("part")))));
        }
        return new MimeBuilder.Out(acc.email(), acc.displayName(), parse((String) o.get("to_addrs")), parse((String) o.get("cc_addrs")), parse((String) o.get("bcc_addrs")), (String) o.get("subject"), (String) o.get("body_html"), (String) o.get("body_text"), (String) o.get("message_id"), (String) o.get("in_reply_to"), (String) o.get("refs"), atts);
    }
    private static List<String> parse(String csv) { return csv == null || csv.isBlank() ? List.of() : Arrays.stream(csv.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")).map(String::trim).filter(s -> !s.isEmpty()).toList(); }

    /** A forwarded attachment is fetched from the mail server at the moment of sending. */
    private MimeBuilder.Attachment forwarded(MailAccountService.Account acc, long localMsgId, int part) throws Exception {
        return ImapAttachment.fetch(accounts, acc, localMsgId, part, jdbc);
    }

    /** The mails that went out through a server that does not keep a copy: append the copy to the Sent folder (once; tried again later when it failed, for one day). */
    void saveSentCopies() {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM mail_outbox WHERE status = 'SENT' AND copy_saved = 0 AND sent_at > NOW() - INTERVAL 1 DAY ORDER BY id LIMIT 20");
        for (Map<String, Object> o : rows) {
            long id = ((Number) o.get("id")).longValue();
            try {
                MailAccountService.Account acc = accounts.loadById(((Number) o.get("account_id")).longValue()).orElse(null);
                if (acc == null || !acc.enabled() || acc.password() == null) { jdbc.update("UPDATE mail_outbox SET copy_saved = 2 WHERE id = ?", id); continue; }
                MimeMessage msg = MimeBuilder.build(Session.getInstance(new Properties()), outOf(acc, o)); msg.setFlag(Flags.Flag.SEEN, true);
                try (Store st = ImapConnect.open(accounts, acc)) {
                    String sent = ImapFolders.discover(st).stream().filter(i -> "SENT".equals(i.role())).map(ImapFolders.Info::path).findFirst().orElse(null);
                    if (sent == null) { jdbc.update("UPDATE mail_outbox SET copy_saved = 2 WHERE id = ?", id); continue; }   // no Sent folder on this server
                    Folder f = st.getFolder(sent); f.open(Folder.READ_WRITE);
                    try { f.appendMessages(new Message[]{msg}); } finally { f.close(false); }
                }
                jdbc.update("UPDATE mail_outbox SET copy_saved = 1 WHERE id = ?", id);
            } catch (Exception e) { log.warn("Sent copy of outbox {} not saved yet: {}", id, e.getMessage()); }
        }
    }

    /** Run for all tenants (scheduler). */
    public void runAll() { tenants.forEachTenant(t -> processTenant()); }
}
