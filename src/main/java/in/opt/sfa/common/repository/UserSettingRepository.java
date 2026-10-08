package in.opt.sfa.common.repository;

import in.opt.sfa.common.entity.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {
    Optional<UserSetting> findByUsernameAndSettingKey(String username, String settingKey);
}
