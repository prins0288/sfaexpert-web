package in.opt.sfa.tenant.master.meetingtype.repository;

import in.opt.sfa.tenant.master.meetingtype.entity.MeetingTypeMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** TENANT-db repository (auto-routed to the active tenant via package location). */
public interface MeetingTypeMasterRepository extends JpaRepository<MeetingTypeMaster, Long> {

    List<MeetingTypeMaster> findAllByOrderByDisplayOrderAscIdAsc();

    List<MeetingTypeMaster> findByIsActiveOrderByDisplayOrderAscMeetingTypeNameAsc(Boolean isActive);

    Optional<MeetingTypeMaster> findByMeetingTypeCode(String meetingTypeCode);

    Optional<MeetingTypeMaster> findByMeetingTypeName(String meetingTypeName);
}
