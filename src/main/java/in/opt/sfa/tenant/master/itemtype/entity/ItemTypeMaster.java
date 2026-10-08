package in.opt.sfa.tenant.master.itemtype.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import in.opt.sfa.security.UserContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Item-type master (TENANT db) — a product dosage-form / packaging lookup
 * (e.g. Tablet, Capsule, Syrup, Injection) with a short name, a display order
 * and full audit columns.
 *
 * Audit "who" comes from the verified JWT (UserContext), falling back to
 * "system" for non-request writes; the timestamps are stamped by the JPA
 * lifecycle callbacks below (the table also has DB-level CURRENT_TIMESTAMP
 * defaults as a safety net). Never trusted from the client.
 */
@Entity
@Table(name = "item_type_master",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_item_type_code", columnNames = "item_type_code"),
                @UniqueConstraint(name = "uk_item_type_name", columnNames = "item_type_name")
        })
@Getter
@Setter
public class ItemTypeMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Primary key", accessMode = Schema.AccessMode.READ_ONLY, example = "1")
    private Long id;

    @Column(name = "item_type_code", nullable = false, length = 30)
    @Schema(description = "Unique short code", example = "TAB")
    private String itemTypeCode;

    @Column(name = "item_type_name", nullable = false, length = 100)
    @Schema(description = "Unique display name", example = "Tablet")
    private String itemTypeName;

    @Column(name = "short_name", length = 30)
    @Schema(description = "Optional abbreviation", example = "Tab")
    private String shortName;

    @Column(length = 500)
    @Schema(description = "Optional description")
    private String description;

    /** Sort order among item types (ascending); 0 by default. */
    @Column(name = "display_order", nullable = false)
    @Schema(description = "Sort order (ascending)", example = "10")
    private Integer displayOrder = 0;

    /** Active flag stored as TINYINT(1): true = 1 (active), false = 0 (inactive). */
    @Column(name = "is_active", nullable = false)
    @Schema(description = "Active flag: true = active, false = inactive", example = "true")
    private Boolean isActive = true;

    // ---- audit (server-managed; read-only to clients) ----------------------
    @Column(name = "created_by", updatable = false, length = 100)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String createdBy;

    @Column(name = "created_at", updatable = false)
    @JsonFormat(pattern = "dd-MM-yyyy")
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, type = "string", example = "04-08-2026")
    private LocalDateTime createdAt;

    @Column(name = "updated_by", length = 100)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String updatedBy;

    @Column(name = "updated_at")
    @JsonFormat(pattern = "dd-MM-yyyy")
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, type = "string", example = "04-08-2026")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        String who = currentUser();
        this.createdBy = who;
        this.updatedBy = who;
        if (this.isActive == null) this.isActive = true;
        if (this.displayOrder == null) this.displayOrder = 0;
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
