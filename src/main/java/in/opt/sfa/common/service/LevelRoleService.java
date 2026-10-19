package in.opt.sfa.common.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The authorization role comes from the hierarchy_level of the employee's
 * designation (designation_master.hierarchy_level), by the company's ranges
 * (Settings > General > Role by hierarchy level):
 *   level >= roleSuperAdminFromLevel -> SUPER_ADMIN
 *   level >= roleAdminFromLevel      -> ADMIN
 *   level >= roleManagerFromLevel    -> MANAGER
 *   otherwise (or no designation)    -> USER
 * Needs a tenant bound (TenantContext) — the ranges are per company.
 */
@Service
public class LevelRoleService {

    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ADMIN = "ADMIN";
    public static final String MANAGER = "MANAGER";
    public static final String USER = "USER";

    public static final String MANAGER_KEY = "roleManagerFromLevel";
    public static final String ADMIN_KEY = "roleAdminFromLevel";
    public static final String SUPER_ADMIN_KEY = "roleSuperAdminFromLevel";
    public static final List<String> KEYS = List.of(MANAGER_KEY, ADMIN_KEY, SUPER_ADMIN_KEY);

    /** Used until the company saves its own ranges. */
    public static final Map<String, String> DEFAULTS = Map.of(MANAGER_KEY, "15", ADMIN_KEY, "50", SUPER_ADMIN_KEY, "90");

    private final CompanySettingStore settings;

    public LevelRoleService(CompanySettingStore settings) {
        this.settings = settings;
    }

    /** The company's three "from level" values: manager, admin, super admin. */
    public int[] ranges() {
        return new int[]{ num(MANAGER_KEY), num(ADMIN_KEY), num(SUPER_ADMIN_KEY) };
    }

    public String roleFor(Integer level) {
        return roleFor(level, ranges());
    }

    public static String roleFor(Integer level, int[] r) {
        if (level == null) return USER;
        if (level >= r[2]) return SUPER_ADMIN;
        if (level >= r[1]) return ADMIN;
        if (level >= r[0]) return MANAGER;
        return USER;
    }

    /** Checks new range values (only the keys given; the others keep their stored value). Throws if invalid. */
    public void validate(Map<String, String> changes) {
        Map<String, Integer> v = new LinkedHashMap<>();
        for (String k : KEYS) {
            String raw = changes.containsKey(k) ? changes.get(k) : String.valueOf(num(k));
            try {
                v.put(k, Integer.parseInt(raw == null ? "" : raw.trim()));
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Role levels must be whole numbers");
            }
        }
        int m = v.get(MANAGER_KEY), a = v.get(ADMIN_KEY), s = v.get(SUPER_ADMIN_KEY);
        if (m < 1 || s > 9999) throw new IllegalStateException("Role levels must be between 1 and 9999");
        if (!(m < a && a < s)) throw new IllegalStateException("Role levels must rise: Manager < Admin < Super Admin");
    }

    private int num(String key) {
        try {
            return Integer.parseInt(settings.value(key, DEFAULTS.get(key)).trim());
        } catch (NumberFormatException e) {
            return Integer.parseInt(DEFAULTS.get(key));
        }
    }
}
