package net.minecraft.client.renderer;

import net.minecraft.resources.ResourceLocation;

/** Preview stub: texture + blend kind (cutout / translucent / eyes = additive). */
public record RenderType(ResourceLocation texture, String kind) {
    public static RenderType entityCutoutNoCull(ResourceLocation t) { return new RenderType(t, "cutout"); }
    public static RenderType entityTranslucent(ResourceLocation t) { return new RenderType(t, "translucent"); }
    public static RenderType eyes(ResourceLocation t) { return new RenderType(t, "eyes"); }
}
