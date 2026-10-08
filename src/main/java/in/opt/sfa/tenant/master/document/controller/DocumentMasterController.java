package in.opt.sfa.tenant.master.document.controller;

import in.opt.sfa.tenant.master.document.dto.DocumentMasterDto;
import in.opt.sfa.tenant.master.document.service.DocumentMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Document master — production-style CRUD with audit + soft-delete. Tenant-routed. */
@RestController
@RequestMapping("/api/master/document-master")
@Tag(name = "Document Master", description = "Document lookup with short name, display order, soft-delete status and audit trail")
public class DocumentMasterController {

    private final DocumentMasterService service;

    public DocumentMasterController(DocumentMasterService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all (active + inactive)")
    public List<DocumentMasterDto> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one by id")
    public DocumentMasterDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @Operation(summary = "Create or update",
            description = "Send id to update, omit it to create. Audit fields are set by the server.")
    public DocumentMasterDto save(@RequestBody DocumentMasterDto form) {
        return service.save(form);
    }

    @PostMapping("/save-multiple")
    @Operation(summary = "Bulk create (blank / duplicate code or name rows are skipped)")
    public Map<String, Object> saveMultiple(@RequestBody List<DocumentMasterDto> forms) {
        return service.saveMultiple(forms);
    }

    @PostMapping("/{id}/status")
    @Operation(summary = "Activate / deactivate (status = true or false)")
    public DocumentMasterDto status(@PathVariable Long id, @RequestParam boolean status) {
        return service.updateStatus(id, status);
    }
}
