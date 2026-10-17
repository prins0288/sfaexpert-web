package in.opt.sfa.tenant.master.item.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * One salt of an item (TENANT db, item_composition), typed by the user — no
 * molecule master. The generated column molecule_key (LOWER(TRIM(name))) keeps
 * a salt from appearing twice in one item; it is DB-only and not mapped.
 */
@Entity
@Table(name = "item_composition")
@Getter
@Setter
public class ItemComposition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "molecule_name", nullable = false, length = 150)
    private String moleculeName;

    @Column(precision = 12, scale = 3)
    private BigDecimal strength;

    @Column(name = "strength_unit", length = 20)
    private String strengthUnit;

    @Column(name = "per_qty", length = 30)
    private String perQty;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 1;
}
