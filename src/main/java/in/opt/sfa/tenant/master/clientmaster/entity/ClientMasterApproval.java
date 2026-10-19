package in.opt.sfa.tenant.master.clientmaster.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One step of a client's add / delete request (TENANT db, client_master_approval):
 * who requested, approved or rejected it, with that user's details at that moment.
 */
@Entity
@Table(name = "client_master_approval")
@Getter
@Setter
public class ClientMasterApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    /** ADD / DELETE. */
    @Column(name = "request_type", length = 10, nullable = false)
    private String requestType;

    /** REQUEST / MANAGER / ADMIN / FINAL / AUTO. */
    @Column(length = 20, nullable = false)
    private String step;

    /** REQUESTED / APPROVED / REJECTED. */
    @Column(length = 20, nullable = false)
    private String action;

    @Column(name = "status_after", length = 20)
    private String statusAfter;

    @Column(length = 255)
    private String remarks;

    @Column(name = "emp_id", length = 40)
    private String empId;

    @Column(name = "emp_code", length = 40)
    private String empCode;

    @Column(name = "emp_name", length = 150)
    private String empName;

    @Column(name = "designation_name", length = 120)
    private String designationName;

    @Column(name = "emp_level")
    private Integer empLevel;

    @Column(length = 20)
    private String role;

    @Column(name = "action_at", nullable = false)
    private LocalDateTime actionAt;
}
