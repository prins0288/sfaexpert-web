package in.opt.sfa.tenant.mailbox;

import jakarta.activation.DataHandler;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.*;
import jakarta.mail.util.ByteArrayDataSource;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the MIME message of one mail. The Message-ID is generated ONCE when the mail is queued and is never changed by the server-side library, so a retry sends the same mail identity.
 * Addresses are parsed strictly and every header value is checked for line breaks (header injection); file names are made safe.
 */
public final class MimeBuilder {
    private MimeBuilder() { }

    public record Attachment(String name, String type, byte[] data) { }
    public record Out(String fromEmail, String fromName, List<String> to, List<String> cc, List<String> bcc, String subject, String html, String text, String messageId, String inReplyTo, String refs, List<Attachment> attachments) { }

    /** A MimeMessage that keeps the Message-ID we generated. */
    private static final class Fixed extends MimeMessage {
        private final String id;
        Fixed(Session s, String id) { super(s); this.id = id; }
        @Override protected void updateMessageID() throws MessagingException { setHeader("Message-ID", id); }
    }

    public static String newMessageId(String fromEmail) {
        String domain = fromEmail != null && fromEmail.contains("@") ? fromEmail.substring(fromEmail.indexOf('@') + 1) : "localhost";
        domain = domain.replaceAll("[^A-Za-z0-9.-]", "");
        return "<" + java.util.UUID.randomUUID() + "@" + (domain.isEmpty() ? "localhost" : domain) + ">";
    }

    /** Every address must be a plain valid address (no groups, no line breaks). Returns the cleaned list. */
    public static List<String> addresses(List<String> raw, String what) {
        List<String> out = new ArrayList<>();
        if (raw == null) return out;
        for (String r : raw) {
            if (r == null || r.isBlank()) continue;
            noCrlf(r, what);
            try { InternetAddress[] p = InternetAddress.parse(r.trim(), true); for (InternetAddress a : p) { a.validate(); if (a.isGroup() || a.getAddress() == null || !a.getAddress().contains("@")) throw new AddressException("group"); out.add(a.toString()); } }
            catch (AddressException e) { throw new IllegalArgumentException("The address is not valid: " + r.trim()); }
        }
        return out;
    }

    public static void noCrlf(String v, String what) { if (v != null && (v.indexOf('\r') >= 0 || v.indexOf('\n') >= 0 || v.indexOf('\0') >= 0)) throw new IllegalArgumentException(what + " has an invalid character"); }

    public static MimeMessage build(Session session, Out o) throws Exception {
        noCrlf(o.subject(), "Subject"); noCrlf(o.messageId(), "Message-ID"); noCrlf(o.inReplyTo(), "In-Reply-To"); noCrlf(o.refs() == null ? null : o.refs().replaceAll("[\\r\\n]+", " "), "References");
        MimeMessage m = new Fixed(session, o.messageId());
        m.setFrom(new InternetAddress(o.fromEmail(), o.fromName() == null ? null : o.fromName().replaceAll("[\\p{Cntrl}]", " "), "UTF-8"));
        m.setRecipients(Message.RecipientType.TO, toArray(o.to())); m.setRecipients(Message.RecipientType.CC, toArray(o.cc())); m.setRecipients(Message.RecipientType.BCC, toArray(o.bcc()));
        m.setSubject(o.subject() == null ? "" : o.subject(), "UTF-8");
        m.setSentDate(new java.util.Date());
        if (o.inReplyTo() != null && !o.inReplyTo().isBlank()) m.setHeader("In-Reply-To", o.inReplyTo());
        if (o.refs() != null && !o.refs().isBlank()) m.setHeader("References", MimeUtility.fold(12, o.refs().trim().replaceAll("\\s+", " ")));
        m.setHeader("X-Mailer", "SFA Mail");

        MimeMultipart alt = new MimeMultipart("alternative");
        MimeBodyPart text = new MimeBodyPart(); text.setText(o.text() == null ? "" : o.text(), "UTF-8"); alt.addBodyPart(text);
        if (o.html() != null && !o.html().isBlank()) { MimeBodyPart html = new MimeBodyPart(); html.setContent(o.html(), "text/html; charset=UTF-8"); alt.addBodyPart(html); }
        if (o.attachments() == null || o.attachments().isEmpty()) { m.setContent(alt); }
        else {
            MimeMultipart mixed = new MimeMultipart("mixed"); MimeBodyPart body = new MimeBodyPart(); body.setContent(alt); mixed.addBodyPart(body);
            for (Attachment a : o.attachments()) {
                MimeBodyPart p = new MimeBodyPart(); String name = MailAttachmentStore.safeName(a.name());
                p.setDataHandler(new DataHandler(new ByteArrayDataSource(a.data(), a.type() == null || a.type().isBlank() ? "application/octet-stream" : a.type())));
                p.setFileName(MimeUtility.encodeText(name, "UTF-8", "B")); p.setDisposition(jakarta.mail.Part.ATTACHMENT); mixed.addBodyPart(p);
            }
            m.setContent(mixed);
        }
        m.saveChanges(); m.setHeader("Message-ID", o.messageId());
        return m;
    }

    private static InternetAddress[] toArray(List<String> l) throws AddressException {
        if (l == null || l.isEmpty()) return new InternetAddress[0];
        List<InternetAddress> out = new ArrayList<>(); for (String s : l) out.addAll(List.of(InternetAddress.parse(s, true))); return out.toArray(new InternetAddress[0]);
    }

    /** A readable text version of the HTML body (for the text/plain part and the list snippet). */
    public static String htmlToText(String html) {
        if (html == null) return "";
        String t = html.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ").replaceAll("(?i)<br\\s*/?>|</p>|</div>|</li>|</h[1-6]>", "\n").replaceAll("(?s)<[^>]+>", "");
        t = t.replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'").replace("&amp;", "&");
        return t.replaceAll("[ \\t]+\\n", "\n").replaceAll("\\n[ \\t]+", "\n").replaceAll("\\n{3,}", "\n\n").trim();
    }

    public static byte[] bytes(MimeMessage m) throws Exception { java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream(); m.writeTo(b); return b.toByteArray(); }
    public static String utf8(byte[] b) { return new String(b, StandardCharsets.UTF_8); }
}
