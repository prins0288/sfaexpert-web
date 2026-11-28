package in.opt.sfa.tenant.mailbox;

import in.opt.sfa.tenant.team.TeamScope;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** Compose and send (/api/mailbox/...). The folders, the list and the mail view are in MailReadController. */
@RestController
@RequestMapping("/api/mailbox")
public class MailboxController {
    private final MailSendService send;
    private final MailAttachmentStore files;
    private final MailAccountService accounts;

    public MailboxController(MailSendService send, MailAttachmentStore files, MailAccountService accounts) { this.send = send; this.files = files; this.accounts = accounts; }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) throws Exception { return files.upload(TeamScope.me(), file, accounts.maxAttachmentMb()); }
    @PostMapping("/send") public Map<String, Object> send(@RequestBody Map<String, Object> b) throws Exception { return send.enqueue(b); }
    @GetMapping("/outbox") public List<Map<String, Object>> outbox() { return send.outbox(); }
    @PostMapping("/outbox/{id}/{action}") public Map<String, Object> resolve(@PathVariable long id, @PathVariable String action) { return send.resolve(id, action.toUpperCase()); }
}
