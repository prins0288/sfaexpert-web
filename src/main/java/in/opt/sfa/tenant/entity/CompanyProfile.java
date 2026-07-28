package in.opt.sfa.tenant.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Per-tenant company profile (TENANT db) — a single row (id = 1) holding the
 * company's details and its logo (base64 data URI). Shown in the app chrome and
 * used in preference to the app-wide default logo.
 */
@Entity
@Table(name = "company_profile")
@Getter
@Setter
public class CompanyProfile {

    @Id
    private Integer id = 1;

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
