package in.opt.sfa.tenant.master.bank.repository;

import in.opt.sfa.tenant.master.bank.entity.BankMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** TENANT-db repository (auto-routed to the active tenant via package location). */
public interface BankMasterRepository extends JpaRepository<BankMaster, Long> {

    List<BankMaster> findAllByOrderByDisplayOrderAscIdAsc();

    List<BankMaster> findByIsActiveOrderByDisplayOrderAscBankNameAsc(Boolean isActive);

    Optional<BankMaster> findByBankCode(String bankCode);

    Optional<BankMaster> findByBankName(String bankName);
}
