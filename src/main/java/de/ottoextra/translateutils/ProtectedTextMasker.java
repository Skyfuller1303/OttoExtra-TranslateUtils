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
 * {@code §x}, URLs und eckige Tags wie {@code [OOC]}. OOC-Passagen
 * {@code ((...))} sind immer geschützt (Serverkonvention).</p>
 *
 * <p>Platzhalter werden im maskierten Text mit Leerzeichen isoliert, damit
 * die Übersetzungs-API sie als eigenes Token stehen lässt (klebende Emotes
 * wie {@code Tag*so*} würden sonst verstümmelt). Das Original-Spacing wird
 * pro Teil gemerkt und beim Wiederherstellen exakt rekonstruiert.</p>
 */
public final class ProtectedTextMasker {

    private static final Pattern OOC_PARENS = Pattern.compile("\\(\\(.{1,256}?\\)\\)");
    private static final Pattern EMOTE = Pattern.compile("\\*[^*]{1,256}\\*");
    private static final Pattern COLOR_CODE = Pattern.compile("§.");
    private static final Pattern URL = Pattern.compile("https?://\\S+");
    private static final Pattern BRACKET_TAG = Pattern.compile("\\[[^\\[\\]]{1,24}]");
    private static final Pattern PLACEHOLDER = Pattern.compile("XKEEP(\\d+)X", Pattern.CASE_INSENSITIVE);
    /** Platzhalter inkl. umgebendem Leerraum — der wird beim Restore ersetzt. */
    private static final Pattern PLACEHOLDER_SPACED =
            Pattern.compile("\\s*XKEEP(\\d+)X\\s*", Pattern.CASE_INSENSITIVE);

    /** Ein geschützter Teil samt Original-Spacing zu den Nachbarn. */
    public record Part(String text, boolean spaceBefore, boolean spaceAfter) {
    }

    /** Ergebnis der Maskierung: Text mit Platzhaltern + Originalteile in Reihenfolge. */
    public record Masked(String text, List<Part> parts) {

        /** Kein Buchstabe außerhalb der Platzhalter? Dann nichts zu übersetzen. */
        public boolean nothingToTranslate() {
            String stripped = PLACEHOLDER.matcher(text).replaceAll("");
            return stripped.codePoints().noneMatch(Character::isLetter);
        }
    }

    private ProtectedTextMasker() {
    }

    public static Masked mask(String message, TranslateUtilsConfig config) {
        List<Pattern> patterns = new ArrayList<>();
        // OOC-Passagen ((...)) immer schützen: nie übersetzen.
        patterns.add(OOC_PARENS);
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

        List<Part> parts = new ArrayList<>();
        String text = message;
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                boolean spaceBefore = matcher.start() == 0
                        || Character.isWhitespace(text.charAt(matcher.start() - 1));
                boolean spaceAfter = matcher.end() == text.length()
                        || Character.isWhitespace(text.charAt(matcher.end()));
                parts.add(new Part(matcher.group(), spaceBefore, spaceAfter));
                // Mit Leerzeichen isolieren, damit die API das Token stehen lässt.
                matcher.appendReplacement(sb,
                        Matcher.quoteReplacement(" XKEEP" + (parts.size() - 1) + "X "));
            }
            matcher.appendTail(sb);
            text = sb.toString();
        }
        return new Masked(text.replaceAll("\\s+", " ").trim(), parts);
    }

    /**
     * Ersetzt Platzhalter in der API-Antwort wieder durch die Originalteile
     * mit ihrem Original-Spacing. Verschluckt die API einen Platzhalter
     * (kommt je nach Sprachpaar vor), geht der geschützte Teil nicht verloren:
     * er wird je nach Originalposition vorn oder hinten wieder angefügt.
     */
    public static String restore(String translated, Masked masked) {
        List<Part> parts = masked.parts();
        boolean[] used = new boolean[parts.size()];
        Matcher matcher = PLACEHOLDER_SPACED.matcher(translated);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            String replacement = matcher.group().trim();
            if (index < parts.size()) {
                Part part = parts.get(index);
                replacement = (part.spaceBefore() ? " " : "") + part.text()
                        + (part.spaceAfter() ? " " : "");
                used[index] = true;
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        StringBuilder prefix = new StringBuilder();
        StringBuilder suffix = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (used[i]) {
                continue;
            }
            int pos = masked.text().indexOf("XKEEP" + i + "X");
            if (pos >= 0 && pos < masked.text().length() / 2) {
                prefix.append(parts.get(i).text()).append(' ');
            } else {
                suffix.append(' ').append(parts.get(i).text());
            }
        }
        return (prefix + sb.toString().trim() + suffix).trim();
    }
}
