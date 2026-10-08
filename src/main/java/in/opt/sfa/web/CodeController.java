package in.opt.sfa.web;

import in.opt.sfa.common.response.ResponseFormat;
import in.opt.sfa.common.util.CodeGeneratorService;
import in.opt.sfa.security.Authz;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Business-code generation. POST (not GET) because each call consumes a unique
 * running number from the tenant's sequence. Gaps are fine (if a code is reserved
 * but the record is never saved); uniqueness is what matters.
 */
@Tag(name = "Code Generator", description = "Generate unique business codes (emp_code, …) per tenant")
@RestController
@RequestMapping("/api/code")
public class CodeController {

    private final CodeGeneratorService generator;

    public CodeController(CodeGeneratorService generator) {
        this.generator = generator;
    }

    /** Reserve the next unique employee code for the current tenant (e.g. AWT201). */
    @PostMapping("/emp-code")
    public ResponseEntity<ResponseFormat<Map<String, String>>> nextEmpCode() {
        return ResponseFormat.ok(Map.of("empCode", generator.nextEmpCode()));
    }

    /** Current emp_code format for this tenant + a live preview of the next code. */
    @GetMapping("/emp-code/config")
    public ResponseEntity<ResponseFormat<Map<String, Object>>> empCodeConfig() {
        return ResponseFormat.ok(generator.empCodeConfig());
    }

    /**
     * Customise the emp_code format for this tenant (prefix / width / suffix).
     * Only a super-admin / admin may change it; the running number is untouched.
     */
    @PostMapping("/emp-code/config")
    public ResponseEntity<ResponseFormat<Map<String, Object>>> saveEmpCodeConfig(@RequestBody Map<String, Object> body) {
        Authz.requireRole("SUPER_ADMIN", "ADMIN");
        String prefix = str(body.get("prefix"));
        String suffix = str(body.get("suffix"));
        Integer width = toInt(body.get("width"));
        return ResponseFormat.ok(generator.saveEmpCodeConfig(prefix, width, suffix));
    }

    private static String str(Object o) { return o == null ? "" : o.toString(); }

    private static Integer toInt(Object o) {
        if (o == null) return 0;
        try { return (int) Double.parseDouble(o.toString().trim()); } catch (Exception e) { return 0; }
    }
}
