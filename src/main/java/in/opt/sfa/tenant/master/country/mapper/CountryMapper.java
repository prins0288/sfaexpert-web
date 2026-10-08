package in.opt.sfa.tenant.master.country.mapper;

import in.opt.sfa.tenant.master.country.dto.CountryDto;
import in.opt.sfa.tenant.master.country.entity.Country;
import org.springframework.stereotype.Component;

@Component
public class CountryMapper {

    public CountryDto toDto(Country e) {
        CountryDto d = new CountryDto();
        d.setOid(e.getOid());
        d.setCountryCode(e.getCountryCode());
        d.setCountryName(e.getCountryName());
        d.setStatus(e.getStatus());
        return d;
    }

    public Country toEntity(CountryDto d) {
        Country e = new Country();
        e.setOid(d.getOid());
        e.setCountryCode(d.getCountryCode());
        e.setCountryName(d.getCountryName());
        e.setStatus(d.getStatus());
        return e;
    }
}
