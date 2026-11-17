package in.opt.sfa.tenant.master.empdetail.repository;

import in.opt.sfa.tenant.master.empdetail.entity.EmpDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** TENANT-db repository. */
public interface EmpDocumentRepository extends JpaRepository<EmpDocument, Long> {
    List<EmpDocument> findByEmpIdAndActiveTrueOrderByUploadedAtDescIdDesc(String empId);
    Optional<EmpDocument> findByIdAndEmpId(Long id, String empId);
}
