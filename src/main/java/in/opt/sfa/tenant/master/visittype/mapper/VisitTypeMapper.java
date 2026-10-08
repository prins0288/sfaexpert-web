package in.opt.sfa.tenant.master.visittype.mapper;

import in.opt.sfa.tenant.master.visittype.dto.VisitTypeDto;
import in.opt.sfa.tenant.master.visittype.entity.VisitType;
import org.springframework.stereotype.Component;

@Component
public class VisitTypeMapper {

    public VisitTypeDto toDto(VisitType e) {
        VisitTypeDto d = new VisitTypeDto();
        d.setOid(e.getOid());
        d.setTypeCode(e.getTypeCode());
        d.setTypeName(e.getTypeName());
        d.setClientTypeId(e.getClientTypeId());
        d.setIsActive(e.getIsActive());
        return d;
    }

    public VisitType toEntity(VisitTypeDto d) {
        VisitType e = new VisitType();
        e.setOid(d.getOid());
        e.setTypeCode(d.getTypeCode());
        e.setTypeName(d.getTypeName());
        e.setClientTypeId(d.getClientTypeId());
        e.setIsActive(d.getIsActive());
        return e;
    }
}
