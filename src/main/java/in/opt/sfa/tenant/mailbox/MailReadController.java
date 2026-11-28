package in.opt.sfa.tenant.mailbox;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Folders, list, mail view, attachment download, refresh and load more (/api/mailbox/...). */
@RestController
@RequestMapping("/api/mailbox")
public class MailReadController {
    private final MailReadService read;
    private final ImapSyncService sync;
    private final MailActionService actions;

    public MailReadController(MailReadService read, ImapSyncService sync, MailActionService actions) { this.read = read; this.sync = sync; this.actions = actions; }

    @GetMapping("/folders") public Map<String, Object> folders() { return read.folders(); }

    @GetMapping("/messages")
    public Map<String, Object> list(@RequestParam(required = false) Long folderId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size, @RequestParam(required = false) String q, @RequestParam(required = false) String from,
                                    @RequestParam(required = false) String subject, @RequestParam(required = false) String dateFrom, @RequestParam(required = false) String dateTo, @RequestParam(defaultValue = "false") boolean unread, @RequestParam(defaultValue = "false") boolean attach, @RequestParam(defaultValue = "false") boolean starred) {
        return read.list(folderId, page, size, q, from, subject, dateFrom, dateTo, unread, attach, starred);
    }

    @GetMapping("/messages/{id}") public Map<String, Object> view(@PathVariable long id, @RequestParam(defaultValue = "false") boolean images) throws Exception { return read.view(id, images); }

    @GetMapping("/messages/{id}/attachments/{part}")
    public ResponseEntity<byte[]> download(@PathVariable long id, @PathVariable int part) throws Exception {
        MailReadService.Download d = read.attachment(id, part);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).header("X-Content-Type-Options", "nosniff")   // always a download, never rendered by the browser
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(d.name(), StandardCharsets.UTF_8).build().toString()).body(d.data());
    }

    @PostMapping("/sync") public Map<String, Object> sync() { ImapSyncService.Result r = sync.syncMine(); return Map.of("newMails", r.newMails(), "folders", r.folders(), "error", r.error() == null ? "" : r.error()); }
    @PostMapping("/folders/{id}/more") public Map<String, Object> more(@PathVariable long id) { return sync.loadMore(id); }

    @PostMapping("/messages/action") public Map<String, Object> action(@RequestBody Map<String, Object> b) throws Exception { return actions.act(b); }
    @PostMapping("/drafts") public Map<String, Object> draft(@RequestBody Map<String, Object> b) throws Exception { return actions.saveDraft(b); }
    @GetMapping("/messages/{id}/prepare") public Map<String, Object> prepare(@PathVariable long id, @RequestParam String mode) throws Exception { return actions.prepare(id, mode); }
    @GetMapping("/contacts") public java.util.List<Map<String, Object>> contacts(@RequestParam(required = false) String q) { return actions.contacts(q); }
}
