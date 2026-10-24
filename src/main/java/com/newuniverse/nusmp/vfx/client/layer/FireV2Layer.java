package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.FireV2;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.hash;
import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.life;

/**
 * Fire, version 2 (docs/vfx_remake/fire_ingame.md): a mix of A "anime cel" (flat baked-colour flames drawn with normal alpha) and B "volumetric glow" (soft puffs, additive halos). This layer is registered
 * AFTER {@link FireSpellLayer}, {@link FlameFxLayer} and {@link FlameMagicLayer}, so for the eleven shapes below it is the one the manager finds; it is the single hook of the change. With
 * {@code vfx.element.fire = V1} (the client config) every call is handed straight to the old layer that owned the shape, so the old look is exactly the old code.
 *
 * <ul>
 *   <li>A FIRE_LION, Leo Rugiens: the cel lion head (kept) with a stream of cel flame behind it, tongues and embers.</li>
 *   <li>A FIRE_SPEAR, Sol Linea: a cel lance wrapped in hooked flame tongues, a fireball head.</li>
 *   <li>A FIRE_PILLAR, Ignis Columna: the flame pillar on a ring of fire AND a defensive flame vortex round it (three strands of flame sheets and a spinning ground swirl).</li>
 *   <li>A FIRE_BURST: fireball, radial speed lines, flame ring, tongues, embers, smoke.</li>
 *   <li>B FIRE_SPIRAL, Spiral Flame: three helices of soft puffs round a hot core, a glowing head.</li>
 *   <li>A FIRE_WILD, Wild Bursting Flame (redone to the anime still): a ground circle of flame, a wall of flame tongues round it, fireballs with a long light beam thrown in every direction in three waves.</li>
 *   <li>B FLAMEFX_CAST: the old charge sigil (kept), a glowing stream of puffs, embers, an arrival flare.</li>
 *   <li>A FLAMEFX_ZONE: the old sun-lion sigil and its ring exactly as before (kept), cel flames standing round them.</li>
 *   <li>A FLAMEFX_BURST, A FLAME_EXPLOSION: cel stars, rings, fireballs, tongues, embers.</li>
 *   <li>B FLAME_TRAIL: a glowing stream with a bright head.</li>
 * </ul>
 *
 * <p>Palette: a fire colour (red / orange / gold / yellow) draws the baked textures {@code fnw_*} with a white vertex colour; any other colour (the pages that reuse the shapes with a blue or green flame)
 * draws the grey twin ({@code _t}) multiplied by it. Halos always use the grey twin of the halo texture in the effect's colour, as the old bloom did.
 *
 * <p>Reduced flashes ({@code vfx.element.fireReducedFlashes}): no hard flash stars, no radial speed lines (a soft halo fades instead), no pulsing light, no spinning flash sprites, and the flip-book holds a frame
 * for 7 ticks (at most three changes a second). The pure rules are in {@link FireV2}.
 *
 * <p>Cost: at most {@link FireV2#worstQuads} quads per activation (under the peak of the code each replaces); the activation's vertex budget is {@link FireV2#vertexBudget}; counts shrink with {@code ctx.seg}
 * (distance and particle level of detail of VfxManager). Nothing is allocated per frame (all vectors are static scratch, every texture is resolved once), nothing is sent or computed on a server.
 */
public class FireV2Layer extends AbstractVfxLayer {
    // texture group indexes (the order of FireV2.TEXTURE_NAMES)
    private static final int TONGUE = 0, BALL = 3, BODY = 6, LION = 9, SWOOSH = 11, RING = 13, RAYS = 14, FLASH = 15, SMOKE = 16, CHIP = 18, ORB = 20, BEAM = 22, FLOOR = 23, SWIRL = 24,
            EMBER = 25, HALO = 26, PUFF = 27, BRAYS = 31, BRING = 32, BSTREAK = 33;
    /** [0 = baked colours, 1 = grey twin][name]; the smoke has no twin and shares its set-0 texture. */
    private static final ResourceLocation[][] TEX = new ResourceLocation[2][FireV2.TEXTURE_NAMES.length];

    static {
        for (int n = 0; n < FireV2.TEXTURE_NAMES.length; n++) {
            String name = FireV2.TEXTURE_NAMES[n];
            TEX[0][n] = VfxTextures.byName(FireV2.fileName(name, false));
            TEX[1][n] = FireV2.noTwin(name) ? TEX[0][n] : VfxTextures.byName(FireV2.fileName(name, true));
        }
    }

    /** the two sprites of the old Flame Fx sigils, drawn exactly as FlameFxLayer drew them */
    private static final ResourceLocation SIGIL_LION = VfxTextures.byName("flamefx_lion"), SIGIL_RING = VfxTextures.byName("flamefx_ring"), OLD_GLOW = VfxTextures.GLOW;

    private static final AbstractVfxLayer OLD_SPELL = new FireSpellLayer(), OLD_FX = new FlameFxLayer(), OLD_MAGIC = new FlameMagicLayer();

    // scratch: the render thread is the only user (one layer instance, drawn one effect at a time)
    private static final Vector3f P0 = new Vector3f(), P1 = new Vector3f(), P2 = new Vector3f(), P3 = new Vector3f(), V = new Vector3f();

    private VfxRenderContext ctx;
    private VfxVertexBuffer buf;
    private VfxInstance inst;
    /** 0 = baked colours + white vertex colour, 1 = grey twins + the effect's colour */
    private int set;
    private int rgb, glowRgb, hotRgb;
    private boolean reduced;
    private int ia;
    private float ft, fa;
    /** horizontal unit vector of the camera's right (flames stand up and face the camera about the vertical axis), and the camera's forward */
    private float hrx, hrz, cfx, cfy, cfz;
    /** side vector (horizontal, perpendicular to the last direction given to {@link #sideOf}) */
    private float sdx, sdz;

    @Override public Set<VfxShape> shapes() { return EnumSet.copyOf(FireV2.SHAPES); }

    /** The layer that drew the shape before this change. */
    private static AbstractVfxLayer old(VfxShape s) {
        return switch (s) {
            case FIRE_LION, FIRE_SPEAR, FIRE_PILLAR, FIRE_BURST, FIRE_SPIRAL, FIRE_WILD -> OLD_SPELL;
            case FLAMEFX_CAST, FLAMEFX_ZONE, FLAMEFX_BURST -> OLD_FX;
            default -> OLD_MAGIC;
        };
    }

    @Override public int defaultDuration(VfxShape s) { return old(s).defaultDuration(s); }

    @Override public int defaultColor(VfxShape s) { return old(s).defaultColor(s); }

    @Override
    public int vertexBudget(VfxShape s) { return FireV2.v2Look() ? FireV2.vertexBudget(s) : old(s).vertexBudget(s); }

