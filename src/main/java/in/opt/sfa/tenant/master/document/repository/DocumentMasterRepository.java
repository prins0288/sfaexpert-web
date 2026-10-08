package in.opt.sfa.tenant.master.document.repository;

import in.opt.sfa.tenant.master.document.entity.DocumentMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** TENANT-db repository (auto-routed to the active tenant via package location). */
public interface DocumentMasterRepository extends JpaRepository<DocumentMaster, Long> {

    List<DocumentMaster> findAllByOrderByDisplayOrderAscIdAsc();

    List<DocumentMaster> findByIsActiveOrderByDisplayOrderAscDocumentNameAsc(Boolean isActive);

    Optional<DocumentMaster> findByDocumentCode(String documentCode);

    Optional<DocumentMaster> findByDocumentName(String documentName);
}
