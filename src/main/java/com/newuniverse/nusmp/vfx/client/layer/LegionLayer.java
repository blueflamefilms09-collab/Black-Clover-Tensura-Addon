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
 * Legion Magic (Gehenna Game, Endless Domination, Blink Castling, End Empress), drawn after the owner's still: dozens of translucent
 * pink-white glass chess soldiers (striped, ringed bodies) standing on a round black-and-white checkerboard, every one wrapped in a
 * crimson halo that rises off it like cold flame, and a grimoire page of handwriting glowing red above them.
 * <ul>
 *   <li>LEGION_FX1 (cast / projectile): a charge at 'from' (a small board spins up facing the target, a grimoire page flares, diamond
 *       glints are pulled in), then a glass knight leaps from 'from' to 'to' on a crimson streak, shedding pawn / rook / knight
 *       afterimages that each carry their own halo, and lands in a shatter of rays, a ring and checker diamonds. power = size of the
 *       piece (0.6 to 3), duration = flight ticks.</li>
 *   <li>LEGION_FX2 (zone / field): the circular board unrolls on the ground to radius 'power' with a counter-rotating notation ring and
 *       a gold outer ring; three ranks of soldiers (queens and knights inside, rooks in the middle, pawns on the rim) rise one by one
 *       onto it, each in a flickering crimson halo; a crimson curtain stands on the rim, a pulse ring sweeps outward every 26 ticks,
 *       diamonds drift up, and the grimoire page hovers over the middle. Fades in over 8 ticks and out over the last 12.</li>
 *   <li>LEGION_FX3 (impact / signature): a white-red flash with shatter rays, the board slams down and breaks into ring shockwaves, the
 *       soldiers (pawns, rooks, knights) and checker diamonds fly out in all directions (biased along 'to - from'), and a tall glass
 *       queen with a crimson halo rises from the centre and lingers as an afterglow. power = scale.</li>
 * </ul>
 * 'color' is mixed a quarter of the way into the glass-pink of the soldiers and the crimson of the halo.
 */
