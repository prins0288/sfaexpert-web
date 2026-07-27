package in.opt.sfa.auth.dto;

public record LoginResponse(
        String token,
        String tenantId,
        String role,
        String tokenType) {

    public LoginResponse(String token, String tenantId, String role) {
        this(token, tenantId, role, "Bearer");
    }
}
