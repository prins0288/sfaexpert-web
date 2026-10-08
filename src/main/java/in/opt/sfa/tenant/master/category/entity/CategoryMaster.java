package in.opt.sfa.tenant.master.category.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import in.opt.sfa.security.UserContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Category master (TENANT db) — a production-style master with a soft-delete
 * flag, an icon, and full audit columns.
 *
 * The audit fields are filled automatically by the JPA lifecycle callbacks
 * below (never trusted from the client): created_at/created_by on insert,
 * updated_at/updated_by on every update. "who" comes from the verified JWT
 * (UserContext), falling back to "system" for non-request writes (e.g. seeds).
 */
@Entity
@Table(name = "category_master",
        uniqueConstraints = @UniqueConstraint(name = "uq_category_master_code", columnNames = "category_code"))
@Getter
@Setter
public class CategoryMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Primary key", accessMode = Schema.AccessMode.READ_ONLY, example = "1")
    private Long oid;

    @Column(name = "category_code", nullable = false, length = 64)
    @Schema(description = "Unique short code", example = "CAT-PREM")
    private String categoryCode;

    @Column(name = "category_name", nullable = false, length = 128)
    @Schema(description = "Display name", example = "Premium")
    private String categoryName;

    @Column(length = 255)
    @Schema(description = "Optional description", example = "High-value / key accounts")
    private String description;

    @Column(length = 128)
    @Schema(description = "Bootstrap-icon name (no 'bi-' prefix) or an image URL", example = "star")
    private String icon;

    /** Active flag stored as TINYINT(1): true = 1 (active), false = 0 (inactive). */
    @Column(nullable = false)
    @Schema(description = "Active flag: true = active, false = inactive", example = "true")
    private Boolean status = true;

    // ---- audit (server-managed; read-only to clients) ----------------------
    // Dates are serialized as dd-MM-yyyy so the report shows them in that format.
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
