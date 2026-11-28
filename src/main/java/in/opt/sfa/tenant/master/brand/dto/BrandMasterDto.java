package in.opt.sfa.tenant.master.brand.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class BrandMasterDto {
    private Long id;
    private String brandCode;
    private String brandName;
    private Boolean status;
    /** On save: true = drop the uploaded image and go back to the default picture (ignored when a new image is uploaded). */
    private Boolean removeImage;
    /** On save with a new image: true = lossless compression (PNG/BMP only; never lowers quality). */
    private Boolean compressImage;

    // ---- read-only: image as stored (relative paths, MIME types as text) ----
    private String imagePath;
    private String imageType;
    private Long imageSize;
    private String imageName;
    private String thumbnailPath;
    private String thumbnailType;
    /** true = no image was uploaded; the brand shows its own copy of the default "no image" picture. */
    private Boolean imageIsDefault;

    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDateTime createdAt;
    private String createdBy;
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDateTime updatedAt;
    private String updatedBy;
}
