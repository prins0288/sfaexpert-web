package in.opt.sfa.tenant.master.hq.mapper;

import in.opt.sfa.tenant.master.hq.dto.HqDto;
import in.opt.sfa.tenant.master.hq.entity.Hq;
import org.springframework.stereotype.Component;

@Component
public class HqMapper {

    public HqDto toDto(Hq e) {
        HqDto d = new HqDto();
        d.setOid(e.getOid());
        d.setHqCode(e.getHqCode());
        d.setHqName(e.getHqName());
        d.setStateOid(e.getStateOid());
        d.setHqGroupId(e.getHqGroupId());
        d.setIsActive(e.getIsActive());
        d.setStateName(e.getStateName());
        d.setHqGroupName(e.getHqGroupName());
        return d;
    }

    public Hq toEntity(HqDto d) {
        Hq e = new Hq();
        e.setOid(d.getOid());
        e.setHqCode(d.getHqCode());
        e.setHqName(d.getHqName());
        e.setStateOid(d.getStateOid());
        e.setHqGroupId(d.getHqGroupId());
        e.setIsActive(d.getIsActive());
        return e;
    }
}
