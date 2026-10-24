package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.WindMc;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.hash;

/**
 * Wind in the Minecraft style, version C "pixel-anime" (docs/vfx_remake/wind_mc.md, wind_mc_ingame.md). This layer is registered AFTER {@link WindMagicLayer} and {@link WindSpellLayer}, so for the five
 * shapes below it is the one the manager finds; it is the single hook of the change. With {@code vfx.element.wind = V1} (the client config) every call is handed straight to the old layer that owned the
 * shape, so the old look is exactly the old code; the Wind Tornado and Spirit of Zephyr are not in this layer's shapes at all and stay with {@link WindSpellLayer} in every setting.
 *
 * <ul>
 *   <li>WIND_SLASH, Crescent Kamaitachi (also the melee pages of other magics, which pass their own colour): a pixel crescent that holds two smear ghosts behind it, two S ribbons, two tip sparks and a
 *       flash on the first ticks.</li>
 *   <li>WIND_EMPEROR, Slicing Wind Emperor: a big crescent with three ghosts, three white ribbons across it, six crosses along the edge and six rays.</li>
 *   <li>WIND_GALE and FX_WIND_GUST, Gale / Gust Lane: swallows (the old sprite is kept) dart down the lane, each trailing a pixel ribbon, with streaks, two sparks and leaves. FX_WIND_GUST had no layer
 *       before, so it drew only dust.</li>
 *   <li>WIND_RING, Wind Ring: two broken-ring decals in two frames, a ground swirl, six rising ribbons and four sparks.</li>
 * </ul>
 *
 * <p>Style: the 32 px textures {@code wmc_*} are sampled with nearest-neighbour (VfxVertexBuffer), alpha is hard and stepped in four levels, everything is drawn "on twos" (the age is held for two ticks, the
 * partial tick is ignored) and the sprites flip between two frames every three ticks. A wind green colour draws the pack palette with a white vertex colour; any other colour draws the grey twin
 * ({@code _t}) multiplied by it, so the tint of the melee pages keeps working.
 *
 * <p>Cost: at most {@link WindMc#worstQuads} quads per activation (slash 8, emperor 18, gale 16, ring 13; the code it replaces peaked at 72 / 80 / 66 / 100), the activation's vertex budget is
 * {@link WindMc#vertexBudget}, counts shrink with {@code ctx.seg} (distance and particle level of detail of VfxManager). Nothing is allocated per frame (every vector is a static scratch, the textures are
 * resolved once), nothing is sent or computed on a server.
 */
public class WindMcLayer extends AbstractVfxLayer {
    private static final int CRESCENT = 0, RIBBON = 1, SWIRL = 2, SPARK = 3, STREAK = 4, LEAF = 5, RING = 6, FLASH = 7;
    /** [0 = pack palette, 1 = grey twin][name][frame] */
    private static final ResourceLocation[][][] TEX = new ResourceLocation[2][WindMc.TEXTURE_NAMES.length][WindMc.FRAMES];

    static {
        for (int set = 0; set < 2; set++)
            for (int n = 0; n < WindMc.TEXTURE_NAMES.length; n++)
                for (int f = 0; f < WindMc.FRAMES; f++) TEX[set][n][f] = VfxTextures.byName(WindMc.fileName(WindMc.TEXTURE_NAMES[n], f, set == 1));
    }

    private static final AbstractVfxLayer OLD_MAGIC = new WindMagicLayer(), OLD_SPELL = new WindSpellLayer();
    /** colour of the swallow sprites in the pack palette (the mint rim) */
    private static final int SWALLOW_MINT = 0xFFCCEEBE;

    // scratch: the render thread is the only user (one layer instance, drawn one effect at a time)
    private static final Vector3f P0 = new Vector3f(), P1 = new Vector3f(), P2 = new Vector3f(), P3 = new Vector3f();
    private static final Vector3f D = new Vector3f(), S = new Vector3f(), AX = new Vector3f(), AY = new Vector3f(), N = new Vector3f();
    private static final Vector3f POS = new Vector3f(), T0 = new Vector3f(), T1 = new Vector3f(), M = new Vector3f();

