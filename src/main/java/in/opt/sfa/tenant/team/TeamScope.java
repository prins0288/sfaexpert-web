package in.opt.sfa.tenant.team;

import in.opt.sfa.common.service.LevelRoleService;
import in.opt.sfa.exception.ForbiddenException;
import in.opt.sfa.security.UserContext;
import in.opt.sfa.tenant.master.empdetail.entity.EmpDetail;
import in.opt.sfa.tenant.master.empdetail.repository.EmpDetailRepository;
import in.opt.sfa.tenant.repository.DesignationRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * THE one place that answers "whose data may the signed-in user see / touch?"
 * from the team hierarchy (emp_detail.manager_id) and the role (hierarchy-level
 * ranges: user below Manager level, manager, admin). Every master / report that
 * is "per employee" (Client Master, Area Master, …) filters with this.
 *
 *   SELF_ONLY  only my own rows (every role)
 *   TEAM       my team, without me (manager: everyone under me, any depth; user: nobody; admin: everyone)
 *   TEAM_SELF  me + my team (user: only me; manager: me + team; admin: everyone)  <- normal "show my data"
 *   ALL        everything (admin only; anyone else gets TEAM_SELF)
 *
 * ids() returns null for "no restriction" (admin), else the allowed emp_ids — a
 * user can therefore never get another user's rows, and a manager only his own
 * and his team's. Adding data is a separate rule: see canAddFor() — only for
 * yourself (admin: for anyone).
 */
@Service
public class TeamScope {

    public enum Scope { SELF_ONLY, TEAM, TEAM_SELF, ALL }

    private final EmpDetailRepository employees;
    private final DesignationRepository designations;
    private final LevelRoleService levelRoles;

    public TeamScope(EmpDetailRepository employees, DesignationRepository designations, LevelRoleService levelRoles) {
        this.employees = employees;
        this.designations = designations;
        this.levelRoles = levelRoles;
    }

    /**
     * Employees whose designation is Admin / Super Admin level. They never appear in an employee dropdown
     * (mapped employee, manager, team, permission targets …) — use this to leave them out.
     */
    public Set<String> adminLevelIds() {
        int[] ranges = levelRoles.ranges();
        Map<Integer, Integer> level = new HashMap<>();
        designations.findAll().forEach(d -> level.put(d.getId(), d.getHierarchyLevel()));
        Set<String> out = new HashSet<>();
        employees.findAll().forEach(e -> {
            Integer l = e.getDesignationId() == null ? null : level.get(e.getDesignationId());
            String role = LevelRoleService.roleFor(l, ranges);
            if (LevelRoleService.ADMIN.equals(role) || LevelRoleService.SUPER_ADMIN.equals(role)) out.add(e.getEmpId());
        });
        return out;
    }

    public static String me() {
        UserContext.CurrentUser u = UserContext.get();
        return u == null ? null : u.empId();
    }

    public static boolean isAdmin() {
        UserContext.CurrentUser u = UserContext.get();
        return u != null && ("ADMIN".equals(u.role()) || "SUPER_ADMIN".equals(u.role()));
    }

    public static boolean isManager() {
        UserContext.CurrentUser u = UserContext.get();
        return u != null && "MANAGER".equals(u.role());
    }

    /** The emp_ids whose rows the signed-in user may see for this scope; null = everyone (no restriction). */
    public Set<String> ids(Scope scope) {
        String me = me();
        Set<String> self = me == null ? Set.of() : Set.of(me);
        if (scope == Scope.SELF_ONLY) return self;
        if (isAdmin()) return null;
        if (!isManager()) return scope == Scope.TEAM ? Set.of() : self;
        Set<String> team = team(me);
        if (scope == Scope.TEAM) return team;
        Set<String> all = new HashSet<>(team);
        if (me != null) all.add(me);
        return all;
    }

    /** May the user see / touch a row that belongs to {ownerEmpId}? (a row with no owner is admin-only) */
    public boolean allows(Scope scope, String ownerEmpId) {
        Set<String> ids = ids(scope);
        return ids == null || (ownerEmpId != null && ids.contains(ownerEmpId));
    }

    public void require(Scope scope, String ownerEmpId, String message) {
        if (!allows(scope, ownerEmpId)) throw new ForbiddenException(message);
    }

    /** Filter a list of rows down to the user's scope. */
    public <T> List<T> filter(Scope scope, Collection<T> rows, java.util.function.Function<T, String> ownerOf) {
        Set<String> ids = ids(scope);
        return rows.stream().filter(r -> ids == null || (ownerOf.apply(r) != null && ids.contains(ownerOf.apply(r)))).toList();
    }

    /** New data is added only for yourself — admin may add for anyone. */
    public boolean canAddFor(String empId) {
        return isAdmin() || (empId != null && empId.equals(me()));
    }

    /** Everyone under {empId}, any depth, not including him. */
    public Set<String> team(String empId) {
        Set<String> team = new HashSet<>();
        if (empId == null) return team;
        Map<String, List<String>> reports = new HashMap<>();
        employees.findAll().forEach(e -> {
            if (e.getManagerId() != null) reports.computeIfAbsent(e.getManagerId(), k -> new ArrayList<>()).add(e.getEmpId());
        });
        Deque<String> todo = new ArrayDeque<>(reports.getOrDefault(empId, List.of()));
        while (!todo.isEmpty()) {
            String id = todo.pop();
            if (team.add(id)) todo.addAll(reports.getOrDefault(id, List.of()));
        }
        return team;
    }

    /** Direct juniors of {empId} (one level). */
    public Set<String> directReports(String empId) {
        Set<String> out = new HashSet<>();
        employees.findAll().forEach(e -> { if (empId != null && empId.equals(e.getManagerId())) out.add(e.getEmpId()); });
        return out;
    }

    /** Immediate manager first, then his manager … up to the top (the user's seniors). */
    public List<String> seniors(String empId) {
        Map<String, EmpDetail> by = new HashMap<>();
        employees.findAll().forEach(e -> by.put(e.getEmpId(), e));
        List<String> chain = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        EmpDetail e = by.get(empId);
        while (e != null && e.getManagerId() != null && seen.add(e.getManagerId())) {
            chain.add(e.getManagerId());
            e = by.get(e.getManagerId());
        }
        return chain;
    }

    public String immediateManager(String empId) {
        return employees.findById(empId).map(EmpDetail::getManagerId).orElse(null);
    }
}
