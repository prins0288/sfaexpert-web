package in.opt.sfa.theme;

import in.opt.sfa.security.UserContext;
import in.opt.sfa.theme.dto.ThemeConfig;
import in.opt.sfa.theme.dto.ThemeUpdateRequest;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * PER-TENANT appearance API. The tenant comes from the VERIFIED JWT (the
 * request is already bound to a tenant DB by TenantAuthFilter), so a caller
 * can only ever read/change their OWN company's look — never another tenant's.
 *
 * Note this is company-wide, not per-user: whoever edits Appearance changes it
 * for every user of that tenant.
 *
 *   GET   /api/theme          -> resolved config (choices + tokens + metadata)
 *   PUT   /api/theme          -> partial update (layout / mode / preset / tokens)
 *   POST  /api/theme/reset    -> clear all overrides for a mode
 */
@Tag(name = "Appearance & Theme", description = "Per-tenant theme: colours, light/dark, nav layout, density")
@RestController
@RequestMapping("/api/theme")
public class ThemeController {

    private final ThemeService themeService;

    public ThemeController(ThemeService themeService) {
        this.themeService = themeService;
    }

    @GetMapping
    public ThemeConfig get() {
        requireAuth();
        return themeService.get();
    }

    @PutMapping
    public ThemeConfig update(@RequestBody ThemeUpdateRequest req) {
        requireAuth();
        return themeService.update(req);
    }

    @PostMapping("/reset")
    public ThemeConfig reset(@RequestParam(defaultValue = "light") String mode) {
        requireAuth();
        return themeService.resetMode(mode);
    }

    private void requireAuth() {
        UserContext.CurrentUser u = UserContext.get();
        if (u == null || u.username() == null) {
            throw new IllegalStateException("No authenticated user in context");
        }
    }
}
