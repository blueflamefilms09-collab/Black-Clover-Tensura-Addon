package com.newuniverse.nusmp.vfx.client;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.vfx.AdditivePolicy;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why (docs/vfx_square_bug.md): the last line of defence of the VFX vertex buffer. Each batch asks it which texture to bind: normally the one it was built for, but a texture that does not exist
 * (the missing-texture checker or a blank would be a solid square), or an additive flat sprite that came out opaque, is swapped for the generated soft circle. The answer is cached per
 * (texture, additive) and the audit reads the file once. Never throws: any trouble means the texture is used as it is. Time Magic textures are never touched.
 */
public final class VfxTextureGuard {
    private VfxTextureGuard() {}

    private record Key(ResourceLocation tex, boolean additive) {}

    private static final Map<Key, ResourceLocation> CACHE = new ConcurrentHashMap<>();

    /** The texture the batch should bind: {@code tex} itself, or the soft circle. */
    public static ResourceLocation resolve(ResourceLocation tex, VfxBlend blend) {
        boolean additive = blend == VfxBlend.ADD || blend == VfxBlend.FIRE || blend == VfxBlend.WIND;
        Key key = new Key(tex, additive);
        ResourceLocation r = CACHE.get(key);
        if (r != null) return r;
        r = tex;
        try {
            if (AdditivePolicy.replaceWithSoftCircle(TextureAudit.kind(tex), additive, tex.getPath(), VfxSafeBlendConfig.safeBlend())) {
                r = SoftCircleTexture.get();
                LogUtils.getLogger().warn("[nusmp] texture {} cannot be drawn as a sprite (missing or opaque): the generated soft circle is used instead", tex);
            }
        } catch (RuntimeException | LinkageError e) {
            r = tex;
        }
        CACHE.put(key, r);
        return r;
    }

    public static void clear() { CACHE.clear(); }
}
