package in.opt.sfa.auth;

import in.opt.sfa.auth.dto.LoginRequest;
import in.opt.sfa.auth.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Authentication", description = "Log in, silently refresh the access token, and log out")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record RefreshRequest(String refreshToken) {}
    public record LogoutRequest(String refreshToken) {}

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** Exchange a still-valid refresh token for a new access token, no credentials needed. */
    @PostMapping("/refresh")
    public LoginResponse refresh(@RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    /** Sign-out: revokes the refresh token AND denylists the current access token
     *  (sent as the Bearer header) so both stop working immediately. */
    @PostMapping("/logout")
    public void logout(@RequestHeader(value = "Authorization", required = false) String authHeader,
                       @RequestBody LogoutRequest request) {
        String accessToken = (authHeader != null && authHeader.startsWith("Bearer ")) ? authHeader.substring(7) : null;
        authService.logout(request.refreshToken(), accessToken);
    }
}