public class LegionLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation PAWN = t("legion_pawn");
    public static final ResourceLocation ROOK = t("legion_rook");
    public static final ResourceLocation KNIGHT = t("legion_knight");
    public static final ResourceLocation QUEEN = t("legion_queen");
    public static final ResourceLocation BOARD = t("legion_board");
    public static final ResourceLocation RING = t("legion_ring");
    public static final ResourceLocation HALO = t("legion_halo");
    public static final ResourceLocation PAGE = t("legion_page");
    public static final ResourceLocation DIAMOND = t("legion_diamond");
    public static final ResourceLocation STREAK = t("legion_streak");
    public static final ResourceLocation RAYS = t("legion_rays");

    private static final int GLASS = 0xFFF6DCE6;
    private static final int CRIMSON = 0xFFFF2A4E;
    private static final int GOLD = 0xFFD0C080;
    private static final int WHITE = 0xFFFFFFFF;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.LEGION_FX1, VfxShape.LEGION_FX2, VfxShape.LEGION_FX3, VfxShape.LEGION_BOARD); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case LEGION_FX1 -> 16;
            case LEGION_FX2 -> 80;
            case LEGION_BOARD -> 24;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFE890A0; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case LEGION_FX1 -> cast(inst, ctx, buf);
            case LEGION_FX2 -> field(inst, ctx, buf);
            case LEGION_FX3 -> burst(inst, ctx, buf);
            case LEGION_BOARD -> boardBehindHead(inst, ctx, buf);
            default -> { }
        }
    }

    private void boardBehindHead(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = Mth.clamp(Math.min(age / 5f, (inst.duration - age) / 5f), 0f, 1f);
        float yaw = inst.followYaw(ctx);
        if (Float.isNaN(yaw)) yaw = 0f;
        float r = yaw * Mth.DEG_TO_RAD;
        Vector3f rear = new Vector3f(Mth.sin(r), 0, -Mth.cos(r));
        Vector3f centre = ctx.rel(inst.from(ctx).add(0, 2.25, 0).add(rear.x * 0.55, 0, rear.z * 0.55));
        Vector3f facing = new Vector3f(-rear.x, 0, -rear.z);
        VfxPose plane = VfxPose.facing(centre, facing).spin(age * 0.12f);
        float size = Math.max(1.3f, inst.power * 0.8f);
        int glass = VfxVertexBuffer.withAlpha(glass(inst), fade * 0.86f);
        int crimson = VfxVertexBuffer.withAlpha(crimson(inst), fade * 0.62f);
        buf.plane(BOARD, VfxBlend.ALPHA, plane, size, glass);
        buf.plane(RING, VfxBlend.ADD, plane.lift(0.025f).spin(-age * 0.2f), size * 1.14f, crimson);
        for (int n = 0; n < 5; n++) {
            float x = (n % 3 - 1) * size * 0.48f;
            float y = (n / 3 == 0 ? -1 : 1) * size * 0.43f;
            Vector3f pos = plane.point(x, y);
            ResourceLocation tex = n % 3 == 0 ? PAWN : n % 3 == 1 ? ROOK : KNIGHT;
            buf.billboard(ctx, tex, VfxBlend.ALPHA, pos, size * 0.3f, 0f, VfxVertexBuffer.withAlpha(glass, fade));
        }
    }

    // ------------------------------------------------------------------ colours and helpers
    private static int glass(VfxInstance inst) { return VfxVertexBuffer.lerpColor(GLASS, inst.color | 0xFF000000, 0.25f); }
    private static int crimson(VfxInstance inst) { return VfxVertexBuffer.lerpColor(CRIMSON, inst.color | 0xFF000000, 0.25f); }

    private static float smooth(float a, float b, float x) {
        float u = Mth.clamp((x - a) / (b - a), 0f, 1f);
        return u * u * (3f - 2f * u);
    }

    /** A piece standing on the ground at 'base', turned to the camera (yaw only), w wide and h tall. */
    private static void stand(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f base, float w, float h, int argb) {
        Vector3f r = new Vector3f(ctx.camRight.x, 0, ctx.camRight.z);
        if (r.lengthSquared() < 1e-6f) r.set(1, 0, 0);
        r.normalize().mul(w * 0.5f);
        int col = blend.grade(argb, 1f);
        Vector3f up = new Vector3f(0, h, 0);
        buf.quad(tex, blend, new Vector3f(base).sub(r), new Vector3f(base).add(r), new Vector3f(base).add(r).add(up), new Vector3f(base).sub(r).add(up),
                0, 0, 1, 1, col, col);
    }

    /** A piece sprite (w by h) centred on 'c', facing the camera and turned by 'rot' radians around the view axis. */
    private static void tall(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f c, float w, float h, float rot, int argb) {
        float co = Mth.cos(rot), si = Mth.sin(rot);
        Vector3f rx = new Vector3f(ctx.camRight).mul(co * w * 0.5f).add(new Vector3f(ctx.camUp).mul(si * w * 0.5f));
        Vector3f ry = new Vector3f(ctx.camUp).mul(co * h * 0.5f).sub(new Vector3f(ctx.camRight).mul(si * h * 0.5f));
        int col = blend.grade(argb, 1f);
        buf.quad(tex, blend, new Vector3f(c).sub(rx).sub(ry), new Vector3f(c).add(rx).sub(ry), new Vector3f(c).add(rx).add(ry), new Vector3f(c).sub(rx).add(ry),
                0, 0, 1, 1, col, col);
    }

    private static ResourceLocation pieceTex(int k) {
        return switch (Math.floorMod(k, 5)) {
            case 0 -> PAWN;
            case 1 -> ROOK;
            case 2 -> KNIGHT;
            default -> QUEEN;
        };
    }

    /** Offset in the camera plane (radius, angle). */
    private static Vector3f around(VfxRenderContext ctx, Vector3f c, float radius, float ang) {
        return new Vector3f(ctx.camRight).mul(Mth.cos(ang) * radius).add(new Vector3f(ctx.camUp).mul(Mth.sin(ang) * radius)).add(c);
    }

    // ================================================================== FX1: the soldier's leap
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float dur = Math.max(4f, inst.duration);
        float q = dur * 0.25f, endFly = dur * 0.72f;
        float pw = Math.max(0.5f, inst.power);
        Vector3f f = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(f);
        float len = dir.length();
        if (len < 0.05f) { dir.set(0, 0, 1); len = 0.05f; } else dir.div(len);
        int glass = glass(inst), crim = crimson(inst);
        RandomSource r = inst.random();
        float seed = r.nextFloat() * Mth.TWO_PI;

        float u = Mth.clamp((age - q) / (endFly - q), 0f, 1f);
        float e = 0.5f * u + 0.5f * VfxAnim.easeOutCubic(u);

        // ---- charge at 'from': a small board spins up facing the target, the page flares, diamonds are pulled in
        float ch = Mth.clamp(age / q, 0f, 1f);
        float chFade = 1f - smooth(q, q * 1.9f, age);
        if (chFade > 0.01f) {
            VfxPose face = VfxPose.facing(f, dir);
            float open = VfxAnim.easeOutBack(ch);
            float sz = 0.42f * pw * open;
            buf.plane(BOARD, VfxBlend.ALPHA, face.spin(age * 0.35f), sz, VfxVertexBuffer.withAlpha(glass, 0.9f * chFade));
            buf.plane(RING, VfxBlend.ADD, face.lift(0.02f).spin(-age * 0.5f), sz * 1.35f, VfxVertexBuffer.withAlpha(crim, chFade));
            buf.billboard(ctx, PAGE, VfxBlend.ALPHA, new Vector3f(f).add(0, 0.28f * pw, 0), 0.5f * pw * open, -0.25f + 0.1f * Mth.sin(age * 0.6f),
                    VfxVertexBuffer.withAlpha(WHITE, 0.95f * chFade));
            VfxBloom.glow(ctx, buf, f, 0.45f * pw * (0.4f + ch), crim, 0.9f * chFade);
            int n = ctx.seg(6, 3);
            for (int i = 0; i < n; i++) {
                float ang = seed + Mth.TWO_PI * i / n + age * 0.2f;
                float rad = (1.1f - 0.95f * ch) * 0.9f * pw * (0.6f + 0.4f * ((i * 7 % 5) / 4f));
                buf.billboard(ctx, DIAMOND, VfxBlend.ADD, around(ctx, f, rad, ang), 0.28f * pw, ang, VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(crim, 0.5f), chFade));
            }
        }

        // ---- the flight
        if (age >= q && u < 1f) {
            Vector3f head = new Vector3f(dir).mul(len * e).add(f);
            float hh = 0.95f * pw, hw = 0.5f * pw;
            // crimson streak, long and tapering, with a wider soft one behind it
            float tail = Math.min(len * e, 2.6f * pw + 1.5f);
            if (tail > 0.05f) {
                Vector3f a = new Vector3f(dir).mul(-tail).add(head);
                buf.beam(ctx, STREAK, VfxBlend.ADD, head, a, 0.55f * pw, 0.1f * pw, 2, 0, VfxVertexBuffer.withAlpha(crim, 0.9f), VfxVertexBuffer.withAlpha(crim, 0.9f));
                buf.beam(ctx, STREAK, VfxBlend.ADD, head, a, 1.0f * pw, 0.3f * pw, 1, 0.3f + age * 0.1f, VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(crim, 0.6f), 0.5f),
                        VfxVertexBuffer.withAlpha(crim, 0.5f));
            }
            // afterimages: soldiers left behind in the order pawn, rook, knight, pawn, each in its own halo
            for (int k = 2; k >= 1; k--) {
                float eg = e - k * 0.075f * (len > 4 ? 4f / len : 1f) * (1f + 0.4f * k);
                if (eg <= 0f) continue;
                float fade = (1f - k / 5f) * 0.75f * (1f - smooth(0.8f, 1f, u));
                float s = 1f - 0.13f * k;
                Vector3f p = new Vector3f(dir).mul(len * eg).add(f);
                p.y += 0.12f * pw * Mth.sin(seed + k * 1.3f + age * 0.3f);
                VfxBloom.glow(ctx, buf, p, 0.5f * pw * s, crim, 0.55f * fade);
                tall(ctx, buf, HALO, VfxBlend.ADD, new Vector3f(p).add(0, 0.18f * pw * s, 0), hw * 1.9f * s, hh * 1.7f * s, 0, VfxVertexBuffer.withAlpha(crim, 0.6f * fade));
                tall(ctx, buf, pieceTex(k - 1), VfxBlend.ALPHA, p, hw * s, hh * s, -0.3f + 0.05f * k, VfxVertexBuffer.withAlpha(glass, fade));
            }
            // the knight itself
            VfxBloom.glow(ctx, buf, head, 0.7f * pw, crim, 1.0f);
            tall(ctx, buf, HALO, VfxBlend.ADD, new Vector3f(head).add(0, 0.2f * pw, 0), hw * 2.4f, hh * 2.0f, -0.3f, VfxVertexBuffer.withAlpha(crim, 0.85f));
            tall(ctx, buf, KNIGHT, VfxBlend.ALPHA, head, hw, hh, -0.3f, VfxVertexBuffer.withAlpha(glass, 1f));
            tall(ctx, buf, KNIGHT, VfxBlend.ADD, head, hw * 1.05f, hh * 1.05f, -0.3f, VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(crim, 0.5f), 0.28f));
            buf.billboard(ctx, RAYS, VfxBlend.ADD, head, 0.9f * pw, age * 0.4f, VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(crim, 0.5f), 0.55f));
            // glints shed along the path
            int n = ctx.seg(3, 1);
            for (int i = 0; i < n; i++) {
                float back = (i + 1f) / (n + 1f) * Math.min(len * e, 3.2f * pw);
                float tw = Mth.sin(age * 0.9f + i * 2.1f) * 0.5f + 0.5f;
                Vector3f p = new Vector3f(dir).mul(-back).add(head);
                p.add(new Vector3f(ctx.camUp).mul(0.25f * pw * Mth.sin(i * 2.7f + seed))).add(new Vector3f(ctx.camRight).mul(0.25f * pw * Mth.cos(i * 3.1f + seed)));
                buf.billboard(ctx, DIAMOND, VfxBlend.ADD, p, (0.18f + 0.18f * tw) * pw, age * 0.2f + i, VfxVertexBuffer.withAlpha(WHITE, 0.4f + 0.5f * tw));
            }
        }

        // ---- impact at 'to'
        float im = Mth.clamp((age - endFly) / (dur - endFly + 0.0001f), 0f, 1f);
        if (age >= endFly - 0.5f) {
            float on = Mth.clamp((age - endFly + 0.5f) / 1.2f, 0f, 1f);
            float fade = 1f - smooth(0.25f, 1f, im);
            VfxBloom.glow(ctx, buf, to, (0.5f + 0.9f * im) * pw, crim, 1.2f * fade * on);
            buf.billboard(ctx, RAYS, VfxBlend.ADD, to, (0.8f + 1.8f * VfxAnim.easeOutCubic(im)) * pw, seed + im * 0.6f, VfxVertexBuffer.withAlpha(WHITE, 0.95f * fade * on));
            buf.billboard(ctx, RING, VfxBlend.ADD, to, (0.5f + 2.2f * VfxAnim.easeOutCubic(im)) * pw, -im, VfxVertexBuffer.withAlpha(crim, 0.9f * fade * on));
            int n = ctx.seg(6, 3);
            for (int i = 0; i < n; i++) {
                float ang = seed + Mth.TWO_PI * i / n;
                float rad = (0.2f + 1.3f * VfxAnim.easeOutCubic(im)) * pw;
                buf.billboard(ctx, DIAMOND, VfxBlend.ADD, around(ctx, to, rad, ang), 0.3f * pw * (1f - 0.5f * im), ang + im * 3f, VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(crim, 0.6f), fade * on));
            }
            // a tiny pawn tumbling out of the flash
            tall(ctx, buf, PAWN, VfxBlend.ALPHA, new Vector3f(to).add(new Vector3f(ctx.camUp).mul(0.5f * pw * VfxAnim.easeOutCubic(im))), 0.22f * pw, 0.44f * pw, im * 4f,
                    VfxVertexBuffer.withAlpha(glass, 0.9f * fade * on));
        }
    }

    // ================================================================== FX2: the Gehenna board
    private void field(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float life = Math.min(Mth.clamp(age / 8f, 0f, 1f), Mth.clamp((inst.duration - age) / 12f, 0f, 1f));
        float R = Math.max(1f, inst.power);
        if (life <= 0.01f) return;
        float open = VfxAnim.easeOutCubic(Mth.clamp(age / 14f, 0f, 1f));
        Vector3f c = ctx.rel(inst.from(ctx));
        int glass = glass(inst), crim = crimson(inst);
        RandomSource r = inst.random();
        float seed = r.nextFloat() * Mth.TWO_PI;

        VfxPose g = VfxPose.ground(new Vector3f(c).add(0, 0.05f, 0));
        VfxBloom.planeGlow(buf, g, R * open, crim, 0.55f * life);
        float spin = age * 0.01f + seed;
        buf.plane(BOARD, VfxBlend.ALPHA, g.lift(0.01f).spin(spin), R * open, VfxVertexBuffer.withAlpha(glass, 0.95f * life));
        buf.plane(RING, VfxBlend.ADD, g.lift(0.04f).spin(-age * 0.02f), R * 1.08f * open, VfxVertexBuffer.withAlpha(crim, 0.95f * life));
        buf.plane(RING, VfxBlend.ADD, g.lift(0.03f).spin(age * 0.014f), R * 1.28f * open, VfxVertexBuffer.withAlpha(GOLD, 0.6f * life));
        // pulse ring sweeping outward
        float pp = (age % 26f) / 26f;
        buf.plane(RING, VfxBlend.ADD, g.lift(0.05f), Math.max(0.1f, R * (0.1f + 1.0f * pp)), VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(crim, 0.5f), 0.55f * (1f - pp) * life));

        // ranks of soldiers: halos first (they sit behind), then the glass bodies
        float ph = Mth.clamp(0.6f + 0.3f * R, 0.9f, 2.0f);
        float[] radii = {0.30f, 0.60f, 0.86f};
        int[] counts = {3, 5, 8};
        for (int pass = 0; pass < 2; pass++) {
            for (int ring = 0; ring < 3; ring++) {
                int n = ctx.seg(counts[ring], 3);
                for (int i = 0; i < n; i++) {
                    float delay = 2f + ring * 3f + i * 0.5f;
                    float gr = VfxAnim.easeOutBack(Mth.clamp((age - delay) / 9f, 0f, 1f));
                    if (gr <= 0.02f) continue;
                    float ang = Mth.TWO_PI * (i + 0.5f * ring) / n + (ring % 2 == 0 ? spin : spin * 1.0f);
                    Vector3f p = new Vector3f(Mth.cos(ang) * R * radii[ring] * open, 0.06f, Mth.sin(ang) * R * radii[ring] * open).add(c);
                    ResourceLocation tex = ring == 0 ? (i % 2 == 0 ? QUEEN : KNIGHT) : ring == 1 ? (i % 2 == 0 ? ROOK : KNIGHT) : (i % 4 == 3 ? ROOK : PAWN);
                    float hs = ph * (tex == PAWN ? 0.9f : 1.08f) * gr;
                    float ws = hs * 0.5f;
                    float flick = 0.7f + 0.3f * Mth.sin(age * 0.45f + i * 1.9f + ring * 2f);
                    if (pass == 0) {
                        stand(ctx, buf, HALO, VfxBlend.ADD, p, ws * 2.3f, hs * 1.9f, VfxVertexBuffer.withAlpha(crim, 0.48f * life * flick * Mth.clamp(gr, 0f, 1f)));
                    } else {
                        stand(ctx, buf, tex, VfxBlend.ALPHA, p, ws, hs, VfxVertexBuffer.withAlpha(glass, life * Mth.clamp(gr * 1.5f, 0f, 1f)));
                    }
                }
            }
        }

        // the crimson curtain standing on the rim
        int rim = ctx.seg(5, 3);
        for (int i = 0; i < rim && buf.hasBudget(24); i++) {
            float ang = Mth.TWO_PI * (i + 0.5f) / rim - spin * 0.5f;
            Vector3f p = new Vector3f(Mth.cos(ang) * R * open, 0.05f, Mth.sin(ang) * R * open).add(c);
            float fl = 0.65f + 0.35f * Mth.sin(age * 0.35f + i * 2.4f);
            stand(ctx, buf, HALO, VfxBlend.ADD, p, R * 0.75f, (0.9f + 0.5f * R * 0.1f) * (1.0f + 0.25f * fl) * 1.6f, VfxVertexBuffer.withAlpha(crim, 0.36f * fl * life));
        }

        // the grimoire page hovering over the middle
        if (buf.hasBudget(20)) {
            Vector3f pc = new Vector3f(c).add(0, ph * 1.55f + 0.5f + 0.08f * Mth.sin(age * 0.12f), 0);
            float pg = VfxAnim.easeOutBack(Mth.clamp((age - 5f) / 10f, 0f, 1f));
            VfxBloom.glow(ctx, buf, pc, 0.55f * pg, crim, 0.8f * life);
            buf.billboard(ctx, PAGE, VfxBlend.ALPHA, pc, (0.8f + 0.1f * R) * pg, 0.12f * Mth.sin(age * 0.07f), VfxVertexBuffer.withAlpha(WHITE, life));
        }

        // diamonds drifting up
        int m = ctx.seg(6, 2);
        for (int i = 0; i < m && buf.hasBudget(4); i++) {
            float a = r.nextFloat() * Mth.TWO_PI, d = Mth.sqrt(r.nextFloat()) * R * 0.95f, ph0 = r.nextFloat();
            float v = (age * 0.018f + ph0) % 1f;
            Vector3f p = new Vector3f(Mth.cos(a) * d + 0.2f * Mth.sin(age * 0.1f + i), 0.2f + v * (1.8f + 0.2f * R), Mth.sin(a) * d).add(c);
            float tw = Mth.sin(v * Mth.PI);
            buf.billboard(ctx, DIAMOND, VfxBlend.ADD, p, 0.22f + 0.1f * R * 0.3f, age * 0.1f + i, VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(crim, 0.5f), tw * life));
        }
    }

    // ================================================================== FX3: the shatter and the queen
    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float dur = Math.max(8f, inst.duration);
        float prog = Mth.clamp(age / dur, 0f, 1f);
        float pw = Math.max(0.5f, inst.power);
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f hint = new Vector3f(ctx.rel(inst.to(ctx))).sub(c);
        boolean hasHint = hint.lengthSquared() > 0.04f;
        if (hasHint) hint.normalize();
        int glass = glass(inst), crim = crimson(inst);
        RandomSource r = inst.random();
        float seed = r.nextFloat() * Mth.TWO_PI;

        float out = 1f - smooth(0.55f, 1f, prog);
        float flash = 1f - smooth(0f, 0.3f, prog);
        float ex = VfxAnim.easeOutCubic(Mth.clamp(age / 12f, 0f, 1f));

        // ---- the board slams down and breaks
        VfxPose g = VfxPose.ground(new Vector3f(c).add(0, 0.05f, 0));
        float bf = (1f - prog) * (1f - prog);
        buf.plane(BOARD, VfxBlend.ALPHA, g.spin(seed + age * 0.04f), pw * (0.7f + 1.1f * ex), VfxVertexBuffer.withAlpha(glass, 0.85f * bf));
        buf.plane(RING, VfxBlend.ADD, g.lift(0.03f).spin(-age * 0.06f), pw * (0.9f + 1.6f * ex), VfxVertexBuffer.withAlpha(crim, 0.9f * out));
        VfxBloom.planeGlow(buf, g, pw * (0.8f + 1.0f * ex), crim, 0.6f * out);

        // ---- the queen rises from the middle, wrapped in her halo
        float qa = smooth(0.08f, 0.35f, prog) * (1f - smooth(0.7f, 1f, prog));
        if (qa > 0.01f) {
            float qh = 2.2f * pw * (0.7f + 0.3f * VfxAnim.easeOutCubic(Mth.clamp(prog * 3f, 0f, 1f)));
            Vector3f base = new Vector3f(c).add(0, -0.2f * pw + 0.0f, 0);
            Vector3f mid = new Vector3f(base).add(0, qh * 0.5f, 0);
            tall(ctx, buf, HALO, VfxBlend.ADD, mid, qh * 1.3f, qh * 1.25f, 0, VfxVertexBuffer.withAlpha(crim, 0.85f * qa));
            tall(ctx, buf, QUEEN, VfxBlend.ALPHA, mid, qh * 0.5f, qh, 0, VfxVertexBuffer.withAlpha(glass, 0.95f * qa));
            tall(ctx, buf, QUEEN, VfxBlend.ADD, mid, qh * 0.52f, qh * 1.02f, 0, VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(crim, 0.6f), 0.3f * qa));
        }

        // ---- the flash
        VfxBloom.glow(ctx, buf, c, pw * (0.8f + 1.2f * ex), crim, 1.1f * out + 0.3f * flash);
        buf.billboard(ctx, RAYS, VfxBlend.ADD, c, pw * (1.2f + 3.2f * ex), seed + prog * 0.7f, VfxVertexBuffer.withAlpha(WHITE, 0.95f * flash));
        buf.billboard(ctx, RING, VfxBlend.ADD, c, pw * (0.6f + 4.2f * ex), prog * 0.5f, VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(crim, 0.5f), 0.9f * out));
        float ex2 = VfxAnim.easeOutCubic(Mth.clamp((age - 4f) / 14f, 0f, 1f));
        if (age > 4f) buf.billboard(ctx, RING, VfxBlend.ADD, c, pw * (0.4f + 3.2f * ex2), -prog * 0.5f, VfxVertexBuffer.withAlpha(GOLD, 0.7f * (1f - ex2)));

        // ---- soldiers thrown out of the burst
        int pieces = ctx.seg(6, 3);
        for (int i = 0; i < pieces; i++) {
            Vector3f v = unit(r);
            if (hasHint) v.add(new Vector3f(hint).mul(1.1f)).normalize();
            v.y = Math.abs(v.y) * 0.7f + 0.25f;
            float spd = (1.6f + 2.4f * r.nextFloat()) * pw, rot = (r.nextFloat() - 0.5f) * 10f, sz = (0.7f + 0.6f * r.nextFloat()) * pw;
            int kind = r.nextInt(3);
            float pe = VfxAnim.easeOutCubic(Mth.clamp(age / (dur * 0.75f), 0f, 1f));
            float tsec = age / 20f;
            Vector3f p = new Vector3f(v).mul(spd * pe).add(c);
            p.y -= 3.0f * tsec * tsec * 0.6f * (pw > 1 ? 1f : 1f);
            float a = (1f - smooth(0.5f, 1f, prog)) * Mth.clamp(age / 2f, 0f, 1f);
            if (a <= 0.01f || !buf.hasBudget(8)) continue;
            if (i % 2 == 0) {
                Vector3f tailp = new Vector3f(v).mul(spd * Math.max(0f, pe - 0.25f)).add(c);
                tailp.y -= 3.0f * tsec * tsec * 0.6f;
                buf.beam(ctx, STREAK, VfxBlend.ADD, p, tailp, 0.22f * pw, 0.05f * pw, 1, 0, VfxVertexBuffer.withAlpha(crim, 0.8f * a), VfxVertexBuffer.withAlpha(crim, 0.8f * a));
            }
            tall(ctx, buf, pieceTex(kind), VfxBlend.ALPHA, p, sz * 0.5f, sz, age * rot * 0.05f, VfxVertexBuffer.withAlpha(glass, a));
        }

        // ---- checker diamonds
        int shards = ctx.seg(18, 6);
        for (int i = 0; i < shards && buf.hasBudget(4); i++) {
            Vector3f v = unit(r);
            if (hasHint) v.add(new Vector3f(hint).mul(0.8f)).normalize();
            float spd = (2.2f + 3.6f * r.nextFloat()) * pw, sz = (0.18f + 0.22f * r.nextFloat()) * pw;
            float se = VfxAnim.easeOutCubic(Mth.clamp(age / (dur * 0.6f), 0f, 1f));
            Vector3f p = new Vector3f(v).mul(spd * se).add(c);
            p.y -= 1.2f * (age / 20f) * (age / 20f);
            float a = (1f - smooth(0.35f, 0.9f, prog));
            buf.billboard(ctx, DIAMOND, VfxBlend.ADD, p, sz, age * 0.2f + i, VfxVertexBuffer.withAlpha(i % 3 == 0 ? WHITE : VfxVertexBuffer.whiten(crim, 0.4f), a));
        }
    }

    private static Vector3f unit(RandomSource r) {
        float u = r.nextFloat() * 2f - 1f, ph = r.nextFloat() * Mth.TWO_PI, s = Mth.sqrt(Math.max(0f, 1f - u * u));
        return new Vector3f(s * Mth.cos(ph), u, s * Mth.sin(ph));
    }
}