    private VfxRenderContext ctx;
    private VfxVertexBuffer buf;
    /** 0 = pack palette textures + white vertex colour, 1 = grey twins + the effect's colour */
    private int set;
    private int rgb;

    @Override public Set<VfxShape> shapes() { return EnumSet.copyOf(WindMc.SHAPES); }

    /** The layer that drew the shape before this change (null for FX_WIND_GUST, which had none). */
    private static AbstractVfxLayer old(VfxShape s) {
        return switch (s) {
            case WIND_SLASH, WIND_RING -> OLD_MAGIC;
            case WIND_EMPEROR, WIND_GALE -> OLD_SPELL;
            default -> null;
        };
    }

    /** With V1, FX_WIND_GUST is again a shape without a layer (dust only), exactly as before. */
    @Override public boolean handles(VfxShape shape) { return shape != VfxShape.FX_WIND_GUST || WindMc.pixelLook(); }

    @Override
    public int defaultDuration(VfxShape s) {
        AbstractVfxLayer o = old(s);
        return o != null ? o.defaultDuration(s) : OLD_SPELL.defaultDuration(s);
    }

    @Override
    public int defaultColor(VfxShape s) {
        AbstractVfxLayer o = old(s);
        return o != null ? o.defaultColor(s) : OLD_SPELL.defaultColor(s);
    }

    @Override
    public int vertexBudget(VfxShape s) {
        if (WindMc.pixelLook()) return WindMc.vertexBudget(s);
        AbstractVfxLayer o = old(s);
        return o != null ? o.vertexBudget(s) : super.vertexBudget(s);
    }

    @Override
    public void onSpawn(VfxInstance inst) {
        AbstractVfxLayer o = old(inst.shape);
        if (o != null) o.onSpawn(inst);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        if (!WindMc.pixelLook()) {
            AbstractVfxLayer o = old(inst.shape);
            if (o != null) o.render(inst, ctx, buf);
            return;
        }
        this.ctx = ctx;
        this.buf = buf;
        int color = inst.color;
        set = WindMc.nativePalette(color) ? 0 : 1;
        rgb = set == 0 ? 0xFFFFFF : color & 0xFFFFFF;
        try {
            switch (inst.shape) {
                case WIND_SLASH -> slash(inst);
                case WIND_EMPEROR -> emperor(inst);
                case WIND_GALE, FX_WIND_GUST -> gale(inst);
                case WIND_RING -> ring(inst);
                default -> { }
            }
        } finally {
            this.ctx = null;
            this.buf = null;
        }
    }

