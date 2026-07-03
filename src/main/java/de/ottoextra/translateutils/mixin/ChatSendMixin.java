package de.ottoextra.translateutils.mixin;

import de.ottoextra.translateutils.OutgoingChatTranslator;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Dünner Adapter: fängt ausgehende Chatnachrichten am HEAD ab und delegiert
 * an {@link OutgoingChatTranslator}. Cancelt nur, wenn der Translator die
 * Nachricht übernimmt (asynchrone Übersetzung, danach Send mit Bypass-Flag).
 *
 * <p>Ziel: {@link ClientPlayNetworkHandler#sendChatMessage(String)} — Commands
 * laufen über {@code sendCommand} und bleiben unberührt. Fallback: Translator
 * deaktiviert sich selbst (Config/Kanal-Gate), dann läuft Vanilla weiter.</p>
 */
@Mixin(ClientPlayNetworkHandler.class)
public class ChatSendMixin {

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void translateutils$onSendChatMessage(String content, CallbackInfo ci) {
        if (OutgoingChatTranslator.intercept(content)) {
            ci.cancel();
        }
    }
}
