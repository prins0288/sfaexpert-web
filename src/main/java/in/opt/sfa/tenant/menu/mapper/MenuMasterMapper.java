package in.opt.sfa.tenant.menu.mapper;

import in.opt.sfa.common.enums.LinkTarget;
import in.opt.sfa.common.enums.MenuTypeEnum;
import in.opt.sfa.tenant.menu.dto.MenuMasterDto;
import in.opt.sfa.tenant.menu.entity.CompanyMenuSelf;
import org.springframework.stereotype.Component;

@Component
public class MenuMasterMapper {

    public MenuMasterDto toDto(CompanyMenuSelf e) {
        MenuMasterDto d = new MenuMasterDto();
        d.setId(e.getId());
        d.setParentId(e.getParentId());
        d.setMenuType(e.getMenuType() == null ? "WEB" : e.getMenuType().name());
        d.setLabel(e.getLabel());
        d.setTitle(e.getTitle());
        d.setIcon(e.getIcon());
        d.setPage(e.getPage());
        d.setHref(e.getHref());
        d.setBaseUrl(e.getBaseUrl());
        d.setTarget(e.getTarget() == null ? "_SELF" : e.getTarget().name());
        d.setBadge(e.getBadge());
        d.setBadgeColor(e.getBadgeColor());
        d.setSortOrder(e.getSortOrder());
        d.setIsVisible(e.getIsVisible());
        d.setIsActive(e.getIsActive());
        d.setDescription(e.getDescription());
        d.setCreatedAt(e.getCreatedAt());
        d.setUpdatedAt(e.getUpdatedAt());
        return d;
    }

    /** Copies only the editable fields — parentId/menuType/target parsed from their string forms;
     *  empId, is_visible and created/updated_at are never touched here (server/other-flow managed). */
    public void applyTo(CompanyMenuSelf target, MenuMasterDto d) {
        target.setParentId(d.getParentId());
        target.setMenuType(parseMenuType(d.getMenuType()));
        target.setLabel(d.getLabel());
        target.setTitle(d.getTitle());
        target.setIcon(d.getIcon());
        target.setPage(d.getPage());
        target.setHref(d.getHref());
        target.setBaseUrl(d.getBaseUrl());
        target.setTarget(parseLinkTarget(d.getTarget()));
        target.setBadge(d.getBadge());
        target.setBadgeColor(d.getBadgeColor());
        if (d.getSortOrder() != null) target.setSortOrder(d.getSortOrder());
    }

    private MenuTypeEnum parseMenuType(String v) {
        try { return v == null ? MenuTypeEnum.WEB : MenuTypeEnum.valueOf(v.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return MenuTypeEnum.WEB; }
    }

    private LinkTarget parseLinkTarget(String v) {
        try { return v == null ? LinkTarget._SELF : LinkTarget.valueOf(v.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return LinkTarget._SELF; }
    }
}
