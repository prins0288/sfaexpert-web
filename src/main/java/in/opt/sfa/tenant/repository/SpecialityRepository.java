package in.opt.sfa.tenant.repository;

import in.opt.sfa.tenant.entity.Speciality;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpecialityRepository extends JpaRepository<Speciality, Long> {
    List<Speciality> findByStatusOrderBySpecialityNameAsc(String status);
}
