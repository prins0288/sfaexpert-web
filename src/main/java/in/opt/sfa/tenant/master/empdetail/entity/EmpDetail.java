package in.opt.sfa.tenant.master.empdetail.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Core employee profile (TENANT db). The business key emp_id also matches
 * AppUser.empId / the JWT emp_id claim (COMMON db, user_login_master) — the
 * login row is kept in sync by EmpDetailService via AppUserAdminService.
 */
@Entity
@Table(name = "emp_detail")
@Getter
@Setter
public class EmpDetail {

    @Id
    @Column(name = "emp_id", length = 40)
    private String empId;

    @Column(name = "emp_code", nullable = false, length = 30)
    private String empCode;

    @Column(name = "emp_name", nullable = false, length = 150)
    private String empName;

    /** NULL for a company-wide role (e.g. Super Admin) that isn't tied to one division. */
    @Column(name = "division_id")
    private Long divisionId;

    @Column(name = "state_id")
    private Long stateId;

    @Column(name = "hq_id")
    private Long hqId;

    /** -> designation_master.id. Column is literally named "designation" in the DB; the
     *  employee's level and role come from the designation (no level is stored here). */
    @Column(name = "designation")
    private Integer designationId;

    /** Immediate manager -> emp_detail.emp_id. */
    @Column(name = "manager_id", length = 40)
    private String managerId;

    private String gender;

    @Column(name = "date_of_joining")
    private LocalDate dateOfJoining;

    @Column(name = "reporting_date")
    private LocalDate reportingDate;

    private String mobile;

    @Column(name = "official_email")
    private String officialEmail;

    private String department;

    @Column(name = "is_office_staff")
    private boolean officeStaff;

    @Column(name = "is_active")
    private boolean active = true;

    @Column(name = "is_confirmed")
    private boolean confirmed;

    @Column(name = "confirmation_date")
    private LocalDate confirmationDate;

    @Column(name = "resignation_date")
    private LocalDate resignationDate;

    @Column(name = "last_working_date")
    private LocalDate lastWorkingDate;

    /** Text from salutation_master, e.g. "Mr." (the value itself, not an id). */
    @Column(name = "salutation", length = 20)
    private String salutation;

    @Column(name = "photo_path")
    private String photoPath;

    /** Thumbnail of a photo uploaded from the employee form (null for a My Profile photo). */
    @Column(name = "photo_thumbnail_path")
    private String photoThumbnailPath;

    /** Vacant position (no person in it): its login is refused (AuthService). */
    @Column(name = "is_vacant", nullable = false)
    private boolean vacant;

    /** True once gender/DOJ/email/department (the "extra" fields) are all filled in. */
    @Column(name = "profile_complete")
    private boolean profileComplete;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @Version
    private Integer version;
}
