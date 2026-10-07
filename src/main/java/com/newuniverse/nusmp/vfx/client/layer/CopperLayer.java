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
 * 0.54 Copper Magic (Red Shine Mace), after the owner's still: a long polearm thrust forward with a ragged yellow-orange blaze
 * burning at its tip, thick-lined faceted metal slabs, vertical speed lines, grey stone behind. Textures from
 * tools/gen_copper_textures.py (copper_*): cel-shaded faceted mace head and chunks, hammered rivet ring, plate, gear sigil,
 * ground cracks, blaze, speed streaks, flecks, flares.
 * <ul>
 *   <li>COPPER_FX1 (cast / projectile, default 16 ticks): the mace thrust. A short charge at 'from' (the blaze swells while a
 *       thin ring contracts onto the hand), then the faceted mace head with its blaze rockets along from -> to, towing a
 *       dark ringed shaft, a fan of speed streaks, three ghost afterimages and spinning copper chips, and ends in a flare and
 *       a sharp shock ring at 'to'. power = size (0.6 to 3), colour = tint of the blaze and the metal (mixed with copper).</li>
 *   <li>COPPER_FX2 (zone / field, default 80 ticks): the Copper Plate Field. A toothed gear sigil turns on the ground, glowing
 *       through a counter-rotating copy, a hammered rivet ring marks the rim (radius = power) with a second ring inside,
 *       riveted copper plates thrust out of the ground around the rim and stand like a palisade with glints on top, a
 *       column of speed lines rises in the middle, flecks of hot metal float up, and a shock ring pulses outward.
 *       Fades in over 8 ticks and out over the last 12. from = centre on the ground, power = RADIUS in blocks.</li>
 *   <li>COPPER_FX3 (impact / signature, default 28 ticks): the Red Shine slam. A giant faceted mace head drops onto 'from'
 *       (from the opposite side of the hint 'to - from', straight down when there is none), a white-yellow flare and blaze
 *       burst on contact, the ground cracks, two shock rings run out (one flat on the ground, one standing on the hit
 *       axis), copper chunks and chips fly out in arcs, grey dust rolls, then an ember afterglow lingers and the screen
 *       shakes. power = scale.</li>
 * </ul>
 */
