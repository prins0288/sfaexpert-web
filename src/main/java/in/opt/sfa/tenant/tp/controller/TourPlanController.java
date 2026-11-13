package in.opt.sfa.tenant.tp.controller;

import in.opt.sfa.security.RequiresPermission;
import in.opt.sfa.tenant.tp.service.TourPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "Tour Plan", description = "Month-wise tour plan, submit, manager / admin approval")
@RestController
@RequestMapping("/api/tp")
public class TourPlanController {

    private final TourPlanService service;

    public TourPlanController(TourPlanService service) {
        this.service = service;
    }

    @GetMapping("/options")
    @Operation(summary = "Planner options of me, or of the plan's owner (empId) for a manager / admin")
    public Map<String, Object> options(@RequestParam(required = false) String empId) { return service.options(empId); }

    @GetMapping("/plan")
    @Operation(summary = "A month's plan with days, holidays and the approval trail (own, or a team member's with empId)")
    public Map<String, Object> plan(@RequestParam int year, @RequestParam int month, @RequestParam(required = false) String empId) { return service.get(year, month, empId); }

    @GetMapping("/list")
    @Operation(summary = "Submitted plans of the team (manager) / everyone (admin) for a month, with canApprove")
    public List<Map<String, Object>> list(@RequestParam int year, @RequestParam int month) { return service.list(year, month); }

    @RequiresPermission("TP_SAVE")
    @PostMapping("/day")
    public Map<String, Object> saveDay(@RequestBody Map<String, Object> body) { return service.saveDay(body); }

    @RequiresPermission("TP_SAVE")
    @DeleteMapping("/day")
    public void clearDay(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @RequestParam(required = false) String empId) { service.clearDay(date, empId); }

    @RequiresPermission("TP_SAVE")
    @PostMapping("/goal")
    public void goal(@RequestBody Map<String, Object> body) { service.saveGoal(Integer.parseInt(String.valueOf(body.get("year"))), Integer.parseInt(String.valueOf(body.get("month"))), body.get("goal") == null ? null : String.valueOf(body.get("goal"))); }

    @RequiresPermission("TP_SAVE")
    @PostMapping("/submit")
    public Map<String, Object> submit(@RequestBody Map<String, Object> body) { return service.submit(Integer.parseInt(String.valueOf(body.get("year"))), Integer.parseInt(String.valueOf(body.get("month")))); }

    @PostMapping("/{tourMain}/approve")
    public Map<String, Object> approve(@PathVariable Long tourMain) { return service.approve(tourMain); }

    @PostMapping("/{tourMain}/unapprove")
    @Operation(summary = "Take a submitted / approved plan back to the employee for changes (reason required)")
    public Map<String, Object> unapprove(@PathVariable Long tourMain, @RequestBody Map<String, Object> body) { return service.unapprove(tourMain, body.get("reason") == null ? null : String.valueOf(body.get("reason"))); }

    @PostMapping("/{tourMain}/delete")
    @Operation(summary = "Delete a whole plan (manager of the employee / admin, reason required); recorded in tour_emp_delete_log")
    public Map<String, Object> deletePlan(@PathVariable Long tourMain, @RequestBody Map<String, Object> body) { return service.deletePlan(tourMain, body.get("reason") == null ? null : String.valueOf(body.get("reason"))); }

    @GetMapping("/deleted")
    @Operation(summary = "Record of deleted plans: who, when, why, with the saved plan")
    public List<Map<String, Object>> deleted(@RequestParam(required = false) Integer year, @RequestParam(required = false) Integer month) { return service.deleted(year, month); }

    @PostMapping("/{tourMain}/reject")
    public Map<String, Object> reject(@PathVariable Long tourMain, @RequestBody Map<String, Object> body) { return service.reject(tourMain, body.get("reason") == null ? null : String.valueOf(body.get("reason"))); }
}
