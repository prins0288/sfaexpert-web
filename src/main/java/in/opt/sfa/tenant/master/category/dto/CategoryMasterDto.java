package in.opt.sfa.tenant.master.category.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CategoryMasterDto {
    private Long oid;
    private String categoryCode;
    private String categoryName;
    private String description;
    private String icon;
    private Boolean status;

    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDateTime createdAt;
    private String createdBy;
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDateTime updatedAt;
    private String updatedBy;
}
