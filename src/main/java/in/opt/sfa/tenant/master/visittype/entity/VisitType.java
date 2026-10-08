package in.opt.sfa.tenant.master.visittype.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Visit Type Master master (TENANT db) — a per-client-type visit classification
 *  (e.g. "Visit|Client"). */
@Entity
@Table(name = "visit_type_master")
@Getter
@Setter
public class VisitType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oid;

    @Column(name = "type_code")
    private String typeCode;

    @Column(name = "type_name")
    private String typeName;

    /** Which client type (Doctor/Chemist/Stockist...) this visit type applies to — FK to client_type.oid. */
    @Column(name = "client_type_id")
    private Long clientTypeId;

    /** Active flag stored as TINYINT(1): true = 1 (active), false = 0 (inactive). */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