    // ================================================================================================ Crescent Kamaitachi
    private void slash(VfxInstance inst) {
        Vec3 fw = inst.from(ctx), tw = inst.to(ctx);
        float fx = rel(fw.x, ctx.camPos.x), fy = rel(fw.y, ctx.camPos.y), fz = rel(fw.z, ctx.camPos.z);
        D.set(rel(tw.x, ctx.camPos.x) - fx, rel(tw.y, ctx.camPos.y) - fy, rel(tw.z, ctx.camPos.z) - fz);
        float len = D.length();
        if (len < 0.1f) return;
        D.div(len);
        int age = inst.age(), ta = WindMc.onTwos(age), f = WindMc.frame(age);
        float a = WindMc.fade(ta, inst.duration, 2, 5);
        if (a <= 0f) return;
        float p = inst.power, ps = Math.min(p, 3f), radius = 1.7f * p;
        sideOf(D, S);
        float start = Math.min(0.6f, len * 0.3f);
        POS.set(D).mul(start + (len - start) * easeOut(ta / (inst.duration * 0.9f))).add(fx, fy, fz);
        crescentAxes(0.5f + ((int) (inst.seed & 7L) - 3.5f) * 0.06f, 0.35f);
        // the held smear: two older copies of the blade stay behind it, fainter
        int ghosts = ctx.seg(WindMc.SLASH_GHOSTS, 1);
        for (int k = ghosts - 1; k >= 0; k--) {
            float al = k == 0 ? 1f : k == 1 ? 0.6f : 0.35f;
            T0.set(POS).sub(D.x * 0.9f * k, D.y * 0.9f * k, D.z * 0.9f * k);
            plane(tex(CRESCENT, f), T0, AX, AY, radius, radius, argb(WindMc.step4(a * al)), false);
        }
        int ribbons = ctx.seg(WindMc.SLASH_RIBBONS, 1);
        for (int i = 0; i < ribbons; i++) {
            T1.set(POS).sub(D.x * 0.8f * ps, D.y * 0.8f * ps, D.z * 0.8f * ps).add(S.x * (i - 0.5f) * 1.5f * ps, S.y * (i - 0.5f) * 1.5f * ps + (0.25f - 0.5f * i) * ps, S.z * (i - 0.5f) * 1.5f * ps);
            T0.set(T1).sub(D.x * 2.3f * ps, D.y * 2.3f * ps, D.z * 2.3f * ps);
            strip(tex(RIBBON, f), T0, T1, 0.5f * ps, argb(WindMc.step4(a * 0.85f)), false);
        }
        int sparks = ctx.seg(WindMc.SLASH_SPARKS, 1);
        for (int i = 0; i < sparks; i++) {
            float sg = i == 0 ? 1f : -1f;
            T0.set(POS).add(AX.x * radius * 0.72f + AY.x * radius * 0.82f * sg, AX.y * radius * 0.72f + AY.y * radius * 0.82f * sg, AX.z * radius * 0.72f + AY.z * radius * 0.82f * sg);
            buf.billboard(ctx, tex(SPARK, f), VfxBlend.ALPHA, T0, 0.6f * ps, 0f, argb(a));
        }
        if (ta < 3) buf.billboard(ctx, tex(FLASH, f), VfxBlend.ALPHA, POS, 1.6f * ps, 0f, argb(a));
    }

