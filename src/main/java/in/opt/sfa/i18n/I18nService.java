package in.opt.sfa.i18n;

import in.opt.sfa.common.entity.UserSetting;
import in.opt.sfa.common.repository.UserSettingRepository;
import in.opt.sfa.tenant.entity.LabelMaster;
import in.opt.sfa.tenant.repository.LabelMasterRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Language / labels service.
 *
 * A label is resolved in three layers, lowest to highest precedence:
 *   1. lang_en.properties          (the base — GUARANTEES every key exists, so
 *                                    a missing translation shows English, never
 *                                    a "?" or the raw key)
 *   2. lang_{code}.properties      (the chosen language's shipped values)
 *   3. label_master (tenant db)    (this company's own overrides, status='Y')
 *
 * Everything is UTF-8 end to end: the .properties files are read with a UTF-8
 * reader (so Hindi etc. need no \\uXXXX escapes and never become "?"), and the
 * JSON response is UTF-8 by Spring default.
 *
 * The per-user chosen language lives in the COMMON db (user_setting, key
 * "lang"); the label bundle itself is per tenant (because of label_master), so
 * the two live in different databases and use different transaction managers.
 */
@Service
public class I18nService {

    public static final String DEFAULT_LANG = "en";
    private static final String USER_LANG_KEY = "lang";
    private static final String BASE = "i18n/lang_";
    private static final Pattern LANG_FILE = Pattern.compile("lang_([a-z]{2,3})\\.properties$");

    /** Display names for the switcher. A code with no entry falls back to the code itself. */
    private static final Map<String, String> DISPLAY = Map.of(
            "en", "English",
            "hi", "हिंदी",
            "ne", "नेपाली",
            "gu", "ગુજરાતી"
    );

    /** Emoji flag per language for the switcher (falls back to a globe). */
    private static final Map<String, String> FLAG = Map.of(
            "en", "🇬🇧",
            "hi", "🇮🇳",
            "ne", "🇳🇵",
            "gu", "🇮🇳"
    );

    private final LabelMasterRepository labels;      // tenant db
    private final UserSettingRepository userSettings; // common db

    public I18nService(LabelMasterRepository labels, UserSettingRepository userSettings) {
        this.labels = labels;
        this.userSettings = userSettings;
    }

    // ---- bundle ------------------------------------------------------------

    /** Fully-resolved bundle for one language for the CURRENT tenant. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public I18nBundle bundle(String requested) {
        String lang = normalize(requested);

        // 1. English base guarantees full key coverage (no "?" ever).
        Map<String, String> merged = new LinkedHashMap<>(loadProps(DEFAULT_LANG));
        // 2. overlay the chosen language's shipped values.
        if (!DEFAULT_LANG.equals(lang)) merged.putAll(loadProps(lang));
        // 3. overlay this tenant's own overrides.
        for (LabelMaster row : labels.findByLangAndStatus(lang, "Y")) {
            if (row.getLabelKey() != null && row.getLabelValue() != null) {
                merged.put(row.getLabelKey(), row.getLabelValue());
            }
        }

        return new I18nBundle(lang, version(lang, merged), available(), merged);
    }

    /** The languages a switcher should offer — every lang_*.properties on the classpath. */
    public List<I18nBundle.LangInfo> available() {
        TreeSet<String> codes = new TreeSet<>();
        codes.add(DEFAULT_LANG);
        try {
            Resource[] found = new PathMatchingResourcePatternResolver()
                    .getResources("classpath*:i18n/lang_*.properties");
            for (Resource r : found) {
                String name = r.getFilename();
                if (name == null) continue;
                Matcher m = LANG_FILE.matcher(name);
                if (m.find()) codes.add(m.group(1));
            }
        } catch (Exception ignored) { /* fall back to just the default */ }

        List<I18nBundle.LangInfo> out = new ArrayList<>();
        for (String c : codes) out.add(new I18nBundle.LangInfo(c, DISPLAY.getOrDefault(c, c), FLAG.getOrDefault(c, "🌐")));
        return out;
    }

    // ---- per-user language choice (common db) ------------------------------

    @Transactional(transactionManager = "commonTransactionManager", readOnly = true)
    public String getUserLang(String username) {
        return userSettings.findByUsernameAndSettingKey(username, USER_LANG_KEY)
                .map(UserSetting::getSettingValue)
                .map(this::normalize)
                .orElse(DEFAULT_LANG);
    }

    @Transactional(transactionManager = "commonTransactionManager")
    public String setUserLang(String username, String lang) {
        String norm = normalize(lang);
        UserSetting row = userSettings.findByUsernameAndSettingKey(username, USER_LANG_KEY)
                .orElseGet(UserSetting::new);
        row.setUsername(username);
        row.setSettingKey(USER_LANG_KEY);
        row.setSettingValue(norm);
        row.setUpdatedAt(LocalDateTime.now());
        userSettings.save(row);
        return norm;
    }

    // ---- helpers -----------------------------------------------------------

    /** Read i18n/lang_{code}.properties as UTF-8 (empty map if the file is absent). */
    private Map<String, String> loadProps(String lang) {
        Map<String, String> out = new LinkedHashMap<>();
        Resource res = new PathMatchingResourcePatternResolver()
                .getResource("classpath:" + BASE + lang + ".properties");
        if (!res.exists()) return out;
        Properties p = new Properties();
        try (InputStream is = res.getInputStream();
             Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            p.load(reader);
        } catch (Exception e) {
            return out;
        }
        p.forEach((k, v) -> out.put(String.valueOf(k), String.valueOf(v)));
        return out;
    }

    /** Only offer/accept languages we actually ship (or the default). Never null. */
    private String normalize(String lang) {
        if (lang == null || lang.isBlank()) return DEFAULT_LANG;
        String code = lang.trim().toLowerCase();
        for (I18nBundle.LangInfo info : available()) {
            if (info.code().equals(code)) return code;
        }
        return DEFAULT_LANG;
    }

    /** Stable content hash so the client only re-downloads when something changed. */
    private String version(String lang, Map<String, String> merged) {
        return lang + "-" + Integer.toHexString(merged.hashCode());
    }
}
