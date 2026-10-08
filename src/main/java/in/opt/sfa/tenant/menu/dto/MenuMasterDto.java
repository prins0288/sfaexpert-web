package in.opt.sfa.tenant.menu.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Request + response shape for the Menu Master CRUD API. */
@Getter
@Setter
public class MenuMasterDto {
    private Long id;
    private Long parentId;
    /** Read-only: the parent's label, for display in the list. */
    private String parentLabel;
    /** Read-only: nesting depth (0 = top level), for indenting the parent picker. */
    private Integer depth;
    /** Read-only: full breadcrumb, e.g. "Masters > Catalog > Degree". */
    private String pathLabel;

    private String menuType;   // WEB | APP | BOTH
    private String label;
    private String title;
    private String icon;
    private String page;
    private String href;
    private String baseUrl;
    private String target;     // _SELF | _BLANK
    private String badge;
    private String badgeColor;
    private Integer sortOrder;
    private Boolean isVisible;
    private Boolean isActive;
    private String description;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime updatedAt;
}
