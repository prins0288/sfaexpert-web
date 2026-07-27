package in.opt.sfa.tenant.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Formula;

@Entity
@Table(name = "client")
@Getter
@Setter
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oid;

    @Column(name = "client_code")
    private String clientCode;

    @Column(name = "client_type_oid")
    private Long clientTypeOid;

    private String prefix;

    private String name;

    @Column(name = "firm_name")
    private String firmName;

    @Column(name = "degree_oid")
    private Long degreeOid;

    @Column(name = "speciality_oid")
    private Long specialityOid;

    @Column(name = "category_oid")
    private Long categoryOid;

    @Column(name = "route_oid")
    private Long routeOid;

    @Column(name = "area_oid")
    private Long areaOid;

    private String address;
    private String city;
    private String mobile;
    private String email;
    private String dob;
    private String anniversary;

    @Column(name = "drug_license_no")
    private String drugLicenseNo;

    @Column(name = "gst_no")
    private String gstNo;

    private String remarks;

    private String status = "Y";

    // ---- display-only ----
    @Formula("(select t.type_name from client_type t where t.oid = client_type_oid)")
    private String typeName;

    @Formula("(select t.singular_label from client_type t where t.oid = client_type_oid)")
    private String typeSingular;

    @Formula("(select a.area_name from area a where a.oid = area_oid)")
    private String areaName;

    @Formula("(select r.route_name from route r where r.oid = route_oid)")
    private String routeName;

    @Formula("(select d.degree_name from degree d where d.oid = degree_oid)")
    private String degreeName;

    @Formula("(select s.speciality_name from speciality s where s.oid = speciality_oid)")
    private String specialityName;

    @Formula("(select c.category_name from category c where c.oid = category_oid)")
    private String categoryName;
}
