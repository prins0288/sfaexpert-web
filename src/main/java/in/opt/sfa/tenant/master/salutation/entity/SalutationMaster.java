package in.opt.sfa.tenant.master.salutation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Salutation shown before a name: Dr., Mr., Mrs. ... (TENANT db, salutation_master).
 * Other tables store the chosen TEXT (e.g. client_master.salutation = "Dr."), not this id.
 */
@Entity
@Table(name = "salutation_master")
@Getter
@Setter
public class SalutationMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String salutation;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_by", length = 40, updatable = false)
    private String createdBy;

    @Column(name = "updated_by", length = 40)
    private String updatedBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
