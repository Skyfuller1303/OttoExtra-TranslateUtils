package de.ottoextra.translateutils;

import de.ottoextra.chat.ChatChannelState;
import de.ottoextra.chat.ChatInputLayout;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * Zeigt das aktive Sprachpaar (z. B. {@code [DE>SV]}) rechts an der
 * Chat-Eingabezeile. Ein Linksklick schaltet die Übersetzung um. Das Kürzel
 * bleibt auch im ausgeschalteten Zustand sichtbar, damit es wieder aktiviert
 * werden kann. OttoExtra reserviert rechts den benötigten Platz im Textfeld.
 */
public final class TranslateIndicator {

    private static final int COLOR_ACTIVE = 0xFF55FF55;
    private static final int COLOR_INACTIVE = 0xFF888888;
    private static final int COLOR_DISABLED = 0xFFAA5555;
    private static final int RIGHT_MARGIN = 4;
    private static final int INPUT_GAP = 6;
    private static final int HIT_PADDING = 2;

    private TranslateIndicator() {
    }

    public static void register() {
        ChatInputLayout.registerRightReservation(TranslateUtils.MOD_ID,
                TranslateIndicator::reservedWidth);

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof ChatScreen)) {
                return;
            }

            ScreenMouseEvents.allowMouseClick(screen).register((s, click) -> {
                if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT
                        || !isVisible()
                        || !contains(s.width, s.height, click.x(), click.y())) {
                    return true;
                }
                TranslateUtilsClient.toggle(client);
                return false;
            });

            ScreenEvents.afterRender(screen).register((s, ctx, mouseX, mouseY, tickDelta) -> {
                TranslateUtilsConfig config = TranslateUtilsConfig.active();
                if (!isVisible()) {
                    return;
                }
                String tag = tag(config);
                var textRenderer = MinecraftClient.getInstance().textRenderer;
                int x = s.width - textRenderer.getWidth(tag) - RIGHT_MARGIN;
                int y = s.height - 12;
                int color = !config.enabled
                        ? COLOR_DISABLED
                        : ChatChannelState.current().rp ? COLOR_ACTIVE : COLOR_INACTIVE;
                if (contains(s.width, s.height, mouseX, mouseY)) {
                    color = brighten(color);
                }
                ctx.drawTextWithShadow(textRenderer, tag, x, y, color);
            });
        });
    }

    private static boolean isVisible() {
        TranslateUtilsConfig config = TranslateUtilsConfig.active();
        return config.showLanguageTag && ChatChannelState.buttonActive();
    }

    private static int reservedWidth() {
        if (!isVisible()) {
            return 0;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) {
            return 0;
        }
        return client.textRenderer.getWidth(tag(TranslateUtilsConfig.active())) + INPUT_GAP;
    }

    private static boolean contains(int screenWidth, int screenHeight, double mouseX, double mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        String tag = tag(TranslateUtilsConfig.active());
        int width = client.textRenderer.getWidth(tag);
        int x = screenWidth - width - RIGHT_MARGIN;
        int y = screenHeight - 12;
        return mouseX >= x - HIT_PADDING
                && mouseX <= x + width + HIT_PADDING
                && mouseY >= y - HIT_PADDING
                && mouseY <= y + client.textRenderer.fontHeight + HIT_PADDING;
    }

    private static String tag(TranslateUtilsConfig config) {
        return "[" + config.sourceLanguage.toUpperCase(Locale.ROOT) + ">"
                + config.targetLanguage.toUpperCase(Locale.ROOT) + "]";
    }

    private static int brighten(int argb) {
        int red = Math.min(255, ((argb >> 16) & 0xFF) + 0x28);
        int green = Math.min(255, ((argb >> 8) & 0xFF) + 0x28);
        int blue = Math.min(255, (argb & 0xFF) + 0x28);
        return (argb & 0xFF000000) | (red << 16) | (green << 8) | blue;
    }
}
