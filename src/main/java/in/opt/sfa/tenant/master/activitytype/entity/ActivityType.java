package in.opt.sfa.tenant.master.activitytype.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Activity Type Master master (TENANT db) — a per-client-type activity classification
 *  (e.g. "CME|Client"). */
@Entity
@Table(name = "activity_type_master")
@Getter
@Setter
public class ActivityType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oid;

    @Column(name = "type_code")
    private String typeCode;

    @Column(name = "type_name")
    private String typeName;

    /** Which client type (Doctor/Chemist/Stockist...) this activity type applies to — FK to client_type.oid. */
    @Column(name = "client_type_id")
    private Long clientTypeId;

    /** Active flag stored as TINYINT(1): true = 1 (active), false = 0 (inactive). */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
