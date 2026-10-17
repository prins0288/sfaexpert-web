package in.opt.sfa.storage;

import in.opt.sfa.tenant.context.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * THE one place every screen uploads a file through (brand logo, item image,
 * employee document, …):
 *
 *   StoredFile f = uploads.upload("brandMaster", file, FileUploadService.Kind.IMAGE);
 *
 * Folder layout, per tenant:
 *   <storage>/<companyCode>/<module>/<uuid>.<ext>                 original
 *   <storage>/<companyCode>/<module>/thumbnail/<uuid>_thumb.<jpg|png>  thumbnail (images)
 *
 * It checks the REAL file type from its first bytes (a renamed .exe is refused
 * even if it ends in .png), enforces a size limit, never trusts the client's
 * file name for the stored name, and for images builds a thumbnail (fits in
 * 240x240, never enlarged; PNG when the image has transparency, else JPEG).
 * Both MIME types are returned as text for the caller to store.
 *
 * Images in any colour space are handled — RGB, CMYK / YCCK JPEGs (read with
 * their colour profile by the TwelveMonkeys plugins on the classpath),
 * grayscale, 16-bit, palette, WebP; thumbnails are always plain RGB(A). An
 * image that still cannot be decoded is stored without a thumbnail, never
 * rejected for that reason.
 *
 * compress = true applies LOSSLESS compression only: PNG re-encoded at maximum
 * deflate, BMP converted to PNG. The result is decoded again and compared
 * pixel by pixel with the original, and kept only when it is identical AND
 * smaller; otherwise the original bytes are stored. JPEG / WebP / GIF are
 * never re-encoded here (that would lose quality or animation).
 */
@Service
@Slf4j
public class FileUploadService {

    public enum Kind { IMAGE, DOCUMENT }

    public static final String THUMB_FOLDER = "thumbnail";
    private static final int THUMB_MAX = 240;
    /** Refuse decoding anything bigger (decompression-bomb guard). */
    private static final long MAX_PIXELS = 50_000_000L;

    private static final Set<String> IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/gif", "image/webp", "image/bmp");
    private static final Set<String> DOCUMENT_TYPES = Set.of("application/pdf", "image/png", "image/jpeg", "image/gif", "image/webp", "image/bmp");

    private final FileStorageService storage;
    private final long maxImageBytes;
    private final long maxDocumentBytes;

    public FileUploadService(FileStorageService storage,
                             @Value("${app.upload.max-image-mb:5}") long maxImageMb,
                             @Value("${app.upload.max-document-mb:20}") long maxDocumentMb) {
        this.storage = storage;
        this.maxImageBytes = maxImageMb * 1024 * 1024;
        this.maxDocumentBytes = maxDocumentMb * 1024 * 1024;
    }

    /** Validate, store and (for images) thumbnail one file for the current tenant. */
    public StoredFile upload(String module, MultipartFile file, Kind kind) throws IOException {
        return upload(module, file, kind, false);
    }

    /** Same, with optional lossless compression (see class comment). */
    public StoredFile upload(String module, MultipartFile file, Kind kind, boolean compress) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalStateException("Please choose a file");
        byte[] data = file.getBytes();
        long max = kind == Kind.IMAGE ? maxImageBytes : maxDocumentBytes;
        if (data.length > max) {
            throw new IllegalStateException("File is too large: " + mb(data.length) + " MB (max " + mb(max) + " MB)");
        }
        String type = detectType(data);
        Set<String> allowed = kind == Kind.IMAGE ? IMAGE_TYPES : DOCUMENT_TYPES;
        if (type == null || !allowed.contains(type)) {
            throw new IllegalStateException(kind == Kind.IMAGE
                    ? "Only PNG, JPG, GIF, WebP or BMP images are allowed"
                    : "Only PDF or image files are allowed");
        }

        long originalSize = data.length;
        BufferedImage img = type.startsWith("image/") ? readImage(data) : null;
        boolean compressed = false;
        if (compress && img != null) {
            byte[] smaller = losslessCompress(type, data);
            if (smaller != null) {
                data = smaller;
                type = "image/png";   // BMP / PNG -> optimised PNG
                compressed = true;
            }
        }

