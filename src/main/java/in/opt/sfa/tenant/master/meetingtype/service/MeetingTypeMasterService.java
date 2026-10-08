package in.opt.sfa.tenant.master.meetingtype.service;

import in.opt.sfa.common.util.Strings;
import in.opt.sfa.tenant.master.meetingtype.dto.MeetingTypeMasterDto;
import in.opt.sfa.tenant.master.meetingtype.entity.MeetingTypeMaster;
import in.opt.sfa.tenant.master.meetingtype.mapper.MeetingTypeMasterMapper;
import in.opt.sfa.tenant.master.meetingtype.repository.MeetingTypeMasterRepository;
import in.opt.sfa.tenant.master.employee.service.EmployeeNameResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * On update the existing row is LOADED and only the editable fields are copied
 * (see MeetingTypeMasterMapper.applyTo), so the server-managed audit columns
 * (created_at / created_by) are preserved; the entity's @PreUpdate then stamps
 * updated_at / updated_by.
 */
@Service
public class MeetingTypeMasterService {

    private final MeetingTypeMasterRepository meetingTypes;
    private final MeetingTypeMasterMapper mapper;
    private final EmployeeNameResolver employeeNames;

    public MeetingTypeMasterService(MeetingTypeMasterRepository meetingTypes, MeetingTypeMasterMapper mapper, EmployeeNameResolver employeeNames) {
        this.meetingTypes = meetingTypes;
        this.mapper = mapper;
        this.employeeNames = employeeNames;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<MeetingTypeMasterDto> list() {
        Map<String, String> names = employeeNames.nameMap();
        return meetingTypes.findAllByOrderByDisplayOrderAscIdAsc().stream().map(mapper::toDto)
                .peek(d -> { d.setCreatedBy(employeeNames.resolve(names, d.getCreatedBy())); d.setUpdatedBy(employeeNames.resolve(names, d.getUpdatedBy())); })
                .collect(Collectors.toList());
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public MeetingTypeMasterDto get(Long id) {
        MeetingTypeMasterDto d = mapper.toDto(find(id));
        Map<String, String> names = employeeNames.nameMap();
        d.setCreatedBy(employeeNames.resolve(names, d.getCreatedBy()));
        d.setUpdatedBy(employeeNames.resolve(names, d.getUpdatedBy()));
        return d;
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public MeetingTypeMasterDto save(MeetingTypeMasterDto form) {
        MeetingTypeMaster target = form.getId() != null ? find(form.getId()) : new MeetingTypeMaster();
        form.setMeetingTypeCode(Strings.isBlank(form.getMeetingTypeCode()) ? null : form.getMeetingTypeCode().trim());
        form.setMeetingTypeName(Strings.isBlank(form.getMeetingTypeName()) ? null : form.getMeetingTypeName().trim());
        mapper.applyTo(target, form);
        return mapper.toDto(meetingTypes.save(target)); // @PrePersist / @PreUpdate fill the audit columns
    }

    /** Bulk create — blank / duplicate code or name rows are skipped. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveMultiple(List<MeetingTypeMasterDto> forms) {
        int saved = 0, skipped = 0;
        for (MeetingTypeMasterDto f : forms) {
            String code = Strings.isBlank(f.getMeetingTypeCode()) ? null : f.getMeetingTypeCode().trim();
            String name = Strings.isBlank(f.getMeetingTypeName()) ? null : f.getMeetingTypeName().trim();
            if (Strings.isBlank(code) || Strings.isBlank(name)
                    || meetingTypes.findByMeetingTypeCode(code).isPresent()
                    || meetingTypes.findByMeetingTypeName(name).isPresent()) {
                skipped++;
                continue;
            }
            MeetingTypeMaster t = new MeetingTypeMaster();
            t.setMeetingTypeCode(code);
            t.setMeetingTypeName(name);
            t.setShortName(f.getShortName());
            t.setDescription(f.getDescription());
            t.setDisplayOrder(f.getDisplayOrder() == null ? 0 : f.getDisplayOrder());
            t.setIsActive(Boolean.TRUE);
            meetingTypes.save(t);
            saved++;
        }
        return Map.of("saved", saved, "skipped", skipped);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public MeetingTypeMasterDto updateStatus(Long id, boolean isActive) {
        MeetingTypeMaster t = find(id);
        t.setIsActive(isActive);
        return mapper.toDto(meetingTypes.save(t));
    }

    private MeetingTypeMaster find(Long id) {
        return meetingTypes.findById(id).orElseThrow(() -> new IllegalStateException("Meeting type not found: " + id));
    }
}
