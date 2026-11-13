package in.opt.sfa.tenant.dcr.geo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.opt.sfa.common.repository.CompanyDetailsRepository;
import in.opt.sfa.exception.ForbiddenException;
import in.opt.sfa.security.Authz;
import in.opt.sfa.tenant.context.TenantContext;
import in.opt.sfa.tenant.dcr.submit.service.DcrRuleService;
import in.opt.sfa.tenant.team.TeamScope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * Geo fencing of the DCR. Rule GEO_FENCING_ALLOW (TRUE = the current location is captured and checked at Start Day and at every check-in) and
 * GEO_FENCING_DISTANCE (metres a client check-in may be from the client's tagged location: client_master latitude / longitude, has_geo_tag).
 * A client whose location is not tagged cannot be checked, so the check-in is allowed (and the user can tag it). A wrong tag is fixed with a retag request,
 * approved by the manager / admin.
 */
@Service
public class GeoService {

    public static final String NO_LOCATION = "We could not get your current location. Turn on the location (GPS) of your phone / computer and allow location access for this site, then open Google Maps once so it gets a fix, and tap Try again.";

    private final JdbcTemplate jdbc;
    private final DcrRuleService rules;
    private final CompanyDetailsRepository companies;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    public GeoService(JdbcTemplate jdbc, DcrRuleService rules, CompanyDetailsRepository companies) { this.jdbc = jdbc; this.rules = rules; this.companies = companies; }

    private static String me() { return TeamScope.me(); }
    private static Double dbl(Object o) { return o == null ? null : ((Number) o).doubleValue(); }

    public boolean on(String empId) { return DcrRuleService.bool(rules.of(empId), "GEO_FENCING_ALLOW", false); }
    public int fenceM(String empId) { return DcrRuleService.integer(rules.of(empId), "GEO_FENCING_DISTANCE", 200); }

    /** What the rules mean for the screen: {on, distance}. */
    public Map<String, Object> config() { String me = me(); return Map.of("on", on(me), "distance", fenceM(me)); }

    // ---- address of a position (Google Geocoding, key of this company; blank when it cannot be found) ----------------------------------------
    public String reverse(Double lat, Double lng) {
        if (!GeoCalc.validLatLng(lat, lng)) return null;
        try {
            String key = companies.findById(TenantContext.getCompanyCode()).map(c -> c.getGoogleApiKey()).filter(k -> k != null && !k.isBlank()).orElse(null);
            if (key == null) return null;
            HttpRequest rq = HttpRequest.newBuilder(URI.create("https://maps.googleapis.com/maps/api/geocode/json?latlng=" + lat + "," + lng + "&key=" + URLEncoder.encode(key, StandardCharsets.UTF_8))).timeout(Duration.ofSeconds(4)).GET().build();
            HttpResponse<String> r = http.send(rq, HttpResponse.BodyHandlers.ofString());
            JsonNode n = json.readTree(r.body()).path("results");
            if (!n.isArray() || n.isEmpty()) return null;
            String a = n.get(0).path("formatted_address").asText(null);
            return a == null ? null : (a.length() > 490 ? a.substring(0, 490) : a);
        } catch (Exception e) { return null; }
    }

    // ---- client check-in check -------------------------------------------------------------------------------------------------------------
    /** Is the user at the client? {ok, code OK / NO_LOCATION / NOT_TAGGED / TOO_FAR, message, distanceM, fenceM, tagged, nearby[], retagPending}. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Map<String, Object> checkListed(String empId, long clientId, Double lat, Double lng) {
        Map<String, Object> out = new LinkedHashMap<>();
        boolean geo = on(empId);
        out.put("geo", geo);
        if (!geo) { out.put("ok", true); out.put("code", "OK"); return out; }
        if (!GeoCalc.validLatLng(lat, lng)) { out.put("ok", false); out.put("code", "NO_LOCATION"); out.put("message", NO_LOCATION); return out; }
        Map<String, Object> c = jdbc.queryForList("SELECT CONCAT(COALESCE(CONCAT(salutation, ' '), ''), client_name) AS name FROM client_master WHERE id = ?", clientId).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Client not found"));
        List<Map<String, Object>> places = places(clientId);   // the primary place and the other places the client sits at
        boolean tagged = !places.isEmpty();
        out.put("tagged", tagged); out.put("clientName", c.get("name")); out.put("fenceM", fenceM(empId));
        if (!tagged) {
            out.put("ok", true); out.put("code", "NOT_TAGGED");
            out.put("message", "The location of " + c.get("name") + " is not tagged yet, so it cannot be checked. Check-in is allowed - please tag the client's location.");
            return out;
        }
        int fence = fenceM(empId);
        Map<String, Object> best = null;
        for (Map<String, Object> pl : places) {
            long dm = GeoCalc.distanceM(lat, lng, dbl(pl.get("latitude")), dbl(pl.get("longitude")));
            pl.put("distanceM", dm); pl.put("prettyDistance", GeoCalc.pretty(dm)); pl.put("within", fence <= 0 || dm <= fence);
            if (best == null || dm < ((Number) best.get("distanceM")).longValue()) best = pl;
        }
        long d = ((Number) best.get("distanceM")).longValue();
        out.put("places", places); out.put("nearestPlace", best.get("label"));
        out.put("distanceM", d); out.put("prettyDistance", GeoCalc.pretty(d)); out.put("clientLat", best.get("latitude")); out.put("clientLng", best.get("longitude")); out.put("clientAddress", best.get("address"));
        if (fence <= 0 || d <= fence) { out.put("ok", true); out.put("code", "OK"); out.put("matchedPlace", best.get("label")); return out; }
        out.put("ok", false); out.put("code", "TOO_FAR");
        out.put("message", "You are far away from " + c.get("name") + ": " + GeoCalc.pretty(d) + " from the nearest tagged place" + (places.size() > 1 ? " (" + best.get("label") + ", one of " + places.size() + " places of this client)" : "")
                + ". A check-in is allowed only within " + GeoCalc.pretty(fence) + " of a tagged place. Go to the client, or if the client sits at a new place, retag it.");
        out.put("nearby", nearby(empId, lat, lng, fence));
        out.put("retagPending", jdbc.queryForObject("SELECT COUNT(*) FROM client_retag_request WHERE client_id = ? AND requested_by = ? AND status = 'PENDING'", Integer.class, clientId, empId) > 0);
        return out;
    }

    // ---- the places of a client: the primary one (client_master) + the others (client_geo_tag) -----------------------------------------------
    /** {id (0 = primary), label, latitude, longitude, address, primary}; the primary first. Empty when the client is not tagged at all. */
    public List<Map<String, Object>> places(long clientId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> c : jdbc.queryForList("SELECT latitude, longitude, gps_address, has_geo_tag FROM client_master WHERE id = ?", clientId)) {
            if (flag(c.get("has_geo_tag")) && c.get("latitude") != null && c.get("longitude") != null) {
                Map<String, Object> m = new LinkedHashMap<>(); m.put("id", 0L); m.put("label", "Primary"); m.put("latitude", c.get("latitude")); m.put("longitude", c.get("longitude")); m.put("address", c.get("gps_address")); m.put("primary", true);
                out.add(m);
            }
        }
        try {
            for (Map<String, Object> t : jdbc.queryForList("SELECT id, label, latitude, longitude, address FROM client_geo_tag WHERE client_id = ? AND is_active = 1 ORDER BY id", clientId)) {
                Map<String, Object> m = new LinkedHashMap<>(); m.put("id", t.get("id")); m.put("label", t.get("label") == null || t.get("label").toString().isBlank() ? "Place " + (out.size() + 1) : t.get("label")); m.put("latitude", t.get("latitude")); m.put("longitude", t.get("longitude")); m.put("address", t.get("address")); m.put("primary", false);
                out.add(m);
            }
        } catch (org.springframework.dao.DataAccessException e) { /* migration 122 not run yet: only the primary place */ }
        return out;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> tags(long clientId) { Authz.requireRole("ADMIN", "MANAGER"); return places(clientId); }

    /** Add a place; a client that has no primary place yet gets it as the primary. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> addTag(long clientId, String label, Double lat, Double lng) {
        Authz.requireRole("ADMIN", "MANAGER");
        if (!GeoCalc.validLatLng(lat, lng)) throw new IllegalStateException("Give a valid latitude and longitude");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM client_master WHERE id = ?", Integer.class, clientId) == 0) throw new IllegalStateException("Client not found");
        String addr = reverse(lat, lng);
        if (places(clientId).isEmpty()) {
            jdbc.update("UPDATE client_master SET latitude = ?, longitude = ?, gps_address = ?, has_geo_tag = 1, tagged_at = NOW(), tagged_by = ? WHERE id = ?", BigDecimal.valueOf(lat), BigDecimal.valueOf(lng), addr, me(), clientId);
        } else {
            jdbc.update("INSERT INTO client_geo_tag (client_id, label, latitude, longitude, address, created_by) VALUES (?,?,?,?,?,?)", clientId, label == null || label.isBlank() ? null : label.trim(), BigDecimal.valueOf(lat), BigDecimal.valueOf(lng), addr, me());
        }
        return Map.of("ok", true);
    }

    /** Make another place the primary: it moves into client_master and the old primary becomes one of the other places. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> makePrimary(long clientId, long tagId) {
        Authz.requireRole("ADMIN", "MANAGER");
        Map<String, Object> t = jdbc.queryForList("SELECT * FROM client_geo_tag WHERE id = ? AND client_id = ? AND is_active = 1", tagId, clientId).stream().findFirst().orElseThrow(() -> new IllegalStateException("Place not found"));
        Map<String, Object> c = jdbc.queryForList("SELECT latitude, longitude, gps_address, has_geo_tag FROM client_master WHERE id = ?", clientId).stream().findFirst().orElseThrow(() -> new IllegalStateException("Client not found"));
        if (flag(c.get("has_geo_tag")) && c.get("latitude") != null)
            jdbc.update("INSERT INTO client_geo_tag (client_id, label, latitude, longitude, address, created_by) VALUES (?,?,?,?,?,?)", clientId, "Earlier primary", c.get("latitude"), c.get("longitude"), c.get("gps_address"), me());
        jdbc.update("UPDATE client_master SET latitude = ?, longitude = ?, gps_address = ?, has_geo_tag = 1, tagged_at = NOW(), tagged_by = ? WHERE id = ?", t.get("latitude"), t.get("longitude"), t.get("address"), me(), clientId);
        jdbc.update("DELETE FROM client_geo_tag WHERE id = ?", tagId);
        return Map.of("ok", true);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> removeTag(long clientId, long tagId) {
        Authz.requireRole("ADMIN", "MANAGER");
        if (tagId == 0) throw new IllegalStateException("The primary place cannot be removed - make another place the primary first");
        return Map.of("ok", true, "removed", jdbc.update("DELETE FROM client_geo_tag WHERE id = ? AND client_id = ?", tagId, clientId));
    }

    /** The user's own tagged clients nearest to a position: which client is how far (the user may be standing at another client). */
    private List<Map<String, Object>> nearby(String empId, double lat, double lng, int fence) {
        List<GeoCalc.Point> pts = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                SELECT c.id, CONCAT(COALESCE(CONCAT(c.salutation, ' '), ''), c.client_name) AS name, c.latitude, c.longitude FROM client_master c
                WHERE c.is_active = 1 AND c.approval_status = 'APPROVED' AND c.has_geo_tag = 1 AND c.latitude IS NOT NULL AND c.longitude IS NOT NULL
                  AND (c.mapped_emp_code = ? OR (c.mapped_emp_code IS NULL AND c.created_by COLLATE utf8mb4_0900_ai_ci = ?))""", empId, empId))
            pts.add(new GeoCalc.Point(((Number) r.get("id")).longValue(), (String) r.get("name"), dbl(r.get("latitude")), dbl(r.get("longitude"))));
        List<Map<String, Object>> out = new ArrayList<>();
        for (GeoCalc.Near n : GeoCalc.nearest(lat, lng, pts, 5)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", n.id()); m.put("name", n.name()); m.put("distanceM", n.distanceM()); m.put("prettyDistance", GeoCalc.pretty(n.distanceM())); m.put("within", fence <= 0 || n.distanceM() <= fence);
            out.add(m);
        }
        return out;
    }

    /** The distance of a position from the nearest tagged place of a client (null when the client is not tagged). */
    public Long distanceToClient(long clientId, double lat, double lng) {
        Long best = null;
        for (Map<String, Object> pl : places(clientId)) { long dm = GeoCalc.distanceM(lat, lng, dbl(pl.get("latitude")), dbl(pl.get("longitude"))); if (best == null || dm < best) best = dm; }
        return best;
    }

    private static boolean flag(Object o) { return o instanceof Boolean b ? b : o instanceof Number n && n.intValue() == 1; }

    // ---- retag ------------------------------------------------------------------------------------------------------------------------------
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> requestRetag(long clientId, Double lat, Double lng, String reason) {
        String me = me();
        if (!GeoCalc.validLatLng(lat, lng)) throw new IllegalStateException("Choose the correct location of the client on the map first");
        Map<String, Object> c = jdbc.queryForList("SELECT client_name, latitude, longitude, gps_address FROM client_master WHERE id = ? AND is_active = 1", clientId).stream().findFirst().orElseThrow(() -> new IllegalStateException("Client not found"));
        if (jdbc.queryForObject("SELECT COUNT(*) FROM client_retag_request WHERE client_id = ? AND requested_by = ? AND status = 'PENDING'", Integer.class, clientId, me) > 0)
            throw new IllegalStateException("You have already asked to retag " + c.get("client_name") + ". It is waiting for your manager / admin to approve");
        String manager = jdbc.queryForList("SELECT manager_id FROM emp_detail WHERE emp_id = ?", String.class, me).stream().findFirst().orElse(null);
        Long moved = c.get("latitude") == null || c.get("longitude") == null ? null : GeoCalc.distanceM(lat, lng, dbl(c.get("latitude")), dbl(c.get("longitude")));
        jdbc.update("INSERT INTO client_retag_request (client_id, requested_by, manager_id, new_lat, new_lng, new_address, old_lat, old_lng, old_address, moved_m, reason) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                clientId, me, manager, BigDecimal.valueOf(lat), BigDecimal.valueOf(lng), reverse(lat, lng), c.get("latitude"), c.get("longitude"), c.get("gps_address"), moved, reason == null || reason.isBlank() ? null : reason.trim());
        return Map.of("ok", true, "message", "Retag request sent to your manager / admin for approval. Once approved, you can check in when you are at that location");
    }

    private static final String RETAG_SEL = "SELECT r.id, r.client_id AS clientId, CONCAT(COALESCE(CONCAT(c.salutation, ' '), ''), c.client_name) AS clientName, c.client_code AS clientCode, e.emp_name AS requestedByName, e.emp_code AS requestedByCode, r.requested_at AS requestedAt, "
            + "r.new_lat AS newLat, r.new_lng AS newLng, r.new_address AS newAddress, r.old_lat AS oldLat, r.old_lng AS oldLng, r.old_address AS oldAddress, r.moved_m AS movedM, r.reason, r.status, d.emp_name AS decidedByName, r.decided_at AS decidedAt, r.decision_remarks AS remarks, r.requested_by AS requestedBy, r.manager_id AS managerId "
            + "FROM client_retag_request r JOIN client_master c ON c.id = r.client_id JOIN emp_detail e ON e.emp_id = r.requested_by LEFT JOIN emp_detail d ON d.emp_id = r.decided_by ";

    private boolean canDecide(String requestedBy, String managerId) {
        if (requestedBy.equals(me())) return false;
        return TeamScope.isAdmin() || me().equals(managerId);
    }

    /** Requests waiting for me (manager: my team's; admin: everyone's). */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> toApprove(boolean all) {
        List<Map<String, Object>> rows = jdbc.queryForList(RETAG_SEL + (all ? "" : "WHERE r.status = 'PENDING' ") + "ORDER BY r.id DESC LIMIT 300");
        rows.removeIf(r -> !canDecide((String) r.get("requestedBy"), (String) r.get("managerId")));
        for (Map<String, Object> r : rows) { r.remove("requestedBy"); r.remove("managerId"); }
        return rows;
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void decide(long id, boolean approve, String remarks) {
        Map<String, Object> r = jdbc.queryForList("SELECT * FROM client_retag_request WHERE id = ?", id).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Request not found"));
        if (!canDecide((String) r.get("requested_by"), (String) r.get("manager_id"))) throw new ForbiddenException("You cannot decide this request");
        if (!"PENDING".equals(r.get("status"))) throw new IllegalStateException("Already " + r.get("status").toString().toLowerCase());
        if (!approve && (remarks == null || remarks.isBlank())) throw new IllegalArgumentException("Please give a reason for rejecting");
        jdbc.update("UPDATE client_retag_request SET status = ?, decided_by = ?, decided_at = NOW(), decision_remarks = ? WHERE id = ?", approve ? "APPROVED" : "REJECTED", me(), remarks, id);
        if (approve) {   // the new place is added; a client that was not tagged gets it as the primary, otherwise the primary stays and the new one is another place of the client
            long cid = ((Number) r.get("client_id")).longValue();
            if (places(cid).isEmpty()) jdbc.update("UPDATE client_master SET latitude = ?, longitude = ?, gps_address = ?, has_geo_tag = 1, tagged_at = NOW(), tagged_by = ? WHERE id = ?", r.get("new_lat"), r.get("new_lng"), r.get("new_address"), r.get("requested_by"), cid);
            else jdbc.update("INSERT INTO client_geo_tag (client_id, label, latitude, longitude, address, created_by) VALUES (?,?,?,?,?,?)", cid, "Retagged", r.get("new_lat"), r.get("new_lng"), r.get("new_address"), r.get("requested_by"));
        }
    }

    /** The key for the map picker (Google Maps JavaScript), same company key as the area auto-complete. */
    public Map<String, Object> mapConfig() {
        String key = companies.findById(TenantContext.getCompanyCode()).map(c -> c.getGoogleApiKey()).filter(k -> k != null && !k.isBlank()).orElse(null);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("enabled", key != null); m.put("apiKey", key);
        return m;
    }
}
