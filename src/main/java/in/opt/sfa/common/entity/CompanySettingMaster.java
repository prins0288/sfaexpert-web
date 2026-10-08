package in.opt.sfa.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Generic key/value company setting — now in the COMMON database (sfa_central)
 * with a {@code company_code} discriminator, so ONE shared table holds every
 * company's settings. Reads/writes are always scoped to the current tenant by
 * {@link in.opt.sfa.common.service.CompanySettingStore}; a company can never see
 * or overwrite another company's rows (UNIQUE on company_code + setting_key).
 *
 * Schema-less on purpose: a new setting added from any Settings page is just a
 * new key — no migration, no new column.
 */
@Entity
@Table(name = "company_setting_master",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_company_setting_tenant_key",
                columnNames = {"company_code", "setting_key"}))
@Getter
@Setter
public class CompanySettingMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Which company this setting belongs to (TenantConfig.companyCode). */
    @Column(name = "company_code", nullable = false, length = 64)
    private String companyCode;

    @Column(name = "setting_key", nullable = false, length = 64)
    private String settingKey;

    @Column(name = "setting_value", length = 255)
    private String settingValue;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 128)
    private String updatedBy;
}
