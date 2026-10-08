package in.opt.sfa.tenant.master.permission.controller;

import in.opt.sfa.tenant.master.permission.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** What the logged-in user is allowed to do — the front-end uses this to hide/show buttons and menu items. */
@RestController
@RequestMapping("/api/permissions")
@Tag(name = "Permissions", description = "Effective permissions for the current user")
public class MyPermissionsController {

    private final PermissionService permissionService;

    public MyPermissionsController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping("/my")
    @Operation(summary = "Permission code -> allowed, for the currently logged-in user",
            description = "A permission with no configured assignment defaults to true (full access).")
    public Map<String, Boolean> my() {
        return permissionService.myPermissions();
    }
}
