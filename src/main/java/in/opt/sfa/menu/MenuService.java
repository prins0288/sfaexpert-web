package in.opt.sfa.menu;

import in.opt.sfa.menu.dto.MenuNode;
import in.opt.sfa.menu.entity.MenuItem;
import in.opt.sfa.menu.entity.UserMenuFavorite;
import in.opt.sfa.menu.repository.MenuItemRepository;
import in.opt.sfa.menu.repository.UserMenuFavoriteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the navigation tree from the {@code menu_item} table and filters it to
 * the caller's role.
 *
 * Visibility rule (applied to every node, branch or leaf):
 *   - a node with an explicit `roles` list is shown only if the caller's role
 *     is in that list; a null/blank list means "all roles";
 *   - a branch (no href) with no surviving children is pruned, so the menu
 *     never shows an empty parent.
 */
@Service
public class MenuService {

    private final MenuItemRepository repo;
    private final UserMenuFavoriteRepository favorites;

    public MenuService(MenuItemRepository repo, UserMenuFavoriteRepository favorites) {
        this.repo = repo;
        this.favorites = favorites;
    }

    /** The ids this user has starred. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Long> favoritesFor(String username) {
        return favorites.findByUsername(username).stream()
                .map(UserMenuFavorite::getMenuItemId).toList();
    }

    /** Star a menu item (idempotent); returns the updated favorites list. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public List<Long> addFavorite(String username, Long menuItemId) {
        if (favorites.findByUsernameAndMenuItemId(username, menuItemId).isEmpty()) {
            UserMenuFavorite f = new UserMenuFavorite();
            f.setUsername(username);
            f.setMenuItemId(menuItemId);
            favorites.save(f);
        }
        return favoritesFor(username);
    }

    /** Un-star a menu item (idempotent); returns the updated favorites list. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public List<Long> removeFavorite(String username, Long menuItemId) {
        favorites.deleteByUsernameAndMenuItemId(username, menuItemId);
        return favoritesFor(username);
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<MenuNode> menuFor(String role) {
        List<MenuItem> all = repo.findByEnabledTrueOrderBySortOrderAscIdAsc();

        // group children by parent id (null parent => top level)
        Map<Long, List<MenuItem>> byParent = new LinkedHashMap<>();
        for (MenuItem m : all) {
            byParent.computeIfAbsent(m.getParentId(), k -> new ArrayList<>()).add(m);
        }

        List<MenuNode> roots = new ArrayList<>();
        for (MenuItem top : byParent.getOrDefault(null, List.of())) {
            MenuNode node = build(top, byParent, role);
            if (node != null) roots.add(node);
        }
        return roots;
    }

    private MenuNode build(MenuItem item, Map<Long, List<MenuItem>> byParent, String role) {
        // explicit role restriction hides the node (and its whole subtree)
        if (!roleAllows(item.getRoles(), role)) return null;

        List<MenuNode> children = new ArrayList<>();
        for (MenuItem child : byParent.getOrDefault(item.getId(), List.of())) {
            MenuNode c = build(child, byParent, role);
            if (c != null) children.add(c);
        }

        boolean isBranch = byParent.containsKey(item.getId());
        boolean hasHref = item.getHref() != null && !item.getHref().isBlank();
        // prune an empty branch that has no link of its own
        if (isBranch && children.isEmpty() && !hasHref) return null;

        return new MenuNode(
                item.getId(), item.getLabel(), item.getTitle(), item.getIcon(), item.getLogo(),
                item.getPage(), item.getHref(),
                children.isEmpty() ? List.of() : children);
    }

    /** True if the CSV role list is blank (all) or contains the role (case-insensitive). */
    private boolean roleAllows(String csv, String role) {
        if (csv == null || csv.isBlank()) return true;
        if (role == null) return false;
        for (String r : csv.split(",")) {
            if (r.trim().equalsIgnoreCase(role)) return true;
        }
        return false;
    }
}
