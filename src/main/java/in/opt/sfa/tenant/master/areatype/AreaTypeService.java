package in.opt.sfa.tenant.master.areatype;

import in.opt.sfa.security.Authz;
import in.opt.sfa.tenant.team.TeamScope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** Area types (HQ / EX / OUT EX / OUT ...) with the km range of each: used in Area Master and for the DA / fare of the expense. */
@Service
public class AreaTypeService {
    public record Type(long id, String code, String name, int from, Integer to, int sort, boolean stay) { }

    private final JdbcTemplate jdbc;
    public AreaTypeService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** Active types, lowest first. Empty when migration 128 has not been run. */
    public List<Type> types() {
        try {
            List<Type> l = new ArrayList<>();
            for (Map<String, Object> r : jdbc.queryForList("SELECT id, type_code, type_name, km_from, km_to, sort_order, is_stay_type FROM area_type_master WHERE is_active = 1 ORDER BY sort_order, id"))
                l.add(new Type(((Number) r.get("id")).longValue(), (String) r.get("type_code"), (String) r.get("type_name"), ((Number) r.get("km_from")).intValue(), r.get("km_to") == null ? null : ((Number) r.get("km_to")).intValue(), ((Number) r.get("sort_order")).intValue(), Boolean.TRUE.equals(r.get("is_stay_type")) || (r.get("is_stay_type") instanceof Number n && n.intValue() == 1)));
            return l;
        } catch (org.springframework.dao.DataAccessException e) { return List.of(); }
    }

    /** The type of a distance from the HQ (km), or null when it is not in any range. */
    public String codeForKm(Double km) {
        if (km == null) return null;
        for (Type t : types()) if (km >= t.from() && (t.to() == null || km <= t.to())) return t.code();
        return null;
    }

    /** The master code of a stored area_type text (the code or the name, any case), else null. */
    public String codeOf(String text) {
        if (text == null || text.isBlank()) return null;
        String t = text.trim();
        for (Type x : types()) if (x.code().equalsIgnoreCase(t) || x.name().equalsIgnoreCase(t) || x.name().replace(" ", "").equalsIgnoreCase(t.replace(" ", ""))) return x.code();
        return null;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> list() { return jdbc.queryForList("SELECT id, type_code AS code, type_name AS name, km_from AS kmFrom, km_to AS kmTo, sort_order AS sortOrder, is_stay_type AS stay, is_active AS active FROM area_type_master ORDER BY sort_order, id"); }

    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> save(Map<String, Object> b) {
        Authz.requireRole("ADMIN");
        Long id = b.get("id") == null || b.get("id").toString().isBlank() ? null : Long.valueOf(b.get("id").toString());
        String code = String.valueOf(b.get("code")).trim().toUpperCase(), name = b.get("name") == null ? "" : b.get("name").toString().trim();
        if (!code.matches("[A-Z][A-Z0-9_]{1,9}")) throw new IllegalArgumentException("Code: 2-10 capital letters / digits (e.g. HQ, EX, OUTEX)");
        if (name.isEmpty()) throw new IllegalArgumentException("Name is required");
        int from = Integer.parseInt(String.valueOf(b.getOrDefault("kmFrom", 0)));
        Integer to = b.get("kmTo") == null || b.get("kmTo").toString().isBlank() ? null : Integer.valueOf(b.get("kmTo").toString());
        if (from < 0 || (to != null && to < from)) throw new IllegalArgumentException("The km range is wrong: 'to' must be at least 'from'");
        boolean active = !Boolean.FALSE.equals(b.get("active")) && !"0".equals(String.valueOf(b.get("active"))), stay = Boolean.TRUE.equals(b.get("stay")) || "1".equals(String.valueOf(b.get("stay")));
        int sort = b.get("sortOrder") == null ? 100 : Integer.parseInt(b.get("sortOrder").toString());
        if (active) for (Map<String, Object> o : jdbc.queryForList("SELECT id, type_code, km_from, km_to FROM area_type_master WHERE is_active = 1" + (id == null ? "" : " AND id <> " + id))) {   // the ranges do not overlap
            int of = ((Number) o.get("km_from")).intValue(); Integer ot = o.get("km_to") == null ? null : ((Number) o.get("km_to")).intValue();
            boolean overlap = from <= (ot == null ? Integer.MAX_VALUE : ot) && of <= (to == null ? Integer.MAX_VALUE : to);
            if (overlap) throw new IllegalArgumentException("The km range overlaps " + o.get("type_code") + " (" + of + " - " + (ot == null ? "unlimited" : ot) + ")");
        }
        if (stay) jdbc.update("UPDATE area_type_master SET is_stay_type = 0");   // one stay type
        if (id == null) jdbc.update("INSERT INTO area_type_master (type_code, type_name, km_from, km_to, sort_order, is_stay_type, is_active, created_by) VALUES (?,?,?,?,?,?,?,?)", code, name, from, to, sort, stay ? 1 : 0, active ? 1 : 0, TeamScope.me());
        else jdbc.update("UPDATE area_type_master SET type_code = ?, type_name = ?, km_from = ?, km_to = ?, sort_order = ?, is_stay_type = ?, is_active = ? WHERE id = ?", code, name, from, to, sort, stay ? 1 : 0, active ? 1 : 0, id);
        return Map.of("saved", true);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void delete(long id) {
        Authz.requireRole("ADMIN");
        String code = jdbc.queryForList("SELECT type_code FROM area_type_master WHERE id = ?", String.class, id).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Type not found"));
        if (jdbc.queryForObject("SELECT COUNT(*) FROM area_master WHERE area_type = ?", Integer.class, code) > 0) throw new IllegalStateException("Areas use this type - switch it off instead of deleting it");
        jdbc.update("DELETE FROM area_type_master WHERE id = ?", id);
    }
}
