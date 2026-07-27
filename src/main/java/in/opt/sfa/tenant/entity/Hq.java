package in.opt.sfa.tenant.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Formula;

@Entity
@Table(name = "hq")
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

    private String status = "Y";

    @Formula("(select s.state_name from state s where s.oid = state_oid)")
    private String stateName;
}
