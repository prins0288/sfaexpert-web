package in.opt.sfa.tenant.master.division.mapper;

import in.opt.sfa.tenant.master.division.dto.DivisionDto;
import in.opt.sfa.tenant.master.division.entity.Division;
import org.springframework.stereotype.Component;

@Component
public class DivisionMapper {

    public DivisionDto toDto(Division e) {
        DivisionDto d = new DivisionDto();
        d.setOid(e.getOid());
        d.setDivisionCode(e.getDivisionCode());
        d.setDivisionName(e.getDivisionName());
        d.setStatus(e.getStatus());
        return d;
    }

    public Division toEntity(DivisionDto d) {
        Division e = new Division();
        e.setOid(d.getOid());
        e.setDivisionCode(d.getDivisionCode());
        e.setDivisionName(d.getDivisionName());
        e.setStatus(d.getStatus());
        return e;
    }
}
