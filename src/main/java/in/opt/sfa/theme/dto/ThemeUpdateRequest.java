package in.opt.sfa.theme.dto;

import java.util.Map;

/**
 * A partial appearance update. Every field is optional (null = leave unchanged).
 * `tokens` upserts colour/size overrides for the effective mode; a blank value
 * for a token clears that override.
 */
public record ThemeUpdateRequest(
        String navLayout,
        String mode,
        String preset,
        String fontScale,
        String density,
        Boolean sidebarCollapsed,
        Map<String, String> tokens
) {}
