package in.opt.sfa.tenant.mailbox;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Part;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/** Fetches the body of ONE mail from the server the first time it is opened and keeps it in the cache; lists the attachments (not their content). Small inline pictures (cid:) are put into the HTML as data: pictures. */
@Service
public class MailBodyService {
    private static final long INLINE_MAX = 400 * 1024, INLINE_TOTAL = 2L * 1024 * 1024;
    private final JdbcTemplate jdbc;
    private final MailAccountService accounts;

    public MailBodyService(JdbcTemplate jdbc, MailAccountService accounts) { this.jdbc = jdbc; this.accounts = accounts; }

    public void fetch(MailAccountService.Account acc, long id) throws Exception {
        Map<String, Object> m = jdbc.queryForList("SELECT m.uid, f.path FROM mail_message m JOIN mail_folder f ON f.id = m.folder_id WHERE m.id = ? AND m.account_id = ?", id, acc.id()).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Mail not found"));
        try (Store st = ImapConnect.open(accounts, acc)) {
            Folder f = st.getFolder((String) m.get("path")); f.open(Folder.READ_ONLY);
            try {
                Message msg = ((UIDFolder) f).getMessageByUID(((Number) m.get("uid")).longValue());
                if (msg == null) throw new IllegalStateException("This mail is no longer on the mail server");
                StringBuilder[] text = new StringBuilder[1], html = new StringBuilder[1]; List<Part> att = new java.util.ArrayList<>();
                ImapAttachment.walk(msg, att, text, html);
                String h = html[0] == null ? null : html[0].toString(), t = text[0] == null ? null : text[0].toString();
                jdbc.update("DELETE FROM mail_attachment WHERE message_id = ?", id);
                long inlineTotal = 0; boolean any = false;
                for (int i = 0; i < att.size(); i++) {
                    Part p = att.get(i); String cid = ImapAttachment.contentId(p), type = ImapAttachment.type(p); int size = Math.max(0, p.getSize());
                    boolean inline = cid != null && type.startsWith("image/") && h != null && h.contains("cid:" + cid);
                    if (inline && size <= INLINE_MAX && inlineTotal + size <= INLINE_TOTAL) { byte[] data = ImapAttachment.read(p, INLINE_MAX); inlineTotal += data.length; h = h.replace("cid:" + cid, "data:" + type + ";base64," + Base64.getEncoder().encodeToString(data)); }
                    else if (inline) inline = false;   // too big: shown as an attachment instead
                    if (!inline) any = true;
                    jdbc.update("INSERT INTO mail_attachment (message_id, part_no, filename, content_type, size_bytes, content_id, is_inline) VALUES (?,?,?,?,?,?,?)", id, i, ImapAttachment.name(p, i), type, size, cid, inline ? 1 : 0);
                }
                String snippet = MimeBuilder.htmlToText(h != null ? h : (t == null ? "" : t.replace("<", "&lt;"))).replaceAll("\\s+", " ").trim(); if (snippet.length() > 200) snippet = snippet.substring(0, 200);
                jdbc.update("UPDATE mail_message SET body_html = ?, body_text = ?, snippet = ?, has_attach = ?, body_fetched = 1 WHERE id = ?", h, t, snippet, any ? 1 : 0, id);
            } finally { try { f.close(false); } catch (Exception ignore) { /* closing */ } }
        }
    }
}
