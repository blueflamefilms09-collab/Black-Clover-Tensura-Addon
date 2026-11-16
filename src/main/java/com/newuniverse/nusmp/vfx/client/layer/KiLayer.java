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
 * Dragon Ball Ki, drawn after the anime (docs/vfx_remake/ki_dragonball.md): hard, bright cel shapes instead of particle sprays. Every ki shape is a flat white-hot CORE with a crisp edge (additive), one
 * or two flat cel steps of saturated HALO round it (alpha, so the colour stays saturated on a bright sky), sharp star-burst FLASHES on impact, SPEED LINES, flat SHOCK RINGS that run over the ground with a
 * DUST ring behind them. The colour of the packet is the ki colour (the server picks it from the caster's form: base blue-white, Super Saiyan gold, Kaio-ken / Super Saiyan 4 red, Blue cyan, Ultra Instinct
 * silver; the Kamehameha stays blue unless the form is red, the Spirit Bomb stays blue-white, the Dragon Fist and God Bind gold). Techniques of TimeMagicLayer (the quality standard) copied here: life
 * ramps, eased opening, ribbons standing on circles (hoops), seeded per-effect randomness so every client sees the same picture. Textures: {@code tools/gen_ki_vfx_textures.py} -> {@code ki_*.png}.
 * <ul>
 *   <li>KI_BLAST: Ki Blast / Rapid Barrage. from = the hand, to = where the ray ended, power = how many orbs (1, or 2-8 for a volley; each later orb starts 1.2 ticks after the one before and lands a
 *       little off the line). A glowing orb with a short tail, then a pop (star flash, halo, three sparks).</li>
 *   <li>KI_BEAM: Kamehameha. from = the hands, to = the end of the beam, power = half-width (0.6-2). A sphere at the hands, the beam shoots out in 3 ticks: white-hot core, saturated halo, a rippling
 *       outer edge, rings running down it, a ball at its head; at the end a star flash, an expanding impact ring and sparks; it thins and fades over the last 8 ticks.</li>
 *   <li>KI_CHARGE: the charge in the cupped hands. from = the hands (follows the caster), power = charge 0-1, 14 ticks, re-sent every 10 ticks while held (in 3 / out 4 ticks: the copies cross-fade).
 *       A growing orb, crackling sparks, motes pulled in.</li>
 *   <li>KI_SPHERE: Spirit Bomb. from = to = over the head (follows): the gathering sphere (power = radius, 24 ticks, re-sent every 20); from != to: the thrown bomb flies from -> to over its duration.
 *       Solid blue-white ball, two counter-turning swirls, a hot heart, glow; while gathering, motes stream in from all round and below; in flight, a tail.</li>
 *   <li>KI_SHOCKWAVE: a landing blast (Spirit Bomb, the last Meteor blow). from = the centre, power = radius. Star flash, a flat ring running out over the ground, a dust ring behind it, a second ring
 *       higher up, rays along the ground; camera shake from radius 4.</li>
 *   <li>KI_AFTERIMAGE: Instant Transmission, sent at both ends. from = the feet, to = the other end. The figure squeezes thin and vanishes with a flicker (two afterimages left towards {@code to}), vertical
 *       speed lines, a flash ring at the waist.</li>
 *   <li>KI_RUSH: Meteor Combination round the foe. from = the foe's centre (follows), power = hits, duration = hits x interval + 6. A flash where each blow lands with the attacker's afterimage on the far
 *       side and speed lines through it, the last blow bigger with a ring; speed lines circle the foe.</li>
 *   <li>KI_STRIKE: Rock, Scissors 'n' Paper. from = the chest, to = the point of the blow, power = 1 Rock (a punch flash, a ring facing the blow, speed lines), 2 Scissors (two crossing ki cuts), 3 Paper
 *       (an open-palm burst and three rings running forward in a cone).</li>
 *   <li>KI_BARRIER: Ki Barrier. from = the chest (follows), power = radius, + 5 on a refresh (the server re-sends it once a second while it stands: 24 ticks each, in 4 / out 4, so it disappears within
 *       about a second of breaking). A sphere with a hexagon net and a bright rim, a shimmer, a ground ring; the first one pops open with a flash.</li>
 *   <li>KI_DEFLECT: Ki Deflection. from = the chest (follows), power = the deflection radius, duration = the window. Two spinning ki hoops round the body, a swirl at the waist, a faint ring on the
 *       ground at the reach.</li>
 *   <li>KI_BIND: God Bind. from = the foe's feet (follows), power = its height, duration = the hold. Three golden rings snap tight round the body one after the other, a turning seal on the ground, a faint
 *       column of light.</li>
 *   <li>KI_DRAGON: Dragon Fist. from = the foe's feet (follows), power = its height, duration = rise + 14. A golden dragon (shaded serpent body, horned head) spirals up round the foe; at the top it
 *       bursts (star flash, ring, glow; camera shake).</li>
 *   <li>KI_TRANSFER: Energy Transfer. from = the giver's chest, to = the ally's chest. A waving stream with motes running along it, the ally lights up with a ring.</li>
 * </ul>
 * <b>Budgets (rule 11).</b> The engine caps every activation at 100 quads (400 vertices) and this layer keeps the default; worst case quads per effect at full detail ({@code ctx.seg} thins them by
 * distance and by the particle setting), counted from the code (the preview, tools/vfx_preview/scenes.d/ki.json, measured at most 70): BLAST 42 (8 orbs all popping), BEAM 75, CHARGE 18, SPHERE 46,
 * SHOCKWAVE 72, AFTERIMAGE 32, RUSH 44, STRIKE 60, BARRIER 26, DEFLECT 54, BIND 68, DRAGON 64, TRANSFER 31; every ring and hoop also checks {@code hasBudget}, so the cap is never passed.
 * Nothing is kept between frames (only short-lived pose vectors, as every layer), every loop is a constant or a {@code ctx.seg} of at most 24, so nothing has to be cleaned up on logout, death or a change
 * of dimension. A followed effect whose creature is gone draws nothing. With {@code saiyan.classicVfx} on the layer draws nothing at all (the old particles still come from the server).
 */
