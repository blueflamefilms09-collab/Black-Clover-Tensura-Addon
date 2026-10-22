package com.newuniverse.nusmp.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Preview stub of the mod's SafeAdditive (the real one asks the safe-blend config and the texture audit): additive and emissive are the preview's additive and translucent-emissive types. */
public final class SafeAdditive {
    private SafeAdditive() {}

    public static RenderType additive(ResourceLocation t) { return AdditiveTypes.of(t); }

    public static RenderType emissive(ResourceLocation t) { return RenderType.entityTranslucentEmissive(t); }

    public static RenderType lightning() { return RenderType.lightning(); }
}
