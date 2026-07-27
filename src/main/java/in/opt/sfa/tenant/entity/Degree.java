package in.opt.sfa.tenant.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "degree")
@Getter
@Setter
public class Degree {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oid;

    @Column(name = "degree_code")
    private String degreeCode;

    @Column(name = "degree_name")
    private String degreeName;

    private String status = "Y";
}