public class KiLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(KiVfx.fileName(n)); }

    public static final ResourceLocation CORE = t("core");
    public static final ResourceLocation HALO = t("halo");
    public static final ResourceLocation BEAM = t("beam");
    public static final ResourceLocation BEAM_EDGE = t("beam_edge");
    public static final ResourceLocation RING = t("ring");
    public static final ResourceLocation DUST = t("dust");
    public static final ResourceLocation FLASH = t("flash");
    public static final ResourceLocation SPARK = t("spark");
    public static final ResourceLocation STREAK = t("streak");
    public static final ResourceLocation SLASH = t("slash");
    public static final ResourceLocation SWIRL = t("swirl");
    public static final ResourceLocation HEX = t("hex");
    public static final ResourceLocation DRAGON_HEAD = t("dragon_head");
    public static final ResourceLocation DRAGON_BODY = t("dragon_body");
    public static final ResourceLocation AFTERIMAGE = t("afterimage");

    private static final int WHITE = 0xFFFFFFFF;
    private static final int KI = 0xFF9FD8FF;
    private static final int KAME = 0xFF5EC8FF;
    private static final int SPIRIT = 0xFF8CD8FF;
    private static final int GOLD = 0xFFFFC832;
    private static final int DUST_COL = 0xFFC8BCA8;
    /** The dragon's burst lasts this long after the rise. */
    private static final int DRAGON_BURST = 14;
    /** The Meteor rush's tail after the last blow. */
    private static final int RUSH_TAIL = 6;

    @Override
    public Set<VfxShape> shapes() {
        return EnumSet.of(VfxShape.KI_BLAST, VfxShape.KI_BEAM, VfxShape.KI_CHARGE, VfxShape.KI_SPHERE, VfxShape.KI_SHOCKWAVE, VfxShape.KI_AFTERIMAGE, VfxShape.KI_RUSH,
                VfxShape.KI_STRIKE, VfxShape.KI_BARRIER, VfxShape.KI_DEFLECT, VfxShape.KI_BIND, VfxShape.KI_DRAGON, VfxShape.KI_TRANSFER);
    }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case KI_BLAST, KI_AFTERIMAGE, KI_STRIKE -> 12;
            case KI_BEAM -> 30;
            case KI_CHARGE -> 14;
            case KI_SPHERE, KI_BARRIER -> 24;
            case KI_SHOCKWAVE, KI_TRANSFER -> 20;
            case KI_RUSH -> 40;
            case KI_DEFLECT -> 80;
            case KI_BIND -> 120;
            case KI_DRAGON -> 26;
            default -> 20;
        };
    }

    @Override
    public int defaultColor(VfxShape s) {
        return switch (s) {
            case KI_BEAM -> KAME;
            case KI_SPHERE -> SPIRIT;
            case KI_BIND, KI_DRAGON -> GOLD;
            default -> KI;
        };
    }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (KiVfx.classic()) return;
        if (inst.shape == VfxShape.KI_SHOCKWAVE && inst.power >= 4f) VfxShake.add(inst.payload.from(), Math.min(2.5f, inst.power * 0.2f), 10);
        else if (inst.shape == VfxShape.KI_BEAM) VfxShake.add(inst.payload.from(), Math.min(1.6f, 0.4f + inst.power * 0.5f), 8);
    }

    @Override
    public void onTick(VfxInstance inst) {
        if (inst.shape == VfxShape.KI_DRAGON && !KiVfx.classic() && inst.age() == Math.max(1, inst.duration - DRAGON_BURST)) VfxShake.add(inst.payload.from(), 1.2f, 8);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        if (KiVfx.classic()) return;
        if (inst.payload.followEntity() >= 0 && Float.isNaN(inst.followYaw(ctx))) return;        // the followed creature is gone (died, left): the effect ends with it
        switch (inst.shape) {
            case KI_BLAST -> blast(inst, ctx, buf);
            case KI_BEAM -> beam(inst, ctx, buf);
            case KI_CHARGE -> charge(inst, ctx, buf);
            case KI_SPHERE -> sphere(inst, ctx, buf);
            case KI_SHOCKWAVE -> shockwave(inst, ctx, buf);
            case KI_AFTERIMAGE -> afterimage(inst, ctx, buf);
            case KI_RUSH -> rush(inst, ctx, buf);
            case KI_STRIKE -> strike(inst, ctx, buf);
            case KI_BARRIER -> barrier(inst, ctx, buf);
            case KI_DEFLECT -> deflect(inst, ctx, buf);
            case KI_BIND -> bind(inst, ctx, buf);
            case KI_DRAGON -> dragon(inst, ctx, buf);
            case KI_TRANSFER -> transfer(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ shared pieces
    /** 1 while running, ramping 0 -> 1 over the first {@code in} ticks and 1 -> 0 over the last {@code out}. */
    private static float life(VfxInstance inst, float age, float in, float out) {
        float a = in <= 0 ? 1 : Mth.clamp(age / in, 0, 1);
        float b = out <= 0 ? 1 : Mth.clamp((inst.duration - age) / out, 0, 1);
        return Math.min(a, b);
    }

    private static int al(int argb, float a) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(a, 0, 1)); }

    private static int hot(int argb, float t) { return VfxVertexBuffer.whiten(argb, t); }

    /** The anime ki ball: a saturated cel halo (alpha) and a white-hot core (additive) on top. r = core radius. 2 quads. */
    private static void orb(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, float r, int col, float alpha) {
        if (alpha <= 0.01f || r <= 0.001f) return;
        buf.billboard(ctx, HALO, VfxBlend.ALPHA, c, r * 4.2f, 0, al(col, 0.75f * alpha));
        buf.billboard(ctx, CORE, VfxBlend.ADD, c, r * 2.2f, 0, al(hot(col, 0.85f), alpha));
    }

    private static Vector3f lerp(Vector3f a, Vector3f b, float t) { return new Vector3f(a).lerp(b, t); }

    private static Vector3f dirOr(Vector3f a, Vector3f b, Vector3f fallback) {
        Vector3f d = new Vector3f(b).sub(a);
        return d.lengthSquared() < 1e-6f ? new Vector3f(fallback) : d.normalize();
    }

    /** A unit vector from the effect's random (uniform on the sphere). */
    private static Vector3f randomDir(RandomSource r) {
        float u = r.nextFloat() * 2 - 1, phi = r.nextFloat() * Mth.TWO_PI, s = Mth.sqrt(1 - u * u);
        return new Vector3f(s * Mth.cos(phi), u, s * Mth.sin(phi));
    }

    /** A figure standing upright at {@code feet}, turned to the camera round the vertical only (afterimages). 1 quad. */
    private static void upright(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f feet, float width, float height, int argb) {
        Vector3f side = new Vector3f(-feet.z, 0, feet.x);                    // feet is camera-relative, so -feet points at the camera; side = up x toCam
        if (side.lengthSquared() < 1e-6f) side.set(1, 0, 0);
        side.normalize().mul(width * 0.5f);
        Vector3f top = new Vector3f(feet).add(0, height, 0);
        buf.quad(tex, blend, new Vector3f(feet).sub(side), new Vector3f(feet).add(side), new Vector3f(top).add(side), new Vector3f(top).sub(side), 0, 0, 1, 1, argb, argb);
    }

    /** A ribbon standing on a circle round the pose normal axis (the Time Magic technique): width along the normal, U wraps {@code repeats} times and scrolls. */
    private static void hoop(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, VfxPose pose, float radius, float halfWidth, int segments, float repeats, float uScroll, int argb) {
        Vector3f n = new Vector3f(pose.normal()).mul(halfWidth);
        for (int i = 0; i < segments; i++) {
            if (!buf.hasBudget(4)) return;
            float a0 = Mth.TWO_PI * i / segments, a1 = Mth.TWO_PI * (i + 1) / segments;
            Vector3f m0 = pose.point(Mth.cos(a0) * radius, Mth.sin(a0) * radius), m1 = pose.point(Mth.cos(a1) * radius, Mth.sin(a1) * radius);
            float u0 = uScroll + repeats * i / segments, u1 = uScroll + repeats * (i + 1) / segments;
            buf.quad(tex, blend, new Vector3f(m0).sub(n), new Vector3f(m1).sub(n), new Vector3f(m1).add(n), new Vector3f(m0).add(n), u0, 0, u1, 1, argb, argb);
        }
    }

    /** A star flash with its glow. 2-4 quads. */
    private static void flash(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, float size, float rot, int col, float alpha) {
        if (alpha <= 0.01f) return;
        buf.billboard(ctx, HALO, VfxBlend.ADD, c, size * 1.3f, 0, al(col, 0.5f * alpha));
        buf.billboard(ctx, FLASH, VfxBlend.ADD, c, size, rot, al(hot(col, 0.8f), alpha));
    }

    // ------------------------------------------------------------------ KI_BLAST
    private void blast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        int n = Mth.clamp(Math.round(inst.power), 1, 8);
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        float len = a.distance(b);
        float travel = Mth.clamp(len / 6f, 2f, 6f);
        float spread = n > 1 ? Math.min(1.4f, 0.1f * len) : 0f;
        RandomSource r = inst.random();
        if (age < 3) flash(ctx, buf, a, 0.7f, age * 0.4f, col, 1 - age / 3f);           // muzzle
        for (int i = 0; i < n; i++) {
            Vector3f end = new Vector3f(b).add((r.nextFloat() - 0.5f) * 2 * spread, (r.nextFloat() - 0.5f) * 2 * spread * 0.6f, (r.nextFloat() - 0.5f) * 2 * spread);
            float rot = r.nextFloat() * Mth.TWO_PI;
            Vector3f s0 = randomDir(r), s1 = randomDir(r), s2 = randomDir(r);
            if (i == 0) end.set(b);
            float t = (age - i * 1.2f) / travel;
            if (t < 0) continue;
            if (t <= 1) {
                Vector3f pos = lerp(a, end, t), tail = lerp(a, end, Math.max(0, t - 0.4f));
                buf.beam(ctx, BEAM, VfxBlend.ADD, tail, pos, 0.04f, 0.34f, 1, 0, al(col, 0.1f), al(hot(col, 0.3f), 0.9f));
                orb(ctx, buf, pos, 0.16f, col, 1);
            } else {
                float p = (age - i * 1.2f - travel) / 5f;
                if (p >= 1) continue;
                float f = 1 - p;
                flash(ctx, buf, end, 0.7f + 1.3f * p, rot + p, col, f);
                Vector3f[] dirs = {s0, s1, s2};
                for (Vector3f d : dirs) buf.billboard(ctx, SPARK, VfxBlend.ADD, new Vector3f(d).mul(0.3f + 1.1f * p).add(end), 0.32f * f, rot, al(hot(col, 0.6f), f));
            }
        }
    }

    // ------------------------------------------------------------------ KI_BEAM
    private void beam(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = dirOr(a, b, new Vector3f(0, 0, 1));
        float len = a.distance(b);
        float extend = VfxAnim.easeOutCubic(Mth.clamp(age / 3f, 0, 1));
        float fade = life(inst, age, 0, 8);
        float w = inst.power * (0.35f + 0.65f * fade) * (1 + 0.06f * Mth.sin(age * 2.3f));
        Vector3f head = new Vector3f(dir).mul(len * extend).add(a);

        // the sphere at the hands
        orb(ctx, buf, a, w * 0.85f, col, fade);
        buf.billboard(ctx, FLASH, VfxBlend.ADD, a, w * 3.2f, age * 0.15f, al(hot(col, 0.6f), 0.45f * fade));

        if (len * extend > 0.05f) {
            // the rippling outer edge: short pieces, each a little wider or narrower than the last
            int n = ctx.seg(10, 4);
            for (int i = 0; i < n; i++) {
                float t0 = (float) i / n * extend, t1 = (float) (i + 1) / n * extend;
                Vector3f p0 = new Vector3f(dir).mul(len * t0).add(a), p1 = new Vector3f(dir).mul(len * t1).add(a);
                float w0 = w * 2.6f * (1 + 0.14f * Mth.sin(age * 1.7f + i * 1.3f)), w1 = w * 2.6f * (1 + 0.14f * Mth.sin(age * 1.7f + (i + 1) * 1.3f));
                buf.beam(ctx, BEAM_EDGE, VfxBlend.ALPHA, p0, p1, w0, w1, 1, i - age * 0.3f, al(col, 0.7f * fade), al(col, 0.7f * fade));
            }
            int seg = ctx.seg(6, 3);
            buf.beam(ctx, BEAM, VfxBlend.ALPHA, a, head, w * 2f, w * 2f, seg, -age * 0.3f, al(col, 0.85f * fade), al(col, 0.85f * fade));
            buf.beam(ctx, BEAM, VfxBlend.ADD, a, head, w * 1.1f, w * 1.1f, seg, -age * 0.5f, al(hot(col, 0.9f), fade), al(hot(col, 0.9f), fade));
            // rings running down the beam
            int rings = 2, rs = ctx.seg(10, 6);
            for (int k = 0; k < rings; k++) {
                float ph = Mth.frac(age * 0.09f + k / (float) rings);
                Vector3f at = new Vector3f(dir).mul(len * extend * ph).add(a);
                buf.ring(RING, VfxBlend.ADD, VfxPose.facing(at, dir), w * 1.2f, w * 1.9f, rs, 2, age * 0.05f, al(hot(col, 0.4f), 0.55f * (1 - ph) * fade));
            }
        }
        // the ball at the head while it shoots out
        if (extend < 1) orb(ctx, buf, head, w * 1.1f, col, 1);

        // impact at the end
        if (age >= 3) {
            float ia = age - 3, span = Math.max(4, inst.duration - 3);
            float f = 1 - ia / span;
            flash(ctx, buf, b, w * (3.2f + 0.6f * Mth.sin(age * 1.9f)), age * 0.2f, col, f);
            float rr = Math.min(w * 6f, w * (1.4f + ia * 0.45f));
            buf.ring(RING, VfxBlend.ADD, VfxPose.facing(b, new Vector3f(dir).negate()), rr * 0.75f, rr, ctx.seg(16, 8), 3, 0, al(hot(col, 0.5f), 0.8f * f));
            RandomSource r = inst.random();
            int sparks = ctx.seg(6, 3);
            for (int i = 0; i < sparks; i++) {
                Vector3f d = randomDir(r);
                if (d.dot(dir) > 0) d.reflect(dir);                                   // sparks fly back off the surface the beam hits
                float ph = Mth.frac(ia * 0.12f + r.nextFloat());
                buf.billboard(ctx, SPARK, VfxBlend.ADD, new Vector3f(d).mul(w * (0.5f + 3f * ph)).add(b), 0.4f * (1 - ph) + 0.1f, ph * 3, al(hot(col, 0.7f), (1 - ph) * f));
            }
        }
    }

    // ------------------------------------------------------------------ KI_CHARGE
    private void charge(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        float c01 = Mth.clamp(inst.power, 0, 1);
        float fade = life(inst, age, 3, 4);
        Vector3f c = ctx.rel(inst.from(ctx));
        float size = (0.18f + 0.45f * c01) * (1 + 0.08f * Mth.sin(age * 1.9f));
        orb(ctx, buf, c, size, col, fade);
        buf.billboard(ctx, SPARK, VfxBlend.ADD, c, size * 4.5f, age * 0.3f, al(WHITE, 0.6f * fade));
        RandomSource r = inst.random();
        int crackle = ctx.seg(6, 3);
        for (int i = 0; i < crackle; i++) {
            Vector3f d = randomDir(r);
            float tw = Mth.sin(age * (0.8f + r.nextFloat()) + r.nextFloat() * Mth.TWO_PI), rot = r.nextFloat() * Mth.TWO_PI;     // every draw takes the same randoms, so the pattern holds from frame to frame
            if (tw < 0.3f) continue;
            buf.billboard(ctx, SPARK, VfxBlend.ADD, new Vector3f(d).mul(size * 1.6f).add(c), size * 0.9f * tw, rot, al(hot(col, 0.6f), tw * fade));
        }
        int motes = ctx.seg(8, 3);
        for (int i = 0; i < motes; i++) {
            Vector3f d = randomDir(r);
            float ph = Mth.frac(age * 0.08f + r.nextFloat());
            float dist = (1 - ph) * (1.0f + 0.8f * c01) + size;
            Vector3f p = new Vector3f(d).mul(dist).add(c), q = new Vector3f(d).mul(dist + 0.35f).add(c);
            buf.beam(ctx, STREAK, VfxBlend.ADD, q, p, 0.08f, 0.08f, 1, 0, al(col, 0.9f * ph * fade), al(col, 0.9f * ph * fade));
        }
    }

    // ------------------------------------------------------------------ KI_SPHERE
    private void sphere(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        boolean gather = from.distanceSquared(to) < 1e-4f;
        float u = Mth.clamp(age / inst.duration, 0, 1);
        Vector3f c = gather ? from : lerp(from, to, u);
        float fade = gather ? life(inst, age, 4, 4) : life(inst, age, 0, 1);
        float R = Math.max(0.3f, inst.power) * (1 + 0.03f * Mth.sin(age * 1.3f));
        if (fade <= 0.01f) return;

        buf.billboard(ctx, HALO, VfxBlend.ALPHA, c, R * 3.0f, 0, al(col, 0.55f * fade));
        buf.billboard(ctx, CORE, VfxBlend.ALPHA, c, R * 2.1f, 0, al(hot(col, 0.45f), 0.9f * fade));
        buf.billboard(ctx, SWIRL, VfxBlend.ADD, c, R * 2.05f, age * 0.06f, al(WHITE, 0.55f * fade));
        buf.billboard(ctx, SWIRL, VfxBlend.ADD, c, R * 1.7f, -age * 0.09f + 1.3f, al(hot(col, 0.5f), 0.45f * fade));
        buf.billboard(ctx, CORE, VfxBlend.ADD, c, R * 1.1f, 0, al(WHITE, 0.8f * fade));
        VfxBloom.glow(ctx, buf, c, R, col, 0.6f * fade);

        RandomSource r = inst.random();
        int crackle = ctx.seg(5, 2);
        for (int i = 0; i < crackle; i++) {
            Vector3f d = randomDir(r);
            float tw = Mth.sin(age * (0.6f + r.nextFloat()) + r.nextFloat() * Mth.TWO_PI);
            if (tw > 0.4f) buf.billboard(ctx, SPARK, VfxBlend.ADD, new Vector3f(d).mul(R * 0.95f).add(c), R * 0.45f * tw, age * 0.1f, al(WHITE, tw * fade));
        }
        if (gather) {
            // energy from all living things: motes stream in from round and below
            int motes = ctx.seg(14, 5);
            for (int i = 0; i < motes; i++) {
                Vector3f d = randomDir(r);
                d.y = -Math.abs(d.y) * 0.8f;
                float ph = Mth.frac(age * 0.045f + r.nextFloat());
                float e = VfxAnim.easeInCubic(ph);
                Vector3f start = new Vector3f(d).mul(R * 6f).add(c);
                Vector3f p = lerp(start, c, e), q = lerp(start, c, Math.max(0, e - 0.08f));
                buf.beam(ctx, STREAK, VfxBlend.ADD, q, p, 0.12f, 0.12f, 1, 0, al(col, ph * fade), al(hot(col, 0.5f), ph * fade));
            }
        } else {
            Vector3f tail = lerp(from, to, Math.max(0, u - 0.18f));
            buf.beam(ctx, BEAM, VfxBlend.ADD, tail, c, R * 0.4f, R * 1.7f, 2, -age * 0.2f, al(col, 0.1f * fade), al(col, 0.6f * fade));
        }
    }

    // ------------------------------------------------------------------ KI_SHOCKWAVE
    private void shockwave(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        float R = Math.max(1f, inst.power);
        Vector3f c = ctx.rel(inst.from(ctx));
        float f = 1 - Mth.clamp(age / inst.duration, 0, 1);
        if (age < 6) flash(ctx, buf, new Vector3f(c).add(0, 0.6f, 0), R * (0.7f + 0.1f * age), age * 0.2f, col, 1 - age / 6f);
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, 0.08f, 0));
        float rr = R * VfxAnim.easeOutCubic(Mth.clamp(age / 12f, 0, 1));
        if (rr > 0.05f) buf.ring(RING, VfxBlend.ADD, ground, rr * 0.8f, rr, ctx.seg(24, 12), 4, 0, al(hot(col, 0.4f), f));
        float rd = R * 0.92f * VfxAnim.easeOutCubic(Mth.clamp(age / 16f, 0, 1));
        if (rd > 0.05f) buf.ring(DUST, VfxBlend.ALPHA, ground.lift(0.03f), rd * 0.65f, rd * 1.05f, ctx.seg(20, 10), 5, 0, al(DUST_COL, 0.85f * f));
        float rh = R * 0.6f * VfxAnim.easeOutCubic(Mth.clamp(age / 9f, 0, 1));
        if (rh > 0.05f) buf.ring(RING, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, 0.5f + R * 0.08f, 0)), rh * 0.8f, rh, ctx.seg(16, 8), 3, 0.3f, al(hot(col, 0.7f), 0.7f * f * f));
        if (age < 10) {
            RandomSource r = inst.random();
            int rays = ctx.seg(8, 4);
            float g = age / 10f;
            for (int i = 0; i < rays; i++) {
                float ang = r.nextFloat() * Mth.TWO_PI;
                Vector3f d = new Vector3f(Mth.cos(ang), 0, Mth.sin(ang));
                Vector3f p0 = new Vector3f(d).mul(R * 0.15f).add(c).add(0, 0.12f, 0), p1 = new Vector3f(d).mul(R * (0.3f + 0.7f * g)).add(c).add(0, 0.12f, 0);
                buf.beam(ctx, STREAK, VfxBlend.ADD, p0, p1, 0.25f, 0.25f, 1, 0, al(hot(col, 0.6f), 1 - g), al(hot(col, 0.6f), 1 - g));
            }
        }
    }

    // ------------------------------------------------------------------ KI_AFTERIMAGE
    private void afterimage(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        Vector3f feet = ctx.rel(inst.from(ctx)), other = ctx.rel(inst.to(ctx));
        Vector3f toward = new Vector3f(other).sub(feet);
        toward.y = 0;
        if (toward.lengthSquared() < 1e-6f) toward.set(1, 0, 0);
        toward.normalize();
        float f = 1 - Mth.clamp(age / inst.duration, 0, 1);
        // the figure squeezes thin and stretches (the "shunk"), flickering; two fainter copies left towards the other end
        float squeeze = 1 - VfxAnim.easeInCubic(Mth.clamp((age - 2) / 5f, 0, 1));
        float flicker = ((int) age & 1) == 0 ? 1f : 0.55f;
        if (squeeze > 0.02f) {
            for (int k = 2; k >= 0; k--) {
                Vector3f at = new Vector3f(toward).mul(k * 0.45f * Math.min(1, age / 3f)).add(feet);
                upright(buf, AFTERIMAGE, VfxBlend.ADD, at, 1.0f * squeeze, 2.0f * (1 + 0.15f * (1 - squeeze)), al(hot(col, 0.5f), (k == 0 ? 0.9f : 0.35f / k) * flicker));
            }
        }
        RandomSource r = inst.random();
        int lines = ctx.seg(10, 5);
        for (int i = 0; i < lines; i++) {
            float ang = r.nextFloat() * Mth.TWO_PI, rad = 0.4f + 0.5f * r.nextFloat(), off = r.nextFloat();
            float lift = (age * 0.25f + off * 2f) % 2.6f;
            Vector3f base = new Vector3f(Mth.cos(ang) * rad, lift - 0.4f, Mth.sin(ang) * rad).add(feet);
            buf.beam(ctx, STREAK, VfxBlend.ADD, base, new Vector3f(base).add(0, 1.0f + off, 0), 0.07f, 0.07f, 1, 0, al(hot(col, 0.4f), 0.8f * f), al(hot(col, 0.4f), 0.8f * f));
        }
        float rr = 0.3f + 1.4f * VfxAnim.easeOutCubic(Mth.clamp(age / 6f, 0, 1));
        buf.ring(RING, VfxBlend.ADD, VfxPose.ground(new Vector3f(feet).add(0, 1.0f, 0)), rr * 0.75f, rr, ctx.seg(14, 8), 2, 0, al(hot(col, 0.5f), f));
        if (age < 3) flash(ctx, buf, new Vector3f(feet).add(0, 1.1f, 0), 1.6f, 0.4f, col, 1 - age / 3f);
    }

    // ------------------------------------------------------------------ KI_RUSH
    private void rush(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        int hits = Mth.clamp(Math.round(inst.power), 1, 16);
        float iv = Math.max(1f, (inst.duration - RUSH_TAIL) / (float) hits);
        Vector3f c = ctx.rel(inst.from(ctx));
        RandomSource r = inst.random();
        for (int k = 0; k < hits; k++) {
            float ang = r.nextFloat() * Mth.TWO_PI, h = (r.nextFloat() - 0.5f) * 0.9f, rot = r.nextFloat() * Mth.TWO_PI;
            float local = age - k * iv;
            if (local < 0 || local >= 4) continue;
            boolean last = k == hits - 1;
            float f = 1 - local / 4f;
            Vector3f d = new Vector3f(Mth.cos(ang), 0, Mth.sin(ang));
            Vector3f hit = new Vector3f(d).mul(0.55f).add(c).add(0, h, 0);
            flash(ctx, buf, hit, (last ? 2.8f : 1.2f) * (0.8f + 0.2f * f), rot, col, f);
            upright(buf, AFTERIMAGE, VfxBlend.ADD, new Vector3f(d).mul(1.4f).add(c).add(0, -1.0f, 0), 0.9f, 1.9f, al(hot(col, 0.4f), 0.45f * f));
            for (int s = -1; s <= 1; s += 2) {
                Vector3f side = new Vector3f(-d.z, 0, d.x).mul(0.18f * s);
                buf.beam(ctx, STREAK, VfxBlend.ADD, new Vector3f(d).mul(2.2f).add(hit).add(side), new Vector3f(d).mul(-0.6f).add(hit).add(side), 0.09f, 0.09f, 1, 0, al(WHITE, f), al(WHITE, f));
            }
            if (last) {
                float rr = 0.5f + 2.2f * (1 - f);
                buf.ring(RING, VfxBlend.ADD, VfxPose.facing(hit, d), rr * 0.75f, rr, ctx.seg(16, 8), 2, 0, al(hot(col, 0.5f), f));
            }
        }
        float f = life(inst, age, 2, RUSH_TAIL);
        int lines = ctx.seg(6, 3);
        for (int i = 0; i < lines; i++) {
            float ang = r.nextFloat() * Mth.TWO_PI + age * (0.5f + 0.3f * r.nextFloat()), y = (r.nextFloat() - 0.5f) * 1.6f;
            Vector3f p0 = new Vector3f(Mth.cos(ang) * 1.3f, y, Mth.sin(ang) * 1.3f).add(c), p1 = new Vector3f(Mth.cos(ang + 0.9f) * 1.3f, y, Mth.sin(ang + 0.9f) * 1.3f).add(c);
            buf.beam(ctx, STREAK, VfxBlend.ADD, p0, p1, 0.08f, 0.08f, 1, 0, al(hot(col, 0.4f), 0.6f * f), al(hot(col, 0.4f), 0.6f * f));
        }
    }

    // ------------------------------------------------------------------ KI_STRIKE
    private void strike(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        int stage = Mth.clamp(Math.round(inst.power), 1, 3);
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = dirOr(a, b, new Vector3f(0, 0, 1));
        float f = 1 - Mth.clamp(age / inst.duration, 0, 1);
        RandomSource r = inst.random();
        if (stage == 1) {                                                       // Rock: the punch lands
            flash(ctx, buf, b, 1.8f * (0.7f + 0.3f * f), r.nextFloat() * Mth.TWO_PI, col, Mth.clamp(1 - age / 6f, 0, 1));
            float rr = 0.4f + age * 0.22f;
            buf.ring(RING, VfxBlend.ADD, VfxPose.facing(b, dir), rr * 0.7f, rr, ctx.seg(14, 8), 2, 0, al(hot(col, 0.5f), f));
            speedLines(ctx, buf, a, b, dir, r, ctx.seg(4, 2), col, f);
        } else if (stage == 2) {                                                // Scissors: two crossing ki cuts
            float grow = VfxAnim.easeOutCubic(Mth.clamp(age / 3f, 0, 1));
            VfxPose pose = VfxPose.facing(b, dir);
            buf.arc(SLASH, VfxBlend.ADD, pose.spin(0.8f), 1.1f, 0.4f, -1.1f, 2.2f * grow, ctx.seg(10, 5), al(hot(col, 0.6f), f));
            buf.arc(SLASH, VfxBlend.ADD, pose.spin(-0.8f), 1.1f, 0.4f, 2.0f, 2.2f * grow, ctx.seg(10, 5), al(hot(col, 0.6f), f));
            flash(ctx, buf, b, 1.1f, 0.3f, col, Mth.clamp(1 - age / 5f, 0, 1));
        } else {                                                                // Paper: the open palm drives a cone of shock forward
            Vector3f palm = new Vector3f(dir).mul(0.8f).add(a);
            float reach = Math.max(2f, a.distance(b));
            flash(ctx, buf, palm, 2.4f, 0.2f, col, Mth.clamp(1 - age / 5f, 0, 1));
            for (int k = 0; k < 3; k++) {
                float p = Mth.clamp((age - k * 1.5f) / 10f, 0, 1);
                if (p <= 0 || p >= 1) continue;
                Vector3f at = new Vector3f(dir).mul(reach * p).add(palm);
                float rr = 0.6f + 2.2f * p;
                buf.ring(RING, VfxBlend.ADD, VfxPose.facing(at, dir), rr * 0.75f, rr, ctx.seg(16, 8), 3, 0, al(hot(col, 0.5f), 1 - p));
            }
            speedLines(ctx, buf, palm, new Vector3f(dir).mul(reach).add(palm), dir, r, ctx.seg(6, 3), col, f);
        }
    }

    /** Speed lines along a blow: parallel streaks round the line from a to b, each sliding forward. */
    private static void speedLines(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f a, Vector3f b, Vector3f dir, RandomSource r, int n, int col, float f) {
        VfxPose across = VfxPose.facing(a, dir);
        float len = a.distance(b);
        for (int i = 0; i < n; i++) {
            float ang = r.nextFloat() * Mth.TWO_PI, rad = 0.25f + 0.5f * r.nextFloat(), s = r.nextFloat() * 0.5f;
            Vector3f off = across.point(Mth.cos(ang) * rad, Mth.sin(ang) * rad).sub(a);
            Vector3f p0 = new Vector3f(dir).mul(len * s).add(a).add(off), p1 = new Vector3f(dir).mul(len * (s + 0.5f)).add(a).add(off);
            buf.beam(ctx, STREAK, VfxBlend.ADD, p0, p1, 0.08f, 0.08f, 1, 0, al(WHITE, 0.8f * f), al(WHITE, 0.8f * f));
        }
    }

    // ------------------------------------------------------------------ KI_BARRIER
    private void barrier(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        boolean refresh = inst.power > 5f;
        float R = Mth.clamp(refresh ? inst.power - 5f : inst.power, 0.5f, 5f);
        float fade = life(inst, age, refresh ? 4 : 0, 4);
        float open = refresh ? 1 : VfxAnim.easeOutBack(Mth.clamp(age / 5f, 0, 1));
        float rad = R * open;
        if (rad <= 0.02f || fade <= 0.01f) return;
        Vector3f c = ctx.rel(inst.from(ctx));
        float pulse = VfxAnim.pulse(age, 1.2f);
        buf.billboard(ctx, HEX, VfxBlend.ALPHA, c, rad * 2f, age * 0.01f, al(col, 0.6f * fade));
        buf.billboard(ctx, HEX, VfxBlend.ADD, c, rad * 2.02f, -age * 0.013f + 0.5f, al(hot(col, 0.5f), (0.2f + 0.2f * pulse) * fade));
        buf.billboard(ctx, HALO, VfxBlend.ADD, c, rad * 2.5f, 0, al(col, 0.25f * fade));
        buf.ring(RING, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, -0.95f, 0)), rad * 0.8f, rad, ctx.seg(16, 8), 3, age * 0.02f, al(hot(col, 0.4f), 0.6f * fade));
        if (!refresh && age < 5) flash(ctx, buf, c, R * 2.2f, 0.3f, col, 1 - age / 5f);
        RandomSource r = inst.random();
        int sparks = ctx.seg(4, 2);
        for (int i = 0; i < sparks; i++) {
            Vector3f d = randomDir(r);
            float tw = Mth.sin(age * (0.5f + r.nextFloat()) + r.nextFloat() * Mth.TWO_PI);
            if (tw > 0.5f) buf.billboard(ctx, SPARK, VfxBlend.ADD, new Vector3f(d).mul(rad * 0.97f).add(c), 0.35f * tw, age * 0.05f, al(WHITE, tw * fade));
        }
    }

    // ------------------------------------------------------------------ KI_DEFLECT
    private void deflect(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        float fade = life(inst, age, 3, 6);
        Vector3f c = ctx.rel(inst.from(ctx));
        int seg = ctx.seg(16, 8);
        for (int k = 0; k < 2; k++) {
            float tilt = (k == 0 ? 0.35f : -0.35f), yaw = age * (k == 0 ? 0.11f : -0.08f);
            Vector3f axis = new Vector3f(Mth.sin(tilt) * Mth.cos(yaw), Mth.cos(tilt), Mth.sin(tilt) * Mth.sin(yaw));
            hoop(buf, SLASH, VfxBlend.ADD, VfxPose.facing(c, axis), 1.15f + 0.1f * k, 0.13f, seg, 3, age * (k == 0 ? 0.12f : -0.1f), al(hot(col, 0.5f), 0.85f * fade));
        }
        buf.plane(SWIRL, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, -0.1f, 0)).spin(age * 0.25f), 1.4f, al(col, 0.45f * fade));
        float R = Mth.clamp(inst.power, 1f, 8f);
        buf.ring(RING, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, -0.95f, 0)), R * 0.95f, R, ctx.seg(20, 10), 4, age * 0.01f, al(col, (0.15f + 0.15f * VfxAnim.pulse(age, 1f)) * fade));
    }

    // ------------------------------------------------------------------ KI_BIND
    private void bind(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        float H = Mth.clamp(inst.power, 0.5f, 6f);
        float fade = life(inst, age, 0, 6);
        Vector3f feet = ctx.rel(inst.from(ctx));
        float tight = 0.35f + 0.2f * H;
        int seg = ctx.seg(16, 8);
        for (int k = 0; k < 3; k++) {
            float snap = VfxAnim.easeOutBack(Mth.clamp((age - k * 2) / 6f, 0, 1));
            if (snap <= 0) continue;
            float rr = Mth.lerp(snap, 1.8f, tight);
            Vector3f at = new Vector3f(feet).add(0, H * (0.25f + 0.3f * k), 0);
            float wob = 0.08f * Mth.sin(age * 0.2f + k);
            hoop(buf, SLASH, VfxBlend.ADD, VfxPose.facing(at, new Vector3f(wob, 1, -wob)), rr, 0.1f, seg, 4, age * 0.05f * (k % 2 == 0 ? 1 : -1), al(hot(col, 0.45f), fade));
        }
        if (((int) age % 10) < 4) buf.billboard(ctx, HALO, VfxBlend.ADD, new Vector3f(feet).add(0, H * 0.55f, 0), H * 1.4f, 0, al(col, 0.3f * fade));
        buf.ring(RING, VfxBlend.ADD, VfxPose.ground(new Vector3f(feet).add(0, 0.05f, 0)), 0.9f, 1.3f, seg, 4, age * 0.03f, al(col, 0.65f * fade));
        buf.beam(ctx, BEAM, VfxBlend.ADD, feet, new Vector3f(feet).add(0, H + 0.8f, 0), 1.3f, 1.0f, 1, -age * 0.05f, al(col, 0.2f * fade), al(col, 0.0f));
    }

    // ------------------------------------------------------------------ KI_DRAGON
    /** A point on the dragon's spiral round the foe: s 0 = the tail at the ground, 1 = the top. */
    private static Vector3f helix(Vector3f feet, float H, float s, float phase) {
        float ang = s * 2.4f * Mth.TWO_PI + phase, rad = 1.15f - 0.35f * s;
        return new Vector3f(Mth.cos(ang) * rad, s * (H + 1.8f) - 0.3f, Mth.sin(ang) * rad).add(feet);
    }

    private void dragon(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        float H = Mth.clamp(inst.power, 0.5f, 6f);
        Vector3f feet = ctx.rel(inst.from(ctx));
        float rise = Math.max(1, inst.duration - DRAGON_BURST);
        float hp = VfxAnim.easeOutCubic(Mth.clamp(age / rise, 0, 1));
        float burst = Mth.clamp((age - rise) / DRAGON_BURST, 0, 1);
        float fade = 1 - burst;
        RandomSource r = inst.random();
        float phase = r.nextFloat() * Mth.TWO_PI;

        int n = ctx.seg(12, 6);
        float bodyLen = 0.8f;
        Vector3f prev = helix(feet, H, Math.max(0, hp - bodyLen), phase), last = prev, before = prev;
        for (int j = 1; j <= n; j++) {
            float s = Math.max(0, hp - bodyLen * (1 - j / (float) n));
            Vector3f p = helix(feet, H, s, phase);
            float w0 = 0.25f + 0.6f * (j - 1) / n, w1 = 0.25f + 0.6f * j / n;
            buf.beam(ctx, BEAM, VfxBlend.ADD, prev, p, w0 * 1.9f, w1 * 1.9f, 1, 0, al(col, 0.35f * fade), al(col, 0.35f * fade));
            buf.beam(ctx, DRAGON_BODY, VfxBlend.ALPHA, prev, p, w0, w1, 1, j * 0.5f, al(col, fade), al(col, fade));
            before = prev;
            prev = p;
            last = p;
        }
        // the head leads, horns back; in the burst it rears straight up
        Vector3f tangent = burst > 0 ? new Vector3f(0, 1, 0) : dirOr(before, last, new Vector3f(0, 1, 0));
        Vector3f neck = new Vector3f(tangent).mul(-0.3f).add(last), snout = new Vector3f(tangent).mul(2.1f).add(last);
        buf.beam(ctx, DRAGON_HEAD, VfxBlend.ALPHA, neck, snout, 1.6f, 1.6f, 1, 0, al(col, fade), al(col, fade));
        VfxBloom.glow(ctx, buf, lerp(neck, snout, 0.5f), 0.8f, col, 0.5f * fade);
        int sparks = ctx.seg(6, 3);
        for (int i = 0; i < sparks; i++) {
            float s = Math.max(0, hp - bodyLen * r.nextFloat());
            float tw = Mth.sin(age * 0.7f + r.nextFloat() * Mth.TWO_PI);
            if (tw > 0.2f) buf.billboard(ctx, SPARK, VfxBlend.ADD, helix(feet, H, s, phase).add(0, 0.3f, 0), 0.35f * tw, age * 0.1f, al(hot(col, 0.6f), tw * fade));
        }
        if (burst > 0) {
            Vector3f top = new Vector3f(feet).add(0, H + 1.6f, 0);
            flash(ctx, buf, top, 2.5f + 3.5f * burst, burst * 1.5f, col, 1 - burst);
            float rr = 0.8f + 4f * VfxAnim.easeOutCubic(burst);
            buf.ring(RING, VfxBlend.ADD, VfxPose.ground(top), rr * 0.8f, rr, ctx.seg(16, 8), 3, 0, al(hot(col, 0.2f), 1 - burst));
        }
    }

    // ------------------------------------------------------------------ KI_TRANSFER
    private void transfer(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = dirOr(a, b, new Vector3f(1, 0, 0));
        Vector3f side = new Vector3f(dir).cross(0, 1, 0);
        if (side.lengthSquared() < 1e-6f) side.set(1, 0, 0);
        side.normalize();
        float fade = life(inst, age, 3, 6);
        float reach = VfxAnim.easeOutCubic(Mth.clamp(age / 4f, 0, 1));
        int n = ctx.seg(10, 5);
        Vector3f prev = new Vector3f(a);
        for (int i = 1; i <= n; i++) {
            float t = i / (float) n * reach;
            float wave = 0.3f * Mth.sin(t * Mth.PI * 3 - age * 0.6f) * Mth.sin(t * Mth.PI);
            Vector3f p = lerp(a, b, t).add(new Vector3f(side).mul(wave)).add(0, wave * 0.5f, 0);
            buf.beam(ctx, BEAM, VfxBlend.ADD, prev, p, 0.36f, 0.36f, 1, i - age * 0.4f, al(hot(col, 0.3f), 0.8f * fade), al(hot(col, 0.3f), 0.8f * fade));
            prev = p;
        }
        int motes = ctx.seg(6, 3);
        for (int k = 0; k < motes; k++) {
            float t = Mth.frac(age * 0.06f + k / (float) motes) * reach;
            buf.billboard(ctx, SPARK, VfxBlend.ADD, lerp(a, b, t), 0.4f, age * 0.2f, al(WHITE, fade));
        }
        float glow = fade * Mth.clamp((age - 3) / 6f, 0, 1);
        if (glow > 0.01f) {
            orb(ctx, buf, b, 0.25f + 0.1f * VfxAnim.pulse(age, 2f), col, 0.6f * glow);
            float rr = 0.5f + 0.6f * Mth.frac(age * 0.05f);
            buf.ring(RING, VfxBlend.ADD, VfxPose.ground(new Vector3f(b).add(0, -0.6f, 0)), rr * 0.75f, rr, ctx.seg(12, 6), 2, 0, al(hot(col, 0.5f), glow * (1 - Mth.frac(age * 0.05f))));
        }
    }
}
