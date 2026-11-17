package in.opt.sfa.tenant.master.salutation.repository;

import in.opt.sfa.tenant.master.salutation.entity.SalutationMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** TENANT-db repository. */
public interface SalutationMasterRepository extends JpaRepository<SalutationMaster, Long> {
    List<SalutationMaster> findAllByOrderByDisplayOrderAscSalutationAsc();
    List<SalutationMaster> findByActiveTrueOrderByDisplayOrderAscSalutationAsc();
    Optional<SalutationMaster> findBySalutation(String salutation);
}
