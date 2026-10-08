package in.opt.sfa.tenant.master.client.service;

import in.opt.sfa.common.util.ExcelUtil;
import in.opt.sfa.common.util.Strings;
import in.opt.sfa.tenant.master.area.repository.AreaRepository;
import in.opt.sfa.tenant.master.client.dto.ClientDto;
import in.opt.sfa.tenant.master.client.entity.Client;
import in.opt.sfa.tenant.master.client.mapper.ClientMapper;
import in.opt.sfa.tenant.master.client.repository.ClientRepository;
import in.opt.sfa.tenant.master.clienttype.repository.ClientTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ClientService {

    private final ClientRepository clients;
    private final ClientTypeRepository clientTypes;
    private final AreaRepository areas;
    private final ClientMapper mapper;

    public ClientService(ClientRepository clients, ClientTypeRepository clientTypes,
                          AreaRepository areas, ClientMapper mapper) {
        this.clients = clients;
        this.clientTypes = clientTypes;
        this.areas = areas;
        this.mapper = mapper;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<ClientDto> list() {
        return clients.findAllByOrderByOidAsc().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public ClientDto get(Long oid) {
        return mapper.toDto(find(oid));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public ClientDto save(ClientDto form) {
        if (Strings.isBlank(form.getStatus())) form.setStatus("Y");
        return mapper.toDto(clients.save(mapper.toEntity(form)));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public ClientDto updateStatus(Long oid, String status) {
        Client c = find(oid);
        c.setStatus(status);
        return mapper.toDto(clients.save(c));
    }

    public byte[] template() throws Exception {
        return ExcelUtil.template("Client",
                List.of("client_code", "client_type", "prefix", "name", "firm_name",
                        "area", "address", "city", "mobile", "email", "drug_license_no", "gst_no"),
                List.of("CL-D003", "DOC", "Dr.", "Sample Name", "",
                        "AR-DADAR", "Address", "Mumbai", "9800000000", "x@y.com", "", ""));
    }

    /** Bulk upload = upsert by client_code; client_type / area resolved by code or name. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> upload(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) throw new IllegalStateException("Please choose a file.");
        int inserted = 0, updated = 0, skipped = 0;
        for (Map<String, String> row : ExcelUtil.read(file.getInputStream())) {
            String code = row.get("client_code"), name = row.get("name");
            Long typeOid = resolveClientType(row.get("client_type"));
            if (Strings.isBlank(code) || Strings.isBlank(name) || typeOid == null) {
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

    private Client find(Long oid) {
        return clients.findById(oid).orElseThrow(() -> new IllegalStateException("Client not found: " + oid));
    }

    private Long resolveClientType(String v) {
        return Strings.isBlank(v) ? null
                : clientTypes.findFirstByTypeCodeOrTypeName(v.trim(), v.trim()).map(t -> t.getOid()).orElse(null);
    }
    private Long resolveArea(String v) {
        return Strings.isBlank(v) ? null
                : areas.findFirstByAreaCodeOrAreaName(v.trim(), v.trim()).map(a -> a.getOid()).orElse(null);
    }
}
