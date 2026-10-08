package in.opt.sfa.tenant.master.hq.dto;

import lombok.Getter;
import lombok.Setter;

/** Request + response shape for the HQ master API — same JSON contract the frontend already uses. */
@Getter
@Setter
public class HqDto {
    private Long oid;
    private String hqCode;
    private String hqName;
    private Long stateOid;
    private Long hqGroupId;
    private Boolean isActive;
    private String stateName;
    /** Read-only: the HQ group's name, joined via @Formula — not persisted here. */
    private String hqGroupName;
}
