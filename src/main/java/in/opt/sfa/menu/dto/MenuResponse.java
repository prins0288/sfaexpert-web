package in.opt.sfa.menu.dto;

import java.util.List;

/**
 * The full navigation payload for a user: the role-filtered tree plus the ids
 * of the menu items this user has starred as favorites.
 */
public record MenuResponse(
        List<MenuNode> items,
        List<Long> favorites
) {}
