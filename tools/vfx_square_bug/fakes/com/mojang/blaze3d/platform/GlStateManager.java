package com.mojang.blaze3d.platform;

/** Harness fake: only the blend factor names the VFX buffer uses. */
public class GlStateManager {
    public enum SourceFactor { ONE, SRC_ALPHA, ONE_MINUS_DST_COLOR }
    public enum DestFactor { ONE, ONE_MINUS_SRC_ALPHA, ONE_MINUS_SRC_COLOR }
}
