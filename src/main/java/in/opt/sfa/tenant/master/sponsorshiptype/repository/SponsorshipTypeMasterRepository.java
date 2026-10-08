package in.opt.sfa.tenant.master.sponsorshiptype.repository;

import in.opt.sfa.tenant.master.sponsorshiptype.entity.SponsorshipTypeMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** TENANT-db repository (auto-routed to the active tenant via package location). */
public interface SponsorshipTypeMasterRepository extends JpaRepository<SponsorshipTypeMaster, Long> {

    List<SponsorshipTypeMaster> findAllByOrderByDisplayOrderAscIdAsc();

    List<SponsorshipTypeMaster> findByIsActiveOrderByDisplayOrderAscSponsorshipTypeNameAsc(Boolean isActive);

    Optional<SponsorshipTypeMaster> findBySponsorshipTypeCode(String sponsorshipTypeCode);

    Optional<SponsorshipTypeMaster> findBySponsorshipTypeName(String sponsorshipTypeName);
}
