package in.opt.sfa.tenant.master.document.mapper;

import in.opt.sfa.tenant.master.document.dto.DocumentMasterDto;
import in.opt.sfa.tenant.master.document.entity.DocumentMaster;
import org.springframework.stereotype.Component;

@Component
public class DocumentMasterMapper {

    public DocumentMasterDto toDto(DocumentMaster e) {
        DocumentMasterDto d = new DocumentMasterDto();
        d.setId(e.getId());
        d.setDocumentCode(e.getDocumentCode());
        d.setDocumentName(e.getDocumentName());
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
    public void applyTo(DocumentMaster target, DocumentMasterDto d) {
        target.setDocumentCode(d.getDocumentCode());
        target.setDocumentName(d.getDocumentName());
        target.setShortName(d.getShortName());
        target.setDescription(d.getDescription());
        if (d.getDisplayOrder() != null) target.setDisplayOrder(d.getDisplayOrder());
        if (d.getIsActive() != null) target.setIsActive(d.getIsActive());
    }
}
