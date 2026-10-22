package com.newuniverse.nusmp.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Preview stub of the mod's AdditiveTypes: the additive type of the preview is the vanilla eyes type (the game's alpha-weighted one differs only for textures with colour under alpha 0). */
public final class AdditiveTypes {
    private AdditiveTypes() {}

    public static RenderType of(ResourceLocation t) { return RenderType.eyes(t); }
}
