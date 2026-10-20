package com.newuniverse.nusmp.vfx.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.vfx.TextureAlpha;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why (docs/vfx_square_bug.md): reads a texture's pixels from the resource manager once and keeps the verdict of {@link TextureAlpha#classify}. The render types and the VFX vertex buffer ask it
 * which blend a sprite needs and whether a texture can be drawn at all; the answer is cached per location (the render path never touches the resource manager twice) and thrown away when the
 * resources reload. A texture that does not exist or cannot be decoded answers {@code null}. Render thread only (the first look reads the file).
 */
public final class TextureAudit {
    private TextureAudit() {}

    /** The audit of one location; {@code kind} is null when the texture is missing or broken. */
    private record Verdict(TextureAlpha.Kind kind) {}

    private static final Map<ResourceLocation, Verdict> CACHE = new ConcurrentHashMap<>();
    /** Textures larger than this on a side are sampled with a stride (the verdict is about shares, so a sample is as good as the whole). */
    private static final int SAMPLE_SIDE = 512;

    /** The kind of the texture, or null when it is missing or cannot be read. */
    public static TextureAlpha.Kind kind(ResourceLocation tex) {
        Verdict v = CACHE.get(tex);
        if (v == null) {
            v = new Verdict(read(tex));
            CACHE.put(tex, v);
        }
        return v.kind;
    }

    public static void clear() { CACHE.clear(); }

    private static TextureAlpha.Kind read(ResourceLocation tex) {
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(tex);
            if (res.isEmpty()) {
                LogUtils.getLogger().warn("[nusmp] texture {} does not exist: sprites using it are drawn with the generated soft circle", tex);
                return null;
            }
            try (InputStream in = res.get().open(); NativeImage img = NativeImage.read(in)) {
                int w = img.getWidth(), h = img.getHeight();
                int stride = Math.max(1, Math.max(w, h) / SAMPLE_SIDE);
                int sw = Math.max(1, w / stride), sh = Math.max(1, h / stride);
                int[] argb = new int[sw * sh];
                for (int y = 0; y < sh; y++) {
                    int sy = y == sh - 1 ? h - 1 : y * stride;
                    for (int x = 0; x < sw; x++) {
                        int sx = x == sw - 1 ? w - 1 : x * stride;
                        argb[y * sw + x] = abgrToArgb(img.getPixelRGBA(sx, sy));
                    }
                }
                return TextureAlpha.classify(sw, sh, argb);
            }
        } catch (Exception | LinkageError e) {
            LogUtils.getLogger().warn("[nusmp] texture {} could not be read for the alpha audit: {}", tex, e.toString());
            return null;
        }
    }

    /** NativeImage stores a pixel as ABGR; the audit works on ARGB. */
    static int abgrToArgb(int abgr) { return (abgr & 0xFF00FF00) | ((abgr >> 16) & 0xFF) | ((abgr & 0xFF) << 16); }
}
