package in.opt.sfa.tenant.master.clienttype.repository;

import in.opt.sfa.tenant.master.clienttype.entity.ClientType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientTypeRepository extends JpaRepository<ClientType, Long> {
    List<ClientType> findAllByOrderByOidAsc();
    List<ClientType> findByIsActiveOrderByTypeNameAsc(Boolean isActive);
    Optional<ClientType> findByTypeCode(String typeCode);
    Optional<ClientType> findFirstByTypeCodeOrTypeName(String typeCode, String typeName);
}
