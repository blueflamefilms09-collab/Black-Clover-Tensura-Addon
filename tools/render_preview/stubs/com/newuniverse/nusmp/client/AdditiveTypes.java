package com.newuniverse.nusmp.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Preview stub of the additive type picker. */
public final class AdditiveTypes {
    private AdditiveTypes() {}

    public static RenderType of(ResourceLocation tex) { return RenderType.eyes(tex); }
}
