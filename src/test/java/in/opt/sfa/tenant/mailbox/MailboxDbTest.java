package in.opt.sfa.tenant.mailbox;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import in.opt.sfa.exception.ForbiddenException;
import in.opt.sfa.security.UserContext;
import in.opt.sfa.storage.FileStorageService;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sync logic, the send queue and the ownership checks against a REAL database (a scratch MySQL schema that is created and dropped by the test) and a local GreenMail server.
 * Skipped when no MySQL is reachable: set SFA_TEST_DB_URL (default jdbc:mysql://localhost:3306/), SFA_TEST_DB_USER (root), SFA_TEST_DB_PASSWORD (empty).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MailboxDbTest {
    static final String URL = System.getenv().getOrDefault("SFA_TEST_DB_URL", "jdbc:mysql://localhost:3306/"), USER = System.getenv().getOrDefault("SFA_TEST_DB_USER", "root"), PASS = System.getenv().getOrDefault("SFA_TEST_DB_PASSWORD", "");
    static String schema; static JdbcTemplate jdbc; static GreenMail gm; static MailAccountService accounts; static ImapSyncService sync; static MailSendService send; static MailReadService read; static MailActionService actions;
    static final int SMTP = 3425, IMAP = 3443;
    static long accA, accB;

    @BeforeAll static void up() throws Exception {
        try (Connection c = DriverManager.getConnection(URL, USER, PASS)) { schema = "sfa_mailtest_" + System.currentTimeMillis(); c.createStatement().execute("CREATE DATABASE " + schema + " CHARACTER SET utf8mb4"); }
        catch (Exception e) { Assumptions.abort("No MySQL for the DB tests: " + e.getMessage()); }
        DriverManagerDataSource ds = new DriverManagerDataSource(URL + schema + "?useSSL=false&allowPublicKeyRetrieval=true", USER, PASS); jdbc = new JdbcTemplate(ds);
        String all = Files.readString(Path.of("src/main/resources/sql/134_mailbox_core.sql")); String block = all.substring(all.indexOf("USE acme_db;") + 12, all.indexOf("USE globex_db;")).replace("\r", "");
        try (Connection c = ds.getConnection()) { ScriptUtils.executeSqlScript(c, new org.springframework.core.io.ByteArrayResource(block.getBytes(StandardCharsets.UTF_8))); }
        jdbc.update("UPDATE mail_admin_setting SET internal_hosts = '127.0.0.1', initial_sync_days = 30");
        gm = new GreenMail(new ServerSetup[]{new ServerSetup(SMTP, "127.0.0.1", "smtp"), new ServerSetup(IMAP, "127.0.0.1", "imap")}); gm.setUser("a@t.local", "a@t.local", "pw"); gm.setUser("b@t.local", "b@t.local", "pw"); gm.start();
        RateLimiter rl = new RateLimiter(); accounts = new MailAccountService(jdbc, rl); sync = new ImapSyncService(jdbc, accounts, rl, null);
        Path tmp = Files.createTempDirectory("mailtest"); send = new MailSendService(jdbc, accounts, new MailAttachmentStore(new FileStorageService(tmp.toString())), rl, null);
        read = new MailReadService(jdbc, accounts, new MailBodyService(jdbc, accounts)); actions = new MailActionService(jdbc, accounts);
        accA = addAccount("EMP-A", "a@t.local"); accB = addAccount("EMP-B", "b@t.local");
    }
    @AfterAll static void down() throws Exception { if (gm != null) gm.stop(); if (schema != null) try (Connection c = DriverManager.getConnection(URL, USER, PASS)) { c.createStatement().execute("DROP DATABASE " + schema); } UserContext.clear(); }
    @AfterEach void clear() { UserContext.clear(); }

    static long addAccount(String emp, String mail) {
        jdbc.update("INSERT INTO mail_account (emp_id, email, username, password, smtp_host, smtp_port, smtp_security, imap_host, imap_port, imap_security) VALUES (?,?,?,?,?,?,?,?,?,?)", emp, mail, mail, "pw", "127.0.0.1", SMTP, "NONE", "127.0.0.1", IMAP, "NONE");
        return jdbc.queryForObject("SELECT id FROM mail_account WHERE emp_id = ?", Long.class, emp);
    }
    static void as(String emp) { UserContext.set(new UserContext.CurrentUser(emp, "USER", emp, "t", null, null)); }
    static void deliver(String to, String subject) { GreenMailUtil.sendTextEmail(to, "x@t.local", subject, "body of " + subject, new ServerSetup(SMTP, "127.0.0.1", "smtp")); }
    static MailAccountService.Account acc(long id) { return accounts.loadById(id).orElseThrow(); }
    static int count(long accId) { return jdbc.queryForObject("SELECT COUNT(*) FROM mail_message WHERE account_id = ?", Integer.class, accId); }

    // ------------------------------------------------------------------ sync
    @Test @Order(1) void firstSyncFetchesTheMailsAndLaterSyncsOnlyTheNewOnes() {
        deliver("a@t.local", "m1"); deliver("a@t.local", "m2");
        ImapSyncService.Result r1 = sync.sync(acc(accA)); assertNull(r1.error()); assertEquals(2, r1.newMails()); assertEquals(2, count(accA));
        assertEquals(0, sync.sync(acc(accA)).newMails());   // nothing new: nothing fetched again, no duplicates
        deliver("a@t.local", "m3"); assertEquals(1, sync.sync(acc(accA)).newMails()); assertEquals(3, count(accA));
        assertEquals(3, jdbc.queryForObject("SELECT total_count FROM mail_folder WHERE account_id = ? AND role = 'INBOX'", Integer.class, accA));
        assertEquals("IDLE", jdbc.queryForObject("SELECT sync_state FROM mail_account WHERE id = ?", String.class, accA));
    }

    @Test @Order(2) void changesAndDeletionsOnTheServerAreCopied() throws Exception {
        try (Store st = Session.getInstance(new Properties()).getStore("imap")) {
            st.connect("127.0.0.1", IMAP, "a@t.local", "pw"); Folder f = st.getFolder("INBOX"); f.open(Folder.READ_WRITE);
            Message[] ms = f.getMessages(); ms[0].setFlag(Flags.Flag.SEEN, true); ms[1].setFlag(Flags.Flag.FLAGGED, true); ms[2].setFlag(Flags.Flag.DELETED, true); f.close(true);
        }
        sync.sync(acc(accA));
        assertEquals(2, count(accA)); assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mail_message WHERE account_id = ? AND is_read = 1", Integer.class, accA)); assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM mail_message WHERE account_id = ? AND is_starred = 1", Integer.class, accA));
    }

    @Test @Order(3) void aChangedUidValidityRebuildsTheFolder() {
        jdbc.update("UPDATE mail_folder SET uidvalidity = 1 WHERE account_id = ? AND role = 'INBOX'", accA);
        jdbc.update("INSERT INTO mail_message (account_id, folder_id, uid, subject, is_read) SELECT ?, id, 999, 'ghost from the old numbering', 0 FROM mail_folder WHERE account_id = ? AND role = 'INBOX'", accA, accA);
        sync.sync(acc(accA));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mail_message WHERE subject LIKE 'ghost%'", Integer.class)); assertEquals(2, count(accA));   // rebuilt from the server
    }

    @Test @Order(4) void twoSyncsOfOneAccountNeverRunTogether() {
        jdbc.update("UPDATE mail_account SET sync_state = 'RUNNING', sync_started_at = NOW() WHERE id = ?", accA);
        assertNotNull(sync.sync(acc(accA)).error());
        jdbc.update("UPDATE mail_account SET sync_started_at = NOW() - INTERVAL 20 MINUTE WHERE id = ?", accA);   // a dead lock is taken over
        assertNull(sync.sync(acc(accA)).error());
    }

    @Test @Order(5) void aWrongPasswordIsReportedAndDoesNotLeaveTheAccountLocked() {
        jdbc.update("UPDATE mail_account SET password = 'bad' WHERE id = ?", accB);
        ImapSyncService.Result r = sync.sync(acc(accB)); assertTrue(r.error().startsWith("Login failed"), r.error());
        assertEquals("IDLE", jdbc.queryForObject("SELECT sync_state FROM mail_account WHERE id = ?", String.class, accB)); jdbc.update("UPDATE mail_account SET password = 'pw' WHERE id = ?", accB);
    }

    // ------------------------------------------------------------------ queue
    private long queue(long accId, String subject, String to) {
        String mid = MimeBuilder.newMessageId("a@t.local");
        jdbc.update("INSERT INTO mail_outbox (account_id, status, message_id, to_addrs, subject, body_html, body_text, next_attempt_at) VALUES (?,'QUEUED',?,?,?,'<p>x</p>','x',NOW())", accId, mid, to, subject);
        return jdbc.queryForObject("SELECT id FROM mail_outbox WHERE message_id = ?", Long.class, mid);
    }

    @Test @Order(6) void aQueuedMailIsSentExactlyOnceEvenWhenTwoWorkersTryAtTheSameTime() throws Exception {
        long id = queue(accA, "once only", "b@t.local"); int before = gm.getReceivedMessages().length;
        ExecutorService ex = Executors.newFixedThreadPool(2); CyclicBarrier go = new CyclicBarrier(2); AtomicInteger took = new AtomicInteger();
        for (int i = 0; i < 2; i++) ex.submit(() -> { go.await(); if (send.processOne(id) != null) took.incrementAndGet(); return null; });
        ex.shutdown(); assertTrue(ex.awaitTermination(30, TimeUnit.SECONDS));
        assertEquals(1, took.get()); assertEquals(before + 1, gm.getReceivedMessages().length); assertEquals("SENT", jdbc.queryForObject("SELECT status FROM mail_outbox WHERE id = ?", String.class, id));
        assertNull(send.processOne(id));   // a sent mail is never taken again
    }

    @Test @Order(7) void aMailWhoseStateIsUnknownIsNeverResentByItself() {
        long stuck = queue(accA, "stuck while sending", "b@t.local");
        jdbc.update("UPDATE mail_outbox SET status = 'SENDING', claimed_at = NOW() - INTERVAL 30 MINUTE WHERE id = ?", stuck);
        int before = gm.getReceivedMessages().length; send.processTenant();
        assertEquals("UNKNOWN", jdbc.queryForObject("SELECT status FROM mail_outbox WHERE id = ?", String.class, stuck)); assertEquals(before, gm.getReceivedMessages().length);
        send.processTenant(); assertEquals("UNKNOWN", jdbc.queryForObject("SELECT status FROM mail_outbox WHERE id = ?", String.class, stuck));
    }

    @Test @Order(8) void aTemporaryFailureIsRetriedAFewTimesThenFails() throws Exception {
        jdbc.update("UPDATE mail_account SET smtp_port = 3999 WHERE id = ?", accA); long id = queue(accA, "server down", "b@t.local");
        assertEquals("QUEUED", send.processOne(id)); assertEquals(1, jdbc.queryForObject("SELECT attempts FROM mail_outbox WHERE id = ?", Integer.class, id));
        assertTrue(jdbc.queryForObject("SELECT next_attempt_at > NOW() FROM mail_outbox WHERE id = ?", Boolean.class, id));   // waits before the next try
        jdbc.update("UPDATE mail_outbox SET next_attempt_at = NOW() WHERE id = ?", id); assertEquals("QUEUED", send.processOne(id));
        jdbc.update("UPDATE mail_outbox SET next_attempt_at = NOW() WHERE id = ?", id); assertEquals("FAILED", send.processOne(id));
        assertNotNull(jdbc.queryForObject("SELECT last_error FROM mail_outbox WHERE id = ?", String.class, id)); jdbc.update("UPDATE mail_account SET smtp_port = ? WHERE id = ?", SMTP, accA);
    }

    @Test @Order(9) void aWrongSmtpLoginFailsAtOnceWithoutRetry() throws Exception {
        jdbc.update("UPDATE mail_account SET password = 'bad' WHERE id = ?", accA); long id = queue(accA, "bad login", "b@t.local");
        assertEquals("FAILED", send.processOne(id)); jdbc.update("UPDATE mail_account SET password = 'pw' WHERE id = ?", accA);
    }

    @Test @Order(10) void theSmtpHostIsCheckedAgainAtSendTime() throws Exception {
        jdbc.update("UPDATE mail_admin_setting SET internal_hosts = NULL"); long id = queue(accA, "internal host", "b@t.local");
        try { assertEquals("FAILED", send.processOne(id)); assertNotNull(jdbc.queryForObject("SELECT last_error FROM mail_outbox WHERE id = ?", String.class, id)); }
        finally { jdbc.update("UPDATE mail_admin_setting SET internal_hosts = '127.0.0.1'"); }
    }

    // ------------------------------------------------------------------ ownership
    @Test @Order(11) void nobodyCanReadChangeOrDownloadAnotherUsersMail() throws Exception {
        long mine = jdbc.queryForObject("SELECT id FROM mail_message WHERE account_id = ? ORDER BY id LIMIT 1", Long.class, accA); long folder = jdbc.queryForObject("SELECT folder_id FROM mail_message WHERE id = ?", Long.class, mine);
        as("EMP-B");
        assertThrows(ForbiddenException.class, () -> read.view(mine, false));
        assertThrows(ForbiddenException.class, () -> read.attachment(mine, 0));
        assertThrows(ForbiddenException.class, () -> read.list(folder, 0, 10, null, null, null, null, null, false, false, false));
        assertThrows(ForbiddenException.class, () -> actions.act(Map.of("ids", java.util.List.of(mine), "action", "DELETE")));
        assertThrows(ForbiddenException.class, () -> actions.prepare(mine, "REPLY"));
        assertThrows(ForbiddenException.class, () -> send.enqueue(Map.of("to", java.util.List.of("x@t.local"), "subject", "s", "html", "h", "replyTo", mine)));
        assertThrows(ForbiddenException.class, () -> send.enqueue(Map.of("to", java.util.List.of("x@t.local"), "subject", "s", "html", "h", "attachments", java.util.List.of(Map.of("path", "mailbox/EMP-A/secret.pdf", "name", "secret.pdf", "size", 1)))));
        long outbox = queue(accA, "mine", "b@t.local"); assertThrows(ForbiddenException.class, () -> send.resolve(outbox, "DISCARD"));
        as("EMP-A"); assertEquals(mine, ((Number) read.view(mine, false).get("id")).longValue());
    }
}
