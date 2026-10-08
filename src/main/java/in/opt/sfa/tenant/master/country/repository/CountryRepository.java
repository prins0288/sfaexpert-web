package in.opt.sfa.tenant.master.country.repository;

import in.opt.sfa.tenant.master.country.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CountryRepository extends JpaRepository<Country, Long> {
    List<Country> findAllByOrderByOidAsc();
    List<Country> findByStatusOrderByCountryNameAsc(Boolean status);
    Optional<Country> findByCountryCode(String countryCode);
    Optional<Country> findFirstByCountryCodeOrCountryName(String countryCode, String countryName);
}
