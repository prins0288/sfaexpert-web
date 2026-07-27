package in.opt.sfa.tenant.repository;

import in.opt.sfa.tenant.entity.ClientType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientTypeRepository extends JpaRepository<ClientType, Long> {
    List<ClientType> findAllByOrderByOidAsc();
    List<ClientType> findByStatusOrderByTypeNameAsc(String status);
    Optional<ClientType> findByTypeCode(String typeCode);
    Optional<ClientType> findFirstByTypeCodeOrTypeName(String typeCode, String typeName);
}
