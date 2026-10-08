package in.opt.sfa.common.util;

/**
 * Maps an employee level (emp_detail.emp_level, which comes from the designation)
 * to an authorization role used by Authz.requireRole(...).
 *
 * Default thresholds (adjust as the hierarchy needs — these can later be moved to
 * company_setting_master to make them per-company configurable):
 *   >= 21  -> SUPER_ADMIN
 *    9..20 -> ADMIN
 *    5..8  -> MANAGER
 *    1..4  -> USER
 */
public final class RoleLevelMapper {

    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ADMIN = "ADMIN";
    public static final String MANAGER = "MANAGER";
    public static final String USER = "USER";

    private RoleLevelMapper() { }

    public static String roleFor(Integer empLevel) {
        int lvl = (empLevel == null) ? 0 : empLevel;
        if (lvl >= 21) return SUPER_ADMIN;
        if (lvl >= 9)  return ADMIN;
        if (lvl >= 5)  return MANAGER;
        return USER;
    }
}
