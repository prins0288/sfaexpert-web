package in.opt.sfa.tenant.master.speciality.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import in.opt.sfa.security.UserContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Speciality master (TENANT db) — same production shape as CategoryMaster:
 * unique code, name, description, icon, a boolean (TINYINT 1/0) soft-delete
 * status, and audit columns filled automatically by the JPA lifecycle callbacks
 * (who = the verified JWT user, "system" fallback). Dates serialize as dd-MM-yyyy.
 */
@Entity
@Table(name = "speciality_master",
        uniqueConstraints = @UniqueConstraint(name = "uq_speciality_master_code", columnNames = "speciality_code"))
@Getter
@Setter
public class SpecialityMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Primary key", accessMode = Schema.AccessMode.READ_ONLY, example = "1")
    private Long oid;

    @Column(name = "speciality_code", nullable = false, length = 64)
    @Schema(description = "Unique short code", example = "SPEC-CARD")
    private String specialityCode;

    @Column(name = "speciality_name", nullable = false, length = 128)
    @Schema(description = "Display name", example = "Cardiology")
    private String specialityName;

    @Column(length = 255)
    @Schema(description = "Optional description", example = "Heart specialists")
    private String description;

    @Column(length = 128)
    @Schema(description = "Bootstrap-icon name (no 'bi-' prefix) or an image URL", example = "heart-pulse")
    private String icon;

    /** Active flag stored as TINYINT(1): true = 1 (active), false = 0 (inactive). */
    @Column(nullable = false)
    @Schema(description = "Active flag: true = active, false = inactive", example = "true")
    private Boolean status = true;

    // ---- audit (server-managed; read-only; dates as dd-MM-yyyy) -------------
    @Column(name = "created_at", updatable = false)
    @JsonFormat(pattern = "dd-MM-yyyy")
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, type = "string", example = "28-07-2026")
    private LocalDateTime createdAt;

    @Column(name = "created_by", updatable = false, length = 128)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String createdBy;

    @Column(name = "updated_at")
    @JsonFormat(pattern = "dd-MM-yyyy")
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, type = "string", example = "28-07-2026")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 128)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
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
