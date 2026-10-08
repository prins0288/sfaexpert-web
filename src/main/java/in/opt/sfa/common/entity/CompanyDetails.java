package in.opt.sfa.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Per-tenant company profile — now in the COMMON database (sfa_central), one row
 * per company keyed by {@code company_code}. Holds the company's details and its
 * logo (base64 data URI), shown in the app chrome. Formerly the per-tenant-db
 * {@code company_profile} (single row id = 1); renamed to {@code company_details}
 * and moved to common so every company's row lives in one place.
 */
@Entity
@Table(name = "company_details")
@Getter
@Setter
public class CompanyDetails {

    /** Tenant this profile belongs to (TenantConfig.companyCode) — the primary key. */
    @Id
    @Column(name = "company_code", length = 64)
    private String companyCode;

    @Column(name = "company_name", length = 160)
    private String companyName;

    @Column(length = 255)
    private String address;

    @Column(length = 96)
    private String city;

    @Column(length = 40)
    private String phone;

    @Column(length = 128)
    private String email;

    @Column(length = 128)
    private String website;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String logo;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 128)
    private String updatedBy;
}
