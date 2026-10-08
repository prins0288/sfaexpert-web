package in.opt.sfa.preference.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.opt.sfa.preference.dto.ColumnConfig;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * user_table_preferences.column_config (JSON) <-> List<ColumnConfig>.
 *
 * Uses a private Jackson 2 ObjectMapper (stable on the classpath, same as
 * AiHttp). A row that can't be parsed (hand-edited, truncated) reads as an
 * EMPTY list rather than throwing — the service then serves the registry
 * defaults, so a bad row can never break the screen.
 */
@Converter
public class ColumnConfigListConverter implements AttributeConverter<List<ColumnConfig>, String> {

    private static final Logger log = LoggerFactory.getLogger(ColumnConfigListConverter.class);
    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final TypeReference<List<ColumnConfig>> TYPE = new TypeReference<>() { };

    @Override
    public String convertToDatabaseColumn(List<ColumnConfig> columns) {
        try {
            return JSON.writeValueAsString(columns == null ? List.of() : columns);
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialise column config", e);
        }
    }

    @Override
    public List<ColumnConfig> convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            List<ColumnConfig> list = JSON.readValue(json, TYPE);
            return list == null ? new ArrayList<>() : new ArrayList<>(list);
        } catch (Exception e) {
            log.warn("Unreadable column_config, falling back to defaults: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
