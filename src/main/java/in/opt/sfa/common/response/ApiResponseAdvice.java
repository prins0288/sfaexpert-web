package in.opt.sfa.common.response;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Guarantees every JSON API response is a {@link ResponseFormat} envelope.
 *
 * Controllers may return the envelope explicitly (ResponseFormat.ok(...)) for a
 * custom message/status; anything else that a controller returns as JSON (a bare
 * List/Map/DTO) is wrapped here automatically as a success envelope, so the API
 * shape is consistent app-wide with no per-controller boilerplate and no endpoint
 * left un-enveloped during a migration.
 *
 * Deliberately NOT touched:
 *   - responses already wrapped in ResponseFormat (incl. the error handler's),
 *   - non-JSON bodies — file downloads (byte[]/Resource), plain String/HTML — since
 *     supports() only matches the Jackson converter,
 *   - springdoc's own /v3/api-docs & swagger JSON, and the /error page,
 *   - error-status responses (>= 400), which the exception handler already envelopes.
 */
@RestControllerAdvice
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // Only JSON (Jackson) bodies. byte[]/String/Resource use other converters
        // and never reach here, so file downloads and HTML are safe.
        String name = converterType.getSimpleName();
        return name.contains("Jackson") || name.contains("Json");
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> converterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof ResponseFormat) return body;   // already enveloped

        String path = request.getURI().getPath();
        if (path.contains("/v3/api-docs") || path.contains("/swagger") || path.contains("/error")) {
            return body;
        }

        int status = (response instanceof ServletServerHttpResponse ssr)
                ? ssr.getServletResponse().getStatus() : 200;
        if (status >= 400) return body;   // errors are enveloped by GlobalExceptionHandler

        return ResponseFormat.success(status, "Success", body);
    }
}
