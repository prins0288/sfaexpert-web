package in.opt.sfa.tenant.master.country.controller;

import in.opt.sfa.tenant.master.country.dto.CountryDto;
import in.opt.sfa.tenant.master.country.service.CountryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Country master — states link to a country; drives country-specific report rules (e.g. Nepal DCR date). Tenant-routed. */
@Tag(name = "Country Master", description = "Countries — soft-delete, bulk add")
@RestController
@RequestMapping("/api/master/country")
public class CountryController {

    private final CountryService service;

    public CountryController(CountryService service) {
        this.service = service;
    }

    @GetMapping
    public List<CountryDto> list() {
        return service.list();
    }

    @GetMapping("/{oid}")
    public CountryDto get(@PathVariable Long oid) {
        return service.get(oid);
    }

    @PostMapping
    public CountryDto save(@RequestBody CountryDto form) {
        return service.save(form);
    }

    /** Add Multiple — one insert per submitted row (blank / duplicate code skipped). */
    @PostMapping("/save-multiple")
    public Map<String, Object> saveMultiple(@RequestBody List<CountryDto> forms) {
        return service.saveMultiple(forms);
    }

    @PostMapping("/{oid}/status")
    public CountryDto status(@PathVariable Long oid, @RequestParam boolean status) {
        return service.updateStatus(oid, status);
    }
}
