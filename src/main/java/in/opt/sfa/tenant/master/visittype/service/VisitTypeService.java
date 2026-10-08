package in.opt.sfa.tenant.master.visittype.service;

import in.opt.sfa.common.util.Strings;
import in.opt.sfa.tenant.master.clienttype.repository.ClientTypeRepository;
import in.opt.sfa.tenant.master.clienttype.entity.ClientType;
import in.opt.sfa.tenant.master.visittype.dto.VisitTypeDto;
import in.opt.sfa.tenant.master.visittype.entity.VisitType;
import in.opt.sfa.tenant.master.visittype.mapper.VisitTypeMapper;
import in.opt.sfa.tenant.master.visittype.repository.VisitTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class VisitTypeService {

    private final VisitTypeRepository repo;
    private final VisitTypeMapper mapper;
    private final ClientTypeRepository clientTypes;

    public VisitTypeService(VisitTypeRepository repo, VisitTypeMapper mapper, ClientTypeRepository clientTypes) {
        this.repo = repo;
        this.mapper = mapper;
        this.clientTypes = clientTypes;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<VisitTypeDto> list() {
        Map<Long, String> names = clientTypeNames();
        return repo.findAllByOrderByOidAsc().stream().map(mapper::toDto)
                .peek(d -> d.setClientTypeName(names.get(d.getClientTypeId())))
                .collect(Collectors.toList());
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public VisitTypeDto get(Long oid) {
        VisitTypeDto d = mapper.toDto(find(oid));
        d.setClientTypeName(clientTypeNames().get(d.getClientTypeId()));
        return d;
    }

    /** Every Client Type's oid -> display name, for joining onto Visit Type rows. */
    private Map<Long, String> clientTypeNames() {
        return clientTypes.findAll().stream().collect(Collectors.toMap(ClientType::getOid, ClientType::getTypeName));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public VisitTypeDto save(VisitTypeDto form) {
        if (form.getIsActive() == null) form.setIsActive(true);
        return mapper.toDto(repo.save(mapper.toEntity(form)));
    }

    /** Add Multiple — one insert per submitted row (blank / duplicate code skipped). */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveMultiple(List<VisitTypeDto> forms) {
        int saved = 0, skipped = 0;
        for (VisitTypeDto f : forms) {
            if (Strings.isBlank(f.getTypeCode()) || Strings.isBlank(f.getTypeName())
                    || repo.findByTypeCode(f.getTypeCode().trim()).isPresent()) {
                skipped++;
                continue;
            }
            f.setOid(null);
            f.setTypeCode(f.getTypeCode().trim());
            f.setTypeName(f.getTypeName().trim());
            f.setIsActive(true);
            repo.save(mapper.toEntity(f));
            saved++;
        }
        return Map.of("saved", saved, "skipped", skipped);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public VisitTypeDto updateStatus(Long oid, boolean isActive) {
        VisitType t = find(oid);
        t.setIsActive(isActive);
        return mapper.toDto(repo.save(t));
    }

    private VisitType find(Long oid) {
        return repo.findById(oid).orElseThrow(() -> new IllegalStateException("Visit Type Master not found: " + oid));
    }
}