    // ================================================================================================ Slicing Wind Emperor
    private void emperor(VfxInstance inst) {
        Vec3 fw = inst.from(ctx), tw = inst.to(ctx);
        float fx = rel(fw.x, ctx.camPos.x), fy = rel(fw.y, ctx.camPos.y), fz = rel(fw.z, ctx.camPos.z);
        D.set(rel(tw.x, ctx.camPos.x) - fx, rel(tw.y, ctx.camPos.y) - fy, rel(tw.z, ctx.camPos.z) - fz);
        float len = D.length();
        if (len < 0.1f) return;
        D.div(len);
        int age = inst.age(), ta = WindMc.onTwos(age), f = WindMc.frame(age);
        float a = WindMc.fade(ta, inst.duration, 2, 5);
        if (a <= 0f) return;
        float p = inst.power;
        float radius = (2.6f + 2.2f * easeOut(ta / (inst.duration * 0.8f))) * p;
        sideOf(D, S);
        float start = Math.min(2f, len * 0.3f);
        POS.set(D).mul(start + (len - start) * easeOut(ta / (inst.duration * 0.9f))).add(fx, fy, fz);
        crescentAxes(0.35f, 0.25f);
        int ghosts = ctx.seg(WindMc.EMPEROR_GHOSTS, 1);
        for (int k = ghosts - 1; k >= 0; k--) {
            float al = k == 0 ? 1f : k == 1 ? 0.55f : 0.3f;
            T0.set(POS).sub(D.x * 1.4f * p * k, D.y * 1.4f * p * k, D.z * 1.4f * p * k);
            plane(tex(CRESCENT, f), T0, AX, AY, radius, radius, argb(WindMc.step4(a * al)), false);
        }
        // three white ribbons cross the blade
        int ribbons = ctx.seg(WindMc.EMPEROR_RIBBONS, 1);
        for (int i = 0; i < ribbons; i++) {
            float ang = (i - 1) * 0.55f, c = Mth.cos(ang), s = Mth.sin(ang);
            M.set(AX.x * c + AY.x * s, AX.y * c + AY.y * s, AX.z * c + AY.z * s);
            T0.set(POS).sub(M.x * radius * 0.95f, M.y * radius * 0.95f, M.z * radius * 0.95f);
            T1.set(POS).add(M.x * radius * 0.95f, M.y * radius * 0.95f, M.z * radius * 0.95f);
            strip(tex(RIBBON, (f + i) & 1), T0, T1, 1.1f * Math.min(p, 2.2f), argb(WindMc.step4(a * 0.9f)), i == 1);
        }
        // crosses along the cutting edge
        int sparks = ctx.seg(WindMc.EMPEROR_SPARKS, 2);
        for (int i = 0; i < sparks; i++) {
            float ph = (float) Math.toRadians(-100.0 + i * (240.0 / Math.max(1, sparks - 1))), cx = radius * (0.35f - Mth.cos(ph)), cy = radius * Mth.sin(ph);
            T0.set(POS).add(AX.x * cx + AY.x * cy, AX.y * cx + AY.y * cy, AX.z * cx + AY.z * cy);
            buf.billboard(ctx, tex(SPARK, (f + i) & 1), VfxBlend.ALPHA, T0, 0.9f * Math.min(p, 2.2f), 0f, argb(a));
        }
        // thin rays from the middle of the blade
        int rays = ctx.seg(WindMc.EMPEROR_RAYS, 2);
        for (int i = 0; i < rays; i++) {
            float ang = (float) Math.toRadians(15.0 + i * (360.0 / rays)), c = Mth.cos(ang), s = Mth.sin(ang);
            M.set(AX.x * c + AY.x * s, AX.y * c + AY.y * s, AX.z * c + AY.z * s);
            T0.set(POS).add(M.x * radius * 0.3f, M.y * radius * 0.3f, M.z * radius * 0.3f);
            T1.set(POS).add(M.x * radius * 0.75f, M.y * radius * 0.75f, M.z * radius * 0.75f);
            strip(tex(STREAK, f), T0, T1, 0.22f * Math.min(p, 2.2f), argb(WindMc.step4(a * 0.7f)), false);
        }
    }

