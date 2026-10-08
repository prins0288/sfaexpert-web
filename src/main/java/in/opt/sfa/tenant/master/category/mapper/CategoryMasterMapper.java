package in.opt.sfa.tenant.master.category.mapper;

import in.opt.sfa.tenant.master.category.dto.CategoryMasterDto;
import in.opt.sfa.tenant.master.category.entity.CategoryMaster;
import org.springframework.stereotype.Component;

@Component
public class CategoryMasterMapper {

    public CategoryMasterDto toDto(CategoryMaster e) {
        CategoryMasterDto d = new CategoryMasterDto();
        d.setOid(e.getOid());
        d.setCategoryCode(e.getCategoryCode());
        d.setCategoryName(e.getCategoryName());
        d.setDescription(e.getDescription());
        d.setIcon(e.getIcon());
        d.setStatus(e.getStatus());
        d.setCreatedAt(e.getCreatedAt());
        d.setCreatedBy(e.getCreatedBy());
        d.setUpdatedAt(e.getUpdatedAt());
        d.setUpdatedBy(e.getUpdatedBy());
        return d;
    }

    /** Copies only the editable fields onto an existing/new entity — audit columns are server-managed (see entity lifecycle callbacks). */
    public void applyTo(CategoryMaster target, CategoryMasterDto d) {
        target.setCategoryCode(d.getCategoryCode());
        target.setCategoryName(d.getCategoryName());
        target.setDescription(d.getDescription());
        target.setIcon(d.getIcon());
        if (d.getStatus() != null) target.setStatus(d.getStatus());
    }
}
