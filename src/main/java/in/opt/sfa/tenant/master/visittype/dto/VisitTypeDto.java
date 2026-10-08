package in.opt.sfa.tenant.master.visittype.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VisitTypeDto {
    private Long oid;
    private String typeCode;
    private String typeName;
    private Long clientTypeId;
    /** Read-only: the Client Type's name, joined server-side for display — not persisted here. */
    private String clientTypeName;
    private Boolean isActive;
}
