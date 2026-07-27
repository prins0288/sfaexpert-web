package in.opt.sfa.tenant.repository;

import in.opt.sfa.tenant.entity.State;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StateRepository extends JpaRepository<State, Long> {
    List<State> findByStatusOrderByStateNameAsc(String status);
    java.util.Optional<State> findFirstByStateCodeOrStateName(String stateCode, String stateName);
}
