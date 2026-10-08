package in.opt.sfa.tenant.master.client.controller;

import in.opt.sfa.common.util.ExcelUtil;
import in.opt.sfa.tenant.master.client.dto.ClientDto;
import in.opt.sfa.tenant.master.client.service.ClientService;
import in.opt.sfa.security.RequiresPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** Client master — the main report (Doctors / Chemists / Stockists). Tenant-routed. */
@Tag(name = "Client Master", description = "Clients (doctors / chemists / stockists)")
@RestController
@RequestMapping("/api/master/client")
public class ClientController {

    private final ClientService service;

    public ClientController(ClientService service) {
        this.service = service;
    }

    @GetMapping
    public List<ClientDto> list() {
        return service.list();
    }

    @GetMapping("/{oid}")
    public ClientDto get(@PathVariable Long oid) {
        return service.get(oid);
    }

    @RequiresPermission("CLIENT_SAVE")
    @PostMapping
    public ClientDto save(@RequestBody ClientDto form) {
        return service.save(form);
    }

    @RequiresPermission("CLIENT_STATUS")
    @PostMapping("/{oid}/status")
    public ClientDto status(@PathVariable Long oid, @RequestParam String status) {
        return service.updateStatus(oid, status);
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() throws Exception {
        return ExcelUtil.xlsxResponse("client_template.xlsx", service.template());
    }

    /** Bulk upload = upsert by client_code; client_type / area resolved by code or name. */
    @RequiresPermission("CLIENT_UPLOAD")
    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) throws Exception {
        return service.upload(file);
    }
}
