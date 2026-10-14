package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.core.magic.merge.ColorBlend;
import com.newuniverse.nusmp.core.magic.merge.MergedBlend;
import com.newuniverse.nusmp.core.magic.merge.MergedDef;
import com.newuniverse.nusmp.core.magic.merge.MergedKeys;
import com.newuniverse.nusmp.core.magic.merge.MergedRegistry;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.AbstractVfxLayer;
import com.newuniverse.nusmp.vfx.client.VfxAnim;
import com.newuniverse.nusmp.vfx.client.VfxBlend;
import com.newuniverse.nusmp.vfx.client.VfxBloom;
import com.newuniverse.nusmp.vfx.client.VfxInstance;
import com.newuniverse.nusmp.vfx.client.VfxPose;
import com.newuniverse.nusmp.vfx.client.VfxRenderContext;
import com.newuniverse.nusmp.vfx.client.VfxTextures;
import com.newuniverse.nusmp.vfx.client.VfxVertexBuffer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 1.27.0: the composite look of a merged magic. The parents' own effects are sent as ordinary effects by core.magic.merge.MergeContext
 * (each parent's shapes, drawn by its own layer, tinted with the shared palette); this layer is what ties them together, drawn from the
 * merged magic's palette alone (the definition is found by the key in the high half of the payload's seed, see MergedKeys):
 * <ul>
 *   <li>MERGED_CAST: one strand of motes per parent, each in its parent's colour, winding round a blended core from 'from' to 'to';
 *       the head glows in all of them, the flash where it ends changes colour from parent to parent. power = size.</li>
 *   <li>MERGED_FIELD: one rune ring per parent turning against its neighbours, a blended circle under them and an outer band that
 *       walks round the palette; motes rise in turns. power = radius.</li>
 *   <li>MERGED_BURST: a shock ring that changes colour from parent to parent as it spreads, sparks that alternate the palette, a
 *       blended flash. power = size.</li>
 *   <li>MERGED_AURA: the stance round the caster (follows): every parent's circle stacked under the feet, turning in turns, with
 *       motes rising in the palette. power = size.</li>
 * </ul>
 * Sizes follow the parents' weights; if the definition is not known yet (the sync has not arrived) it draws the payload's colour and a
 * paler twin, so it never fails.
 */
public class MergedMagicLayer extends AbstractVfxLayer {
    private static final Set<VfxShape> SHAPES = EnumSet.of(VfxShape.MERGED_CAST, VfxShape.MERGED_FIELD, VfxShape.MERGED_BURST, VfxShape.MERGED_AURA);

