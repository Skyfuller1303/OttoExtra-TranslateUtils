package de.ottoextra.translateutils;

import de.ottoextra.chat.ChatChannelState;
import net.minecraft.client.MinecraftClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Fängt ausgehende RP-Chatnachrichten ab, übersetzt sie asynchron und sendet
 * das Ergebnis. Bei jedem Fehler (API weg, Timeout, ...) geht die
 * Originalnachricht unverändert raus — Chat darf nie verloren gehen.
 */
public final class OutgoingChatTranslator {

    /** Serverbound-Chat ist auf 256 Zeichen begrenzt. */
    private static final int CHAT_MAX = 256;

    /** Versatz zwischen aufeinanderfolgenden Sendungen (Flood-Schutz). */
    private static final long SEND_DELAY_MS = 600;

    /** Reentrancy-Schutz: gesetzt, während wir selbst senden (nur Client-Thread). */
    private static boolean sending;

    /** Puffer für LongChat-Teile (nur Client-Thread); Serie verfällt nach Timeout. */
    private static final StringBuilder pendingParts = new StringBuilder();
    private static long pendingAtMs;
    private static final long PENDING_TIMEOUT_MS = 10_000;

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
        String trimmed = message.trim();
        if (trimmed.isEmpty()) {
            return false;
        }

        // OttoExtras LongChat splittet lange Eingaben in Teile; nicht-letzte
        // enden mit dem Fortsetzungsmarker (z. B. " >"). Teile hier wieder
        // zusammensetzen: die ganze Nachricht wird EINMAL übersetzt und
        // geclustert ausgegeben (erst Übersetzung komplett, dann ((Original))).
        long now = System.currentTimeMillis();
        if (pendingParts.length() > 0 && now - pendingAtMs > PENDING_TIMEOUT_MS) {
            pendingParts.setLength(0);
        }
        String marker = longChatMarker();
        if (!marker.isEmpty() && trimmed.endsWith(marker)) {
            String part = trimmed.substring(0, trimmed.length() - marker.length()).trim();
            pendingParts.append(part).append(' ');
            pendingAtMs = now;
            return true; // Teil geschluckt — Rest der Serie folgt gleich
        }
        final String original = pendingParts.isEmpty()
                ? trimmed
                : (pendingParts + trimmed).trim();
        pendingParts.setLength(0);

        MinecraftClient client = MinecraftClient.getInstance();
        if (original.length() > config.maxCharacters) {
            // Zu lang für die API: unübersetzt, aber vollständig rausschicken.
            sendSequential(client, splitChunks(original, CHAT_MAX));
            return true;
        }

        ProtectedTextMasker.Masked masked = ProtectedTextMasker.mask(original, config);
        if (masked.nothingToTranslate()) {
            if (original.equals(trimmed)) {
                return false; // nichts aggregiert -> Vanilla normal senden lassen
            }
            sendSequential(client, splitChunks(original, CHAT_MAX));
            return true;
        }

        LibreTranslateClient.translate(config, masked.text())
                .orTimeout(config.timeoutMillis, TimeUnit.MILLISECONDS)
                .handle((translated, error) -> {
                    if (error != null) {
                        TranslateUtils.LOGGER.warn("Übersetzung fehlgeschlagen, sende Original: {}",
                                error.toString());
                        return List.of(original);
                    }
                    return outgoingMessages(config, original,
                            ProtectedTextMasker.restore(translated, masked));
                })
                .thenAccept(messages -> sendSequential(client, messages));
        return true;
    }

    /**
     * Sendet Nachrichten nacheinander mit Versatz (Flood-Schutz); die erste
     * sofort, damit der Chat nicht spürbar verzögert.
     */
    private static void sendSequential(MinecraftClient client, List<String> messages) {
        for (int i = 0; i < messages.size(); i++) {
            String msg = messages.get(i);
            if (i == 0) {
                client.execute(() -> sendBypassing(client, msg));
            } else {
                CompletableFuture.delayedExecutor(i * SEND_DELAY_MS, TimeUnit.MILLISECONDS)
                        .execute(() -> client.execute(() -> sendBypassing(client, msg)));
            }
        }
    }

    /**
     * Zuerst die Übersetzung, optional danach das Original als eigenständige
     * {@code ((OOC))}-Nachricht — inline hinter RP-Text färbt der Server
     * Doppelklammern nicht als OOC ein, als ganze Nachricht schon. Beides wird
     * wortweise auf das 256-Zeichen-Limit gechunkt statt abgeschnitten.
     * Das Sprachkürzel wird nicht mitgesendet, sondern nur lokal an der
     * Eingabezeile angezeigt ({@link TranslateIndicator}).
     */
    private static List<String> outgoingMessages(TranslateUtilsConfig config,
                                                 String original, String translated) {
        String body = translated.trim();
        if (body.isEmpty()) {
            return List.of(original);
        }
        List<String> out = new java.util.ArrayList<>(splitChunks(body, CHAT_MAX));
        if (config.showOriginalMessage) {
            for (String chunk : splitChunks(original, CHAT_MAX - 4)) {
                out.add("((" + chunk + "))");
            }
        }
        return out;
    }

    /** Unterhalb dieser Restlänge wird früher geschnitten (kein Winz-Chunk). */
    private static final int MIN_TAIL = 16;

    /** Teilt Text an Wortgrenzen in Stücke von maximal {@code max} Zeichen. */
    private static List<String> splitChunks(String text, int max) {
        List<String> chunks = new java.util.ArrayList<>();
        String rest = text.trim();
        while (!rest.isEmpty()) {
            if (rest.length() <= max) {
                chunks.add(rest);
                break;
            }
            // Würde nur ein Winz-Rest übrig bleiben, früher schneiden.
            int cutLimit = rest.length() - max < MIN_TAIL ? max - MIN_TAIL : max;
            int cut = rest.lastIndexOf(' ', cutLimit);
            if (cut <= cutLimit / 2) {
                cut = cutLimit; // kein brauchbares Leerzeichen -> hart trennen
            }
            chunks.add(rest.substring(0, cut).trim());
            rest = rest.substring(cut).trim();
        }
        return chunks;
    }

    /** OttoExtras LongChat-Marker (getrimmt), leer wenn Chat-Config fehlt. */
    private static String longChatMarker() {
        var chatConfig = ChatChannelState.chatConfig();
        if (chatConfig == null || chatConfig.longChatMarker == null) {
            return "";
        }
        return chatConfig.longChatMarker.trim();
    }

    private static void sendBypassing(MinecraftClient client, String outgoing) {
        if (client.getNetworkHandler() == null || outgoing.isBlank()) {
            return;
        }
        sending = true;
        try {
            TranslateUtils.LOGGER.info("Sende uebersetzt: {}", outgoing);
            client.getNetworkHandler().sendChatMessage(outgoing);
        } finally {
            sending = false;
        }
    }
}
