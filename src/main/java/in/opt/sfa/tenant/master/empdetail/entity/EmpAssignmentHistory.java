package in.opt.sfa.tenant.master.empdetail.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * emp_assignment_history: one row per period an employee held a given
 * designation/manager/division/HQ/state (a promotion, transfer or manager
 * change closes the previous row — effective_to — and opens a new one).
 * Written by EmpDetailService.save() whenever one of those actually changes;
 * applied directly (no approval) and answers "what was X on date D".
 */
@Entity
@Table(name = "emp_assignment_history")
@Getter
@Setter
public class EmpAssignmentHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "emp_id", nullable = false, length = 40)
    private String empId;

    @Column(name = "division_id")
    private Long divisionId;

    @Column(name = "state_id")
    private Long stateId;

    @Column(name = "hq_id")
    private Long hqId;

    @Column(name = "designation_id")
    private Integer designationId;

    @Column(name = "manager_id", length = 40)
    private String managerId;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    /** null = this is the current / open row. */
    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    /** JOINING / TRANSFER / PROMOTION / DEMOTION / MANAGER_CHANGE / CORRECTION */
    @Column(name = "change_reason", length = 30)
    private String changeReason;

    @Column(name = "remarks", length = 255)
    private String remarks;

    @Column(name = "created_by", length = 30)
    private String createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