    // ================================================================================================ Gale / Gust Lane
    private void gale(VfxInstance inst) {
        Vec3 fw = inst.from(ctx), tw = inst.to(ctx);
        float fx = rel(fw.x, ctx.camPos.x), fy = rel(fw.y, ctx.camPos.y), fz = rel(fw.z, ctx.camPos.z);
        D.set(rel(tw.x, ctx.camPos.x) - fx, rel(tw.y, ctx.camPos.y) - fy, rel(tw.z, ctx.camPos.z) - fz);
        float len = D.length();
        if (len < 0.1f) return;
        D.div(len);
        int age = inst.age(), ta = WindMc.onTwos(age), f = WindMc.frame(age);
        float a = WindMc.fade(ta, inst.duration, 2, 6);
        if (a <= 0f) return;
        float p = inst.power, ps = Math.min(p, 2.2f);
        sideOf(D, S);
        AY.set(S).cross(D).normalize();                                  // "up" across the lane (S is the side)
        int swallows = ctx.seg(WindMc.GALE_SWALLOWS, 2);
        int swallowColour = set == 0 ? SWALLOW_MINT : VfxVertexBuffer.whiten(0xFF000000 | rgb, 0.6f);
        float flight = Mth.clamp(inst.duration - 2f * (swallows - 1) - 2f, 6f, 12f);
        // swallows (the old sprite is kept) dart down the lane on staggered, weaving paths, each trailing a pixel ribbon
        for (int i = 0; i < swallows; i++) {
            float t = (ta - i * 2f) / flight;
            if (t <= 0f || t >= 1f) continue;
            float lat = (hash(inst.seed, i, 1) - 0.5f) * 1.6f * ps + Mth.sin(t * 9f + i) * 0.5f * ps, vert = (hash(inst.seed, i, 2) - 0.3f) * 1.2f * ps;
            float along = len * easeInOutSine(t);
            POS.set(D).mul(along).add(fx, fy, fz).add(S.x * lat + AY.x * vert, S.y * lat + AY.y * vert, S.z * lat + AY.z * vert);
            float al = WindMc.step4(a * Mth.clamp(Math.min(t, 1f - t) * 8f, 0f, 1f));
            if (al <= 0f) continue;
            T0.set(POS).sub(D.x * 0.9f * ps, D.y * 0.9f * ps, D.z * 0.9f * ps);                // back
            T1.set(POS).add(D.x * 0.45f * ps, D.y * 0.45f * ps, D.z * 0.45f * ps);             // front
            swallow(ElementFx.WIND_SWALLOW, T0, T1, 1.5f * ps, VfxVertexBuffer.withAlpha(swallowColour, al));
            M.set(T0).sub(D.x * 3.0f * ps, D.y * 3.0f * ps, D.z * 3.0f * ps);
            strip(tex(RIBBON, (f + i) & 1), M, T0, 0.6f * ps, argb(WindMc.step4(al * 0.7f)), (i & 1) == 1);
        }
        // streaks racing down the lane
        int streaks = ctx.seg(WindMc.GALE_STREAKS, 1);
        for (int i = 0; i < streaks; i++) {
            float t = (hash(inst.seed, i, 7) + ta * (0.045f + 0.01f * i)) % 1f, ang = i * 2.4f;
            T1.set(D).mul(t * len).add(fx, fy, fz).add(S.x * Mth.cos(ang) * 0.6f * ps + AY.x * Mth.sin(ang) * 0.6f * ps, S.y * Mth.cos(ang) * 0.6f * ps + AY.y * Mth.sin(ang) * 0.6f * ps,
                    S.z * Mth.cos(ang) * 0.6f * ps + AY.z * Mth.sin(ang) * 0.6f * ps);
            T0.set(T1).sub(D.x * 2.4f * ps, D.y * 2.4f * ps, D.z * 2.4f * ps);
            strip(tex(STREAK, f), T0, T1, 0.3f * ps, argb(WindMc.step4(a * (1f - Math.abs(t - 0.5f)))), false);
        }
        // two crosses ahead of the lane
        float head = Math.min(len, 1f + Math.max(0f, len - 2f) * easeOut(ta / (inst.duration * 0.9f)));
        int sparks = ctx.seg(WindMc.GALE_SPARKS, 1);
        for (int i = 0; i < sparks; i++) {
            T0.set(D).mul(head + 1.3f * ps).add(fx, fy, fz).add(S.x * (i - 0.5f) * 0.9f * ps + AY.x * 0.2f * i, S.y * (i - 0.5f) * 0.9f * ps + AY.y * 0.2f * i, S.z * (i - 0.5f) * 0.9f * ps + AY.z * 0.2f * i);
            buf.billboard(ctx, tex(SPARK, (f + i) & 1), VfxBlend.ALPHA, T0, 0.5f * ps, 0f, argb(a));
        }
        // leaves tumbling along
        int leaves = ctx.seg(WindMc.GALE_LEAVES, 1);
        for (int i = 0; i < leaves; i++) {
            float t = (hash(inst.seed, i, 12) + ta * 0.04f) % 1f, side = (hash(inst.seed, i, 14) - 0.5f) * 1.8f * ps;
            T0.set(D).mul(t * len).add(fx, fy, fz).add(S.x * side + AY.x * 0.3f * ps, S.y * side + AY.y * 0.3f * ps, S.z * side + AY.z * 0.3f * ps);
            buf.billboard(ctx, tex(LEAF, (f + i) & 1), VfxBlend.ALPHA, T0, 0.45f * ps, 0f, argb(a));
        }
    }

