package in.opt.sfa.tenant.master.designation.service;

import in.opt.sfa.common.util.RoleLevelMapper;
import in.opt.sfa.common.util.Strings;
import in.opt.sfa.tenant.entity.Designation;
import in.opt.sfa.tenant.master.designation.dto.DesignationMasterDto;
import in.opt.sfa.tenant.repository.DesignationRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Designation master (designation_master, TENANT db). The emp_level of a
 * designation is copied onto its employees (emp_detail.emp_level) and decides
 * their role in the JWT (RoleLevelMapper), so changing a designation's level
 * re-stamps every employee who holds it — effective from their next login.
 */
@Service
public class DesignationMasterService {

    private final DesignationRepository designations;
    private final JdbcTemplate jdbc;   // tenant-routed (@Primary)

    public DesignationMasterService(DesignationRepository designations, JdbcTemplate jdbc) {
        this.designations = designations;
        this.jdbc = jdbc;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<DesignationMasterDto> list() {
        Map<Long, Long> counts = new HashMap<>();
        jdbc.query("SELECT designation, COUNT(*) FROM emp_detail GROUP BY designation",
                rs -> { counts.put(rs.getLong(1), rs.getLong(2)); });
        return designations.findAllByOrderByEmpLevelDescDesignationNameAsc().stream()
                .map(d -> toDto(d, counts.getOrDefault(d.getOid(), 0L))).toList();
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public DesignationMasterDto get(Long oid) {
        return toDto(find(oid), employeeCount(oid));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public DesignationMasterDto save(DesignationMasterDto form) {
        boolean isNew = form.getOid() == null;
        Designation d = isNew ? new Designation() : find(form.getOid());
        String code = Strings.isBlank(form.getDesignationCode()) ? null : form.getDesignationCode().trim().toUpperCase();
        String name = Strings.isBlank(form.getDesignationName()) ? null : form.getDesignationName().trim();
        validate(code, name, form.getEmpLevel(), isNew ? null : d.getOid());

        Integer oldLevel = d.getEmpLevel();
        d.setDesignationCode(code);
        d.setDesignationName(name);
        d.setEmpLevel(form.getEmpLevel());
        if (form.getStatus() != null) d.setStatus(form.getStatus());
        if (d.getStatus() == null) d.setStatus(true);
        designations.saveAndFlush(d);

        if (!isNew && !Objects.equals(oldLevel, d.getEmpLevel())) {
            // keep every holder's level (and so their role) in step with the designation
            jdbc.update("UPDATE emp_detail SET emp_level = ? WHERE designation = ?", d.getEmpLevel(), d.getOid());
        }
        return toDto(d, employeeCount(d.getOid()));
    }

    /** Add Multiple: rows with a blank / duplicate code, blank name or bad level are skipped (with the reason). */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveMultiple(List<DesignationMasterDto> forms) {
        int saved = 0, skipped = 0;
        List<String> errors = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int row = 0;
        for (DesignationMasterDto f : forms) {
            row++;
            String code = Strings.isBlank(f.getDesignationCode()) ? null : f.getDesignationCode().trim().toUpperCase();
            String name = Strings.isBlank(f.getDesignationName()) ? null : f.getDesignationName().trim();
            try {
                if (code != null && !seen.add(code)) throw new IllegalStateException("code " + code + " repeated in this list");
                validate(code, name, f.getEmpLevel(), null);
            } catch (IllegalStateException e) {
                skipped++;
                errors.add("Row " + row + ": " + e.getMessage());
                continue;
            }
            Designation d = new Designation();
            d.setDesignationCode(code);
            d.setDesignationName(name);
            d.setEmpLevel(f.getEmpLevel());
            d.setStatus(true);
            designations.save(d);
            saved++;
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("saved", saved);
        res.put("skipped", skipped);
        res.put("errors", errors);
        return res;
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public DesignationMasterDto updateStatus(Long oid, boolean status) {
        Designation d = find(oid);
        d.setStatus(status);
        designations.save(d);
        return toDto(d, employeeCount(oid));
    }

    // ---- helpers ------------------------------------------------------------

    private void validate(String code, String name, Integer level, Long selfOid) {
        if (code == null) throw new IllegalStateException("Code is required");
        if (code.length() > 30) throw new IllegalStateException("Code can be at most 30 characters");
        if (name == null) throw new IllegalStateException("Name is required");
        if (name.length() > 120) throw new IllegalStateException("Name can be at most 120 characters");
        if (level == null) throw new IllegalStateException("Emp level is required");
        if (level < 1 || level > 99) throw new IllegalStateException("Emp level must be between 1 and 99");
        designations.findByDesignationCode(code)
                .filter(other -> !other.getOid().equals(selfOid))
                .ifPresent(other -> { throw new IllegalStateException("Code " + code + " already exists (" + other.getDesignationName() + ")"); });
    }

    private long employeeCount(Long oid) {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM emp_detail WHERE designation = ?", Long.class, oid);
        return n == null ? 0 : n;
    }

    private Designation find(Long oid) {
        return designations.findById(oid).orElseThrow(() -> new IllegalStateException("Designation not found: " + oid));
    }

    private static DesignationMasterDto toDto(Designation d, long employees) {
        DesignationMasterDto dto = new DesignationMasterDto();
        dto.setOid(d.getOid());
        dto.setDesignationCode(d.getDesignationCode());
        dto.setDesignationName(d.getDesignationName());
        dto.setEmpLevel(d.getEmpLevel());
        dto.setStatus(d.getStatus());
        dto.setRole(d.getEmpLevel() == null ? null : RoleLevelMapper.roleFor(d.getEmpLevel()));
        dto.setEmployeeCount(employees);
        return dto;
    }
}
