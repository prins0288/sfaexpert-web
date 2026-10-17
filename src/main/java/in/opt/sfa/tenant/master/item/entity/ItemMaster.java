package in.opt.sfa.tenant.master.item.entity;

import in.opt.sfa.security.UserContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Item master (TENANT db, item_master): one row per product — the same
 * item_code in every state. State / date-wise rates live in item_rate and the
 * salts in item_composition; composition_text is rebuilt from those on save.
 */
@Entity
@Table(name = "item_master")
@Getter
@Setter
public class ItemMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_code", nullable = false, length = 50)
    private String itemCode;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(name = "short_name", length = 100)
    private String shortName;

    /** -> item_type_master.id (Product / Sample / Gift / Input ...). */
    @Column(name = "item_type_id", nullable = false)
    private Long itemTypeId;

    /** -> division_master.oid. */
    @Column(name = "division_id")
    private Long divisionId;

    /** -> brand_master.id. */
    @Column(name = "brand_id")
    private Long brandId;

    @Column(name = "item_class", length = 100)
    private String itemClass;

    @Column(length = 150)
    private String manufacturer;

    @Column(name = "pack_size", length = 50)
    private String packSize;

    @Column(name = "sample_pack_size", length = 50)
    private String samplePackSize;

    @Column(length = 20)
    private String uom;

    @Column(name = "hsn_code", length = 8)
    private String hsnCode;

    @Column(name = "gst_rate", precision = 5, scale = 2)
    private BigDecimal gstRate;

    /** "Paracetamol 500 mg + Caffeine 30 mg" — rebuilt from item_composition on save. */
    @Column(name = "composition_text", length = 1000)
    private String compositionText;

    // ---- optional image (user's own file name kept, see FileUploadService.uploadKeepingName) ----
    @Column(name = "image_path", length = 255)
    private String imagePath;

    @Column(name = "image_type", length = 100)
    private String imageType;

    @Column(name = "image_size")
    private Long imageSize;

    @Column(name = "image_name", length = 255)
    private String imageName;

    @Column(name = "thumbnail_path", length = 255)
    private String thumbnailPath;

    @Column(name = "thumbnail_type", length = 100)
    private String thumbnailType;

    // ---- kept from the 2012 item_details table ----
    @Column(name = "item_code_main", length = 100)
    private String itemCodeMain;

    @Column(name = "legacy_set_id", length = 100)
    private String legacySetId;

    @Column(name = "legacy_new_set_id", length = 100)
    private String legacyNewSetId;

    /** TINYINT(1): true = active. */
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

    /** Optimistic lock. Must start null: Spring Data treats a non-null version as an existing row (merge, not insert). */
    @Version
    @Column(nullable = false)
    private Integer version;

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

    static String currentUser() {
        UserContext.CurrentUser u = UserContext.get();
        return (u == null || u.empId() == null) ? "system" : u.empId();
    }
}
