package com.newuniverse.nusmp.mixin.client;

import com.newuniverse.nusmp.client.multiverse.FourKingdomsSlot;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 0.57: on Tensura's magic categories screen the "Coming Soon" slot is the Four Kingdoms button, but Tensura still draws its red
 * "COMING SOON" tooltip over it, on top of ours. While that screen is open the tooltip that reads "coming soon" is skipped
 * (ours, "Four Kingdoms", is drawn by FourKingdomsSlot and is not affected). All targets are optional (require = 0).
 */
@Mixin(GuiGraphics.class)
public abstract class ComingSoonTooltipMixin {
    private static boolean nusmp$soon(String s) { return s != null && s.toLowerCase(java.util.Locale.ROOT).contains("coming soon"); }

    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;II)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void nusmp$tipComponent(Font font, Component text, int x, int y, CallbackInfo ci) {
        if (text != null && nusmp$soon(text.getString()) && FourKingdomsSlot.onTensuraScreen()) ci.cancel();
    }

    @Inject(method = "renderComponentTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;II)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void nusmp$tipList(Font font, List<Component> lines, int x, int y, CallbackInfo ci) {
        if (lines != null && !lines.isEmpty() && nusmp$soon(lines.get(0).getString()) && FourKingdomsSlot.onTensuraScreen()) ci.cancel();
    }

    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;II)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void nusmp$tipSeq(Font font, List<? extends FormattedCharSequence> lines, int x, int y, CallbackInfo ci) {
        if (lines == null || lines.isEmpty() || !FourKingdomsSlot.onTensuraScreen()) return;
        StringBuilder sb = new StringBuilder();
        lines.get(0).accept((i, st, cp) -> { sb.appendCodePoint(cp); return true; });
        if (nusmp$soon(sb.toString())) ci.cancel();
    }
}
