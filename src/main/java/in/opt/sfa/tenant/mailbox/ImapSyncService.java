package in.opt.sfa.tenant.mailbox;

import in.opt.sfa.exception.ForbiddenException;
import in.opt.sfa.tenant.team.TeamScope;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeUtility;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.ReceivedDateTerm;
import org.eclipse.angus.mail.imap.IMAPFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;

/**
 * IMAP -> cache. Only NEW mails are fetched (per folder: UIDVALIDITY + last UID); the first sync is limited to the last N days and older mails come with "Load more".
 * Changes made on the server (read, star, deleted) are copied into the cache. A changed UIDVALIDITY means the server renumbered the folder: the cache of that folder is rebuilt.
 * One account is never synced twice at the same time (atomic lock in the table; a lock older than 15 minutes is taken over).
 */
@Service
public class ImapSyncService {
    private static final Logger log = LoggerFactory.getLogger(ImapSyncService.class);
    private static final int BATCH = 200, MORE = 50;

    private final JdbcTemplate jdbc;
    private final MailAccountService accounts;
    private final RateLimiter limiter;
    private final TenantRunner tenants;

    public ImapSyncService(JdbcTemplate jdbc, MailAccountService accounts, RateLimiter limiter, TenantRunner tenants) { this.jdbc = jdbc; this.accounts = accounts; this.limiter = limiter; this.tenants = tenants; }

    public record Result(int newMails, int folders, String error) { }

    // ------------------------------------------------------------------ entry points
    /** The refresh button: my account now (not more than once in 15 seconds). */
    public Result syncMine() {
        accounts.requireExternal(); String me = TeamScope.me(); if (me == null) throw new ForbiddenException("Sign in again");
        limiter.check("mailsync:" + me, 1, 15_000, "Wait a few seconds before refreshing again");
        MailAccountService.Account acc = accounts.load(me).orElseThrow(() -> new IllegalStateException("Set up your mail account first (Mail Settings)"));
        return sync(acc);
    }

    /** The scheduler: every tenant, the accounts that are due - one after the other, inactive users skipped. */
    public void syncDueAllTenants() { tenants.forEachTenant(t -> syncDue()); }

    void syncDue() {
        if (!accounts.externalOn()) return;
        int interval = accounts.syncIntervalMin();
        List<Long> due = jdbc.queryForList("SELECT id FROM mail_account WHERE is_enabled = 1 AND password IS NOT NULL AND (last_sync_at IS NULL OR last_sync_at < NOW() - INTERVAL ? MINUTE) AND (last_seen_at IS NULL OR last_seen_at > NOW() - INTERVAL 3 DAY) AND sync_state = 'IDLE' ORDER BY COALESCE(last_sync_at, '2000-01-01') LIMIT 25", Long.class, interval);
        for (Long id : due) { try { accounts.loadById(id).ifPresent(this::sync); } catch (Exception e) { log.warn("Mail sync of account {} failed: {}", id, e.getMessage()); } }
    }

