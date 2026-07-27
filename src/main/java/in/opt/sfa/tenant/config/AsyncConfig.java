package in.opt.sfa.tenant.config;

import in.opt.sfa.tenant.context.TenantContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;

/**
 * Makes @Async tenant-aware.
 *
 * The active tenant is held in a ThreadLocal ({@link TenantContext}) that the
 * auth filter sets on the request thread. An @Async method runs on a DIFFERENT
 * (pool) thread, which would NOT see that ThreadLocal — so a tenant repository
 * inside @Async would fail with "No tenant bound to the current thread".
 *
 * This TaskDecorator captures the tenant on the submitting thread and re-binds
 * it on the async thread (clearing it afterwards so pooled threads never leak a
 * tenant). Spring Boot's auto-configured task executor picks up a single
 * TaskDecorator bean automatically, so @Async just works.
 */
@Configuration
public class AsyncConfig {

    @Bean
    public TaskDecorator tenantAwareTaskDecorator() {
        return runnable -> {
            String tenantId = TenantContext.getTenantId();   // captured on the caller thread
            return () -> {
                if (tenantId != null) {
                    TenantContext.setTenantId(tenantId);
                }
                try {
                    runnable.run();
                } finally {
                    TenantContext.clear();
                }
            };
        };
    }
}
