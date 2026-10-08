package in.opt.sfa.tenant.menu.repository;

import in.opt.sfa.tenant.menu.entity.MenuAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** TENANT-db repository (auto-routed to the active tenant via package location). */
public interface MenuAuditLogRepository extends JpaRepository<MenuAuditLog, Long> {
    List<MenuAuditLog> findAllByOrderByChangedAtDesc();
}
