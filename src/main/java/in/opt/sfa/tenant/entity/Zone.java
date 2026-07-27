package in.opt.sfa.tenant.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "zone")
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
}
