package in.opt.sfa.tenant.master.employee.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Formula;

import java.time.LocalDate;

/**
 * The user's FULL profile — lives in the TENANT database. The common
 * app_user row (credentials) links here via emp_id. Name, state, district,
 * designation, dob etc. are all here, not in the common DB.
 */
@Entity
@Table(name = "employee")
@Getter
@Setter
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oid;

    @Column(name = "emp_id")
    private String empId;

    @Column(name = "emp_name")
    private String empName;

    private String username;

    /**
     * Legacy NOT-NULL column from the websfa schema. Authentication happens
     * against the common app_user, not here, so we just keep it non-null ("").
     */
    @Column(name = "password_hash")
    private String passwordHash = "";

    @Column(name = "designation_oid")
    private Long designationOid;

    @Column(name = "emp_level")
    private Integer empLevel;

    private String mobile;
    private String email;

    @Column(name = "state_oid")
    private Long stateOid;

    @Column(name = "district_oid")
    private Long districtOid;

    private LocalDate dob;

    /** Profile photo as a base64 data URI (e.g. "data:image/png;base64,..."). */
    @Column(columnDefinition = "MEDIUMTEXT")
    private String photo;

    private String status = "Y";

    // ---- display-only (joined names) ----
    @Formula("(select d.designation_name from designation_master d where d.oid = designation_oid)")
    private String designationName;

    @Formula("(select s.state_name from state_master s where s.oid = state_oid)")
    private String stateName;

    @Formula("(select di.district_name from district di where di.oid = district_oid)")
    private String districtName;
}
