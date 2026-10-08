package in.opt.sfa.tenant.menu.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A sparse PER-EMPLOYEE menu visibility override (TENANT db). Only rows that
 * differ from the base company_menu_self's global {@link CompanyMenuSelf#getIsVisible()} are
 * stored — one row per (emp_id, menu_item_id). Used for both:
 *   - admin "employee-wise" hide/show (emp_id = that employee), and
 *   - "self" hide/show (emp_id = the caller's own employee id).
 *
 * When building a user's menu: effective visible = override for their emp_id if
 * present, else the item's global is_visible.
 */
@Entity
@Table(name = "menu_visibility",
        uniqueConstraints = @UniqueConstraint(name = "uq_menu_visibility", columnNames = {"emp_id", "menu_item_id"}))
@Getter
@Setter
public class MenuVisibility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "emp_id", nullable = false, length = 100)
    private String empId;

    @Column(name = "menu_item_id", nullable = false)
    private Long menuItemId;

    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible = true;

    /** Per-employee re-parent override; null = follow the base menu_item.parent_id. */
    @Column(name = "parent_id")
    private Long parentId;

    /** Per-employee reorder override; null = follow the base menu_item.sort_order. */
    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
