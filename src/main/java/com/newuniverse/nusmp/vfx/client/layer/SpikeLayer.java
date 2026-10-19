package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 1.93.0 Spike Magic (rebuilt; it used the shared wiki layer before). Brushed gun-metal spikes with ice-white tips: tapered spikes thrusting out of cracked ground, bursting stars of spikes, glinting shards. Textures from tools/gen_spike_textures.py (spike_*).
 * <ul>
 *   <li>SPIKE_FX1 (cast / projectile, default 16 ticks): the Spike Lance: a charge ring on the hand, then a big spike rockets from -> to on a streak with two ghosts and shards shed behind it, and bursts into a star of spikes and cracks at the target. from = origin, to = target, power = size (0.6 to 3), colour = tint mixed with the palette.</li>
 *   <li>SPIKE_FX2 (zone / field, default 80 ticks): the Impaling Field: a seal and cracks mark the ground inside a rim ring, spikes thrust up at staggered moments all over the field and round the rim, shards float up, a pulse runs out. from = centre on the ground, power = RADIUS in blocks; fades in over 8 ticks and out over the last 12.</li>
 *   <li>SPIKE_FX3 (impact / signature, default 28 ticks): the Spike Burst: a flash, a star of spikes thrown out in every direction round the point (they hang, then sink), a ring and cracks on the ground, shards flying in arcs. from = centre, to - from = a direction hint (may be zero), power = scale.</li>
 * </ul>
 * At most 400 vertices per activation (every sprite goes through bb / hasBudget); sprites are layered back to front: dark solid (ALPHA) first, glows (ADD) last.
 */