        String company = TenantContext.getCompanyCode();
        String id = UUID.randomUUID().toString().replace("-", "");
        String path = storage.storeIn(company, module, id + "." + extension(type), data);

        Integer width = null, height = null;
        String thumbPath = null, thumbType = null;
        if (img != null) {
            width = img.getWidth();
            height = img.getHeight();
            boolean alpha = img.getColorModel().hasAlpha();
            thumbType = alpha ? "image/png" : "image/jpeg";
            byte[] thumb = encode(scale(img, alpha), alpha);
            thumbPath = storage.storeIn(company, module + "/" + THUMB_FOLDER,
                    id + "_thumb." + (alpha ? "png" : "jpg"), thumb);
        }
        return new StoredFile(path, type, data.length, originalSize, compressed, cleanName(file.getOriginalFilename()),
                width, height, thumbPath, thumbType);
    }

    /** Remove stored files (original and/or thumbnail) of the current tenant; nulls are ignored. */
    public void delete(String... paths) {
        String company = TenantContext.getCompanyCode();
        for (String p : paths) if (p != null && !p.isBlank()) storage.delete(company, p);
    }

    public byte[] read(String path) throws IOException {
        return storage.load(TenantContext.getCompanyCode(), path);
    }

    /** Real MIME type from the file's first bytes (magic numbers), or null if unknown. */
    public static String detectType(byte[] b) {
        if (b == null || b.length < 12) return null;
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "image/png";
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "image/jpeg";
        if (b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') return "image/gif";
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "image/webp";
        if (b[0] == 'B' && b[1] == 'M') return "image/bmp";
        if (b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F') return "application/pdf";
        return null;
    }

    /** MIME type of an already-stored file, from its extension (for serving). */
    public static String typeOfPath(String path) {
        String p = path == null ? "" : path.toLowerCase(Locale.ROOT);
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".jpg") || p.endsWith(".jpeg")) return "image/jpeg";
        if (p.endsWith(".gif")) return "image/gif";
        if (p.endsWith(".webp")) return "image/webp";
        if (p.endsWith(".bmp")) return "image/bmp";
        if (p.endsWith(".pdf")) return "application/pdf";
        return "application/octet-stream";
    }

    // ---- image helpers ------------------------------------------------------

    /**
     * Decode with every reader that claims the format (TwelveMonkeys first for
     * JPEG/WebP, then the JDK's), after checking the declared size — so CMYK,
     * YCCK, grayscale, 16-bit or palette images all decode, and a header can
     * never make us allocate a huge bitmap. Returns null if nothing can decode it.
     */
    private static BufferedImage readImage(byte[] data) {
        try (ImageInputStream probe = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(probe);
            while (readers.hasNext()) {
                ImageReader reader = readers.next();
                try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
                    reader.setInput(in, true, true);
                    long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                    if (pixels > MAX_PIXELS) {
                        throw new IllegalStateException("Image is too large (" + reader.getWidth(0) + "x" + reader.getHeight(0) + ")");
                    }
                    BufferedImage img = reader.read(0);
                    if (img != null) return img;
                } catch (IllegalStateException e) {
                    throw e;
                } catch (Exception e) {
                    log.debug("Reader {} could not decode image: {}", reader.getClass().getSimpleName(), e.getMessage());
                } finally {
                    reader.dispose();
                }
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Could not decode image for thumbnail: {}", e.getMessage());
        }
        return null;   // stored as-is, just without a thumbnail
    }

    /** Images above this are not re-encoded (the pixel-by-pixel check needs memory). */
    private static final long MAX_COMPRESS_PIXELS = 25_000_000L;

    /**
     * Lossless re-encode of PNG / BMP to a maximally deflated PNG. Returns the new
     * bytes only if they decode to EXACTLY the same pixels and are smaller,
     * otherwise null (= keep the original). PNGs carrying an ICC colour profile
     * are left alone so colours can never shift.
     */
    private static byte[] losslessCompress(String type, byte[] data) {
        if (!type.equals("image/png") && !type.equals("image/bmp")) return null;   // JPEG/WebP/GIF: would lose quality / animation
        if (type.equals("image/png") && pngHasChunk(data, "iCCP")) return null;
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(data));
            if (src == null || (long) src.getWidth() * src.getHeight() > MAX_COMPRESS_PIXELS) return null;
            ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
                writer.setOutput(ios);
                ImageWriteParam p = writer.getDefaultWriteParam();
                if (p.canWriteCompressed()) {
                    p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    p.setCompressionQuality(0.0f);   // PNG: 0 = strongest deflate (still lossless)
                }
                writer.write(null, new IIOImage(src, null, null), p);
            } finally {
                writer.dispose();
            }
            byte[] png = out.toByteArray();
            if (png.length >= data.length) return null;
            BufferedImage back = ImageIO.read(new ByteArrayInputStream(png));
            return samePixels(src, back) ? png : null;
        } catch (Exception e) {
            log.warn("Lossless compression skipped: {}", e.getMessage());
            return null;
        }
    }

    /** Every pixel (all channels, full bit depth) identical. */
    private static boolean samePixels(BufferedImage a, BufferedImage b) {
        if (b == null || a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) return false;
        var ra = a.getRaster();
        var rb = b.getRaster();
        if (ra.getNumBands() != rb.getNumBands()) {
            // e.g. palette -> indexed vs expanded: compare the colours the eye sees, ARGB per pixel
            for (int y = 0; y < a.getHeight(); y++) {
                for (int x = 0; x < a.getWidth(); x++) if (a.getRGB(x, y) != b.getRGB(x, y)) return false;
            }
            return true;
        }
        int w = a.getWidth(), bands = ra.getNumBands();
        int[] rowA = new int[w * bands], rowB = new int[w * bands];
        for (int y = 0; y < a.getHeight(); y++) {
            ra.getPixels(0, y, w, 1, rowA);
            rb.getPixels(0, y, w, 1, rowB);
            if (!java.util.Arrays.equals(rowA, rowB)) return false;
        }
        return true;
    }

    /** Does the PNG contain this chunk before its image data? */
    private static boolean pngHasChunk(byte[] d, String name) {
        int i = 8;   // skip the signature
        while (i + 8 <= d.length) {
            int len = ((d[i] & 0xFF) << 24) | ((d[i + 1] & 0xFF) << 16) | ((d[i + 2] & 0xFF) << 8) | (d[i + 3] & 0xFF);
            String type = new String(d, i + 4, 4, java.nio.charset.StandardCharsets.US_ASCII);
            if (type.equals(name)) return true;
            if (type.equals("IDAT") || len < 0) return false;
            i += 12 + len;
        }
        return false;
    }

    /** Fit inside THUMB_MAX x THUMB_MAX keeping the aspect ratio; halving steps keep it sharp. */
    private static BufferedImage scale(BufferedImage src, boolean alpha) {
        int w = src.getWidth(), h = src.getHeight();
        double f = Math.min(1.0, Math.min((double) THUMB_MAX / w, (double) THUMB_MAX / h));
        int tw = Math.max(1, (int) Math.round(w * f)), th = Math.max(1, (int) Math.round(h * f));
        int type = alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage cur = src;
        int cw = w, ch = h;
        do {
            cw = Math.max(tw, cw / 2);
            ch = Math.max(th, ch / 2);
            BufferedImage next = new BufferedImage(cw, ch, type);
            Graphics2D g = next.createGraphics();
            if (!alpha) { g.setColor(Color.WHITE); g.fillRect(0, 0, cw, ch); }   // JPEG has no transparency
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(cur, 0, 0, cw, ch, null);
            g.dispose();
            cur = next;
        } while (cw != tw || ch != th);
        return cur;
    }

    private static byte[] encode(BufferedImage img, boolean png) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (png) {
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        }
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            ImageWriteParam p = writer.getDefaultWriteParam();
            p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            p.setCompressionQuality(0.85f);
            writer.write(null, new IIOImage(img, null, null), p);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

    private static String extension(String type) {
        return switch (type) {
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            case "image/bmp" -> "bmp";
            case "application/pdf" -> "pdf";
            default -> "bin";
        };
    }

    private static String cleanName(String name) {
        if (name == null || name.isBlank()) return null;
        String base = name.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1);
        return base.length() > 200 ? base.substring(base.length() - 200) : base;
    }

    private static String mb(long bytes) {
        return String.format(Locale.ROOT, "%.1f", bytes / 1024.0 / 1024.0);
    }
}
