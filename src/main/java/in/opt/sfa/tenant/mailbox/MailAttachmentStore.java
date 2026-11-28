package in.opt.sfa.tenant.mailbox;

import in.opt.sfa.storage.FileStorageService;
import in.opt.sfa.tenant.context.TenantContext;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Files attached while composing: stored under mailbox/<employee>/, size limited, name made safe, programs refused. A path is usable only by the employee who uploaded it. */
@Component
public class MailAttachmentStore {
    private static final Set<String> BLOCKED = Set.of("exe", "bat", "cmd", "com", "scr", "msi", "vbs", "vbe", "js", "jse", "jar", "ps1", "wsf", "hta", "cpl", "lnk", "reg", "dll", "apk");
    private final FileStorageService storage;

    public MailAttachmentStore(FileStorageService storage) { this.storage = storage; }

    /** Made safe: no folders, no control characters or quotes, a sensible length. */
    public static String safeName(String original) {
        String n = original == null ? "file" : original.replace('\\', '/'); n = n.substring(n.lastIndexOf('/') + 1);
        n = n.replaceAll("[\\p{Cntrl}\"<>:|?*]", "_").trim();
        while (n.startsWith(".")) n = n.substring(1);
        if (n.isEmpty()) n = "file";
        if (n.length() > 120) { int dot = n.lastIndexOf('.'); String ext = dot > 0 && n.length() - dot <= 12 ? n.substring(dot) : ""; n = n.substring(0, 120 - ext.length()) + ext; }
        return n;
    }

    public static String ext(String name) { int d = name.lastIndexOf('.'); return d < 0 ? "" : name.substring(d + 1).toLowerCase(Locale.ROOT); }

    public Map<String, Object> upload(String empId, MultipartFile file, int maxMb) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Choose a file");
        if (file.getSize() > (long) maxMb * 1024 * 1024) throw new IllegalArgumentException("The file is bigger than the limit of " + maxMb + " MB");
        String name = safeName(file.getOriginalFilename());
        if (BLOCKED.contains(ext(name))) throw new IllegalArgumentException("Programs and scripts (." + ext(name) + ") cannot be attached");
        byte[] data = file.getBytes();
        String path = storage.storeIn(TenantContext.getCompanyCode(), "mailbox/" + folderOf(empId), UUID.randomUUID().toString().replace("-", "") + "_" + name, data);
        Map<String, Object> m = new LinkedHashMap<>(); m.put("path", path); m.put("name", name); m.put("size", data.length); m.put("type", file.getContentType() == null ? "application/octet-stream" : file.getContentType());
        return m;
    }

    /** The folder name of an employee (safe characters only). */
    public static String folderOf(String empId) { return empId.replaceAll("[^A-Za-z0-9_-]", "_"); }

    public boolean owns(String empId, String path) { return path != null && path.startsWith("mailbox/" + folderOf(empId) + "/") && !path.contains(".."); }

    public byte[] read(String path) throws IOException { return storage.load(TenantContext.getCompanyCode(), path); }

    public void delete(String path) { try { storage.delete(TenantContext.getCompanyCode(), path); } catch (RuntimeException e) { /* already gone */ } }
}
