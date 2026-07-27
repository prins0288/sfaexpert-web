package in.opt.sfa.tenant.repository;

import in.opt.sfa.tenant.entity.Area;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AreaRepository extends JpaRepository<Area, Long> {
    List<Area> findAllByOrderByOidAsc();
    List<Area> findByStatusOrderByAreaNameAsc(String status);
    Optional<Area> findByAreaCode(String areaCode);
    Optional<Area> findFirstByAreaCodeOrAreaName(String areaCode, String areaName);
}
