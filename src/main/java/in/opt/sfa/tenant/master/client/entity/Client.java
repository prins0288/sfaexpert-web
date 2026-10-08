package in.opt.sfa.tenant.master.client.entity;

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

    @Formula("(select a.area_name from city_master a where a.oid = area_oid)")
    private String areaName;

    @Formula("(select r.route_name from route r where r.oid = route_oid)")
    private String routeName;

    // Degrees now live in degree_master (PK = id), not the old `degree` table
    // (which no longer exists — hence "Table acme_db.degree doesn't exist").
    // client.degree_oid stores a degree_master.id.
    @Formula("(select d.degree_name from degree_master d where d.id = degree_oid)")
    private String degreeName;

    // speciality_master / category_master use `oid` as their PK column
    // (only degree_master uses `id`), so match on oid here.
    @Formula("(select s.speciality_name from speciality_master s where s.oid = speciality_oid)")
    private String specialityName;

    @Formula("(select c.category_name from category_master c where c.oid = category_oid)")
    private String categoryName;
}
