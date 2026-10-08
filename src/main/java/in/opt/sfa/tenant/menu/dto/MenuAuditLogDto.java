package in.opt.sfa.tenant.menu.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MenuAuditLogDto {
    private Long id;
    private Long menuItemId;
    private String label;
    private String menuType;
    private String action;
    private String changedBy;
    /** Read-only: changedBy (emp_id) resolved to the employee's display name. */
    private String changedByName;
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime changedAt;
    private String details;
}
