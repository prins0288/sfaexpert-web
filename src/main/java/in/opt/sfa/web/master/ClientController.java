package in.opt.sfa.web.master;

import in.opt.sfa.common.util.ExcelUtil;
import in.opt.sfa.tenant.entity.Client;
import in.opt.sfa.tenant.repository.AreaRepository;
import in.opt.sfa.tenant.repository.ClientRepository;
import in.opt.sfa.tenant.repository.ClientTypeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Client master — the main report (Doctors / Chemists / Stockists). Tenant-routed. */
@Tag(name = "Client Master", description = "Clients (doctors / chemists / stockists)")
@RestController
@RequestMapping("/api/master/client")
public class ClientController {

    private final ClientRepository clients;
    private final ClientTypeRepository clientTypes;
    private final AreaRepository areas;

    public ClientController(ClientRepository clients, ClientTypeRepository clientTypes, AreaRepository areas) {
        this.clients = clients;
        this.clientTypes = clientTypes;
        this.areas = areas;
    }

    @GetMapping
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Client> list() {
        return clients.findAllByOrderByOidAsc();
    }

    @GetMapping("/{oid}")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public Client get(@PathVariable Long oid) {
        return clients.findById(oid)
                .orElseThrow(() -> new IllegalStateException("Client not found: " + oid));
    }

    @PostMapping
    @Transactional(transactionManager = "tenantTransactionManager")
    public Client save(@RequestBody Client form) {
        if (form.getStatus() == null || form.getStatus().isBlank()) {
            form.setStatus("Y");
        }
        return clients.save(form);
    }

    @PostMapping("/{oid}/status")
    @Transactional(transactionManager = "tenantTransactionManager")
    public Client status(@PathVariable Long oid, @RequestParam String status) {
        Client c = clients.findById(oid)
                .orElseThrow(() -> new IllegalStateException("Client not found: " + oid));
        c.setStatus(status);
        return clients.save(c);
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() throws Exception {
        byte[] xlsx = ExcelUtil.template("Client",
                List.of("client_code", "client_type", "prefix", "name", "firm_name",
                        "area", "address", "city", "mobile", "email", "drug_license_no", "gst_no"),
                List.of("CL-D003", "DOC", "Dr.", "Sample Name", "",
                        "AR-DADAR", "Address", "Mumbai", "9800000000", "x@y.com", "", ""));
        return ExcelUtil.xlsxResponse("client_template.xlsx", xlsx);
    }

    /** Bulk upload = upsert by client_code; client_type / area resolved by code or name. */
    @PostMapping("/upload")
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) throw new IllegalStateException("Please choose a file.");
        int inserted = 0, updated = 0, skipped = 0;
        for (Map<String, String> row : ExcelUtil.read(file.getInputStream())) {
            String code = row.get("client_code"), name = row.get("name");
            Long typeOid = resolveClientType(row.get("client_type"));
            if (RouteController.isBlank(code) || RouteController.isBlank(name) || typeOid == null) {
                skipped++;
                continue;
            }
            Client c = clients.findByClientCode(code.trim()).orElseGet(Client::new);
            boolean isNew = c.getOid() == null;
            c.setClientCode(code.trim());
            c.setClientTypeOid(typeOid);
            c.setPrefix(row.get("prefix"));
            c.setName(name.trim());
            c.setFirmName(row.get("firm_name"));
            c.setAreaOid(resolveArea(row.get("area")));
            c.setAddress(row.get("address"));
            c.setCity(row.get("city"));
            c.setMobile(row.get("mobile"));
            c.setEmail(row.get("email"));
            c.setDrugLicenseNo(row.get("drug_license_no"));
            c.setGstNo(row.get("gst_no"));
            c.setStatus("Y");
            clients.save(c);
            if (isNew) inserted++; else updated++;
        }
        return Map.of("inserted", inserted, "updated", updated, "skipped", skipped);
    }

    private Long resolveClientType(String v) {
        return RouteController.isBlank(v) ? null
                : clientTypes.findFirstByTypeCodeOrTypeName(v.trim(), v.trim()).map(t -> t.getOid()).orElse(null);
    }
    private Long resolveArea(String v) {
        return RouteController.isBlank(v) ? null
                : areas.findFirstByAreaCodeOrAreaName(v.trim(), v.trim()).map(a -> a.getOid()).orElse(null);
    }
}
