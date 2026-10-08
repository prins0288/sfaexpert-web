package in.opt.sfa.tenant.master.permission.entity;

import in.opt.sfa.security.UserContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * permission_assignment (TENANT db) — grants or denies one permission_code to
 * one target: a single emp_id, a whole designation, or a whole emp_level.
 * No row for a given (target, permission_code) means "not configured" — the
 * resolver then falls back to allow-by-default (see PermissionService).
 */
@Entity
@Table(name = "permission_assignment",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_permission_assignment_target",
                columnNames = {"target_type", "target_value", "permission_code"}))
@Getter
@Setter
public class PermissionAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oid;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private PermissionTargetType targetType;

    /** emp_id, designation_code, or emp_level (as a string), depending on targetType. */
    @Column(name = "target_value", nullable = false, length = 100)
    private String targetValue;

    @Column(name = "permission_code", nullable = false, length = 100)
    private String permissionCode;

    @Column(nullable = false)
    private Boolean allowed = true;

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
        if (this.allowed == null) this.allowed = true;
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
