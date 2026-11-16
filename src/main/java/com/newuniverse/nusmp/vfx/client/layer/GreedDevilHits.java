package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

import static com.newuniverse.nusmp.vfx.client.layer.GreedLayer.*;

/**
 * Why: a devil's combat hit (DEVIL_HIT, sent at most once a second per host by {@code devil.DevilHitFx}) drawn in the style of the anime its devil comes from, instead of one tinted mote spray
 * (docs/vfx_remake/greed_devils.md). from = the target's chest, to = the attacker's chest, power = the world ({@link GreedVfx#devilWorld}), colour = the devil's own colour (used for the accent; the
 * signature piece keeps its world's colour). Drawn by {@link GreedLayer} with its textures and helpers. Worst case measured by the preview 46 quads (Tensura; Chainsaw Man 42), every loop a {@code ctx.seg} of at most 12.
 * <ul>
 *   <li>Black Clover: a crack in space breaks open over the target, ink with a black-red glow, the devil's ring round it (the devils of the underworld tearing through).</li>
 *   <li>Chainsaw Man: a saw-tooth rip slashes across, blood drops arc out and fall, orange sparks.</li>
 *   <li>Black Butler: the contract pentagram glows violet over the target, black feathers burst out and drift down.</li>
 *   <li>Blue Exorcist: blue flames (Satan's flames) leap up round the target, a ring of blue fire.</li>
 *   <li>Tensura: a demonic aura pulses out in purple waves from a dark core, motes rise.</li>
 *   <li>Dororo: pale ghost fires (onibi) circle the target, bobbing.</li>
 *   <li>Berserk: the Brand of Sacrifice bleeds red on the target, a black eclipse with a red corona hangs over it.</li>
 *   <li>Fairy Tail: two bands of Etherious curse runes turn round the target, a dark curse crack under them.</li>
 * </ul>
 */
final class GreedDevilHits {
    private GreedDevilHits() {}

    private static final int BC_RED = 0xFFB0101E;
    private static final int SAW = 0xFF4A4448;
    private static final int SPARK = 0xFFFFA040;
    private static final int BLOOD_RED = 0xFF8A0610;
    private static final int CONTRACT = 0xFFA060FF;
    private static final int SATAN_BLUE = 0xFF2A6BFF;
    private static final int DAEMON = 0xFF8A2AD8;
    private static final int ONIBI = 0xFFA8F0FF;
    private static final int BRAND_RED = 0xFFE01020;
    private static final int CURSE = 0xFF7A3CFF;

    static void draw(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        Vector3f c = ctx.rel(inst.from(ctx)), atk = ctx.rel(inst.to(ctx));
        Vector3f blow = dirOr(atk, c, new Vector3f(0, 0, 1));
        float fade = life(inst, age, 0, 6);
        RandomSource r = inst.random();
        if (age < 4) flash(ctx, buf, c, 1.1f, r.nextFloat() * Mth.TWO_PI, col, 1 - age / 4f);
        switch (GreedVfx.devilWorld(inst.power)) {
            case 0 -> blackClover(ctx, buf, c, blow, age, col, fade);
            case 1 -> chainsaw(ctx, buf, c, blow, age, inst.duration, fade, r);
            case 2 -> butler(ctx, buf, c, age, fade, r);
            case 3 -> exorcist(ctx, buf, c, age, col, fade, r);
            case 4 -> tensura(ctx, buf, c, age, col, fade, r);
            case 5 -> dororo(ctx, buf, c, age, fade);
            case 6 -> berserk(ctx, buf, c, age, fade);
            default -> fairyTail(ctx, buf, c, age, col, fade);
        }
    }

    /** Black Clover: the crack in space. ~20 quads. */
    private static void blackClover(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, Vector3f blow, float age, int col, float fade) {
        float open = VfxAnim.easeOutBack(Mth.clamp(age / 3f, 0, 1)), s = 1.8f * open;
        VfxPose face = VfxPose.facing(new Vector3f(blow).mul(-0.35f).add(c), new Vector3f(blow).negate());
        buf.plane(CRACK, VfxBlend.ALPHA, face, s, al(INK, 0.95f * fade));
        buf.plane(CRACK, VfxBlend.ADD, face.lift(0.02f), s * 1.06f, al(BC_RED, (0.6f + 0.4f * VfxAnim.pulse(age, 4f)) * fade));
        buf.ring(RING, VfxBlend.ADD, face, s * 0.8f, s, ctx.seg(14, 8), 3, age * 0.05f, al(col, 0.8f * fade));
        buf.billboard(ctx, GLOW, VfxBlend.ADD, c, s * 1.8f, 0, al(BC_RED, 0.35f * fade));
    }

