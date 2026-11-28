package in.opt.sfa.tenant.mailbox;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class MimeBuilderTest {
    private static MimeBuilder.Out out(String subject, String mid, List<MimeBuilder.Attachment> a) {
        return new MimeBuilder.Out("me@test.local", "Me", List.of("u2@test.local"), List.of(), List.of("hidden@test.local"), subject, "<p>Hello</p>", "Hello", mid, "<orig@x>", "<a@x> <orig@x>", a);
    }
    private static MimeMessage build(MimeBuilder.Out o) throws Exception { return MimeBuilder.build(Session.getInstance(new Properties()), o); }

    @Test void messageIdIsKeptAndThreadingHeadersAreSet() throws Exception {
        String mid = MimeBuilder.newMessageId("me@test.local");
        MimeMessage m = build(out("Hi", mid, List.of()));
        assertEquals(mid, m.getMessageID()); assertEquals("<orig@x>", m.getHeader("In-Reply-To", null)); assertTrue(m.getHeader("References", null).contains("<a@x>"));
        assertEquals(mid, build(out("Hi", mid, List.of())).getMessageID());   // the same on a second build: a retry keeps the identity
    }
    @Test void headerInjectionIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> build(out("Hi\r\nBcc: evil@x.com", "<m@x>", List.of())));
        assertThrows(IllegalArgumentException.class, () -> MimeBuilder.addresses(List.of("a@b.com\r\nBcc: evil@x.com"), "To"));
        assertThrows(IllegalArgumentException.class, () -> MimeBuilder.addresses(List.of("not an address"), "To"));
    }
    @Test void addressesAreCleaned() { assertEquals(List.of("x@y.com", "z@y.com"), MimeBuilder.addresses(List.of("x@y.com, z@y.com"), "To")); }
    @Test void attachmentNameIsMadeSafe() throws Exception {
        MimeMessage m = build(out("Hi", "<m@x>", List.of(new MimeBuilder.Attachment("..\\..\\evil\"\r\n.pdf", "application/pdf", new byte[]{1, 2, 3}))));
        String raw = MimeBuilder.utf8(MimeBuilder.bytes(m));
        assertFalse(raw.contains("..\\"), raw); assertFalse(raw.contains("Content-Disposition: attachment; filename=\"..")); assertTrue(raw.contains("evil"));
        assertEquals("evil___.pdf", MailAttachmentStore.safeName("..\\..\\evil\"\r\n.pdf"));
    }
    @Test void safeNameRules() {
        assertEquals("file", MailAttachmentStore.safeName(null)); assertEquals("a.txt", MailAttachmentStore.safeName("/etc/../a.txt")); assertEquals("hidden", MailAttachmentStore.safeName(".hidden"));
        assertTrue(MailAttachmentStore.safeName("x".repeat(300) + ".pdf").length() <= 120); assertTrue(MailAttachmentStore.safeName("x".repeat(300) + ".pdf").endsWith(".pdf"));
    }
    @Test void htmlToText() { assertEquals("Hi\nthere", MimeBuilder.htmlToText("<p>Hi</p><script>x()</script><div>there</div>")); }
    @Test void composeHtmlIsSanitised() {
        String h = MailSendService.COMPOSE_POLICY.sanitize("<p onclick=\"x()\">a<script>alert(1)</script><a href=\"javascript:alert(1)\">l</a><img src=\"https://x/y.png\" onerror=\"x()\"></p>");
        assertFalse(h.contains("script")); assertFalse(h.contains("onclick")); assertFalse(h.contains("javascript:")); assertFalse(h.contains("onerror"));
    }
}
