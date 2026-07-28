package in.opt.sfa.menu;

import in.opt.sfa.menu.dto.MenuResponse;
import in.opt.sfa.security.UserContext;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Navigation API. Everything is scoped to the current user via the verified JWT
 * (UserContext) — role for filtering, username for favorites.
 *
 *   GET    /api/menu               -> { items:[tree], favorites:[ids] }
 *   POST   /api/menu/favorites/{id} -> star an item,   returns updated ids
 *   DELETE /api/menu/favorites/{id} -> un-star an item, returns updated ids
 */
@Tag(name = "Navigation Menu", description = "Role-filtered 3-level menu tree and per-user favorites")
@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping
    public MenuResponse menu() {
        UserContext.CurrentUser u = current();
        String role = u == null ? null : u.role();
        String username = u == null ? null : u.username();
        return new MenuResponse(menuService.menuFor(role), menuService.favoritesFor(username));
    }

    @PostMapping("/favorites/{id}")
    public List<Long> addFavorite(@PathVariable Long id) {
        return menuService.addFavorite(username(), id);
    }

    @DeleteMapping("/favorites/{id}")
    public List<Long> removeFavorite(@PathVariable Long id) {
        return menuService.removeFavorite(username(), id);
    }

    private UserContext.CurrentUser current() {
        return UserContext.get();
    }

    private String username() {
        UserContext.CurrentUser u = current();
        if (u == null || u.username() == null) {
            throw new IllegalStateException("No authenticated user in context");
        }
        return u.username();
    }
}
