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

    /** Read the bytes of {@code <base>/<companyCode>/<filename>}. */
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

    /** Resolve a file name inside dir and confirm it did not escape it. */
    private Path resolveInside(Path dir, String filename) throws IOException {
        Path target = dir.resolve(safeName(filename)).normalize();
        if (!target.startsWith(dir)) throw new IOException("Illegal path: " + filename);
        return target;
    }

    /** Reduce any input to a single safe path segment (defends against traversal). */
    private static String safeName(String name) {
        if (name == null || name.isBlank()) return "_";
        String base = Paths.get(name).getFileName().toString();   // strip any directory parts
        String cleaned = base.replaceAll("[^A-Za-z0-9._-]", "_");
        return cleaned.isBlank() ? "_" : cleaned;
    }
}
