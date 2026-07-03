package de.ottoextra.translateutils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maskiert schützenswerte Textteile vor der Übersetzung mit Platzhaltern
 * ({@code XKEEP0X}, {@code XKEEP1X}, ...) und stellt sie danach wieder her.
 *
 * <p>Geschützt (je nach Config): Emotes {@code *...*}, Minecraft-Farbcodes
 * {@code §x}, URLs und eckige Tags wie {@code [OOC]}.</p>
 */
public final class ProtectedTextMasker {

    private static final Pattern EMOTE = Pattern.compile("\\*[^*]{1,256}\\*");
    private static final Pattern COLOR_CODE = Pattern.compile("§.");
    private static final Pattern URL = Pattern.compile("https?://\\S+");
    private static final Pattern BRACKET_TAG = Pattern.compile("\\[[^\\[\\]]{1,24}]");
    private static final Pattern PLACEHOLDER = Pattern.compile("XKEEP(\\d+)X", Pattern.CASE_INSENSITIVE);

    /** Ergebnis der Maskierung: Text mit Platzhaltern + Originalteile in Reihenfolge. */
    public record Masked(String text, List<String> parts) {

        /** Nur Platzhalter/Leerraum übrig? Dann gibt es nichts zu übersetzen. */
        public boolean nothingToTranslate() {
            return PLACEHOLDER.matcher(text).replaceAll("").isBlank();
        }
    }

    private ProtectedTextMasker() {
    }

    public static Masked mask(String message, TranslateUtilsConfig config) {
        List<Pattern> patterns = new ArrayList<>();
        if (config.protectEmotes) {
            patterns.add(EMOTE);
        }
        if (config.protectColorCodes) {
            patterns.add(COLOR_CODE);
        }
        if (config.protectUrls) {
            patterns.add(URL);
        }
        if (config.protectBracketTags) {
            patterns.add(BRACKET_TAG);
        }

        List<String> parts = new ArrayList<>();
        String text = message;
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                parts.add(matcher.group());
                matcher.appendReplacement(sb,
                        Matcher.quoteReplacement("XKEEP" + (parts.size() - 1) + "X"));
            }
            matcher.appendTail(sb);
            text = sb.toString();
        }
        return new Masked(text, parts);
    }

    /** Ersetzt Platzhalter in der API-Antwort wieder durch die Originalteile. */
    public static String restore(String translated, List<String> parts) {
        Matcher matcher = PLACEHOLDER.matcher(translated);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            String replacement = index < parts.size() ? parts.get(index) : matcher.group();
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
