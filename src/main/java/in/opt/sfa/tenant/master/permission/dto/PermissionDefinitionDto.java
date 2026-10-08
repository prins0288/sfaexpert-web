package in.opt.sfa.tenant.master.permission.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PermissionDefinitionDto {
    private Long oid;
    private String permissionCode;
    private String module;
    private String description;
    private Boolean status;

    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDateTime createdAt;
    private String createdBy;
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDateTime updatedAt;
    private String updatedBy;
}
