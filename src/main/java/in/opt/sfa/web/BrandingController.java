package in.opt.sfa.web;

import in.opt.sfa.common.entity.CompanyDetails;
import in.opt.sfa.common.repository.CompanyDetailsRepository;
import in.opt.sfa.common.service.CompanySettingStore;
import in.opt.sfa.security.Authz;
import in.opt.sfa.storage.FileStorageService;
import in.opt.sfa.tenant.context.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Branding / logos.
 *   - the APP-WIDE default (appName + default logo) lives in company_setting_master
 *     under the GLOBAL pseudo-tenant ("*"): keys branding.appName / branding.logo.
 *     The logo is a FILE in the external storage folder (path, not base64), so the
 *     login page (which has no tenant yet) can show it.
 *   - the per-tenant COMPANY logo stays in company_details.logo (base64), set from
 *     the company profile page.
 *
 * GET /api/public/branding is PUBLIC (login page, pre-auth). GET /api/branding is
 * authenticated and returns the effective logo = company logo if set, else the
 * app-wide default. The default logo bytes are streamed by a PUBLIC endpoint so an
 * <img> can load them without a Bearer header.
 */
@RestController
@Tag(name = "Branding & Logos", description = "App-wide default (name + logo) and per-tenant company logo")
public class BrandingController {

    private static final String K_APP_NAME = "branding.appName";
    private static final String K_LOGO = "branding.logo";      // stored file name (external folder)
    private static final String BRAND_FOLDER = "__global__";   // external storage sub-folder for global assets
    private static final String DEFAULT_APP_NAME = "StarSFA";

    private final CompanySettingStore settings;            // common (GLOBAL + per-tenant)
    private final CompanyDetailsRepository companies;      // common (per-tenant company logo)
    private final FileStorageService storage;

    public BrandingController(CompanySettingStore settings, CompanyDetailsRepository companies,
                              FileStorageService storage) {
        this.settings = settings;
        this.companies = companies;
        this.storage = storage;
    }

    /** Public: app name + default logo URL (used by the login page, pre-auth). */
    @GetMapping("/api/public/branding")
    @Operation(summary = "Public branding (app name + default logo) — no auth required")
    public Map<String, Object> publicBranding() {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("appName", settings.valueFor(CompanySettingStore.GLOBAL, K_APP_NAME, DEFAULT_APP_NAME));
        res.put("defaultLogo", defaultLogoUrl());
        return res;
    }

    /** Authenticated: default + company logos, plus the effective logo to show. */
    @GetMapping("/api/branding")
    @Operation(summary = "Branding for the app chrome (effective logo = company, else default)")
    public Map<String, Object> branding() {
        String defaultLogo = defaultLogoUrl();
        CompanyDetails c = companies.findById(TenantContext.getCompanyCode()).orElse(null);
        String companyLogo = c == null ? null : c.getLogo();
        String effective = hasText(companyLogo) ? companyLogo : defaultLogo;

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("appName", settings.valueFor(CompanySettingStore.GLOBAL, K_APP_NAME, DEFAULT_APP_NAME));
        res.put("defaultLogo", defaultLogo);
        res.put("companyName", c == null ? null : c.getCompanyName());
        res.put("companyLogo", companyLogo);
        res.put("logo", effective);
        return res;
    }

    /** Set the app-wide DEFAULT logo (global) — upload a FILE, stored in the external
     *  folder as a path (not base64). Super-admin only. */
    @PostMapping("/api/branding/logo")
    @Operation(summary = "Upload the app-wide default logo (multipart 'file'); stored as a path")
    public Map<String, Object> setDefaultLogo(@RequestParam("file") MultipartFile file) throws Exception {
        Authz.requireRole("SUPER_ADMIN");
        if (file == null || file.isEmpty()) throw new IllegalStateException("Please choose an image.");
        String ext = ext(file.getOriginalFilename());
        String filename = "brand-" + UUID.randomUUID().toString().replace("-", "") + (ext.isEmpty() ? "" : "." + ext);

        String old = settings.valueFor(CompanySettingStore.GLOBAL, K_LOGO).orElse(null);
        if (old != null && !old.isBlank()) storage.delete(BRAND_FOLDER, old);   // replace the previous one

        String stored = storage.store(BRAND_FOLDER, filename, file.getBytes());
        settings.putFor(CompanySettingStore.GLOBAL, K_LOGO, stored);
        return Map.of("saved", true, "defaultLogo", logoUrl(stored));
    }

    /** Remove the app-wide default logo. Super-admin only. */
    @DeleteMapping("/api/branding/logo")
    @Operation(summary = "Clear the app-wide default logo")
    public Map<String, Object> clearDefaultLogo() {
        Authz.requireRole("SUPER_ADMIN");
        String old = settings.valueFor(CompanySettingStore.GLOBAL, K_LOGO).orElse(null);
        if (old != null && !old.isBlank()) storage.delete(BRAND_FOLDER, old);
        settings.putFor(CompanySettingStore.GLOBAL, K_LOGO, "");
        return Map.of("saved", true);
    }

    /** Set the app-wide app name (global). Super-admin only. */
    @PostMapping("/api/branding/app-name")
    @Operation(summary = "Set the app-wide application name")
    public Map<String, Object> setAppName(@RequestBody Map<String, String> body) {
        Authz.requireRole("SUPER_ADMIN");
        String name = body.get("appName");
        settings.putFor(CompanySettingStore.GLOBAL, K_APP_NAME,
                (name == null || name.isBlank()) ? DEFAULT_APP_NAME : name.trim());
        return Map.of("saved", true);
    }

    /** PUBLIC default-logo stream — an <img> can load it with no Bearer header. */
    @GetMapping("/api/public/branding-image")
    public ResponseEntity<byte[]> image(@RequestParam String file) {
        byte[] data;
        try {
            data = storage.load(BRAND_FOLDER, file);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();   // missing file -> 404, client falls back to app name
        }
        return ResponseEntity.ok()
                .contentType(contentType(file))
                .cacheControl(CacheControl.noCache())
                .body(data);
    }

    // ---- helpers ------------------------------------------------------------

    /** The public URL for the current global default logo, or null if none. */
    private String defaultLogoUrl() {
        String file = settings.valueFor(CompanySettingStore.GLOBAL, K_LOGO).orElse(null);
        return hasText(file) ? logoUrl(file) : null;
    }

    /** Relative URL (resolves under the app's <base href>) for a stored logo file. */
    private static String logoUrl(String file) {
        return "api/public/branding-image?file=" + file;
    }

    private static boolean hasText(String s) { return s != null && !s.isBlank(); }

    private static String ext(String name) {
        if (name == null) return "";
        int i = name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i + 1).toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private static MediaType contentType(String name) {
        switch (ext(name)) {
            case "png":  return MediaType.IMAGE_PNG;
            case "jpg":  case "jpeg": return MediaType.IMAGE_JPEG;
            case "gif":  return MediaType.IMAGE_GIF;
            case "svg":  return MediaType.valueOf("image/svg+xml");
            case "webp": return MediaType.valueOf("image/webp");
            default:     return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
