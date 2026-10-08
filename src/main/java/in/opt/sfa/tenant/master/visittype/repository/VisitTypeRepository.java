package in.opt.sfa.tenant.master.visittype.repository;

import in.opt.sfa.tenant.master.visittype.entity.VisitType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VisitTypeRepository extends JpaRepository<VisitType, Long> {
    List<VisitType> findAllByOrderByOidAsc();
    List<VisitType> findByIsActiveOrderByTypeNameAsc(Boolean isActive);
    Optional<VisitType> findByTypeCode(String typeCode);
    Optional<VisitType> findFirstByTypeCodeOrTypeName(String typeCode, String typeName);
}