public class SpikeLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    static final ResourceLocation SIGIL = t("spike_sigil"), RING = t("spike_ring"), FLARE = t("spike_flare"), STREAK = t("spike_streak"), GLOW = t("spike_glow"),
            CRACK = t("spike_crack"), MOTE = t("spike_mote"), SPIKE = t("spike_spike"), SPIKE2 = t("spike_spike2"), SHARD = t("spike_shard"), BURST = t("spike_burst");
    static final int STEEL = 0xFF9BA8B9, DARK = 0xFF3A404C, ICE = 0xFFCFEFFF, HOT = 0xFFF2FAFF;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.SPIKE_FX1, VfxShape.SPIKE_FX2, VfxShape.SPIKE_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case SPIKE_FX1 -> 16;
            case SPIKE_FX2 -> 80;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFF9BA8B9; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.SPIKE_FX3) VfxShake.add(inst.payload.from(), 0.35f * Mth.clamp(inst.power, 0.6f, 2f), 7);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case SPIKE_FX1 -> cast(inst, ctx, buf);
            case SPIKE_FX2 -> zone(inst, ctx, buf);
            case SPIKE_FX3 -> impact(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------------------------------------ helpers
    /** A palette colour pulled a quarter of the way toward the spell tint. */
    private static int mix(int palette, VfxInstance inst) { return VfxVertexBuffer.lerpColor(palette, inst.color | 0xFF000000, 0.25f); }

    private static void bb(VfxVertexBuffer b, VfxRenderContext ctx, ResourceLocation tex, VfxBlend bl, Vector3f p, float size, float rot, int col, float a) {
        if (a <= 0.01f || size <= 0.001f || !b.hasBudget(4)) return;
        b.billboard(ctx, tex, bl, p, size, rot, VfxVertexBuffer.withAlpha(col, Mth.clamp(a, 0, 1)));
    }

    private static Vector3f at(Vector3f o, float x, float y, float z) { return new Vector3f(o).add(x, y, z); }

    private static Vector3f lerp3(Vector3f a, Vector3f b, float t) { return new Vector3f(a).lerp(b, t); }

    /** Screen-plane angle of a world direction, so a billboard's x axis points along it. */
    private static float screenAngle(VfxRenderContext ctx, Vector3f d) { return (float) Math.atan2(d.dot(ctx.camUp), d.dot(ctx.camRight)); }

    private static float life(VfxInstance inst, float age, float in, float out) {
        float a = in <= 0 ? 1 : Mth.clamp(age / in, 0, 1), b = out <= 0 ? 1 : Mth.clamp((inst.duration - age) / out, 0, 1);
        return Math.min(a, b);
    }

    /** A vertical sprite standing on base (its bottom edge), facing the direction yaw, half width w, height h, leaning toward (lx, lz) by 'lean' of its height. */
    private static void stand(VfxVertexBuffer b, ResourceLocation tex, VfxBlend bl, Vector3f base, float yaw, float w, float h, float lx, float lz, float lean, int col) {
        if (h <= 0.01f || !b.hasBudget(4)) return;
        float ux = Mth.cos(yaw) * w, uz = Mth.sin(yaw) * w;
        Vector3f l = new Vector3f(base).add(-ux, 0, -uz), r = new Vector3f(base).add(ux, 0, uz);
        Vector3f top = new Vector3f(lx * lean * h, h, lz * lean * h);
        int c = bl.grade(col, 1f);
        b.quad(tex, bl, l, r, new Vector3f(r).add(top), new Vector3f(l).add(top), 0, 0, 1, 1, c, c);
    }

    private static float ease(float x) { return VfxAnim.easeOutCubic(Mth.clamp(x, 0f, 1f)); }

    // ------------------------------------------------------------------------------------------------ FX1: the Spike Lance
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1, inst.duration), t = Mth.clamp(age / dur, 0f, 1f);
        float p = Mth.clamp(inst.power, 0.5f, 3.2f);
        Vector3f f = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f d = new Vector3f(to).sub(f);
        float len = d.length();
        if (len < 0.3f) { d.set(0, 0, 1); len = 3f * p; to = new Vector3f(f).add(0, 0, len); } else d.div(len);
        int steel = mix(STEEL, inst), dark = mix(DARK, inst), ice = mix(ICE, inst);
        float rot = screenAngle(ctx, d) - 1.5708f;
        float cu = Mth.clamp(t / 0.22f, 0f, 1f), u = Mth.clamp((t - 0.14f) / 0.6f, 0f, 1f);
        float e = 0.7f * ease(u) + 0.3f * u, imp = Mth.clamp((t - 0.72f) / 0.28f, 0f, 1f), live = 1f - VfxAnim.easeInCubic(imp);
        Vector3f head = lerp3(f, to, e);
        float chargeA = cu * (1f - Mth.clamp((t - 0.2f) / 0.2f, 0f, 1f));
        bb(buf, ctx, SIGIL, VfxBlend.ADD, f, (2.2f - 1.5f * ease(cu)) * p, age * 0.15f, ice, chargeA);
        bb(buf, ctx, GLOW, VfxBlend.ADD, f, (0.8f + 0.7f * cu) * p, 0, ice, chargeA * 0.8f);
        bb(buf, ctx, FLARE, VfxBlend.ADD, f, (0.6f + 1.4f * cu) * p, 0.4f, HOT, chargeA * 0.9f);
        if (u > 0f || cu >= 1f) {
            float grow = VfxAnim.easeOutBack(Mth.clamp(t / 0.2f, 0f, 1f));
            Vector3f tail = lerp3(f, head, Mth.clamp(1f - 0.55f * (1f - u) - 0.25f, 0f, 0.9f));
            buf.beam(ctx, STREAK, VfxBlend.ADD, tail, head, 0.25f * p, 1.3f * p, 3, -age * 0.12f, VfxVertexBuffer.withAlpha(steel, 0f), VfxVertexBuffer.withAlpha(ice, 0.85f * live));
            for (int k = 2; k >= 1; k--) {
                float ek = 0.7f * ease(Math.max(0, u - 0.08f * k)) + 0.3f * Math.max(0, u - 0.08f * k);
                bb(buf, ctx, SPIKE, VfxBlend.ALPHA, lerp3(f, to, ek), 1.1f * p * grow, rot, VfxVertexBuffer.lerpColor(steel, dark, k / 3f), (0.5f - 0.15f * k) * live);
            }
            RandomSource r = inst.random();
            for (int k = 0; k < 4; k++) {
                float lag = 0.07f + 0.05f * k + r.nextFloat() * 0.03f, ek = 0.7f * ease(Math.max(0, u - lag)) + 0.3f * Math.max(0, u - lag);
                Vector3f cp = lerp3(f, to, ek);
                float off = (r.nextFloat() - 0.5f) * 0.9f * p, fall = (age - lag * dur) * 0.03f * p;
                cp.add(ctx.camRight.x * off, ctx.camRight.y * off - fall, ctx.camRight.z * off);
                bb(buf, ctx, SHARD, VfxBlend.ALPHA, cp, (0.2f + 0.1f * r.nextFloat()) * p, age * (0.3f + r.nextFloat() * 0.3f), steel, 0.9f * live);
            }
            bb(buf, ctx, GLOW, VfxBlend.ADD, head, 1.8f * p * grow, 0, ice, 0.5f * live);
            bb(buf, ctx, SPIKE, VfxBlend.ALPHA, head, 1.6f * p * grow, rot, steel, 0.98f * live);
            bb(buf, ctx, FLARE, VfxBlend.ADD, at(head, 0, 0, 0), 1.0f * p * grow, age * 0.3f, HOT, 0.8f * live);
        }
        if (imp > 0f) {
            float s = ease(imp);
            bb(buf, ctx, FLARE, VfxBlend.ADD, to, (0.8f + 2.2f * s) * p, 0.2f + imp, HOT, 1f - imp);
            bb(buf, ctx, BURST, VfxBlend.ALPHA, to, (0.5f + 1.8f * s) * p, 0f, steel, 0.95f * (1f - imp));
            bb(buf, ctx, RING, VfxBlend.ADD, to, (0.5f + 2.4f * s) * p, 0f, ice, 0.9f * (1f - imp));
            RandomSource r = inst.random();
            for (int k = 0; k < 4; k++) {
                float a = r.nextFloat() * Mth.TWO_PI, sp = (0.6f + r.nextFloat() * 0.8f) * p * s;
                Vector3f cp = new Vector3f(to).fma(Mth.cos(a) * sp, ctx.camRight).fma(Mth.sin(a) * sp, ctx.camUp);
                bb(buf, ctx, SHARD, VfxBlend.ALPHA, cp, 0.22f * p, a + age * 0.4f, steel, 1f - imp);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ FX2: the Impaling Field
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1, inst.duration);
        float R = Math.max(1f, inst.power), fade = life(inst, age, 8f, 12f);
        if (fade <= 0f) return;
        Vector3f c = ctx.rel(inst.from(ctx));
        int steel = mix(STEEL, inst), dark = mix(DARK, inst), ice = mix(ICE, inst);
        float open = VfxAnim.easeOutBack(Mth.clamp(age / 14f, 0f, 1f)), pulse = VfxAnim.pulse(age, 1.2f);
        VfxPose ground = VfxPose.ground(c);
        buf.plane(CRACK, VfxBlend.ALPHA, ground.lift(0.015f).spin(0.4f), R * 1.02f * open, VfxVertexBuffer.withAlpha(dark, 0.8f * fade));
        buf.plane(SIGIL, VfxBlend.ADD, ground.lift(0.03f).spin(age * 0.012f), R * open, VfxVertexBuffer.withAlpha(ice, (0.5f + 0.2f * pulse) * fade));
        buf.plane(CRACK, VfxBlend.ADD, ground.lift(0.06f).spin(0.4f), R * 0.9f * open, VfxVertexBuffer.withAlpha(ice, (0.2f + 0.25f * pulse) * fade));
        int seg = ctx.seg(16, 10);
        float band = Math.max(0.22f, R * 0.07f);
        buf.ring(RING, VfxBlend.ADD, ground.lift(0.07f), R * open - band, R * open, seg, Math.max(2, Math.round(Mth.TWO_PI * R / (6f * band))), age * 0.005f, VfxVertexBuffer.withAlpha(ice, 0.8f * fade));
        RandomSource rnd = inst.random();
        int n = Mth.clamp(Math.round(R * 4f), 8, 16);
        for (int i = 0; i < n; i++) {                                                             // spikes thrusting up, each on its own beat, then sinking, then again
            float a = rnd.nextFloat() * Mth.TWO_PI, rad = (i < n / 2 ? 0.95f : 0.2f + 0.7f * rnd.nextFloat()) * R;
            float beat = 26f + 8f * rnd.nextFloat(), ph = ((age + rnd.nextFloat() * beat) % beat) / beat;
            float h = (0.5f + 0.9f * rnd.nextFloat()) * Mth.clamp(R * 0.5f, 0.8f, 1.8f) * Mth.sin(Mth.clamp(ph * 1.6f, 0f, 1f) * Mth.PI) * open;
            Vector3f pos = at(c, Mth.cos(a) * rad, 0.04f, Mth.sin(a) * rad);
            stand(buf, (i & 1) == 0 ? SPIKE : SPIKE2, VfxBlend.ALPHA, pos, a + 0.9f, 0.42f + 0.1f * h, h, 0f, 0f, 0f, VfxVertexBuffer.withAlpha(steel, 0.96f * fade));
            bb(buf, ctx, GLOW, VfxBlend.ADD, at(pos, 0, h, 0), 0.35f, 0, ice, 0.5f * fade * Mth.clamp(h, 0f, 1f));
        }
        for (int i = 0; i < 8; i++) {
            float a = rnd.nextFloat() * Mth.TWO_PI, rad = (0.15f + 0.8f * rnd.nextFloat()) * R, sp = 0.012f + 0.012f * rnd.nextFloat();
            float life = ((age * sp * 8f + rnd.nextFloat()) % 1f), y = life * Mth.clamp(R * 0.9f, 1.2f, 3f);
            bb(buf, ctx, SHARD, VfxBlend.ADD, at(c, Mth.cos(a + age * 0.01f) * rad, 0.15f + y, Mth.sin(a + age * 0.01f) * rad), 0.2f, age * 0.1f + i, ice, Mth.sin(life * Mth.PI) * fade);
        }
        float ph = (age % 24f) / 24f;
        buf.plane(RING, VfxBlend.ADD, ground.lift(0.09f), R * (0.25f + 0.8f * ease(ph)), VfxVertexBuffer.withAlpha(HOT, (1f - ph) * 0.7f * fade));
    }

    // ------------------------------------------------------------------------------------------------ FX3: the Spike Burst
    private void impact(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1, inst.duration), t = Mth.clamp(age / dur, 0f, 1f);
        float p = Mth.clamp(inst.power, 0.5f, 3.2f);
        Vector3f c = ctx.rel(inst.from(ctx));
        int steel = mix(STEEL, inst), dark = mix(DARK, inst), ice = mix(ICE, inst);
        VfxPose ground = VfxPose.ground(c);
        float s = ease(t * 1.6f), flashA = Mth.clamp(1f - t * 3.4f, 0f, 1f), fadeC = 1f - VfxAnim.easeInCubic(Mth.clamp((t - 0.45f) / 0.55f, 0f, 1f));
        buf.plane(CRACK, VfxBlend.ALPHA, ground.lift(0.02f).spin(0.3f), 2.2f * p * (0.3f + 0.7f * s), VfxVertexBuffer.withAlpha(dark, 0.85f * fadeC));
        buf.plane(CRACK, VfxBlend.ADD, ground.lift(0.04f).spin(0.3f), 2.2f * p * (0.3f + 0.7f * s), VfxVertexBuffer.withAlpha(ice, (1f - t) * 0.85f));
        buf.plane(RING, VfxBlend.ADD, ground.lift(0.07f), (0.3f + 3.0f * ease(t)) * p, VfxVertexBuffer.withAlpha(ice, (1f - t) * 0.95f));
        buf.plane(RING, VfxBlend.ADD, ground.lift(0.08f).spin(1f), (0.2f + 1.8f * ease(t * 1.5f)) * p, VfxVertexBuffer.withAlpha(HOT, flashA));
        bb(buf, ctx, FLARE, VfxBlend.ADD, at(c, 0, 0.4f * p, 0), (1.4f + 5.0f * s) * p, 0.35f + t, HOT, flashA);
        bb(buf, ctx, GLOW, VfxBlend.ADD, at(c, 0, 0.5f * p, 0), (1.4f + 2.4f * s) * p, 0, ice, (1f - t) * 0.7f);
        int n = 10;
        for (int i = 0; i < n; i++) {                                                             // a star of spikes thrown out every way, hanging, then sinking
            float a = Mth.TWO_PI * i / n + 0.2f, el = (i % 2 == 0 ? 0.35f : 0.9f), out = (0.6f + 1.5f * s) * p, len = (0.9f + 0.5f * (i % 3)) * p * ease(t * 3f) * (1f - VfxAnim.easeInCubic(Mth.clamp((t - 0.55f) / 0.45f, 0f, 1f)));
            Vector3f dir = new Vector3f(Mth.cos(a) * Mth.cos(el), Mth.sin(el), Mth.sin(a) * Mth.cos(el));
            Vector3f pos = at(c, dir.x * out * 0.5f, 0.1f + dir.y * out * 0.5f, dir.z * out * 0.5f);
            float rot = screenAngle(ctx, dir) - 1.5708f;
            bb(buf, ctx, i % 2 == 0 ? SPIKE : SPIKE2, VfxBlend.ALPHA, at(pos, dir.x * len * 0.4f, dir.y * len * 0.4f, dir.z * len * 0.4f), len, rot, steel, 0.95f * fadeC);
        }
        RandomSource r = inst.random();
        float sec = age / 20f;
        for (int i = 0; i < 8; i++) {
            float az = r.nextFloat() * Mth.TWO_PI, sp = (1.0f + r.nextFloat() * 1.5f) * p, vy = (3.0f + r.nextFloat() * 3.0f) * p, spin = (r.nextFloat() - 0.5f) * 0.6f;
            float hor = sp * (1f - (float) Math.exp(-3.2f * sec)) / 3.2f * 2.2f, y = Math.max(0f, vy * sec - 5.5f * p * sec * sec);
            Vector3f pos = at(c, Mth.cos(az) * hor, 0.15f + y, Mth.sin(az) * hor);
            float al = 1f - VfxAnim.easeInCubic(Mth.clamp((t - 0.55f) / 0.45f, 0f, 1f));
            bb(buf, ctx, SHARD, VfxBlend.ALPHA, pos, 0.3f * p, age * spin + i, steel, al);
        }
    }
}
