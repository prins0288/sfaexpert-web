package in.opt.sfa.tenant.master.designation.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class DesignationMasterDto {
    private Integer id;
    /** 'MR', 'ABM', 'Dy. RSM' … */
    private String designation;
    /** 10, 15, 20, 32 … — unique, higher = more senior. */
    private Integer hierarchyLevel;
    /** UI sorting (1, 2, 3 …); blank = after the last one. */
    private Integer displayOrder;
    /** Junior / Senior (optional). */
    private String grade;
    /** Field / Manager / Admin / Support (optional). */
    private String designationCategory;
    private Boolean active;

    // ---- read-only, filled by the server ----
    /** Role its hierarchy level gives (LevelRoleService): USER / MANAGER / ADMIN / SUPER_ADMIN. */
    private String role;
    /** Admin / Super Admin designation: cannot be edited or deactivated from the panel. */
    private Boolean locked;
    /** Employees currently holding this designation. */
    private Long employeeCount;
    private LocalDateTime createdAt;
}
