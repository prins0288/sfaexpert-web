package in.opt.sfa.tenant.master.country.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CountryDto {
    private Long oid;
    private String countryCode;
    private String countryName;
    private Boolean status;
}
