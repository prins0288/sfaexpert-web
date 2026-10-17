package in.opt.sfa.tenant.master.brand.entity;

import in.opt.sfa.security.UserContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Brand master (TENANT db, brand_master). The brand image is stored through
 * FileUploadService under <storage>/<company>/brandMaster/ (thumbnail in
 * brandMaster/thumbnail/); this row keeps the relative paths and the MIME
 * types as text. Audit columns are filled by the lifecycle callbacks below.
 */
@Entity
@Table(name = "brand_master")
@Getter
@Setter
public class BrandMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "brand_code", nullable = false, length = 30)
    private String brandCode;

    @Column(name = "brand_name", nullable = false, length = 150)
    private String brandName;

    // ---- image (all null when the brand has no image) ----
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
