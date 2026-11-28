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

    /** "Mr.", "Dr." ... from the Salutation master; "" clears it on update, null = unchanged. */
    private String salutation;
    private String empName;
    private Long divisionId;
    private Integer designationId;
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

    // ---- employment status ----
    private Boolean officeStaff;
    /** true = vacant position (login refused), false = working. null on update = unchanged. */
    private Boolean vacant;
    private Boolean confirmed;
    private String confirmationDate;  // ISO yyyy-MM-dd
    private String resignationDate;   // ISO yyyy-MM-dd
    private String lastWorkingDate;   // ISO yyyy-MM-dd

    /** Optional profile sections (address, bank, nominee, …); null = leave them as they are. */
    private EmpProfileDto profile;

    // ---- promotion / transfer (emp_assignment_history) ----
    /** When designation/manager/division/HQ/state change and a reason is not given, it is auto-detected
     *  (PROMOTION / DEMOTION / MANAGER_CHANGE / TRANSFER). To record a change that keeps the same
     *  designation (e.g. a correction), set this explicitly, e.g. "CORRECTION". */
    private String changeReason;
    /** The change takes effect from this date (ISO yyyy-MM-dd); blank/null = today. Applied directly — no approval. */
    private String effectiveDate;
    /** Optional free-text note stored with the history row (e.g. "promoted after Q2 review"). */
    private String changeRemarks;
}
