package in.opt.sfa.tenant.loader;

import in.opt.sfa.common.service.CompanySettingStore;
import in.opt.sfa.security.Authz;
import in.opt.sfa.storage.FileStorageService;
import in.opt.sfa.tenant.context.TenantContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-tenant PAGE LOADER look. Everything is customisable and stored per tenant
 * in company_setting_master (loader.* keys); the optional custom image is stored
 * in the EXTERNAL folder (FileStorageService), not the DB.
 *
 * Keys: loader.type (spinner|image), loader.anim (spin|pulse|none — for the image),
 * loader.size (px), loader.speed (ms), loader.text (message under it), loader.image
 * (stored file name in the external folder).
 *
 * The image is streamed by a PUBLIC endpoint so an <img> tag can load it without a
 * Bearer header; it takes tenant + file as params and reads straight off disk (no
 * DB), sandboxed to <base-dir>/<tenant>/ by FileStorageService.
 */
@Tag(name = "Page Loader", description = "Customise the page loader (spinner or external custom image, animation, text)")
@RestController
public class LoaderController {

    private static final String K_TYPE = "loader.type", K_ANIM = "loader.anim",
            K_SIZE = "loader.size", K_SPEED = "loader.speed", K_TEXT = "loader.text",
            K_ROUND = "loader.round", K_RING = "loader.ring", K_BAR = "loader.bar", K_IMAGE = "loader.image";

    private final CompanySettingStore settings;
    private final FileStorageService storage;

    public LoaderController(CompanySettingStore settings, FileStorageService storage) {
        this.settings = settings;
        this.storage = storage;
    }

    /** Current loader config for this tenant (used by the customiser page and by uikit.js). */
    @GetMapping("/api/loader")
    @Transactional(transactionManager = "commonTransactionManager", readOnly = true)
    public Map<String, Object> get() {
        Map<String, String> s = map();
        String image = s.get(K_IMAGE);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("type", orDefault(s.get(K_TYPE), "spinner"));      // spinner | image
        res.put("anim", orDefault(s.get(K_ANIM), "spin"));         // spin | pulse | none
        res.put("size", parseInt(s.get(K_SIZE), 56));
        res.put("speed", parseInt(s.get(K_SPEED), 800));
        res.put("text", orDefault(s.get(K_TEXT), ""));
        res.put("round", "true".equals(s.get(K_ROUND)));          // show the image as a circle
        res.put("ring", "true".equals(s.get(K_RING)));            // spinning arc ring around the image
        res.put("bar", "true".equals(s.get(K_BAR)));              // indeterminate progress bar
        res.put("hasImage", image != null && !image.isBlank());
        res.put("imageName", image);
        res.put("companyCode", TenantContext.getCompanyCode());
        return res;
    }

    /** Save the non-file settings (type / anim / size / speed / text). */
    @PostMapping("/api/loader")
    @Transactional(transactionManager = "commonTransactionManager")
    public Map<String, Object> save(@RequestBody Map<String, Object> body) {
        Authz.requireRole("ADMIN", "MANAGER");
        if (body.containsKey("type")) put(K_TYPE, str(body.get("type")));
        if (body.containsKey("anim")) put(K_ANIM, str(body.get("anim")));
        if (body.containsKey("size")) put(K_SIZE, str(body.get("size")));
        if (body.containsKey("speed")) put(K_SPEED, str(body.get("speed")));
        if (body.containsKey("text")) put(K_TEXT, str(body.get("text")));
        if (body.containsKey("round")) put(K_ROUND, str(body.get("round")));
        if (body.containsKey("ring")) put(K_RING, str(body.get("ring")));
        if (body.containsKey("bar")) put(K_BAR, str(body.get("bar")));
        return get();
    }

    /** Upload a custom loader image to the external folder; switches type -> image. */
    @PostMapping("/api/loader/image")
    @Transactional(transactionManager = "commonTransactionManager")
    public Map<String, Object> uploadImage(@RequestParam("file") MultipartFile file) throws Exception {
        Authz.requireRole("ADMIN", "MANAGER");
        if (file == null || file.isEmpty()) throw new IllegalStateException("Please choose an image.");
        String companyCode = TenantContext.getCompanyCode();
        String ext = ext(file.getOriginalFilename());
        String filename = "loader-" + UUID.randomUUID().toString().replace("-", "") + (ext.isEmpty() ? "" : "." + ext);

        String old = map().get(K_IMAGE);
        if (old != null && !old.isBlank()) storage.delete(companyCode, old);   // replace the previous one

        String stored = storage.store(companyCode, filename, file.getBytes());
        put(K_IMAGE, stored);
        put(K_TYPE, "image");
        return get();
    }

    /** Remove the custom image and revert to the spinner. */
    @DeleteMapping("/api/loader/image")
    @Transactional(transactionManager = "commonTransactionManager")
    public Map<String, Object> deleteImage() {
        Authz.requireRole("ADMIN", "MANAGER");
        String old = map().get(K_IMAGE);
        if (old != null && !old.isBlank()) storage.delete(TenantContext.getCompanyCode(), old);
        put(K_IMAGE, "");
        put(K_TYPE, "spinner");
        return get();
    }

    /** PUBLIC image stream — an <img> can load it with no Bearer header. */
    @GetMapping("/api/public/loader-image")
    public ResponseEntity<byte[]> image(@RequestParam String companyCode, @RequestParam String file) {
        byte[] data;
        try {
            data = storage.load(companyCode, file);
        } catch (Exception e) {
            // Missing file (e.g. config points to an image not in THIS instance's
            // storage folder) -> 404, not 500. The client falls back to the spinner.
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(contentType(file))
                .cacheControl(CacheControl.noCache())
                .body(data);
    }

    // ---- helpers ------------------------------------------------------------
    private Map<String, String> map() {
        return settings.all();
    }

    private void put(String key, String value) {
        settings.put(key, value);
    }

    private static String str(Object o) { return o == null ? "" : o.toString(); }
    private static String orDefault(String v, String d) { return (v == null || v.isBlank()) ? d : v; }
    private static int parseInt(String s, int def) {
        try { return (s == null || s.isBlank()) ? def : Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }
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
