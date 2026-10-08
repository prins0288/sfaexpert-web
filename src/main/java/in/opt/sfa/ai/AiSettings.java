package in.opt.sfa.ai;

import in.opt.sfa.common.service.CompanySettingStore;
import in.opt.sfa.tenant.context.TenantContext;
import org.springframework.stereotype.Service;

/**
 * AI configuration, stored in company_setting_master (sfa_central):
 *   ai.provider  -> openai | gemini | claude
 *   ai.apiKey    -> the provider API key
 *   ai.model     -> default model id
 *
 * Resolution: the GLOBAL ("*") default applies to ALL tenants — EXCEPT any tenant
 * that has its OWN API key assigned, which then uses entirely its own
 * provider/key/model (the global default is not used for it). Decided by the API
 * key: a tenant with its own key is fully self-contained; a tenant without one
 * inherits the global default.
 */
@Service
public class AiSettings {

    public static final String K_PROVIDER = "ai.provider";
    public static final String K_API_KEY = "ai.apiKey";
    public static final String K_MODEL = "ai.model";

    private final CompanySettingStore store;

    public AiSettings(CompanySettingStore store) {
        this.store = store;
    }

    public record Config(String provider, String apiKey, String model) {
        public boolean isReady() {
            return provider != null && !provider.isBlank() && apiKey != null && !apiKey.isBlank();
        }
    }

    /**
     * Effective config for the current tenant. If the tenant has its OWN API key,
     * the whole config comes from the tenant (the global default does not apply to
     * it). Otherwise the GLOBAL default applies.
     */
    public Config current() {
        String companyCode = TenantContext.getCompanyCode();
        if (companyCode != null && !companyCode.isBlank()) {
            String ownKey = store.valueFor(companyCode, K_API_KEY, null);
            if (ownKey != null && !ownKey.isBlank()) {
                return new Config(
                        store.valueFor(companyCode, K_PROVIDER, null),
                        ownKey,
                        store.valueFor(companyCode, K_MODEL, null));
            }
        }
        // No own key -> inherit the app-wide global default.
        return new Config(
                store.valueFor(CompanySettingStore.GLOBAL, K_PROVIDER, null),
                store.valueFor(CompanySettingStore.GLOBAL, K_API_KEY, null),
                store.valueFor(CompanySettingStore.GLOBAL, K_MODEL, null));
    }
}
