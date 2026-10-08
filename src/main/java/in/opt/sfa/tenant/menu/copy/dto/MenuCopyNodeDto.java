package in.opt.sfa.tenant.menu.copy.dto;

import lombok.Getter;
import lombok.Setter;

/** One row in the "pick items to copy" tree — a lighter shape than MenuMasterDto,
 *  just enough to render a checkbox tree and drive the copy request. */
@Getter
@Setter
public class MenuCopyNodeDto {
    private Long id;
    private Long parentId;
    private Integer depth;
    private String label;
    private String icon;
    private String menuType;
    private String href;
    private String page;
    private Boolean isActive;
}
