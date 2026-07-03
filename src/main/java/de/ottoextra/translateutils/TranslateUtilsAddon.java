package de.ottoextra.translateutils;

import de.ottoextra.addon.OttoExtraAddon;
import de.ottoextra.config.settings.SettingsRegistry;
import de.ottoextra.config.settings.SettingsRegistry.Option;

/**
 * OttoExtra-Entrypoint ({@code "ottoextra"} in der fabric.mod.json):
 * hängt den Reiter "TranslateUtils" in die OttoExtra-Einstellungen.
 */
public final class TranslateUtilsAddon implements OttoExtraAddon {

    private static final String[] LANGUAGE_VALUES =
            TranslateUtilsConfig.LANGUAGES.toArray(String[]::new);

    @Override
    public void registerSettings(SettingsRegistry registry) {
        build(registry, TranslateUtilsConfig.active(), true);
    }

    @Override
    public void registerDefaultSettings(SettingsRegistry registry) {
        build(registry, new TranslateUtilsConfig(), false);
    }

    private void build(SettingsRegistry registry, TranslateUtilsConfig c, boolean persist) {
        Runnable save = persist ? c::save : () -> { };

        var module = registry.module("translateutils",
                "ottoextra_translateutils.module", "ottoextra_translateutils.module.desc");

        var base = SettingsRegistry.tab(module, "ottoextra.set.tab.base");
        SettingsRegistry.card(base,
                "ottoextra_translateutils.card.main", "ottoextra_translateutils.card.main.desc",
                Option.bool("ottoextra_translateutils.opt.enabled", "translate.enabled",
                        () -> c.enabled, v -> { c.enabled = v; save.run(); }),
                Option.cycle("ottoextra_translateutils.opt.sourceLanguage", "translate.sourceLanguage",
                        () -> c.sourceLanguage, v -> { c.sourceLanguage = v; save.run(); },
                        LANGUAGE_VALUES),
                Option.cycle("ottoextra_translateutils.opt.targetLanguage", "translate.targetLanguage",
                        () -> c.targetLanguage, v -> { c.targetLanguage = v; save.run(); },
                        LANGUAGE_VALUES),
                Option.bool("ottoextra_translateutils.opt.showLanguageTag", "translate.showLanguageTag",
                        () -> c.showLanguageTag, v -> { c.showLanguageTag = v; save.run(); })
                        .tooltip("ottoextra_translateutils.opt.showLanguageTag.tip"),
                Option.bool("ottoextra_translateutils.opt.showOriginal", "translate.showOriginalMessage",
                        () -> c.showOriginalMessage, v -> { c.showOriginalMessage = v; save.run(); })
                        .tooltip("ottoextra_translateutils.opt.showOriginal.tip"),
                Option.bool("ottoextra_translateutils.opt.feedback", "translate.feedbackMessages",
                        () -> c.feedbackMessages, v -> { c.feedbackMessages = v; save.run(); }));
        SettingsRegistry.card(base,
                "ottoextra_translateutils.card.protect", "ottoextra_translateutils.card.protect.desc",
                Option.bool("ottoextra_translateutils.opt.protectEmotes", "translate.protectEmotes",
                        () -> c.protectEmotes, v -> { c.protectEmotes = v; save.run(); })
                        .tooltip("ottoextra_translateutils.opt.protectEmotes.tip"),
                Option.bool("ottoextra_translateutils.opt.protectColors", "translate.protectColorCodes",
                        () -> c.protectColorCodes, v -> { c.protectColorCodes = v; save.run(); }),
                Option.bool("ottoextra_translateutils.opt.protectUrls", "translate.protectUrls",
                        () -> c.protectUrls, v -> { c.protectUrls = v; save.run(); }),
                Option.bool("ottoextra_translateutils.opt.protectTags", "translate.protectBracketTags",
                        () -> c.protectBracketTags, v -> { c.protectBracketTags = v; save.run(); })
                        .tooltip("ottoextra_translateutils.opt.protectTags.tip"));

        var advanced = SettingsRegistry.tab(module, "ottoextra.set.tab.advanced");
        SettingsRegistry.card(advanced,
                "ottoextra_translateutils.card.api", "ottoextra_translateutils.card.api.desc",
                Option.string("ottoextra_translateutils.opt.apiUrl", "translate.apiUrl",
                        () -> c.apiUrl, v -> { c.apiUrl = v; save.run(); }),
                Option.string("ottoextra_translateutils.opt.apiKey", "translate.apiKey",
                        () -> c.apiKey, v -> { c.apiKey = v; save.run(); })
                        .tooltip("ottoextra_translateutils.opt.apiKey.tip"),
                Option.intVal("ottoextra_translateutils.opt.maxCharacters", "translate.maxCharacters",
                        () -> c.maxCharacters, v -> { c.maxCharacters = v; save.run(); }, 50, 5000),
                Option.intVal("ottoextra_translateutils.opt.timeoutMillis", "translate.timeoutMillis",
                        () -> c.timeoutMillis, v -> { c.timeoutMillis = v; save.run(); }, 1000, 30000));
    }
}
