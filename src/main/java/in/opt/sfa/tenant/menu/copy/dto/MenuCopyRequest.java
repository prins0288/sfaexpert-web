package in.opt.sfa.tenant.menu.copy.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MenuCopyRequest {
    private String sourceCompanyCode;
    private List<String> targetCompanyCodes;
    /** ids (in the SOURCE tenant) of the item(s) to copy — each copied with its FULL subtree, any depth. */
    private List<Long> itemIds;
}
