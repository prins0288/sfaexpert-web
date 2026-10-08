package in.opt.sfa.tenant.menu;

import in.opt.sfa.tenant.master.employee.entity.Employee;
import in.opt.sfa.tenant.master.employee.repository.EmployeeRepository;
import in.opt.sfa.security.Authz;
import in.opt.sfa.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Menu Visibility management API.
 *
 *   GET  /api/menu-visibility/employees        -> [{empId,name}] for the picker
 *   GET  /api/menu-visibility?target=ALL|SELF|<empId>  -> full menu tree + visible flags
 *   POST /api/menu-visibility  {target, items:{id:bool}} -> save
 *
 * Access: ALL and a specific <empId> require ADMIN/MANAGER; SELF (the caller's
 * own employee id) is allowed for anyone who has an employee record.
 */
@Tag(name = "Menu Visibility", description = "Show/hide menu items per employee, for everyone, or for yourself")
@RestController
@RequestMapping("/api/menu-visibility")
public class MenuVisibilityController {

    private final MenuVisibilityService service;
    private final EmployeeRepository employees;

    public MenuVisibilityController(MenuVisibilityService service, EmployeeRepository employees) {
        this.service = service;
        this.employees = employees;
    }

    public record EmpOption(String empId, String name) {}
    public record SaveRequest(String target, Map<Long, Boolean> items) {}
    public record ReorderRequest(String target, List<MenuVisibilityService.Move> moves) {}
    public record TypeRequest(Long id, String menuType) {}
    /** Both panels in one payload: WEB nested tree + APP flat card list. */
    public record Panels(List<MenuVisibilityService.VisNode> web, List<MenuVisibilityService.VisNode> app) {}

    @GetMapping("/employees")
    @Operation(summary = "Active employees for the visibility picker")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<EmpOption> employees() {
        Authz.requireRole("ADMIN", "MANAGER");
        return employees.findByStatusOrderByEmpNameAsc("Y").stream()
                .filter(e -> e.getEmpId() != null)
                .map(e -> new EmpOption(e.getEmpId(), e.getEmpName()))
                .toList();
    }

    @GetMapping
    @Operation(summary = "Both menu panels (WEB tree + APP cards) with effective visibility for a target")
    public Panels tree(@RequestParam String target) {
        String empId = resolveEmpId(target, false);
        return new Panels(service.tree(empId), service.appList(empId));
    }

    @PostMapping
    @Operation(summary = "Save visibility for a target (ALL = global, else per-employee)")
    public Panels save(@RequestBody SaveRequest req) {
        String empId = resolveEmpId(req.target(), true);
        Map<Long, Boolean> vals = req.items() == null ? Map.of() : req.items();
        if (empId == null) service.saveGlobal(vals);      // ALL / global
        else service.saveForEmp(empId, vals);             // employee-wise or self
        return new Panels(service.tree(empId), service.appList(empId));
    }

    @PostMapping("/reorder")
    @Operation(summary = "Persist drag reorder / re-parent. SELF = your own menu (any user); ALL/global and a specific employee are Admin/Manager only.")
    public Panels reorder(@RequestBody ReorderRequest req) {
        String empId = reorderEmpId(req.target());        // gates per target
        List<MenuVisibilityService.Move> moves = req.moves() == null ? List.of() : req.moves();
        if (empId == null) service.saveGlobalLayout(moves);   // ALL / global structure
        else service.saveEmpLayout(empId, moves);             // this employee's own arrangement
        return new Panels(service.tree(empId), service.appList(empId));
    }

    @PostMapping("/type")
    @Operation(summary = "Set an item's platform WEB|APP|BOTH (global; Admin/Manager only)")
    public Panels setType(@RequestBody TypeRequest req) {
        Authz.requireRole("ADMIN", "MANAGER");
        service.setMenuType(req.id(), req.menuType());
        return new Panels(service.tree(null), service.appList(null));
    }

    /** target -> empId for reorder (null == ALL/global). SELF (own menu) is
     *  allowed for anyone; ALL and a specific employee require Admin/Manager. */
    private String reorderEmpId(String target) {
        String t = target == null ? "ALL" : target.trim();
        if (t.isEmpty() || t.equalsIgnoreCase("ALL")) {
            Authz.requireRole("ADMIN", "MANAGER");
            return null;
        }
        if (t.equalsIgnoreCase("SELF")) {
            String empId = currentEmpId();
            if (empId == null) throw new IllegalStateException("Your login has no employee profile.");
            return empId;                                 // no role gate: your own menu
        }
        Authz.requireRole("ADMIN", "MANAGER");            // someone else's menu
        return t;
    }

    /**
     * Turn a target ("ALL" | "SELF" | "<empId>") into the empId to operate on
     * (null == global/ALL), enforcing the access rules.
     */
    private String resolveEmpId(String target, boolean writing) {
        String t = target == null ? "ALL" : target.trim();
        if (t.isEmpty() || t.equalsIgnoreCase("ALL")) {
            Authz.requireRole("ADMIN", "MANAGER");
            return null;                                  // global scope
        }
        if (t.equalsIgnoreCase("SELF")) {
            String empId = currentEmpId();
            if (empId == null) throw new IllegalStateException("Your login has no employee profile, so there is no personal menu to change.");
            return empId;                                 // no role gate for self
        }
        // a specific employee id -> admin/manager only
        Authz.requireRole("ADMIN", "MANAGER");
        return t;
    }

    /** The current login's employee id, from the verified JWT
     *  (user_login_master.emp_id -> emp_detail.emp_id). */
    private static String currentEmpId() {
        UserContext.CurrentUser u = UserContext.get();
        if (u == null || u.username() == null) throw new IllegalStateException("No authenticated user in context");
        return (u.empId() == null || u.empId().isBlank()) ? null : u.empId();
    }
}
