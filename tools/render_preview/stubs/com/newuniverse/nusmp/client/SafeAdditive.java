package com.newuniverse.nusmp.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Preview stub of client.SafeAdditive. */
public final class SafeAdditive {
    private SafeAdditive() {}
    public static RenderType additive(ResourceLocation tex) { return RenderType.eyes(tex); }
    public static RenderType emissive(ResourceLocation tex) { return RenderType.entityTranslucentEmissive(tex); }
    public static RenderType lightning() { return RenderType.lightning(); }
}
