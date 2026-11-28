package in.opt.sfa.tenant.mailbox;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/** Simple sliding-window limit per key (user + action), in memory: Test Connection and sending cannot be hammered. */
@Component
public class RateLimiter {
    private final ConcurrentHashMap<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    /** Throws when the key already used its {@code max} hits in the last {@code windowMs}. */
    public void check(String key, int max, long windowMs, String message) {
        long now = System.currentTimeMillis();
        Deque<Long> q = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > windowMs) q.pollFirst();
            if (q.size() >= max) throw new IllegalStateException(message);
            q.addLast(now);
        }
    }
}
