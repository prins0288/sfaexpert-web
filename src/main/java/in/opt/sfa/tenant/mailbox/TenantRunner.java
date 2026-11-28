package in.opt.sfa.tenant.mailbox;

import in.opt.sfa.common.entity.TenantConfig;
import in.opt.sfa.common.repository.TenantConfigRepository;
import in.opt.sfa.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

/** Runs a background job once for every tenant: binds the tenant to the thread (so tenant repositories route to its database) and always clears it. One tenant failing never stops the others. */
@Component
public class TenantRunner {
    private static final Logger log = LoggerFactory.getLogger(TenantRunner.class);
    private final TenantConfigRepository tenants;

    public TenantRunner(TenantConfigRepository tenants) { this.tenants = tenants; }

    public void forEachTenant(Consumer<String> job) {
        List<TenantConfig> all;
        try { all = tenants.findAll(); } catch (RuntimeException e) { log.warn("Mail jobs: the tenant list cannot be read: {}", e.getMessage()); return; }
        for (TenantConfig t : all) {
            String code = t.getCompanyCode(); if (code == null) continue;
            try { TenantContext.setCompanyCode(code); job.accept(code); }
            catch (Exception e) { log.warn("Mail job failed for tenant {}: {}", code, e.getMessage()); }
            finally { TenantContext.clear(); }
        }
    }
}
