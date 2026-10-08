package in.opt.sfa.tenant.master.routearea.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Formula;

@Entity
@Table(name = "route_area_map")
@Getter
@Setter
public class RouteAreaMap {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oid;

    @Column(name = "route_oid")
    private Long routeOid;

    @Column(name = "area_oid")
    private Long areaOid;

    @Column(name = "visit_sequence")
    private Integer visitSequence;

    private String status = "Y";

    // ---- display-only ----
    @Formula("(select r.route_name from route r where r.oid = route_oid)")
    private String routeName;

    @Formula("(select r.route_code from route r where r.oid = route_oid)")
    private String routeCode;

    @Formula("(select a.area_name from city_master a where a.oid = area_oid)")
    private String areaName;

    @Formula("(select a.area_code from city_master a where a.oid = area_oid)")
    private String areaCode;
}
