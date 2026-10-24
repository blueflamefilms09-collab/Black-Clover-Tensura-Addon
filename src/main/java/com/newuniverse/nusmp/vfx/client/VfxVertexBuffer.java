package com.newuniverse.nusmp.vfx.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.newuniverse.nusmp.vfx.FlatProps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Immediate-mode quad builder for all Black Clover VFX.
 * Quads are collected per (texture, blend) batch, then flushed in blend order with one draw
 * call per batch. Positions are camera-relative and transformed to view space on the CPU.
 * Each effect activation is capped at {@link #MAX_VERTICES_PER_ACTIVATION}.
 *
 * <p>1.99.0 (performance pass, docs/performance_1_99.md): the output is bit for bit the one the buffer always produced, but a quad no longer allocates. The old code built a
 * {@code new Key(texture, blend)} for every quad (a record, hashed and compared, thrown away); now a batch is found by the texture (a map of one slot per blend mode) with a one-entry
 * "same as the last quad" shortcut, and the batch objects with their arrays are kept from frame to frame instead of being rebuilt. The shapes ({@link #billboard}, {@link #plane},
 * {@link #ring}, {@link #beam}, {@link #arc}) do their vector arithmetic in plain floats, in the same order of operations as the vector classes did, so they allocate nothing per segment.
 * Draw order is unchanged: batches are drawn in the order of their first quad of the frame, stably sorted by blend order.
 */
public final class VfxVertexBuffer {
    public static final int MAX_VERTICES_PER_ACTIVATION = 400;

    private static final int BLENDS = VfxBlend.values().length;
    /** Batches kept between frames; past this many textures the table starts again (a texture set that is not finite must not grow it without end). */
    private static final int MAX_KEPT_TEXTURES = 512;

    /** x, y, z, u, v, argb(as float bits) per vertex. One per (texture, blend) pair, kept between frames. */
    private static final class Batch {
        final ResourceLocation texture;
        final VfxBlend blend;
        float[] data = new float[6 * 64];
        int size;
        boolean listed;
        /** the texture is one of the flat object cut-outs ({@link FlatProps}): its quads are dropped while {@code visual.flatProps} is off. */
        final boolean flat;

        Batch(ResourceLocation texture, VfxBlend blend) {
            this.texture = texture;
            this.blend = blend;
            this.flat = FlatProps.isFlat(texture.getPath());
        }

        void add(float x, float y, float z, float u, float v, int argb) {
            if (size + 6 > data.length) data = Arrays.copyOf(data, data.length * 2);
            data[size++] = x; data[size++] = y; data[size++] = z;
            data[size++] = u; data[size++] = v; data[size++] = Float.intBitsToFloat(argb);
        }
    }

    /** texture -> one batch slot per blend mode (equal locations share their slots, as the old (texture, blend) key did). */
    private final Map<ResourceLocation, Batch[]> byTexture = new HashMap<>();
    /** The batches used this frame, in the order of their first quad. */
    private final List<Batch> order = new ArrayList<>();
    private ResourceLocation lastTex;
    private VfxBlend lastBlend;
    private Batch lastBatch;
    private int activationVertices;
    private int activationLimit = MAX_VERTICES_PER_ACTIVATION;
    private int lastFlushVertices;

    /**
     * 1.105.0: the texture a batch binds, given the one it was built for and its blend. Identity until {@link VfxClientEvents} installs {@link VfxTextureGuard#resolve}; a guard that throws is ignored
     * (the texture is used as it is), so the render path can never fail on it.
     */
    public static volatile java.util.function.BiFunction<ResourceLocation, VfxBlend, ResourceLocation> textureGuard = (t, bl) -> t;

    private static ResourceLocation guard(ResourceLocation tex, VfxBlend blend) {
        try {
            ResourceLocation r = textureGuard.apply(tex, blend);
            return r != null ? r : tex;
        } catch (RuntimeException | LinkageError e) {
            return tex;
        }
    }

    /** The vertices the last {@link #flush} drew (for the performance report). */
    public int lastFlushVertices() { return lastFlushVertices; }

    private Batch batch(ResourceLocation tex, VfxBlend blend) {
        if (tex == lastTex && blend == lastBlend && lastBatch != null) return lastBatch;
        Batch[] row = byTexture.get(tex);
        if (row == null) {
            row = new Batch[BLENDS];
            byTexture.put(tex, row);
        }
        int k = blend.ordinal();
        Batch b = row[k];
        if (b == null) {
            b = new Batch(tex, blend);
            row[k] = b;
        }
        if (!b.listed) {
            b.listed = true;
            order.add(b);
        }
        lastTex = tex;
        lastBlend = blend;
        lastBatch = b;
        return b;
    }

    /** Call before drawing each effect instance: resets that instance's vertex budget. */
    public void beginActivation(int limit) {
        activationVertices = 0;
        activationLimit = Math.min(limit, MAX_VERTICES_PER_ACTIVATION);
    }

    public boolean hasBudget(int vertices) { return activationVertices + vertices <= activationLimit; }

    /** The vertices the current (or just finished) activation has added. */
    public int activationUsed() { return activationVertices; }

    // ------------------------------------------------------------------ primitives

    /** Raw quad. p0..p3 counter-clockwise; uv rectangle u0,v0 -> u1,v1. */
    public void quad(ResourceLocation tex, VfxBlend blend, Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3,
                     float u0, float v0, float u1, float v1, int c0, int c1) {
        quadRaw(tex, blend, p0.x, p0.y, p0.z, p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, p3.x, p3.y, p3.z, u0, v0, u1, v1, c0, c1);
    }

    /** The same quad from plain floats (no vectors to build): the shapes below use it. */
    private void quadRaw(ResourceLocation tex, VfxBlend blend,
                         float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3,
                         float u0, float v0, float u1, float v1, int c0, int c1) {
        if (!hasBudget(4)) return;
        Batch b = batch(tex, blend);
        if (b.flat && !FlatProps.drawFlat()) return;                       // the flat sword / axe / shield / paper-figure sprites are not drawn (docs/visual/flat_props.md)
        activationVertices += 4;
        b.add(x0, y0, z0, u0, v1, c0);
        b.add(x1, y1, z1, u1, v1, c0);
        b.add(x2, y2, z2, u1, v0, c1);
        b.add(x3, y3, z3, u0, v0, c1);
    }

    /** Camera-facing square (sparks, glows, burst textures). */
    public void billboard(VfxRenderContext ctx, ResourceLocation tex, VfxBlend blend, Vector3f center, float size, float rotation, int argb) {
        float c = Mth.cos(rotation) * size * 0.5f, s = Mth.sin(rotation) * size * 0.5f;
        Vector3f r = ctx.camRight, u = ctx.camUp;
        // rx = camRight * c + camUp * s ; ry = camUp * c - camRight * s   (the order of the vector version)
        float rxx = r.x * c + u.x * s, rxy = r.y * c + u.y * s, rxz = r.z * c + u.z * s;
        float ryx = u.x * c - r.x * s, ryy = u.y * c - r.y * s, ryz = u.z * c - r.z * s;
        int col = blend.grade(argb, 1f);
        float cx = center.x, cy = center.y, cz = center.z;
        quadRaw(tex, blend,
                cx - rxx - ryx, cy - rxy - ryy, cz - rxz - ryz,
                cx + rxx - ryx, cy + rxy - ryy, cz + rxz - ryz,
                cx + rxx + ryx, cy + rxy + ryy, cz + rxz + ryz,
                cx - rxx + ryx, cy - rxy + ryy, cz - rxz + ryz,
                0, 0, 1, 1, col, col);
    }

    /** The x / y / z of {@code pose.point(x, y)}: origin + (right * x + up * y), the arithmetic of {@link VfxPose#point}. */
    private static float px(VfxPose pose, float x, float y) { return pose.origin().x + (pose.right().x * x + pose.up().x * y); }

    private static float py(VfxPose pose, float x, float y) { return pose.origin().y + (pose.right().y * x + pose.up().y * y); }

    private static float pz(VfxPose pose, float x, float y) { return pose.origin().z + (pose.right().z * x + pose.up().z * y); }

    /** Flat square lying on a pose plane (magic circles). */
    public void plane(ResourceLocation tex, VfxBlend blend, VfxPose pose, float halfSize, int argb) {
        int col = blend.grade(argb, 1f);
        float h = halfSize;
        quadRaw(tex, blend,
                px(pose, -h, -h), py(pose, -h, -h), pz(pose, -h, -h),
                px(pose, h, -h), py(pose, h, -h), pz(pose, h, -h),
                px(pose, h, h), py(pose, h, h), pz(pose, h, h),
                px(pose, -h, h), py(pose, -h, h), pz(pose, -h, h),
                0, 0, 1, 1, col, col);
    }

    /**
     * Ring band on a pose plane. The texture's U wraps 'uRepeat' times around the ring and
     * scrolls by uScroll (for rune bands that crawl around a circle).
     */
    public void ring(ResourceLocation tex, VfxBlend blend, VfxPose pose, float innerR, float outerR, int segments,
                     float uRepeat, float uScroll, int argb) {
        int col = blend.grade(argb, 0.6f);
        for (int i = 0; i < segments; i++) {
            if (!hasBudget(4)) return;
            float a0 = Mth.TWO_PI * i / segments, a1 = Mth.TWO_PI * (i + 1) / segments;
            float u0 = uScroll + uRepeat * i / segments, u1 = uScroll + uRepeat * (i + 1) / segments;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            quadRaw(tex, blend,
                    px(pose, c0 * innerR, s0 * innerR), py(pose, c0 * innerR, s0 * innerR), pz(pose, c0 * innerR, s0 * innerR),
                    px(pose, c1 * innerR, s1 * innerR), py(pose, c1 * innerR, s1 * innerR), pz(pose, c1 * innerR, s1 * innerR),
                    px(pose, c1 * outerR, s1 * outerR), py(pose, c1 * outerR, s1 * outerR), pz(pose, c1 * outerR, s1 * outerR),
                    px(pose, c0 * outerR, s0 * outerR), py(pose, c0 * outerR, s0 * outerR), pz(pose, c0 * outerR, s0 * outerR),
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
            if (!hasBudget(4)) return;
            float t0 = (float) i / segments, t1 = (float) (i + 1) / segments;
            float c0x = dir.x * t0 + a.x, c0y = dir.y * t0 + a.y, c0z = dir.z * t0 + a.z;
            float c1x = dir.x * t1 + a.x, c1y = dir.y * t1 + a.y, c1z = dir.z * t1 + a.z;
            float w0 = Mth.lerp(t0, widthA, widthB) * 0.5f, w1 = Mth.lerp(t1, widthA, widthB) * 0.5f;
            int k0 = blend.grade(lerpColor(colA, colB, t0), 1 - t0), k1 = blend.grade(lerpColor(colA, colB, t1), 1 - t1);
            float s0x = side.x * w0, s0y = side.y * w0, s0z = side.z * w0;
            float s1x = side.x * w1, s1y = side.y * w1, s1z = side.z * w1;
            quadRaw(tex, blend,
                    c0x - s0x, c0y - s0y, c0z - s0z,
                    c0x + s0x, c0y + s0y, c0z + s0z,
                    c1x + s1x, c1y + s1y, c1z + s1z,
                    c1x - s1x, c1y - s1y, c1z - s1z,
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
            if (!hasBudget(4)) return;
            float t0 = (float) i / segments, t1 = (float) (i + 1) / segments;
            float a0 = start + sweep * t0, a1 = start + sweep * t1;
            float w0 = width * Mth.sin(t0 * Mth.PI) * 0.5f + 0.01f, w1 = width * Mth.sin(t1 * Mth.PI) * 0.5f + 0.01f;
            int k0 = blend.grade(withAlpha(argb, t0), t0), k1 = blend.grade(withAlpha(argb, t1), t1);
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            float xi0 = c0 * (radius - w0), yi0 = s0 * (radius - w0), xi1 = c1 * (radius - w1), yi1 = s1 * (radius - w1);
            float xo1 = c1 * (radius + w1), yo1 = s1 * (radius + w1), xo0 = c0 * (radius + w0), yo0 = s0 * (radius + w0);
            quadRaw(tex, blend,
                    px(pose, xi0, yi0), py(pose, xi0, yi0), pz(pose, xi0, yi0),
                    px(pose, xi1, yi1), py(pose, xi1, yi1), pz(pose, xi1, yi1),
                    px(pose, xo1, yo1), py(pose, xo1, yo1), pz(pose, xo1, yo1),
                    px(pose, xo0, yo0), py(pose, xo0, yo0), pz(pose, xo0, yo0),
                    t0, 0, t1, 1, k0, k1);
        }
    }

    // ------------------------------------------------------------------ flush

    /** Draws every batch (ALPHA first, glows last) and clears. Call once per frame. */
    public void flush(Matrix4f modelView) {
        lastFlushVertices = 0;
        if (order.isEmpty()) return;
        List<Batch> sorted = new ArrayList<>(order);
        sorted.sort(Comparator.comparingInt(b -> b.blend.order));              // stable: equal blend orders stay in the order of their first quad

        Vector3f p = new Vector3f();
        for (Batch b : sorted) {
            if (b.size == 0) continue;
            lastFlushVertices += b.size / 6;
            // 1.105.0 (docs/vfx_square_bug.md): every batch sets the whole state it depends on itself (shader, colour modulator, depth, cull, texture, blend), so nothing a draw before it left behind
            // (a modulator, a disabled blend, a texture bound to unit 0) can reach it; the texture goes through the guard (a missing or opaque flat sprite becomes the generated soft circle).
            ResourceLocation tex = guard(b.texture, b.blend);
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.enableDepthTest();
            // not cached on purpose: the aura render types set the same texture to nearest between frames. The pixel textures of the wind style (wmc_*) are sampled hard, as vanilla's particle textures are.
            Minecraft.getInstance().getTextureManager().getTexture(tex).setFilter(!com.newuniverse.nusmp.vfx.WindMc.isPixelTexture(tex.getPath()), false);
            RenderSystem.setShaderTexture(0, tex);
            b.blend.apply();
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
        for (Batch b : order) {                                                // the batches (and their arrays) stay for the next frame
            b.size = 0;
            b.listed = false;
        }
        order.clear();
        lastTex = null;
        lastBlend = null;
        lastBatch = null;
        if (byTexture.size() > MAX_KEPT_TEXTURES) byTexture.clear();
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
