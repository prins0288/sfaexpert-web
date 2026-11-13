package in.opt.sfa.tenant.master.item.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One rate of an item (TENANT db, item_rate): for one state (null = All India)
 * from effective_from to effective_to (null = still current). The generated
 * column state_key (IFNULL(state_id, 0)) is DB-only and not mapped.
 */
@Entity
@Table(name = "item_rate")
@Getter
@Setter
public class ItemRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    /** -> state_master.oid; null = all states (All India). */
    @Column(name = "state_id")
    private Long stateId;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(precision = 12, scale = 2)
    private BigDecimal pts;

    @Column(precision = 12, scale = 2)
    private BigDecimal ptr;

    @Column(precision = 12, scale = 2)
    private BigDecimal mrp;

    @Column(name = "purchase_rate", precision = 12, scale = 2)
    private BigDecimal purchaseRate;

    @Column(name = "sample_rate", precision = 12, scale = 2)
    private BigDecimal sampleRate;

    @Column(name = "discount_pct", precision = 5, scale = 2)
    private BigDecimal discountPct;

    @Column(name = "tax_included", nullable = false)
    private Boolean taxIncluded = false;

    /** Old item_details.item_newid (one product x state row). */
    @Column(name = "legacy_item_id", length = 255)
    private String legacyItemId;

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
        String who = ItemMaster.currentUser();
        this.createdBy = who;
        this.updatedBy = who;
        if (this.status == null) this.status = true;
        if (this.taxIncluded == null) this.taxIncluded = false;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = ItemMaster.currentUser();
    }
}
