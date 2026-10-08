package in.opt.sfa.tenant.master.division.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "division_master")
@Getter
@Setter
public class Division {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oid;

    @Column(name = "division_code")
    private String divisionCode;

    @Column(name = "division_name")
    private String divisionName;

    private String status = "Y";
}
