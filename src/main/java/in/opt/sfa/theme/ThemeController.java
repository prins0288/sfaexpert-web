package in.opt.sfa.theme;

import in.opt.sfa.security.UserContext;
import in.opt.sfa.theme.dto.ThemeConfig;
import in.opt.sfa.theme.dto.ThemeUpdateRequest;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Per-user appearance API. The username always comes from the VERIFIED JWT
 * (UserContext), never from the request body, so a user can only read and
 * change their own theme.
 *
 *   GET   /api/theme          -> resolved config (choices + tokens + metadata)
 *   PUT   /api/theme          -> partial update (layout / mode / preset / tokens)
 *   POST  /api/theme/reset    -> clear all overrides for a mode
 */
@Tag(name = "Appearance & Theme", description = "Per-user theme: colours, light/dark, nav layout, density")
@RestController
@RequestMapping("/api/theme")
public class ThemeController {

    private final ThemeService themeService;

    public ThemeController(ThemeService themeService) {
        this.themeService = themeService;
    }

    @GetMapping
    public ThemeConfig get() {
        return themeService.get(currentUser());
    }

    @PutMapping
    public ThemeConfig update(@RequestBody ThemeUpdateRequest req) {
        return themeService.update(currentUser(), req);
    }

    @PostMapping("/reset")
    public ThemeConfig reset(@RequestParam(defaultValue = "light") String mode) {
        return themeService.resetMode(currentUser(), mode);
    }

    private String currentUser() {
        UserContext.CurrentUser u = UserContext.get();
        if (u == null || u.username() == null) {
            throw new IllegalStateException("No authenticated user in context");
        }
        return u.username();
    }
}
