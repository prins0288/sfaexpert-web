package in.opt.sfa.tenant.master.bank.mapper;

import in.opt.sfa.tenant.master.bank.dto.BankMasterDto;
import in.opt.sfa.tenant.master.bank.entity.BankMaster;
import org.springframework.stereotype.Component;

@Component
public class BankMasterMapper {

    public BankMasterDto toDto(BankMaster e) {
        BankMasterDto d = new BankMasterDto();
        d.setId(e.getId());
        d.setBankCode(e.getBankCode());
        d.setBankName(e.getBankName());
        d.setShortName(e.getShortName());
        d.setDescription(e.getDescription());
        d.setDisplayOrder(e.getDisplayOrder());
        d.setIsActive(e.getIsActive());
        d.setCreatedBy(e.getCreatedBy());
        d.setCreatedAt(e.getCreatedAt());
        d.setUpdatedBy(e.getUpdatedBy());
        d.setUpdatedAt(e.getUpdatedAt());
        return d;
    }

    /** Copies only the editable fields onto an existing/new entity — audit columns are server-managed (see entity lifecycle callbacks). */
    public void applyTo(BankMaster target, BankMasterDto d) {
        target.setBankCode(d.getBankCode());
        target.setBankName(d.getBankName());
        target.setShortName(d.getShortName());
        target.setDescription(d.getDescription());
        if (d.getDisplayOrder() != null) target.setDisplayOrder(d.getDisplayOrder());
        if (d.getIsActive() != null) target.setIsActive(d.getIsActive());
    }
}
