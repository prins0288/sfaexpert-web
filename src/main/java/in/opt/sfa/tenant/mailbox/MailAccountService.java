package in.opt.sfa.tenant.mailbox;

import in.opt.sfa.exception.ForbiddenException;
import in.opt.sfa.security.Authz;
import in.opt.sfa.tenant.team.TeamScope;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.Folder;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** The external mail account of the signed-in employee (SMTP + IMAP) and the company's mail defaults. The password is stored as typed (company decision) but is never sent back to the screen or logged. */
@Service
public class MailAccountService {
    private static final Logger log = LoggerFactory.getLogger(MailAccountService.class);
    private static final Set<String> SEC = Set.of("SSL", "STARTTLS", "NONE");

    /** An account with its decrypted password - only for the services that connect (sync, send); never put into a response or a log. */
    public record Account(long id, String empId, String email, String displayName, String username, String password, MailSessions.Server smtp, MailSessions.Server imap, String signature, boolean enabled) { }

    private final JdbcTemplate jdbc;
    private final RateLimiter limiter;

    public MailAccountService(JdbcTemplate jdbc, RateLimiter limiter) { this.jdbc = jdbc; this.limiter = limiter; }

    // ------------------------------------------------------------------ company defaults (admin)
    private Map<String, Object> adminRow() {
        try { List<Map<String, Object>> l = jdbc.queryForList("SELECT * FROM mail_admin_setting WHERE id = 1"); return l.isEmpty() ? new HashMap<>() : l.get(0); }
        catch (org.springframework.dao.DataAccessException e) { throw new IllegalStateException("The mail tables are missing - run migration 134_mailbox_core.sql"); }
    }
    String internalHosts() { Object o = adminRow().get("internal_hosts"); return o == null ? "" : o.toString(); }
    public int syncIntervalMin() { Object o = adminRow().get("sync_interval_min"); return o instanceof Number n ? Math.max(1, n.intValue()) : 10; }
    public int initialSyncDays() { Object o = adminRow().get("initial_sync_days"); return o instanceof Number n ? Math.max(1, n.intValue()) : 30; }
    public int maxAttachmentMb() { Object o = adminRow().get("max_attachment_mb"); return o instanceof Number n ? Math.max(1, n.intValue()) : 20; }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> adminGet() {
        Authz.requireRole("ADMIN");
        Map<String, Object> r = adminRow(), m = new LinkedHashMap<>();
        for (String k : List.of("company_domain", "default_smtp_host", "default_smtp_port", "default_smtp_security", "default_imap_host", "default_imap_port", "default_imap_security", "internal_hosts", "sync_interval_min", "initial_sync_days", "max_attachment_mb", "intramail_for_all")) m.put(k, r.get(k));
        return m;
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void adminSave(Map<String, Object> b) {
        Authz.requireRole("ADMIN"); adminRow();
        String ih = s(b.get("internal_hosts")); List<String> hosts = HostGuard.allowedHosts(ih);
        for (String h : hosts) if (!h.matches("[a-z0-9]([a-z0-9.-]*[a-z0-9])?")) throw new IllegalArgumentException("Internal host is not valid: " + h);
        int sync = intOf(b.get("sync_interval_min"), 10, 1, 1440, "Sync interval (minutes)"), days = intOf(b.get("initial_sync_days"), 30, 1, 3650, "First sync (days)"), mb = intOf(b.get("max_attachment_mb"), 20, 1, 100, "Attachment limit (MB)");
        String sec1 = secOrNull(b.get("default_smtp_security")), sec2 = secOrNull(b.get("default_imap_security"));
        jdbc.update("UPDATE mail_admin_setting SET company_domain = ?, default_smtp_host = ?, default_smtp_port = ?, default_smtp_security = ?, default_imap_host = ?, default_imap_port = ?, default_imap_security = ?, internal_hosts = ?, sync_interval_min = ?, initial_sync_days = ?, max_attachment_mb = ?, intramail_for_all = ?, updated_by = ?, updated_at = NOW() WHERE id = 1",
                s(b.get("company_domain")), s(b.get("default_smtp_host")), b.get("default_smtp_port") == null || b.get("default_smtp_port").toString().isBlank() ? null : intOf(b.get("default_smtp_port"), 587, 1, 65535, "SMTP port"), sec1,
                s(b.get("default_imap_host")), b.get("default_imap_port") == null || b.get("default_imap_port").toString().isBlank() ? null : intOf(b.get("default_imap_port"), 993, 1, 65535, "IMAP port"), sec2,
                hosts.isEmpty() ? null : String.join(",", hosts), sync, days, mb, !"false".equalsIgnoreCase(String.valueOf(b.getOrDefault("intramail_for_all", true))) ? 1 : 0, TeamScope.me());
    }

    /** Presets for the form: where the server of a provider is. OAuth2 note: see the hint text. */
    public List<Map<String, Object>> presets() {
        return List.of(
                preset("gmail", "Gmail / Google Workspace", "smtp.gmail.com", 587, "STARTTLS", "imap.gmail.com", 993, "SSL", "Gmail needs an App Password (2-Step Verification must be on): Google Account > Security > App passwords. Your normal Gmail password does not work here. Google Workspace may require OAuth2 if the admin turned off app passwords."),
                preset("outlook", "Outlook / Microsoft 365", "smtp.office365.com", 587, "STARTTLS", "outlook.office365.com", 993, "SSL", "Microsoft 365 / Outlook.com have switched off password (basic) sign-in for IMAP and SMTP - they need OAuth2 (not available in this version). It works only if your admin still allows basic authentication."),
                preset("yahoo", "Yahoo Mail", "smtp.mail.yahoo.com", 465, "SSL", "imap.mail.yahoo.com", 993, "SSL", "Yahoo needs an App Password: Yahoo Account Security > Generate app password."));
    }
    private static Map<String, Object> preset(String id, String name, String sh, int sp, String ss, String ih, int ip, String is, String hint) {
        Map<String, Object> m = new LinkedHashMap<>(); m.put("id", id); m.put("name", name); m.put("smtpHost", sh); m.put("smtpPort", sp); m.put("smtpSecurity", ss); m.put("imapHost", ih); m.put("imapPort", ip); m.put("imapSecurity", is); m.put("hint", hint); return m;
    }

    // ------------------------------------------------------------------ my account
    private static String s(Object o) { return o == null || o.toString().isBlank() ? null : o.toString().trim(); }
    private static int intOf(Object o, int dflt, int min, int max, String what) {
        if (o == null || o.toString().isBlank()) return dflt;
        try { int v = Integer.parseInt(o.toString().trim()); if (v < min || v > max) throw new NumberFormatException(); return v; } catch (NumberFormatException e) { throw new IllegalArgumentException(what + " must be a number from " + min + " to " + max); }
    }
    private static String secOrNull(Object o) { String v = s(o); if (v == null) return null; v = v.toUpperCase(); if (!SEC.contains(v)) throw new IllegalArgumentException("Security must be SSL, STARTTLS or NONE"); return v; }
    private static String sec(Object o, String what) { String v = secOrNull(o); if (v == null) throw new IllegalArgumentException(what + " security is required"); return v; }
    private static String noCrlf(String v, String what) { if (v != null && (v.indexOf('\r') >= 0 || v.indexOf('\n') >= 0)) throw new IllegalArgumentException(what + " has an invalid character"); return v; }
    private static String me() { String m = TeamScope.me(); if (m == null) throw new ForbiddenException("Sign in again"); return m; }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> get() {
        Map<String, Object> out = new LinkedHashMap<>(); Map<String, Object> admin = adminRow();
        out.put("presets", presets());
        Map<String, Object> d = new LinkedHashMap<>(); d.put("domain", admin.get("company_domain")); d.put("smtpHost", admin.get("default_smtp_host")); d.put("smtpPort", admin.get("default_smtp_port")); d.put("smtpSecurity", admin.get("default_smtp_security")); d.put("imapHost", admin.get("default_imap_host")); d.put("imapPort", admin.get("default_imap_port")); d.put("imapSecurity", admin.get("default_imap_security"));
        out.put("defaults", d); out.put("intramailForAll", admin.get("intramail_for_all"));
        List<Map<String, Object>> l = jdbc.queryForList("SELECT id, email, display_name AS displayName, username, auth_type AS authType, smtp_host AS smtpHost, smtp_port AS smtpPort, smtp_security AS smtpSecurity, imap_host AS imapHost, imap_port AS imapPort, imap_security AS imapSecurity, signature, is_enabled AS enabled, (password IS NOT NULL) AS hasPassword, last_sync_at AS lastSyncAt, last_sync_error AS lastSyncError FROM mail_account WHERE emp_id = ?", me());
        out.put("account", l.isEmpty() ? null : l.get(0));
        return out;
    }

    /** Create or change my account. A blank password keeps the saved one. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> save(Map<String, Object> b) {
        String emp = me(); String hosts = internalHosts();
        String email = noCrlf(s(b.get("email")), "Email"), name = noCrlf(s(b.get("displayName")), "Display name"), user = noCrlf(s(b.get("username")), "User name");
        if (email == null || !email.matches("[^@\\s<>,;]+@[^@\\s<>,;]+\\.[^@\\s<>,;]+") || email.length() > 150) throw new IllegalArgumentException("Enter a valid email address");
        if (name != null && name.length() > 100) throw new IllegalArgumentException("Display name is too long");
        if (user == null) user = email; if (user.length() > 150) throw new IllegalArgumentException("User name is too long");
        String auth = String.valueOf(b.getOrDefault("authType", "PASSWORD")).toUpperCase();
        if (!"PASSWORD".equals(auth)) throw new IllegalArgumentException("OAuth2 sign-in is not available yet - use a password (App Password for Gmail / Yahoo)");
        String sh = s(b.get("smtpHost")), ih = s(b.get("imapHost")); int sp = intOf(b.get("smtpPort"), 587, 1, 65535, "SMTP port"), ip = intOf(b.get("imapPort"), 993, 1, 65535, "IMAP port");
        String ss = sec(b.get("smtpSecurity"), "SMTP"), is = sec(b.get("imapSecurity"), "IMAP");
        HostGuard.check(sh, sp, hosts); HostGuard.check(ih, ip, hosts);
        String sig = b.get("signature") == null ? null : b.get("signature").toString(); if (sig != null && sig.length() > 4000) throw new IllegalArgumentException("Signature is too long (4000 characters)");
        String pw = b.get("password") == null || b.get("password").toString().isEmpty() ? null : b.get("password").toString();
        boolean exists = !jdbc.queryForList("SELECT id FROM mail_account WHERE emp_id = ?", emp).isEmpty();
        if (!exists && pw == null) throw new IllegalArgumentException("Enter the mail password");
        boolean enabled = !"false".equalsIgnoreCase(String.valueOf(b.getOrDefault("enabled", true)));
        if (exists) jdbc.update("UPDATE mail_account SET email = ?, display_name = ?, username = ?, password = COALESCE(?, password), smtp_host = ?, smtp_port = ?, smtp_security = ?, imap_host = ?, imap_port = ?, imap_security = ?, signature = ?, is_enabled = ?, updated_at = NOW() WHERE emp_id = ?",
                email, name, user, pw, sh.toLowerCase(), sp, ss, ih.toLowerCase(), ip, is, sig, enabled ? 1 : 0, emp);
        else jdbc.update("INSERT INTO mail_account (emp_id, email, display_name, username, password, smtp_host, smtp_port, smtp_security, imap_host, imap_port, imap_security, signature, is_enabled) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                emp, email, name, user, pw, sh.toLowerCase(), sp, ss, ih.toLowerCase(), ip, is, sig, enabled ? 1 : 0);
        log.info("Mail account saved for employee {}", emp);   // never the password
        return Map.of("saved", true);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void setEnabled(boolean on) { jdbc.update("UPDATE mail_account SET is_enabled = ? WHERE emp_id = ?", on ? 1 : 0, me()); }

    /** Remove my mail account with every cached mail and queued mail (the mails stay on the server). */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void delete() { jdbc.update("DELETE FROM mail_account WHERE emp_id = ?", me()); }

    /** The account of an employee with the decrypted password - for the sync and the sender only. */
    public Optional<Account> load(String empId) {
        List<Map<String, Object>> l = jdbc.queryForList("SELECT * FROM mail_account WHERE emp_id = ?", empId);
        return l.isEmpty() ? Optional.empty() : Optional.of(toAccount(l.get(0)));
    }
    public Optional<Account> loadById(long id) {
        List<Map<String, Object>> l = jdbc.queryForList("SELECT * FROM mail_account WHERE id = ?", id);
        return l.isEmpty() ? Optional.empty() : Optional.of(toAccount(l.get(0)));
    }
    private Account toAccount(Map<String, Object> r) {
        String emp = (String) r.get("emp_id"); String pw = (String) r.get("password");   // as typed
        return new Account(((Number) r.get("id")).longValue(), emp, (String) r.get("email"), (String) r.get("display_name"), (String) r.get("username"), pw,
                new MailSessions.Server((String) r.get("smtp_host"), ((Number) r.get("smtp_port")).intValue(), (String) r.get("smtp_security")),
                new MailSessions.Server((String) r.get("imap_host"), ((Number) r.get("imap_port")).intValue(), (String) r.get("imap_security")),
                (String) r.get("signature"), Boolean.TRUE.equals(r.get("is_enabled")) || (r.get("is_enabled") instanceof Number n && n.intValue() == 1));
    }

    // ------------------------------------------------------------------ test connection
    /** Test the SMTP and the IMAP login with the values on the screen (a blank password uses the saved one). Each result has a clear message. */
    public Map<String, Object> test(Map<String, Object> b) {
        String emp = me();
        limiter.check("mailtest:" + emp, 6, 60_000, "Too many connection tests - wait a minute and try again");
        String hosts = internalHosts();
        String user = s(b.get("username")) != null ? s(b.get("username")) : s(b.get("email"));
        String sh = s(b.get("smtpHost")), ih = s(b.get("imapHost")); int sp = intOf(b.get("smtpPort"), 587, 1, 65535, "SMTP port"), ip = intOf(b.get("imapPort"), 993, 1, 65535, "IMAP port");
        String ss = sec(b.get("smtpSecurity"), "SMTP"), is = sec(b.get("imapSecurity"), "IMAP");
        String pw = b.get("password") == null || b.get("password").toString().isEmpty() ? null : b.get("password").toString();
        if (pw == null) pw = load(emp).map(Account::password).orElse(null);
        if (user == null) throw new IllegalArgumentException("Enter the user name or email");
        if (pw == null) throw new IllegalArgumentException("Enter the mail password");
        Map<String, Object> out = new LinkedHashMap<>();
        final String pwf = pw;
        out.put("smtp", probe(() -> { HostGuard.check(sh, sp, hosts); smtpLogin(new MailSessions.Server(sh, sp, ss), user, pwf); return "SMTP login works (" + sh + ":" + sp + ")"; }));
        out.put("imap", probe(() -> { HostGuard.check(ih, ip, hosts); int n = imapLogin(new MailSessions.Server(ih, ip, is), user, pwf); return "IMAP login works (" + ih + ":" + ip + "), " + n + " folders found"; }));
        return out;
    }

    private interface Probe { String run() throws Exception; }
    private Map<String, Object> probe(Probe p) {
        Map<String, Object> m = new LinkedHashMap<>();
        try { m.put("ok", true); m.put("message", p.run()); }
        catch (Exception e) { m.put("ok", false); m.put("message", friendly(e)); }
        return m;
    }

    static void smtpLogin(MailSessions.Server s, String user, String pw) throws Exception {
        Session session = MailSessions.smtp(s, true);
        try (Transport t = session.getTransport("smtp")) { t.connect(s.host(), s.port(), user, pw); }
    }
    static int imapLogin(MailSessions.Server s, String user, String pw) throws Exception {
        Session session = MailSessions.imap(s);
        try (Store st = session.getStore(MailSessions.protocol(s.security()))) {
            st.connect(s.host(), s.port(), user, pw);
            Folder[] all = st.getDefaultFolder().list("*"); return all.length;
        }
    }

    /** A message the user can act on - never a stack trace, never the password. */
    static String friendly(Throwable e) {
        Throwable t = e; while (t.getCause() != null && !(t instanceof AuthenticationFailedException)) t = t.getCause();
        if (e instanceof IllegalArgumentException || e instanceof IllegalStateException) return e.getMessage();
        if (t instanceof AuthenticationFailedException || String.valueOf(e.getMessage()).toLowerCase().contains("auth")) return "Login failed: the user name or password is wrong. Gmail / Yahoo need an App Password; Microsoft 365 may need OAuth2.";
        if (t instanceof javax.net.ssl.SSLHandshakeException || String.valueOf(t.getMessage()).contains("PKIX") || String.valueOf(t.getMessage()).toLowerCase().contains("certificate")) return "The server certificate is not trusted or does not match the host name. Check the host name and the security setting.";
        if (t instanceof java.net.SocketTimeoutException) return "The server did not answer in time (timeout). Check the host, port and security setting.";
        if (t instanceof java.net.UnknownHostException) return "The server host cannot be found. Check the host name.";
        if (t instanceof java.net.ConnectException) return "Cannot connect to the server (refused). Check the port and the security setting.";
        String m = t.getMessage(); return "Connection failed" + (m == null ? "" : ": " + m.replaceAll("[\\r\\n]+", " ").substring(0, Math.min(160, m.length())));
    }
}
