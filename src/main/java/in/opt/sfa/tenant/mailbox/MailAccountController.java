package in.opt.sfa.tenant.mailbox;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** My mail account settings (/api/mailbox/account), test connection and the company defaults (admin). */
@RestController
@RequestMapping("/api/mailbox")
public class MailAccountController {
    private final MailAccountService accounts;
    public MailAccountController(MailAccountService accounts) { this.accounts = accounts; }

    @GetMapping("/mode") public Map<String, Object> mode() { return Map.of("mode", accounts.mode()); }
    @GetMapping("/account") public Map<String, Object> get() { return accounts.get(); }
    @PostMapping("/account") public Map<String, Object> save(@RequestBody Map<String, Object> b) { return accounts.save(b); }
    @PostMapping("/account/enabled") public Map<String, Object> enabled(@RequestBody Map<String, Object> b) { accounts.setEnabled(Boolean.parseBoolean(String.valueOf(b.get("enabled")))); return Map.of("saved", true); }
    @PostMapping("/account/delete") public Map<String, Object> delete() { accounts.delete(); return Map.of("deleted", true); }
    @PostMapping("/account/test") public Map<String, Object> test(@RequestBody Map<String, Object> b) { return accounts.test(b); }
    @GetMapping("/admin") public Map<String, Object> adminGet() { return accounts.adminGet(); }
    @PostMapping("/admin") public Map<String, Object> adminSave(@RequestBody Map<String, Object> b) { accounts.adminSave(b); return Map.of("saved", true); }
}
