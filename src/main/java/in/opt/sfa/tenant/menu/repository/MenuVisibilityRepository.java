package in.opt.sfa.tenant.menu.repository;

import in.opt.sfa.tenant.menu.entity.MenuVisibility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MenuVisibilityRepository extends JpaRepository<MenuVisibility, Long> {
    List<MenuVisibility> findByEmpId(String empId);
    Optional<MenuVisibility> findByEmpIdAndMenuItemId(String empId, Long menuItemId);
}
