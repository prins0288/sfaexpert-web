package in.opt.sfa.tenant.master.empdetail.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Single form for both quick-create and update. On create, only empName,
 * divisionId, designationId, username and password are required ("important
 * data"); the rest are optional and can be filled in later via an update —
 * when they ARE provided, EmpDetailService marks the profile complete.
 */
@Getter
@Setter
public class EmpDetailSaveRequest {
    /** Blank/null => create a new employee (emp_id/emp_code auto-generated). */
    private String empId;

    private String empName;
    private Long divisionId;
    private Long designationId;
    private Long stateId;
    private Long hqId;
    private String managerId;
    private String mobile;

    private String username;
    /** Plain text — stored as-is (no hashing), blank on update keeps the existing one. */
    private String password;

    // ---- optional / "complete profile" fields ----
    private String gender;
    private String dateOfJoining;   // ISO yyyy-MM-dd
    private String reportingDate;   // ISO yyyy-MM-dd
    private String officialEmail;
    private String department;
}
