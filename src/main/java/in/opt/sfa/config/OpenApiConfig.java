package in.opt.sfa.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI for the mobile REST API.
 *
 *   UI:   /swagger-ui.html
 *   Spec: /v3/api-docs
 *
 * Both live outside /api/, so the TenantAuthFilter treats them as public and
 * they load without a token. The API itself is JWT-secured: the mobile client
 * (or a tester in Swagger UI) calls POST /api/auth/login, then presses
 * "Authorize" and pastes the returned token. The JWT carries the tenant, so
 * each request auto-routes to that tenant's database.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearer-jwt";

    @Bean
    public OpenAPI starSfaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("StarSFA Mobile API")
                        .version("1.0.0")
                        .description("REST API for the StarSFA mobile app. Multi-tenant and JWT-secured: "
                                + "log in via POST /api/auth/login, click Authorize, and paste the token "
                                + "(the tenant travels inside the JWT)."))
                .addServersItem(new Server().url("/").description("This server"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the token returned by /api/auth/login "
                                        + "(Swagger adds the 'Bearer ' prefix for you).")));
    }
}