    /** Chainsaw Man: the rip and the blood. ~48 quads. */
    private static void chainsaw(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, Vector3f blow, float age, int duration, float fade, RandomSource r) {
        float spin = r.nextFloat() * Mth.TWO_PI, grow = VfxAnim.easeOutCubic(Mth.clamp(age / 3f, 0, 1)), rip = Mth.clamp(1 - age / 9f, 0, 1);
        VfxPose pose = crescentPose(c, blow, spin, 1.3f);
        int segs = ctx.seg(10, 5);
        if (rip > 0) {
            buf.arc(TEETH, VfxBlend.ALPHA, pose, 1.3f, 0.9f, -1.3f, 2.6f * grow, segs, al(SAW, rip));
            buf.arc(SLASH, VfxBlend.ADD, pose, 1.6f, 0.3f, -1.3f, 2.6f * grow, segs, al(SPARK, rip));
        }
        int drops = ctx.seg(12, 6);
        for (int i = 0; i < drops; i++) {
            Vector3f v = randomDir(r);
            v.add(new Vector3f(blow).mul(0.8f)).normalize().mul(0.09f + 0.08f * r.nextFloat());
            v.y = Math.abs(v.y) * 0.8f + 0.1f;
            float t = age, size = 0.3f + 0.3f * r.nextFloat(), rot = r.nextFloat() * Mth.TWO_PI, t0 = Math.max(0, t - 2.5f);
            Vector3f at = new Vector3f(v).mul(t).add(c).add(0, -0.02f * t * t, 0);
            float a = Mth.clamp(1 - t / duration, 0, 1);
            buf.billboard(ctx, BLOOD, VfxBlend.ALPHA, at, size, rot, al(BLOOD_RED, a));
            if (i % 3 == 0) buf.beam(ctx, STREAK, VfxBlend.ALPHA, new Vector3f(v).mul(t0).add(c).add(0, -0.02f * t0 * t0, 0), at, 0.1f, 0.18f, 1, 0, al(BLOOD_RED, 0.2f * a), al(BLOOD_RED, 0.85f * a));
        }
        int sparks = ctx.seg(4, 2);
        for (int i = 0; i < sparks; i++) {
            Vector3f d = randomDir(r).mul(0.4f + age * 0.18f).add(c);
            buf.billboard(ctx, CORE, VfxBlend.ADD, d, 0.16f, 0, al(SPARK, rip * fade));
        }
    }

    /** Black Butler: the contract seal and black feathers. ~22 quads. */
    private static void butler(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, float age, float fade, RandomSource r) {
        float open = VfxAnim.easeOutBack(Mth.clamp(age / 4f, 0, 1));
        buf.billboard(ctx, GLOW, VfxBlend.ADD, c, 3.2f * open, 0, al(CONTRACT, 0.35f * fade));
        buf.billboard(ctx, PENTAGRAM, VfxBlend.ADD, c, 2.2f * open, age * 0.03f, al(hot(CONTRACT, 0.2f), fade));
        int feathers = ctx.seg(10, 5);
        for (int i = 0; i < feathers; i++) {
            Vector3f d = randomDir(r);
            float burst = VfxAnim.easeOutCubic(Mth.clamp(age / 5f, 0, 1)), fall = Math.max(0, age - 4f) * 0.05f, sway = Mth.sin(age * 0.35f + i * 1.9f) * 0.25f;
            Vector3f at = new Vector3f(d).mul(0.5f + 1.3f * burst).add(c).add(sway, -fall, 0);
            buf.billboard(ctx, FEATHER, VfxBlend.ALPHA, at, 0.5f, r.nextFloat() * Mth.TWO_PI + sway * 2f, al(INK, 0.95f * fade));
        }
    }

    /** Blue Exorcist: Satan's blue flames. ~32 quads. */
    private static void exorcist(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, float age, int col, float fade, RandomSource r) {
        Vector3f feet = new Vector3f(c).add(0, -1.0f, 0);
        int n = ctx.seg(10, 5);
        for (int i = 0; i < n; i++) {
            float ang = r.nextFloat() * Mth.TWO_PI, rad = 0.25f + 0.35f * r.nextFloat(), h = (1.2f + 0.8f * r.nextFloat()) * (0.8f + 0.2f * Mth.sin(age * 0.8f + i)) * fade;
            Vector3f at = new Vector3f(Mth.cos(ang) * rad, 0, Mth.sin(ang) * rad).add(feet);
            upright(buf, FLAME, VfxBlend.ADD, at, h * 0.55f, h, al(SATAN_BLUE, 0.9f * fade));
            upright(buf, FLAME, VfxBlend.ADD, at, h * 0.25f, h * 0.55f, al(hot(SATAN_BLUE, 0.7f), fade));
        }
        buf.ring(RING, VfxBlend.ADD, VfxPose.ground(new Vector3f(feet).add(0, 0.05f, 0)), 0.7f, 1.0f + 0.4f * VfxAnim.easeOutCubic(Mth.clamp(age / 6f, 0, 1)), ctx.seg(12, 6), 3, age * 0.05f, al(SATAN_BLUE, fade));
        buf.billboard(ctx, GLOW, VfxBlend.ADD, c, 2.4f, 0, al(col, 0.3f * fade));
    }

