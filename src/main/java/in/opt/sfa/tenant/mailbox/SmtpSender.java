package in.opt.sfa.tenant.mailbox;

import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import org.eclipse.angus.mail.smtp.SMTPSendFailedException;

/**
 * One SMTP delivery with an honest result. The caller must NEVER resend automatically when the result is UNKNOWN: the server may already have the mail.
 *  SENT       - the server accepted the mail.
 *  RETRY      - nothing was handed to the server (connect problem, or a temporary 4xx refusal): safe to try again later.
 *  REJECTED   - the server or the login refused it for good (wrong login, bad address, 5xx): do not retry, tell the user.
 *  UNKNOWN    - the connection broke while the mail was being handed over: it may or may not have gone.
 */
public final class SmtpSender {
    public enum Result { SENT, RETRY, REJECTED, UNKNOWN }
    public record Outcome(Result result, String message) { }

    private SmtpSender() { }

    public static Outcome send(MailSessions.Server server, String user, String password, MimeMessage msg) {
        Session session = MailSessions.smtp(server, user != null);
        Transport t = null;
        try {
            t = session.getTransport("smtp");
            try { t.connect(server.host(), server.port(), user, password); }
            catch (AuthenticationFailedException e) { return new Outcome(Result.REJECTED, "The mail server refused the login. Check the user name and password."); }
            catch (MessagingException e) { return new Outcome(Result.RETRY, MailAccountService.friendly(e)); }
            try { t.sendMessage(msg, msg.getAllRecipients()); return new Outcome(Result.SENT, null); }
            catch (SMTPSendFailedException e) { int rc = e.getReturnCode(); return new Outcome(rc >= 400 && rc < 500 ? Result.RETRY : Result.REJECTED, "The server said: " + clean(e.getMessage())); }
            catch (SendFailedException e) { return new Outcome(Result.REJECTED, "The server refused the mail: " + clean(e.getMessage())); }
            catch (MessagingException e) { return new Outcome(Result.UNKNOWN, "The connection broke while sending - it is not known if the mail went. Check your Sent folder before sending again."); }
        } catch (Exception e) { return new Outcome(Result.UNKNOWN, "The mail could not be handed over cleanly - check your Sent folder before sending again."); }
        finally { if (t != null) try { t.close(); } catch (MessagingException ignore) { /* closing */ } }
    }

    private static String clean(String m) { return m == null ? "" : m.replaceAll("[\\r\\n]+", " ").trim().substring(0, Math.min(200, m.replaceAll("[\\r\\n]+", " ").trim().length())); }
}
