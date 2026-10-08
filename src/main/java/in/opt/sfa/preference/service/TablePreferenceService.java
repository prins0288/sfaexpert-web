package in.opt.sfa.preference.service;

import in.opt.sfa.common.entity.UserTablePreference;
import in.opt.sfa.common.repository.UserTablePreferenceRepository;
import in.opt.sfa.exception.ForbiddenException;
import in.opt.sfa.preference.dto.ColumnConfig;
import in.opt.sfa.preference.dto.TablePreferenceDto;
import in.opt.sfa.preference.registry.DefaultColumnRegistry;
import in.opt.sfa.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Per-user column layouts for registered grids. The owner is always
 * (companyCode, empId) from the verified JWT (UserContext) — never from the
 * request — so every read and write is company-isolated.
 */
@Service
public class TablePreferenceService {

    private final UserTablePreferenceRepository repository;

    public TablePreferenceService(UserTablePreferenceRepository repository) {
        this.repository = repository;
    }

    /** Saved layout merged with the current defaults, or the defaults if nothing is saved. */
    @Transactional(transactionManager = "commonTransactionManager", readOnly = true)
    public TablePreferenceDto get(String screenKey) {
        requireRegistered(screenKey);
        Owner o = owner();
        return repository.findByCompanyCodeAndUserIdAndScreenKey(o.companyCode, o.userId, screenKey)
                .map(row -> new TablePreferenceDto(screenKey, true, merge(screenKey, row.getColumnConfig())))
                .orElseGet(() -> new TablePreferenceDto(screenKey, false, DefaultColumnRegistry.defaults(screenKey)));
    }

    /** Upsert. Unknown / duplicate fields are dropped, missing ones re-added from the defaults. */
    @Transactional(transactionManager = "commonTransactionManager")
    public TablePreferenceDto save(String screenKey, List<ColumnConfig> columns) {
        requireRegistered(screenKey);
        List<ColumnConfig> merged = merge(screenKey, columns);
        if (merged.stream().noneMatch(c -> Boolean.TRUE.equals(c.getVisible()))) {
            throw new IllegalArgumentException("At least one column must stay visible");
        }
        Owner o = owner();
        UserTablePreference row = repository.findByCompanyCodeAndUserIdAndScreenKey(o.companyCode, o.userId, screenKey)
                .orElseGet(() -> {
                    UserTablePreference r = new UserTablePreference();
                    r.setCompanyCode(o.companyCode);
                    r.setUserId(o.userId);
                    r.setScreenKey(screenKey);
                    return r;
                });
        row.setColumnConfig(merged);
        row.setUpdatedAt(LocalDateTime.now());
        repository.save(row);
        return new TablePreferenceDto(screenKey, true, merged);
    }

    /** Reset: drop the saved row, return the defaults. */
    @Transactional(transactionManager = "commonTransactionManager")
    public TablePreferenceDto reset(String screenKey) {
        requireRegistered(screenKey);
        Owner o = owner();
        repository.deleteByCompanyCodeAndUserIdAndScreenKey(o.companyCode, o.userId, screenKey);
        return new TablePreferenceDto(screenKey, false, DefaultColumnRegistry.defaults(screenKey));
    }

    /**
     * Saved layout reconciled against today's defaults:
     *  - saved order and visibility/width/extraSettings win for known fields,
     *  - labelKey always comes from the registry (renamed labels follow),
     *  - stale fields (removed/renamed since) and duplicates are ignored,
     *  - fields added since the user saved are inserted right after their
     *    default predecessor, with their default visibility.
     */
    static List<ColumnConfig> merge(String screenKey, List<ColumnConfig> saved) {
        List<ColumnConfig> defaults = DefaultColumnRegistry.defaults(screenKey);
        Map<String, ColumnConfig> byField = new LinkedHashMap<>();
        defaults.forEach(d -> byField.put(d.getField(), d));

        List<ColumnConfig> savedSorted = new ArrayList<>(saved == null ? List.of() : saved);
        savedSorted.removeIf(Objects::isNull);
        // stable sort; a missing order keeps the entry's position relative to the others
        savedSorted.sort(Comparator.comparing(c -> c.getOrder() == null ? Integer.MAX_VALUE : c.getOrder()));

        List<ColumnConfig> result = new ArrayList<>();
        Set<String> placed = new HashSet<>();
        for (ColumnConfig s : savedSorted) {
            ColumnConfig d = byField.get(s.getField());
            if (d == null || !placed.add(s.getField())) continue;   // stale or duplicate
            ColumnConfig c = d.copy();
            if (s.getVisible() != null) c.setVisible(s.getVisible());
            if (s.getWidth() != null && !s.getWidth().isBlank()) c.setWidth(s.getWidth().trim());
            if (s.getExtraSettings() != null) {
                Map<String, Object> extra = c.getExtraSettings() == null ? new LinkedHashMap<>() : c.getExtraSettings();
                extra.putAll(s.getExtraSettings());
                c.setExtraSettings(extra);
            }
            result.add(c);
        }

        for (int i = 0; i < defaults.size(); i++) {
            ColumnConfig d = defaults.get(i);
            if (placed.contains(d.getField())) continue;
            int pos = 0;
            for (int j = i - 1; j >= 0; j--) {
                int at = indexOfField(result, defaults.get(j).getField());
                if (at >= 0) { pos = at + 1; break; }
            }
            result.add(pos, d.copy());
            placed.add(d.getField());
        }

        for (int i = 0; i < result.size(); i++) result.get(i).setOrder(i);
        return result;
    }

    private static int indexOfField(List<ColumnConfig> list, String field) {
        for (int i = 0; i < list.size(); i++) if (list.get(i).getField().equals(field)) return i;
        return -1;
    }

    private static void requireRegistered(String screenKey) {
        if (!DefaultColumnRegistry.isRegistered(screenKey)) {
            throw new IllegalArgumentException("Unknown screen: " + screenKey);
        }
    }

    private record Owner(String companyCode, String userId) { }

    private static Owner owner() {
        UserContext.CurrentUser u = UserContext.get();
        if (u == null || u.companyCode() == null || u.empId() == null) {
            throw new ForbiddenException("No verified user on this request");
        }
        return new Owner(u.companyCode(), u.empId());
    }
}
