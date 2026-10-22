package com.newuniverse.nusmp.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Preview stub of client.AdditiveTypes: the additive type is the stub's RenderType.eyes. */
public final class AdditiveTypes {
    private AdditiveTypes() {}
    public static RenderType of(ResourceLocation tex) { return RenderType.eyes(tex); }
}
