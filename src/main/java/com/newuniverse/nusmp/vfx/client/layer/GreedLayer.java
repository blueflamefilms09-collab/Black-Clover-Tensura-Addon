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
 * The Black Sword Greed (Berserk of Gluttony), drawn after the anime (docs/vfx_remake/greed_devils.md), and the devil hits ({@link GreedDevilHits}): hard ink-black shapes with a thin crimson edge instead of
 * dust sprays. The black parts are drawn with the alpha blend in a near-black ink colour (so they stay black on a bright sky), the crimson parts additive in the packet colour. The pieces: ink CRESCENTS with a
 * blood-red rim (the cuts), black LIGHTNING (a jagged ink core in a red glow, re-rolled every 2 ticks: the anime flicker), the hungry VOID ringed with fangs, the dome of black SCALES, two red EYE glints,
 * black FLAMES with a crimson heart. Techniques of TimeMagicLayer (the quality standard) copied here, not its code: life ramps, eased opening, seeded per-effect randomness so every client sees the same
 * picture. Textures: {@code tools/gen_greed_vfx_textures.py} -> {@code greed_*.png}.
 * <ul>
 *   <li>GREED_SLASH: power = cuts. from = to: Infinity Divide, a storm of cuts round from (one every span / cuts ticks, 7 ticks each). from -> to closer than 4: the cuts cross at {@code to} (Level Zero's blink, an X
 *       for 2). Farther: Bloody Ptarmigan Cross, the crossed crescents fly from -> to growing, a crimson streak behind them, a flash where they land.</li>
 *   <li>GREED_LANCE: from -> to, power = half-width. Under 0.5 an arrow (the bow's bolt, the spear's thrust): a black-red streak flies out with a glowing head. 0.5 and up a beam that shoots out in 3 ticks
 *       (Piercing Meteor, Revolt Brionac), with black lightning coiling round it from 0.6. An ink splash, a flash and a ring where it ends.</li>
 *   <li>GREED_FLAME: from -> to: Black Flames, ink flame tongues with crimson hearts rolling out in a cone. from = to: Twilight Healing, a ring of black flames at power = radius, a column of light and motes
 *       rising in the packet colour.</li>
 *   <li>GREED_THREADS: Spatial Threads from the hand to the end of the reach, power = how many: taut crimson threads shoot out in 3 ticks and snap at the end with a small flash each.</li>
 *   <li>GREED_REAP: Soul Reap round from (follows), power = radius: a huge ink crescent sweeps a full circle in 7 ticks, a ring runs out, souls are torn from the circle and spiral in.</li>
 *   <li>GREED_DOME: the dome of black scales round from (follows). power = radius: the guard flash of a reflected blow. 10 + radius: Reflection Fortress, the shell forms, shatters into scale shards that fly out
 *       to the radius, shock rings (camera shake).</li>
 *   <li>GREED_MAW: the devouring at from (the victim's chest), to = the wielder's chest, power = the victim's height. A black void opens ringed with fangs and two red eyes over it, souls stream from it to the
 *       wielder, and at the end the jaws close (the bite).</li>
 *   <li>GREED_LIGHTNING: from -> to: black lightning arcs out and tears a small void rift at the end (Dimension Destruction). from = to: crackling round from, power = radius.</li>
 *   <li>GREED_AWAKEN: the form switch / a new tier, at the wielder's feet (follows). Black lightning runs over the body, two red eye glints where the eyes are (the body yaw when known, else towards the camera),
 *       black flames rise, an aura ring runs out. power 9 (a tier): bigger, camera shake.</li>
 *   <li>DEVIL_HIT: a devil's combat hit in its world's style, see {@link GreedDevilHits}.</li>
 * </ul>
 * <b>Budgets (rule 11).</b> The engine caps every activation at 100 quads (400 vertices) and this layer keeps the default; worst-case quads at full detail counted from the code ({@code ctx.seg} thins them by
 * distance and particle setting), the highest the preview measured (tools/vfx_preview/scenes.d/greed.json, devil.json): SLASH 80 (the storm), LANCE 78, FLAME 65, THREADS 24, REAP 74, DOME 62, MAW 45,
 * LIGHTNING 65, AWAKEN 74 (a tier), DEVIL_HIT 46 (Tensura; Chainsaw Man 42); a storm with every cut alive at once would ask for more, and every arc, ring and beam checks
 * {@code hasBudget}, so the cap is never passed. Nothing is kept between frames (short-lived pose vectors only, as every layer); every loop is a constant or a {@code ctx.seg} of at most 20. A followed effect
 * whose creature is gone draws nothing. With {@code greed.classicVfx} / {@code devil.classicVfx} on, the layer draws nothing for those shapes (the old particles still come from the server).
 */
public class GreedLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(GreedVfx.fileName(n)); }

    public static final ResourceLocation CORE = t("core");
    public static final ResourceLocation GLOW = t("glow");
    public static final ResourceLocation VOID = t("void");
    public static final ResourceLocation SLASH = t("slash");
    public static final ResourceLocation BOLT = t("bolt");
    public static final ResourceLocation STREAK = t("streak");
    public static final ResourceLocation RING = t("ring");
    public static final ResourceLocation SCALES = t("scales");
    public static final ResourceLocation SCALE = t("scale");
    public static final ResourceLocation TEETH = t("teeth");
    public static final ResourceLocation GLINT = t("glint");
    public static final ResourceLocation FLAME = t("flame");
    public static final ResourceLocation FEATHER = t("feather");
    public static final ResourceLocation PENTAGRAM = t("pentagram");
    public static final ResourceLocation CRACK = t("crack");
    public static final ResourceLocation RUNES = t("runes");
    public static final ResourceLocation BRAND = t("brand");
    public static final ResourceLocation BLOOD = t("blood");
    public static final ResourceLocation FLASH = t("flash");

    /** Near-black ink with a red cast: the black of the blade, the lightning, the void (drawn with the alpha blend). */
    static final int INK = 0xFF080206;
    static final int CRIMSON = 0xFFD2142E;
    static final int EYE_RED = 0xFFFF2236;
    static final int FANG = 0xFF5A0814;
    static final int WHITE = 0xFFFFFFFF;
    /** A cut of the storm lives this long. */
    private static final float CUT_LIFE = 7f;
    private static final float STORM_R = 4.5f;

    @Override
    public Set<VfxShape> shapes() {
        return EnumSet.of(VfxShape.GREED_SLASH, VfxShape.GREED_LANCE, VfxShape.GREED_FLAME, VfxShape.GREED_THREADS, VfxShape.GREED_REAP, VfxShape.GREED_DOME, VfxShape.GREED_MAW,
                VfxShape.GREED_LIGHTNING, VfxShape.GREED_AWAKEN, VfxShape.DEVIL_HIT);
    }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case GREED_DOME, GREED_LIGHTNING -> 14;
            case GREED_SLASH -> 16;
            case GREED_LANCE, GREED_THREADS, DEVIL_HIT -> 18;
            case GREED_FLAME, GREED_REAP -> 20;
            case GREED_MAW, GREED_AWAKEN -> 24;
            default -> 20;
        };
    }

    @Override
    public int defaultColor(VfxShape s) { return CRIMSON; }

    private static boolean classic(VfxShape s) { return s == VfxShape.DEVIL_HIT ? GreedVfx.classicDevil() : GreedVfx.classicGreed(); }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (classic(inst.shape)) return;
        if (inst.shape == VfxShape.GREED_DOME && inst.power >= 10f) VfxShake.add(inst.payload.from(), Math.min(2.2f, 0.6f + (inst.power - 10f) * 0.15f), 10);
        else if (inst.shape == VfxShape.GREED_LANCE && inst.power >= 1f) VfxShake.add(inst.payload.from(), 1.2f, 8);
        else if (inst.shape == VfxShape.GREED_AWAKEN && inst.power >= 9f) VfxShake.add(inst.payload.from(), 1.0f, 8);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        if (inst.shape == VfxShape.DEVIL_HIT ? GreedVfx.classicDevil() : GreedVfx.classicGreed()) return;
        if (inst.payload.followEntity() >= 0 && Float.isNaN(inst.followYaw(ctx))) return;        // the followed creature is gone (died, left): the effect ends with it
        switch (inst.shape) {
            case GREED_SLASH -> slash(inst, ctx, buf);
            case GREED_LANCE -> lance(inst, ctx, buf);
            case GREED_FLAME -> flame(inst, ctx, buf);
            case GREED_THREADS -> threads(inst, ctx, buf);
            case GREED_REAP -> reap(inst, ctx, buf);
            case GREED_DOME -> dome(inst, ctx, buf);
            case GREED_MAW -> maw(inst, ctx, buf);
            case GREED_LIGHTNING -> lightningShape(inst, ctx, buf);
            case GREED_AWAKEN -> awaken(inst, ctx, buf);
            case DEVIL_HIT -> GreedDevilHits.draw(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ shared pieces (GreedDevilHits uses them too)
    /** 1 while running, ramping 0 -> 1 over the first {@code in} ticks and 1 -> 0 over the last {@code out}. */
    static float life(VfxInstance inst, float age, float in, float out) {
        float a = in <= 0 ? 1 : Mth.clamp(age / in, 0, 1);
        float b = out <= 0 ? 1 : Mth.clamp((inst.duration - age) / out, 0, 1);
        return Math.min(a, b);
    }

    static int al(int argb, float a) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(a, 0, 1)); }

    static int hot(int argb, float t) { return VfxVertexBuffer.whiten(argb, t); }

    static Vector3f lerp(Vector3f a, Vector3f b, float t) { return new Vector3f(a).lerp(b, t); }

    static Vector3f dirOr(Vector3f a, Vector3f b, Vector3f fallback) {
        Vector3f d = new Vector3f(b).sub(a);
        return d.lengthSquared() < 1e-6f ? new Vector3f(fallback) : d.normalize();
    }

    static Vector3f randomDir(RandomSource r) {
        float u = r.nextFloat() * 2 - 1, phi = r.nextFloat() * Mth.TWO_PI, s = Mth.sqrt(1 - u * u);
        return new Vector3f(s * Mth.cos(phi), u, s * Mth.sin(phi));
    }

    /** A plane at {@code c} turned to the camera (c is camera-relative, so -c points at the camera). */
    static VfxPose camPose(Vector3f c) { return VfxPose.facing(c, new Vector3f(c).negate()); }

    /** A figure standing upright at {@code feet}, turned to the camera round the vertical only (flames, wisps). 1 quad. */
    static void upright(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f feet, float width, float height, int argb) {
        Vector3f side = new Vector3f(-feet.z, 0, feet.x);
        if (side.lengthSquared() < 1e-6f) side.set(1, 0, 0);
        side.normalize().mul(width * 0.5f);
        Vector3f top = new Vector3f(feet).add(0, height, 0);
        buf.quad(tex, blend, new Vector3f(feet).sub(side), new Vector3f(feet).add(side), new Vector3f(top).add(side), new Vector3f(top).sub(side), 0, 0, 1, 1, argb, argb);
    }

    /** A ribbon standing on a circle round the pose normal (the Time Magic technique): width along the normal, U wraps {@code repeats} times and scrolls. */
    static void hoop(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, VfxPose pose, float radius, float halfWidth, int segments, float repeats, float uScroll, int argb) {
        Vector3f n = new Vector3f(pose.normal()).mul(halfWidth);
        for (int i = 0; i < segments; i++) {
            if (!buf.hasBudget(4)) return;
            float a0 = Mth.TWO_PI * i / segments, a1 = Mth.TWO_PI * (i + 1) / segments;
            Vector3f m0 = pose.point(Mth.cos(a0) * radius, Mth.sin(a0) * radius), m1 = pose.point(Mth.cos(a1) * radius, Mth.sin(a1) * radius);
            float u0 = uScroll + repeats * i / segments, u1 = uScroll + repeats * (i + 1) / segments;
            buf.quad(tex, blend, new Vector3f(m0).sub(n), new Vector3f(m1).sub(n), new Vector3f(m1).add(n), new Vector3f(m0).add(n), u0, 0, u1, 1, argb, argb);
        }
    }

    /** A spiky impact flash with its glow. 2 quads. */
    static void flash(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, float size, float rot, int col, float alpha) {
        if (alpha <= 0.01f) return;
        buf.billboard(ctx, GLOW, VfxBlend.ADD, c, size * 1.4f, 0, al(col, 0.55f * alpha));
        buf.billboard(ctx, FLASH, VfxBlend.ADD, c, size, rot, al(hot(col, 0.55f), alpha));
    }

    /**
     * Black lightning a -> b: a crimson glow ribbon under an ink core, the zigzag taken from {@code seed} (pass a seed that changes every 2 ticks for the anime flicker). Two passes over the same zigzag so
     * every glow piece lies under every ink piece. 2 quads per piece.
     */
    static void lightning(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f a, Vector3f b, int pieces, float jitter, float width, long seed, int glowCol, float alpha) {
        if (alpha <= 0.01f || a.distanceSquared(b) < 1e-4f) return;
        for (int pass = 0; pass < 2; pass++) {
            RandomSource r = RandomSource.create(seed);
            Vector3f prev = new Vector3f(a);
            for (int i = 1; i <= pieces; i++) {
                Vector3f p = lerp(a, b, i / (float) pieces);
                if (i < pieces) {
                    float j = jitter * Mth.sin(Mth.PI * i / pieces);
                    p.add((r.nextFloat() - 0.5f) * 2 * j, (r.nextFloat() - 0.5f) * 2 * j, (r.nextFloat() - 0.5f) * 2 * j);
                }
                if (pass == 0) buf.beam(ctx, BOLT, VfxBlend.ALPHA, prev, p, width * 8f, width * 8f, 1, 0, al(glowCol, 0.85f * alpha), al(glowCol, 0.85f * alpha));
                else buf.beam(ctx, BOLT, VfxBlend.ALPHA, prev, p, width * 3.5f, width * 3.5f, 1, 0, al(INK, alpha), al(INK, alpha));
                prev = p;
            }
        }
    }

    /** The plane of a crescent cut centred on {@code centre}: the arc's middle (angle 0) passes just by the centre. */
    static VfxPose crescentPose(Vector3f centre, Vector3f facing, float spin, float radius) {
        VfxPose p = VfxPose.facing(centre, facing).spin(spin);
        return new VfxPose(new Vector3f(p.right()).mul(-radius * 0.85f).add(centre), p.right(), p.up(), p.normal());
    }

    /**
     * An ink-black crescent with a blood-red rim on its outer (cutting) edge: the rim is drawn with the alpha blend after the ink (the same batch, so it lies on top) so the red stays saturated on a
     * bright sky; {@code glow} adds a faint additive light along the rim. 2 quads per segment (3 with the glow).
     */
    static void inkArc(VfxVertexBuffer buf, VfxPose pose, float radius, float width, float start, float sweep, int segs, int edgeCol, float alpha, boolean glow) {
        if (alpha <= 0.01f || sweep <= 0.01f) return;
        buf.arc(SLASH, VfxBlend.ALPHA, pose, radius, width, start, sweep, segs, al(INK, alpha));
        buf.arc(SLASH, VfxBlend.ALPHA, pose, radius + width * 0.34f, width * 0.32f, start, sweep, segs, al(edgeCol, alpha));
        if (glow) buf.arc(SLASH, VfxBlend.ADD, pose, radius + width * 0.34f, width * 0.6f, start, sweep, segs, al(edgeCol, 0.35f * alpha));
    }

    /** The flicker seed: the same for 2 ticks, then a new zigzag. */
    static long flicker(VfxInstance inst, float age, int k) { return inst.seed * 31L + k * 7919L + (long) (age * 0.5f); }

    // ------------------------------------------------------------------ GREED_SLASH
    /** One cut of the storm: a crescent that slashes open in 2 ticks and fades in 3. */
    private static void cut(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f at, Vector3f face, float spin, float rad, float local, int col, int segs) {
        float grow = VfxAnim.easeOutCubic(Mth.clamp(local / 2f, 0, 1)), fade = Mth.clamp((CUT_LIFE - local) / 3f, 0, 1);
        inkArc(buf, crescentPose(at, face, spin, rad), rad, 0.5f * rad, -1.2f, 2.4f * grow, segs, col, fade, false);
        if (local < 3) flash(ctx, buf, at, 0.9f, spin, col, 1 - local / 3f);
    }

    private void slash(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        RandomSource r = inst.random();
        if (a.distanceSquared(b) < 0.01f) {
            int segs = ctx.seg(6, 3);                                       // Infinity Divide: a storm of cuts round the wielder
            int n = Mth.clamp(Math.round(inst.power), 1, 16);
            float span = Math.max(1f, inst.duration - CUT_LIFE);
            for (int i = 0; i < n; i++) {
                Vector3f d = randomDir(r);
                float rad = STORM_R * (0.3f + 0.7f * r.nextFloat()), spin = r.nextFloat() * Mth.TWO_PI, size = 1.6f + 0.8f * r.nextFloat();
                Vector3f face = randomDir(r);
                float local = age - i * span / n;
                if (local < 0 || local > CUT_LIFE) continue;
                cut(ctx, buf, new Vector3f(d.x * rad, d.y * rad * 0.35f, d.z * rad).add(a), face, spin, size, local, col, segs);
            }
            float f = life(inst, age, 2, 6);
            buf.ring(RING, VfxBlend.ALPHA, VfxPose.ground(new Vector3f(a).add(0, -0.9f, 0)), STORM_R * 0.92f, STORM_R, ctx.seg(16, 8), 4, age * 0.03f, al(col, 0.6f * f));
            return;
        }
        Vector3f dir = dirOr(a, b, new Vector3f(0, 0, 1));
        float dist = a.distance(b);
        int n = Mth.clamp(Math.round(inst.power), 1, 6);
        int segs = ctx.seg(10, 5);
        boolean fly = dist > 4f;
        float flyT = fly ? Mth.clamp(dist / 2.5f, 3f, 8f) : 0f;
        float u = fly ? Mth.clamp(age / flyT, 0, 1) : 1f;
        Vector3f c = fly ? lerp(a, b, u) : new Vector3f(b);
        float fade = life(inst, age, 0, 5);
        float rad = fly ? 1.4f + 1.8f * u : 1.3f;
        float grow = VfxAnim.easeOutCubic(Mth.clamp(age / 2.5f, 0, 1));
        for (int k = 0; k < n; k++) {
            float spin = (n == 1 ? 0.5f : -0.8f + 1.6f * k / (n - 1)) + (n > 2 ? (r.nextFloat() - 0.5f) * 0.4f : 0f);
            inkArc(buf, crescentPose(c, dir, spin, rad), rad, 0.55f * rad, -1.2f, 2.4f * grow, segs, col, fade, true);
        }
        if (fly && u < 1f) {
            Vector3f tail = lerp(a, b, Math.max(0, u - 0.3f));
            buf.beam(ctx, STREAK, VfxBlend.ADD, tail, c, 0.15f, rad * 1.4f, 1, 0, al(col, 0.1f * fade), al(hot(col, 0.2f), 0.7f * fade));
        }
        float land = age - flyT;
        if (land >= 0 && land < 5) flash(ctx, buf, c, 1.4f + 0.4f * land, land * 0.3f, col, 1 - land / 5f);
        buf.billboard(ctx, GLINT, VfxBlend.ADD, c, rad * 1.2f, 0.785f, al(col, 0.45f * fade * grow));
    }

    // ------------------------------------------------------------------ GREED_LANCE
    private void lance(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = dirOr(a, b, new Vector3f(0, 0, 1));
        float len = a.distance(b);
        float w = Mth.clamp(inst.power, 0.15f, 2f);
        float fade = life(inst, age, 0, 6);
        RandomSource r = inst.random();
        float arrive;
        if (w < 0.5f) {                                                           // the arrow: a black-red streak with a glowing head
            float travel = Mth.clamp(len / 16f, 2f, 5f);
            arrive = travel;
            float headT = Mth.clamp(age / travel, 0, 1), tailT = Mth.clamp(age / travel - 0.4f, 0, 1);
            Vector3f head = lerp(a, b, headT), tail = lerp(a, b, tailT);
            if (headT - tailT > 0.001f) {
                buf.beam(ctx, STREAK, VfxBlend.ADD, tail, head, w * 1.5f, w * 6f, 1, 0, al(col, 0.1f * fade), al(col, 0.5f * fade));
                buf.beam(ctx, BOLT, VfxBlend.ALPHA, tail, head, w * 1f, w * 5f, 1, 0, al(col, 0.2f * fade), al(col, 0.9f * fade));
                buf.beam(ctx, BOLT, VfxBlend.ALPHA, tail, head, w * 0.6f, w * 3.2f, 1, 0, al(INK, 0.3f * fade), al(INK, fade));
            }
            if (headT < 1) {
                buf.billboard(ctx, GLOW, VfxBlend.ADD, head, w * 6f, 0, al(col, 0.7f));
                buf.billboard(ctx, CORE, VfxBlend.ADD, head, w * 2.2f, 0, al(hot(col, 0.6f), 1f));
            }
        } else {                                                                  // the beam: Piercing Meteor, Revolt Brionac
            arrive = 3f;
            float extend = VfxAnim.easeOutCubic(Mth.clamp(age / 3f, 0, 1));
            Vector3f head = new Vector3f(dir).mul(len * extend).add(a);
            float ww = w * (0.35f + 0.65f * fade);
            int seg = ctx.seg(6, 3);
            if (len * extend > 0.05f) {
                buf.beam(ctx, STREAK, VfxBlend.ADD, a, head, ww * 4f, ww * 4f, 1, 0, al(col, 0.2f * fade), al(col, 0.2f * fade));
                buf.beam(ctx, BOLT, VfxBlend.ALPHA, a, head, ww * 4.4f, ww * 3.4f, seg, -age * 0.3f, al(col, 0.85f * fade), al(col, 0.85f * fade));
                buf.beam(ctx, BOLT, VfxBlend.ALPHA, a, head, ww * 3.0f, ww * 2.2f, seg, -age * 0.3f, al(INK, fade), al(INK, fade));
                if (w >= 0.6f) {
                    int pieces = ctx.seg(10, 5);
                    for (int k = 0; k < 2; k++) lightning(ctx, buf, a, head, pieces, ww * 0.9f, 0.05f + 0.02f * w, flicker(inst, age, k), col, fade);
                }
            }
            if (extend < 1) buf.billboard(ctx, CORE, VfxBlend.ADD, head, ww * 3f, 0, al(hot(col, 0.5f), 1f));
            buf.billboard(ctx, GLOW, VfxBlend.ADD, a, ww * 4f, 0, al(col, 0.6f * fade));
        }
        // the end: an ink splash, a flash and a ring facing back along the shot, red sparks
        float ia = age - arrive;
        if (ia >= 0) {
            float span = Math.max(4f, inst.duration - arrive), f = Mth.clamp(1 - ia / span, 0, 1);
            float s = Math.max(0.6f, w * 2.5f);
            buf.billboard(ctx, VOID, VfxBlend.ALPHA, b, s * (1.2f + 0.8f * VfxAnim.easeOutCubic(Mth.clamp(ia / 4f, 0, 1))), r.nextFloat() * Mth.TWO_PI, al(INK, 0.9f * f));
            flash(ctx, buf, b, s * 2f, ia * 0.2f, col, Mth.clamp(1 - ia / 6f, 0, 1));
            float rr = s * (0.8f + 1.6f * VfxAnim.easeOutCubic(Mth.clamp(ia / 8f, 0, 1)));
            buf.ring(RING, VfxBlend.ALPHA, VfxPose.facing(b, new Vector3f(dir).negate()), rr * 0.75f, rr, ctx.seg(16, 8), 3, 0, al(col, f));
            int sparks = ctx.seg(5, 2);
            for (int i = 0; i < sparks; i++) {
                Vector3f d = randomDir(r);
                if (d.dot(dir) > 0) d.reflect(dir);
                float ph = Mth.clamp(ia / 8f, 0, 1);
                buf.billboard(ctx, CORE, VfxBlend.ADD, new Vector3f(d).mul(s * (0.4f + 2.2f * ph)).add(b), 0.22f * (1 - ph) + 0.05f, 0, al(hot(col, 0.3f), 1 - ph));
            }
        }
    }

    // ------------------------------------------------------------------ GREED_FLAME
    private void flame(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        RandomSource r = inst.random();
        float fade = life(inst, age, 2, 6);
        if (fade <= 0.01f) return;
        if (a.distanceSquared(b) < 0.01f) {                                       // Twilight Healing: a ring of black flames, a column of light, motes rising
            float R = Mth.clamp(inst.power, 1f, 8f);
            Vector3f ground = new Vector3f(a).add(0, -0.95f, 0);
            int n = ctx.seg(18, 8);
            for (int i = 0; i < n; i++) {
                float ang = Mth.TWO_PI * i / n + age * 0.02f, h = (0.9f + 0.35f * Mth.sin(age * 0.5f + i * 1.7f)) * 2.4f * fade;
                Vector3f at = new Vector3f(Mth.cos(ang) * R, 0, Mth.sin(ang) * R).add(ground);
                upright(buf, FLAME, VfxBlend.ALPHA, at, h * 0.75f, h, al(INK, 0.92f * fade));
                upright(buf, FLAME, VfxBlend.ALPHA, at, h * 0.38f, h * 0.62f, al(col, fade));
            }
            buf.ring(RING, VfxBlend.ALPHA, VfxPose.ground(new Vector3f(ground).add(0, 0.05f, 0)), R * 0.88f, R * 1.08f, ctx.seg(20, 10), 5, age * 0.02f, al(col, 0.8f * fade));
            buf.beam(ctx, STREAK, VfxBlend.ADD, ground, new Vector3f(ground).add(0, 4f, 0), 2.2f, 2.2f, 1, 0, al(col, 0.45f * fade), al(col, 0.45f * fade));
            int motes = ctx.seg(8, 4);
            for (int i = 0; i < motes; i++) {
                float ang = r.nextFloat() * Mth.TWO_PI, rad = R * Mth.sqrt(r.nextFloat()), ph = Mth.frac(age * 0.05f + r.nextFloat());
                buf.billboard(ctx, CORE, VfxBlend.ADD, new Vector3f(Mth.cos(ang) * rad, ph * 3.5f, Mth.sin(ang) * rad).add(ground), 0.22f, 0, al(hot(col, 0.5f), Mth.sin(ph * Mth.PI) * fade));
            }
            return;
        }
        // Black Flames: ink tongues with crimson hearts rolling out in a cone
        Vector3f dir = dirOr(a, b, new Vector3f(0, 0, 1));
        float len = a.distance(b), coneR = Math.min(3.5f, len * 0.25f);
        VfxPose across = VfxPose.facing(a, dir);
        int n = ctx.seg(16, 6);
        for (int i = 0; i < n; i++) {
            float ph = Mth.frac(age * 0.06f + r.nextFloat()), ang = r.nextFloat() * Mth.TWO_PI, off = r.nextFloat();
            float size = 0.9f + 2.6f * ph, alpha = Mth.sin(ph * Mth.PI) * fade;
            Vector3f side = across.point(Mth.cos(ang) * coneR * ph * off, Mth.sin(ang) * coneR * ph * off).sub(a);
            Vector3f at = new Vector3f(dir).mul(len * ph).add(a).add(side).add(0, -size * 0.35f, 0);
            upright(buf, FLAME, VfxBlend.ALPHA, at, size * 0.75f, size, al(INK, 0.95f * alpha));
            upright(buf, FLAME, VfxBlend.ALPHA, new Vector3f(at).add(0, size * 0.04f, 0), size * 0.38f, size * 0.62f, al(col, alpha));
        }
        buf.billboard(ctx, GLOW, VfxBlend.ADD, a, 1.3f, 0, al(col, 0.6f * fade));
    }

    // ------------------------------------------------------------------ GREED_THREADS
    private void threads(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = dirOr(a, b, new Vector3f(0, 0, 1));
        int n = Mth.clamp(Math.round(inst.power), 3, 10);
        float reach = VfxAnim.easeOutCubic(Mth.clamp(age / 3f, 0, 1)), fade = life(inst, age, 0, 4);
        float snapAt = inst.duration - 6f;
        VfxPose across = VfxPose.facing(b, dir);
        RandomSource r = inst.random();
        for (int i = 0; i < n; i++) {
            float ang = r.nextFloat() * Mth.TWO_PI, rad = 2.5f * Mth.sqrt(r.nextFloat()), rot = r.nextFloat() * Mth.TWO_PI;
            Vector3f end = across.point(Mth.cos(ang) * rad, Mth.sin(ang) * rad);
            Vector3f tip = lerp(a, end, reach);
            buf.beam(ctx, BOLT, VfxBlend.ALPHA, a, tip, 0.05f, 0.16f, 1, 0, al(col, 0.5f * fade), al(col, 0.8f * fade));
            buf.beam(ctx, BOLT, VfxBlend.ADD, a, tip, 0.03f, 0.06f, 1, 0, al(hot(col, 0.5f), fade), al(hot(col, 0.5f), fade));
            float sn = age - snapAt;
            if (sn >= 0 && sn < 4) flash(ctx, buf, end, 0.8f, rot, col, 1 - sn / 4f);
        }
        if (age < 6) buf.billboard(ctx, GLINT, VfxBlend.ADD, a, 1.4f, age * 0.1f, al(hot(col, 0.3f), 1 - age / 6f));
    }

    // ------------------------------------------------------------------ GREED_REAP
    private void reap(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        Vector3f c = ctx.rel(inst.from(ctx)).add(0, 1.0f, 0);
        float R = Mth.clamp(inst.power, 1.5f, 10f);
        RandomSource r = inst.random();
        float start = r.nextFloat() * Mth.TWO_PI;
        float fade = life(inst, age, 0, 6);
        float sweepT = 7f, p = VfxAnim.easeOutCubic(Mth.clamp(age / sweepT, 0, 1));
        float head = start + Mth.TWO_PI * 1.15f * p;
        VfxPose plane = VfxPose.facing(c, new Vector3f(0.12f, 1f, 0.08f));
        inkArc(buf, plane, R * 0.85f, 2.2f, head - 2.8f, 2.8f, ctx.seg(14, 7), col, fade, true);
        if (age > sweepT * 0.6f) {
            float q = Mth.clamp((age - sweepT * 0.6f) / 10f, 0, 1);
            float rr = R * (0.9f + 0.3f * q);
            buf.ring(RING, VfxBlend.ALPHA, VfxPose.ground(new Vector3f(c).add(0, -0.95f, 0)), rr * 0.85f, rr, ctx.seg(16, 8), 4, 0, al(col, (1 - q) * fade));
        }
        // the souls torn from the circle spiral in to the reaper
        int souls = ctx.seg(8, 4);
        for (int i = 0; i < souls; i++) {
            float ang = r.nextFloat() * Mth.TWO_PI, rad0 = R * (0.6f + 0.4f * r.nextFloat()), delay = 3f + 4f * r.nextFloat();
            float ph = Mth.clamp((age - delay) / 10f, 0, 1);
            if (ph <= 0 || ph >= 1) continue;
            float e = VfxAnim.easeInCubic(ph), rad = Mth.lerp(e, rad0, 0.3f), an = ang + ph * 2f;
            Vector3f at = new Vector3f(Mth.cos(an) * rad, -0.6f + ph * 1.4f, Mth.sin(an) * rad).add(c);
            float s = 1.1f * (1 - 0.4f * ph);
            upright(buf, FLAME, VfxBlend.ADD, at, s * 0.5f, s, al(hot(col, 0.45f), Mth.sin(ph * Mth.PI) * fade));
            buf.billboard(ctx, CORE, VfxBlend.ADD, new Vector3f(at).add(0, s * 0.25f, 0), s * 0.35f, 0, al(WHITE, Mth.sin(ph * Mth.PI) * fade));
        }
    }

    // ------------------------------------------------------------------ GREED_DOME
    private void dome(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        boolean burst = inst.power >= 10f;
        float R = Mth.clamp(burst ? inst.power - 10f : inst.power, 0.8f, 12f);
        Vector3f c = ctx.rel(inst.from(ctx));
        if (!burst) {                                                             // the guard: the dome of scales flashes up round the holder
            float open = VfxAnim.easeOutBack(Mth.clamp(age / 4f, 0, 1)), fade = life(inst, age, 0, 5);
            float rad = R * open;
            if (rad <= 0.02f || fade <= 0.01f) return;
            buf.billboard(ctx, SCALES, VfxBlend.ALPHA, c, rad * 2f, age * 0.01f, al(INK, 0.9f * fade));
            buf.billboard(ctx, SCALES, VfxBlend.ADD, c, rad * 2.03f, age * 0.01f, al(col, 0.3f * fade));
            buf.billboard(ctx, GLOW, VfxBlend.ADD, c, rad * 2.8f, 0, al(col, 0.2f * fade));
            buf.ring(RING, VfxBlend.ALPHA, VfxPose.ground(new Vector3f(c).add(0, -0.95f, 0)), rad * 0.8f, rad, ctx.seg(16, 8), 3, age * 0.02f, al(col, 0.7f * fade));
            if (age < 4) flash(ctx, buf, c, rad * 1.6f, 0.3f, col, 1 - age / 4f);
            return;
        }
        // Reflection Fortress: the shell forms in 5 ticks, then shatters; the shards fly out to the radius
        float shell = 1.7f, formT = 5f;
        if (age < formT + 1) {
            float g = Mth.clamp(age / formT, 0, 1), pulse = 1 + 0.06f * Mth.sin(age * 2.5f);
            buf.billboard(ctx, SCALES, VfxBlend.ALPHA, c, shell * 2f * pulse, 0, al(INK, 0.92f * g));
            buf.billboard(ctx, SCALES, VfxBlend.ADD, c, shell * 2.03f * pulse, 0, al(col, 0.4f * g));
        }
        if (age < formT) return;
        float q = Mth.clamp((age - formT) / Math.max(1f, inst.duration - formT), 0, 1), out = VfxAnim.easeOutCubic(q);
        RandomSource r = inst.random();
        int shards = ctx.seg(18, 8);
        for (int i = 0; i < shards; i++) {
            Vector3f d = randomDir(r);
            d.y = d.y * 0.6f;
            float spin = r.nextFloat() * Mth.TWO_PI, speed = 0.7f + 0.3f * r.nextFloat();
            Vector3f at = new Vector3f(d).mul(shell + (R - shell) * out * speed).add(c);
            buf.billboard(ctx, SCALE, VfxBlend.ALPHA, at, 1.1f * (1 - 0.4f * q), spin + q * 6f, al(INK, 1 - q));
            if (i % 3 == 0) buf.billboard(ctx, CORE, VfxBlend.ADD, at, 0.3f, 0, al(hot(col, 0.2f), 1 - q));
        }
        float rr = shell + (R - shell) * out;
        buf.ring(RING, VfxBlend.ALPHA, VfxPose.ground(new Vector3f(c).add(0, -0.9f, 0)), rr * 0.8f, rr, ctx.seg(20, 10), 5, 0, al(col, 1 - q));
        buf.ring(RING, VfxBlend.ADD, VfxPose.ground(c), rr * 0.6f, rr * 0.75f, ctx.seg(16, 8), 4, 0.3f, al(hot(col, 0.3f), 0.7f * (1 - q) * (1 - q)));
        if (q < 0.3f) flash(ctx, buf, c, shell * 3f, q * 2f, col, 1 - q / 0.3f);
    }

    // ------------------------------------------------------------------ GREED_MAW
    private void maw(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        Vector3f c = ctx.rel(inst.from(ctx)), k = ctx.rel(inst.to(ctx));
        float S = Mth.clamp(inst.power * 0.6f, 0.6f, 2.5f);
        float biteAt = inst.duration - 7f, bite = Mth.clamp((age - biteAt) / 3f, 0, 1);
        float open = age < biteAt ? VfxAnim.easeOutBack(Mth.clamp(age / 5f, 0, 1)) : 1 - VfxAnim.easeInCubic(bite);
        float fade = life(inst, age, 0, 3);
        RandomSource r = inst.random();
        if (open > 0.01f) {
            VfxPose face = camPose(c);
            buf.billboard(ctx, GLOW, VfxBlend.ADD, c, S * 3.4f * open, 0, al(col, 0.4f * fade));
            buf.billboard(ctx, VOID, VfxBlend.ALPHA, c, S * 2f * open, age * 0.08f, al(INK, 0.97f * fade));
            float inner = S * (0.55f - 0.45f * Mth.clamp((age - biteAt + 2f) / 4f, 0, 1)) * open;     // the fangs lengthen as the jaws close
            buf.ring(TEETH, VfxBlend.ALPHA, face.spin(age * 0.03f), inner, S * 0.98f * open, ctx.seg(16, 8), 2, 0, al(FANG, 0.95f * fade));
            buf.ring(RING, VfxBlend.ALPHA, face, S * 0.95f * open, S * 1.15f * open, ctx.seg(16, 8), 3, age * 0.04f, al(col, 0.9f * fade));
            float eye = fade * open * (0.75f + 0.25f * Mth.sin(age * 0.9f));
            for (int s = -1; s <= 1; s += 2) {
                Vector3f at = new Vector3f(ctx.camUp).mul(S * 1.15f).add(new Vector3f(ctx.camRight).mul(S * 0.42f * s)).add(c);
                buf.billboard(ctx, GLINT, VfxBlend.ALPHA, at, S * 1.1f, s * 0.12f, al(EYE_RED, eye));
                buf.billboard(ctx, CORE, VfxBlend.ADD, at, S * 0.14f, 0, al(hot(EYE_RED, 0.6f), eye));
            }
        }
        // the souls stream out of the victim to the wielder
        int souls = ctx.seg(8, 4);
        for (int i = 0; i < souls; i++) {
            Vector3f start = randomDir(r).mul(S * 1.4f).add(c);
            float ph = Mth.clamp((age - 2f - i * 0.8f) / 9f, 0, 1);
            if (ph <= 0 || ph >= 1) continue;
            float e = VfxAnim.easeInCubic(ph), e0 = VfxAnim.easeInCubic(Math.max(0, ph - 0.15f));
            Vector3f p = lerp(start, k, e).add(0, Mth.sin(ph * Mth.PI) * 1.2f, 0), q = lerp(start, k, e0).add(0, Mth.sin(Math.max(0, ph - 0.15f) * Mth.PI) * 1.2f, 0);
            buf.beam(ctx, STREAK, VfxBlend.ADD, q, p, 0.2f, 0.2f, 1, 0, al(col, 0.9f * fade), al(hot(col, 0.4f), fade));
        }
        if (age > 8) buf.billboard(ctx, GLOW, VfxBlend.ADD, k, 1.6f, 0, al(col, (0.4f + 0.3f * VfxAnim.pulse(age, 3f)) * fade));
    }

    // ------------------------------------------------------------------ GREED_LIGHTNING
    private void lightningShape(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        float fade = life(inst, age, 0, 5);
        if (a.distanceSquared(b) < 0.01f) {                                       // crackling round a point
            float R = Mth.clamp(inst.power, 0.8f, 6f);
            RandomSource rr = RandomSource.create(flicker(inst, age, 9));
            int n = ctx.seg(5, 3);
            for (int i = 0; i < n; i++) {
                Vector3f p0 = randomDir(rr).mul(R * 0.3f).add(a), p1 = randomDir(rr).mul(R).add(a);
                lightning(ctx, buf, p0, p1, 4, R * 0.15f, 0.07f, flicker(inst, age, i), col, fade);
            }
            buf.billboard(ctx, GLOW, VfxBlend.ADD, a, R * 1.4f, 0, al(col, 0.4f * fade));
            return;
        }
        // Dimension Destruction: black lightning arcs out and tears a small rift at the end
        float len = a.distance(b), extend = VfxAnim.easeOutCubic(Mth.clamp(age / 2f, 0, 1));
        Vector3f head = lerp(a, b, extend);
        int bolts = inst.power >= 1f ? 3 : 2, pieces = ctx.seg(8, 4);
        for (int k = 0; k < bolts; k++) lightning(ctx, buf, a, head, pieces, Math.min(1.2f, 0.08f * len + 0.3f), 0.09f, flicker(inst, age, k), col, fade);
        if (extend >= 1) {
            float S = Mth.clamp(inst.power, 0.6f, 3f), open = VfxAnim.easeOutBack(Mth.clamp((age - 2f) / 4f, 0, 1));
            buf.billboard(ctx, VOID, VfxBlend.ALPHA, b, S * 1.8f * open, -age * 0.1f, al(INK, 0.95f * fade));
            buf.ring(RING, VfxBlend.ALPHA, camPose(b), S * 0.75f * open, S * 1.0f * open, ctx.seg(14, 8), 3, age * 0.05f, al(col, fade));
            if (age < 6) flash(ctx, buf, b, S * 2.2f, age * 0.3f, col, 1 - age / 6f);
        }
    }

    // ------------------------------------------------------------------ GREED_AWAKEN
    private void awaken(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        boolean big = inst.power >= 9f;
        Vector3f feet = ctx.rel(inst.from(ctx));
        float H = 1.8f, fade = life(inst, age, 0, 6);
        // black lightning over the body
        RandomSource rr = RandomSource.create(flicker(inst, age, 5));
        int bolts = big ? 4 : 3;
        for (int i = 0; i < bolts; i++) {
            float ang = rr.nextFloat() * Mth.TWO_PI;
            Vector3f p0 = new Vector3f(Mth.cos(ang) * 0.45f, H + 0.2f, Mth.sin(ang) * 0.45f).add(feet), p1 = new Vector3f(Mth.cos(ang + 1.2f) * 0.55f, 0.05f, Mth.sin(ang + 1.2f) * 0.55f).add(feet);
            lightning(ctx, buf, p0, p1, 5, 0.3f, 0.07f, flicker(inst, age, i), col, fade);
        }
        // two red eye glints where the eyes are: the body yaw when the wielder is known, else towards the camera
        float yaw = inst.followYaw(ctx);
        Vector3f fwd;
        if (Float.isNaN(yaw)) {
            fwd = new Vector3f(feet).negate();
            fwd.y = 0;
            if (fwd.lengthSquared() < 1e-6f) fwd.set(0, 0, 1);
            fwd.normalize();
        } else {
            float rad = yaw * Mth.DEG_TO_RAD;
            fwd = new Vector3f(-Mth.sin(rad), 0, Mth.cos(rad));
        }
        Vector3f side = new Vector3f(-fwd.z, 0, fwd.x);
        float eye = fade * (age < 3 ? 1f : 0.7f + 0.3f * Mth.sin(age * 0.6f));
        for (int s = -1; s <= 1; s += 2) {
            Vector3f at = new Vector3f(feet).add(0, 1.62f, 0).add(new Vector3f(fwd).mul(0.3f)).add(new Vector3f(side).mul(0.12f * s));
            buf.billboard(ctx, GLINT, VfxBlend.ALPHA, at, big ? 1.1f : 0.75f, 0, al(EYE_RED, eye));
            buf.billboard(ctx, CORE, VfxBlend.ADD, at, 0.12f, 0, al(hot(EYE_RED, 0.5f), eye));
        }
        // the ink aura: black flames rise round the body, a ring runs out over the ground
        RandomSource r = inst.random();
        int flames = ctx.seg(6, 3);
        for (int i = 0; i < flames; i++) {
            float ang = Mth.TWO_PI * i / flames + r.nextFloat() * 0.6f, h = (1.0f + 0.4f * Mth.sin(age * 0.7f + i * 2.1f)) * (big ? 1.6f : 1.2f) * fade;
            Vector3f at = new Vector3f(Mth.cos(ang) * 0.55f, 0, Mth.sin(ang) * 0.55f).add(feet);
            upright(buf, FLAME, VfxBlend.ALPHA, at, h * 0.7f, h, al(INK, 0.85f * fade));
            upright(buf, FLAME, VfxBlend.ALPHA, at, h * 0.35f, h * 0.6f, al(col, fade));
        }
        float ringR = 0.5f + (big ? 3.5f : 2.2f) * VfxAnim.easeOutCubic(Mth.clamp(age / 8f, 0, 1));
        buf.ring(RING, VfxBlend.ALPHA, VfxPose.ground(new Vector3f(feet).add(0, 0.06f, 0)), ringR * 0.8f, ringR, ctx.seg(16, 8), 3, 0, al(col, Mth.clamp(1 - age / inst.duration, 0, 1)));
        if (age < 3) flash(ctx, buf, new Vector3f(feet).add(0, 1.1f, 0), big ? 3f : 2f, 0.4f, col, 1 - age / 3f);
    }
}
