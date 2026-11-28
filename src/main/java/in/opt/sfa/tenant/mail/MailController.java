package in.opt.sfa.tenant.mail;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mail")
public class MailController {
    private final MailService mail;
    public MailController(MailService mail) { this.mail = mail; }

    @GetMapping("/smtp") public Map<String, Object> get() { return mail.get(); }
    @PostMapping("/smtp") public Map<String, Object> save(@RequestBody Map<String, Object> b) { mail.save(b); return Map.of("saved", true); }
    @PostMapping("/smtp/test") public Map<String, Object> test(@RequestBody Map<String, Object> b) { mail.test(b.get("to") == null ? null : b.get("to").toString()); return Map.of("sent", true); }
}
