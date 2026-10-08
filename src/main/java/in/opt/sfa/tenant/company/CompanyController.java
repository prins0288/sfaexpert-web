package in.opt.sfa.tenant.company;

import in.opt.sfa.common.entity.CompanyDetails;
import in.opt.sfa.common.repository.CompanyDetailsRepository;
import in.opt.sfa.security.UserContext;
import in.opt.sfa.tenant.context.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Per-tenant company profile + logo (one row per tenant in the COMMON db,
 * company_details, keyed by company_code).
 */
@RestController
@RequestMapping("/api/company")
@Tag(name = "Company Profile", description = "Per-tenant company details and logo")
public class CompanyController {

    private final CompanyDetailsRepository companies;

    public CompanyController(CompanyDetailsRepository companies) {
        this.companies = companies;
    }

    @GetMapping
    @Operation(summary = "Get the company profile")
    @Transactional(transactionManager = "commonTransactionManager", readOnly = true)
    public CompanyDetails get() {
        return current();
    }

    @PostMapping
    @Operation(summary = "Update the company profile (name, address, contact)")
    @Transactional(transactionManager = "commonTransactionManager")
    public CompanyDetails update(@RequestBody CompanyDetails form) {
        CompanyDetails c = current();
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
    @Transactional(transactionManager = "commonTransactionManager")
    public Map<String, Object> logo(@RequestBody Map<String, String> body) {
        CompanyDetails c = current();
        String image = body.get("image");
        c.setLogo(image == null || image.isBlank() ? null : image);
        stamp(c);
        companies.save(c);
        return Map.of("saved", true);
    }

    /** The current tenant's row, or a blank one stamped with the tenant id. */
    private CompanyDetails current() {
        String companyCode = TenantContext.getCompanyCode();
        return companies.findById(companyCode).orElseGet(() -> {
            CompanyDetails c = new CompanyDetails();
            c.setCompanyCode(companyCode);
            return c;
        });
    }

    private static void stamp(CompanyDetails c) {
        c.setUpdatedAt(LocalDateTime.now());
        UserContext.CurrentUser u = UserContext.get();
        c.setUpdatedBy(u == null ? "system" : u.username());
    }
}
