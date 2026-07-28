package in.opt.sfa.tenant.entity;

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

    /** Soft-delete flag: 'Y' active, 'N' inactive. */
    @Column(length = 1, nullable = false)
    @Schema(description = "Soft-delete flag: Y = active, N = inactive", example = "Y")
    private String status = "Y";

    // ---- audit (server-managed; read-only to clients) ----------------------
    @Column(name = "created_at", updatable = false)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Column(name = "created_by", updatable = false, length = 128)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String createdBy;

    @Column(name = "updated_at")
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
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
        if (this.status == null || this.status.isBlank()) this.status = "Y";
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = currentUser();
    }

    private static String currentUser() {
        UserContext.CurrentUser u = UserContext.get();
        return (u == null || u.username() == null) ? "system" : u.username();
    }
}
