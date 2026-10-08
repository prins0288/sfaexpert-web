package in.opt.sfa.tenant.master.division.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DivisionDto {
    private Long oid;
    private String divisionCode;
    private String divisionName;
    private String status;
}
