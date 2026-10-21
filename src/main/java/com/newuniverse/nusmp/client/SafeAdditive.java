package com.newuniverse.nusmp.client;

import com.newuniverse.nusmp.vfx.client.VfxSafeBlendConfig;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * 1.110.0 (docs/visual_upgrade/safe_additive.md): the ONE place every additive / emissive pass of the mod's client code asks for its render type. Why: vanilla's {@code RenderType.eyes} adds a
 * texture's RGB without weighing it by alpha (and drops texels under alpha 0.1), so a card or glow painted "white everywhere, alpha shapes it" shows as a hard white rectangle; the upgraded
 * weapons and relics drew their halo cards, flares, glow, vein and star passes with {@code eyes} directly and did not go through the 1.105.0 audit. Now:
 * <ul>
 *   <li>{@link #additive}: {@link AdditiveTypes#of}: the alpha-weighted additive type ({@code SRC_ALPHA, ONE}, nearest sampling, no depth write) for every texture that is not black-under-alpha
 *       premultiplied, {@code RenderType.eyes} only for a texture proven premultiplied (identical look to before), and always the alpha-weighted one with {@code vfx.safeBlend}.</li>
 *   <li>{@link #emissive}: the translucent emissive type (alpha blended, so it cannot box a transparent texel); with {@code vfx.safeBlend} the alpha-weighted additive one.</li>
 *   <li>{@link #lightning}: position+colour bolts, no texture, nothing to box.</li>
 * </ul>
 * {@code SafeAdditiveSourceScanTest} fails if any other client class builds these render types directly. Render thread only.
 */
public final class SafeAdditive {
    private SafeAdditive() {}

    /** The additive render type of a sprite, halo card, glow map, vein map or star map. */
    public static RenderType additive(ResourceLocation tex) { return AdditiveTypes.of(tex); }

    /** The emissive (full bright, alpha blended) render type of a glow card or glow pass. */
    public static RenderType emissive(ResourceLocation tex) {
        return VfxSafeBlendConfig.safeBlend() ? AdditiveTypes.of(tex) : RenderType.entityTranslucentEmissive(tex);
    }

    /** Untextured colour-only bolts (full bright). */
    public static RenderType lightning() { return RenderType.lightning(); }
}
