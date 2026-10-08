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

        // master/activity-type.html
        register("ACTIVITY_TYPE_MASTER",
                col("code", "common.code"),
                col("name", "common.name"),
                col("clientType", "entity.clientType"),
                col("status", "common.status"));

        // master/area.html
        register("AREA_MASTER",
                col("code", "common.code"),
                col("name", "common.name"),
                col("city", "common.city"),
                col("state", "entity.state"),
                col("hq", "entity.hq"),
                col("pincode", "common.pincode"),
                col("type", "common.type"),
                col("routes", "entity.routes"),
                col("status", "common.status"));

        // master/bank-master.html
        register("BANK_MASTER",
                col("code", "common.code"),
                col("name", "common.name"),
                col("shortName", null),
                col("description", "common.description"),
                col("order", null),
                col("status", "common.status"),
                col("created", "common.created"),
                col("updated", "common.updated"));

        // master/client-type.html
        register("CLIENT_TYPE_MASTER",
                col("code", "common.code"),
                col("name", "common.name"),
                col("singular", null),
                col("plural", null),
                col("status", "common.status"));

        // master/client.html
        register("CLIENT_MASTER",
                col("code", "common.code"),
                col("type", "common.type"),
                col("name", "common.name"),
                col("area", "entity.area"),
                col("route", "entity.route"),
                col("mobile", "common.mobile"),
                col("category", "entity.category"),
                col("status", "common.status"));

        // master/country.html
        register("COUNTRY_MASTER",
                col("code", "common.code"),
                col("name", "common.name"),
                col("status", "common.status"));

        // master/create-employee.html
        register("CREATE_EMPLOYEE_MASTER",
                col("empCode", null),
                col("name", "common.name"),
                col("mobile", "common.mobile"),
                col("designation", "entity.designation"),
                col("division", "entity.division"),
                col("username", null),
                col("active", "common.active"),
                hidden("gender", null),
                hidden("officialEmail", null),
                hidden("department", null),
                hidden("dateOfJoining", null),
                hidden("manager", null),
                hidden("profileComplete", null));

        // master/degree-master.html
        register("DEGREE_MASTER",
                col("code", "common.code"),
                col("name", "common.name"),
                col("shortName", null),
                col("description", "common.description"),
                col("order", null),
                col("status", "common.status"),
                col("created", "common.created"),
                col("updated", "common.updated"));

        // master/division.html
        register("DIVISION_MASTER",
                col("code", "common.code"),
                col("name", "common.name"),
                col("status", "common.status"));

        // master/document-master.html
        register("DOCUMENT_MASTER",
                col("code", "common.code"),
                col("name", "common.name"),
                col("shortName", null),
                col("description", "common.description"),
                col("order", null),
                col("status", "common.status"),
                col("created", "common.created"),
                col("updated", "common.updated"));

        // master/hq-group-master.html
        register("HQ_GROUP_MASTER",
                col("code", "common.code"),
                col("name", "common.name"),
                col("shortName", null),
                col("description", "common.description"),
                col("order", null),
                col("status", "common.status"),
                col("created", "common.created"),
                col("updated", "common.updated"));

        // master/hq.html
        register("HQ_MASTER",
                col("code", "common.code"),
                col("name", "common.name"),
                col("state", "entity.state"),
                col("hqGroup", null),
                col("status", "common.status"));

        // master/image-type-master.html
        register("IMAGE_TYPE_MASTER",
                col("code", "common.code"),
                col("name", "common.name"),
                col("shortName", null),
                col("description", "common.description"),
                col("order", null),
                col("status", "common.status"),
                col("created", "common.created"),
                col("updated", "common.updated"));

        // master/item-type-master.html
        register("ITEM_TYPE_MASTER",
                col("code", "common.code"),
                col("name", "common.name"),
                col("shortName", null),
                col("description", "common.description"),
                col("order", null),
                col("status", "common.status"),
                col("created", "common.created"),
                col("updated", "common.updated"));

        // master/meeting-type-master.html
        register("MEETING_TYPE_MASTER",
                col("code", "common.code"),
                col("name", "common.name"),
                col("shortName", null),
                col("description", "common.description"),
                col("order", null),
                col("status", "common.status"),
                col("created", "common.created"),
                col("updated", "common.updated"));

        // master/products.html
        register("PRODUCTS_MASTER",
                col("id", null),
                col("name", "common.name"),
                col("price", null));

        // master/route-area.html
        register("ROUTE_AREA_MASTER",
                col("routeCode", null),
                col("route", "entity.route"),
                col("areaCode", null),
                col("area", "entity.area"),
                col("visitSeq", null),
                col("status", "common.status"));

        // master/route.html
        register("ROUTE_MASTER",
                col("code", "common.code"),
                col("name", "common.name"),
                col("division", "entity.division"),
                col("zone", "entity.zone"),
                col("state", "entity.state"),
                col("hq", "entity.hq"),
                col("distanceKm", null),
                col("status", "common.status"));

        // master/speciality-master.html
        register("SPECIALITY_MASTER",
                col("code", "common.code"),
                col("name", "common.name"),
                col("icon", "common.icon"),
                col("description", "common.description"),
                col("status", "common.status"),
                col("created", "common.created"),
                col("updated", "common.updated"));

        // master/sponsorship-type-master.html
        register("SPONSORSHIP_TYPE_MASTER",
                col("code", "common.code"),
                col("name", "common.name"),
                col("shortName", null),
                col("description", "common.description"),
                col("order", null),
                col("status", "common.status"),
                col("created", "common.created"),
                col("updated", "common.updated"));

        // master/state.html
        register("STATE_MASTER",
                col("code", "common.code"),
                col("name", "common.name"),
                col("country", "entity.country"),
                col("status", "common.status"));

        // master/travel-type-master.html
        register("TRAVEL_TYPE_MASTER",
                col("code", "common.code"),
                col("name", "common.name"),
                col("shortName", null),
                col("description", "common.description"),
                col("order", null),
                col("status", "common.status"),
                col("created", "common.created"),
                col("updated", "common.updated"));

        // master/visit-type.html
        register("VISIT_TYPE_MASTER",
                col("code", "common.code"),
                col("name", "common.name"),
                col("clientType", "entity.clientType"),
                col("status", "common.status"));

        // master/zone.html
        register("ZONE_MASTER",
                col("code", "common.code"),
                col("name", "common.name"),
                col("division", "entity.division"),
                col("status", "common.status"));

        // utilities/menu-master.html
        register("MENU_MASTER",
                col("label", null),
                col("parent", null),
                col("type", "common.type"),
                col("hrefPage", null),
                col("target", null),
                col("order", null),
                col("status", "common.status"));
    }

    private DefaultColumnRegistry() { }

    private static ColumnConfig col(String field, String labelKey) {
        return new ColumnConfig(field, labelKey, true);
    }

    /** Use for a column that exists but should start hidden. labelKey may be null: the page's header text is used. */
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
