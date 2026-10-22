package com.newuniverse.nusmp.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Preview stub of the safe additive helper: the additive type of the sprite (the preview rasteriser weighs it by alpha like the safe type does). */
public final class SafeAdditive {
    private SafeAdditive() {}

    public static RenderType additive(ResourceLocation tex) { return RenderType.eyes(tex); }

    public static RenderType emissive(ResourceLocation tex) { return RenderType.eyes(tex); }

    public static RenderType lightning() { return RenderType.lightning(); }
}