    // ------------------------------------------------------------------ one account
    public Result sync(MailAccountService.Account acc) {
        int locked = jdbc.update("UPDATE mail_account SET sync_state = 'RUNNING', sync_started_at = NOW() WHERE id = ? AND (sync_state = 'IDLE' OR sync_started_at < NOW() - INTERVAL 15 MINUTE)", acc.id());
        if (locked == 0) return new Result(0, 0, "A sync of this account is already running");
        String error = null; int fresh = 0, nf = 0;
        try (Store st = ImapConnect.open(accounts, acc)) {
            List<ImapFolders.Info> found = ImapFolders.discover(st); nf = found.size();
            Set<String> paths = new HashSet<>();
            for (ImapFolders.Info i : found) {
                paths.add(i.path());
                jdbc.update("INSERT INTO mail_folder (account_id, name, path, role, sort_order) VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE name = VALUES(name), role = VALUES(role), sort_order = VALUES(sort_order)", acc.id(), i.name(), i.path(), i.role(), i.order());
            }
            for (Map<String, Object> f : jdbc.queryForList("SELECT id, path FROM mail_folder WHERE account_id = ?", acc.id())) if (!paths.contains(f.get("path"))) jdbc.update("DELETE FROM mail_folder WHERE id = ?", f.get("id"));   // removed on the server
            for (Map<String, Object> f : jdbc.queryForList("SELECT * FROM mail_folder WHERE account_id = ? ORDER BY sort_order, id", acc.id())) {
                try { fresh += syncFolder(st, acc, f); } catch (Exception e) { log.warn("Mail sync: folder {} of account {} failed: {}", f.get("path"), acc.id(), e.getMessage()); }
            }
        } catch (Exception e) { error = MailAccountService.friendly(e); }
        finally { jdbc.update("UPDATE mail_account SET sync_state = 'IDLE', last_sync_at = CASE WHEN ? IS NULL THEN NOW() ELSE last_sync_at END, last_sync_error = ? WHERE id = ?", error, error == null ? null : error.substring(0, Math.min(290, error.length())), acc.id()); }
        return new Result(fresh, nf, error);
    }

    private int syncFolder(Store st, MailAccountService.Account acc, Map<String, Object> f) throws Exception {
        long folderId = ((Number) f.get("id")).longValue(); String path = (String) f.get("path");
        Folder folder = st.getFolder(path); if (!(folder instanceof UIDFolder uf)) return 0;
        folder.open(Folder.READ_ONLY);
        try {
            long validity = uf.getUIDValidity(); Long stored = f.get("uidvalidity") == null ? null : ((Number) f.get("uidvalidity")).longValue();
            long lastUid = ((Number) f.get("last_uid")).longValue(); Long lowest = f.get("lowest_uid") == null ? null : ((Number) f.get("lowest_uid")).longValue();
            if (stored != null && stored != validity) {   // the server renumbered the folder: everything cached for it is wrong
                log.info("Mail sync: UIDVALIDITY of {} changed ({} -> {}), re-sync", path, stored, validity);
                jdbc.update("DELETE FROM mail_message WHERE folder_id = ?", folderId); lastUid = 0; lowest = null;
            }
            jdbc.update("UPDATE mail_folder SET uidvalidity = ? WHERE id = ?", validity, folderId);
            int fresh = 0;
            if (folder.getMessageCount() > 0) {
                Message[] news;
                if (lastUid == 0) {   // the first sync: only the last N days
                    Date since = new Date(System.currentTimeMillis() - accounts.initialSyncDays() * 86_400_000L);
                    news = folder.search(new ReceivedDateTerm(ComparisonTerm.GE, since));
                } else {
                    Message[] r = uf.getMessagesByUID(lastUid + 1, UIDFolder.LASTUID);   // an empty range still returns the last mail: filtered below
                    news = r;
                }
                List<Message> fresh1 = new ArrayList<>();
                for (Message m : news) { long uid = uf.getUID(m); if (uid > lastUid) fresh1.add(m); }
                for (int i = 0; i < fresh1.size(); i += BATCH) {
                    List<Message> part = fresh1.subList(i, Math.min(fresh1.size(), i + BATCH)); fetchHeaders(folder, part);
                    for (Message m : part) {
                        long uid = uf.getUID(m); if (m.isSet(Flags.Flag.DELETED)) continue;
                        fresh += insert(acc.id(), folderId, uid, m, "SENT".equals(f.get("role")) || "DRAFTS".equals(f.get("role")));
                        lastUid = Math.max(lastUid, uid); lowest = lowest == null ? uid : Math.min(lowest, uid);
                    }
                }
            }
            jdbc.update("UPDATE mail_folder SET last_uid = ?, lowest_uid = ? WHERE id = ?", lastUid, lowest, folderId);
            syncChanges(folder, uf, folderId);
            recount(folderId);
            return fresh;
        } finally { try { folder.close(false); } catch (Exception ignore) { /* closing */ } }
    }

