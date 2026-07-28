package in.opt.sfa.theme.repository;

import in.opt.sfa.theme.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** COMMON-db repository (bound to the common EMF via package location). */
public interface UserPreferenceRepository extends JpaRepository<UserPreference, String> {
    Optional<UserPreference> findByUsername(String username);
}
