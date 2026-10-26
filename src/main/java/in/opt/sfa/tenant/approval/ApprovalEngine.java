package in.opt.sfa.tenant.approval;

import in.opt.sfa.common.service.CompanySettingStore;
import in.opt.sfa.common.service.LevelRoleService;
import in.opt.sfa.common.util.Strings;
import in.opt.sfa.security.UserContext;
import in.opt.sfa.tenant.entity.Designation;
import in.opt.sfa.tenant.master.empdetail.entity.EmpDetail;
import in.opt.sfa.tenant.master.empdetail.repository.EmpDetailRepository;
import in.opt.sfa.tenant.master.permission.service.PermissionService;
import in.opt.sfa.tenant.repository.DesignationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Add / delete approval shared by the masters that need it (Client Master,
 * Area Master …). Each master has its own flow setting (Settings > General):
 *   AUTO           applied at once
 *   MANAGER        a manager of the row's employee approves -> applied
 *   ADMIN          an admin approves -> applied
 *   MANAGER_ADMIN  manager approves, then an admin gives the final approval (default)
 * A new row is INACTIVE until its add request is finally approved; a row asked
 * to be deleted stays active until the delete request is finally approved.
 * Roles come from the designation's hierarchy level (LevelRoleService). A manager
 * acts only for employees in his reporting chain (emp_detail.manager_id); in
 * Manager -> Admin an admin cannot skip the manager step unless no manager is
 * above the employee; the super admin can approve any step.
 * Add and delete have their own permissions. When the person raising a request
 * may approve steps of it, those steps are done at once (an admin's row is
 * approved directly, a manager's waits only for the admin in Manager -> Admin).
 * Nobody approves a request he raised, or one about a row that is his.
 * Every step comes back as a LogRow, which the master stores in its own history table.
 */
@Service
public class ApprovalEngine {

    public static final String ADD = "ADD", DELETE = "DELETE";
    public static final String PENDING_MANAGER = "PENDING_MANAGER", PENDING_ADMIN = "PENDING_ADMIN",
            APPROVED = "APPROVED", REJECTED = "REJECTED";
    public static final List<String> FLOWS = List.of("AUTO", "MANAGER", "ADMIN", "MANAGER_ADMIN");
    public static final String DEFAULT_FLOW = "MANAGER_ADMIN";

    static final int USER_RANK = 0, MANAGER_RANK = 1, ADMIN_RANK = 2, SUPER_RANK = 3;

    /** One master's approval settings: flow setting key, add / delete permission codes, and what to call a row. */
    public record Config(String flowKey, String addPermission, String deletePermission, String noun) { }

    /** One history row: what happened, the status after it, and who did it (snapshot). */
    public record LogRow(String requestType, String step, String action, String statusAfter, String remarks, Actor by) { }

    private final CompanySettingStore settings;
    private final PermissionService permissions;
    private final EmpDetailRepository employees;
    private final DesignationRepository designations;
    private final LevelRoleService levelRoles;

    public ApprovalEngine(CompanySettingStore settings, PermissionService permissions, EmpDetailRepository employees,
                          DesignationRepository designations, LevelRoleService levelRoles) {
        this.settings = settings;
        this.permissions = permissions;
        this.employees = employees;
        this.designations = designations;
        this.levelRoles = levelRoles;
    }

    /** The company's flow for this master. */
    public String flow(Config cfg) {
        String f = settings.value(cfg.flowKey(), DEFAULT_FLOW);
        return FLOWS.contains(f) ? f : DEFAULT_FLOW;
    }

    /** The signed-in user with role, permissions and team — build once per request. */
    public Actor actor(Config cfg) {
        UserContext.CurrentUser u = UserContext.get();
        String role = u == null ? null : u.role();
        Map<String, EmpDetail> emps = new HashMap<>();
        employees.findAll().forEach(e -> emps.put(e.getEmpId(), e));
        Map<Integer, Designation> desig = new HashMap<>();
        designations.findAll().forEach(d -> desig.put(d.getId(), d));
        return new Actor(u == null ? null : u.empId(), role, rank(role),
                permissions.isAllowed(cfg.addPermission()), permissions.isAllowed(cfg.deletePermission()),
                emps, desig, levelRoles.ranges());
    }

    /** The signed-in user for reach checks only (own / team / all) — no approval permissions needed. */
    public Actor reach() {
        UserContext.CurrentUser u = UserContext.get();
        String role = u == null ? null : u.role();
        Map<String, EmpDetail> emps = new HashMap<>();
        employees.findAll().forEach(e -> emps.put(e.getEmpId(), e));
        Map<Integer, Designation> desig = new HashMap<>();
        designations.findAll().forEach(d -> desig.put(d.getId(), d));
        return new Actor(u == null ? null : u.empId(), role, rank(role), true, true, emps, desig, levelRoles.ranges());
    }

    // ---- state -------------------------------------------------------------------

    public static String status(Approvable a, String req) {
        return DELETE.equals(req) ? a.getDeleteStatus() : a.getApprovalStatus();
    }

    private static void setStatus(Approvable a, String req, String st) {
        if (DELETE.equals(req)) a.setDeleteStatus(st); else a.setApprovalStatus(st);
    }

    /** ADD while a new row waits, DELETE while a delete request waits, else null. */
    public static String pendingRequest(Approvable a) {
        String s = a.getApprovalStatus();
        if (PENDING_MANAGER.equals(s) || PENDING_ADMIN.equals(s)) return ADD;
        String d = a.getDeleteStatus();
        if (Boolean.TRUE.equals(a.getDeleteRequested()) && (PENDING_MANAGER.equals(d) || PENDING_ADMIN.equals(d))) return DELETE;
        return null;
    }

    public static boolean isApproved(Approvable a) {
        return APPROVED.equals(a.getApprovalStatus());
    }

    private static String requester(Approvable a, String req) {
        if (DELETE.equals(req)) return a.getDeleteRequestedBy();
        return a.getAddRequestedBy() != null ? a.getAddRequestedBy() : a.getCreatedBy();
    }

    /** The request is approved: a new row becomes active, a deleted one inactive. */
    private static void apply(Approvable a, String req, String who) {
        if (ADD.equals(req)) {
            a.setApprovalStatus(APPROVED);
            a.finalApproval(who, LocalDateTime.now());
            a.activeNow(true);
        } else {
            a.setDeleteStatus(APPROVED);
            a.setDeleteRequested(false);
            a.activeNow(false);
        }
    }

    private static void managerStep(Approvable a, String req, String who) {
        if (ADD.equals(req)) { a.setManagerApprovedBy(who); a.setManagerApprovedOn(LocalDateTime.now()); }
    }

    // ---- requests ----------------------------------------------------------------

    /**
     * Raise an ADD request (new row, or corrected after a rejection) by the signed-in
     * user. The steps they may approve themselves are done at once. Returns the
     * history rows to store once the row has its id.
     */
    public List<LogRow> requestAdd(Approvable a, Config cfg, Actor me) {
        a.setAddRequestedBy(me.empId);
        a.setManagerApprovedBy(null);
        a.setManagerApprovedOn(null);
        a.finalApproval(null, null);
        a.setRejectedBy(null);
        a.setRejectedOn(null);
        a.setRejectReason(null);
        a.activeNow(false);
        return start(a, ADD, null, cfg, me);
    }

    /** Ask for the row to be deleted (made inactive); it stays active until the request is finally approved. */
    public List<LogRow> requestDelete(Approvable a, String reason, Config cfg, Actor me) {
        if (!a.isActiveNow()) throw new IllegalStateException("This " + cfg.noun() + " is already inactive");
        if (!isApproved(a)) throw new IllegalStateException("This " + cfg.noun() + " is not approved yet — reject its add request instead");
        if (Boolean.TRUE.equals(a.getDeleteRequested())) throw new IllegalStateException("A delete request is already waiting for approval");
        String r = reason(reason, "Please give the reason for deleting this " + cfg.noun());
        a.setDeleteRequested(true);
        a.setDeleteRequestedBy(me.empId);
        a.setDeleteRequestReason(r);
        return start(a, DELETE, r, cfg, me);
    }

    private List<LogRow> start(Approvable a, String req, String remarks, Config cfg, Actor me) {
        String flow = flow(cfg);
        boolean may = me.allowed(req);
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{ "REQUEST", "REQUESTED", remarks });
        String first = switch (flow) { case "AUTO" -> null; case "ADMIN" -> PENDING_ADMIN; default -> PENDING_MANAGER; };
        boolean done;
        if (first == null) {
            done = true;
            rows.add(new String[]{ "AUTO", "APPROVED", "Auto approval (Settings)" });
        } else if (may && me.rank >= ADMIN_RANK) {
            done = true;   // an admin / super admin may approve every step
            managerStep(a, req, me.empId);
            rows.add(new String[]{ "FINAL", "APPROVED", "Raised by an approver: applied directly" });
        } else if (may && me.rank == MANAGER_RANK && PENDING_MANAGER.equals(first)) {
            managerStep(a, req, me.empId);
            done = "MANAGER".equals(flow);
            rows.add(new String[]{ "MANAGER", "APPROVED", "Raised by a manager: manager step done" });
            if (!done) setStatus(a, req, PENDING_ADMIN);
        } else {
            done = false;
            setStatus(a, req, first);
        }
        if (done) apply(a, req, me.empId);
        String after = status(a, req);
        return rows.stream().map(r -> new LogRow(req, r[0], r[1], after, r[2], me)).toList();
    }

    /** Approve the step the row's open request (add or delete) is waiting for. */
    public LogRow approve(Approvable a, Config cfg, Actor me) {
        String req = pendingRequest(a);
        String why = whyNot(a, req, cfg, me);
        if (why != null) throw new IllegalStateException(why);
        String st = status(a, req), flow = flow(cfg);
        String step;
        boolean managerOnlyStep = PENDING_MANAGER.equals(st) && me.rank < ADMIN_RANK
                && ("MANAGER_ADMIN".equals(flow) || "ADMIN".equals(flow));   // an admin's approval is still needed
        if (managerOnlyStep) {
            managerStep(a, req, me.empId);
            setStatus(a, req, PENDING_ADMIN);
            step = "MANAGER";
        } else {
            if (PENDING_MANAGER.equals(st)) managerStep(a, req, me.empId);
            apply(a, req, me.empId);
            step = PENDING_ADMIN.equals(st) || me.rank >= ADMIN_RANK ? "FINAL" : "MANAGER";
        }
        return new LogRow(req, step, "APPROVED", status(a, req), null, me);
    }

    /** Reject the open request (reason required). A rejected new row goes for approval again when edited. */
    public LogRow reject(Approvable a, String reason, Config cfg, Actor me) {
        String req = pendingRequest(a);
        String why = whyNot(a, req, cfg, me);
        if (why != null) throw new IllegalStateException(why.replace("approve", "reject"));
        String r = reason(reason, "Please give the reason for rejecting");
        String step = PENDING_ADMIN.equals(status(a, req)) || me.rank >= ADMIN_RANK ? "ADMIN" : "MANAGER";
        if (ADD.equals(req)) {
            a.setApprovalStatus(REJECTED);
            a.finalApproval(null, null);
            a.activeNow(false);
            a.setRejectedBy(me.empId);
            a.setRejectedOn(LocalDateTime.now());
            a.setRejectReason(r);
        } else {
            a.setDeleteStatus(REJECTED);
            a.setDeleteRequested(false);   // the row simply stays active
        }
        return new LogRow(req, step, "REJECTED", status(a, req), r, me);
    }

    private static String reason(String raw, String missing) {
        String r = Strings.isBlank(raw) ? null : raw.trim();
        if (r == null) throw new IllegalStateException(missing);
        if (r.length() > 255) throw new IllegalStateException("Reason can be at most 255 characters");
        return r;
    }

    /** Can this user approve / reject the row's open request right now? */
    public boolean canApprove(Approvable a, Config cfg, Actor me) {
        String req = pendingRequest(a);
        return req != null && whyNot(a, req, cfg, me) == null;
    }

    /** null = this user may approve (or reject) the request's current step; else the reason why not. */
    public String whyNot(Approvable a, String req, Config cfg, Actor me) {
        if (req == null) return "Nothing is waiting for approval on this " + cfg.noun();
        String kind = ADD.equals(req) ? "new " + cfg.noun() : "delete request";
        if (!me.allowed(req)) return "You are not allowed to approve a " + kind + " (permission "
                + (ADD.equals(req) ? cfg.addPermission() : cfg.deletePermission()) + ")";
        if (me.empId != null && me.empId.equals(requester(a, req))) return "You cannot approve your own " + kind + " — someone above you has to";
        String owner = a.approvalOwner();
        if (me.empId != null && me.empId.equals(owner)) return "This " + cfg.noun() + " is yours — someone above you has to approve";
        if (me.rank >= SUPER_RANK) return null;
        String st = status(a, req);
        if (PENDING_ADMIN.equals(st)) return me.rank >= ADMIN_RANK ? null : "Waiting for an admin's final approval";
        // PENDING_MANAGER
        if (me.rank == MANAGER_RANK) {
            // the manager step belongs to the owner's IMMEDIATE manager (emp_detail.manager_id); only if he is
            // inactive / vacant / the requester himself does the next manager up the chain take it
            if (owner == null) return null;
            String who = me.stepManager(owner, requester(a, req));
            return me.empId != null && me.empId.equals(who) ? null
                    : "Only " + (who == null ? "the manager" : me.name(who)) + ", the immediate manager of " + me.name(owner) + ", can approve this " + kind;
        }
        if (me.rank >= ADMIN_RANK) {
            // Manager -> Admin: the manager step is the manager's; an admin takes it only when nobody of
            // manager level (other than the requester) is above the employee. Other flows: an admin may stand in.
            if (!"MANAGER_ADMIN".equals(flow(cfg))) return null;
            return owner == null || me.stepManager(owner, requester(a, req)) == null ? null
                    : "Waiting for the manager's approval first (then the admin gives the final approval)";
        }
        return "Only a manager or an admin can approve";
    }

    static int rank(String role) {
        if (role == null) return USER_RANK;
        return switch (role) {
            case "SUPER_ADMIN" -> SUPER_RANK;
            case "ADMIN" -> ADMIN_RANK;
            case "MANAGER" -> MANAGER_RANK;
            default -> USER_RANK;
        };
    }

    /** The signed-in user: role, approve permissions, the employees reporting to him (any depth), details for the log. */
    public static final class Actor {
        public final String empId, role;
        final int rank;
        final boolean addAllowed, deleteAllowed;
        final Set<String> team = new HashSet<>();
        final Map<String, EmpDetail> emps;
        final Map<Integer, Designation> desig;
        final int[] ranges;

        Actor(String empId, String role, int rank, boolean addAllowed, boolean deleteAllowed,
              Map<String, EmpDetail> emps, Map<Integer, Designation> desig, int[] ranges) {
            this.empId = empId;
            this.role = role;
            this.rank = rank;
            this.addAllowed = addAllowed;
            this.deleteAllowed = deleteAllowed;
            this.emps = emps;
            this.desig = desig;
            this.ranges = ranges;
            if (empId == null) return;
            Map<String, List<String>> reports = new HashMap<>();
            emps.values().forEach(e -> { if (e.getManagerId() != null) reports.computeIfAbsent(e.getManagerId(), k -> new ArrayList<>()).add(e.getEmpId()); });
            Deque<String> todo = new ArrayDeque<>(reports.getOrDefault(empId, List.of()));
            while (!todo.isEmpty()) {
                String id = todo.pop();
                if (team.add(id)) todo.addAll(reports.getOrDefault(id, List.of()));
            }
        }

        boolean allowed(String req) { return ADD.equals(req) ? addAllowed : deleteAllowed; }

        /** Admin or super admin. */
        public boolean isAdmin() { return rank >= ADMIN_RANK; }

        /**
         * Is a row of this employee within the user's reach? Admin: every row;
         * manager: his own and his team's (any depth); user: only his own.
         */
        public boolean reaches(String ownerEmpId) {
            if (isAdmin()) return true;
            if (ownerEmpId == null || empId == null) return false;
            if (ownerEmpId.equals(empId)) return true;
            return rank == MANAGER_RANK && team.contains(ownerEmpId);
        }

        /**
         * Who handles the manager step for {of}'s row: his immediate manager, or — if that one is inactive,
         * vacant or the requester — the next one up the chain. null = nobody (an admin takes it).
         */
        String stepManager(String of, String requester) {
            Set<String> seen = new HashSet<>();
            EmpDetail e = emps.get(of);
            while (e != null && e.getManagerId() != null && seen.add(e.getEmpId())) {
                EmpDetail m = emps.get(e.getManagerId());
                if (m == null) return null;
                if (m.isActive() && !m.isVacant() && !m.getEmpId().equals(requester)) return m.getEmpId();
                e = m;
            }
            return null;
        }

        /** Is anyone with a Manager role, other than `except`, above this employee in the reporting chain? */
        boolean hasManagerAbove(String of, String except) {
            Set<String> seen = new HashSet<>();
            EmpDetail e = emps.get(of);
            while (e != null && e.getManagerId() != null && seen.add(e.getEmpId())) {
                EmpDetail m = emps.get(e.getManagerId());
                if (m == null) return false;
                Designation d = m.getDesignationId() == null ? null : desig.get(m.getDesignationId());
                boolean manager = d != null && LevelRoleService.MANAGER.equals(LevelRoleService.roleFor(d.getHierarchyLevel(), ranges));
                if (manager && m.isActive() && !m.getEmpId().equals(except)) return true;
                e = m;
            }
            return false;
        }

        public String name(String id) {
            EmpDetail e = id == null ? null : emps.get(id);
            return e == null ? id : e.getEmpName();
        }

        // ---- snapshot for the history log ----
        public String empCode() { EmpDetail e = me(); return e == null ? null : e.getEmpCode(); }
        public String empName() { EmpDetail e = me(); return e == null ? null : e.getEmpName(); }
        public String designationName() { Designation d = myDesignation(); return d == null ? null : d.getDesignation(); }
        public Integer hierarchyLevel() { Designation d = myDesignation(); return d == null ? null : d.getHierarchyLevel(); }

        private EmpDetail me() { return empId == null ? null : emps.get(empId); }
        private Designation myDesignation() {
            EmpDetail e = me();
            return e == null || e.getDesignationId() == null ? null : desig.get(e.getDesignationId());
        }
    }
}
