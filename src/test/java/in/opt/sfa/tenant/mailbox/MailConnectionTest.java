package in.opt.sfa.tenant.mailbox;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** SMTP and IMAP login against a local GreenMail server - nothing leaves this machine. */
class MailConnectionTest {
    static GreenMail gm;
    @BeforeAll static void up() { gm = new GreenMail(new ServerSetup[]{new ServerSetup(3325, "127.0.0.1", "smtp"), new ServerSetup(3343, "127.0.0.1", "imap")}); gm.setUser("a@test.local", "a@test.local", "pw1"); gm.start(); }
    @AfterAll static void down() { gm.stop(); }

    @Test void smtpAndImapLoginWork() throws Exception {
        MailAccountService.smtpLogin(new MailSessions.Server("127.0.0.1", 3325, "NONE"), "a@test.local", "pw1");
        assertTrue(MailAccountService.imapLogin(new MailSessions.Server("127.0.0.1", 3343, "NONE"), "a@test.local", "pw1") >= 1);
    }
    @Test void wrongPasswordGivesAClearMessage() {
        Exception e = assertThrows(Exception.class, () -> MailAccountService.imapLogin(new MailSessions.Server("127.0.0.1", 3343, "NONE"), "a@test.local", "bad"));
        String m = MailAccountService.friendly(e);
        assertTrue(m.startsWith("Login failed"), m); assertFalse(m.contains("bad"));
    }
    @Test void closedPortGivesAClearMessage() {
        Exception e = assertThrows(Exception.class, () -> MailAccountService.imapLogin(new MailSessions.Server("127.0.0.1", 3999, "NONE"), "a", "b"));
        assertTrue(MailAccountService.friendly(e).startsWith("Cannot connect"), MailAccountService.friendly(e));
    }
}
