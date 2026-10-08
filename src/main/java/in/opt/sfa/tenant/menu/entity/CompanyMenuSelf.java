package in.opt.sfa.tenant.menu.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import in.opt.sfa.common.enums.LinkTarget;
import in.opt.sfa.common.enums.MenuTypeEnum;

/**
 * A single navigation entry (TENANT db — each tenant owns its menu). The whole
 * menu lives in this one table instead of being hard-coded in JavaScript, so
 * the sidebar / menu bar can be changed, reordered or hidden from data alone.
 *
 * Hierarchy is self-referential: parent_id = null for a top-level (L1) item.
 *
 * VISIBILITY model:
 *   - The BASE menu is the set of rows with emp_id = NULL. {@link #isVisible} on
 *     a base row is the GLOBAL default (the "All employees" switch) and
 *     {@link #isActive} is the soft-delete flag.
 *   - PER-EMPLOYEE (and self) hide/show is stored as sparse overrides in the
 *     menu_visibility table, NOT by duplicating rows here — see MenuVisibility.
 *
 * Role-based gating was removed (the roles column is gone); menu visibility is
 * now driven by the global flag + per-employee overrides. API endpoints remain
 * role-gated server-side, so hiding/showing a menu item is a navigation-UX
 * concern only, never an authorization one.
 */
@Entity
@Table(name = "company_menu_self")
@Getter
@Setter
public class CompanyMenuSelf {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Parent menu id; null for a top-level item. */
    @Column(name = "parent_id")
    private Long parentId;

    /** Reserved for per-employee custom rows; the shared BASE menu uses NULL. */
    @Column(name = "emp_id", length = 100)
    private String empId;

    /** Optional external/base URL (e.g. for APP menu types). */
    @Column(name = "base_url", length = 255)
    private String baseUrl;

    /** Platform this entry targets: WEB | APP | BOTH. */
    @Enumerated(EnumType.STRING)
    @Column(name = "menu_type", length = 10)
    private MenuTypeEnum menuType = MenuTypeEnum.WEB;

    /** Nav text (short), shown in the sidebar / menu bar. */
    @Column(nullable = false, length = 128)
    private String label;

    /** Report/page title shown in the header + breadcrumb; falls back to label. */
    @Column(length = 160)
    private String title;

    /** Bootstrap-icon name (without the "bi-" prefix), e.g. "speedometer2". */
    @Column(length = 64)
    private String icon;

    /** Active-highlight key; matches the page's data-page attribute. */
    @Column(length = 64)
    private String page;

    /** Link target (relative in-app path). Null/blank => this row is a branch. */
    @Column(length = 256)
    private String href;

    /** Anchor target: _SELF | _BLANK. */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private LinkTarget target = LinkTarget._SELF;

    /** Optional badge text (e.g. "New"). */
    @Column(length = 30)
    private String badge;

    @Column(name = "badge_color", length = 100)
    private String badgeColor;

    /** Ordering among siblings (ascending). */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    /** Global default visibility (the "All employees" hide/show switch). */
    @Column(name = "is_visible")
    private Boolean isVisible = true;

    /** Soft-delete / active flag. */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /** Help text for this page's header help button (HTML allowed). */
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
