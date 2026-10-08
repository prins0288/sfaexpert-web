package in.opt.sfa.tenant.dcr.view.entity;

import in.opt.sfa.security.UserContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Daily Call Report entry — one row per employee+client visit on a given day.
 *
 * dcrDate is the real DATE column used for every normal (India) client.
 * dcrDateNp is a VARCHAR column used ONLY for Nepal clients (Nepali/Bikram
 * Sambat calendar, stored as text since it isn't a Gregorian date) — which
 * column a report reads is decided by the client's country (see
 * DcrReportController / DcrReportRow.countryName).
 */
@Entity
@Table(name = "dcr")
@Getter
@Setter
public class Dcr {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oid;

    /** The employee who made the call -> emp_detail.emp_id. */
    @Column(name = "emp_id", nullable = false, length = 40)
    private String empId;

    @Column(name = "client_oid", nullable = false)
    private Long clientOid;

    @Column(name = "dcr_date")
    private LocalDate dcrDate;

    @Column(name = "dcr_date_np", length = 20)
    private String dcrDateNp;

    @Column(name = "work_type")
    private String workType;

    private String remarks;

    @Column(name = "time_in")
    private LocalTime timeIn;

    @Column(name = "time_out")
    private LocalTime timeOut;

    /** Active flag stored as TINYINT(1): true = 1 (active), false = 0 (soft-deleted). */
    @Column(nullable = false)
    private Boolean status = true;

    // ---- audit ----
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", updatable = false, length = 128)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 128)
    private String updatedBy;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        String who = currentUser();
        this.createdBy = who;
        this.updatedBy = who;
        if (this.status == null) this.status = true;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = currentUser();
    }

    private static String currentUser() {
        UserContext.CurrentUser u = UserContext.get();
        return (u == null || u.username() == null) ? "system" : u.username();
    }
}
