package in.opt.sfa.tenant.mailbox;

import jakarta.mail.*;
import jakarta.mail.internet.MimeUtility;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/** The attachments of a message and one attachment fetched from the mail server (only when it is downloaded or forwarded). The part number is the position among the attachments of that message, the same everywhere. */
final class ImapAttachment {
    private ImapAttachment() { }

    record Loaded(String name, String type, byte[] data) { }

    /** A leaf part that is not the text of the mail itself: files and inline pictures. */
    static List<Part> attachments(Part root) throws Exception { List<Part> out = new ArrayList<>(); walk(root, out, null, null); return out; }

    /** Walks the message: fills the attachments and (when asked) the first plain text and html bodies. */
    static void walk(Part p, List<Part> att, StringBuilder[] text, StringBuilder[] html) throws Exception {
        if (p.isMimeType("multipart/*")) { Multipart mp = (Multipart) p.getContent(); for (int i = 0; i < mp.getCount(); i++) walk(mp.getBodyPart(i), att, text, html); return; }
        String disp = p.getDisposition(); boolean named = p.getFileName() != null;
        boolean body = (p.isMimeType("text/plain") || p.isMimeType("text/html")) && !Part.ATTACHMENT.equalsIgnoreCase(disp) && !named;
        if (body) {
            if (text != null && p.isMimeType("text/plain")) { if (text[0] == null) text[0] = new StringBuilder(); text[0].append(string(p)); }
            else if (html != null && p.isMimeType("text/html")) { if (html[0] == null) html[0] = new StringBuilder(); html[0].append(string(p)); }
        } else if (p.isMimeType("message/rfc822") && !named && !Part.ATTACHMENT.equalsIgnoreCase(disp)) { att.add(p); }
        else att.add(p);
    }

    static String string(Part p) throws Exception {
        try { Object c = p.getContent(); return c instanceof String s ? s : ""; }
        catch (java.io.UnsupportedEncodingException e) { try (java.io.InputStream in = p.getInputStream()) { return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1); } }
    }

    static String name(Part p, int index) throws Exception {
        String n = p.getFileName(); if (n == null) return p.isMimeType("message/rfc822") ? "message.eml" : "attachment-" + (index + 1);
        try { return MailAttachmentStore.safeName(MimeUtility.decodeText(n)); } catch (Exception e) { return MailAttachmentStore.safeName(n); }
    }
    static String type(Part p) { try { String t = p.getContentType(); int s = t.indexOf(';'); return (s > 0 ? t.substring(0, s) : t).trim().toLowerCase(); } catch (Exception e) { return "application/octet-stream"; } }
    static String contentId(Part p) throws Exception {
        String id = p instanceof jakarta.mail.internet.MimePart mp ? mp.getContentID() : null;   // an IMAP part knows it from the body structure
        if (id == null) { String[] h = p.getHeader("Content-ID"); id = h == null || h.length == 0 ? null : h[0]; }
        return id == null ? null : id.trim().replaceAll("^<|>$", "");
    }

    static byte[] read(Part p, long max) throws Exception {
        try (java.io.InputStream in = p.getInputStream(); ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192]; int n; long t = 0;
            while ((n = in.read(buf)) > 0) { t += n; if (t > max) throw new IllegalStateException("The attachment is bigger than the allowed size"); o.write(buf, 0, n); }
            return o.toByteArray();
        }
    }

    static MimeBuilder.Attachment fetch(MailAccountService accounts, MailAccountService.Account acc, long localMsgId, int part, org.springframework.jdbc.core.JdbcTemplate jdbc) throws Exception {
        Loaded l = load(accounts, acc, localMsgId, part, 50L * 1024 * 1024, jdbc);
        return new MimeBuilder.Attachment(l.name(), l.type(), l.data());
    }

    /** Opens the mail on the server, finds the part and reads it. The caller has already checked that the mail belongs to the account. */
    static Loaded load(MailAccountService accounts, MailAccountService.Account acc, long localMsgId, int part, long maxBytes, org.springframework.jdbc.core.JdbcTemplate jdbc) throws Exception {
        java.util.Map<String, Object> m = jdbc.queryForList("SELECT m.uid, f.path FROM mail_message m JOIN mail_folder f ON f.id = m.folder_id WHERE m.id = ? AND m.account_id = ?", localMsgId, acc.id()).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Mail not found"));
        try (Store st = ImapConnect.open(accounts, acc)) {
            Folder f = st.getFolder((String) m.get("path")); f.open(Folder.READ_ONLY);
            try {
                Message msg = ((UIDFolder) f).getMessageByUID(((Number) m.get("uid")).longValue()); if (msg == null) throw new IllegalStateException("This mail is no longer on the mail server");
                List<Part> parts = attachments(msg); if (part < 0 || part >= parts.size()) throw new IllegalArgumentException("Attachment not found");
                Part p = parts.get(part); return new Loaded(name(p, part), type(p), read(p, maxBytes));
            } finally { try { f.close(false); } catch (Exception ignore) { /* closing */ } }
        }
    }
}
