package in.opt.sfa.common.repository;

import in.opt.sfa.common.entity.AiChatLog;
import org.springframework.data.jpa.repository.JpaRepository;

/** COMMON-db repository for the AI chat log (ai_chat). */
public interface AiChatLogRepository extends JpaRepository<AiChatLog, Long> {
}
