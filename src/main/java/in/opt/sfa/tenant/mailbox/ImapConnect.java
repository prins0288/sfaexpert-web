package in.opt.sfa.tenant.mailbox;

import jakarta.mail.Store;

/** One place that opens an IMAP connection for an account: the host is checked again at connect time (SSRF), the session has timeouts, certificates are verified. */
final class ImapConnect {
    private ImapConnect() { }

    static Store open(MailAccountService accounts, MailAccountService.Account acc) throws Exception {
        HostGuard.check(acc.imap().host(), acc.imap().port(), accounts.internalHosts());
        Store st = MailSessions.imap(acc.imap()).getStore(MailSessions.protocol(acc.imap().security()));
        try { st.connect(acc.imap().host(), acc.imap().port(), acc.username(), acc.password()); return st; }
        catch (Exception e) { try { st.close(); } catch (Exception ignore) { /* not open */ } throw e; }
    }
}
