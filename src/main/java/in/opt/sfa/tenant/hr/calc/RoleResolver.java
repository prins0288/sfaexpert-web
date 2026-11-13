package in.opt.sfa.tenant.hr.calc;

import java.util.*;

/**
 * Pure role logic. Explicit roles come from hr_emp_role; implicit ones from the existing hierarchy level
 * (ADMIN -> HR_ADMIN, SUPER_ADMIN -> SUPER_ADMIN, MANAGER -> MANAGER, everybody -> EMPLOYEE). PAYROLL_APPROVER is never implicit.
 */
public final class RoleResolver {

    private RoleResolver() { }

    public static Set<String> roles(String levelRole, Collection<String> explicit) {
        Set<String> out = new LinkedHashSet<>();
        out.add("EMPLOYEE");
        if ("MANAGER".equals(levelRole)) out.add("MANAGER");
        if ("ADMIN".equals(levelRole)) { out.add("MANAGER"); out.add("HR_ADMIN"); }
        if ("SUPER_ADMIN".equals(levelRole)) { out.add("MANAGER"); out.add("HR_ADMIN"); out.add("SUPER_ADMIN"); }
        if (explicit != null) out.addAll(explicit);
        return out;
    }

    /** Union of the permission codes of every role; SUPER_ADMIN passes everything. */
    public static boolean allowed(Set<String> roles, Map<String, Set<String>> permissionsByRole, String permission) {
        if (roles.contains("SUPER_ADMIN")) return true;
        for (String r : roles) {
            Set<String> p = permissionsByRole.get(r);
            if (p != null && p.contains(permission)) return true;
        }
        return false;
    }

    /** Maker-checker: the person who prepared something may not approve it. */
    public static boolean canApprove(String makerEmpId, String approverEmpId) {
        return makerEmpId != null && approverEmpId != null && !makerEmpId.equals(approverEmpId);
    }
}
