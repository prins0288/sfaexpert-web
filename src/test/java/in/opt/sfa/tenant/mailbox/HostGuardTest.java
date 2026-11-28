package in.opt.sfa.tenant.mailbox;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;

class HostGuardTest {
    private static boolean internal(String ip) throws Exception { return HostGuard.isInternal(InetAddress.getByName(ip)); }

    @Test void internalAddressesAreBlocked() throws Exception {
        for (String ip : new String[]{"127.0.0.1", "10.1.2.3", "172.16.0.5", "192.168.1.1", "169.254.169.254", "100.64.0.1", "0.0.0.0", "::1", "fc00::1", "fe80::1", "::ffff:10.0.0.1", "224.0.0.1"})
            assertTrue(internal(ip), ip + " must be internal");
    }
    @Test void publicAddressesAreAllowed() throws Exception {
        for (String ip : new String[]{"8.8.8.8", "142.250.80.5", "2607:f8b0:4004:c07::6a"}) assertFalse(internal(ip), ip);
    }
    @Test void ipLiteralOfTheInternalNetworkIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> HostGuard.check("127.0.0.1", 587, null));
        assertThrows(IllegalArgumentException.class, () -> HostGuard.check("169.254.169.254", 993, null));
        assertThrows(IllegalArgumentException.class, () -> HostGuard.check("localhost", 587, null));
    }
    @Test void onlyMailPortsForUserHosts() {
        assertThrows(IllegalArgumentException.class, () -> HostGuard.check("8.8.8.8", 22, null));
        assertThrows(IllegalArgumentException.class, () -> HostGuard.check("8.8.8.8", 3306, null));
        assertDoesNotThrow(() -> HostGuard.check("8.8.8.8", 587, null));
    }
    @Test void badHostNames() {
        for (String h : new String[]{"", " ", "a b", "mail.example.com/../x", "host;rm", "ho\nst", "-bad.com"}) assertThrows(IllegalArgumentException.class, () -> HostGuard.check(h, 587, null), h);
    }
    @Test void adminMayAllowTheCompanyMailServer() {
        assertDoesNotThrow(() -> HostGuard.check("localhost", 3143, "localhost, mail.internal"));
        assertThrows(IllegalArgumentException.class, () -> HostGuard.check("localhost", 3143, "mail.internal"));
    }
}
