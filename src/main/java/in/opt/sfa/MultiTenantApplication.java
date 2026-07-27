package in.opt.sfa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * We exclude DataSourceAutoConfiguration because we define BOTH datasources
 * (common + tenant-routing) ourselves. @EnableScheduling powers the
 * idle-tenant auto-close job. @EnableAsync enables @Async methods — the tenant
 * (a ThreadLocal) is carried onto async threads by the TaskDecorator in
 * AsyncConfig, so tenant repositories keep working inside @Async.
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
@EnableScheduling
@EnableAsync
public class MultiTenantApplication {

    public static void main(String[] args) {
        SpringApplication.run(MultiTenantApplication.class, args);
    }
}
