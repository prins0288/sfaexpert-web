package in.opt.sfa.tenant.master.employee.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Full profile shape — used by GET /{oid} (edit-modal population). */
@Getter
@Setter
public class EmployeeDto {
    private Long oid;
    private String empId;
    private String empName;
    private String username;
    private Long designationOid;
    private String designationName;
    private Integer empLevel;
    private String mobile;
    private String email;
    private Long stateOid;
    private String stateName;
    private Long districtOid;
    private String districtName;
    private LocalDate dob;
    private String photo;
    private String status;
}
