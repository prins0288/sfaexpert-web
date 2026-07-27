package in.opt.sfa.tenant.repository;

import in.opt.sfa.tenant.entity.District;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DistrictRepository extends JpaRepository<District, Long> {
    List<District> findByStatusOrderByDistrictNameAsc(String status);
}
