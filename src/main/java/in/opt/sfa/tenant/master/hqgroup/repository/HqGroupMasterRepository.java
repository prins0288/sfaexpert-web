package in.opt.sfa.tenant.master.hqgroup.repository;

import in.opt.sfa.tenant.master.hqgroup.entity.HqGroupMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** TENANT-db repository (auto-routed to the active tenant via package location). */
public interface HqGroupMasterRepository extends JpaRepository<HqGroupMaster, Long> {

    List<HqGroupMaster> findAllByOrderByDisplayOrderAscIdAsc();

    List<HqGroupMaster> findByIsActiveOrderByDisplayOrderAscGroupNameAsc(Boolean isActive);

    Optional<HqGroupMaster> findByGroupCode(String groupCode);

    Optional<HqGroupMaster> findByGroupName(String groupName);
}
