package in.opt.sfa.tenant.repository;

import in.opt.sfa.tenant.entity.LabelMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LabelMasterRepository extends JpaRepository<LabelMaster, Long> {
    /** Active override rows for one language (this tenant). */
    List<LabelMaster> findByLangAndStatus(String lang, String status);
}
