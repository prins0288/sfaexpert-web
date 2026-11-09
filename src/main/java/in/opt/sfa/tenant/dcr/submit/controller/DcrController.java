package in.opt.sfa.tenant.dcr.submit.controller;

import in.opt.sfa.security.RequiresPermission;
import in.opt.sfa.storage.FileUploadService;
import in.opt.sfa.storage.StoredFile;
import in.opt.sfa.tenant.dcr.submit.dto.DcrDto;
import in.opt.sfa.tenant.dcr.submit.service.DcrService;
import in.opt.sfa.tenant.dcr.submit.service.DcrWorktypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Daily Call Report — fill / submit (own), list / approve (manager, admin), and the tabs setup (admin). */
@Tag(name = "DCR", description = "Daily Call Report: submit, list, approve; DCR tabs per user")
@RestController
@RequestMapping("/api/dcr")
public class DcrController {

    private final DcrService service;
    private final DcrWorktypeService worktypes;
    private final FileUploadService uploads;

    public DcrController(DcrService service, DcrWorktypeService worktypes, FileUploadService uploads) {
        this.service = service;
        this.worktypes = worktypes;
        this.uploads = uploads;
    }

    @GetMapping("/options")
    @Operation(summary = "Form options for the signed-in user: his tabs, clients, areas, items, expense types …")
    public Map<String, Object> options() {
        return service.options();
    }

    @GetMapping
    @Operation(summary = "DCRs in a date range: own, a manager's team, or everyone (admin); optional empId")
    public List<Map<String, Object>> list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                          @RequestParam(required = false) String empId) {
        return service.list(from, to, empId);
    }

    @GetMapping("/{dcrId}")
    public DcrDto get(@PathVariable Long dcrId) {
        return service.get(dcrId);
    }

    @GetMapping("/mine")
    @Operation(summary = "My DCR of a date (null when there is none yet)")
    public DcrDto mine(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.getMine(date);
    }

    @RequiresPermission("DCR_SAVE")
    @PostMapping("/start")
    @Operation(summary = "Start MY day: creates the DCR draft with time in = now. Nothing else can be filled before this")
    public DcrDto start(@RequestBody DcrDto form) {
        return service.start(form);
    }

    @RequiresPermission("DCR_SAVE")
    @PostMapping
    @Operation(summary = "Create / replace MY DCR of dcrDate; submit=true sends it for approval, false keeps a draft")
    public DcrDto save(@RequestBody DcrDto form) {
        return service.save(form);
    }

    // Rights checked in the service: the owner's immediate manager (or the next one up), or an admin; never your own
    @RequiresPermission("DCR_APPROVE")
    @PostMapping("/{dcrId}/approve")
    public DcrDto approve(@PathVariable Long dcrId, @RequestBody(required = false) Map<String, String> body) {
        return service.approve(dcrId, body == null ? null : body.get("remarks"));
    }

    @RequiresPermission("DCR_APPROVE")
    @PostMapping("/{dcrId}/reject")
    public DcrDto reject(@PathVariable Long dcrId, @RequestBody Map<String, String> body) {
        return service.reject(dcrId, body == null ? null : body.get("reason"));
    }

    @RequiresPermission("DCR_SAVE")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a bill / odometer photo / signature; returns the stored path to put in the DCR")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam(defaultValue = "image") String kind) throws Exception {
        StoredFile f = uploads.upload("dcr", file, "document".equalsIgnoreCase(kind) ? FileUploadService.Kind.DOCUMENT : FileUploadService.Kind.IMAGE, true);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("path", f.path());
        m.put("thumbnailPath", f.thumbnailPath());
        m.put("name", f.originalName());
        m.put("type", f.type());
        return m;
    }

    // ---- DCR tabs (app_dcr_worktype) — admin ----

    @GetMapping("/worktypes")
    @Operation(summary = "Every DCR-tab row (who gets which tab) — admin")
    public Map<String, Object> worktypes(@RequestParam(required = false) String empId) {
        return worktypes.list(empId);
    }

    @PostMapping("/worktypes")
    @Operation(summary = "Add / change one DCR-tab row for ALL, an employee, a designation or a hierarchy level — admin")
    public void saveWorktype(@RequestBody Map<String, Object> body) {
        worktypes.save(body);
    }

    @DeleteMapping("/worktypes/{id}")
    public void deleteWorktype(@PathVariable Long id) {
        worktypes.delete(id);
    }
}
