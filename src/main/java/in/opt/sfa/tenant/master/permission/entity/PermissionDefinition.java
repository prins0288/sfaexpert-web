package in.opt.sfa.tenant.master.permission.entity;

import in.opt.sfa.security.UserContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * permission_master (TENANT db) — the catalog of permission codes this company
 * can assign (e.g. "USER_CREATE", "EXPENSE_APPROVE"). This table only names the
 * permissions; who actually has them is decided by PermissionAssignment rows.
 */
@Entity
@Table(name = "permission_master",
        uniqueConstraints = @UniqueConstraint(name = "uq_permission_master_code", columnNames = "permission_code"))
@Getter
@Setter
public class PermissionDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "permission_code", nullable = false, length = 100)
    private String permissionCode;

    @Column(length = 100)
    private String module;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private Boolean status = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", updatable = false, length = 128)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 128)
    private String updatedBy;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        String who = currentUser();
        this.createdBy = who;
        this.updatedBy = who;
        if (this.status == null) this.status = true;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = currentUser();
    }

    private static String currentUser() {
        UserContext.CurrentUser u = UserContext.get();
        return (u == null || u.empId() == null) ? "system" : u.empId();
    }
}
