package in.opt.sfa.tenant.tp.service;

import in.opt.sfa.tenant.approval.Approvable;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** One tour_emp_parent row seen by the shared ApprovalEngine (add / submit approval only; a plan is never "deleted" through it). */
@Getter
@Setter
public class TourPlanRow implements Approvable {
    private Long tourMain;
    private String empId;
    private String approvalStatus;
    private String addRequestedBy;
    private String managerApprovedBy;
    private LocalDateTime managerApprovedOn;
    private String approvedBy;
    private LocalDateTime approvedOn;
    private String rejectedBy;
    private LocalDateTime rejectedOn;
    private String rejectReason;

    @Override public String getDeleteStatus() { return null; }
    @Override public void setDeleteStatus(String status) { }
    @Override public Boolean getDeleteRequested() { return false; }
    @Override public void setDeleteRequested(Boolean requested) { }
    @Override public String getDeleteRequestedBy() { return null; }
    @Override public void setDeleteRequestedBy(String empId) { }
    @Override public void setDeleteRequestReason(String reason) { }
    @Override public String getCreatedBy() { return empId; }
    @Override public void finalApproval(String who, LocalDateTime at) { this.approvedBy = who; this.approvedOn = at; }
    @Override public void activeNow(boolean active) { }
    @Override public boolean isActiveNow() { return true; }
    @Override public String approvalOwner() { return empId; }
}
