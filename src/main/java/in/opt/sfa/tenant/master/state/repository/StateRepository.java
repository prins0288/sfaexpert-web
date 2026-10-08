package in.opt.sfa.tenant.master.state.repository;

import in.opt.sfa.tenant.master.state.entity.State;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StateRepository extends JpaRepository<State, Long> {
    List<State> findAllByOrderByOidAsc();
    List<State> findByStatusOrderByStateNameAsc(String status);
    Optional<State> findByStateCode(String stateCode);
    Optional<State> findFirstByStateCodeOrStateName(String stateCode, String stateName);
}
