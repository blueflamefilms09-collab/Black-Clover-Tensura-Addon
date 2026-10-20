package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Harness fake: records every call and keeps the state a draw would see. */
public final class RenderSystem {
    public static final List<String> LOG = new ArrayList<>();
    public static ResourceLocation texture0;
    public static boolean blend, cull = true, depthTest, depthMask = true, shaderSet;
    public static String blendFunc = "default";
    public static float[] shaderColor = {1, 1, 1, 1};

    public static void setShader(Supplier<ShaderInstance> s) { shaderSet = s.get() != null; LOG.add("setShader"); }
    public static void setShaderColor(float r, float g, float b, float a) { shaderColor = new float[]{r, g, b, a}; LOG.add("setShaderColor " + r + " " + g + " " + b + " " + a); }
    public static void setShaderTexture(int unit, ResourceLocation t) { texture0 = t; LOG.add("setShaderTexture " + unit + " " + t); }
    public static void depthMask(boolean v) { depthMask = v; LOG.add("depthMask " + v); }
    public static void disableCull() { cull = false; LOG.add("disableCull"); }
    public static void enableCull() { cull = true; LOG.add("enableCull"); }
    public static void enableDepthTest() { depthTest = true; LOG.add("enableDepthTest"); }
    public static void enableBlend() { blend = true; LOG.add("enableBlend"); }
    public static void disableBlend() { blend = false; LOG.add("disableBlend"); }
    public static void defaultBlendFunc() { blendFunc = "default"; LOG.add("defaultBlendFunc"); }
    public static void blendFunc(GlStateManager.SourceFactor s, GlStateManager.DestFactor d) { blendFunc = s + "," + d; LOG.add("blendFunc " + s + " " + d); }
    public static void blendFuncSeparate(GlStateManager.SourceFactor s, GlStateManager.DestFactor d, GlStateManager.SourceFactor sa, GlStateManager.DestFactor da) {
        blendFunc = s + "," + d + "/" + sa + "," + da;
        LOG.add("blendFuncSeparate " + blendFunc);
    }
}
