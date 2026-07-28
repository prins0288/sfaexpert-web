package in.opt.sfa.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * App-wide branding (COMMON db) — a single row (id = 1) holding the application
 * name and the DEFAULT logo used when a tenant has not set its own company logo.
 * The logo is a base64 data URI.
 */
@Entity
@Table(name = "app_branding")
@Getter
@Setter
public class AppBranding {

    @Id
    private Integer id = 1;

    @Column(name = "app_name", nullable = false, length = 128)
    private String appName = "StarSFA";

    @Column(name = "default_logo", columnDefinition = "MEDIUMTEXT")
    private String defaultLogo;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 128)
    private String updatedBy;
}
