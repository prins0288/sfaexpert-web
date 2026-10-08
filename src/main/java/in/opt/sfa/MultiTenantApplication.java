package in.opt.sfa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * We exclude DataSourceAutoConfiguration because we define BOTH datasources
 * (common + tenant-routing) ourselves. @EnableScheduling powers the
 * idle-tenant auto-close job. @EnableAsync enables @Async methods — the tenant
 * (a ThreadLocal) is carried onto async threads by the TaskDecorator in
 * AsyncConfig, so tenant repositories keep working inside @Async.
 * @EnableAspectJAutoProxy turns on PermissionAspect (@RequiresPermission) —
 * added by hand because spring-aop/aspectjweaver are included directly
 * instead of the spring-boot-starter-aop starter (see pom.xml).
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
@EnableScheduling
@EnableAsync
@EnableAspectJAutoProxy
public class MultiTenantApplication extends SpringBootServletInitializer {

    /** Bootstraps the app when deployed as a WAR to an external servlet container
     *  (Tomcat 11+ / Java 21). The main() below still lets the same artifact run
     *  standalone via `java -jar`. */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(MultiTenantApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(MultiTenantApplication.class, args);
    }
}
