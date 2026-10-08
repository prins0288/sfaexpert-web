package in.opt.sfa.tenant.master.zone.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ZoneDto {
    private Long oid;
    private String zoneCode;
    private String zoneName;
    private Long divisionOid;
    private String status;
    private String divisionName;
}
