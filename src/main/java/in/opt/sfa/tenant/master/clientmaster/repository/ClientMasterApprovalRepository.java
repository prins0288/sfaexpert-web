package in.opt.sfa.tenant.master.clientmaster.repository;

import in.opt.sfa.tenant.master.clientmaster.entity.ClientMasterApproval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** TENANT-db repository. */
public interface ClientMasterApprovalRepository extends JpaRepository<ClientMasterApproval, Long> {
    List<ClientMasterApproval> findByClientIdOrderByActionAtAscIdAsc(Long clientId);
}
