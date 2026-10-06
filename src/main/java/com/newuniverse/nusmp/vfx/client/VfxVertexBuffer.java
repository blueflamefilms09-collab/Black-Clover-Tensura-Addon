package com.newuniverse.nusmp.vfx.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immediate-mode quad builder for all Black Clover VFX.
 * Quads are collected per (texture, blend) batch, then flushed in blend order with one draw
 * call per batch. Positions are camera-relative and transformed to view space on the CPU.
 * Each effect activation is capped at {@link #MAX_VERTICES_PER_ACTIVATION}.
 */
public final class VfxVertexBuffer {
    public static final int MAX_VERTICES_PER_ACTIVATION = 400;

    private record Key(ResourceLocation texture, VfxBlend blend) {}

    /** x, y, z, u, v, argb(as float bits) per vertex. */
    private static final class Batch {
        float[] data = new float[6 * 64];
        int size;
        void add(float x, float y, float z, float u, float v, int argb) {
            if (size + 6 > data.length) data = Arrays.copyOf(data, data.length * 2);
            data[size++] = x; data[size++] = y; data[size++] = z;
            data[size++] = u; data[size++] = v; data[size++] = Float.intBitsToFloat(argb);
        }
    }

    private final Map<Key, Batch> batches = new LinkedHashMap<>();
    private int activationVertices;
    private int activationLimit = MAX_VERTICES_PER_ACTIVATION;

    /** Call before drawing each effect instance: resets that instance's vertex budget. */
    public void beginActivation(int limit) {
        activationVertices = 0;
        activationLimit = Math.min(limit, MAX_VERTICES_PER_ACTIVATION);
    }

    public boolean hasBudget(int vertices) { return activationVertices + vertices <= activationLimit; }

    // ------------------------------------------------------------------ primitives

    /** Raw quad. p0..p3 counter-clockwise; uv rectangle u0,v0 -> u1,v1. */
    public void quad(ResourceLocation tex, VfxBlend blend, Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3,
                     float u0, float v0, float u1, float v1, int c0, int c1) {
        if (!hasBudget(4)) return;
        activationVertices += 4;
        Batch b = batches.computeIfAbsent(new Key(tex, blend), k -> new Batch());
        b.add(p0.x, p0.y, p0.z, u0, v1, c0);
        b.add(p1.x, p1.y, p1.z, u1, v1, c0);
        b.add(p2.x, p2.y, p2.z, u1, v0, c1);
        b.add(p3.x, p3.y, p3.z, u0, v0, c1);
    }

    /** Camera-facing square (sparks, glows, burst textures). */
    public void billboard(VfxRenderContext ctx, ResourceLocation tex, VfxBlend blend, Vector3f center, float size, float rotation, int argb) {
        float c = Mth.cos(rotation) * size * 0.5f, s = Mth.sin(rotation) * size * 0.5f;
        Vector3f rx = new Vector3f(ctx.camRight).mul(c).add(new Vector3f(ctx.camUp).mul(s));
        Vector3f ry = new Vector3f(ctx.camUp).mul(c).sub(new Vector3f(ctx.camRight).mul(s));
        int col = blend.grade(argb, 1f);
        quad(tex, blend,
                new Vector3f(center).sub(rx).sub(ry), new Vector3f(center).add(rx).sub(ry),
                new Vector3f(center).add(rx).add(ry), new Vector3f(center).sub(rx).add(ry),
                0, 0, 1, 1, col, col);
    }

    /** Flat square lying on a pose plane (magic circles). */
    public void plane(ResourceLocation tex, VfxBlend blend, VfxPose pose, float halfSize, int argb) {
        int col = blend.grade(argb, 1f);
        quad(tex, blend, pose.point(-halfSize, -halfSize), pose.point(halfSize, -halfSize),
                pose.point(halfSize, halfSize), pose.point(-halfSize, halfSize), 0, 0, 1, 1, col, col);
    }

    /**
     * Ring band on a pose plane. The texture's U wraps 'uRepeat' times around the ring and
     * scrolls by uScroll (for rune bands that crawl around a circle).
     */
    public void ring(ResourceLocation tex, VfxBlend blend, VfxPose pose, float innerR, float outerR, int segments,
                     float uRepeat, float uScroll, int argb) {
        int col = blend.grade(argb, 0.6f);
        for (int i = 0; i < segments; i++) {
            float a0 = Mth.TWO_PI * i / segments, a1 = Mth.TWO_PI * (i + 1) / segments;
            float u0 = uScroll + uRepeat * i / segments, u1 = uScroll + uRepeat * (i + 1) / segments;
            quad(tex, blend,
                    pose.point(Mth.cos(a0) * innerR, Mth.sin(a0) * innerR), pose.point(Mth.cos(a1) * innerR, Mth.sin(a1) * innerR),
                    pose.point(Mth.cos(a1) * outerR, Mth.sin(a1) * outerR), pose.point(Mth.cos(a0) * outerR, Mth.sin(a0) * outerR),
                    u0, 0, u1, 1, col, col);
        }
    }

    /**
     * Camera-facing ribbon from a to b (flame streams, beams). Width tapers from widthA to widthB,
     * color fades from colA to colB, texture V scrolls along the length.
     */
    public void beam(VfxRenderContext ctx, ResourceLocation tex, VfxBlend blend, Vector3f a, Vector3f b,
                     float widthA, float widthB, int segments, float vScroll, int colA, int colB) {
        Vector3f dir = new Vector3f(b).sub(a);
        if (dir.lengthSquared() < 1e-6f) return;
        Vector3f mid = new Vector3f(a).add(b).mul(0.5f);
        Vector3f toCam = new Vector3f(mid).negate().normalize();
        Vector3f side = new Vector3f(dir).cross(toCam).normalize();
        for (int i = 0; i < segments; i++) {
            float t0 = (float) i / segments, t1 = (float) (i + 1) / segments;
            Vector3f c0 = new Vector3f(dir).mul(t0).add(a), c1 = new Vector3f(dir).mul(t1).add(a);
            float w0 = Mth.lerp(t0, widthA, widthB) * 0.5f, w1 = Mth.lerp(t1, widthA, widthB) * 0.5f;
            int k0 = blend.grade(lerpColor(colA, colB, t0), 1 - t0), k1 = blend.grade(lerpColor(colA, colB, t1), 1 - t1);
            Vector3f s0 = new Vector3f(side).mul(w0), s1 = new Vector3f(side).mul(w1);
            quad(tex, blend, new Vector3f(c0).sub(s0), new Vector3f(c0).add(s0), new Vector3f(c1).add(s1), new Vector3f(c1).sub(s1),
                    0, vScroll + t0, 1, vScroll + t1, k0, k1);
        }
    }

    /**
     * Crescent arc on a pose plane (slashes). Sweeps 'sweep' radians starting at 'start';
     * width tapers to the tips, alpha fades toward the tail (start).
     */
    public void arc(ResourceLocation tex, VfxBlend blend, VfxPose pose, float radius, float width,
                    float start, float sweep, int segments, int argb) {
        for (int i = 0; i < segments; i++) {
            float t0 = (float) i / segments, t1 = (float) (i + 1) / segments;
            float a0 = start + sweep * t0, a1 = start + sweep * t1;
            float w0 = width * Mth.sin(t0 * Mth.PI) * 0.5f + 0.01f, w1 = width * Mth.sin(t1 * Mth.PI) * 0.5f + 0.01f;
            int k0 = blend.grade(withAlpha(argb, t0), t0), k1 = blend.grade(withAlpha(argb, t1), t1);
            quad(tex, blend,
                    pose.point(Mth.cos(a0) * (radius - w0), Mth.sin(a0) * (radius - w0)),
                    pose.point(Mth.cos(a1) * (radius - w1), Mth.sin(a1) * (radius - w1)),
                    pose.point(Mth.cos(a1) * (radius + w1), Mth.sin(a1) * (radius + w1)),
                    pose.point(Mth.cos(a0) * (radius + w0), Mth.sin(a0) * (radius + w0)),
                    t0, 0, t1, 1, k0, k1);
        }
    }

    // ------------------------------------------------------------------ flush

    /** Draws every batch (ALPHA first, glows last) and clears. Call once per frame. */
    public void flush(Matrix4f modelView) {
        if (batches.isEmpty()) return;
        List<Map.Entry<Key, Batch>> order = new ArrayList<>(batches.entrySet());
        order.sort(Comparator.comparingInt(e -> e.getKey().blend().order));

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        Vector3f p = new Vector3f();
        for (Map.Entry<Key, Batch> e : order) {
            Batch b = e.getValue();
            if (b.size == 0) continue;
            Minecraft.getInstance().getTextureManager().getTexture(e.getKey().texture()).setFilter(true, false);
            RenderSystem.setShaderTexture(0, e.getKey().texture());
            e.getKey().blend().apply();
            BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            for (int i = 0; i < b.size; i += 6) {
                modelView.transformPosition(b.data[i], b.data[i + 1], b.data[i + 2], p);
                int c = Float.floatToRawIntBits(b.data[i + 5]);
                bb.addVertex(p.x, p.y, p.z).setUv(b.data[i + 3], b.data[i + 4])
                        .setColor((c >> 16) & 255, (c >> 8) & 255, c & 255, c >>> 24);
            }
            MeshData mesh = bb.build();
            if (mesh != null) BufferUploader.drawWithShader(mesh);
        }
        batches.clear();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
    }

    // ------------------------------------------------------------------ color helpers

    public static int withAlpha(int argb, float alphaMul) {
        int a = Mth.clamp((int) ((argb >>> 24) * alphaMul), 0, 255);
        return (a << 24) | (argb & 0xFFFFFF);
    }

    public static int lerpColor(int c0, int c1, float t) {
        int a = (int) Mth.lerp(t, c0 >>> 24, c1 >>> 24), r = (int) Mth.lerp(t, (c0 >> 16) & 255, (c1 >> 16) & 255);
        int g = (int) Mth.lerp(t, (c0 >> 8) & 255, (c1 >> 8) & 255), bl = (int) Mth.lerp(t, c0 & 255, c1 & 255);
        return (a << 24) | (r << 16) | (g << 8) | bl;
    }

    /** Brightens a color toward white (used for hot cores). */
    public static int whiten(int argb, float t) { return lerpColor(argb, (argb & 0xFF000000) | 0xFFFFFF, t); }
}
