package in.opt.sfa.tenant.mailbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM for the mail passwords. The key comes only from the environment variable SFA_MAIL_KEY (32 bytes, Base64) - never from code, database or git.
 * Without the key nothing can be encrypted or decrypted: the mail features say so instead of storing a password in a weaker way.
 * The stored text is "v1:" + Base64(iv(12) + ciphertext + tag); the account (context) is bound as additional authenticated data, so a value copied to another account does not decrypt.
 */
@Component
public class MailCrypto {
    private static final SecureRandom RNG = new SecureRandom();
    private final byte[] key;

    @org.springframework.beans.factory.annotation.Autowired
    public MailCrypto(@Value("${SFA_MAIL_KEY:}") String base64Key) { this.key = parse(base64Key); }

    /** For tests. */
    public MailCrypto(byte[] key) { this.key = key; }

    private static byte[] parse(String b64) {
        if (b64 == null || b64.isBlank()) return null;
        try { byte[] k = Base64.getDecoder().decode(b64.trim()); return k.length == 32 ? k : null; } catch (IllegalArgumentException e) { return null; }
    }

    public boolean available() { return key != null; }

    private void need() { if (key == null) throw new IllegalStateException("The mail encryption key is not set. Set the environment variable SFA_MAIL_KEY (32 bytes, Base64) and restart the application."); }

    public String encrypt(String plain, String context) {
        need();
        try {
            byte[] iv = new byte[12]; RNG.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            c.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            byte[] ct = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] all = new byte[iv.length + ct.length]; System.arraycopy(iv, 0, all, 0, iv.length); System.arraycopy(ct, 0, all, iv.length, ct.length);
            return "v1:" + Base64.getEncoder().encodeToString(all);
        } catch (Exception e) { throw new IllegalStateException("The password could not be encrypted"); }
    }

    public String decrypt(String stored, String context) {
        need();
        try {
            if (stored == null || !stored.startsWith("v1:")) throw new IllegalStateException("bad format");
            byte[] all = Base64.getDecoder().decode(stored.substring(3));
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, all, 0, 12));
            c.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            return new String(c.doFinal(all, 12, all.length - 12), StandardCharsets.UTF_8);
        } catch (Exception e) { throw new IllegalStateException("The saved mail password cannot be read - save the password again"); }
    }
}
