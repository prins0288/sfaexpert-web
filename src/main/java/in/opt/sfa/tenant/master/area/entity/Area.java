package in.opt.sfa.tenant.master.area.entity;

import in.opt.sfa.tenant.approval.Approvable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Formula;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * area_master (was city_master). An area belongs to an employee (emp_id); its
 * add and delete go through approval (ApprovalEngine, Settings > General >
 * Area approval), and who sees / edits it depends on the role (AreaService).
 */
@Entity
@Table(name = "area_master")
@Getter
@Setter
public class Area implements Approvable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oid;

    @Column(name = "area_code")
    private String areaCode;

    @Column(name = "area_name")
    private String areaName;

    private String city;

    @Column(name = "state_oid")
    private Long stateOid;

    @Column(name = "hq_oid")
    private Long hqOid;

    /** -> emp_detail.emp_id: whose area it is. */
    @Column(name = "emp_id", length = 40)
    private String empId;

    private String pincode;

    @Column(name = "area_type")
    private String areaType;

    @Column(name = "distance_km")
    private BigDecimal distanceKm;

    /** Y = active, N = inactive. */
    private String status = "Y";

    // ---- approval ----
    /** PENDING_MANAGER / PENDING_ADMIN / APPROVED / REJECTED (the add request). */
    @Column(name = "approval_status", length = 20)
    private String approvalStatus = "APPROVED";

    @Column(name = "add_requested_by", length = 40)
    private String addRequestedBy;

    @Column(name = "manager_approved_by", length = 40)
    private String managerApprovedBy;

    @Column(name = "manager_approved_on")
    private LocalDateTime managerApprovedOn;

    @Column(name = "approved_by", length = 40)
    private String approvedBy;

    @Column(name = "approved_on")
    private LocalDateTime approvedOn;

    @Column(name = "rejected_by", length = 40)
    private String rejectedBy;

    @Column(name = "rejected_on")
    private LocalDateTime rejectedOn;

    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    @Column(name = "is_delete_requested")
    private Boolean deleteRequested = false;

    /** PENDING_MANAGER / PENDING_ADMIN / APPROVED / REJECTED (the delete request). */
    @Column(name = "delete_status", length = 20)
    private String deleteStatus;

    @Column(name = "delete_requested_by", length = 40)
    private String deleteRequestedBy;

    @Column(name = "delete_request_reason", length = 255)
    private String deleteRequestReason;

    @Column(name = "created_by", length = 40, updatable = false)
    private String createdBy;

    @Column(name = "updated_by", length = 40)
    private String updatedBy;

    // ---- display-only ----
    @Formula("(select s.state_name from state_master s where s.oid = state_oid)")
    private String stateName;

    @Formula("(select h.hq_name from hq_master h where h.oid = hq_oid)")
    private String hqName;

    @Formula("(select e.emp_name from emp_detail e where e.emp_id = emp_id)")
    private String empName;

    /**
     * Comma-joined route names mapped to this area. Populated by AreaService
     * (not a @Formula: a correlated subquery on the area's own `oid` would be
     * ambiguous with route_area_map.oid / route.oid).
     */
    @Transient
    private String routes;

    // ---- Approvable (ApprovalEngine) ----

    @Override
    public void finalApproval(String empId, LocalDateTime at) {
        this.approvedBy = empId;
        this.approvedOn = at;
    }

    @Override
    public void activeNow(boolean active) { this.status = active ? "Y" : "N"; }

    @Override
    public boolean isActiveNow() { return "Y".equals(status); }

    /** The area's employee, else its creator. */
    @Override
    public String approvalOwner() { return empId != null ? empId : createdBy; }
}
