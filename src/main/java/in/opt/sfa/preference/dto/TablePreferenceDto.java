package in.opt.sfa.preference.dto;

import java.util.List;

/**
 * A screen's effective column layout for the current user's company.
 * {@code customized=false} means no saved row — these are the registry defaults.
 */
public record TablePreferenceDto(String screenKey, boolean customized, List<ColumnConfig> columns) { }