    /** Tensura: the demonic aura pulse. ~24 quads. */
    private static void tensura(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, float age, int col, float fade, RandomSource r) {
        buf.billboard(ctx, VOID, VfxBlend.ALPHA, c, 0.9f, age * 0.1f, al(INK, 0.8f * fade));
        for (int k = 0; k < 3; k++) {
            float ph = Mth.clamp((age - k * 3f) / 9f, 0, 1);
            if (ph <= 0 || ph >= 1) continue;
            buf.billboard(ctx, GLOW, VfxBlend.ADD, c, 0.8f + 3.2f * ph, 0, al(DAEMON, 0.7f * (1 - ph) * fade));
            buf.ring(RING, VfxBlend.ADD, camPose(c), 0.4f + 1.6f * ph, 0.6f + 1.9f * ph, ctx.seg(12, 6), 3, k * 0.3f, al(VfxVertexBuffer.lerpColor(DAEMON, col, 0.4f), (1 - ph) * fade));
        }
        int motes = ctx.seg(6, 3);
        for (int i = 0; i < motes; i++) {
            Vector3f d = randomDir(r);
            float ph = Mth.frac(age * 0.07f + r.nextFloat());
            buf.billboard(ctx, CORE, VfxBlend.ADD, new Vector3f(d.x * 0.7f, ph * 2f - 1f, d.z * 0.7f).add(c), 0.15f, 0, al(hot(DAEMON, 0.4f), Mth.sin(ph * Mth.PI) * fade));
        }
    }

    /** Dororo: ghost fires circling. ~18 quads. */
    private static void dororo(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, float age, float fade) {
        int n = ctx.seg(6, 3);
        float open = VfxAnim.easeOutCubic(Mth.clamp(age / 5f, 0, 1));
        for (int i = 0; i < n; i++) {
            float ang = Mth.TWO_PI * i / n + age * 0.08f, bob = 0.25f * Mth.sin(age * 0.4f + i * 2.3f), flick = 0.85f + 0.15f * Mth.sin(age * 1.7f + i);
            Vector3f at = new Vector3f(Mth.cos(ang) * 1.1f * open, bob - 0.2f, Mth.sin(ang) * 1.1f * open).add(c);
            buf.billboard(ctx, GLOW, VfxBlend.ADD, new Vector3f(at).add(0, 0.3f, 0), 1.3f, 0, al(ONIBI, 0.4f * fade));
            upright(buf, FLAME, VfxBlend.ADD, at, 0.55f * flick, 0.95f * flick, al(ONIBI, 0.9f * fade));
            buf.billboard(ctx, CORE, VfxBlend.ADD, new Vector3f(at).add(0, 0.22f, 0), 0.2f, 0, al(WHITE, fade));
        }
    }

    /** Berserk: the Brand bleeding and the eclipse. ~22 quads. */
    private static void berserk(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, float age, float fade) {
        float bleed = 0.7f + 0.3f * VfxAnim.pulse(age, 5f);
        buf.billboard(ctx, GLOW, VfxBlend.ADD, c, 1.6f, 0, al(BRAND_RED, 0.5f * bleed * fade));
        upright(buf, BRAND, VfxBlend.ADD, new Vector3f(c).add(0, -0.45f, 0), 0.5f, 1.0f, al(hot(BRAND_RED, 0.15f), bleed * fade));
        Vector3f sun = new Vector3f(c).add(0, 1.6f, 0);
        float rise = VfxAnim.easeOutCubic(Mth.clamp(age / 6f, 0, 1)), s = 0.7f * rise;
        buf.billboard(ctx, VOID, VfxBlend.ALPHA, sun, s * 2f, age * 0.02f, al(INK, fade));
        buf.ring(RING, VfxBlend.ADD, camPose(sun), s * 0.85f, s * 1.5f, ctx.seg(16, 8), 4, age * 0.03f, al(BRAND_RED, fade));
        buf.billboard(ctx, GLOW, VfxBlend.ADD, sun, s * 3.4f, 0, al(BRAND_RED, 0.35f * fade));
    }

    /** Fairy Tail: Etherious curse runes. ~42 quads. */
    private static void fairyTail(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, float age, int col, float fade) {
        float open = VfxAnim.easeOutBack(Mth.clamp(age / 4f, 0, 1));
        int seg = ctx.seg(16, 8);
        int rune = VfxVertexBuffer.lerpColor(CURSE, col, 0.3f);
        hoop(buf, RUNES, VfxBlend.ADD, VfxPose.facing(new Vector3f(c).add(0, 0.35f, 0), new Vector3f(0.2f, 1f, 0.1f)), 0.9f * open, 0.24f, seg, 3, age * 0.04f, al(hot(rune, 0.2f), fade));
        hoop(buf, RUNES, VfxBlend.ADD, VfxPose.facing(new Vector3f(c).add(0, -0.35f, 0), new Vector3f(-0.2f, 1f, -0.15f)), 1.05f * open, 0.24f, seg, 3, -age * 0.035f, al(hot(rune, 0.2f), fade));
        buf.plane(CRACK, VfxBlend.ALPHA, VfxPose.ground(new Vector3f(c).add(0, -0.95f, 0)).spin(0.4f), 1.3f * open, al(INK, 0.8f * fade));
        buf.billboard(ctx, GLOW, VfxBlend.ADD, c, 2.6f, 0, al(CURSE, 0.35f * fade));
    }
}
