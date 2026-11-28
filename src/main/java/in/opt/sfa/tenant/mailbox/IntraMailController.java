package in.opt.sfa.tenant.mailbox;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** Intramail (/api/intramail): messages between the users of the application. */
@RestController
@RequestMapping("/api/intramail")
public class IntraMailController {
    private final IntraMailService svc;
    public IntraMailController(IntraMailService svc) { this.svc = svc; }

    @GetMapping("/folders") public Map<String, Object> folders() { return svc.folders(); }
    @GetMapping("/messages") public Map<String, Object> list(@RequestParam(defaultValue = "inbox") String folder, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size, @RequestParam(required = false) String q, @RequestParam(defaultValue = "false") boolean unread) { return svc.list(folder, page, size, q, unread); }
    @GetMapping("/messages/{id}") public Map<String, Object> view(@PathVariable long id) { return svc.view(id); }
    @GetMapping("/messages/{id}/prepare") public Map<String, Object> prepare(@PathVariable long id, @RequestParam String mode) { return svc.prepare(id, mode); }
    @PostMapping("/send") public Map<String, Object> send(@RequestBody Map<String, Object> b) throws Exception { return svc.send(b); }
    @PostMapping("/drafts") public Map<String, Object> draft(@RequestBody Map<String, Object> b) throws Exception { return svc.saveDraft(b); }
    @PostMapping("/messages/action") public Map<String, Object> act(@RequestBody Map<String, Object> b) { return svc.act(b); }
    @GetMapping("/people") public List<Map<String, Object>> people(@RequestParam(required = false) String q) { return svc.people(q); }
    @GetMapping("/groups") public List<Map<String, Object>> groups() { return svc.groups(); }
    @PostMapping("/groups") public Map<String, Object> saveGroup(@RequestBody Map<String, Object> b) { return svc.saveGroup(b); }
    @PostMapping("/groups/{id}/delete") public Map<String, Object> deleteGroup(@PathVariable long id) { svc.deleteGroup(id); return Map.of("deleted", true); }

    @GetMapping("/messages/{id}/attachments/{attId}")
    public ResponseEntity<byte[]> download(@PathVariable long id, @PathVariable long attId) throws Exception {
        IntraMailService.Download d = svc.attachment(id, attId);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(d.name(), StandardCharsets.UTF_8).build().toString()).body(d.data());
    }
}
