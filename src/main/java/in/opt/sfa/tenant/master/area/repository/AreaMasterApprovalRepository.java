package in.opt.sfa.tenant.master.area.repository;

import in.opt.sfa.tenant.master.area.entity.AreaMasterApproval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AreaMasterApprovalRepository extends JpaRepository<AreaMasterApproval, Long> {
    List<AreaMasterApproval> findByAreaIdOrderByActionAtAscIdAsc(Long areaId);
}
