package de.ottoextra.translateutils;

import de.ottoextra.chat.ChatChannelState;
import net.minecraft.client.MinecraftClient;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Fängt ausgehende RP-Chatnachrichten ab, übersetzt sie asynchron und sendet
 * das Ergebnis. Bei jedem Fehler (API weg, Timeout, ...) geht die
 * Originalnachricht unverändert raus — Chat darf nie verloren gehen.
 */
public final class OutgoingChatTranslator {

    /** Serverbound-Chat ist auf 256 Zeichen begrenzt. */
    private static final int CHAT_MAX = 256;

    /** Reentrancy-Schutz: gesetzt, während wir selbst senden (nur Client-Thread). */
    private static boolean sending;

    private OutgoingChatTranslator() {
    }

    /**
     * Vom Mixin am HEAD von {@code sendChatMessage} aufgerufen.
     *
     * @return {@code true} = Vanilla-Send canceln, wir übernehmen asynchron.
     */
    public static boolean intercept(String message) {
        if (sending) {
            return false;
        }
        TranslateUtilsConfig config = TranslateUtilsConfig.active();
        if (!config.enabled) {
            return false;
        }
        // Nur RP-Kanäle auf Ottonien; OOC/Hilfe und andere Server bleiben unberührt.
        if (!ChatChannelState.buttonActive() || !ChatChannelState.current().rp) {
            return false;
        }
        String original = message.trim();
        if (original.isEmpty() || original.length() > config.maxCharacters) {
            return false;
        }

        ProtectedTextMasker.Masked masked = ProtectedTextMasker.mask(original, config);
        if (masked.nothingToTranslate()) {
            return false;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        LibreTranslateClient.translate(config, masked.text())
                .orTimeout(config.timeoutMillis, TimeUnit.MILLISECONDS)
                .handle((translated, error) -> {
                    if (error != null) {
                        TranslateUtils.LOGGER.warn("Übersetzung fehlgeschlagen, sende Original: {}",
                                error.toString());
                        return original;
                    }
                    return compose(config, original,
                            ProtectedTextMasker.restore(translated, masked.parts()));
                })
                .thenAccept(outgoing -> client.execute(() -> sendBypassing(client, outgoing)));
        return true;
    }

    /** Baut "[DE>SV] übersetzt (Original)" und hält das 256-Zeichen-Limit ein. */
    private static String compose(TranslateUtilsConfig config, String original, String translated) {
        String tag = config.showLanguageTag
                ? "[" + config.sourceLanguage.toUpperCase(Locale.ROOT) + ">"
                        + config.targetLanguage.toUpperCase(Locale.ROOT) + "] "
                : "";
        String body = translated.trim();
        if (body.isEmpty()) {
            return original;
        }
        String result = tag + body;
        if (config.showOriginalMessage) {
            String withOriginal = result + " (" + original + ")";
            if (withOriginal.length() <= CHAT_MAX) {
                result = withOriginal;
            }
        }
        if (result.length() > CHAT_MAX) {
            result = result.substring(0, CHAT_MAX);
        }
        return result;
    }

    private static void sendBypassing(MinecraftClient client, String outgoing) {
        if (client.getNetworkHandler() == null || outgoing.isBlank()) {
            return;
        }
        sending = true;
        try {
            client.getNetworkHandler().sendChatMessage(outgoing);
        } finally {
            sending = false;
        }
    }
}