    @Override public void onSpawn(VfxInstance inst) { old(inst.shape).onSpawn(inst); }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        if (!FireV2.v2Look()) {
            old(inst.shape).render(inst, ctx, buf);
            return;
        }
        this.ctx = ctx;
        this.buf = buf;
        this.inst = inst;
        int color = inst.color;
        set = FireV2.nativeFire(color) ? 0 : 1;
        rgb = set == 0 ? 0xFFFFFF : color & 0xFFFFFF;
        glowRgb = color & 0xFFFFFF;
        hotRgb = VfxVertexBuffer.whiten(0xFF000000 | glowRgb, 0.6f) & 0xFFFFFF;
        reduced = FireV2.reducedFlashes();
        ia = inst.age();
        ft = inst.ageTicks(ctx.partialTick);
        fa = reduced ? ft * 0.4f : ft;
        float rl = Mth.sqrt(ctx.camRight.x * ctx.camRight.x + ctx.camRight.z * ctx.camRight.z);
        if (rl < 1e-4f) { hrx = 1f; hrz = 0f; } else { hrx = ctx.camRight.x / rl; hrz = ctx.camRight.z / rl; }
        Vector3f r = ctx.camRight, u = ctx.camUp;
        cfx = u.y * r.z - u.z * r.y;                                       // forward = up x right
        cfy = u.z * r.x - u.x * r.z;
        cfz = u.x * r.y - u.y * r.x;
        try {
            switch (inst.shape) {
                case FIRE_LION -> lion();
                case FIRE_SPEAR -> spear();
                case FIRE_PILLAR -> pillar();
                case FIRE_BURST -> burst();
                case FIRE_SPIRAL -> spiral();
                case FIRE_WILD -> wild();
                case FLAMEFX_CAST -> cast();
                case FLAMEFX_ZONE -> zone();
                case FLAMEFX_BURST -> fxBurst();
                case FLAME_TRAIL -> trail();
                case FLAME_EXPLOSION -> explosion();
                default -> { }
            }
        } finally {
            this.ctx = null;
            this.buf = null;
            this.inst = null;
        }
    }

    // ================================================================================================ A  Leo Rugiens
    private void lion() {
        Vec3 fw = inst.from(ctx), tw = inst.to(ctx);
        float ax = rel(fw.x, ctx.camPos.x), ay = rel(fw.y, ctx.camPos.y), az = rel(fw.z, ctx.camPos.z);
        float dx = rel(tw.x, ctx.camPos.x) - ax, dy = rel(tw.y, ctx.camPos.y) - ay, dz = rel(tw.z, ctx.camPos.z) - az;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.01f) return;
        dx /= len; dy /= len; dz /= len;
        float p = inst.power, dur = inst.duration;
        float charge = dur * 0.65f;
        float travel = VfxAnim.easeOutCubic(Mth.clamp(ft / charge, 0f, 1f));
        float arrived = Mth.clamp((ft - charge) / (dur - charge), 0f, 1f);
        float fade = life(inst, ft, 3f, 8f);
        float hx = ax + dx * len * travel, hy = ay + dy * len * travel + 0.6f * p * Mth.sin(travel * Mth.PI), hz = az + dz * len * travel;
        float hs = 3.4f * p * (1f + 0.5f * arrived) * Mth.clamp(ft / 4f, 0f, 1f);
        float body = Math.min(len * travel, 5.5f * p);
        float tx = hx - dx * body, ty = hy - dy * body - 0.3f * p, tz = hz - dz * body;
        sideOf(dx, dy, dz);
        // the head first (it is the part that must never be dropped by the activation budget)
        dirSprite(tex(LION + fr(0, 2)), hx, hy, hz, dx, dy, dz, hs * 1.32f, hs * 1.32f / 1.6f, argb(fade * (1f - arrived * 0.7f)));
        if (len * travel > 0.2f) {
            strip(tex(BODY + fr(0, 3)), VfxBlend.ALPHA, tx, ty, tz, hx - dx * hs * 0.2f, hy - dy * hs * 0.2f, hz - dz * hs * 0.2f, 1.7f * p, argb(fade), false);
            strip(tex(BODY + fr(1, 3)), VfxBlend.ALPHA, tx, ty + 0.35f * p, tz, hx - dx * hs * 0.3f, hy - dy * hs * 0.3f + 0.2f * p, hz - dz * hs * 0.3f, 1.0f * p, argb(0.95f * fade), false);
        }
        int tongues = ctx.seg(FireV2.LION_TONGUES, 4);
        for (int i = 0; i < tongues; i++) {
            float s = hash(inst.seed, i, 1);
            float h = (1.5f + 1.2f * s) * p * (0.75f + 0.25f * Mth.sin(fa * 0.8f + i * 1.7f));
            upright(tex(TONGUE + fr(i, 3)), VfxBlend.ALPHA, tx + (hx - tx) * s, ty + (hy - ty) * s + 0.25f * p, tz + (hz - tz) * s, 1.0f * p, h, argb(fade), (i & 1) == 0, 0f);
        }
        sprite(texT(HALO), VfxBlend.ADD, hx - dx * 0.4f, hy - dy * 0.4f, hz - dz * 0.4f, hs * 1.7f, 0f, glowArgb(0.5f * fade));
        if (arrived > 0f) {
            rays(RAYS, hx + dx * hs * 0.1f, hy + dy * hs * 0.1f, hz + dz * hs * 0.1f, hs * 3f, ft * 0.05f, 0.9f * (1f - arrived) * fade);
            flashA(hx + dx * hs * 0.55f, hy + dy * hs * 0.55f + 0.1f * hs, hz + dz * hs * 0.55f, hs * 0.9f * (1f + arrived), 0f, (1f - arrived) * fade);
        }
        int chips = ctx.seg(FireV2.LION_CHIPS, 4);
        for (int i = 0; i < chips; i++) {
            float lf = ((ft + hash(inst.seed, i, 5) * 12f) % 12f) / 12f, s = hash(inst.seed, i, 6), sd = (hash(inst.seed, i, 7) - 0.5f) * 2.4f * p * lf;
            float csz = 0.28f * p * (1f - lf) + 0.05f;
            sprite(tex(CHIP + (i & 1)), VfxBlend.ALPHA, tx + (hx - tx) * s + sdx * sd, ty + (hy - ty) * s + lf * 1.8f * p, tz + (hz - tz) * s + sdz * sd, csz, ft * 0.2f + i, argb(fade * (1f - lf)));
        }
    }

    // ================================================================================================ A  Sol Linea
    private void spear() {
        Vec3 fw = inst.from(ctx), tw = inst.to(ctx);
        float ax = rel(fw.x, ctx.camPos.x), ay = rel(fw.y, ctx.camPos.y), az = rel(fw.z, ctx.camPos.z);
        float dx = rel(tw.x, ctx.camPos.x) - ax, dy = rel(tw.y, ctx.camPos.y) - ay, dz = rel(tw.z, ctx.camPos.z) - az;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.01f) return;
        dx /= len; dy /= len; dz /= len;
        float p = inst.power, dur = inst.duration;
        float travel = VfxAnim.easeOutCubic(Mth.clamp(ft / (dur * 0.55f), 0f, 1f));
        float fade = life(inst, ft, 0f, dur * 0.45f);
        float headD = len * travel, trail = Math.min(headD, 9f);
        float hx = ax + dx * headD, hy = ay + dy * headD, hz = az + dz * headD;
        float tx = ax + dx * (headD - trail), ty = ay + dy * (headD - trail), tz = az + dz * (headD - trail);
        sideOf(dx, dy, dz);
        sprite(tex(BALL + fr(0, 3)), VfxBlend.ALPHA, hx + dx * 0.2f * p, hy + dy * 0.2f * p, hz + dz * 0.2f * p, 1.5f * p, 0f, argb(fade));
        strip(tex(BODY + fr(0, 3)), VfxBlend.ALPHA, tx, ty, tz, hx + dx * 0.7f * p, hy + dy * 0.7f * p, hz + dz * 0.7f * p, 1.5f * p, argb(fade), false);
        strip(tex(BODY + fr(1, 3)), VfxBlend.ALPHA, tx + dx * trail * 0.25f, ty + dy * trail * 0.25f, tz + dz * trail * 0.25f, hx, hy, hz, 0.8f * p, argb(0.95f * fade), false);
        int tongues = ctx.seg(FireV2.SPEAR_TONGUES, 3);
        for (int i = 0; i < tongues; i++) {
            float s = 0.25f + 0.56f * i / Math.max(1f, tongues - 1f);
            upright(tex(TONGUE + fr(i, 3)), VfxBlend.ALPHA, tx + (hx - tx) * s, ty + (hy - ty) * s + 0.12f * p, tz + (hz - tz) * s, 0.9f * p, (1.2f + 0.7f * s) * p, argb(fade), (i & 1) == 1, 0f);
        }
        sprite(texT(HALO), VfxBlend.ADD, hx, hy, hz, 2.6f * p, 0f, glowArgb(0.5f * fade));
        flashA(hx + dx * 0.9f * p, hy + dy * 0.9f * p, hz + dz * 0.9f * p, 1.4f * p, ft * 0.08f, fade);
        int chips = ctx.seg(FireV2.SPEAR_CHIPS, 3);
        for (int i = 0; i < chips; i++) {
            float s = hash(inst.seed, i, 1), dr = Math.max(0f, ft - s * dur * 0.55f) * 0.08f, sd = (hash(inst.seed, i, 2) - 0.5f) * dr * 3f;
            sprite(tex(CHIP + (i & 1)), VfxBlend.ALPHA, ax + dx * headD * s + sdx * sd, ay + dy * headD * s + dr * 2f, az + dz * headD * s + sdz * sd, 0.22f * p, ft * 0.3f,
                    argb(fade * Mth.clamp(1.5f - dr, 0f, 1f)));
        }
    }

    // ================================================================================================ A  Ignis Columna (pillar + defensive vortex)
    private void pillar() {
        Vec3 fw = inst.from(ctx);
        float gx = rel(fw.x, ctx.camPos.x), gy = rel(fw.y, ctx.camPos.y) + 0.05f, gz = rel(fw.z, ctx.camPos.z);
        float p = inst.power, dur = inst.duration;
        float rise = VfxAnim.easeOutBack(Mth.clamp(ft / 6f, 0f, 1f));
        float fade = life(inst, ft, 0f, 10f);
        float h = 4.2f * p * rise, r = 0.85f * p;
        // the vortex first (the part this effect adds), then the old pillar
        vortex(gx, gy, gz, r, h, fade);
        ground(texT(HALO), VfxBlend.ADD, gx, gy, gz, r * 2.0f, 0f, glowArgb(0.8f * fade));
        ground(tex(RING), VfxBlend.ALPHA, gx, gy + 0.02f, gz, r * 1.55f, fa * 0.03f, argb(fade));
        int tongues = ctx.seg(FireV2.PILLAR_TONGUES, 3);
        for (int i = 0; i < tongues; i++) {
            // offset, height factor, width factor of the five tongues (the tall one in the middle)
            float ox = i == 0 ? 0f : i == 1 ? -0.45f : i == 2 ? 0.45f : i == 3 ? -0.22f : 0.25f;
            float hk = i == 0 ? 1f : i == 1 ? 0.85f : i == 2 ? 0.8f : i == 3 ? 1.05f : 0.95f;
            float wk = i == 0 ? 1f : i == 1 ? 0.75f : i == 2 ? 0.8f : i == 3 ? 0.6f : 0.55f;
            float fl = 0.9f + 0.12f * Mth.sin(fa * 0.8f + i * 2.1f);
            upright(tex(TONGUE + fr(i, 3)), VfxBlend.ALPHA, gx + hrx * ox * r, gy, gz + hrz * ox * r, r * 2.5f * wk, h * hk * fl * 1.05f, argb(fade * (i == 0 ? 1f : 0.92f)), (i & 1) == 1, 0f);
        }
        int loose = ctx.seg(FireV2.PILLAR_LOOSE, 3);
        for (int i = 0; i < loose; i++) {
            float ang = hash(inst.seed, i, 1) * Mth.TWO_PI + fa * 0.08f, lf = ((ft + hash(inst.seed, i, 2) * 10f) % 10f) / 10f;
            upright(tex(TONGUE + fr(i + 1, 3)), VfxBlend.ALPHA, gx + Mth.cos(ang) * r * 0.8f, gy + h * (0.2f + 0.8f * lf) - 0.4f * r, gz + Mth.sin(ang) * r * 0.8f,
                    r * 0.9f * (1f - lf * 0.5f), r * 1.6f * (1f - lf * 0.5f), argb(fade * (1f - lf)), false, 0f);
        }
        int chips = ctx.seg(FireV2.PILLAR_CHIPS, 3);
        for (int i = 0; i < chips; i++) {
            float lf = ((ft + hash(inst.seed, i, 3) * 16f) % 16f) / 16f, ang = hash(inst.seed, i, 4) * Mth.TWO_PI + lf * 3f;
            sprite(tex(CHIP + (i & 1)), VfxBlend.ALPHA, gx + Mth.cos(ang) * r * (0.6f + lf), gy + h * (0.5f + lf * 0.9f), gz + Mth.sin(ang) * r * (0.6f + lf), 0.24f * p * (1f - lf) + 0.05f,
                    ft * 0.2f + i, argb(fade * (1f - lf)));
        }
    }

    /** The defensive vortex of Ignis Columna: a spinning swirl of flame on the ground and three strands of flame sheets winding up round the pillar. */
    private void vortex(float gx, float gy, float gz, float r, float h, float fade) {
        float born = Mth.clamp(ft / 5f, 0f, 1f);
        float rv = r * 1.9f, hv = Math.min(h, 3.2f * inst.power);
        ground(tex(SWIRL), VfxBlend.ALPHA, gx, gy + 0.03f, gz, rv * 1.25f * born, -fa * 0.12f, argb(0.9f * fade));
        int pieces = ctx.seg(FireV2.VORTEX_PIECES, 2);
        int strands = FireV2.VORTEX_STRANDS;
        for (int s = 0; s < strands; s++) {
            float prevX = 0f, prevY = 0f, prevZ = 0f;
            for (int k = 0; k <= pieces; k++) {
                float f = (float) k / pieces;
                float phi = f * 4.4f - fa * 0.32f + s * Mth.TWO_PI / strands, rad = rv * (1f - 0.22f * f);
                float qx = gx + Mth.cos(phi) * rad, qy = gy + 0.15f + hv * f * 0.85f, qz = gz + Mth.sin(phi) * rad;
                if (k > 0) {
                    float fm = f - 0.5f / pieces;
                    strip(tex(SWOOSH + fr(s + k, 2)), VfxBlend.ALPHA, prevX, prevY, prevZ, qx + (qx - prevX) * 0.5f, qy + (qy - prevY) * 0.5f, qz + (qz - prevZ) * 0.5f, (0.85f + 0.6f * fm) * inst.power,
                            argb(fade * born * (0.55f + 0.45f * fm)), false);
                }
                prevX = qx; prevY = qy; prevZ = qz;
            }
        }
    }

    // ================================================================================================ A  FIRE_BURST
    private void burst() {
        Vec3 fw = inst.from(ctx);
        float cx = rel(fw.x, ctx.camPos.x), cy = rel(fw.y, ctx.camPos.y), cz = rel(fw.z, ctx.camPos.z);
        float p = inst.power;
        float t = Mth.clamp(ft / inst.duration, 0f, 1f), e = VfxAnim.easeOutCubic(Mth.clamp(ft / 8f, 0f, 1f)), fade = 1f - VfxAnim.easeInCubic(t);
        float bs = 3.4f * p * (0.35f + 0.65f * e) * (1f - t * 0.35f);
        sprite(tex(BALL + fr(0, 3)), VfxBlend.ALPHA, cx, cy, cz, bs, 0f, argb(fade));
        ground(tex(RING), VfxBlend.ALPHA, cx, cy - 0.4f * p, cz, 3.2f * p * e * 1.05f, fa * 0.05f, argb(fade));
        int n = ctx.seg(FireV2.BURST_TONGUES, 5);
        for (int i = 0; i < n; i++) {
            float ang = Mth.TWO_PI * i / n + hash(inst.seed, i, 1) * 0.5f, dd = 2.4f * p * e * (0.7f + 0.5f * hash(inst.seed, i, 2));
            upright(tex(TONGUE + fr(i, 3)), VfxBlend.ALPHA, cx + Mth.cos(ang) * dd, cy + 1.4f * p * e - 2.5f * t * t * p - 0.6f * p, cz + Mth.sin(ang) * dd, 1.2f * p * (1f - t * 0.5f),
                    1.9f * p * (1f - t * 0.6f), argb(fade), (i & 1) == 0, 0f);
        }
        sprite(texT(HALO), VfxBlend.ADD, cx, cy, cz, 6.0f * p * e, 0f, glowArgb(0.7f * fade));
        rays(RAYS, cx, cy, cz, 6.4f * p * e, ft * 0.04f, (float) Math.pow(1f - t, 1.5));
        flashTicks(cx, cy, cz, 4.2f * p * Math.max(0f, 1f - ft / 7f), 0.2f, 1f, 1f);
        int k = ctx.seg(FireV2.BURST_CHIPS, 5);
        for (int i = 0; i < k; i++) {
            float ang = hash(inst.seed, i, 4) * Mth.TWO_PI, up = 0.5f + hash(inst.seed, i, 5), dd = 3.8f * p * e;
            sprite(tex(CHIP + (i & 1)), VfxBlend.ALPHA, cx + Mth.cos(ang) * dd, cy + up * dd - 3f * t * t * p, cz + Mth.sin(ang) * dd, 0.3f * p, ft * 0.3f + i, argb(fade));
        }
        if (t > 0.3f) {
            int sm = ctx.seg(FireV2.BURST_SMOKE, 2);
            float st = (t - 0.3f) / 0.7f;
            for (int i = 0; i < sm; i++) {
                sprite(tex(SMOKE + (i & 1)), VfxBlend.ALPHA, cx + (hash(inst.seed, i, 6) - 0.5f) * 2f * p, cy + st * 2.2f * p, cz + (hash(inst.seed, i, 7) - 0.5f) * 2f * p, (1.4f + st * 1.6f) * p, 0f, whiteArgb(1f - st));
            }
        }
    }

    // ================================================================================================ B  Spiral Flame
    private void spiral() {
        Vec3 fw = inst.from(ctx), tw = inst.to(ctx);
        float ax = rel(fw.x, ctx.camPos.x), ay = rel(fw.y, ctx.camPos.y), az = rel(fw.z, ctx.camPos.z);
        float dx = rel(tw.x, ctx.camPos.x) - ax, dy = rel(tw.y, ctx.camPos.y) - ay, dz = rel(tw.z, ctx.camPos.z) - az;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.01f) return;
        dx /= len; dy /= len; dz /= len;
        float p = inst.power, dur = inst.duration;
        sideOf(dx, dy, dz);
        // up across the helix = side x d
        float ux = -sdz * dy, uy = sdz * dx - sdx * dz, uz = sdx * dy;
        float ul = Mth.sqrt(ux * ux + uy * uy + uz * uz);
        if (ul > 1e-5f) { ux /= ul; uy /= ul; uz /= ul; }
        float grow = Mth.clamp(ft / (dur * 0.6f), 0f, 1f), travel = VfxAnim.easeOutCubic(grow);
        float fade = life(inst, ft, 0f, dur * 0.4f);
        float headD = len * travel;
        float hx = ax + dx * headD, hy = ay + dy * headD, hz = az + dz * headD;
        sprite(texT(HALO), VfxBlend.ADD, hx, hy, hz, 6.2f * p, 0f, glowArgb(0.9f * fade));
        strip(tex(BSTREAK), VfxBlend.ADD, ax, ay, az, hx, hy, hz, 0.9f * p, hotArgb(0.9f * fade), false);
        int pts = ctx.seg(FireV2.SPIRAL_POINTS - 1, 5);
        float spin = reduced ? 0.45f : 0.9f;
        for (int s = 0; s < FireV2.SPIRAL_STRANDS; s++) {
            for (int k = 0; k <= pts; k++) {
                float f = (float) k / pts, phi = f * 9f - ft * spin + s * Mth.TWO_PI / FireV2.SPIRAL_STRANDS, rad = (0.25f + 1.15f * f * f) * p;
                sprite(tex(PUFF + ((k + s) & 3)), VfxBlend.ALPHA, ax + dx * headD * f + sdx * Mth.cos(phi) * rad + ux * Mth.sin(phi) * rad, ay + dy * headD * f + uy * Mth.sin(phi) * rad,
                        az + dz * headD * f + sdz * Mth.cos(phi) * rad + uz * Mth.sin(phi) * rad, (0.45f + 0.9f * f) * p, k + s * 2f + fa * 0.05f, argb(0.75f * fade * (0.3f + 0.7f * f)));
            }
        }
        sprite(tex(PUFF + 2), VfxBlend.ALPHA, hx + dx * 0.3f * p, hy + dy * 0.3f * p, hz + dz * 0.3f * p, 3.0f * p, 0f, argb(fade));
        sprite(texT(HALO), VfxBlend.ADD, hx + dx * 0.3f * p, hy + dy * 0.3f * p, hz + dz * 0.3f * p, 1.8f * p, 0f, hotArgb(fade));
        int e = ctx.seg(FireV2.SPIRAL_EMBERS, 5);
        for (int i = 0; i < e; i++) {
            float f = hash(inst.seed, i, 1);
            if (f > travel) continue;
            float ang = hash(inst.seed, i, 2) * Mth.TWO_PI + fa * 0.2f, rad = (0.6f + 1.2f * f) * p;
            sprite(tex(EMBER), VfxBlend.ADD, ax + dx * len * f + sdx * Mth.cos(ang) * rad + ux * Mth.sin(ang) * rad, ay + dy * len * f + uy * Mth.sin(ang) * rad,
                    az + dz * len * f + sdz * Mth.cos(ang) * rad + uz * Mth.sin(ang) * rad, 0.3f * p, 0f, argb(fade));
        }
    }

    // ================================================================================================ A  Wild Bursting Flame (a ground circle of flame, fireballs with a light beam in every direction)
    private void wild() {
        Vec3 fw = inst.from(ctx);
        float cx = rel(fw.x, ctx.camPos.x), cy = rel(fw.y, ctx.camPos.y), cz = rel(fw.z, ctx.camPos.z);
        float r = inst.power, rm = Math.min(r, 3f);
        float fade = life(inst, ft, 0f, 10f);
        float open = VfxAnim.easeOutCubic(Mth.clamp(ft / 6f, 0f, 1f));
        // the ground circle of flame: the carpet, its ring, and a wall of cel tongues standing round the rim
        ground(texT(HALO), VfxBlend.ADD, cx, cy + 0.02f, cz, r * 1.9f * open, 0f, glowArgb(0.55f * fade));
        ground(tex(FLOOR), VfxBlend.ALPHA, cx, cy + 0.04f, cz, r * 1.25f * open, fa * 0.02f, argb(0.95f * fade));
        ground(tex(RING), VfxBlend.ALPHA, cx, cy + 0.07f, cz, r * 1.4f * open, -fa * 0.03f, argb(fade));
        // the fireballs, three waves six ticks apart: a long light beam from the burst out to each round ball (the thin yellow beams and big round balls of the still)
        int n = ctx.seg(FireV2.WILD_BALLS, 3);
        for (int w = 0; w < FireV2.WILD_WAVES; w++) {
            float local = ft - w * 6f, t = Mth.clamp(local / 16f, 0f, 1f);
            if (local < 0f || t >= 1f) continue;
            float e = VfxAnim.easeOutCubic(Mth.clamp(local / 9f, 0f, 1f)), env = Mth.clamp((1f - t) / 0.4f, 0f, 1f), grow = Mth.clamp(local / 3f, 0f, 1f);
            for (int i = 0; i < n; i++) {
                int hi = i + w * 31;
                float ang = Mth.TWO_PI * (i + 0.5f * w) / n + (hash(inst.seed, hi, 1) - 0.5f) * 0.5f, lift = (hash(inst.seed, hi, 2) - 0.15f) * 1.1f;
                float ox = Mth.cos(ang), oy = lift, oz = Mth.sin(ang), ol = Mth.sqrt(ox * ox + oy * oy + oz * oz);
                ox /= ol; oy /= ol; oz /= ol;
                float reach = r * (0.9f + 0.8f * hash(inst.seed, hi, 4)) * e + 0.3f * r;
                float bx = cx + ox * reach, by = cy + 1f + oy * reach, bz = cz + oz * reach;
                float size = 0.95f * rm * (0.7f + 0.6f * hash(inst.seed, hi, 3)) * (0.85f + 0.15f * env) * grow;
                strip(tex(BEAM), VfxBlend.ALPHA, cx + ox * 0.3f * r, cy + 1f + oy * 0.3f * r, cz + oz * 0.3f * r, bx, by, bz, 0.55f * rm * (0.7f + 0.3f * env), argb(0.95f * fade * env), false);
                sprite(texT(HALO), VfxBlend.ADD, bx, by, bz, size * 1.9f, 0f, glowArgb(0.4f * fade * env));
                sprite(tex(ORB + fr(i + w, 2)), VfxBlend.ALPHA, bx, by, bz, size, 0f, argb(fade * env));
            }
        }
        int u = ctx.seg(FireV2.WILD_UPRIGHTS, 7);
        for (int i = 0; i < u; i++) {
            float a = Mth.TWO_PI * i / u + hash(inst.seed, i, 8) * 0.15f, ph = hash(inst.seed, i, 9) * Mth.TWO_PI;
            float h = (0.6f + 0.5f * hash(inst.seed, i, 10)) * rm * 0.5f * (0.85f + 0.15f * Mth.sin(fa * 0.5f + ph)) * open;
            upright(tex(TONGUE + fr(i, 3)), VfxBlend.ALPHA, cx + Mth.cos(a) * r * 1.1f * open, cy + 0.05f, cz + Mth.sin(a) * r * 1.1f * open, h * 0.85f, h, argb(0.95f * fade), (i & 1) == 0, 0f);
        }
        sprite(texT(HALO), VfxBlend.ADD, cx, cy + 1f, cz, Math.max(5f, r * 1.1f), 0f, glowArgb(0.5f * fade * FireV2.pulse(ft, 0.8f, 0.4f, reduced)));
        int k = ctx.seg(FireV2.WILD_CHIPS, 4);
        for (int i = 0; i < k; i++) {
            float lt = ((ft + hash(inst.seed, i, 4) * 14f) % 14f) / 14f, ang = hash(inst.seed, i, 5) * Mth.TWO_PI, d = r * (0.3f + 1.0f * lt);
            sprite(tex(CHIP + (i & 1)), VfxBlend.ALPHA, cx + Mth.cos(ang) * d, cy + 1f + 2.2f * lt - 2.6f * lt * lt, cz + Mth.sin(ang) * d, 0.3f, ft * 0.3f, argb(fade * (1f - lt)));
        }
    }

    // ================================================================================================ B  Flame Cast (the old charge sigil is kept)
    private void cast() {
        Vec3 fw = inst.from(ctx), tw = inst.to(ctx);
        float ax = rel(fw.x, ctx.camPos.x), ay = rel(fw.y, ctx.camPos.y), az = rel(fw.z, ctx.camPos.z);
        float bx = rel(tw.x, ctx.camPos.x), by = rel(tw.y, ctx.camPos.y), bz = rel(tw.z, ctx.camPos.z);
        float dx = bx - ax, dy = by - ay, dz = bz - az;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.05f) { dx = 0f; dy = 0f; dz = 1f; len = 0.05f; bx = ax; by = ay; bz = az + 0.05f; } else { dx /= len; dy /= len; dz /= len; }
        float p = inst.power, dur = inst.duration, fade = life(inst, ft, 0f, 6f);
        float charge = Mth.clamp(ft / (dur * 0.25f), 0f, 1f), run = Mth.clamp((ft - dur * 0.12f) / (dur * 0.7f), 0f, 1f);
        float head = VfxAnim.easeOutCubic(run), tail = Math.max(0f, head - 0.55f);
        // the charge: the sun-lion sigil facing along the cast and its ring, exactly as the old layer drew them
        float open = VfxAnim.easeOutBack(charge) * (1f - 0.6f * Mth.clamp((ft - dur * 0.3f) / (dur * 0.3f), 0f, 1f));
        if (open > 0.02f) {
            float ox = ax + dx * 0.25f * p, oy = ay + dy * 0.25f * p, oz = az + dz * 0.25f * p;
            facing(dx, dy, dz);
            spinPlane(SIGIL_LION, ox, oy, oz, ft * 0.07f, 0.65f * p * open, VfxBlend.ADD, (Math.round(Mth.clamp(0.9f * fade, 0f, 1f) * 255f) << 24) | glowRgb);
            spinPlane(SIGIL_RING, ox + dx * 0.04f, oy + dy * 0.04f, oz + dz * 0.04f, -ft * 0.11f, 0.5f * p * open, VfxBlend.ADD, (Math.round(Mth.clamp(0.8f * fade, 0f, 1f) * 255f) << 24) | hotRgb);
            sprite(texT(HALO), VfxBlend.ADD, ax, ay, az, 1.1f * p * open, 0f, glowArgb(0.8f * fade * (1f - run * 0.6f)));
        }
        if (head <= 0.001f) return;
        float hx = ax + (bx - ax) * head, hy = ay + (by - ay) * head, hz = az + (bz - az) * head;
        float tx = ax + (bx - ax) * tail, ty = ay + (by - ay) * tail, tz = az + (bz - az) * tail;
        sideOf(dx, dy, dz);
        sprite(texT(HALO), VfxBlend.ADD, hx, hy, hz, 4.6f * p, 0f, glowArgb(0.9f * fade));
        strip(tex(BSTREAK), VfxBlend.ALPHA, tx, ty, tz, hx + dx * 0.6f * p, hy + dy * 0.6f * p, hz + dz * 0.6f * p, 1.7f * p, argb(0.9f * fade), false);
        strip(tex(BSTREAK), VfxBlend.ADD, tx, ty, tz, hx + dx * 0.6f * p, hy + dy * 0.6f * p, hz + dz * 0.6f * p, 0.7f * p, hotArgb(fade), false);
        int puffs = ctx.seg(FireV2.CAST_PUFFS, 3);
        for (int i = 0; i < puffs; i++) {
            float s = (i + 0.5f) / puffs;
            sprite(tex(PUFF + (i & 3)), VfxBlend.ALPHA, tx + (hx - tx) * s, ty + (hy - ty) * s, tz + (hz - tz) * s, (0.5f + 0.9f * s) * p, i + fa * 0.1f, argb(0.8f * fade * (0.3f + 0.7f * s)));
        }
        sprite(tex(PUFF + 1), VfxBlend.ALPHA, hx + dx * 0.3f * p, hy + dy * 0.3f * p, hz + dz * 0.3f * p, 1.7f * p, 0f, argb(fade));
        sprite(texT(HALO), VfxBlend.ADD, hx + dx * 0.3f * p, hy + dy * 0.3f * p, hz + dz * 0.3f * p, 1.2f * p, 0f, hotArgb(fade));
        int e = ctx.seg(FireV2.CAST_EMBERS, 4);
        for (int i = 0; i < e; i++) {
            float s = tail + (head - tail) * hash(inst.seed, i, 1), lf = Mth.frac(ft * 0.07f + hash(inst.seed, i, 2)), sd = (hash(inst.seed, i, 3) - 0.5f) * 0.6f * p;
            sprite(tex(EMBER), VfxBlend.ADD, ax + (bx - ax) * s + sdx * sd, ay + (by - ay) * s + hash(inst.seed, i, 4) * 0.5f * p + lf * 0.5f, az + (bz - az) * s + sdz * sd, 0.34f * p * (1f - lf) + 0.06f, 0f,
                    argb(Mth.sin(lf * Mth.PI) * fade));
        }
        if (head > 0.88f) {
            float k = (head - 0.88f) / 0.12f;
            rays(BRAYS, bx, by, bz, 3.0f * p * k, ft * 0.1f, (1f - 0.4f * k) * fade);
            if (reduced) sprite(texT(HALO), VfxBlend.ADD, bx, by, bz, 2.4f * p * k, 0f, glowArgb(0.5f * (1f - 0.4f * k) * fade));
            ground(tex(BRING), VfxBlend.ADD, bx, by + 0.08f, bz, 0.5f * p + 0.9f * p * k, fa * 0.1f, argb((1f - k) * 0.8f * fade));
        }
    }

    // ================================================================================================ A  Flame Zone (the old sigil and ring are kept; the flames round them are new)
    private void zone() {
        Vec3 fw = inst.from(ctx);
        float cx = rel(fw.x, ctx.camPos.x), cy = rel(fw.y, ctx.camPos.y), cz = rel(fw.z, ctx.camPos.z);
        float R = inst.power, dur = inst.duration, fade = life(inst, ft, 8f, 12f);
        if (fade <= 0.01f) return;
        float open = VfxAnim.easeOutBack(Mth.clamp(ft / 10f, 0f, 1f));
        float gy = cy + 0.06f;
        // the old disc, exactly as FlameFxLayer drew it: the heat glow, the sun-lion sigil, the rim ring
        groundPose(OLD_GLOW, VfxBlend.ADD, cx, gy - 0.02f, cz, R * open * 1.5f, 0f, (Math.round(Mth.clamp(0.45f * 0.55f * fade, 0f, 1f) * 255f) << 24) | glowRgb);
        groundPose(SIGIL_LION, VfxBlend.ADD, cx, gy, cz, R * 0.92f * open, ft * 0.018f, (Math.round(Mth.clamp(0.85f * fade, 0f, 1f) * 255f) << 24) | glowRgb);
        groundPose(SIGIL_RING, VfxBlend.ADD, cx, gy + 0.03f, cz, R * 1.04f * open, -ft * 0.03f,
                (Math.round(Mth.clamp(0.95f * fade * (0.8f + 0.2f * Mth.sin(fa * 0.3f)), 0f, 1f) * 255f) << 24) | hotRgb);
        // the new flames: a ring of cel tongues standing on the rim, a small ring inside, rising embers
        int n = ctx.seg(FireV2.ZONE_TONGUES, 6);
        for (int i = 0; i < n; i++) {
            float a = Mth.TWO_PI * i / n + hash(inst.seed, i, 1) * 0.2f, ph = hash(inst.seed, i, 2) * Mth.TWO_PI;
            float h = (0.55f + 0.35f * hash(inst.seed, i, 3)) * Math.max(1f, R * 0.35f) * (0.8f + 0.2f * Mth.sin(fa * 0.5f + ph)) * open * 1.5f;
            upright(tex(TONGUE + fr(i, 3)), VfxBlend.ALPHA, cx + Mth.cos(a) * R * 0.98f, cy + 0.05f, cz + Mth.sin(a) * R * 0.98f, h * 0.8f, h, argb(0.95f * fade), (i & 1) == 0, 0f);
        }
        int m = ctx.seg(FireV2.ZONE_INNER, 3);
        for (int i = 0; i < m; i++) {
            float a = Mth.TWO_PI * i / m + fa * 0.02f, h = 0.5f * Math.max(1f, R * 0.3f) * (0.8f + 0.2f * Mth.sin(fa * 0.6f + i)) * open;
            upright(tex(TONGUE + fr(i + 1, 3)), VfxBlend.ALPHA, cx + Mth.cos(a) * R * 0.45f, cy + 0.05f, cz + Mth.sin(a) * R * 0.45f, h * 0.7f, h, argb(0.7f * fade), (i & 1) == 1, 0f);
        }
        int e = ctx.seg(FireV2.ZONE_CHIPS, 4);
        for (int i = 0; i < e; i++) {
            float lf = Mth.frac(ft * 0.022f * (1f + 0.4f * hash(inst.seed, i, 5)) + hash(inst.seed, i, 6)), a = hash(inst.seed, i, 7) * Mth.TWO_PI, d = Mth.sqrt(hash(inst.seed, i, 8)) * R;
            sprite(tex(CHIP + (i & 1)), VfxBlend.ALPHA, cx + Mth.cos(a) * d, cy + 0.26f + lf * (1.2f + R * 0.3f), cz + Mth.sin(a) * d, 0.24f * (1f - lf) + 0.06f, ft * 0.2f, argb(Mth.sin(lf * Mth.PI) * fade));
        }
    }

    // ================================================================================================ A  Flame Burst
    private void fxBurst() {
        Vec3 fw = inst.from(ctx);
        float fx = rel(fw.x, ctx.camPos.x), fy = rel(fw.y, ctx.camPos.y), fz = rel(fw.z, ctx.camPos.z);
        float p = inst.power;
        float t = Mth.clamp(ft / inst.duration, 0f, 1f), fade = 1f - VfxAnim.easeInCubic(t);
        float cy = fy + 0.6f;
        float bsz = (1.3f + 2.4f * VfxAnim.easeOutCubic(Mth.clamp(t * 2f, 0f, 1f))) * p;
        ground(tex(RING), VfxBlend.ALPHA, fx, fy + 0.07f, fz, (0.4f + 2.4f * VfxAnim.easeOutCubic(t)) * p, fa * 0.08f, argb(0.95f * fade));
        float e = VfxAnim.easeOutCubic(Mth.clamp(t * 1.5f, 0f, 1f));
        int n = ctx.seg(FireV2.FXBURST_TONGUES, 5);
        for (int i = 0; i < n; i++) {
            float a = Mth.TWO_PI * i / n + hash(inst.seed, i, 1) * 0.4f, reach = (0.9f + 0.9f * hash(inst.seed, i, 2)) * p, up = (0.5f + 0.7f * hash(inst.seed, i, 3)) * p;
            float tipX = Mth.cos(a) * reach * e, tipY = 0.1f + up * Mth.sin(Mth.clamp(t * 1.4f, 0f, 1f) * Mth.PI), tipZ = Mth.sin(a) * reach * e;
            float baseX = Mth.cos(a) * 0.15f * p, baseZ = Mth.sin(a) * 0.15f * p;
            upright(tex(TONGUE + fr(i, 3)), VfxBlend.ALPHA, fx + baseX + (tipX - baseX) * 0.3f, fy + 0.1f + (tipY - 0.1f) * 0.3f, fz + baseZ + (tipZ - baseZ) * 0.3f, 0.75f * p * (1f - t * 0.5f),
                    Math.max(0.3f, tipY - 0.1f + 0.6f) * 1.25f, argb(fade), (i & 1) == 0, 0f);
        }
        sprite(texT(HALO), VfxBlend.ADD, fx, cy, fz, 3.4f * p * (0.5f + VfxAnim.easeOutCubic(t)), 0f, glowArgb(0.6f * fade));
        if (t < 0.45f) rays(RAYS, fx, cy, fz, bsz * 2f, ft * 0.04f, 0.9f);
        flashTicks(fx, cy, fz, bsz * 1.15f, ft * 0.05f, 1f, 1f, 5);
        int k = ctx.seg(FireV2.FXBURST_CHIPS, 4);
        for (int i = 0; i < k; i++) {
            float a = hash(inst.seed, i, 4) * Mth.TWO_PI, v = (1.2f + 2.4f * hash(inst.seed, i, 5)) * p, vy = (1.4f + 2.2f * hash(inst.seed, i, 6)) * p, tt = t * 1.2f;
            sprite(tex(CHIP + (i & 1)), VfxBlend.ALPHA, fx + Mth.cos(a) * v * tt * 0.7f, fy + 0.1f + vy * tt - 2.6f * tt * tt * p, fz + Mth.sin(a) * v * tt * 0.7f, 0.3f * p * (1f - t) + 0.08f, ft * 0.3f + i, argb(fade));
        }
        if (t > 0.3f) {
            int sm = ctx.seg(FireV2.FXBURST_SMOKE, 2);
            float st = (t - 0.3f) / 0.7f;
            for (int i = 0; i < sm; i++) {
                float a = hash(inst.seed, i, 8) * Mth.TWO_PI, dd = (0.2f + 0.8f * hash(inst.seed, i, 9)) * p * VfxAnim.easeOutCubic(t);
                sprite(tex(SMOKE + (i & 1)), VfxBlend.ALPHA, fx + Mth.cos(a) * dd, fy + 0.5f + t * 1.2f * p, fz + Mth.sin(a) * dd, (1.0f + 1.2f * t) * p, 0f, whiteArgb(0.9f * Mth.sin(st * Mth.PI)));
            }
        }
    }

    // ================================================================================================ B  Flame Trail
    private void trail() {
        Vec3 fw = inst.from(ctx), tw = inst.to(ctx);
        float ax = rel(fw.x, ctx.camPos.x), ay = rel(fw.y, ctx.camPos.y), az = rel(fw.z, ctx.camPos.z);
        float dx = rel(tw.x, ctx.camPos.x) - ax, dy = rel(tw.y, ctx.camPos.y) - ay, dz = rel(tw.z, ctx.camPos.z) - az;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.01f) return;
        float t = Mth.clamp(ft / inst.duration, 0f, 1f);
        float head = VfxAnim.slashHead(t), tail = Math.max(0f, VfxAnim.slashTail(t) - 0.1f);
        float gp = (reduced ? VfxAnim.easeOutCubic(Mth.clamp(t * 3f, 0f, 1f)) : VfxAnim.flameGrow(t, ft, inst.seed)) * inst.power;
        float alpha = VfxAnim.fadeInOut(t, 0.05f, 0.3f);
        float hx = ax + dx * head, hy = ay + dy * head, hz = az + dz * head, tx = ax + dx * tail, ty = ay + dy * tail, tz = az + dz * tail;
        float ex = hx - tx, ey = hy - ty, ez = hz - tz, el = Mth.sqrt(ex * ex + ey * ey + ez * ez);
        if (el < 0.1f) return;
        float ux = dx / len, uy = dy / len, uz = dz / len;
        sideOf(ux, uy, uz);
        strip(tex(BSTREAK), VfxBlend.ALPHA, tx, ty, tz, hx + ux * 0.5f * gp, hy + uy * 0.5f * gp, hz + uz * 0.5f * gp, 1.4f * gp, argb(0.9f * alpha), false);
        strip(tex(BSTREAK), VfxBlend.ADD, tx, ty, tz, hx + ux * 0.5f * gp, hy + uy * 0.5f * gp, hz + uz * 0.5f * gp, 0.5f * gp, hotArgb(alpha), false);
        sprite(texT(HALO), VfxBlend.ADD, hx, hy, hz, 3.6f * gp, 0f, glowArgb(0.9f * alpha));
        int puffs = ctx.seg(FireV2.TRAIL_PUFFS, 3);
        for (int i = 0; i < puffs; i++) {
            float s = (i + 0.5f) / puffs;
            sprite(tex(PUFF + (i & 3)), VfxBlend.ALPHA, tx + ex * s, ty + ey * s, tz + ez * s, (0.4f + 0.8f * s) * gp, i + fa * 0.1f, argb(0.8f * alpha * (0.3f + 0.7f * s)));
        }
        sprite(tex(PUFF + 2), VfxBlend.ALPHA, hx, hy, hz, 1.4f * gp, 0f, argb(alpha));
        sprite(texT(HALO), VfxBlend.ADD, hx, hy, hz, 1.0f * gp, 0f, hotArgb(alpha));
        int e = ctx.seg(FireV2.TRAIL_EMBERS, 5);
        for (int i = 0; i < e; i++) {
            float al = hash(inst.seed, i, 1), lf = Mth.frac(ft * 0.05f + hash(inst.seed, i, 2)), sd = (hash(inst.seed, i, 3) - 0.5f) * gp;
            sprite(tex(EMBER), VfxBlend.ADD, tx + ex * al + sdx * sd, ty + ey * al + lf * 1.2f * gp, tz + ez * al + sdz * sd, 0.26f * (1f - lf) + 0.05f, 0f, argb(alpha * (1f - lf)));
        }
    }

    // ================================================================================================ A  Flame Explosion
    private void explosion() {
        Vec3 tw = inst.to(ctx);
        float tx = rel(tw.x, ctx.camPos.x), ty = rel(tw.y, ctx.camPos.y), tz = rel(tw.z, ctx.camPos.z);
        float p = inst.power;
        float t = Mth.clamp(ft / inst.duration, 0f, 1f), fade = 1f - VfxAnim.easeInCubic(t);
        float size = VfxAnim.explosionBurst(t) * 3.2f * p;
        float cx = tx, cy = ty + 0.2f, cz = tz;
        float ke = 0.5f + 0.5f * VfxAnim.easeOutCubic(Mth.clamp(t * 3f, 0f, 1f)), dd = 0.45f * size * ke;
        // the cluster of fireballs (the centre one first): offsets in the camera plane and a little towards the camera
        for (int i = 0; i < FireV2.EXPL_BALLS; i++) {
            float x = i == 0 ? 0f : i == 1 ? 0.45f : i == 2 ? -0.5f : i == 3 ? 0.2f : i == 4 ? -0.25f : 0.1f;
            float y = i == 0 ? 0f : i == 1 ? 0.25f : i == 2 ? 0.15f : i == 3 ? 0.55f : i == 4 ? -0.2f : -0.35f;
            float z = i == 0 ? 0f : i == 1 ? 0.1f : i == 2 ? 0.2f : i == 3 ? -0.1f : i == 4 ? 0.35f : -0.2f;
            float k = i == 0 ? 1f : i == 1 ? 0.66f : i == 2 ? 0.6f : i == 3 ? 0.55f : i == 4 ? 0.5f : 0.46f;
            sprite(tex(BALL + fr(i, 3)), VfxBlend.ALPHA, cx + (ctx.camRight.x * x + ctx.camUp.x * y - cfx * 0.3f * z) * dd, cy + (ctx.camRight.y * x + ctx.camUp.y * y - cfy * 0.3f * z) * dd,
                    cz + (ctx.camRight.z * x + ctx.camUp.z * y - cfz * 0.3f * z) * dd, size * 1.25f * k, 0f, argb(fade));
        }
        ground(tex(RING), VfxBlend.ALPHA, tx, ty - 0.4f * p, tz, size * (0.4f + t * 0.6f) * 1.2f, fa * 0.04f, argb(0.8f * fade));
        int n = ctx.seg(FireV2.EXPL_TONGUES, 4);
        for (int i = 0; i < n; i++) {
            float yaw = hash(inst.seed, i, 1) * Mth.TWO_PI, d = size * 0.55f * (0.7f + 0.4f * hash(inst.seed, i, 2)) * VfxAnim.easeOutCubic(Mth.clamp(t * 2f, 0f, 1f));
            upright(tex(TONGUE + fr(i, 3)), VfxBlend.ALPHA, tx + Mth.cos(yaw) * d, ty - 0.3f + 0.3f * hash(inst.seed, i, 3), tz + Mth.sin(yaw) * d, 1.0f * p * (1f - t * 0.5f), 1.7f * p * (1f - t * 0.5f), argb(fade), (i & 1) == 0, 0f);
        }
        sprite(texT(HALO), VfxBlend.ADD, cx, cy, cz, size * 1.7f, 0f, glowArgb(0.75f * fade));
        if (ia < 9) rays(RAYS, cx, cy, cz, size * 1.7f, 0.3f, 1f - ft / 9f);
        flashTicks(cx, cy, cz, size * 1.2f, 0.2f, Math.max(0f, 1f - ft / 5f), 1f, 5);
        int k = ctx.seg(FireV2.EXPL_CHIPS, 4);
        for (int i = 0; i < k; i++) {
            float yaw = hash(inst.seed, i, 6) * Mth.TWO_PI, up = hash(inst.seed, i, 7), d = size * VfxAnim.easeOutCubic(t) * (0.8f + hash(inst.seed, i, 8));
            sprite(tex(CHIP + (i & 1)), VfxBlend.ALPHA, cx + Mth.cos(yaw) * d, cy + up * d - t * t * 2f, cz + Mth.sin(yaw) * d, 0.3f * p, yaw + ft * 0.2f, argb(fade));
        }
        if (t > 0.35f) {
            int sm = ctx.seg(FireV2.EXPL_SMOKE, 2);
            float st = (t - 0.35f) / 0.65f;
            for (int i = 0; i < sm; i++) {
                sprite(tex(SMOKE + (i & 1)), VfxBlend.ALPHA, cx + (hash(inst.seed, i, 1) - 0.5f) * 2f * p, cy + 0.8f + st * 2.0f * p, cz + (hash(inst.seed, i, 2) - 0.5f) * 2f * p, (1.3f + 1.6f * st) * p, 0f,
                        whiteArgb(0.9f * (1f - st)));
            }
        }
    }

    // ================================================================================================ flashes (the reduced-flashes rules are in FireV2)
    /** A hard flash star that fades with {@code a}, or with reduced flashes a soft halo at half the strength. */
    private void flashA(float x, float y, float z, float size, float roll, float a) {
        if (a <= 0.01f) return;
        if (reduced) sprite(texT(HALO), VfxBlend.ADD, x, y, z, size * 1.2f, 0f, glowArgb(0.5f * a));
        else sprite(tex(FLASH), VfxBlend.ALPHA, x, y, z, size, roll, argb(a));
    }

    /** The flash of the first ticks of an effect (5 ticks unless given): a hard star scaled by {@code a}, or with reduced flashes the slow soft fade of {@link FireV2#softFlash}. */
    private void flashTicks(float x, float y, float z, float size, float roll, float a, float soft) { flashTicks(x, y, z, size, roll, a, soft, 7); }

    private void flashTicks(float x, float y, float z, float size, float roll, float a, float soft, int ticks) {
        float hard = FireV2.hardFlash(ia, ticks, reduced) * a;
        if (hard > 0.01f && size > 0f) sprite(tex(FLASH), VfxBlend.ALPHA, x, y, z, size, roll, argb(hard));
        float sf = FireV2.softFlash(ia, ticks, reduced) * soft;
        if (sf > 0.01f && size > 0f) sprite(texT(HALO), VfxBlend.ADD, x, y, z, Math.max(size, 1f) * 1.2f, 0f, glowArgb(sf));
    }

    /** Hard radial speed lines: never drawn with reduced flashes. */
    private void rays(int which, float x, float y, float z, float size, float roll, float a) {
        if (reduced || a <= 0.01f) return;
        sprite(tex(which), which == RAYS ? VfxBlend.ALPHA : VfxBlend.ADD, x, y, z, size, roll, argb(a));
    }

    // ================================================================================================ helpers (no allocation)
    private ResourceLocation tex(int idx) { return TEX[set][idx]; }

    /** the grey twin whatever the colour (halos take the colour of the effect) */
    private ResourceLocation texT(int idx) { return TEX[1][idx]; }

    private int fr(int offset, int n) { return FireV2.frame(ia, offset, n, reduced); }

    private int argb(float alpha) { return (Math.round(Mth.clamp(alpha, 0f, 1f) * 255f) << 24) | rgb; }

    /** smoke keeps its own dark brown-grey whatever the colour of the spell */
    private static int whiteArgb(float alpha) { return (Math.round(Mth.clamp(alpha, 0f, 1f) * 255f) << 24) | 0xFFFFFF; }

    private int glowArgb(float alpha) { return (Math.round(Mth.clamp(alpha, 0f, 1f) * 255f) << 24) | glowRgb; }

    private int hotArgb(float alpha) { return (Math.round(Mth.clamp(alpha, 0f, 1f) * 255f) << 24) | hotRgb; }

    private static float rel(double world, double cam) { return (float) (world - cam); }

    /** The horizontal unit side vector of a direction (d x up) into sdx / sdz (y is 0). */
    private void sideOf(float dx, float dy, float dz) {
        float x = -dz, z = dx, l = Mth.sqrt(x * x + z * z);
        if (l < 1e-3f) { sdx = 1f; sdz = 0f; return; }
        sdx = x / l;
        sdz = z / l;
    }

    /** A camera-facing square (flames, balls, sparks, smoke, halos): {@code size} blocks across, rolled by {@code roll}. */
    private void sprite(ResourceLocation tex, VfxBlend blend, float x, float y, float z, float size, float roll, int argb) {
        V.set(x, y, z);
        buf.billboard(ctx, tex, blend, V, size, roll, argb);
    }

    /** A flat quad of half-size hw x hh on the plane (ax, ay) around c; the texture's top is +ay. */
    private void plane(ResourceLocation tex, VfxBlend blend, float cx, float cy, float cz, float axx, float axy, float axz, float ayx, float ayy, float ayz, float hw, float hh, int argb, boolean flip) {
        P0.set(cx - axx * hw - ayx * hh, cy - axy * hw - ayy * hh, cz - axz * hw - ayz * hh);
        P1.set(cx + axx * hw - ayx * hh, cy + axy * hw - ayy * hh, cz + axz * hw - ayz * hh);
        P2.set(cx + axx * hw + ayx * hh, cy + axy * hw + ayy * hh, cz + axz * hw + ayz * hh);
        P3.set(cx - axx * hw + ayx * hh, cy - axy * hw + ayy * hh, cz - axz * hw + ayz * hh);
        buf.quad(tex, blend, P0, P1, P2, P3, flip ? 1f : 0f, 0f, flip ? 0f : 1f, 1f, argb, argb);
    }

    /** A flat decal lying on the ground (a square of half-size rad, turned by spin). */
    private void ground(ResourceLocation tex, VfxBlend blend, float cx, float cy, float cz, float rad, float spin, int argb) {
        float c = Mth.cos(spin), s = Mth.sin(spin);
        plane(tex, blend, cx, cy, cz, c, 0f, s, -s, 0f, c, rad, rad, argb, false);
    }

    /** The ground plane of VfxPose.ground(..).spin(angle): right = (sin, 0, cos), up = (cos, 0, -sin); the old sigils were laid out on it, so the kept ones use it unchanged. */
    private void groundPose(ResourceLocation tex, VfxBlend blend, float cx, float cy, float cz, float half, float spin, int argb) {
        float c = Mth.cos(spin), s = Mth.sin(spin);
        plane(tex, blend, cx, cy, cz, s, 0f, c, c, 0f, -s, half, half, argb, false);
    }

    // the plane of VfxPose.facing(origin, d) into pr* / pu*
    private float prx, pry, prz, pux, puy, puz;

    /** Basis of VfxPose.facing(_, d): right = ref x d, up = d x right (ref = up, or x when d is nearly vertical), both normalised. */
    private void facing(float dx, float dy, float dz) {
        float rx, ry, rz;
        if (Math.abs(dy) > 0.95f) { rx = 1f; ry = 0f; rz = 0f; } else { rx = 0f; ry = 1f; rz = 0f; }
        float cx = ry * dz - rz * dy, cy = rz * dx - rx * dz, cz = rx * dy - ry * dx, cl = Mth.sqrt(cx * cx + cy * cy + cz * cz);
        if (cl > 1e-6f) { cx /= cl; cy /= cl; cz /= cl; }
        prx = cx; pry = cy; prz = cz;
        float ux = dy * prz - dz * pry, uy = dz * prx - dx * prz, uz = dx * pry - dy * prx, ul = Mth.sqrt(ux * ux + uy * uy + uz * uz);
        if (ul > 1e-6f) { ux /= ul; uy /= ul; uz /= ul; }
        pux = ux; puy = uy; puz = uz;
    }

    /** VfxPose.facing(..).spin(angle) as a quad of half-size {@code half} (call {@link #facing} first). */
    private void spinPlane(ResourceLocation tex, float cx, float cy, float cz, float spin, float half, VfxBlend blend, int argb) {
        float c = Mth.cos(spin), s = Mth.sin(spin);
        plane(tex, blend, cx, cy, cz, prx * c + pux * s, pry * c + puy * s, prz * c + puz * s, pux * c - prx * s, puy * c - pry * s, puz * c - prz * s, half, half, argb, false);
    }

    /** A camera-facing strip from a to b (camera-relative points); the texture's U runs from a to b, its top edge is the +cross side. */
    private void strip(ResourceLocation tex, VfxBlend blend, float ax, float ay, float az, float bx, float by, float bz, float width, int argb, boolean flip) {
        float dx = bx - ax, dy = by - ay, dz = bz - az;
        if (dx * dx + dy * dy + dz * dz < 1e-6f) return;
        float mx = (ax + bx) * 0.5f, my = (ay + by) * 0.5f, mz = (az + bz) * 0.5f;
        // top = d x mid (the camera is at the origin)
        float tx = dy * mz - dz * my, ty = dz * mx - dx * mz, tz = dx * my - dy * mx;
        float l = Mth.sqrt(tx * tx + ty * ty + tz * tz);
        if (l < 1e-6f) return;
        float k = width * 0.5f / l;
        tx *= k; ty *= k; tz *= k;
        P0.set(ax - tx, ay - ty, az - tz);
        P1.set(bx - tx, by - ty, bz - tz);
        P2.set(bx + tx, by + ty, bz + tz);
        P3.set(ax + tx, ay + ty, az + tz);
        buf.quad(tex, blend, P0, P1, P2, P3, flip ? 1f : 0f, 0f, flip ? 0f : 1f, 1f, argb, argb);
    }

    /** A flame standing on {@code base}, facing the camera about the vertical axis; the texture's bottom is on the ground. */
    private void upright(ResourceLocation tex, VfxBlend blend, float bx, float by, float bz, float w, float h, int argb, boolean flip, float lean) {
        float hx = hrx * w * 0.5f, hz = hrz * w * 0.5f, tx = bx + hrx * lean, tz = bz + hrz * lean, ty = by + h;
        P0.set(bx - hx, by, bz - hz);
        P1.set(bx + hx, by, bz + hz);
        P2.set(tx + hx, ty, tz + hz);
        P3.set(tx - hx, ty, tz - hz);
        buf.quad(tex, blend, P0, P1, P2, P3, flip ? 1f : 0f, 0f, flip ? 0f : 1f, 1f, argb, argb);
    }

    /** A card whose texture runs along the world direction d as the camera sees it (the lion's head faces the way it leaps; it is mirrored, never turned upside down, when it points left). */
    private void dirSprite(ResourceLocation tex, float x, float y, float z, float dx, float dy, float dz, float w, float h, int argb) {
        Vector3f r = ctx.camRight, u = ctx.camUp;
        float pr = dx * r.x + dy * r.y + dz * r.z, pu = dx * u.x + dy * u.y + dz * u.z, l = Mth.sqrt(pr * pr + pu * pu);
        if (l < 1e-5f) { pr = 1f; pu = 0f; } else { pr /= l; pu /= l; }
        boolean flip = false;
        if (pr < 0f) { pr = -pr; pu = -pu; flip = true; }
        // ax = pr * R + pu * U, ay = -pu * R + pr * U
        plane(tex, VfxBlend.ALPHA, x, y, z, r.x * pr + u.x * pu, r.y * pr + u.y * pu, r.z * pr + u.z * pu, -r.x * pu + u.x * pr, -r.y * pu + u.y * pr, -r.z * pu + u.z * pr, w * 0.5f, h * 0.5f, argb, flip);
    }
}
