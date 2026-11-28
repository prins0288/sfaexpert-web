package in.opt.sfa.tenant.master.area.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** area_master_approval: every request / approval / rejection of an area, with who did it (snapshot). */
@Entity
@Table(name = "area_master_approval")
@Getter
@Setter
public class AreaMasterApproval {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "area_id", nullable = false)
    private Long areaId;

    /** ADD / DELETE */
    @Column(name = "request_type", nullable = false, length = 10)
    private String requestType;

    /** REQUEST / AUTO / MANAGER / ADMIN / FINAL */
    @Column(name = "step", nullable = false, length = 20)
    private String step;

    /** REQUESTED / APPROVED / REJECTED */
    @Column(name = "action", nullable = false, length = 20)
    private String action;

    @Column(name = "status_after", length = 20)
    private String statusAfter;

    @Column(name = "remarks", length = 255)
    private String remarks;

    @Column(name = "emp_id", length = 40)
    private String empId;

    @Column(name = "emp_code", length = 30)
    private String empCode;

    @Column(name = "emp_name", length = 150)
    private String empName;

    @Column(name = "designation_name", length = 120)
    private String designationName;

    @Column(name = "hierarchy_level")
    private Integer hierarchyLevel;

    @Column(name = "role", length = 20)
    private String role;

    @Column(name = "action_at", nullable = false)
    private LocalDateTime actionAt;
}
