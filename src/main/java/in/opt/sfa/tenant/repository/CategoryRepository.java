package in.opt.sfa.tenant.repository;

import in.opt.sfa.tenant.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByStatusOrderByCategoryNameAsc(String status);
}
