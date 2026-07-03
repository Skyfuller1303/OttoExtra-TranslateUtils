package de.ottoextra.translateutils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Config des TranslateUtils-Addons — eigene Datei
 * {@code config/ottoextra-translateutils.json}, unabhängig von der
 * OttoExtra-Config. Tolerant lesen, atomar schreiben (tmp + move).
 */
public final class TranslateUtilsConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Unterstützte Sprachkürzel (LibreTranslate-kompatibel, klein). */
    public static final List<String> LANGUAGES = List.of("de", "en", "da", "sv", "pl", "fr", "es");

    public boolean enabled = false;
    public String sourceLanguage = "de";
    public String targetLanguage = "sv";
    public boolean showLanguageTag = true;
    public boolean showOriginalMessage = true;
    public boolean protectEmotes = true;
    public boolean protectColorCodes = true;
    public boolean protectUrls = true;
    public boolean protectBracketTags = true;
    public boolean feedbackMessages = true;
    public String apiUrl = "https://translate.skyfuller.de/translate";
    public String apiKey = "";
    public int maxCharacters = 1000;
    public int timeoutMillis = 5000;

    private static TranslateUtilsConfig active;

    public static synchronized TranslateUtilsConfig active() {
        if (active == null) {
            active = load();
        }
        return active;
    }

    static Path configFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("ottoextra-translateutils.json");
    }

    public static TranslateUtilsConfig load() {
        Path file = configFile();
        TranslateUtilsConfig config = null;
        if (Files.isRegularFile(file)) {
            try {
                config = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8),
                        TranslateUtilsConfig.class);
            } catch (Exception e) {
                TranslateUtils.LOGGER.warn("Config nicht lesbar, verwende Defaults: {}", file, e);
            }
        }
        if (config == null) {
            config = new TranslateUtilsConfig();
        }
        config.sanitize();
        config.save();
        return config;
    }

    private void sanitize() {
        if (!LANGUAGES.contains(sourceLanguage)) {
            sourceLanguage = "de";
        }
        if (!LANGUAGES.contains(targetLanguage)) {
            targetLanguage = "sv";
        }
        maxCharacters = Math.max(50, Math.min(5000, maxCharacters));
        timeoutMillis = Math.max(1000, Math.min(30000, timeoutMillis));
        if (apiUrl == null || apiUrl.isBlank()) {
            apiUrl = "https://translate.skyfuller.de/translate";
        }
        if (apiKey == null) {
            apiKey = "";
        }
    }

    public synchronized void save() {
        Path file = configFile();
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(this), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            TranslateUtils.LOGGER.error("Config konnte nicht gespeichert werden: {}", file, e);
        }
    }
}
