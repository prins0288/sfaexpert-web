package in.opt.sfa.tenant.mailbox;

import jakarta.mail.*;
import jakarta.mail.internet.MimeUtility;

import java.io.ByteArrayOutputStream;
import java.util.Map;

/** Fetches ONE attachment of a cached mail from the mail server (only when it is downloaded or forwarded). The part number is the position among the attachments of that message. */
final class ImapAttachment {
    private ImapAttachment() { }

    record Loaded(String name, String type, byte[] data) { }

    static MimeBuilder.Attachment fetch(MailAccountService accounts, MailAccountService.Account acc, long localMsgId, int part) throws Exception {
        Loaded l = load(accounts, acc, localMsgId, part, 50L * 1024 * 1024);
        return new MimeBuilder.Attachment(l.name(), l.type(), l.data());
    }

    static Loaded load(MailAccountService accounts, MailAccountService.Account acc, long localMsgId, int part, long maxBytes) throws Exception {
        throw new UnsupportedOperationException("filled in Phase 3 (IMAP sync)");
    }

    static void collect(Part p, java.util.List<Part> out) throws Exception {
        if (p.isMimeType("multipart/*")) { Multipart mp = (Multipart) p.getContent(); for (int i = 0; i < mp.getCount(); i++) collect(mp.getBodyPart(i), out); }
        else if (Part.ATTACHMENT.equalsIgnoreCase(p.getDisposition()) || (p.getFileName() != null && !Part.INLINE.equalsIgnoreCase(p.getDisposition()))) out.add(p);
    }
    static String name(Part p) throws Exception { String n = p.getFileName(); return n == null ? "attachment" : MimeUtility.decodeText(n); }
    static byte[] read(Part p, long max) throws Exception {
        try (java.io.InputStream in = p.getInputStream(); ByteArrayOutputStream o = new ByteArrayOutputStream()) { byte[] buf = new byte[8192]; int n; long t = 0; while ((n = in.read(buf)) > 0) { t += n; if (t > max) throw new IllegalStateException("The attachment is too big"); o.write(buf, 0, n); } return o.toByteArray(); }
    }
}
