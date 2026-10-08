package in.opt.sfa.tenant.master.client.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientDto {
    private Long oid;
    private String clientCode;
    private Long clientTypeOid;
    private String prefix;
    private String name;
    private String firmName;
    private Long degreeOid;
    private Long specialityOid;
    private Long categoryOid;
    private Long routeOid;
    private Long areaOid;
    private String address;
    private String city;
    private String mobile;
    private String email;
    private String dob;
    private String anniversary;
    private String drugLicenseNo;
    private String gstNo;
    private String remarks;
    private String status;

    private String typeName;
    private String typeSingular;
    private String areaName;
    private String routeName;
    private String degreeName;
    private String specialityName;
    private String categoryName;
}
