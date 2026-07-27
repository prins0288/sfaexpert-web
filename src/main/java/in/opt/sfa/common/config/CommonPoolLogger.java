package in.opt.sfa.common.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logs the COMMON HikariCP pool's open time in the same "yyyy-MM-dd HH:mm:ss"
 * format used for the tenant pools. The common pool is a Spring-managed
 * HikariDataSource that Hikari starts lazily on the first connection; here we
 * force that first connection once the app is ready and stamp the time.
 */
@Component
@Slf4j
public class CommonPoolLogger {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DataSource commonDataSource;

    public CommonPoolLogger(@Qualifier("common") DataSource commonDataSource) {
        this.commonDataSource = commonDataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logCommonPoolOpen() {
        String poolName = commonDataSource instanceof HikariDataSource h ? h.getPoolName() : "common";
        try (Connection ignored = commonDataSource.getConnection()) {
            log.info("Pool '{}' (COMMON) OPENED at {}", poolName, LocalDateTime.now().format(TS));
        } catch (Exception e) {
            log.warn("Common pool '{}' could not open: {}", poolName, e.getMessage());
        }
    }
}
