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

        // report/dcr-report.html - the two grids of the redesigned report (summary per employee, and date wise)
        register("DCR_REPORT_EMP",
                col("code", null), col("employee", "entity.employee"), col("designation", "entity.designation"), col("hq", null), col("state", null),
                col("present", null), col("absent", null), col("leave", null), col("off", null), col("calls", null), col("pob", null), col("expense", null),
                col("submitted", null), col("draft", null), col("totalHrs", null), col("avgHrs", null), col("avgStart", null), col("avgEnd", null), hidden("avgFirst", null), hidden("avgLast", null), col("last", null));
        register("DCR_REPORT_DAY",
                col("date", "common.date"), col("day", null), col("code", null), col("employee", "entity.employee"), col("designation", "entity.designation"), col("hq", null),
                col("status", "common.status"), col("workType", "report.dcr.workType"), col("dayStart", null), col("dayEnd", null), col("firstCall", null), col("lastCall", null), col("workHrs", null), col("callHrs", null), col("startArea", null), col("areaType", null),
                col("calls", null), col("pob", null), col("expense", null), col("dcrStatus", null), col("source", null));

        register("DCR_DAY_CALLS",
                col("client", "entity.client"), col("kind", null), col("code", null), col("type", "entity.clientType"), col("category", "entity.category"), col("speciality", "entity.speciality"), col("area", "entity.area"),
                col("in", null), col("out", null), col("duration", null), col("visit", null), col("activity", null), col("pob", null), col("joint", null), col("location", null), col("distance", null));

        // report/attendance-report.html (the sheet itself has one column per date, so only these two grids are configurable)
        register("ATTENDANCE_SUMMARY",
                col("code", "common.code"), col("employee", "entity.employee"), col("designation", "entity.designation"), col("hq", "entity.hq"), col("state", "entity.state"), col("doj", null), col("status", "common.status"),
                col("present", null), col("half", null), col("leave", null), col("off", null), col("absent", null), col("calls", null), col("hours", null), col("pct", null));
        register("ATTENDANCE_NOTSUB",
                col("employee", "entity.employee"), col("code", "common.code"), col("designation", "entity.designation"), col("hq", "entity.hq"), col("mobile", "common.mobile"));

        // report/calendar-report.html
        register("CALENDAR_SUMMARY",
                col("group", null), col("employees", "entity.employees"), col("present", null), col("half", null), col("leave", null), col("off", null), col("absent", null), col("calls", null), col("hours", null), col("pct", null));
        register("SENIOR_CLIENT",
                col("manager", null), col("client", "entity.client"), col("owner", null), col("geo", null), col("givenBy", null));
        register("DCR_PERMISSION",
                col("emp", "entity.employee"), col("designation", "entity.designation"), col("hq", "entity.hq"), col("type", null), col("dcrDates", null), col("window", null), col("status", null), col("reason", null), col("givenBy", null));
        register("CALENDAR_DAY",
                col("group", null), col("employees", "entity.employees"), col("present", null), col("half", null), col("leave", null), col("off", null), col("absent", null), col("calls", null));

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
                col("employee", null),
                col("city", "common.city"),
                col("state", "entity.state"),
                col("hq", "entity.hq"),
                col("pincode", "common.pincode"),
                col("type", "common.type"),
                col("routes", "entity.routes"),
                col("approval", null),
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
                col("photo", null),
                col("empCode", null),
                col("name", "common.name"),
                col("mobile", "common.mobile"),
                col("designation", "entity.designation"),
                col("division", "entity.division"),
                col("username", null),
                col("active", "common.active"),
                col("vacant", null),
                hidden("gender", null),
                hidden("officialEmail", null),
                hidden("department", null),
                hidden("dateOfJoining", null),
                hidden("manager", null),
                hidden("profileComplete", null));

        // master/brand-master.html
        register("BRAND_MASTER",
                col("image", null),
                col("code", "common.code"),
                col("name", "common.name"),
                col("status", "common.status"),
                col("created", "common.created"),
                col("updated", "common.updated"));

        // master/salutation-master.html
        register("SALUTATION_MASTER",
                col("salutation", null),
                col("order", null),
                col("status", "common.status"));

        // master/client-master.html (the old master/client.html keeps CLIENT_MASTER)
        register("CLIENT_MASTER_V2",
                col("photo", null),
                col("code", "common.code"),
                col("name", "common.name"),
                col("type", null),
                col("speciality", null),
                hidden("category", null),
                col("mobile", "common.mobile"),
                col("employee", "entity.employee"),
                col("state", null),
                hidden("district", null),
                col("city", null),
                hidden("locality", null),
                hidden("visitDays", null),
                hidden("prescriber", null),
                hidden("geo", null),
                col("approval", null),
                col("status", "common.status"),
                hidden("created", "common.created"),
                hidden("updated", "common.updated"));

        // master/password-reset.html
        register("PASSWORD_RESET",
                col("empCode", null),
                col("name", "common.name"),
                col("designation", "entity.designation"),
                hidden("division", "entity.division"),
                col("username", null),
                col("login", null),
                col("vacant", null),
                col("lastLogin", null));

        // master/item-master.html
        register("ITEM_MASTER",
                col("image", null),
                col("code", "common.code"),
                col("name", "common.name"),
                col("type", null),
                col("brand", null),
                hidden("division", null),
                col("packSize", null),
                col("composition", null),
                hidden("hsn", null),
                hidden("gst", null),
                col("pts", null),
                col("ptr", null),
                col("mrp", null),
                hidden("rateCount", null),
                hidden("manufacturer", null),
                col("status", "common.status"),
                hidden("created", "common.created"),
                hidden("updated", "common.updated"));

        // master/designation-master.html
        register("DESIGNATION_MASTER",
                col("name", "common.name"),
                col("hierarchyLevel", null),
                col("displayOrder", null),
                col("grade", null),
                col("role", null),
                col("employees", null),
                col("status", "common.status"));

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
