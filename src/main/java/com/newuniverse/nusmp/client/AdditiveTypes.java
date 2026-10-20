package com.newuniverse.nusmp.client;

import com.newuniverse.nusmp.vfx.AdditivePolicy;
import com.newuniverse.nusmp.vfx.client.TextureAudit;
import com.newuniverse.nusmp.vfx.client.VfxSafeBlendConfig;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why (docs/vfx_square_bug.md): the one place that picks the additive render type of an aura sprite or glow map. {@link AdditivePolicy#choose} decides from the texture's audit ({@link TextureAudit}):
 * a texture that carries a colour under its transparent texels (or cannot be read) gets {@link NURenderTypes#additiveAlpha}, the others keep {@code RenderType.eyes} exactly as before, and
 * {@code vfx.safeBlend} forces the alpha-weighted type for all. The render type per location is cached (the painters ask for it every frame); the cache is emptied when the resources reload and
 * when the config is toggled (two tables, one per setting). Render thread only.
 */
public final class AdditiveTypes {
    private AdditiveTypes() {}

    private static final Map<ResourceLocation, RenderType> AUTO = new ConcurrentHashMap<>(), SAFE = new ConcurrentHashMap<>();

    public static RenderType of(ResourceLocation tex) {
        boolean forced = VfxSafeBlendConfig.safeBlend();
        Map<ResourceLocation, RenderType> table = forced ? SAFE : AUTO;
        RenderType rt = table.get(tex);
        if (rt == null) {
            AdditivePolicy.Blend blend;
            try {
                blend = AdditivePolicy.choose(TextureAudit.kind(tex), forced);
            } catch (RuntimeException | LinkageError e) {
                blend = AdditivePolicy.Blend.ALPHA_ADD;                       // an audit that fails is a texture we know nothing about: the blend that cannot make a square
            }
            rt = blend == AdditivePolicy.Blend.ALPHA_ADD ? NURenderTypes.additiveAlpha(tex) : RenderType.eyes(tex);
            table.put(tex, rt);
        }
        return rt;
    }

    public static void clear() {
        AUTO.clear();
        SAFE.clear();
    }
}
