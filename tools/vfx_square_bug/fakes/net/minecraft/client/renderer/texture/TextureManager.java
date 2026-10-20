package net.minecraft.client.renderer.texture;

import net.minecraft.resources.ResourceLocation;

public class TextureManager {
    public AbstractTexture getTexture(ResourceLocation loc) { return new AbstractTexture(loc.toString()); }
}
