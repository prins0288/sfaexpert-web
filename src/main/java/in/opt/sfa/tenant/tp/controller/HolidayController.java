package in.opt.sfa.tenant.tp.controller;

import in.opt.sfa.security.RequiresPermission;
import in.opt.sfa.tenant.tp.service.HolidayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "Holiday", description = "Holiday master, assignment to state / HQ / employee, weekly off")
@RestController
@RequestMapping("/api/holiday")
public class HolidayController {

    private final HolidayService service;

    public HolidayController(HolidayService service) {
        this.service = service;
    }

    @GetMapping("/config")
    @Operation(summary = "Weekly off days + states / HQs / employees to assign to (admin)")
    public Map<String, Object> config() { return service.config(); }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) Integer year) { return service.list(year); }

    @RequiresPermission("HOLIDAY_SAVE")
    @PostMapping
    public Map<String, Object> save(@RequestBody Map<String, Object> body) { return service.save(body); }

    @RequiresPermission("HOLIDAY_SAVE")
    @PostMapping("/assign")
    @Operation(summary = "Add state / HQ / employee targets to many holidays at once")
    @SuppressWarnings("unchecked")
    public Map<String, Object> assign(@RequestBody Map<String, Object> body) {
        List<Long> ids = ((List<Object>) body.getOrDefault("holidayIds", List.of())).stream().map(o -> Long.valueOf(String.valueOf(o))).toList();
        return Map.of("added", service.assign(ids, body));
    }

    @GetMapping("/assignments")
    @Operation(summary = "Assignment report: holiday -> state / HQ / employee (filter by date, target type and target)")
    public List<Map<String, Object>> assignments(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                 @RequestParam(required = false) String type, @RequestParam(required = false) String target) {
        return service.assignments(from, to, type, target);
    }

    @RequiresPermission("HOLIDAY_SAVE")
    @PostMapping("/assignments/delete")
    @Operation(summary = "Remove many wrong assignments at once")
    @SuppressWarnings("unchecked")
    public Map<String, Object> deleteAssignments(@RequestBody Map<String, Object> body) {
        return service.deleteAssignments(((List<Object>) body.getOrDefault("ids", List.of())).stream().map(o -> Long.valueOf(String.valueOf(o))).toList());
    }

    @RequiresPermission("HOLIDAY_SAVE")
    @PostMapping("/delete")
    @Operation(summary = "Delete many holidays at once")
    @SuppressWarnings("unchecked")
    public Map<String, Object> delete(@RequestBody Map<String, Object> body) {
        return Map.of("deleted", service.delete(((List<Object>) body.getOrDefault("ids", List.of())).stream().map(o -> Long.valueOf(String.valueOf(o))).toList()));
    }

    @RequiresPermission("HOLIDAY_SAVE")
    @PostMapping("/weekly-off")
    public Map<String, Object> weeklyOff(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked") List<String> days = (List<String>) body.getOrDefault("days", List.of());
        return Map.of("days", service.saveWeeklyOff(days));
    }

    @GetMapping("/weekly-rules")
    @Operation(summary = "Weekly off rules (3rd Saturday, Saturday + Sunday ...) with their state / HQ / employee targets")
    public List<Map<String, Object>> weeklyRules() { return service.weeklyRules(); }

    @RequiresPermission("HOLIDAY_SAVE")
    @PostMapping("/weekly-rules")
    public Map<String, Object> saveRule(@RequestBody Map<String, Object> body) { return service.saveRule(body); }

    @RequiresPermission("HOLIDAY_SAVE")
    @PostMapping("/weekly-rules/delete")
    @SuppressWarnings("unchecked")
    public Map<String, Object> deleteRules(@RequestBody Map<String, Object> body) {
        return Map.of("deleted", service.deleteRules(((List<Object>) body.getOrDefault("ids", List.of())).stream().map(o -> Long.valueOf(String.valueOf(o))).toList()));
    }

    @RequiresPermission("HOLIDAY_SAVE")
    @PostMapping("/weekly-rules/targets/delete")
    @SuppressWarnings("unchecked")
    public Map<String, Object> deleteRuleTargets(@RequestBody Map<String, Object> body) {
        return service.deleteRuleTargets(((List<Object>) body.getOrDefault("ids", List.of())).stream().map(o -> Long.valueOf(String.valueOf(o))).toList());
    }
}
