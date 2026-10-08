package in.opt.sfa.tenant.master.route.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Formula;

import java.math.BigDecimal;

@Entity
@Table(name = "route")
@Getter
@Setter
public class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oid;

    @Column(name = "route_code")
    private String routeCode;

    @Column(name = "route_name")
    private String routeName;

    @Column(name = "division_oid")
    private Long divisionOid;

    @Column(name = "zone_oid")
    private Long zoneOid;

    @Column(name = "state_oid")
    private Long stateOid;

    @Column(name = "hq_oid")
    private Long hqOid;

    @Column(name = "distance_km")
    private BigDecimal distanceKm;

    private String description;

    private String status = "Y";

    // ---- display-only (joined names, like the websfa Dao SELECT) ----
    @Formula("(select d.division_name from division_master d where d.oid = division_oid)")
    private String divisionName;

    @Formula("(select z.zone_name from zone_master z where z.oid = zone_oid)")
    private String zoneName;

    @Formula("(select s.state_name from state_master s where s.oid = state_oid)")
    private String stateName;

    @Formula("(select h.hq_name from hq_master h where h.oid = hq_oid)")
    private String hqName;
}
