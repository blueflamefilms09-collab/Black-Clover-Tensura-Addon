package com.newuniverse.nusmp.mixin.client;

import com.newuniverse.nusmp.client.multiverse.MultiverseStatusClient;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The Tensura magic / status menu draws its "COMING SOON" placeholder as plain text over an icon, not as a widget, so the
 * Multiverse panel could not find it by widget. This catches that text while such a screen is open: it is not drawn, and the
 * Multiverse status panel takes its place (see MultiverseStatusClient#placeholderText). Every other text is untouched.
 */
@Mixin(GuiGraphics.class)
public abstract class ComingSoonTextMixin {
    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I", at = @At("HEAD"), cancellable = true)
    private void nusmp$comingSoonString(Font font, String text, int x, int y, int color, boolean shadow, CallbackInfoReturnable<Integer> cir) {
        if (text != null && MultiverseStatusClient.placeholderText((GuiGraphics) (Object) this, font, text, x, y)) cir.setReturnValue(x);
    }

    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)I", at = @At("HEAD"), cancellable = true)
    private void nusmp$comingSoonSequence(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow, CallbackInfoReturnable<Integer> cir) {
        if (text == null || !MultiverseStatusClient.watching()) return;
        StringBuilder sb = new StringBuilder();
        text.accept((i, style, cp) -> { sb.appendCodePoint(cp); return true; });
        if (MultiverseStatusClient.placeholderText((GuiGraphics) (Object) this, font, sb.toString(), x, y)) cir.setReturnValue(x);
    }
}
