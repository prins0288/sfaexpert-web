package in.opt.sfa.tenant.master.client.mapper;

import in.opt.sfa.tenant.master.client.dto.ClientDto;
import in.opt.sfa.tenant.master.client.entity.Client;
import org.springframework.stereotype.Component;

@Component
public class ClientMapper {

    public ClientDto toDto(Client e) {
        ClientDto d = new ClientDto();
        d.setOid(e.getOid());
        d.setClientCode(e.getClientCode());
        d.setClientTypeOid(e.getClientTypeOid());
        d.setPrefix(e.getPrefix());
        d.setName(e.getName());
        d.setFirmName(e.getFirmName());
        d.setDegreeOid(e.getDegreeOid());
        d.setSpecialityOid(e.getSpecialityOid());
        d.setCategoryOid(e.getCategoryOid());
        d.setRouteOid(e.getRouteOid());
        d.setAreaOid(e.getAreaOid());
        d.setAddress(e.getAddress());
        d.setCity(e.getCity());
        d.setMobile(e.getMobile());
        d.setEmail(e.getEmail());
        d.setDob(e.getDob());
        d.setAnniversary(e.getAnniversary());
        d.setDrugLicenseNo(e.getDrugLicenseNo());
        d.setGstNo(e.getGstNo());
        d.setRemarks(e.getRemarks());
        d.setStatus(e.getStatus());
        d.setTypeName(e.getTypeName());
        d.setTypeSingular(e.getTypeSingular());
        d.setAreaName(e.getAreaName());
        d.setRouteName(e.getRouteName());
        d.setDegreeName(e.getDegreeName());
        d.setSpecialityName(e.getSpecialityName());
        d.setCategoryName(e.getCategoryName());
        return d;
    }

    public Client toEntity(ClientDto d) {
        Client e = new Client();
        e.setOid(d.getOid());
        e.setClientCode(d.getClientCode());
        e.setClientTypeOid(d.getClientTypeOid());
        e.setPrefix(d.getPrefix());
        e.setName(d.getName());
        e.setFirmName(d.getFirmName());
        e.setDegreeOid(d.getDegreeOid());
        e.setSpecialityOid(d.getSpecialityOid());
        e.setCategoryOid(d.getCategoryOid());
        e.setRouteOid(d.getRouteOid());
        e.setAreaOid(d.getAreaOid());
        e.setAddress(d.getAddress());
        e.setCity(d.getCity());
        e.setMobile(d.getMobile());
        e.setEmail(d.getEmail());
        e.setDob(d.getDob());
        e.setAnniversary(d.getAnniversary());
        e.setDrugLicenseNo(d.getDrugLicenseNo());
        e.setGstNo(d.getGstNo());
        e.setRemarks(d.getRemarks());
        e.setStatus(d.getStatus());
        return e;
    }
}
