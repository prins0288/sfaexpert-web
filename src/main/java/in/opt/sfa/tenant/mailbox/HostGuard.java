package in.opt.sfa.tenant.mailbox;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Checks a mail server given by a user (SSRF protection): a valid host name, a mail port, and no address on the internal network (loopback, private, link-local, carrier-grade NAT, multicast,
 * unique-local IPv6, cloud metadata). A host in the admin's allow list (the company's own internal mail server) may be internal.
 * The address is resolved here and the same check is made at connect time; a DNS answer that changes in between is a known limit of this approach.
 */
public final class HostGuard {
    private static final Set<Integer> MAIL_PORTS = Set.of(25, 465, 587, 2525, 143, 993);

    private HostGuard() { }

    public static void check(String host, int port, String adminAllowedHosts) {
        if (host == null || host.isBlank()) throw new IllegalArgumentException("Enter the mail server host");
        String h = host.trim().toLowerCase(Locale.ROOT);
        if (h.length() > 150 || !h.matches("[a-z0-9]([a-z0-9.-]*[a-z0-9])?")) throw new IllegalArgumentException("The mail server host is not valid: " + host);
        boolean allowed = allowedHosts(adminAllowedHosts).contains(h);
        if (!allowed && !MAIL_PORTS.contains(port)) throw new IllegalArgumentException("Port " + port + " is not a mail port (use 25, 465, 587, 2525, 143 or 993)");
        if (port < 1 || port > 65535) throw new IllegalArgumentException("The port is not valid");
        if (allowed) return;   // the admin allowed this (internal) server
        InetAddress[] all;
        try { all = InetAddress.getAllByName(h); } catch (Exception e) { throw new IllegalArgumentException("The mail server host cannot be found: " + host); }
        for (InetAddress a : all) if (isInternal(a)) throw new IllegalArgumentException("This mail server address is on an internal network and is not allowed. Ask the admin to allow your company's own mail server.");
    }

    public static List<String> allowedHosts(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split("[,;\\s]+")).map(s -> s.trim().toLowerCase(Locale.ROOT)).filter(s -> !s.isEmpty()).toList();
    }

    public static boolean isInternal(InetAddress a) {
        if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress() || a.isSiteLocalAddress() || a.isMulticastAddress()) return true;
        byte[] b = a.getAddress();
        if (a instanceof Inet4Address) {
            int b0 = b[0] & 0xff, b1 = b[1] & 0xff;
            return b0 == 0 || (b0 == 100 && b1 >= 64 && b1 <= 127) || (b0 == 169 && b1 == 254) || (b0 == 192 && b1 == 0 && (b[2] & 0xff) == 0) || b0 >= 240;
        }
        if (a instanceof Inet6Address) {
            int b0 = b[0] & 0xff;
            if ((b0 & 0xfe) == 0xfc) return true;   // unique local fc00::/7
            boolean v4mapped = true; for (int i = 0; i < 10; i++) if (b[i] != 0) v4mapped = false;
            if (v4mapped && (b[10] & 0xff) == 0xff && (b[11] & 0xff) == 0xff) { try { return isInternal(InetAddress.getByAddress(Arrays.copyOfRange(b, 12, 16))); } catch (Exception e) { return true; } }
        }
        return false;
    }
}
