package in.opt.sfa.tenant.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Per-tenant translation OVERRIDE (TENANT db). The base translations ship in
 * the app's i18n/lang_{code}.properties files; a row here overrides ONE
 * (label_key, lang) pair for this tenant only — so a company can reword any
 * label (e.g. call a "Client" a "Customer") without affecting other tenants
 * or the shipped defaults. Unique on (label_key, lang); status 'Y'/'N'.
 */
@Entity
@Table(name = "label_master",
        uniqueConstraints = @UniqueConstraint(name = "uq_label", columnNames = {"label_key", "lang"}))
@Getter
@Setter
public class LabelMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oid;

    @Column(name = "label_key", nullable = false, length = 120)
    private String labelKey;

    @Column(name = "label_value", nullable = false, length = 400)
    private String labelValue;

    @Column(nullable = false, length = 10)
    private String lang = "en";

    @Column(nullable = false, length = 1)
    private String status = "Y";

    @Column(name = "updated_on")
    private LocalDateTime updatedOn;
}
