package in.opt.sfa.i18n;

import in.opt.sfa.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Language / labels API. The tenant (for label_master overrides) and the user
 * (for the saved language choice) both come from the VERIFIED JWT, never the
 * request body.
 *
 *   GET  /api/i18n/bundle?lang=xx  -> resolved labels + version + available langs
 *   GET  /api/i18n/lang           -> the caller's saved language (defaults to en)
 *   PUT  /api/i18n/lang?lang=xx    -> save the caller's language, returns the new bundle
 *
 * The browser downloads a bundle once and caches it in localStorage, re-fetching
 * only when `version` changes — so normal navigation never hits these endpoints.
 */
@Tag(name = "Language & Labels", description = "Per-user language; per-tenant label overrides; download-once bundle")
@RestController
@RequestMapping("/api/i18n")
public class I18nController {

    private final I18nService i18n;

    public I18nController(I18nService i18n) {
        this.i18n = i18n;
    }

    @GetMapping("/bundle")
    @Operation(summary = "Resolved labels for a language (defaults to the caller's saved language)")
    public I18nBundle bundle(@RequestParam(required = false) String lang) {
        String effective = (lang == null || lang.isBlank()) ? i18n.getUserLang(currentUser()) : lang;
        return i18n.bundle(effective);
    }

    @GetMapping("/languages")
    @Operation(summary = "Languages available to choose from")
    public List<I18nBundle.LangInfo> languages() {
        return i18n.available();
    }

    @GetMapping("/lang")
    @Operation(summary = "The caller's saved language")
    public Map<String, String> getLang() {
        return Map.of("lang", i18n.getUserLang(currentUser()));
    }

    @PutMapping("/lang")
    @Operation(summary = "Save the caller's language and return the new resolved bundle")
    public I18nBundle setLang(@RequestParam String lang) {
        String saved = i18n.setUserLang(currentUser(), lang);
        return i18n.bundle(saved);
    }

    private static String currentUser() {
        UserContext.CurrentUser u = UserContext.get();
        if (u == null || u.username() == null) {
            throw new IllegalStateException("No authenticated user in context");
        }
        return u.username();
    }
}