    // ================================================================================================ Wind Ring
    private void ring(VfxInstance inst) {
        Vec3 fw = inst.from(ctx);
        float cx = rel(fw.x, ctx.camPos.x), cy = rel(fw.y, ctx.camPos.y), cz = rel(fw.z, ctx.camPos.z);
        int age = inst.age(), ta = WindMc.onTwos(age), f = WindMc.frame(age);
        float p = inst.power, ps = Math.min(p, 2.5f);
        AX.set(1f, 0f, 0f);
        AY.set(0f, 0f, 1f);
        float r0 = 0f, a0 = 0f;
        for (int k = 0; k < WindMc.RING_DECALS; k++) {
            float tk = (ta - 5f * k) / (inst.duration * 0.9f);
            if (tk <= 0f) continue;
            float r = (0.6f + 4.5f * easeOut(tk)) * p * (1f - 0.3f * k);
            float a = WindMc.step4(1f - Math.max(0f, (ta - 5f * k) / inst.duration - 0.4f) / 0.6f);
            if (k == 0) { r0 = r; a0 = a; }
            if (a <= 0f) continue;
            T0.set(cx, cy + 0.1f + 0.06f * k, cz);
            plane(tex(RING, (f + k) & 1), T0, AX, AY, r, r, argb(a), false);
        }
        if (a0 <= 0f) return;
        T0.set(cx, cy + 0.1f, cz);
        plane(tex(SWIRL, f), T0, AX, AY, 1.2f * ps, 1.2f * ps, argb(a0), false);
        int ribbons = ctx.seg(WindMc.RING_RIBBONS, 2);
        AY.set(0f, 1f, 0f);
        float rise = 0.7f + Math.min(ta, 60) * 0.03f;
        for (int j = 0; j < ribbons; j++) {
            float th = j * Mth.TWO_PI / ribbons + ta * 0.12f, rr = r0 * 0.55f;
            AX.set(-Mth.sin(th), 0f, Mth.cos(th));
            T0.set(cx + rr * Mth.cos(th), cy + rise, cz + rr * Mth.sin(th));
            plane(tex(RIBBON, (f + j) & 1), T0, AX, AY, 0.7f * ps, 0.35f * ps, argb(WindMc.step4(a0 * 0.9f)), (j & 1) == 1);
        }
        int sparks = ctx.seg(WindMc.RING_SPARKS, 1);
        for (int j = 0; j < sparks; j++) {
            float th = j * Mth.PI * 0.5f + 0.8f;
            T0.set(cx + r0 * 0.5f * Mth.cos(th), cy + 0.5f + Math.min(ta, 60) * 0.05f, cz + r0 * 0.5f * Mth.sin(th));
            buf.billboard(ctx, tex(SPARK, (f + j) & 1), VfxBlend.ALPHA, T0, 0.45f * ps, 0f, argb(a0));
        }
    }

    // ================================================================================================ helpers (no allocation)
    private ResourceLocation tex(int name, int frame) { return TEX[set][name][frame]; }

    private int argb(float alpha) { return (Math.round(Mth.clamp(alpha, 0f, 1f) * 255f) << 24) | rgb; }

    private static float rel(double world, double cam) { return (float) (world - cam); }

    private static float easeOut(float t) { float c = Mth.clamp(t, 0f, 1f), u = 1f - c; return 1f - u * u * u; }

    private static float easeInOutSine(float t) { return -(Mth.cos(Mth.PI * t) - 1f) / 2f; }

    /** Unit vector perpendicular to dir (horizontal when possible), into out. */
    private static void sideOf(Vector3f dir, Vector3f out) {
        out.set(dir).cross(0f, 1f, 0f);
        if (out.lengthSquared() < 1e-5f) out.set(1f, 0f, 0f);
        out.normalize();
    }

    /**
     * The plane of a flying crescent into AX (the way its tips point), AY and N (its normal): it faces the flight direction D, leaning back by {@code lean}, and AX is turned from straight up
     * towards the side S by {@code roll}.
     */
    private static void crescentAxes(float roll, float lean) {
        N.set(D).mul(Mth.cos(lean)).add(0f, Mth.sin(lean), 0f).normalize();
        T0.set(S).mul(Mth.sin(roll)).add(0f, Mth.cos(roll), 0f);
        float dot = T0.dot(N);
        AX.set(T0).sub(N.x * dot, N.y * dot, N.z * dot).normalize();
        AY.set(N).cross(AX).normalize();
    }

