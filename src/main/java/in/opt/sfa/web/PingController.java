package in.opt.sfa.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * PUBLIC, no-DB, near-instant reachability check — used by the footer's
 * online/offline indicator (js/shell.js) to detect that OUR server is
 * actually reachable, not just that the device has a network interface up
 * (navigator.onLine reports true even on a wifi with no real internet).
 */
@Tag(name = "Ping", description = "Lightweight public reachability check for the online/offline indicator")
@RestController
@RequestMapping("/api/public/ping")
public class PingController {

    @GetMapping
    @Operation(summary = "Always returns ok — no DB access, just confirms the server responds")
    public Map<String, Object> ping() {
        return Map.of("status", "ok", "time", System.currentTimeMillis());
    }
}
