package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.platform.GlStateManager;

/** Why: harness shim: only the additive-or-normal blend choice matters to the picture. */
public final class RenderSystem {
    private RenderSystem() {}
    public static boolean additive;
    public static void enableBlend() {}
    public static void disableBlend() {}
    public static void defaultBlendFunc() { additive = false; }
    public static void blendFunc(GlStateManager.SourceFactor s, GlStateManager.DestFactor d) { additive = d == GlStateManager.DestFactor.ONE; }
}
