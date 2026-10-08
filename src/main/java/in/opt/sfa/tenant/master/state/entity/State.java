package in.opt.sfa.tenant.master.state.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Formula;

@Entity
@Table(name = "state_master")
@Getter
@Setter
public class State {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oid;

    @Column(name = "state_code")
    private String stateCode;

    @Column(name = "state_name")
    private String stateName;

    @Column(name = "country_oid")
    private Long countryOid;

    private String status = "Y";

    // ---- display-only ----
    @Formula("(select c.country_name from country_master c where c.oid = country_oid)")
    private String countryName;
}
