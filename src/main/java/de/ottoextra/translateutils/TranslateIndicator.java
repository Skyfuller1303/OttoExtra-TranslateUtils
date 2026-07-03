package de.ottoextra.translateutils;

import de.ottoextra.chat.ChatChannelState;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;

import java.util.Locale;

/**
 * Zeigt das aktive Sprachpaar (z. B. {@code [DE>SV]}) rechts an der
 * Chat-Eingabezeile, solange die Übersetzung eingeschaltet ist — grün auf
 * RP-Kanälen (Übersetzung greift), grau auf OOC-Kanälen (greift nicht).
 * Rein clientseitige Anzeige über Fabric-Screen-Events, kein Mixin.
 */
public final class TranslateIndicator {

    private static final int COLOR_ACTIVE = 0xFF55FF55;
    private static final int COLOR_INACTIVE = 0xFF888888;

    private TranslateIndicator() {
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof ChatScreen)) {
                return;
            }
            ScreenEvents.afterRender(screen).register((s, ctx, mouseX, mouseY, tickDelta) -> {
                TranslateUtilsConfig config = TranslateUtilsConfig.active();
                if (!config.enabled || !config.showLanguageTag) {
                    return;
                }
                if (!ChatChannelState.buttonActive()) {
                    return;
                }
                String tag = "[" + config.sourceLanguage.toUpperCase(Locale.ROOT) + ">"
                        + config.targetLanguage.toUpperCase(Locale.ROOT) + "]";
                var textRenderer = MinecraftClient.getInstance().textRenderer;
                int x = s.width - textRenderer.getWidth(tag) - 4;
                int y = s.height - 12;
                int color = ChatChannelState.current().rp ? COLOR_ACTIVE : COLOR_INACTIVE;
                ctx.drawTextWithShadow(textRenderer, tag, x, y, color);
            });
        });
    }
}
