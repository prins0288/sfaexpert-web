package in.opt.sfa.tenant.master.routearea.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RouteAreaMapDto {
    private Long oid;
    private Long routeOid;
    private Long areaOid;
    private Integer visitSequence;
    private String status;

    private String routeName;
    private String routeCode;
    private String areaName;
    private String areaCode;
}
