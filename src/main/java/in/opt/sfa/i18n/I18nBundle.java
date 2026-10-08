package in.opt.sfa.i18n;

import java.util.List;
import java.util.Map;

/**
 * The payload the browser downloads once and caches: the resolved label map for
 * one language plus enough metadata to (a) render the language switcher and
 * (b) know when its cache is stale.
 *
 * version is a content hash of {@code labels} — it changes whenever a shipped
 * default OR a tenant label_master override changes, so the client can keep the
 * bundle in localStorage and only re-download when version differs.
 */
public record I18nBundle(
        String lang,
        String version,
        List<LangInfo> available,
        Map<String, String> labels
) {
    /** One selectable language for the switcher (flag = an emoji flag). */
    public record LangInfo(String code, String label, String flag) {}
}
