package in.opt.sfa.tenant.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "speciality")
@Getter
@Setter
public class Speciality {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oid;

    @Column(name = "speciality_code")
    private String specialityCode;

    @Column(name = "speciality_name")
    private String specialityName;

    private String status = "Y";
}
