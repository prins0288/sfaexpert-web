package in.opt.sfa.tenant.master.area.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class AreaDto {
    private Long oid;
    private String areaCode;
    private String areaName;
    private String city;
    private Long stateOid;
    private Long hqOid;
    /** Whose area (emp_detail.emp_id). Only an admin chooses it; others always get their own. */
    private String empId;
    private String pincode;
    private String areaType;
    private BigDecimal distanceKm;
    private String status;

    // ---- read-only, filled by the server ----
    private String stateName;
    private String hqName;
    private String empName;
    private String routes;

    private String approvalStatus;
    private String addRequestedByName;
    private String managerApprovedByName;
    private LocalDateTime managerApprovedOn;
    private String approvedByName;
    private LocalDateTime approvedOn;
    private String rejectedByName;
    private String rejectReason;
    private Boolean deleteRequested;
    private String deleteStatus;
    private String deleteRequestedByName;
    private String deleteRequestReason;
    /** ADD / DELETE while a request waits, else null. */
    private String pendingRequest;
    /** The signed-in user may approve / reject the waiting request now. */
    private Boolean canApprove;
}
