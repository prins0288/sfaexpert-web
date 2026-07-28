package in.opt.sfa.web;

import in.opt.sfa.common.entity.AppBranding;
import in.opt.sfa.common.repository.AppBrandingRepository;
import in.opt.sfa.security.UserContext;
import in.opt.sfa.tenant.entity.CompanyProfile;
import in.opt.sfa.tenant.repository.CompanyProfileRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Branding / logos.
 *   - the app-wide DEFAULT logo lives in the COMMON db (app_branding, one row);
 *   - the per-tenant COMPANY logo lives in the TENANT db (company_profile).
 *
 * GET /api/public/branding is PUBLIC (the login page shows the default logo
 * before anyone is authenticated). GET /api/branding is authenticated and
 * returns the effective logo = company logo if set, else the default.
 */
@RestController
@Tag(name = "Branding & Logos", description = "App default logo and per-tenant company logo")
public class BrandingController {

    private final AppBrandingRepository branding;          // common
    private final CompanyProfileRepository companies;      // tenant

    public BrandingController(AppBrandingRepository branding, CompanyProfileRepository companies) {
        this.branding = branding;
        this.companies = companies;
    }

    /** Public: app name + default logo (used by the login page). */
    @GetMapping("/api/public/branding")
    @Operation(summary = "Public branding (app name + default logo) — no auth required")
    @Transactional(transactionManager = "commonTransactionManager", readOnly = true)
    public Map<String, Object> publicBranding() {
        AppBranding b = branding.findById(1).orElseGet(AppBranding::new);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("appName", b.getAppName());
        res.put("defaultLogo", b.getDefaultLogo());
        return res;
    }

    /** Authenticated: default + company logos, plus the effective logo to show. */
    @GetMapping("/api/branding")
    @Operation(summary = "Branding for the app chrome (effective logo = company, else default)")
    public Map<String, Object> branding() {
        AppBranding b = branding.findById(1).orElseGet(AppBranding::new);
        CompanyProfile c = companies.findById(1).orElse(null);
        String companyLogo = c == null ? null : c.getLogo();
        String effective = hasText(companyLogo) ? companyLogo : b.getDefaultLogo();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("appName", b.getAppName());
        res.put("defaultLogo", b.getDefaultLogo());
        res.put("companyName", c == null ? null : c.getCompanyName());
        res.put("companyLogo", companyLogo);
        res.put("logo", effective);
        return res;
    }

    /** Set the app-wide DEFAULT logo (common db). */
    @PostMapping("/api/branding/logo")
    @Operation(summary = "Set the app-wide default logo (base64 data URI in `image`; blank clears it)")
    @Transactional(transactionManager = "commonTransactionManager")
    public Map<String, Object> setDefaultLogo(@RequestBody Map<String, String> body) {
        AppBranding b = branding.findById(1).orElseGet(() -> {
            AppBranding x = new AppBranding();
            x.setId(1);
            return x;
        });
        String image = body.get("image");
        b.setDefaultLogo(hasText(image) ? image : null);
        b.setUpdatedAt(LocalDateTime.now());
        UserContext.CurrentUser u = UserContext.get();
        b.setUpdatedBy(u == null ? "system" : u.username());
        branding.save(b);
        return Map.of("saved", true);
    }

    private static boolean hasText(String s) { return s != null && !s.isBlank(); }
}