    /** Copy the read / star / deleted state of the cached mails from the server (and drop the mails that are gone). */
    private void syncChanges(Folder folder, UIDFolder uf, long folderId) throws Exception {
        List<Map<String, Object>> cached = jdbc.queryForList("SELECT id, uid, is_read, is_starred FROM mail_message WHERE folder_id = ? ORDER BY uid DESC LIMIT 3000", folderId);
        if (cached.isEmpty()) return;
        long lo = cached.stream().mapToLong(r -> ((Number) r.get("uid")).longValue()).min().getAsLong(), hi = cached.stream().mapToLong(r -> ((Number) r.get("uid")).longValue()).max().getAsLong();
        Message[] onServer = uf.getMessagesByUID(lo, hi); FetchProfile fp = new FetchProfile(); fp.add(FetchProfile.Item.FLAGS); fp.add(UIDFolder.FetchProfileItem.UID); folder.fetch(onServer, fp);
        Map<Long, Message> byUid = new HashMap<>(); for (Message m : onServer) if (m != null) byUid.put(uf.getUID(m), m);
        for (Map<String, Object> r : cached) {
            long uid = ((Number) r.get("uid")).longValue(), id = ((Number) r.get("id")).longValue(); Message m = byUid.get(uid);
            if (m == null || m.isSet(Flags.Flag.DELETED)) { jdbc.update("DELETE FROM mail_message WHERE id = ?", id); continue; }
            boolean seen = m.isSet(Flags.Flag.SEEN), star = m.isSet(Flags.Flag.FLAGGED);
            boolean dbSeen = flag(r.get("is_read")), dbStar = flag(r.get("is_starred"));
            if (seen != dbSeen || star != dbStar) jdbc.update("UPDATE mail_message SET is_read = ?, is_starred = ? WHERE id = ?", seen ? 1 : 0, star ? 1 : 0, id);
        }
    }

    private static boolean flag(Object o) { return Boolean.TRUE.equals(o) || (o instanceof Number n && n.intValue() == 1); }

    private void recount(long folderId) { jdbc.update("UPDATE mail_folder SET unread_count = (SELECT COUNT(*) FROM mail_message WHERE folder_id = ? AND is_read = 0), total_count = (SELECT COUNT(*) FROM mail_message WHERE folder_id = ?) WHERE id = ?", folderId, folderId, folderId); }

    private static void fetchHeaders(Folder folder, List<Message> part) throws MessagingException {
        FetchProfile fp = new FetchProfile(); fp.add(FetchProfile.Item.ENVELOPE); fp.add(FetchProfile.Item.FLAGS); fp.add(FetchProfile.Item.CONTENT_INFO); fp.add(UIDFolder.FetchProfileItem.UID); fp.add("Message-ID"); fp.add("References"); fp.add("In-Reply-To");
        folder.fetch(part.toArray(new Message[0]), fp);
    }

