package in.opt.sfa.tenant.master.hq.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Formula;

@Entity
@Table(name = "hq_master")
@Getter
@Setter
public class Hq {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oid;

    @Column(name = "hq_code")
    private String hqCode;

    @Column(name = "hq_name")
    private String hqName;

    @Column(name = "state_oid")
    private Long stateOid;

    /** Optional wider region/cluster this HQ belongs to — FK to hq_group_master.id. */
    @Column(name = "hq_group_id")
    private Long hqGroupId;

    /** Active flag stored as TINYINT(1): true = 1 (active), false = 0 (inactive). */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Formula("(select s.state_name from state_master s where s.oid = state_oid)")
    private String stateName;

    @Formula("(select g.group_name from hq_group_master g where g.id = hq_group_id)")
    private String hqGroupName;
}
