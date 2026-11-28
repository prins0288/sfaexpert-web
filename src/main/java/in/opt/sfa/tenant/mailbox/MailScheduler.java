package in.opt.sfa.tenant.mailbox;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/** Background jobs of the mail module. Each job skips a round when its previous round is still running. */
@Component
public class MailScheduler {
    private final MailSendService send;
    private final AtomicBoolean sending = new AtomicBoolean(false);

    public MailScheduler(MailSendService send) { this.send = send; }

    /** Sends the queued mails (every 15 seconds). */
    @Scheduled(initialDelay = 20_000, fixedDelay = 15_000)
    public void outbox() {
        if (!sending.compareAndSet(false, true)) return;
        try { send.runAll(); } finally { sending.set(false); }
    }
}
