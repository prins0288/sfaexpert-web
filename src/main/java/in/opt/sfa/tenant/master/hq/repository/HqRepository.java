package in.opt.sfa.tenant.master.hq.repository;

import in.opt.sfa.tenant.master.hq.entity.Hq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HqRepository extends JpaRepository<Hq, Long> {
    List<Hq> findAllByOrderByOidAsc();
    List<Hq> findByIsActiveOrderByHqNameAsc(Boolean isActive);
    Optional<Hq> findByHqCode(String hqCode);
    Optional<Hq> findFirstByHqCodeOrHqName(String hqCode, String hqName);
}
