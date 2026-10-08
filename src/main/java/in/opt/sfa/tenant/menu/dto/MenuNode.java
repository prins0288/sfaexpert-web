package in.opt.sfa.tenant.menu.dto;

import java.util.List;

import in.opt.sfa.common.enums.LinkTarget;

/**
 * A node of the navigation tree as sent to the browser. The shell renders this
 * for both the vertical sidebar and the horizontal menu bar, derives breadcrumbs
 * from it, and uses {@code id} as the key for marking a leaf as a favorite.
 */
public record MenuNode(
        Long id,
        String label,
        String title,
        String description,
        String icon,
        String logo,
        String page,
        String href,
       // String target,      // "_SELF" | "_BLANK" — _BLANK opens the link in a new tab
        LinkTarget linkTarget, // enum version of target
        String baseUrl,     // optional absolute base for external links
        List<MenuNode> children
) {}
