package in.opt.sfa.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;

/**
 * The ONE response envelope for every API endpoint — success and error alike.
 *
 *   { "success": true, "status": 200, "message": "...", "data": {...}, "timestamp": "..." }
 *   { "success": false, "status": 400, "message": "...", "errors": {...}, "timestamp": "..." }
 *
 * Controllers never return a raw List/Map/DTO; they return one of the factory
 * methods here, which already hand back a {@code ResponseEntity<ResponseFormat<T>>}
 * with the right HTTP status:
 *
 *   return ResponseFormat.ok(service.list());
 *   return ResponseFormat.ok("Saved", service.save(dto));
 *   return ResponseFormat.created("Created", saved);
 *
 * Errors are produced centrally by GlobalExceptionHandler via {@link #error}.
 * {@code null} data / errors are omitted from the JSON (see @JsonInclude).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "success", "status", "message", "data", "errors", "timestamp" })
public class ResponseFormat<T> {

    private final boolean success;
    private final int status;
    private final String message;
    private final T data;
    private final Object errors;      // field/validation errors or details — null on success
    private final String timestamp;

    private ResponseFormat(boolean success, int status, String message, T data, Object errors) {
        this.success = success;
        this.status = status;
        this.message = message;
        this.data = data;
        this.errors = errors;
        this.timestamp = OffsetDateTime.now().toString();
    }

    // ---- body builders (the advice / handler use these to make the object) --------
    public static <T> ResponseFormat<T> body(boolean success, int status, String message, T data, Object errors) {
        return new ResponseFormat<>(success, status, message, data, errors);
    }

    /** Wrap an already-produced value as a success body (used by the auto-wrap advice). */
    public static <T> ResponseFormat<T> success(int status, String message, T data) {
        return new ResponseFormat<>(true, status, message, data, null);
    }

    // ---- success factories (controllers) ------------------------------------------
    public static <T> ResponseEntity<ResponseFormat<T>> ok(T data) {
        return ok("Success", data);
    }

    public static <T> ResponseEntity<ResponseFormat<T>> ok(String message, T data) {
        return ResponseEntity.ok(new ResponseFormat<>(true, 200, message, data, null));
    }

    public static <T> ResponseEntity<ResponseFormat<T>> created(String message, T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseFormat<>(true, HttpStatus.CREATED.value(), message, data, null));
    }

    /** Success with no payload (e.g. after a delete/status change). */
    public static ResponseEntity<ResponseFormat<Void>> message(String message) {
        return ResponseEntity.ok(new ResponseFormat<>(true, 200, message, null, null));
    }

    // ---- error factories (GlobalExceptionHandler) ---------------------------------
    public static <T> ResponseEntity<ResponseFormat<T>> error(HttpStatus status, String message) {
        return error(status, message, null);
    }

    public static <T> ResponseEntity<ResponseFormat<T>> error(HttpStatus status, String message, Object errors) {
        return ResponseEntity.status(status)
                .body(new ResponseFormat<>(false, status.value(), message, null, errors));
    }

    // ---- getters (Jackson) --------------------------------------------------------
    public boolean isSuccess() { return success; }
    public int getStatus() { return status; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public Object getErrors() { return errors; }
    public String getTimestamp() { return timestamp; }
}