    private int insert(long accountId, long folderId, long uid, Message m, boolean useSentDate) throws Exception {
        String from = null, fromName = null; Address[] fa = m.getFrom();
        if (fa != null && fa.length > 0 && fa[0] instanceof InternetAddress ia) { from = trunc(ia.getAddress(), 200); fromName = trunc(ia.getPersonal(), 200); }
        Date d = useSentDate || m.getReceivedDate() == null ? m.getSentDate() : m.getReceivedDate(); if (d == null) d = m.getSentDate();
        String subject = m.getSubject(); boolean attach = m.isMimeType("multipart/mixed");
        String[] mid = m.getHeader("Message-ID"), irt = m.getHeader("In-Reply-To"), refs = m.getHeader("References");
        return jdbc.update("INSERT IGNORE INTO mail_message (account_id, folder_id, uid, message_id, in_reply_to, refs, from_addr, from_name, to_addrs, cc_addrs, bcc_addrs, subject, sent_at, size_bytes, has_attach, is_read, is_starred, is_draft) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                accountId, folderId, uid, trunc(first(mid), 300), trunc(first(irt), 300), refs == null || refs.length == 0 ? null : String.join(" ", refs), from, fromName, addrs(m.getRecipients(Message.RecipientType.TO)), addrs(m.getRecipients(Message.RecipientType.CC)), addrs(m.getRecipients(Message.RecipientType.BCC)),
                trunc(subject == null ? "" : subject.replaceAll("[\\r\\n]+", " "), 500), d == null ? null : new Timestamp(d.getTime()), m.getSize() < 0 ? null : m.getSize(), attach ? 1 : 0, m.isSet(Flags.Flag.SEEN) ? 1 : 0, m.isSet(Flags.Flag.FLAGGED) ? 1 : 0, m.isSet(Flags.Flag.DRAFT) ? 1 : 0);
    }
    private static String first(String[] a) { return a == null || a.length == 0 ? null : a[0]; }
    private static String trunc(String s, int n) { return s == null ? null : s.length() <= n ? s : s.substring(0, n); }
    static String addrs(Address[] a) {
        if (a == null || a.length == 0) return null; StringBuilder b = new StringBuilder();
        for (Address x : a) { if (b.length() > 0) b.append(", "); try { b.append(x instanceof InternetAddress ia ? MimeUtility.decodeText(ia.toUnicodeString()) : x.toString()); } catch (Exception e) { b.append(x); } }
        return b.length() > 20000 ? b.substring(0, 20000) : b.toString();
    }

    // ------------------------------------------------------------------ older mails
    /** "Load more": the next older mails of a folder (50 at a time). Returns how many came and whether older ones remain. */
    public Map<String, Object> loadMore(long folderId) {
        String me = TeamScope.me();
        Map<String, Object> f = jdbc.queryForList("SELECT f.*, a.emp_id FROM mail_folder f JOIN mail_account a ON a.id = f.account_id WHERE f.id = ? AND a.emp_id = ?", folderId, me).stream().findFirst().orElseThrow(() -> new ForbiddenException("This folder is not yours"));
        MailAccountService.Account acc = accounts.load(me).orElseThrow();
        long accId = acc.id(); int got = 0; boolean more = false;
        try (Store st = ImapConnect.open(accounts, acc)) {
            Folder folder = st.getFolder((String) f.get("path")); UIDFolder uf = (UIDFolder) folder; folder.open(Folder.READ_ONLY);
            try {
                if (f.get("lowest_uid") == null) return Map.of("loaded", 0, "more", false);
                Message low = uf.getMessageByUID(((Number) f.get("lowest_uid")).longValue()); if (low == null) return Map.of("loaded", 0, "more", false);
                int n = low.getMessageNumber(), from = Math.max(1, n - MORE), to = n - 1;
                if (to >= from) {
                    Message[] ms = folder.getMessages(from, to); fetchHeaders(folder, Arrays.asList(ms)); long lowest = ((Number) f.get("lowest_uid")).longValue();
                    for (Message m : ms) { long uid = uf.getUID(m); if (m.isSet(Flags.Flag.DELETED)) continue; got += insert(accId, ((Number) f.get("id")).longValue(), uid, m, "SENT".equals(f.get("role")) || "DRAFTS".equals(f.get("role"))); lowest = Math.min(lowest, uid); }
                    jdbc.update("UPDATE mail_folder SET lowest_uid = ? WHERE id = ?", lowest, folderId);
                }
                more = from > 1; recount(folderId);
            } finally { try { folder.close(false); } catch (Exception ignore) { /* closing */ } }
        } catch (Exception e) { throw new IllegalStateException(MailAccountService.friendly(e)); }
        return Map.of("loaded", got, "more", more);
    }
}
