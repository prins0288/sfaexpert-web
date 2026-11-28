package in.opt.sfa.tenant.mailbox;

import jakarta.mail.Session;

import java.util.Properties;

/** Builds a JavaMail session for ONE account (never a global one). Certificates are always verified; every connection has a timeout. */
public final class MailSessions {
    private static final String T = "15000";

    private MailSessions() { }

    public record Server(String host, int port, String security) { }

    public static Session smtp(Server s, boolean auth) {
        Properties p = new Properties();
        p.put("mail.smtp.host", s.host()); p.put("mail.smtp.port", String.valueOf(s.port())); p.put("mail.smtp.auth", String.valueOf(auth));
        p.put("mail.smtp.connectiontimeout", T); p.put("mail.smtp.timeout", T); p.put("mail.smtp.writetimeout", T);
        if ("SSL".equals(s.security())) { p.put("mail.smtp.ssl.enable", "true"); p.put("mail.smtp.ssl.checkserveridentity", "true"); }
        else if ("STARTTLS".equals(s.security())) { p.put("mail.smtp.starttls.enable", "true"); p.put("mail.smtp.starttls.required", "true"); p.put("mail.smtp.ssl.checkserveridentity", "true"); }
        return Session.getInstance(p);
    }

    public static Session imap(Server s) {
        Properties p = new Properties(); String proto = "SSL".equals(s.security()) ? "imaps" : "imap";
        p.put("mail.store.protocol", proto);
        p.put("mail." + proto + ".host", s.host()); p.put("mail." + proto + ".port", String.valueOf(s.port()));
        p.put("mail." + proto + ".connectiontimeout", T); p.put("mail." + proto + ".timeout", T); p.put("mail." + proto + ".writetimeout", T);
        p.put("mail." + proto + ".peek", "true");   // reading a body must not mark it as read on the server
        if ("SSL".equals(s.security())) { p.put("mail.imaps.ssl.enable", "true"); p.put("mail.imaps.ssl.checkserveridentity", "true"); }
        else if ("STARTTLS".equals(s.security())) { p.put("mail.imap.starttls.enable", "true"); p.put("mail.imap.starttls.required", "true"); p.put("mail.imap.ssl.checkserveridentity", "true"); }
        return Session.getInstance(p);
    }

    public static String protocol(String security) { return "SSL".equals(security) ? "imaps" : "imap"; }
}
