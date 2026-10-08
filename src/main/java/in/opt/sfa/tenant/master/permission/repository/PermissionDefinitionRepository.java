package in.opt.sfa.tenant.master.permission.repository;

import in.opt.sfa.tenant.master.permission.entity.PermissionDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** TENANT-db repository (auto-routed to the active tenant via package location). */
public interface PermissionDefinitionRepository extends JpaRepository<PermissionDefinition, Long> {

    List<PermissionDefinition> findAllByOrderByModuleAscPermissionCodeAsc();

    Optional<PermissionDefinition> findByPermissionCode(String permissionCode);

    List<PermissionDefinition> findByStatusTrueOrderByModuleAscPermissionCodeAsc();
}
