package in.opt.sfa.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Lives in the COMMON database. Holds login credentials and which tenant the
 * user belongs to. On login we read companyCode from here and route accordingly.
 */
@Entity
@Table(name = "user_login_master")
@Getter
@Setter
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    /** Plain-text password (stored and compared as-is — no hashing; see AuthService). */
    @Column(nullable = false)
    private String password;

    /** FK-style link to TenantConfig.companyCode. */
    @Column(name = "company_code", nullable = false)
    private String companyCode;

   

    /**
     * Link to the user's full profile row in the TENANT database
     * (employee.emp_id). Credentials live here (common); name/state/district/
     * designation/dob etc. live in the tenant's employee table.
     */
    @Column(name = "emp_id", nullable = false)
    private String empId;

    @Column(nullable = false)
    private boolean isActive = true;

    /** Stamped by AuthService on every successful login (updated AFTER the
     *  previous value is read out and returned to the client as "last login"). */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
}
