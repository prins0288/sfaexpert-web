package in.opt.sfa.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Lives in the COMMON database. Holds login credentials and which tenant the
 * user belongs to. On login we read tenantId from here and route accordingly.
 */
@Entity
@Table(name = "app_user")
@Getter
@Setter
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    /** BCrypt-hashed password. */
    @Column(nullable = false)
    private String password;

    /** FK-style link to TenantConfig.tenantId. */
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /** Auth role kept in the common DB (ADMIN / MANAGER / USER). */
    @Column(nullable = false)
    private String role = "USER";

    /**
     * Link to the user's full profile row in the TENANT database
     * (employee.emp_id). Credentials live here (common); name/state/district/
     * designation/dob etc. live in the tenant's employee table.
     */
    @Column(name = "emp_id")
    private String empId;

    @Column(nullable = false)
    private boolean enabled = true;
}
