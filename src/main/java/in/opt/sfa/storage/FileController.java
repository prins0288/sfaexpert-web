package in.opt.sfa.storage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Serves any file stored through FileUploadService — the current tenant's
 * files only (the tenant comes from the JWT; paths cannot leave its folder).
 * Needs the normal Bearer token: the front-end fetches it as a blob
 * (SFAFiles in app.js) instead of a bare <img src>.
 */
@RestController
@RequestMapping("/api/files")
@Tag(name = "Files", description = "View / download uploaded files of the current company")
public class FileController {

    private final FileUploadService uploads;

    public FileController(FileUploadService uploads) {
        this.uploads = uploads;
    }

    @GetMapping
    @Operation(summary = "Stream a stored file",
            description = "path = the relative path saved in the table (e.g. brandMaster/ab12.png). "
                    + "download=true sends it as an attachment named `name`.")
    public ResponseEntity<byte[]> file(@RequestParam String path,
                                       @RequestParam(defaultValue = "false") boolean download,
                                       @RequestParam(required = false) String name) {
        byte[] data;
        try {
            data = uploads.read(path);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
        String type = FileUploadService.typeOfPath(path);
        String fileName = (name == null || name.isBlank()) ? path.substring(path.lastIndexOf('/') + 1) : name;
        ContentDisposition cd = (download ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(fileName, StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(type))
                .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .header("X-Content-Type-Options", "nosniff")
                // stored names are random UUIDs, never overwritten -> safe to cache privately
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePrivate())
                .body(data);
    }
}
