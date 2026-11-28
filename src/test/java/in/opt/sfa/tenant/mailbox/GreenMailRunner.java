package in.opt.sfa.tenant.mailbox;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

/** Starts a local test mail server (SMTP 3125, IMAP 3143) for trying the screens by hand: users u1@test.local / u2@test.local, password pw. Not used by the application. */
public class GreenMailRunner {
    public static void main(String[] a) throws Exception {
        GreenMail gm = new GreenMail(new ServerSetup[]{new ServerSetup(3125, "127.0.0.1", "smtp"), new ServerSetup(3143, "127.0.0.1", "imap")});
        gm.setUser("u1@test.local", "u1@test.local", "pw"); gm.setUser("u2@test.local", "u2@test.local", "pw");
        gm.start(); System.out.println("GreenMail up"); Thread.currentThread().join();
    }
}
