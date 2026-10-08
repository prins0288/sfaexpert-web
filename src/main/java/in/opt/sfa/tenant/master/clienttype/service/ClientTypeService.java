package in.opt.sfa.tenant.master.clienttype.service;

import in.opt.sfa.common.util.Strings;
import in.opt.sfa.tenant.master.clienttype.dto.ClientTypeDto;
import in.opt.sfa.tenant.master.clienttype.entity.ClientType;
import in.opt.sfa.tenant.master.clienttype.mapper.ClientTypeMapper;
import in.opt.sfa.tenant.master.clienttype.repository.ClientTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ClientTypeService {

    private final ClientTypeRepository clientTypes;
    private final ClientTypeMapper mapper;

    public ClientTypeService(ClientTypeRepository clientTypes, ClientTypeMapper mapper) {
        this.clientTypes = clientTypes;
        this.mapper = mapper;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<ClientTypeDto> list() {
        return clientTypes.findAllByOrderByOidAsc().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public ClientTypeDto get(Long oid) {
        return mapper.toDto(find(oid));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public ClientTypeDto save(ClientTypeDto form) {
        if (Strings.isBlank(form.getSingularLabel())) form.setSingularLabel(form.getTypeName());
        if (Strings.isBlank(form.getPluralLabel())) {
            form.setPluralLabel(form.getTypeName() == null ? null : form.getTypeName() + "s");
        }
        if (form.getIsActive() == null) form.setIsActive(true);
        return mapper.toDto(clientTypes.save(mapper.toEntity(form)));
    }

    /** Add Multiple — one insert per submitted row (blank / duplicate code skipped). */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveMultiple(List<ClientTypeDto> forms) {
        int saved = 0, skipped = 0;
        for (ClientTypeDto f : forms) {
            if (Strings.isBlank(f.getTypeCode()) || Strings.isBlank(f.getTypeName())
                    || clientTypes.findByTypeCode(f.getTypeCode().trim()).isPresent()) {
                skipped++;
                continue;
            }
            f.setOid(null);
            f.setTypeCode(f.getTypeCode().trim());
            f.setTypeName(f.getTypeName().trim());
            if (Strings.isBlank(f.getSingularLabel())) f.setSingularLabel(f.getTypeName());
            if (Strings.isBlank(f.getPluralLabel())) f.setPluralLabel(f.getTypeName() + "s");
            f.setIsActive(true);
            clientTypes.save(mapper.toEntity(f));
            saved++;
        }
        return Map.of("saved", saved, "skipped", skipped);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public ClientTypeDto updateStatus(Long oid, boolean isActive) {
        ClientType t = find(oid);
        t.setIsActive(isActive);
        return mapper.toDto(clientTypes.save(t));
    }

    private ClientType find(Long oid) {
        return clientTypes.findById(oid).orElseThrow(() -> new IllegalStateException("Client type not found: " + oid));
    }
}
