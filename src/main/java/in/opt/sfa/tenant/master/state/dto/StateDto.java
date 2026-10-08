package in.opt.sfa.tenant.master.state.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StateDto {
    private Long oid;
    private String stateCode;
    private String stateName;
    private Long countryOid;
    private String status;
    private String countryName;
}
