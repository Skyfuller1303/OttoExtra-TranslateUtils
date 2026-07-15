package de.ottoextra.translateutils;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * Client-Entrypoint des Addons: lädt die Config und registriert den
 * Umschalt-Hotkey (Standard: nicht belegt — in den Steuerung-Optionen
 * zuweisbar, z. B. auf eine freie Taste).
 */
public final class TranslateUtilsClient implements ClientModInitializer {

    private KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        TranslateUtilsConfig.active();

        toggleKey = new KeyBinding("key.ottoextra_translateutils.toggle",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, KeyBinding.Category.MISC);
        KeyBindingHelper.registerKeyBinding(toggleKey);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                toggle(client);
            }
        });

        TranslateIndicator.register();

        TranslateUtils.LOGGER.info("{} bereit (Addon fuer OttoExtra).", TranslateUtils.MOD_NAME);
    }

    public static void toggle(MinecraftClient client) {
        TranslateUtilsConfig config = TranslateUtilsConfig.active();
        config.enabled = !config.enabled;
        config.save();
        if (config.feedbackMessages && client.player != null) {
            Text message = config.enabled
                    ? Text.translatable("ottoextra_translateutils.toggle.on",
                            config.sourceLanguage.toUpperCase(Locale.ROOT),
                            config.targetLanguage.toUpperCase(Locale.ROOT))
                            .formatted(Formatting.GREEN)
                    : Text.translatable("ottoextra_translateutils.toggle.off")
                            .formatted(Formatting.GRAY);
            client.player.sendMessage(message, false);
        }
    }
}
