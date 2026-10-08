package in.opt.sfa.tenant.master.permission.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import in.opt.sfa.tenant.master.permission.entity.PermissionTargetType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PermissionAssignmentDto {
    private Long oid;
    private PermissionTargetType targetType;
    /** emp_id, designation_code, or emp_level (as a string) depending on targetType. */
    private String targetValue;
    private String permissionCode;
    private Boolean allowed;

    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDateTime createdAt;
    private String createdBy;
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDateTime updatedAt;
    private String updatedBy;
}
