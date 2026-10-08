package in.opt.sfa.tenant.master.area.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Formula;

import java.math.BigDecimal;

@Entity
@Table(name = "city_master")
@Getter
@Setter
public class Area {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oid;

    @Column(name = "area_code")
    private String areaCode;

    @Column(name = "area_name")
    private String areaName;

    private String city;

    @Column(name = "state_oid")
    private Long stateOid;

    @Column(name = "hq_oid")
    private Long hqOid;

    private String pincode;

    @Column(name = "area_type")
    private String areaType;

    @Column(name = "distance_km")
    private BigDecimal distanceKm;

    private String status = "Y";

    // ---- display-only ----
    @Formula("(select s.state_name from state_master s where s.oid = state_oid)")
    private String stateName;

    @Formula("(select h.hq_name from hq_master h where h.oid = hq_oid)")
    private String hqName;

    /**
     * Comma-joined route names mapped to this area. Populated by AreaService
     * (not a @Formula: a correlated subquery on the area's own `oid` would be
     * ambiguous with route_area_map.oid / route.oid).
     */
    @Transient
    private String routes;
}
