package in.opt.sfa.tenant.master.imagetype.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ImageTypeMasterDto {
    private Long id;
    private String imageTypeCode;
    private String imageTypeName;
    private String shortName;
    private String description;
    private Integer displayOrder;
    private Boolean isActive;

    private String createdBy;
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDateTime createdAt;
    private String updatedBy;
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDateTime updatedAt;
}
