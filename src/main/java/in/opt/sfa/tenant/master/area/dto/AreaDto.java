package in.opt.sfa.tenant.master.area.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AreaDto {
    private Long oid;
    private String areaCode;
    private String areaName;
    private String city;
    private Long stateOid;
    private Long hqOid;
    private String pincode;
    private String areaType;
    private BigDecimal distanceKm;
    private String status;

    private String stateName;
    private String hqName;
    private String routes;
}
