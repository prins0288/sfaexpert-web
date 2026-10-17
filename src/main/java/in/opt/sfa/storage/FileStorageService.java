package in.opt.sfa.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Stores uploaded files on disk under an EXTERNAL base folder (app.storage.base-dir
 * in application.yml), organised per tenant: {@code <base-dir>/<companyCode>/<filename>}.
 *
 * The base folder lives outside the app so uploads survive redeploys and can be
 * backed up / mounted independently. Every tenant and file name is sanitised to a
 * plain name (no separators, no "..") and the resolved path is verified to stay
 * inside the tenant folder, so a crafted name can never escape the base dir.
 */
@Component
@Slf4j
public class FileStorageService {

    private final Path baseDir;

    public FileStorageService(@Value("${app.storage.base-dir:./sfa-storage}") String base) {
        this.baseDir = Paths.get(base).toAbsolutePath().normalize();
        try {
            Files.createDirectories(baseDir);
            log.info("File storage base dir: {}", baseDir);
        } catch (IOException e) {
            log.warn("Could not create storage base dir {}: {}", baseDir, e.getMessage());
        }
    }

    /** Save bytes as {@code <base>/<companyCode>/<filename>}; returns the stored file name. */
    public String store(String companyCode, String filename, byte[] data) throws IOException {
        Path dir = companyCodeDir(companyCode);
        Files.createDirectories(dir);
        Path target = resolveInside(dir, filename);
        Files.write(target, data);
        return target.getFileName().toString();
    }

    /**
     * Save bytes as {@code <base>/<companyCode>/<folder>/<filename>} where folder
     * is the module, optionally with one sub-folder — e.g. "brandMaster" or
     * "brandMaster/thumbnail". Returns the stored RELATIVE path
     * ("brandMaster/thumbnail/abc_thumb.jpg"), which load() / delete() accept as-is.
     */
    public String storeIn(String companyCode, String folder, String filename, byte[] data) throws IOException {
        Path root = companyCodeDir(companyCode);
        Path dir = root;
        for (String seg : segments(folder, 2)) dir = dir.resolve(safeName(seg));
        dir = dir.normalize();
        if (!dir.startsWith(root)) throw new IOException("Illegal folder: " + folder);
        Files.createDirectories(dir);
        Path target = dir.resolve(safeName(filename)).normalize();
        if (!target.startsWith(dir)) throw new IOException("Illegal path: " + filename);
        Files.write(target, data);
        return root.relativize(target).toString().replace('\\', '/');
    }

    /** Read the bytes of {@code <base>/<companyCode>/<filename>} — filename may be "folder/file". */
    public byte[] load(String companyCode, String filename) throws IOException {
        Path target = resolveInside(companyCodeDir(companyCode), filename);
        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            throw new IOException("File not found: " + filename);
        }
        return Files.readAllBytes(target);
    }

    /** Delete a file if present (never throws). */
    public void delete(String companyCode, String filename) {
        if (filename == null || filename.isBlank()) return;
        try {
            Files.deleteIfExists(resolveInside(companyCodeDir(companyCode), filename));
        } catch (Exception ignored) { /* best-effort */ }
    }

    // ---- internals ----------------------------------------------------------
    private Path companyCodeDir(String companyCode) {
        return baseDir.resolve(safeName(companyCode)).normalize();
    }

    /**
     * Resolve a relative file path inside dir and confirm it did not escape it.
     * Accepts a plain name or up to two folders ("brandMaster/thumbnail/x.jpg");
     * every segment is reduced to a safe single name, so "../" or absolute paths
     * can never get out of the tenant folder.
     */
    private Path resolveInside(Path dir, String filename) throws IOException {
        String[] parts = segments(filename, 3);
        if (parts.length == 0) throw new IOException("Illegal path: " + filename);
        Path target = dir;
        for (String seg : parts) target = target.resolve(safeName(seg));
        target = target.normalize();
        if (!target.startsWith(dir) || target.equals(dir)) throw new IOException("Illegal path: " + filename);
        return target;
    }

    /** Non-blank "/"-separated segments, at most max of them (more -> rejected). */
    private static String[] segments(String path, int max) throws IOException {
        if (path == null) return new String[0];
        String[] raw = path.replace('\\', '/').split("/");
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String r : raw) if (!r.isBlank()) out.add(r);
        if (out.size() > max) throw new IOException("Illegal path: " + path);
        return out.toArray(new String[0]);
    }

    /** Reduce any input to a single safe path segment (defends against traversal). */
    private static String safeName(String name) {
        if (name == null || name.isBlank()) return "_";
        String base = Paths.get(name).getFileName().toString();   // strip any directory parts
        String cleaned = base.replaceAll("[^A-Za-z0-9._-]", "_");
        return cleaned.isBlank() ? "_" : cleaned;
    }
}
