package in.opt.sfa.storage;

/**
 * What FileUploadService.upload() saved. Paths are relative to the tenant's
 * storage folder ("brandMaster/ab12.png", "brandMaster/thumbnail/ab12_thumb.jpg")
 * and are what a table stores; types are plain-text MIME types ("image/png").
 * Thumbnail fields are null for a non-image (or an image that cannot be decoded).
 * size = bytes stored; originalSize = bytes uploaded (differs only when the
 * lossless compression made the file smaller — then compressed = true).
 */
public record StoredFile(
        String path,
        String type,
        long size,
        long originalSize,
        boolean compressed,
        String originalName,
        Integer width,
        Integer height,
        String thumbnailPath,
        String thumbnailType) {
}
