package in.opt.sfa.tenant.master.activitytype.service;

import in.opt.sfa.common.util.Strings;
import in.opt.sfa.tenant.master.clienttype.repository.ClientTypeRepository;
import in.opt.sfa.tenant.master.clienttype.entity.ClientType;
import in.opt.sfa.tenant.master.activitytype.dto.ActivityTypeDto;
import in.opt.sfa.tenant.master.activitytype.entity.ActivityType;
import in.opt.sfa.tenant.master.activitytype.mapper.ActivityTypeMapper;
import in.opt.sfa.tenant.master.activitytype.repository.ActivityTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ActivityTypeService {

    private final ActivityTypeRepository repo;
    private final ActivityTypeMapper mapper;
    private final ClientTypeRepository clientTypes;

    public ActivityTypeService(ActivityTypeRepository repo, ActivityTypeMapper mapper, ClientTypeRepository clientTypes) {
        this.repo = repo;
        this.mapper = mapper;
        this.clientTypes = clientTypes;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<ActivityTypeDto> list() {
        Map<Long, String> names = clientTypeNames();
        return repo.findAllByOrderByOidAsc().stream().map(mapper::toDto)
                .peek(d -> d.setClientTypeName(names.get(d.getClientTypeId())))
                .collect(Collectors.toList());
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public ActivityTypeDto get(Long oid) {
        ActivityTypeDto d = mapper.toDto(find(oid));
        d.setClientTypeName(clientTypeNames().get(d.getClientTypeId()));
        return d;
    }

    /** Every Client Type's oid -> display name, for joining onto Activity Type rows. */
    private Map<Long, String> clientTypeNames() {
        return clientTypes.findAll().stream().collect(Collectors.toMap(ClientType::getOid, ClientType::getTypeName));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public ActivityTypeDto save(ActivityTypeDto form) {
        if (form.getIsActive() == null) form.setIsActive(true);
        return mapper.toDto(repo.save(mapper.toEntity(form)));
    }

    /** Add Multiple — one insert per submitted row (blank / duplicate code skipped). */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveMultiple(List<ActivityTypeDto> forms) {
        int saved = 0, skipped = 0;
        for (ActivityTypeDto f : forms) {
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
    public ActivityTypeDto updateStatus(Long oid, boolean isActive) {
        ActivityType t = find(oid);
        t.setIsActive(isActive);
        return mapper.toDto(repo.save(t));
    }

    private ActivityType find(Long oid) {
        return repo.findById(oid).orElseThrow(() -> new IllegalStateException("Activity Type Master not found: " + oid));
    }
}
