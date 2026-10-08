package in.opt.sfa.tenant.master.activitytype.repository;

import in.opt.sfa.tenant.master.activitytype.entity.ActivityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityTypeRepository extends JpaRepository<ActivityType, Long> {
    List<ActivityType> findAllByOrderByOidAsc();
    List<ActivityType> findByIsActiveOrderByTypeNameAsc(Boolean isActive);
    Optional<ActivityType> findByTypeCode(String typeCode);
    Optional<ActivityType> findFirstByTypeCodeOrTypeName(String typeCode, String typeName);
}
