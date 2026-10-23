package com.newuniverse.nusmp.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Preview stub of the mod's NURenderTypes: only the scrolling swirl of the Spirit Dive ANIME look (the game's is the vanilla energy swirl type, as here). */
public final class NURenderTypes {
    private NURenderTypes() {}

    public static RenderType spiritSwirl(ResourceLocation tex, float u, float v) { return RenderType.energySwirl(tex, u, v); }
}
