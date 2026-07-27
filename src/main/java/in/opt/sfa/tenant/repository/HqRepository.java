package in.opt.sfa.tenant.repository;

import in.opt.sfa.tenant.entity.Hq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HqRepository extends JpaRepository<Hq, Long> {
    List<Hq> findByStatusOrderByHqNameAsc(String status);
    java.util.Optional<Hq> findFirstByHqCodeOrHqName(String hqCode, String hqName);
}
