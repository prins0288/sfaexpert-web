package in.opt.sfa.tenant.mailbox;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;

class MailCryptoTest {
    private static byte[] key() { byte[] k = new byte[32]; new SecureRandom().nextBytes(k); return k; }

    @Test void roundTrip() {
        MailCrypto c = new MailCrypto(key());
        String enc = c.encrypt("S3cret-Pa$$", "mailacct:E1");
        assertTrue(enc.startsWith("v1:")); assertFalse(enc.contains("S3cret"));
        assertEquals("S3cret-Pa$$", c.decrypt(enc, "mailacct:E1"));
    }
    @Test void sameTextGivesDifferentCipher() {
        MailCrypto c = new MailCrypto(key());
        assertNotEquals(c.encrypt("x", "a"), c.encrypt("x", "a"));   // random IV every time
    }
    @Test void anotherAccountCannotReadIt() {
        MailCrypto c = new MailCrypto(key());
        String enc = c.encrypt("pw", "mailacct:E1");
        assertThrows(IllegalStateException.class, () -> c.decrypt(enc, "mailacct:E2"));   // bound to the account
    }
    @Test void anotherKeyCannotReadIt() {
        String enc = new MailCrypto(key()).encrypt("pw", "c");
        assertThrows(IllegalStateException.class, () -> new MailCrypto(key()).decrypt(enc, "c"));
    }
    @Test void tamperedValueIsRejected() {
        MailCrypto c = new MailCrypto(key()); String enc = c.encrypt("pw", "c");
        String bad = enc.substring(0, enc.length() - 2) + (enc.endsWith("AA") ? "BB" : "AA");
        assertThrows(IllegalStateException.class, () -> c.decrypt(bad, "c"));
    }
    @Test void withoutKeyNothingIsStored() {
        MailCrypto c = new MailCrypto((byte[]) null);
        assertFalse(c.available());
        assertThrows(IllegalStateException.class, () -> c.encrypt("pw", "c"));
        assertThrows(IllegalStateException.class, () -> c.decrypt("v1:AAAA", "c"));
    }
    @Test void keyFromEnvironmentTextMustBe32Bytes() {
        assertFalse(new MailCrypto("").available());
        assertFalse(new MailCrypto("c2hvcnQ=").available());
        assertTrue(new MailCrypto(java.util.Base64.getEncoder().encodeToString(key())).available());
    }
}
