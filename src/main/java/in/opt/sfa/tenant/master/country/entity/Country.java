package in.opt.sfa.tenant.master.country.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Country master — states are linked to a country, which drives country-specific report rules (e.g. Nepal DCR date). */
@Entity
@Table(name = "country_master")
@Getter
@Setter
public class Country {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oid;

    @Column(name = "country_code")
    private String countryCode;

    @Column(name = "country_name")
    private String countryName;

    /** Active flag stored as TINYINT(1): true = 1 (active), false = 0 (inactive). */
    @Column(nullable = false)
    private Boolean status = true;
}
