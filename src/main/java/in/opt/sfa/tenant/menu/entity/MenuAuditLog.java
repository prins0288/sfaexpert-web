package in.opt.sfa.tenant.menu.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One row per add / edit / activate / deactivate / delete made through the
 * Menu Master admin page (TENANT db). {@link CompanyMenuSelf} itself carries
 * no created_by/updated_by — this table is the audit trail answering "who
 * changed this menu item, and how" for the Menu Audit Report.
 *
 * label/menuType are a SNAPSHOT at the time of the action (not a live join),
 * so the report still reads correctly after a later rename or a hard delete
 * (menuItemId then points at a row that no longer exists).
 */
@Entity
@Table(name = "menu_audit_log")
@Getter
@Setter
public class MenuAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The affected company_menu_self.id — may no longer exist after a DELETE. */
    @Column(name = "menu_item_id")
    private Long menuItemId;

    @Column(length = 128)
    private String label;

    @Column(name = "menu_type", length = 10)
    private String menuType;

    /** CREATE | UPDATE | ACTIVATE | DEACTIVATE | DELETE */
    @Column(length = 20, nullable = false)
    private String action;

    /** emp_id of the admin who made the change — resolved to a display name at read time. */
    @Column(name = "changed_by", length = 100)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    /** Human-readable "field: old -> new" summary; null for CREATE/DELETE. */
    @Column(columnDefinition = "TEXT")
    private String details;
}
