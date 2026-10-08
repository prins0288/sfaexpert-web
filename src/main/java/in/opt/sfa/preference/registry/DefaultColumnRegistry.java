package in.opt.sfa.preference.registry;

import in.opt.sfa.preference.dto.ColumnConfig;

import java.util.*;

/**
 * Default columns of every grid that supports "Column Settings".
 *
 * Adding a grid = ONE entry here, plus on the page: data-screen-key="KEY" on
 * the <table> and data-field="..." on each configurable <th>. Nothing else —
 * the controller, service, storage and the front-end component are generic.
 *
 * The list order is the default display order. Columns not listed here
 * (S.No, Actions) are not configurable and stay where the page puts them.
 * A field later added to an entry shows up for users who already saved a
 * layout (merged in next to its default neighbour); a field removed or
 * renamed here is silently dropped from saved layouts.
 */
public final class DefaultColumnRegistry {

    private static final Map<String, List<ColumnConfig>> REGISTRY = new LinkedHashMap<>();

    static {
        register("DCR_REPORT",
                col("date", "common.date"),
                col("employee", "entity.employee"),
                col("division", "entity.division"),
                col("route", "entity.route"),
                col("area", "entity.area"),
                col("client", "entity.client"),
                col("clientType", "entity.clientType"),
                col("workType", "report.dcr.workType"),
                col("timeIn", "report.dcr.timeIn"),
                col("timeOut", "report.dcr.timeOut"),
                col("remarks", "common.remarks"));

        register("MENU_AUDIT_REPORT",
                col("menuItem", "report.menuAudit.menuItem"),
                col("type", "common.type"),
                col("action", "report.menuAudit.action"),
                col("changedBy", "report.menuAudit.changedBy"),
                col("changedAt", "report.menuAudit.changedAt"),
                col("details", "report.menuAudit.details"));

        register("CATEGORY_MASTER",
                col("code", "common.code"),
                col("name", "common.name"),
                col("icon", "common.icon"),
                col("description", "common.description"),
                col("status", "common.status"),
                col("created", "common.created"),
                col("updated", "common.updated"));
    }

    private DefaultColumnRegistry() { }

    private static ColumnConfig col(String field, String labelKey) {
        return new ColumnConfig(field, labelKey, true);
    }

    /** Use for a column that exists but should start hidden. */
    @SuppressWarnings("unused")
    private static ColumnConfig hidden(String field, String labelKey) {
        return new ColumnConfig(field, labelKey, false);
    }

    private static void register(String screenKey, ColumnConfig... columns) {
        Set<String> seen = new HashSet<>();
        List<ColumnConfig> list = new ArrayList<>();
        for (int i = 0; i < columns.length; i++) {
            if (!seen.add(columns[i].getField())) {
                throw new IllegalStateException("Duplicate field '" + columns[i].getField() + "' in " + screenKey);
            }
            columns[i].setOrder(i);
            list.add(columns[i]);
        }
        if (REGISTRY.put(screenKey, Collections.unmodifiableList(list)) != null) {
            throw new IllegalStateException("Screen registered twice: " + screenKey);
        }
    }

    public static boolean isRegistered(String screenKey) {
        return screenKey != null && REGISTRY.containsKey(screenKey);
    }

    /** A fresh, mutable copy of the defaults (callers may edit it freely); empty if unknown. */
    public static List<ColumnConfig> defaults(String screenKey) {
        List<ColumnConfig> list = REGISTRY.get(screenKey);
        if (list == null) return new ArrayList<>();
        List<ColumnConfig> copy = new ArrayList<>(list.size());
        list.forEach(c -> copy.add(c.copy()));
        return copy;
    }

    public static Set<String> screenKeys() {
        return Collections.unmodifiableSet(REGISTRY.keySet());
    }
}
