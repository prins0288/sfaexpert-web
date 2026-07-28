package in.opt.sfa.web;

import in.opt.sfa.security.UserContext;
import in.opt.sfa.tenant.entity.CompanyProfile;
import in.opt.sfa.tenant.repository.CompanyProfileRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Per-tenant company profile + logo (single row, id = 1). Tenant-routed.
 */
@RestController
@RequestMapping("/api/company")
@Tag(name = "Company Profile", description = "Per-tenant company details and logo")
public class CompanyController {

    private final CompanyProfileRepository companies;

    public CompanyController(CompanyProfileRepository companies) {
        this.companies = companies;
    }

    @GetMapping
    @Operation(summary = "Get the company profile")
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public CompanyProfile get() {
        return companies.findById(1).orElseGet(CompanyController::blank);
    }

    @PostMapping
    @Operation(summary = "Update the company profile (name, address, contact)")
    @Transactional(transactionManager = "tenantTransactionManager")
    public CompanyProfile update(@RequestBody CompanyProfile form) {
        CompanyProfile c = companies.findById(1).orElseGet(CompanyController::blank);
        c.setCompanyName(form.getCompanyName());
        c.setAddress(form.getAddress());
        c.setCity(form.getCity());
        c.setPhone(form.getPhone());
        c.setEmail(form.getEmail());
        c.setWebsite(form.getWebsite());
        stamp(c);
        return companies.save(c);
    }

    @PostMapping("/logo")
    @Operation(summary = "Set the company logo (base64 data URI in `image`; blank clears it)")
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> logo(@RequestBody Map<String, String> body) {
        CompanyProfile c = companies.findById(1).orElseGet(CompanyController::blank);
        String image = body.get("image");
        c.setLogo(image == null || image.isBlank() ? null : image);
        stamp(c);
        companies.save(c);
        return Map.of("saved", true);
    }

    private static CompanyProfile blank() {
        CompanyProfile c = new CompanyProfile();
        c.setId(1);
        return c;
    }

    private static void stamp(CompanyProfile c) {
        c.setUpdatedAt(LocalDateTime.now());
        UserContext.CurrentUser u = UserContext.get();
        c.setUpdatedBy(u == null ? "system" : u.username());
    }
}
