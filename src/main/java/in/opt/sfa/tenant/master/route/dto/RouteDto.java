package in.opt.sfa.tenant.master.route.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RouteDto {
    private Long oid;
    private String routeCode;
    private String routeName;
    private Long divisionOid;
    private Long zoneOid;
    private Long stateOid;
    private Long hqOid;
    private BigDecimal distanceKm;
    private String description;
    private String status;

    private String divisionName;
    private String zoneName;
    private String stateName;
    private String hqName;
}
