package in.opt.sfa.tenant.master.zone.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Formula;

@Entity
@Table(name = "zone_master")
@Getter
@Setter
public class Zone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oid;

    @Column(name = "zone_code")
    private String zoneCode;

    @Column(name = "zone_name")
    private String zoneName;

    @Column(name = "division_oid")
    private Long divisionOid;

    private String status = "Y";

    // ---- display-only ----
    @Formula("(select d.division_name from division_master d where d.oid = division_oid)")
    private String divisionName;
}
