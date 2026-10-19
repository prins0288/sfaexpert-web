package in.opt.sfa.tenant.master.clientmaster.controller;

import in.opt.sfa.common.util.ExcelUtil;
import in.opt.sfa.security.RequiresPermission;
import in.opt.sfa.tenant.master.clientmaster.dto.ClientMasterDto;
import in.opt.sfa.tenant.master.clientmaster.service.ClientMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** Client Master — Master Entry > Client Master. Tenant-routed. */
@RestController
@RequestMapping("/api/master/client-master")
@Tag(name = "Client Master", description = "Doctors / chemists / stockists with approval, delete requests, photo and bulk upload")
public class ClientMasterController {

    private final ClientMasterService service;

    public ClientMasterController(ClientMasterService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "All clients (active + inactive) with master names and social handles")
    public List<ClientMasterDto> list() {
        return service.list();
    }

    @GetMapping("/options")
    @Operation(summary = "Form dropdowns (districts / cities / employees carry stateId for filtering) and fixed lists")
    public Map<String, Object> options() {
        return service.options();
    }

    @GetMapping("/next-code")
    @Operation(summary = "Preview of the next code for a client type (e.g. DO00001); given for real on save")
    public Map<String, String> nextCode(@RequestParam(required = false) Long clientTypeId) {
        return Map.of("clientCode", service.nextCode(clientTypeId));
    }

    @GetMapping("/{id}")
    public ClientMasterDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @RequiresPermission("CLIENT_MASTER_SAVE")
    @PostMapping
    @Operation(summary = "Create (no id) or update a client. Blank clientCode on create = generated from the client type")
    public ClientMasterDto save(@RequestBody ClientMasterDto form) {
        return service.save(form);
    }

    @RequiresPermission("CLIENT_MASTER_STATUS")
    @PostMapping("/{id}/status")
    public ClientMasterDto status(@PathVariable Long id, @RequestParam boolean active) {
        return service.updateStatus(id, active);
    }

    @RequiresPermission("CLIENT_MASTER_APPROVE")
    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve the step the client waits for (manager / admin, per the company's client approval flow)")
    public ClientMasterDto approve(@PathVariable Long id) {
        return service.approve(id);
    }

    @RequiresPermission("CLIENT_MASTER_APPROVE")
    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject at the current step; body {reason}. Editing the client sends it for approval again")
    public ClientMasterDto reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return service.reject(id, body == null ? null : body.get("reason"));
    }

    @RequiresPermission("CLIENT_MASTER_SAVE")
    @PostMapping("/{id}/delete-request")
    @Operation(summary = "Ask for the client to be removed; body {reason}")
    public ClientMasterDto requestDelete(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return service.requestDelete(id, body == null ? null : body.get("reason"));
    }

    @RequiresPermission("CLIENT_MASTER_APPROVE")
    @PostMapping("/{id}/delete-decision")
    @Operation(summary = "accept=true deactivates the client, false keeps it; closes the request")
    public ClientMasterDto decideDelete(@PathVariable Long id, @RequestParam boolean accept) {
        return service.decideDelete(id, accept);
    }

    @RequiresPermission("CLIENT_MASTER_SAVE")
    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ClientMasterDto photo(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws Exception {
        return service.setPhoto(id, file);
    }

    @RequiresPermission("CLIENT_MASTER_SAVE")
    @DeleteMapping("/{id}/photo")
    public ClientMasterDto removePhoto(@PathVariable Long id) throws Exception {
        return service.setPhoto(id, null);
    }

    @RequiresPermission("CLIENT_MASTER_UPLOAD")
    @GetMapping("/template")
    public ResponseEntity<byte[]> template() throws Exception {
        return ExcelUtil.xlsxResponse("client_master_template.xlsx", service.template());
    }

    @RequiresPermission("CLIENT_MASTER_UPLOAD")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Bulk create / update clients from the Excel template; bad rows are skipped with the reason")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) throws Exception {
        return service.upload(file);
    }
}
