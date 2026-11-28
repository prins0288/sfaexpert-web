package in.opt.sfa.tenant.approval;

import java.time.LocalDateTime;

/**
 * A master row whose ADD and DELETE go through approval (ApprovalEngine):
 * Client Master, Area Master … Most methods are the entity's own Lombok
 * getters / setters; the few defaults the entity writes itself are below.
 */
public interface Approvable {

    String getApprovalStatus();
    void setApprovalStatus(String status);

    String getDeleteStatus();
    void setDeleteStatus(String status);

    Boolean getDeleteRequested();
    void setDeleteRequested(Boolean requested);
    String getDeleteRequestedBy();
    void setDeleteRequestedBy(String empId);
    void setDeleteRequestReason(String reason);

    String getAddRequestedBy();
    void setAddRequestedBy(String empId);
    String getCreatedBy();

    void setManagerApprovedBy(String empId);
    void setManagerApprovedOn(LocalDateTime at);
    void setRejectedBy(String empId);
    void setRejectedOn(LocalDateTime at);
    void setRejectReason(String reason);

    /** Final approval of the ADD request (null, null = cleared for a new request). */
    void finalApproval(String empId, LocalDateTime at);

    /** Active / inactive (a new row is active only once approved, a deleted one becomes inactive). */
    void activeNow(boolean active);
    boolean isActiveNow();

    /** The employee the row belongs to — decides whose manager approves; he never approves it himself. */
    String approvalOwner();
}
