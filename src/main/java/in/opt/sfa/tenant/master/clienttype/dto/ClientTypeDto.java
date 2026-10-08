package in.opt.sfa.tenant.master.clienttype.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientTypeDto {
    private Long oid;
    private String typeCode;
    private String typeName;
    private String singularLabel;
    private String pluralLabel;
    private Boolean isActive;
}
