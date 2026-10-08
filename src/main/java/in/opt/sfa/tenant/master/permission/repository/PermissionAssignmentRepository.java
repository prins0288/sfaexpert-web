package in.opt.sfa.tenant.master.permission.repository;

import in.opt.sfa.tenant.master.permission.entity.PermissionAssignment;
import in.opt.sfa.tenant.master.permission.entity.PermissionTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** TENANT-db repository (auto-routed to the active tenant via package location). */
public interface PermissionAssignmentRepository extends JpaRepository<PermissionAssignment, Long> {

    List<PermissionAssignment> findAllByOrderByIdAsc();

    List<PermissionAssignment> findByTargetTypeAndTargetValueOrderByPermissionCodeAsc(
            PermissionTargetType targetType, String targetValue);

    Optional<PermissionAssignment> findByTargetTypeAndTargetValueAndPermissionCode(
            PermissionTargetType targetType, String targetValue, String permissionCode);
}
