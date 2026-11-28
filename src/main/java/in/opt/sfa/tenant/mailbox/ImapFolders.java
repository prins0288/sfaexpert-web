package in.opt.sfa.tenant.mailbox;

import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.Store;
import org.eclipse.angus.mail.imap.IMAPFolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Finds the folders of an IMAP account and the role of each (Inbox, Sent, Drafts, Trash, Spam, Archive, other) - by the server's special-use flags, else by the usual names. */
public final class ImapFolders {
    private ImapFolders() { }

    public record Info(String path, String name, String role, int order) { }

    private static final Set<String> SENT = Set.of("sent", "sent items", "sent mail", "sent messages", "gesendet");
    private static final Set<String> TRASH = Set.of("trash", "deleted items", "deleted messages", "bin");
    private static final Set<String> SPAM = Set.of("spam", "junk", "junk email", "junk e-mail", "bulk mail");
    private static final Set<String> ARCH = Set.of("archive", "archives", "all mail");

    public static List<Info> discover(Store store) throws MessagingException {
        List<Info> out = new ArrayList<>(); Folder[] all = store.getDefaultFolder().list("*");
        for (Folder f : all) {
            int type = f.getType(); if ((type & Folder.HOLDS_MESSAGES) == 0) continue;   // a folder that only holds folders
            String path = f.getFullName(), name = f.getName(), role = roleOf(f, path, name);
            out.add(new Info(path, name, role, order(role)));
        }
        if (out.stream().noneMatch(i -> "INBOX".equals(i.role()))) out.add(new Info("INBOX", "INBOX", "INBOX", 0));
        out.sort((a, b) -> a.order() != b.order() ? Integer.compare(a.order(), b.order()) : a.name().compareToIgnoreCase(b.name()));
        return out;
    }

    private static String roleOf(Folder f, String path, String name) {
        if ("INBOX".equalsIgnoreCase(path)) return "INBOX";
        if (f instanceof IMAPFolder imap) {
            try {
                for (String a : imap.getAttributes()) {
                    switch (a.toLowerCase(Locale.ROOT)) { case "\\sent" -> { return "SENT"; } case "\\drafts" -> { return "DRAFTS"; } case "\\trash" -> { return "TRASH"; } case "\\junk" -> { return "SPAM"; } case "\\archive", "\\all" -> { return "ARCHIVE"; } default -> { } }
                }
            } catch (MessagingException ignore) { /* name rules below */ }
        }
        String n = name.toLowerCase(Locale.ROOT);
        if (SENT.contains(n)) return "SENT"; if (n.equals("drafts") || n.equals("draft")) return "DRAFTS"; if (TRASH.contains(n)) return "TRASH"; if (SPAM.contains(n)) return "SPAM"; if (ARCH.contains(n)) return "ARCHIVE";
        return "CUSTOM";
    }

    static int order(String role) { return switch (role) { case "INBOX" -> 0; case "SENT" -> 1; case "DRAFTS" -> 2; case "ARCHIVE" -> 3; case "SPAM" -> 4; case "TRASH" -> 5; default -> 10; }; }

    /** The servers that already keep a copy of a mail sent through their SMTP in the Sent folder - appending another copy would make a duplicate. */
    public static boolean serverSavesSent(String smtpHost) {
        String h = smtpHost == null ? "" : smtpHost.toLowerCase(Locale.ROOT);
        return h.endsWith("gmail.com") || h.endsWith("googlemail.com") || h.endsWith("office365.com") || h.endsWith("outlook.com") || h.endsWith("hotmail.com") || h.endsWith("yahoo.com");
    }
}
