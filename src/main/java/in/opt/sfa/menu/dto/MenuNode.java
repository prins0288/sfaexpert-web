package in.opt.sfa.menu.dto;

import java.util.List;

/**
 * A node of the navigation tree as sent to the browser. The shell renders this
 * for both the vertical sidebar and the horizontal menu bar, derives breadcrumbs
 * from it, and uses {@code id} as the key for marking a leaf as a favorite.
 */
public record MenuNode(
        Long id,
        String label,
        String title,
        String icon,
        String logo,
        String page,
        String href,
        List<MenuNode> children
) {}
