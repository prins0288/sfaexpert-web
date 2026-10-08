package in.opt.sfa.tenant.master.bank.controller;

import in.opt.sfa.tenant.master.bank.dto.BankMasterDto;
import in.opt.sfa.tenant.master.bank.service.BankMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Bank master — production-style CRUD with audit + soft-delete. Tenant-routed. */
@RestController
@RequestMapping("/api/master/bank-master")
@Tag(name = "Bank Master", description = "Banks with short name, display order, soft-delete status and audit trail")
public class BankMasterController {

    private final BankMasterService service;

    public BankMasterController(BankMasterService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all banks (active + inactive)")
    public List<BankMasterDto> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one bank by id")
    public BankMasterDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @Operation(summary = "Create or update a bank",
            description = "Send id to update, omit it to create. Audit fields are set by the server.")
    public BankMasterDto save(@RequestBody BankMasterDto form) {
        return service.save(form);
    }

    @PostMapping("/save-multiple")
    @Operation(summary = "Bulk create banks (blank / duplicate code or name rows are skipped)")
    public Map<String, Object> saveMultiple(@RequestBody List<BankMasterDto> forms) {
        return service.saveMultiple(forms);
    }

    @PostMapping("/{id}/status")
    @Operation(summary = "Activate / deactivate a bank (status = true or false)")
    public BankMasterDto status(@PathVariable Long id, @RequestParam boolean status) {
        return service.updateStatus(id, status);
    }
}
