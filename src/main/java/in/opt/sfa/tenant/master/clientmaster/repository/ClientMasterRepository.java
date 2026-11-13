package in.opt.sfa.tenant.master.clientmaster.repository;

import in.opt.sfa.tenant.master.clientmaster.entity.ClientMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** TENANT-db repository (auto-routed to the active tenant via package location). */
public interface ClientMasterRepository extends JpaRepository<ClientMaster, Long> {
    List<ClientMaster> findAllByOrderByClientNameAsc();
    Optional<ClientMaster> findByClientCode(String clientCode);

    /**
     * Highest number after the prefix among codes like DR00001 (null = none yet).
     * A locking read: it sees the latest committed codes (not the transaction's
     * snapshot) and holds the rows until commit, so two saves never take the same code.
     */
    @Query(nativeQuery = true, value = "SELECT MAX(CAST(SUBSTRING(client_code, :start) AS UNSIGNED)) FROM client_master WHERE client_code REGEXP :pattern FOR UPDATE")
    Long maxCodeNumber(@Param("start") int start, @Param("pattern") String pattern);
}
