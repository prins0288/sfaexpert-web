package in.opt.sfa.tenant.mailbox;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/** Real SMTP delivery to a local GreenMail server - nothing leaves this machine. */
class SmtpSenderTest {
    static GreenMail gm; static final MailSessions.Server SMTP = new MailSessions.Server("127.0.0.1", 3225, "NONE");
    @BeforeAll static void up() { gm = new GreenMail(new ServerSetup[]{new ServerSetup(3225, "127.0.0.1", "smtp"), new ServerSetup(3243, "127.0.0.1", "imap")}); gm.setUser("me@test.local", "me@test.local", "pw"); gm.setUser("u2@test.local", "u2@test.local", "pw"); gm.start(); }
    @AfterAll static void down() { gm.stop(); }

    private static MimeMessage msg(String mid, List<MimeBuilder.Attachment> a) throws Exception {
        return MimeBuilder.build(Session.getInstance(new Properties()), new MimeBuilder.Out("me@test.local", "Me", List.of("u2@test.local"), List.of(), List.of(), "Test " + mid, "<p>Hello <b>world</b></p>", "Hello world", mid, null, null, a));
    }

    @Test void sentMailArrivesWithItsMessageIdAndAttachment() throws Exception {
        String mid = MimeBuilder.newMessageId("me@test.local");
        SmtpSender.Outcome o = SmtpSender.send(SMTP, "me@test.local", "pw", msg(mid, List.of(new MimeBuilder.Attachment("a.txt", "text/plain", "data".getBytes()))));
        assertEquals(SmtpSender.Result.SENT, o.result());
        MimeMessage[] got = gm.getReceivedMessagesForDomain("test.local");
        MimeMessage mine = java.util.Arrays.stream(got).filter(m -> { try { return mid.equals(m.getMessageID()); } catch (Exception e) { return false; } }).findFirst().orElseThrow();
        assertTrue(mine.isMimeType("multipart/*")); assertEquals("Test " + mid, mine.getSubject());
    }
    @Test void wrongPasswordIsRejectedNotRetried() throws Exception {
        assertEquals(SmtpSender.Result.REJECTED, SmtpSender.send(SMTP, "me@test.local", "WRONG", msg("<w@x>", List.of())).result());
    }
    @Test void serverDownIsARetryBecauseNothingWasHandedOver() throws Exception {
        assertEquals(SmtpSender.Result.RETRY, SmtpSender.send(new MailSessions.Server("127.0.0.1", 3999, "NONE"), "a", "b", msg("<d@x>", List.of())).result());
    }
}
