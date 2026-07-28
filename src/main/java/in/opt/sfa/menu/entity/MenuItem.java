package in.opt.sfa.menu.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * A single navigation entry (TENANT db — each tenant owns its menu). The whole menu — every level of it —
 * lives in this one table instead of being hard-coded in JavaScript, so the
 * sidebar / menu bar can be changed, reordered or role-gated from data alone.
 *
 * Hierarchy is self-referential: parent_id = null for a top-level (L1) item,
 * otherwise it points at the parent's id. Three levels are used today
 * (L1 > L2 > L3) but the model allows any depth.
 *
 * `roles` is a comma-separated allow-list (e.g. "ADMIN,MANAGER"); null/blank
 * means "visible to every authenticated role". The server filters the tree by
 * the caller's JWT role before returning it, and prunes branches that end up
 * with no visible children.
 */
@Entity
@Table(name = "menu_item")
@Getter
@Setter
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Parent menu id; null for a top-level item. */
    @Column(name = "parent_id")
    private Long parentId;

    /** Nav text (short), shown in the sidebar / menu bar. */
    @Column(nullable = false, length = 128)
    private String label;

    /** Report/page title shown in the header + on the breadcrumb bar. Falls back
     *  to {@link #label} when null, so only pages that need a longer title set it. */
    @Column(length = 160)
    private String title;

    /** Bootstrap-icon name (without the "bi-" prefix), e.g. "speedometer2". */
    @Column(length = 64)
    private String icon;

    /** Optional image/logo URL shown instead of (or beside) the icon. */
    @Column(length = 256)
    private String logo;

    /** Active-highlight key; matches the page's data-page attribute. */
    @Column(length = 64)
    private String page;

    /** Link target (relative in-app path). Null/blank => this row is a branch. */
    @Column(length = 256)
    private String href;

    /** Ordering among siblings (ascending). */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    /** Comma-separated allowed roles; null/blank = all roles. */
    @Column(length = 256)
    private String roles;

    @Column(nullable = false)
    private boolean enabled = true;
}
