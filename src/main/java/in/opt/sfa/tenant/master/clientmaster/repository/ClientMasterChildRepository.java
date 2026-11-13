package in.opt.sfa.tenant.master.clientmaster.repository;

import in.opt.sfa.tenant.master.clientmaster.entity.ClientMasterChild;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** TENANT-db repository. */
public interface ClientMasterChildRepository extends JpaRepository<ClientMasterChild, Long> {
    Optional<ClientMasterChild> findByClientId(Long clientId);
}
