package com.newuniverse.nusmp.mixin.client;

import com.newuniverse.nusmp.client.multiverse.FourKingdomsSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 0.46: every ResourceLocation blit in GuiGraphics ends in innerBlit (with or without a colour). When Tensura's categories screen
 * draws its "coming soon" slot icon, FourKingdomsSlot draws the Four Kingdoms logo there instead (the menu's button) and the
 * original quad is skipped. Anything else passes straight through. Both targets are optional (require = 0).
 */
@Mixin(GuiGraphics.class)
public abstract class FourKingdomsSlotMixin {
    @Inject(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFF)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void nusmp$fourKingdomsSlot(ResourceLocation atlas, int x1, int x2, int y1, int y2, int z, float u0, float u1, float v0, float v1, CallbackInfo ci) {
        if (FourKingdomsSlot.replace((GuiGraphics) (Object) this, atlas, x1, x2, y1, y2)) ci.cancel();
    }

    @Inject(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFFFFFF)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void nusmp$fourKingdomsSlotTinted(ResourceLocation atlas, int x1, int x2, int y1, int y2, int z, float u0, float u1, float v0, float v1,
                                             float r, float g, float b, float a, CallbackInfo ci) {
        if (FourKingdomsSlot.replace((GuiGraphics) (Object) this, atlas, x1, x2, y1, y2)) ci.cancel();
    }
}
