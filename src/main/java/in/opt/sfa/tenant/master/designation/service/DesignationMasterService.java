package in.opt.sfa.tenant.master.designation.service;

import in.opt.sfa.common.service.LevelRoleService;
import in.opt.sfa.common.util.Strings;
import in.opt.sfa.tenant.entity.Designation;
import in.opt.sfa.tenant.master.designation.dto.DesignationMasterDto;
import in.opt.sfa.tenant.repository.DesignationRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Designation master (designation_master, TENANT db). Employees hold only the
 * designation (emp_detail.designation -> id); their seniority is its
 * hierarchy_level (unique), and their role comes from that level by the
 * company's ranges (LevelRoleService) — so editing a designation's level changes
 * every holder's role from their next login.
 *
 * Admin and Super Admin designations (hierarchy level in the Admin range or
 * above) are LOCKED on the panel: they cannot be added, edited or deactivated
 * here, and no designation can be moved into that range — they are managed in
 * the database only, so nobody can raise their own (or anyone's) role from the UI.
 */
@Service
public class DesignationMasterService {

    private final DesignationRepository designations;
    private final JdbcTemplate jdbc;   // tenant-routed (@Primary)
    private final LevelRoleService levelRoles;

    /** designation_category choices (optional, information only). */
    public static final List<String> CATEGORIES = List.of("Field", "Manager", "Admin", "Support");

    public DesignationMasterService(DesignationRepository designations, JdbcTemplate jdbc, LevelRoleService levelRoles) {
        this.designations = designations;
        this.levelRoles = levelRoles;
        this.jdbc = jdbc;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<DesignationMasterDto> list() {
        Map<Integer, Long> counts = new HashMap<>();
        jdbc.query("SELECT designation, COUNT(*) FROM emp_detail GROUP BY designation",
                rs -> { counts.put(rs.getInt(1), rs.getLong(2)); });
        int[] ranges = levelRoles.ranges();
        return designations.findAllByOrderByDisplayOrderAscHierarchyLevelAsc().stream()
                .map(d -> toDto(d, counts.getOrDefault(d.getId(), 0L), ranges)).toList();
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public DesignationMasterDto get(Integer id) {
        return toDto(find(id), employeeCount(id), levelRoles.ranges());
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public DesignationMasterDto save(DesignationMasterDto form) {
        boolean isNew = form.getId() == null;
        Designation d = isNew ? new Designation() : find(form.getId());
        int[] ranges = levelRoles.ranges();
        if (!isNew) refuseLocked(d, ranges);
        Clean c = clean(form);
        validate(c, isNew ? null : d.getId());
        refuseLockedLevel(c.level, ranges);
        if (!isNew && Boolean.FALSE.equals(form.getActive()) && employeeCount(d.getId()) > 0 && !Boolean.FALSE.equals(d.getActive())) {
            throw new IllegalStateException("Cannot deactivate " + d.getDesignation() + " — employees still hold it");
        }
        apply(d, c);
        if (form.getActive() != null) d.setActive(form.getActive());
        if (d.getActive() == null) d.setActive(true);
        designations.saveAndFlush(d);
        return toDto(d, employeeCount(d.getId()), levelRoles.ranges());
    }

    /** Add Multiple: invalid / duplicate rows are skipped (with the reason). */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveMultiple(List<DesignationMasterDto> forms) {
        int saved = 0, skipped = 0;
        List<String> errors = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();
        Set<Integer> seenLevels = new HashSet<>();
        int row = 0;
        int[] ranges = levelRoles.ranges();
        for (DesignationMasterDto f : forms) {
            row++;
            Clean c = clean(f);
            try {
                if (c.name != null && !seenNames.add(c.name.toLowerCase())) throw new IllegalStateException(c.name + " repeated in this list");
                if (c.level != null && !seenLevels.add(c.level)) throw new IllegalStateException("hierarchy level " + c.level + " repeated in this list");
                validate(c, null);
                refuseLockedLevel(c.level, ranges);
            } catch (IllegalStateException e) {
                skipped++;
                errors.add("Row " + row + ": " + e.getMessage());
                continue;
            }
            Designation d = new Designation();
            apply(d, c);
            d.setActive(true);
            designations.saveAndFlush(d);
            saved++;
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("saved", saved);
        res.put("skipped", skipped);
        res.put("errors", errors);
        return res;
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public DesignationMasterDto updateStatus(Integer id, boolean active) {
        Designation d = find(id);
        refuseLocked(d, levelRoles.ranges());
        long holders = employeeCount(id);
        if (!active && holders > 0) {
            throw new IllegalStateException("Cannot deactivate " + d.getDesignation() + " — " + holders + " employee(s) still hold it");
        }
        d.setActive(active);
        designations.save(d);
        return toDto(d, holders, levelRoles.ranges());
    }

    /** What the form needs: the category choices and the company's role ranges. */
    public Map<String, Object> options() {
        int[] r = levelRoles.ranges();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("categories", CATEGORIES);
        m.put("managerFrom", r[0]);
        m.put("adminFrom", r[1]);
        m.put("superAdminFrom", r[2]);
        return m;
    }

    // ---- helpers ------------------------------------------------------------

    /** Admin / Super Admin level: managed in the database only. */
    private static boolean locked(Integer level, int[] ranges) {
        String role = LevelRoleService.roleFor(level, ranges);
        return LevelRoleService.ADMIN.equals(role) || LevelRoleService.SUPER_ADMIN.equals(role);
    }

    private static void refuseLocked(Designation d, int[] ranges) {
        if (locked(d.getHierarchyLevel(), ranges)) {
            throw new IllegalStateException(d.getDesignation() + " is an Admin / Super Admin designation — it cannot be changed or deactivated from the panel");
        }
    }

    private static void refuseLockedLevel(Integer level, int[] ranges) {
        if (level != null && locked(level, ranges)) {
            throw new IllegalStateException("Hierarchy level " + level + " is in the Admin range (" + ranges[1]
                    + "+) — Admin / Super Admin designations are managed by the backend only. Use a level below " + ranges[1]);
        }
    }

    private record Clean(String name, Integer level, Integer order, String grade, String category, String rawCategory) { }

    private static String category(String raw) {
        return raw == null ? null : CATEGORIES.stream().filter(c -> c.equalsIgnoreCase(raw)).findFirst().orElse(null);
    }

    private static Clean clean(DesignationMasterDto f) {
        String name = Strings.isBlank(f.getDesignation()) ? null : f.getDesignation().trim().replaceAll("\\s+", " ");
        String grade = Strings.isBlank(f.getGrade()) ? null : f.getGrade().trim();
        String raw = Strings.isBlank(f.getDesignationCategory()) ? null : f.getDesignationCategory().trim();
        return new Clean(name, f.getHierarchyLevel(), f.getDisplayOrder(), grade, category(raw), raw);
    }

    private void validate(Clean c, Integer selfId) {
        if (c.name == null) throw new IllegalStateException("Designation is required");
        if (c.name.length() > 50) throw new IllegalStateException("Designation can be at most 50 characters");
        if (c.level == null) throw new IllegalStateException("Hierarchy level is required");
        if (c.level < 1 || c.level > 9999) throw new IllegalStateException("Hierarchy level must be between 1 and 9999");
        if (c.order != null && (c.order < 0 || c.order > 9999)) throw new IllegalStateException("Display order must be between 0 and 9999");
        if (c.grade != null && c.grade.length() > 20) throw new IllegalStateException("Grade can be at most 20 characters");
        if (c.rawCategory != null && c.category == null) throw new IllegalStateException("Unknown category '" + c.rawCategory + "' — use " + String.join(" / ", CATEGORIES));
        designations.findFirstByDesignationIgnoreCase(c.name)
                .filter(other -> !other.getId().equals(selfId))
                .ifPresent(other -> { throw new IllegalStateException("Designation " + c.name + " already exists"); });
        designations.findByHierarchyLevel(c.level)
                .filter(other -> !other.getId().equals(selfId))
                .ifPresent(other -> { throw new IllegalStateException("Hierarchy level " + c.level + " is already used by " + other.getDesignation()); });
    }

    private void apply(Designation d, Clean c) {
        d.setDesignation(c.name);
        d.setHierarchyLevel(c.level);
        d.setGrade(c.grade);
        d.setDesignationCategory(c.category);
        if (c.order != null) d.setDisplayOrder(c.order);
        else if (d.getDisplayOrder() == null) d.setDisplayOrder(nextOrder());
    }

    private int nextOrder() {
        Integer max = jdbc.queryForObject("SELECT MAX(display_order) FROM designation_master", Integer.class);
        return max == null ? 1 : max + 1;
    }

    private long employeeCount(Integer id) {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM emp_detail WHERE designation = ?", Long.class, id);
        return n == null ? 0 : n;
    }

    private Designation find(Integer id) {
        return designations.findById(id).orElseThrow(() -> new IllegalStateException("Designation not found: " + id));
    }

    private static DesignationMasterDto toDto(Designation d, long employees, int[] ranges) {
        DesignationMasterDto dto = new DesignationMasterDto();
        dto.setId(d.getId());
        dto.setDesignation(d.getDesignation());
        dto.setHierarchyLevel(d.getHierarchyLevel());
        dto.setDisplayOrder(d.getDisplayOrder());
        dto.setGrade(d.getGrade());
        dto.setDesignationCategory(d.getDesignationCategory());
        dto.setActive(d.getActive());
        dto.setRole(LevelRoleService.roleFor(d.getHierarchyLevel(), ranges));
        dto.setLocked(locked(d.getHierarchyLevel(), ranges));
        dto.setEmployeeCount(employees);
        dto.setCreatedAt(d.getCreatedAt());
        return dto;
    }
}
