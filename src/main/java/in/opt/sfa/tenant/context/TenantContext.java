package in.opt.sfa.tenant.context;

/**
 * Holds the current tenant id for the running thread (request). The auth filter
 * sets it from the JWT at the start of every request and clears it afterwards.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(String tenantId) {
        CURRENT.set(tenantId);
    }

    public static String getTenantId() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