    @Override public Set<VfxShape> shapes() { return SHAPES; }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case MERGED_CAST -> 16;
            case MERGED_FIELD -> 42;
            case MERGED_BURST -> 22;
            default -> 100;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFC8A0FF; }

    // ------------------------------------------------------------------ the palette of an instance
    /** The colours and weights of one merged magic (or the stand-in), kept so a frame does not rebuild them. */
    private record Pal(MergedDef def, int[] colors, float[] weights, int primary) {}

    private static final Map<String, Pal> PALETTES = new HashMap<>();
    private static final Map<Integer, Pal> STAND_INS = new HashMap<>();

    private static Pal palette(VfxInstance inst) {
        MergedDef def = MergedRegistry.byKey(MergedKeys.keyOfSeed(inst.seed));
        if (def != null) {
            Pal p = PALETTES.get(def.id());
            if (p == null || !p.def().equals(def)) {
                MergedBlend b = MergedRegistry.blend(def);
                p = new Pal(def, b.colors, b.weights, b.primary);
                PALETTES.put(def.id(), p);
            }
            return p;
        }
        int c = inst.color | 0xFF000000;
        return STAND_INS.computeIfAbsent(c, k -> {
            int[] cols = {k, ColorBlend.whiten(k, 0.55f) | 0xFF000000};
            float[] w = ColorBlend.weights(2);
            return new Pal(null, cols, w, k);
        });
    }

    // ------------------------------------------------------------------ small helpers
    private static float sat(float v) { return Mth.clamp(v, 0f, 1f); }

    private static float noise(int i, int salt) {
        int h = i * 0x9E3779B1 + salt * 0x85EBCA6B;
        h ^= h >>> 15; h *= 0x2C1B3C6D; h ^= h >>> 13;
        return (h & 0xFFFF) / 65535f;
    }

    private static Vector3f lerp(Vector3f a, Vector3f b, float t) { return new Vector3f(a).lerp(b, t); }

    private static Vector3f[] frame(Vector3f dir) {
        Vector3f right = new Vector3f(dir).cross(0f, 1f, 0f);
        if (right.lengthSquared() < 1e-4f) right.set(1f, 0f, 0f);
        right.normalize();
        Vector3f up = new Vector3f(dir).cross(right).normalize();
        return new Vector3f[]{right, up};
    }

    private static ResourceLocation mote(int parent) {
        return switch (Math.floorMod(parent, 4)) {
            case 1 -> VfxTextures.SHARD;
            case 2 -> VfxTextures.MANA_MOTE;
            case 3 -> VfxTextures.GLOW;
            default -> VfxTextures.SPARK;
        };
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        Pal pal = palette(inst);
        switch (inst.shape) {
            case MERGED_CAST -> cast(inst, ctx, buf, pal);
            case MERGED_FIELD -> field(inst, ctx, buf, pal);
            case MERGED_BURST -> burst(inst, ctx, buf, pal);
            case MERGED_AURA -> aura(inst, ctx, buf, pal);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ MERGED_CAST
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, Pal pal) {
        float age = inst.ageTicks(ctx.partialTick), t = inst.progress(ctx.partialTick);
        float s = Mth.clamp(inst.power, 0.6f, 3f);
        int n = pal.colors().length;
        Vector3f f = ctx.rel(inst.from(ctx)), g = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(g).sub(f);
        float len = dir.length();
        if (len < 1e-3f) return;
        dir.div(len);
        Vector3f[] fr = frame(dir);

        float e = VfxAnim.easeOutCubic(sat(t / 0.45f));
        float tailE = Math.min(e, sat((t - 0.12f) / 0.88f));
        float fade = 1f - 0.7f * sat((t - 0.45f) / 0.55f);
        Vector3f head = lerp(f, g, e), tail = lerp(f, g, tailE);
        int tailCol = ColorBlend.cycle(pal.colors(), age * 0.05f);

        // the core: the blended colour at the head, a walk round the palette at the tail, a white-hot filament inside
        buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, head, tail, 0.5f * s, 0.14f * s, 1, 0f, VfxVertexBuffer.withAlpha(pal.primary(), 0.9f * fade), VfxVertexBuffer.withAlpha(tailCol, 0.5f * fade));
        buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, head, tail, 0.18f * s, 0.06f * s, 1, 0f, VfxVertexBuffer.withAlpha(ColorBlend.whiten(pal.primary(), 0.85f), fade), VfxVertexBuffer.withAlpha(ColorBlend.whiten(tailCol, 0.7f), 0.6f * fade));

        // one strand of motes per parent, each winding round the core in its own colour (heavier parents wind wider)
        int motes = ctx.seg(7, 4);
        for (int strand = 0; strand < n; strand++) {
            int col = pal.colors()[strand];
            float rad = 0.30f * s * ColorBlend.sizeFor(pal.weights()[strand], n);
            ResourceLocation m = mote(strand);
            for (int i = 0; i < motes; i++) {
                float k = (i + 0.5f) / motes;
                Vector3f p = lerp(tail, head, k);
                float ph = k * 9f + age * 0.5f + Mth.TWO_PI * strand / n;
                float r = rad * (0.4f + 0.6f * Mth.sin(k * Mth.PI));
                p.add(new Vector3f(fr[0]).mul(Mth.cos(ph) * r)).add(new Vector3f(fr[1]).mul(Mth.sin(ph) * r));
                buf.billboard(ctx, m, VfxBlend.ADD, p, 0.28f * s * (0.5f + 0.5f * k), ph, VfxVertexBuffer.withAlpha(col, fade * (0.35f + 0.65f * k)));
            }
        }

        // the head glows in every parent's colour
        float hs = 0.8f * s;
        for (int i = 0; i < n; i++)
            buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, head, hs * (1f - 0.22f * i), 0f, VfxVertexBuffer.withAlpha(pal.colors()[i], 0.75f * fade));
        buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, head, hs * 0.8f, age * 0.3f, VfxVertexBuffer.withAlpha(ColorBlend.whiten(pal.primary(), 0.9f), fade));

        // the flash where it ends changes colour from parent to parent
        if (t > 0.7f) {
            float u = sat((t - 0.7f) / 0.3f), out = 1f - u * u;
            buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, g, (1.0f + 1.6f * VfxAnim.easeOutCubic(u)) * s, 0f, VfxVertexBuffer.withAlpha(ColorBlend.cycle(pal.colors(), u), 0.8f * out));
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, g, 1.8f * s * VfxAnim.easeOutCubic(u), age * 0.2f, VfxVertexBuffer.withAlpha(ColorBlend.whiten(pal.primary(), 0.8f), out));
        }
    }

    // ------------------------------------------------------------------ MERGED_FIELD
    private void field(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, Pal pal) {
        float age = inst.ageTicks(ctx.partialTick), t = inst.progress(ctx.partialTick);
        float R = Mth.clamp(inst.power * 1.6f, 1.6f, 7.5f);
        int n = pal.colors().length;
        float lf = Math.min(sat(age / 5f), sat((inst.duration - age) / 8f));
        if (lf <= 0f) return;
        float open = VfxAnim.easeOutCubic(sat(age / 10f));
        Vector3f c = ctx.rel(inst.from(ctx));
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0f, 0.06f, 0f));

        buf.plane(VfxTextures.MAGIC_CIRCLE, VfxBlend.ADD, ground.spin(age * 0.03f), R * open * 0.95f, VfxVertexBuffer.withAlpha(pal.primary(), 0.55f * lf));
        for (int i = 0; i < n; i++) {                                           // one rune ring per parent, neighbours turning against each other
            float share = n == 1 ? 1f : 0.45f + 0.5f * i / (n - 1);
            float dir = i % 2 == 0 ? 1f : -1f;
            buf.plane(VfxTextures.RUNE_RING, VfxBlend.ADD, ground.lift(0.015f * (i + 1)).spin(dir * age * (0.05f + 0.02f * i)), R * open * share * ColorBlend.sizeFor(pal.weights()[i], n) * 0.9f,
                    VfxVertexBuffer.withAlpha(pal.colors()[i], 0.85f * lf));
        }
        buf.ring(VfxTextures.RUNE_RING, VfxBlend.ADD, ground.lift(0.08f), R * 0.9f * open, R * open, ctx.seg(24, 10), 3f, age * 0.01f,
                VfxVertexBuffer.withAlpha(ColorBlend.cycle(pal.colors(), age * 0.02f), 0.8f * lf));
        int rise = ctx.seg(9, 4);
        for (int i = 0; i < rise; i++) {                                        // motes rise in turns
            float life = ((age * 0.03f) + i / (float) rise) % 1f;
            float a = Mth.TWO_PI * i / rise + age * 0.04f + noise(i, 3) * 2f;
            float r = R * (0.25f + 0.65f * noise(i, 5));
            Vector3f p = new Vector3f(c).add(Mth.cos(a) * r, 0.1f + life * 2.2f, Mth.sin(a) * r);
            buf.billboard(ctx, mote(i), VfxBlend.ADD, p, 0.3f * (1f - life * 0.5f), a, VfxVertexBuffer.withAlpha(ColorBlend.alternate(pal.colors(), i), lf * (1f - life)));
        }
        VfxBloom.planeGlow(buf, ground, R * 0.7f, pal.primary(), 0.5f * lf);
    }

    // ------------------------------------------------------------------ MERGED_BURST
    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, Pal pal) {
        float age = inst.ageTicks(ctx.partialTick), t = inst.progress(ctx.partialTick);
        float s = Mth.clamp(inst.power, 0.6f, 4f);
        float R = 1.5f * s;
        int n = pal.colors().length;
        float out = 1f - t * t;
        float grow = VfxAnim.easeOutCubic(sat(t / 0.8f));
        Vector3f c = ctx.rel(inst.from(ctx)).add(0f, 0.5f, 0f);
        VfxPose face = VfxPose.facing(c, new Vector3f(ctx.camera.getLookVector()));

        buf.plane(VfxTextures.RUNE_RING, VfxBlend.ADD, face.spin(age * 0.12f), R * (0.2f + 0.85f * grow), VfxVertexBuffer.withAlpha(ColorBlend.cycle(pal.colors(), t * 1.5f), 0.9f * out));
        buf.plane(VfxTextures.MAGIC_CIRCLE, VfxBlend.ADD, face.spin(-age * 0.08f), R * (0.15f + 0.55f * grow), VfxVertexBuffer.withAlpha(pal.primary(), 0.7f * out));
        float fi = 1f - sat(t / 0.35f);
        if (fi > 0f) buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, c, R * 1.3f * fi, 0f, VfxVertexBuffer.withAlpha(pal.primary(), 0.85f * fi));
        int sparks = ctx.seg(5 * n, 6);
        for (int i = 0; i < sparks; i++) {
            float a = Mth.TWO_PI * i / sparks + age * 0.05f + noise(i, 7);
            float el = (noise(i, 11) - 0.5f) * 1.6f;
            float r = R * grow * (0.5f + 0.5f * noise(i, 13));
            Vector3f p = new Vector3f(c).add(Mth.cos(a) * r * Mth.cos(el), Mth.sin(el) * r, Mth.sin(a) * r * Mth.cos(el));
            buf.billboard(ctx, mote(i), VfxBlend.ADD, p, 0.3f * s * (1f - 0.6f * t), a, VfxVertexBuffer.withAlpha(ColorBlend.alternate(pal.colors(), i), out));
        }
    }

    // ------------------------------------------------------------------ MERGED_AURA
    private void aura(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, Pal pal) {
        float age = inst.ageTicks(ctx.partialTick), t = inst.progress(ctx.partialTick);
        float s = Mth.clamp(inst.power, 0.5f, 2.5f);
        int n = pal.colors().length;
        float fade = VfxAnim.fadeInOut(t, 0.08f, 0.3f);
        if (fade <= 0f) return;
        Vector3f base = ctx.rel(inst.from(ctx));
        VfxPose feet = VfxPose.ground(new Vector3f(base).add(0f, 0.04f, 0f));
        for (int i = 0; i < n; i++) {                                           // every parent's circle, stacked, turning in turns
            float dir = i % 2 == 0 ? 1f : -1f;
            buf.plane(VfxTextures.MAGIC_CIRCLE, VfxBlend.ADD, feet.lift(0.012f * i).spin(dir * age * (0.05f + 0.015f * i) + i), 1.35f * s * ColorBlend.sizeFor(pal.weights()[i], n),
                    VfxVertexBuffer.withAlpha(pal.colors()[i], 0.7f * fade));
        }
        buf.plane(VfxTextures.RUNE_RING, VfxBlend.ADD, feet.lift(0.08f).spin(-age * 0.04f), 1.9f * s, VfxVertexBuffer.withAlpha(pal.primary(), 0.55f * fade));
        VfxBloom.planeGlow(buf, feet, 1.1f * s, pal.primary(), 0.45f * fade);
        int motes = ctx.seg(8, 4);
        for (int i = 0; i < motes; i++) {
            float life = ((age * 0.025f) + i / (float) motes) % 1f;
            float a = Mth.TWO_PI * i / motes + age * 0.07f;
            Vector3f p = new Vector3f(base).add(Mth.cos(a) * 0.9f * s, 0.1f + life * 2.3f, Mth.sin(a) * 0.9f * s);
            buf.billboard(ctx, mote(i), VfxBlend.ADD, p, 0.28f * (1f - 0.5f * life), a, VfxVertexBuffer.withAlpha(ColorBlend.cycle(pal.colors(), i / (float) motes + age * 0.01f), fade * (1f - life)));
        }
    }
}
