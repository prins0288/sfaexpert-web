package in.opt.sfa.tenant.master.employee.mapper;

import in.opt.sfa.tenant.master.employee.dto.EmployeeDto;
import in.opt.sfa.tenant.master.employee.entity.Employee;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {

    public EmployeeDto toDto(Employee e) {
        EmployeeDto d = new EmployeeDto();
        d.setOid(e.getOid());
        d.setEmpId(e.getEmpId());
        d.setEmpName(e.getEmpName());
        d.setUsername(e.getUsername());
        d.setDesignationOid(e.getDesignationOid());
        d.setDesignationName(e.getDesignationName());
        d.setEmpLevel(e.getEmpLevel());
        d.setMobile(e.getMobile());
        d.setEmail(e.getEmail());
        d.setStateOid(e.getStateOid());
        d.setStateName(e.getStateName());
        d.setDistrictOid(e.getDistrictOid());
        d.setDistrictName(e.getDistrictName());
        d.setDob(e.getDob());
        d.setPhoto(e.getPhoto());
        d.setStatus(e.getStatus());
        return d;
    }
}