public class CopperLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    static final ResourceLocation HEAD = t("copper_mace_head"), SHARD = t("copper_shard"), CHUNK = t("copper_chunk"),
            BLAZE = t("copper_blaze"), STREAK = t("copper_streak"), SHAFT = t("copper_shaft"), RING = t("copper_ring"),
            SIGIL = t("copper_sigil"), CRACK = t("copper_crack"), FLARE = t("copper_flare"), FLECKS = t("copper_flecks"),
            PLATE = t("copper_plate"), SLAM = t("copper_slam_ring");

    static final int COPPER = 0xFFC8702E, DEEP = 0xFF7A3414, ORANGE = 0xFFFF9A4A, YELLOW = 0xFFFFD25A, HOT = 0xFFFFF2C8, DUST = 0xFF8A8478;

    @Override
    public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.COPPER_FX1, VfxShape.COPPER_FX2, VfxShape.COPPER_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case COPPER_FX1 -> 16;
            case COPPER_FX2 -> 80;
            default -> 28;
        };
    }

    @Override
    public int defaultColor(VfxShape s) { return 0xFFFF9A4A; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.COPPER_FX3) VfxShake.add(inst.payload.from(), 0.3f * Mth.clamp(inst.power, 0.6f, 2f), 7);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case COPPER_FX1 -> cast(inst, ctx, buf);
            case COPPER_FX2 -> field(inst, ctx, buf);
            case COPPER_FX3 -> slam(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------------------------------------ helpers
    /** Palette colour pulled a third of the way toward the spell tint, so the effect reads as copper even for a white tint. */
    private static int mix(int palette, VfxInstance inst) { return VfxVertexBuffer.lerpColor(palette, inst.color | 0xFF000000, 0.28f); }

    private static void bb(VfxVertexBuffer b, VfxRenderContext ctx, ResourceLocation tex, VfxBlend bl, Vector3f p, float size, float rot, int col, float a) {
        if (a <= 0.01f || size <= 0.001f || !b.hasBudget(4)) return;
        b.billboard(ctx, tex, bl, p, size, rot, VfxVertexBuffer.withAlpha(col, Mth.clamp(a, 0, 1)));
    }

    private static Vector3f at(Vector3f o, float x, float y, float z) { return new Vector3f(o).add(x, y, z); }

    private static Vector3f lerp3(Vector3f a, Vector3f b, float t) { return new Vector3f(a).lerp(b, t); }

    /** Screen-plane angle of a world direction, so a billboard's x axis points along it. */
    private static float screenAngle(VfxRenderContext ctx, Vector3f d) { return (float) Math.atan2(d.dot(ctx.camUp), d.dot(ctx.camRight)); }

    /** A vertical panel standing on the ground at p, facing along -tangent normal, width w, height h, leaning by 'lean' toward 'inward'. */
    private static void panel(VfxVertexBuffer b, ResourceLocation tex, VfxBlend bl, Vector3f p, Vector3f tan, Vector3f inward, float w, float h, float lean, int col) {
        if (!b.hasBudget(4) || h <= 0.01f) return;
        Vector3f l = new Vector3f(tan).mul(-w / 2), r = new Vector3f(tan).mul(w / 2), top = new Vector3f(0, h, 0).add(new Vector3f(inward).mul(lean * h));
        int c = bl.grade(col, 1f);
        b.quad(tex, bl, new Vector3f(p).add(l), new Vector3f(p).add(r), new Vector3f(p).add(r).add(top), new Vector3f(p).add(l).add(top), 0, 0, 1, 1, c, c);
    }

    // ------------------------------------------------------------------------------------------------ FX1
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1, inst.duration), t = Mth.clamp(age / dur, 0f, 1f);
        float p = Mth.clamp(inst.power, 0.5f, 3.2f);
        Vector3f f = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f d = new Vector3f(to).sub(f);
        float len = d.length();
        if (len < 0.3f) { d.set(0, 0, 1); len = 3f * p; to = new Vector3f(f).add(0, 0, len); } else d.div(len);
        int blaze = mix(YELLOW, inst), glow = mix(ORANGE, inst), metal = mix(COPPER, inst), deep = mix(DEEP, inst);
        float rot = screenAngle(ctx, d);

        float cu = Mth.clamp(t / 0.22f, 0f, 1f);                       // charge 0..1
        float u = Mth.clamp((t - 0.14f) / 0.6f, 0f, 1f);               // flight 0..1
        float e = 0.7f * VfxAnim.easeOutCubic(u) + 0.3f * u;           // fast launch, no slow tail
        float imp = Mth.clamp((t - 0.72f) / 0.28f, 0f, 1f);            // impact 0..1
        float live = 1f - VfxAnim.easeInCubic(imp);                    // projectile fades during the impact
        Vector3f head = lerp3(f, to, e);
        float flick = 0.88f + 0.12f * Mth.sin(age * 2.3f);

        // back: charge glow + contracting ring on the hand
        float chargeA = cu * (1f - Mth.clamp((t - 0.2f) / 0.2f, 0f, 1f));
        bb(buf, ctx, VfxTextures.GLOW, VfxBlend.ADD, f, (0.9f + 0.7f * cu) * p, 0, glow, chargeA * 0.8f);
        bb(buf, ctx, SLAM, VfxBlend.ADD, f, (2.3f - 1.7f * VfxAnim.easeOutCubic(cu)) * p, age * 0.12f, blaze, chargeA);
        bb(buf, ctx, FLARE, VfxBlend.ADD, f, (0.6f + 1.4f * cu) * p, 0.4f, HOT, chargeA * 0.9f);

        if (u > 0f || cu >= 1f) {
            float grow = VfxAnim.easeOutBack(Mth.clamp(t / 0.2f, 0f, 1f));
            // speed streaks: a fan of thin lines, thin at the tail, wide at the head
            Vector3f tail = lerp3(f, head, Mth.clamp(1f - 0.55f * (1f - u) - 0.25f, 0f, 0.9f));
            buf.beam(ctx, STREAK, VfxBlend.ADD, tail, head, 0.25f * p, 1.5f * p, 3, -age * 0.12f, VfxVertexBuffer.withAlpha(glow, 0f), VfxVertexBuffer.withAlpha(blaze, 0.85f * live));
            // the shaft: dark ringed metal pole behind the head
            float shaftLen = Math.min(e * len, 2.6f * p);
            Vector3f shaftTail = new Vector3f(head).fma(-shaftLen, d);
            buf.beam(ctx, SHAFT, VfxBlend.ALPHA, shaftTail, head, 0.17f * p, 0.17f * p, 2, 0f, VfxVertexBuffer.withAlpha(deep, 0.95f * live), VfxVertexBuffer.withAlpha(metal, 0.95f * live));

            // afterimages (ghosts of the mace head, each older one dimmer and more orange)
            for (int k = 3; k >= 1; k--) {
                float ek = 0.7f * VfxAnim.easeOutCubic(Math.max(0, u - 0.07f * k)) + 0.3f * Math.max(0, u - 0.07f * k);
                Vector3f g = lerp3(f, to, ek);
                bb(buf, ctx, HEAD, VfxBlend.ALPHA, g, 0.85f * p * grow, rot + 0.2f * k, VfxVertexBuffer.lerpColor(metal, glow, k / 3f), (0.42f - 0.1f * k) * live);
            }
            // trailing chips
            RandomSource r = inst.random();
            for (int k = 0; k < 4; k++) {
                float lag = 0.07f + 0.05f * k + r.nextFloat() * 0.03f;
                float ek = 0.7f * VfxAnim.easeOutCubic(Math.max(0, u - lag)) + 0.3f * Math.max(0, u - lag);
                Vector3f cp = lerp3(f, to, ek);
                float off = (r.nextFloat() - 0.5f) * 0.9f * p, fall = (age - lag * dur) * 0.03f * p;
                cp.add(ctx.camRight.x * off, ctx.camRight.y * off - fall + (r.nextFloat() - 0.5f) * 0.2f * p, ctx.camRight.z * off);
                float spin = age * (0.3f + r.nextFloat() * 0.3f);
                bb(buf, ctx, (k & 1) == 0 ? SHARD : CHUNK, VfxBlend.ALPHA, cp, (0.22f + 0.1f * r.nextFloat()) * p, spin, metal, 0.9f * live);
            }
            bb(buf, ctx, FLECKS, VfxBlend.ADD, lerp3(f, head, 0.75f), 1.0f * p, age * 0.2f, blaze, 0.35f * live);

            // the head: faceted slab inside its blaze
            bb(buf, ctx, VfxTextures.GLOW, VfxBlend.ADD, head, 2.1f * p * grow, 0, glow, 0.55f * live);
            bb(buf, ctx, HEAD, VfxBlend.ALPHA, head, 1.25f * p * grow, rot, metal, 0.98f * live);
            bb(buf, ctx, BLAZE, VfxBlend.ADD, head, 1.5f * p * grow * flick, -age * 0.35f, glow, 0.8f * live);
            bb(buf, ctx, BLAZE, VfxBlend.ADD, head, 1.05f * p * grow, age * 0.5f, HOT, 0.95f * live);
        }

        // impact at 'to': flare + hard ring + a few chips
        if (imp > 0f) {
            float s = VfxAnim.easeOutCubic(imp);
            bb(buf, ctx, FLARE, VfxBlend.ADD, to, (0.8f + 2.4f * s) * p, 0.2f + imp, HOT, 1f - imp);
            bb(buf, ctx, SLAM, VfxBlend.ADD, to, (0.5f + 2.6f * s) * p, 0.5f, blaze, 0.95f * (1f - imp));
            bb(buf, ctx, BLAZE, VfxBlend.ADD, to, (0.7f + 1.0f * s) * p, age * 0.4f, glow, 0.9f * (1f - imp));
            RandomSource r = inst.random();
            for (int k = 0; k < 4; k++) {
                float a = r.nextFloat() * Mth.TWO_PI, sp = (0.6f + r.nextFloat() * 0.8f) * p * s;
                Vector3f cp = new Vector3f(to).fma(Mth.cos(a) * sp, ctx.camRight).fma(Mth.sin(a) * sp, ctx.camUp);
                bb(buf, ctx, (k & 1) == 0 ? SHARD : CHUNK, VfxBlend.ALPHA, cp, 0.2f * p, a + age * 0.4f, metal, 1f - imp);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ FX2
    private void field(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1, inst.duration);
        float R = Math.max(1f, inst.power);
        float fade = Mth.clamp(Math.min(age / 8f, (dur - age) / 12f), 0f, 1f);
        if (fade <= 0f) return;
        Vector3f c = ctx.rel(inst.from(ctx));
        int glow = mix(ORANGE, inst), metal = mix(COPPER, inst), blaze = mix(YELLOW, inst), deep = mix(DEEP, inst);
        float open = VfxAnim.easeOutBack(Mth.clamp(age / 14f, 0f, 1f));
        float pulse = VfxAnim.pulse(age, 1.1f);
        VfxPose ground = VfxPose.ground(c);

        // 1 ground: cracked earth under a toothed gear sigil, the sigil's glow copy turning the other way
        buf.plane(CRACK, VfxBlend.ALPHA, ground.lift(0.015f).spin(1.1f), R * 1.02f * open, VfxVertexBuffer.withAlpha(deep, 0.75f * fade));
        buf.plane(SIGIL, VfxBlend.ALPHA, ground.lift(0.03f).spin(age * 0.012f), R * open, VfxVertexBuffer.withAlpha(metal, 0.92f * fade));
        buf.plane(SIGIL, VfxBlend.ADD, ground.lift(0.05f).spin(-age * 0.02f), R * 0.88f * open, VfxVertexBuffer.withAlpha(glow, (0.3f + 0.3f * pulse) * fade));
        buf.plane(CRACK, VfxBlend.ADD, ground.lift(0.06f).spin(1.1f), R * 0.9f * open, VfxVertexBuffer.withAlpha(blaze, (0.25f + 0.25f * pulse) * fade));

        // 2 hammered rivet rings: rim (turns slowly) and a narrower one inside
        int seg = ctx.seg(16, 10);
        float band = Math.max(0.28f, R * 0.09f);
        float circ = Mth.TWO_PI * R;
        buf.ring(RING, VfxBlend.ALPHA, ground.lift(0.07f), R * open - band, R * open, seg, Math.max(2, Math.round(circ / (8f * band))), age * 0.004f, VfxVertexBuffer.withAlpha(metal, 0.95f * fade));
        buf.ring(RING, VfxBlend.ADD, ground.lift(0.08f), R * 0.58f * open - band * 0.5f, R * 0.58f * open, ctx.seg(12, 8),
                Math.max(2, Math.round(Mth.TWO_PI * R * 0.58f / (8f * band * 0.5f))), -age * 0.006f, VfxVertexBuffer.withAlpha(glow, 0.5f * fade));

        // 3 palisade: riveted plates thrust out of the ground around the rim, with glints on their tops
        int n = Mth.clamp(Math.round(R * 1.8f), 6, 10);
        float w = Math.min(1.9f, Mth.TWO_PI * R / n * 0.86f), hMax = Mth.clamp(R * 0.55f, 0.9f, 2.4f);
        RandomSource rnd = inst.random();
        for (int i = 0; i < n; i++) {
            float a = Mth.TWO_PI * i / n + 0.2f;
            float ri = VfxAnim.easeOutBack(Mth.clamp((age - 3f - i * 1.1f) / 9f, 0f, 1f));
            float hh = hMax * (0.8f + 0.2f * rnd.nextFloat()) * ri;
            Vector3f pos = at(c, Mth.cos(a) * R * 0.96f, 0.05f, Mth.sin(a) * R * 0.96f);
            Vector3f tan = new Vector3f(-Mth.sin(a), 0, Mth.cos(a)), inward = new Vector3f(-Mth.cos(a), 0, -Mth.sin(a));
            panel(buf, PLATE, VfxBlend.ALPHA, pos, tan, inward, w, hh, 0.12f, VfxVertexBuffer.withAlpha(metal, 0.95f * fade));
            float gl = Mth.clamp(0.5f + 0.5f * Mth.sin(age * 0.18f + i * 1.7f), 0, 1);
            bb(buf, ctx, FLARE, VfxBlend.ADD, at(pos, 0, hh, 0).fma(-0.12f * hh, new Vector3f(Mth.cos(a), 0, Mth.sin(a))), 0.55f * w * ri, i + age * 0.05f, blaze, fade * (0.25f + 0.55f * gl));
        }

        // 4 column of speed lines in the middle + a slow rising blaze
        Vector3f base = at(c, 0, 0.05f, 0), top = at(c, 0, Mth.clamp(R * 0.9f, 1.2f, 3.4f) * open, 0);
        buf.beam(ctx, STREAK, VfxBlend.ADD, base, top, Math.min(1.8f, R * 0.5f) * open, 0.25f, 2, -age * 0.07f, VfxVertexBuffer.withAlpha(glow, 0.6f * fade), VfxVertexBuffer.withAlpha(blaze, 0f));
        bb(buf, ctx, BLAZE, VfxBlend.ADD, at(c, 0, 0.35f, 0), Math.min(2f, R * 0.5f) * (0.8f + 0.2f * pulse) * open, age * 0.15f, glow, 0.55f * fade);

        // 5 rising flecks of hot metal
        for (int i = 0; i < 12; i++) {
            float a = rnd.nextFloat() * Mth.TWO_PI, rad = (0.15f + 0.8f * rnd.nextFloat()) * R;
            float sp = 0.012f + 0.014f * rnd.nextFloat(), life = ((age * sp * 8f + rnd.nextFloat()) % 1f);
            float y = life * Mth.clamp(R * 0.9f, 1.2f, 3f);
            float al = Mth.sin(life * Mth.PI) * fade;
            bb(buf, ctx, (i & 3) == 0 ? SHARD : FLECKS, VfxBlend.ADD, at(c, Mth.cos(a + age * 0.01f) * rad, 0.15f + y, Mth.sin(a + age * 0.01f) * rad),
                    (i & 3) == 0 ? 0.18f : 0.5f, age * 0.1f + i, blaze, al * 0.9f);
        }

        // 6 pulse: a hard shock ring runs out to the rim every ~25 ticks
        float ph = (age % 26f) / 26f;
        buf.plane(SLAM, VfxBlend.ADD, ground.lift(0.09f), R * (0.25f + 0.8f * VfxAnim.easeOutCubic(ph)), VfxVertexBuffer.withAlpha(blaze, (1f - ph) * 0.8f * fade));
    }

    // ------------------------------------------------------------------------------------------------ FX3
    private void slam(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1, inst.duration), t = Mth.clamp(age / dur, 0f, 1f);
        float p = Mth.clamp(inst.power, 0.5f, 3.2f);
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f hint = new Vector3f(ctx.rel(inst.to(ctx))).sub(c);
        Vector3f axis = hint.lengthSquared() < 0.04f ? new Vector3f(0, -1, 0) : hint.normalize();
        int glow = mix(ORANGE, inst), metal = mix(COPPER, inst), blaze = mix(YELLOW, inst), deep = mix(DEEP, inst);
        VfxPose ground = VfxPose.ground(c);

        float hitT = 0.2f;                                  // the head lands at 20 % of the life
        float after = Mth.clamp((t - hitT) / (1f - hitT), 0f, 1f);
        float s = VfxAnim.easeOutCubic(after);
        float flashA = Mth.clamp(1f - after * 3.2f, 0f, 1f);

        // ground damage: crack (dark, then glowing as it cools) and a scorch of glow
        if (t >= hitT) {
            float fadeC = 1f - VfxAnim.easeInCubic(Mth.clamp((after - 0.4f) / 0.6f, 0f, 1f));
            buf.plane(CRACK, VfxBlend.ALPHA, ground.lift(0.02f).spin(0.6f), 2.4f * p * (0.3f + 0.7f * s), VfxVertexBuffer.withAlpha(deep, 0.85f * fadeC));
            buf.plane(CRACK, VfxBlend.ADD, ground.lift(0.04f).spin(0.6f), 2.4f * p * (0.3f + 0.7f * s), VfxVertexBuffer.withAlpha(blaze, (1f - after) * 0.9f));
            buf.plane(VfxTextures.GLOW, VfxBlend.ADD, ground.lift(0.05f), 2.2f * p, VfxVertexBuffer.withAlpha(glow, (1f - after) * 0.5f));
            buf.plane(SLAM, VfxBlend.ADD, ground.lift(0.07f), (0.3f + 3.0f * s) * p, VfxVertexBuffer.withAlpha(blaze, (1f - after) * 0.95f));
            buf.plane(SLAM, VfxBlend.ADD, ground.lift(0.08f).spin(1f), (0.2f + 1.9f * VfxAnim.easeOutCubic(Mth.clamp(after * 1.5f, 0, 1))) * p, VfxVertexBuffer.withAlpha(HOT, flashA));
            // standing ring on the hit axis
            buf.plane(SLAM, VfxBlend.ADD, VfxPose.facing(c, new Vector3f(axis).negate()), (0.3f + 2.2f * s) * p, VfxVertexBuffer.withAlpha(glow, (1f - after) * 0.85f));
        }

        // anticipation: the giant mace head drops along the hit axis (fast, accelerating), motion streak behind it
        if (t < hitT + 0.08f) {
            float fall = Mth.clamp(t / hitT, 0f, 1f);
            float fe = VfxAnim.easeInCubic(fall);
            Vector3f from = new Vector3f(c).fma(-4.2f * p, axis);
            Vector3f hp = lerp3(from, c, fe);
            float vis = t < hitT ? 1f : 1f - (t - hitT) / 0.08f;
            float rot = screenAngle(ctx, axis) + 0.5f;
            buf.beam(ctx, STREAK, VfxBlend.ADD, from, hp, 0.2f * p, 1.7f * p, 3, -age * 0.2f, VfxVertexBuffer.withAlpha(glow, 0f), VfxVertexBuffer.withAlpha(blaze, 0.8f * Mth.clamp(fall * 3f, 0, 1) * vis));
            bb(buf, ctx, VfxTextures.GLOW, VfxBlend.ADD, hp, 3.2f * p, 0, glow, 0.5f * fall * vis);
            bb(buf, ctx, HEAD, VfxBlend.ALPHA, hp, 2.0f * p, rot, metal, vis * Mth.clamp(fall * 4f, 0, 1));
            bb(buf, ctx, BLAZE, VfxBlend.ADD, hp, 2.6f * p, -age * 0.4f, glow, 0.75f * fall * vis);
            bb(buf, ctx, BLAZE, VfxBlend.ADD, hp, 1.4f * p, age * 0.5f, HOT, 0.9f * fall * vis);
        }

        if (t >= hitT) {
            // the burst: flare + blaze at the contact point, a huge short flash
            bb(buf, ctx, FLARE, VfxBlend.ADD, at(c, 0, 0.3f * p, 0), (1.5f + 5.5f * s) * p, 0.35f + after, HOT, flashA);
            bb(buf, ctx, BLAZE, VfxBlend.ADD, at(c, 0, 0.5f * p, 0), (1.4f + 2.6f * s) * p, age * 0.3f, blaze, (1f - after) * 0.95f);
            bb(buf, ctx, BLAZE, VfxBlend.ADD, at(c, 0, 0.4f * p, 0), (2.6f + 2.2f * s) * p, -age * 0.2f, glow, (1f - after) * 0.6f);
            // grey dust rolling out
            for (int i = 0; i < 4; i++) {
                float a = i * 1.7f + 0.4f;
                float rr = (0.3f + 1.7f * s) * p * (0.6f + 0.1f * i);
                bb(buf, ctx, VfxTextures.GLOW, VfxBlend.ALPHA, at(c, Mth.cos(a) * rr, 0.25f * p + 0.5f * s * p, Mth.sin(a) * rr), (0.9f + 1.0f * s) * p, 0, DUST, 0.42f * (1f - after) * Mth.clamp(after * 6f, 0, 1));
            }
            // debris: faceted copper chunks and chips in arcs, hot at first
            RandomSource r = inst.random();
            float sec = (age - hitT * dur) / 20f;
            for (int i = 0; i < 10; i++) {
                float az = r.nextFloat() * Mth.TWO_PI, sp = (1.0f + r.nextFloat() * 1.6f) * p, vy = (3.2f + r.nextFloat() * 3.2f) * p;
                float spin = (r.nextFloat() - 0.5f) * 0.6f;
                float tt = Math.max(0f, sec);
                float hor = sp * (1f - (float) Math.exp(-3.2f * tt)) / 3.2f * 2.2f;
                float y = vy * tt - 5.5f * p * tt * tt;
                if (y < 0f) y = 0f;
                Vector3f pos = at(c, Mth.cos(az) * hor, 0.15f + y, Mth.sin(az) * hor);
                boolean big = i < 5;
                float al = 1f - VfxAnim.easeInCubic(Mth.clamp((after - 0.55f) / 0.45f, 0f, 1f));
                bb(buf, ctx, big ? CHUNK : SHARD, VfxBlend.ALPHA, pos, (big ? 0.5f : 0.3f) * p, age * spin + i, VfxVertexBuffer.lerpColor(metal, glow, Math.max(0f, 1f - after * 2.5f)), al);
                if (!big) bb(buf, ctx, VfxTextures.GLOW, VfxBlend.ADD, pos, 0.5f * p, 0, blaze, al * Math.max(0f, 1f - after * 2f) * 0.8f);
            }
            // sparks + afterglow embers
            bb(buf, ctx, FLECKS, VfxBlend.ADD, at(c, 0, 0.6f * p * (0.4f + s), 0), (2.6f + 3.0f * s) * p, age * 0.05f, blaze, (1f - after) * 0.9f);
            for (int i = 0; i < 4; i++) {
                float a = i * 1.57f + 0.5f, y = after * (1.0f + 0.4f * i) * p;
                bb(buf, ctx, FLECKS, VfxBlend.ADD, at(c, Mth.cos(a) * 0.6f * p, 0.3f + y, Mth.sin(a) * 0.6f * p), 0.9f * p, age * 0.08f + i, glow, Mth.clamp(after * 4f, 0, 1) * (1f - after) * 0.8f);
            }
        }
    }
}