    /** A flat quad of half-size hw x hh on the plane (ax, ay) around c; the texture's top is +ay. */
    private void plane(ResourceLocation tex, Vector3f c, Vector3f ax, Vector3f ay, float hw, float hh, int argb, boolean flip) {
        P0.set(c).sub(ax.x * hw, ax.y * hw, ax.z * hw).sub(ay.x * hh, ay.y * hh, ay.z * hh);
        P1.set(c).add(ax.x * hw, ax.y * hw, ax.z * hw).sub(ay.x * hh, ay.y * hh, ay.z * hh);
        P2.set(c).add(ax.x * hw, ax.y * hw, ax.z * hw).add(ay.x * hh, ay.y * hh, ay.z * hh);
        P3.set(c).sub(ax.x * hw, ax.y * hw, ax.z * hw).add(ay.x * hh, ay.y * hh, ay.z * hh);
        buf.quad(tex, VfxBlend.ALPHA, P0, P1, P2, P3, flip ? 1f : 0f, 0f, flip ? 0f : 1f, 1f, argb, argb);
    }

    /** A camera-facing strip from a to b (camera-relative points); the texture's U runs from a to b, its top edge is the +cross side. */
    private void strip(ResourceLocation tex, Vector3f a, Vector3f b, float width, int argb, boolean flip) {
        float dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z;
        if (dx * dx + dy * dy + dz * dz < 1e-6f) return;
        float mx = (a.x + b.x) * 0.5f, my = (a.y + b.y) * 0.5f, mz = (a.z + b.z) * 0.5f;
        // top = d x mid (the camera is at the origin)
        float tx = dy * mz - dz * my, ty = dz * mx - dx * mz, tz = dx * my - dy * mx;
        float l = Mth.sqrt(tx * tx + ty * ty + tz * tz);
        if (l < 1e-6f) return;
        float k = width * 0.5f / l;
        tx *= k; ty *= k; tz *= k;
        P0.set(a.x - tx, a.y - ty, a.z - tz);
        P1.set(b.x - tx, b.y - ty, b.z - tz);
        P2.set(b.x + tx, b.y + ty, b.z + tz);
        P3.set(a.x + tx, a.y + ty, a.z + tz);
        buf.quad(tex, VfxBlend.ALPHA, P0, P1, P2, P3, flip ? 1f : 0f, 0f, flip ? 0f : 1f, 1f, argb, argb);
    }

    /** The swallow sprite from back to front, as the old beam drew it: U across, V along the flight (the nose at the top of the sprite). */
    private void swallow(ResourceLocation tex, Vector3f back, Vector3f front, float width, int argb) {
        float dx = front.x - back.x, dy = front.y - back.y, dz = front.z - back.z;
        float mx = (back.x + front.x) * 0.5f, my = (back.y + front.y) * 0.5f, mz = (back.z + front.z) * 0.5f;
        // side = d x (-mid)
        float sx = dy * -mz - dz * -my, sy = dz * -mx - dx * -mz, sz = dx * -my - dy * -mx;
        float l = Mth.sqrt(sx * sx + sy * sy + sz * sz);
        if (l < 1e-6f) return;
        float k = width * 0.5f / l;
        sx *= k; sy *= k; sz *= k;
        P0.set(back.x - sx, back.y - sy, back.z - sz);
        P1.set(back.x + sx, back.y + sy, back.z + sz);
        P2.set(front.x + sx, front.y + sy, front.z + sz);
        P3.set(front.x - sx, front.y - sy, front.z - sz);
        buf.quad(tex, VfxBlend.ALPHA, P0, P1, P2, P3, 0f, 0f, 1f, 1f, argb, argb);
    }
}
