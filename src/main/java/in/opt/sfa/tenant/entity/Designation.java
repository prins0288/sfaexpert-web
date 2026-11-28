package in.opt.sfa.tenant.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * designation_master (TENANT db). An employee holds only the designation
 * (emp_detail.designation -> id); their level comes from here, and the role
 * from that hierarchy_level by the company's ranges (LevelRoleService).
 */
@Entity
@Table(name = "designation_master")
@Getter
@Setter
public class Designation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** 'MR', 'ABM', 'Dy. RSM' … */
    @Column(name = "designation", nullable = false, length = 50)
    private String designation;

    /** 10, 15, 20, 32 … — unique, higher = more senior. */
    @Column(name = "hierarchy_level", nullable = false)
    private Integer hierarchyLevel;

    /** UI sorting (1, 2, 3 …). */
    @Column(name = "display_order")
    private Integer displayOrder;

    /** Junior / Senior (optional). */
    @Column(name = "grade", length = 20)
    private String grade;

    /** Field / Manager / Admin / Support (information only — the role comes from the level). */
    @Column(name = "designation_category", length = 30)
    private String designationCategory;

    @Column(name = "is_active")
    private Boolean active = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
