package in.opt.sfa.tenant.mailbox;

import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class ReadSafetyTest {
    private static final String PNG = "data:image/png;base64,iVBORw0KGgo=";

    @Test void scriptsAndEventHandlersNeverSurvive() {
        String h = MailReadService.sanitizeForView("<p onclick=\"x()\">a<script>alert(1)</script><iframe src=\"https://e.com\"></iframe><object data=\"x\"></object><svg onload=\"x()\"></svg><style>body{display:none}</style><form action=\"x\"><input></form></p>", true);
        for (String bad : new String[]{"script", "onclick", "iframe", "object", "svg", "onload", "<style", "<form", "<input"}) assertFalse(h.toLowerCase().contains(bad), bad + " in " + h);
    }
    @Test void linksAreHttpOrMailtoOnly() {
        String h = MailReadService.sanitizeForView("<a href=\"javascript:alert(1)\">a</a><a href=\"data:text/html,<script>1</script>\">b</a><a href=\"vbscript:x\">c</a><a href=\"https://ok.com/p\">d</a><a href=\"mailto:x@y.com\">e</a>", false);
        assertFalse(h.contains("javascript:")); assertFalse(h.contains("data:text")); assertFalse(h.contains("vbscript")); assertTrue(h.contains("https://ok.com/p")); assertTrue(h.contains("mailto:x&#64;y.com")); assertTrue(h.contains("nofollow"));
    }
    @Test void remoteImagesBlockedByDefaultAndLocalPicturesKept() {
        String html = "<img src=\"https://tracker.example/p.gif\"><img src=\"" + PNG + "\"><img src=\"javascript:alert(1)\"><img src=\"//evil/x.png\">";
        String blocked = MailReadService.sanitizeForView(html, false);
        assertFalse(blocked.contains("tracker.example")); assertTrue(blocked.contains("data:image/png")); assertFalse(blocked.contains("javascript:")); assertFalse(blocked.contains("//evil"));
        String allowed = MailReadService.sanitizeForView(html, true);
        assertTrue(allowed.contains("tracker.example")); assertFalse(allowed.contains("javascript:")); assertFalse(allowed.contains("//evil"));
    }
    @Test void dataImageOfOtherTypesIsRefused() { assertFalse(MailReadService.sanitizeForView("<img src=\"data:text/html;base64,PHNjcmlwdD4=\"><img src=\"data:image/svg+xml;base64,PHN2Zz4=\">", true).contains("data:")); }

    @Test void attachmentsAreListedInAStableOrderAndTheBodyIsNotAnAttachment() throws Exception {
        MimeMessage m = new MimeMessage(Session.getInstance(new Properties()));
        MimeMultipart mixed = new MimeMultipart("mixed"), alt = new MimeMultipart("alternative");
        MimeBodyPart t = new MimeBodyPart(); t.setText("plain"); alt.addBodyPart(t);
        MimeBodyPart h = new MimeBodyPart(); h.setContent("<p>html</p>", "text/html; charset=UTF-8"); alt.addBodyPart(h);
        MimeBodyPart body = new MimeBodyPart(); body.setContent(alt); mixed.addBodyPart(body);
        MimeBodyPart a1 = new MimeBodyPart(); a1.setDataHandler(new jakarta.activation.DataHandler(new ByteArrayDataSource(new byte[]{1}, "application/pdf"))); a1.setFileName("a.pdf"); a1.setDisposition(Part.ATTACHMENT); mixed.addBodyPart(a1);
        MimeBodyPart a2 = new MimeBodyPart(); a2.setDataHandler(new jakarta.activation.DataHandler(new ByteArrayDataSource(new byte[]{2}, "image/png"))); a2.setHeader("Content-ID", "<pic>"); a2.setDisposition(Part.INLINE); mixed.addBodyPart(a2);
        m.setContent(mixed); m.saveChanges();
        List<Part> att = ImapAttachment.attachments(m);
        assertEquals(2, att.size()); assertEquals("a.pdf", ImapAttachment.name(att.get(0), 0)); assertEquals("pic", ImapAttachment.contentId(att.get(1))); assertEquals("image/png", ImapAttachment.type(att.get(1)));
        StringBuilder[] text = new StringBuilder[1], html = new StringBuilder[1]; ImapAttachment.walk(m, new java.util.ArrayList<>(), text, html);
        assertEquals("plain", text[0].toString()); assertTrue(html[0].toString().contains("<p>html</p>"));
    }
    @Test void attachmentNameCannotEscapeAFolder() throws Exception {
        MimeBodyPart p = new MimeBodyPart(); p.setFileName("../../etc/passwd"); assertEquals("passwd", ImapAttachment.name(p, 0));
    }
}
