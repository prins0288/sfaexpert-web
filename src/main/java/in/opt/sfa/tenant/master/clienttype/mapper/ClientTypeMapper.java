package in.opt.sfa.tenant.master.clienttype.mapper;

import in.opt.sfa.tenant.master.clienttype.dto.ClientTypeDto;
import in.opt.sfa.tenant.master.clienttype.entity.ClientType;
import org.springframework.stereotype.Component;

@Component
public class ClientTypeMapper {

    public ClientTypeDto toDto(ClientType e) {
        ClientTypeDto d = new ClientTypeDto();
        d.setOid(e.getOid());
        d.setTypeCode(e.getTypeCode());
        d.setTypeName(e.getTypeName());
        d.setSingularLabel(e.getSingularLabel());
        d.setPluralLabel(e.getPluralLabel());
        d.setIsActive(e.getIsActive());
        return d;
    }

    public ClientType toEntity(ClientTypeDto d) {
        ClientType e = new ClientType();
        e.setOid(d.getOid());
        e.setTypeCode(d.getTypeCode());
        e.setTypeName(d.getTypeName());
        e.setSingularLabel(d.getSingularLabel());
        e.setPluralLabel(d.getPluralLabel());
        e.setIsActive(d.getIsActive());
        return e;
    }
}
