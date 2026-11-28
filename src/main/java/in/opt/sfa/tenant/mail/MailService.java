package in.opt.sfa.tenant.mail;

import in.opt.sfa.security.Authz;
import in.opt.sfa.tenant.team.TeamScope;
import jakarta.mail.internet.MimeMessage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/** The mail server of the company (table smtp_setting, one row per tenant DB). Used for the expense reminders and any other system mail. */
@Service
public class MailService {
    private final JdbcTemplate jdbc;

    public MailService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private Map<String, Object> row() {
        try { List<Map<String, Object>> l = jdbc.queryForList("SELECT * FROM smtp_setting WHERE id = 1"); return l.isEmpty() ? null : l.get(0); }
        catch (org.springframework.dao.DataAccessException e) { throw new IllegalStateException("The mail setting table is missing - run migration 133_expense_dashboard_mail.sql"); }
    }
    private static String s(Object o) { return o == null || o.toString().isBlank() ? null : o.toString().trim(); }

    /** The setting for the admin screen; the password is never sent back (only whether one is saved). */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> get() {
        Authz.requireRole("ADMIN");
        Map<String, Object> r = row(), m = new LinkedHashMap<>();
        m.put("host", r == null ? null : r.get("host")); m.put("port", r == null ? 587 : r.get("port")); m.put("username", r == null ? null : r.get("username"));
        m.put("hasPassword", r != null && s(r.get("password")) != null); m.put("fromEmail", r == null ? null : r.get("from_email")); m.put("fromName", r == null ? null : r.get("from_name"));
        m.put("security", r == null ? "TLS" : r.get("security")); m.put("enabled", r != null && (Boolean.TRUE.equals(r.get("is_enabled")) || (r.get("is_enabled") instanceof Number n && n.intValue() == 1)));
        return m;
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void save(Map<String, Object> b) {
        Authz.requireRole("ADMIN"); row();
        String sec = String.valueOf(b.getOrDefault("security", "TLS")).toUpperCase();
        if (!List.of("TLS", "SSL", "NONE").contains(sec)) throw new IllegalArgumentException("Security must be TLS, SSL or NONE");
        int port; try { port = Integer.parseInt(String.valueOf(b.getOrDefault("port", 587)).trim()); } catch (NumberFormatException e) { throw new IllegalArgumentException("Port must be a number"); }
        boolean on = Boolean.TRUE.equals(b.get("enabled")) || "true".equalsIgnoreCase(String.valueOf(b.get("enabled")));
        String pw = s(b.get("password"));   // blank = keep the saved password
        jdbc.update("INSERT INTO smtp_setting (id, host, port, username, password, from_email, from_name, security, is_enabled, updated_by, updated_at) VALUES (1,?,?,?,?,?,?,?,?,?,NOW()) ON DUPLICATE KEY UPDATE host = VALUES(host), port = VALUES(port), username = VALUES(username), password = COALESCE(?, password), from_email = VALUES(from_email), from_name = VALUES(from_name), security = VALUES(security), is_enabled = VALUES(is_enabled), updated_by = VALUES(updated_by), updated_at = NOW()",
                s(b.get("host")), port, s(b.get("username")), pw, s(b.get("fromEmail")), s(b.get("fromName")), sec, on ? 1 : 0, TeamScope.me(), pw);
    }

    /** True when the company has switched its mail on and filled the server. */
    public boolean ready() { Map<String, Object> r = row(); return r != null && s(r.get("host")) != null && s(r.get("from_email")) != null && (Boolean.TRUE.equals(r.get("is_enabled")) || (r.get("is_enabled") instanceof Number n && n.intValue() == 1)); }

    /** Send one HTML mail through the company mail server; throws with the reason when it cannot. */
    public void send(String to, String subject, String html) throws Exception {
        Map<String, Object> r = row();
        if (r == null || s(r.get("host")) == null || s(r.get("from_email")) == null) throw new IllegalStateException("The mail server is not set up - fill it in Expense Setup > Email (SMTP)");
        JavaMailSenderImpl m = new JavaMailSenderImpl();
        m.setHost(s(r.get("host"))); m.setPort(((Number) r.get("port")).intValue()); m.setDefaultEncoding("UTF-8");
        if (s(r.get("username")) != null) { m.setUsername(s(r.get("username"))); m.setPassword(s(r.get("password"))); }
        Properties p = m.getJavaMailProperties(); String sec = String.valueOf(r.get("security"));
        p.put("mail.smtp.auth", s(r.get("username")) != null ? "true" : "false");
        if ("TLS".equals(sec)) p.put("mail.smtp.starttls.enable", "true"); else if ("SSL".equals(sec)) p.put("mail.smtp.ssl.enable", "true");
        p.put("mail.smtp.connectiontimeout", "10000"); p.put("mail.smtp.timeout", "15000"); p.put("mail.smtp.writetimeout", "15000");
        MimeMessage msg = m.createMimeMessage(); MimeMessageHelper h = new MimeMessageHelper(msg, "UTF-8");
        h.setFrom(s(r.get("from_email")), s(r.get("from_name")) == null ? s(r.get("from_email")) : s(r.get("from_name")));
        h.setTo(to); h.setSubject(subject); h.setText(html, true);
        m.send(msg);
    }

    /** Admin: send a test mail to check the setting. */
    public void test(String to) {
        Authz.requireRole("ADMIN");
        if (to == null || !to.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) throw new IllegalArgumentException("Enter a valid email address");
        try { send(to.trim(), "SFA test mail", "<p>This is a test mail from your SFA mail setting. It works.</p>"); }
        catch (IllegalStateException e) { throw e; }
        catch (Exception e) { throw new IllegalStateException("The mail could not be sent: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage())); }
    }
}
