package in.opt.sfa.tenant.master.item.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** One salt line, typed by the user: "Paracetamol 500 mg per tablet". */
@Getter
@Setter
public class ItemCompositionDto {
    private String moleculeName;
    private BigDecimal strength;
    private String strengthUnit;
    private String perQty;
}
