package in.opt.sfa.tenant.master.designation.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DesignationMasterDto {
    private Long oid;
    private String designationCode;
    private String designationName;
    /** Hierarchy level; copied onto every employee of this designation and decides their role. */
    private Integer empLevel;
    private Boolean status;

    // ---- read-only, filled by the server ----
    /** Role this level gives (RoleLevelMapper): USER / MANAGER / ADMIN / SUPER_ADMIN. */
    private String role;
    /** Employees currently holding this designation. */
    private Long employeeCount;
}
