package in.opt.sfa.tenant.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "designation")
@Getter
@Setter
public class Designation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oid;

    @Column(name = "designation_code")
    private String designationCode;

    @Column(name = "designation_name")
    private String designationName;

    private String status = "Y";
}
