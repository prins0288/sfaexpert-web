package in.opt.sfa.tenant.master.country.service;

import in.opt.sfa.common.util.Strings;
import in.opt.sfa.tenant.master.country.dto.CountryDto;
import in.opt.sfa.tenant.master.country.entity.Country;
import in.opt.sfa.tenant.master.country.mapper.CountryMapper;
import in.opt.sfa.tenant.master.country.repository.CountryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CountryService {

    private final CountryRepository countries;
    private final CountryMapper mapper;

    public CountryService(CountryRepository countries, CountryMapper mapper) {
        this.countries = countries;
        this.mapper = mapper;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<CountryDto> list() {
        return countries.findAllByOrderByOidAsc().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public CountryDto get(Long oid) {
        return mapper.toDto(find(oid));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public CountryDto save(CountryDto form) {
        if (form.getStatus() == null) form.setStatus(Boolean.TRUE);
        return mapper.toDto(countries.save(mapper.toEntity(form)));
    }

    /** Add Multiple — one insert per submitted row (blank / duplicate code skipped). */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveMultiple(List<CountryDto> forms) {
        int saved = 0, skipped = 0;
        for (CountryDto f : forms) {
            if (Strings.isBlank(f.getCountryCode()) || Strings.isBlank(f.getCountryName())
                    || countries.findByCountryCode(f.getCountryCode().trim()).isPresent()) {
                skipped++;
                continue;
            }
            f.setOid(null);
            f.setCountryCode(f.getCountryCode().trim());
            f.setCountryName(f.getCountryName().trim());
            f.setStatus(Boolean.TRUE);
            countries.save(mapper.toEntity(f));
            saved++;
        }
        return Map.of("saved", saved, "skipped", skipped);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public CountryDto updateStatus(Long oid, boolean status) {
        Country c = find(oid);
        c.setStatus(status);
        return mapper.toDto(countries.save(c));
    }

    private Country find(Long oid) {
        return countries.findById(oid).orElseThrow(() -> new IllegalStateException("Country not found: " + oid));
    }
}
