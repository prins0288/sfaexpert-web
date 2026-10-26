package in.opt.sfa.tenant.master.team.controller;

import in.opt.sfa.security.RequiresPermission;
import in.opt.sfa.tenant.master.team.service.TeamHierarchyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Team hierarchy — Masters > Master Entry > Team Hierarchy. Tenant-routed. */
@Tag(name = "Team Hierarchy", description = "Who reports to whom: add / move / remove team members")
@RestController
@RequestMapping("/api/master/team-hierarchy")
public class TeamHierarchyController {

    private final TeamHierarchyService service;

    public TeamHierarchyController(TeamHierarchyService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "All employees with manager, designation, level, team size and whether you may change them")
    public List<Map<String, Object>> list() {
        return service.list();
    }

    @RequiresPermission("TEAM_HIERARCHY_SAVE")
    @PostMapping("/{empId}/manager")
    @Operation(summary = "Add / move / remove: body {managerId, effectiveDate?, remarks?}; blank managerId removes from the team")
    public Map<String, Object> setManager(@PathVariable String empId, @RequestBody Map<String, String> body) {
        return service.setManager(empId, body == null ? null : body.get("managerId"),
                body == null ? null : body.get("effectiveDate"), body == null ? null : body.get("remarks"));
    }
}
