package in.opt.sfa.tenant.master.item.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Item with its rates and composition. On save the rates and composition
 * lists replace what is stored (send the rate id to keep / edit a rate).
 * rates / compositions = null on save means "leave them unchanged".
 */
@Getter
@Setter
public class ItemMasterDto {
    private Long id;
    private String itemCode;
    private String itemName;
    private String shortName;
    private Long itemTypeId;
    private Long divisionId;
    private Long brandId;
    private String itemClass;
    private String manufacturer;
    private String packSize;
    private String samplePackSize;
    private String uom;
    private String hsnCode;
    private BigDecimal gstRate;
    private String itemCodeMain;
    private Boolean status;
    /** On save: true = delete the current image (ignored when a new image is uploaded). */
    private Boolean removeImage;
    /** On save with a new image: true = lossless compression (PNG only; never lowers quality). */
    private Boolean compressImage;
    /** Optimistic lock: send back what was read; a stale value = someone else saved meanwhile. */
    private Integer version;

    private List<ItemRateDto> rates;
    private List<ItemCompositionDto> compositions;

    // ---- read-only ----
    private String itemTypeName;
    private String itemTypeCode;
    private String divisionName;
    private String brandName;
    private String compositionText;
    private String imagePath;
    private String imageType;
    private Long imageSize;
    private String imageName;
    private String thumbnailPath;
    private String thumbnailType;
    private String legacySetId;
    private String legacyNewSetId;
    /** List view: number of rates, and today's All-India rate (else any state's). */
    private Integer rateCount;
    private ItemRateDto currentRate;

    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDateTime createdAt;
    private String createdBy;
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDateTime updatedAt;
    private String updatedBy;
}
