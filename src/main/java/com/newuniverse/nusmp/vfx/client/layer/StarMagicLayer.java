package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.StarV2;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.hash;

/**
 * Star Magic, version 2 (docs/vfx_remake/star_ingame.md): the seven effect shapes of the Star nodes, a mix of A "anime cel" (flat gold-white stars, hard blue-white lines, the pane, the whip: alpha-blended baked
 * textures, stepped two-frame timing) and B "volumetric glow" (soft additive bloom: Conjunction, the dome, the beam of the lance). The placed stars and the pane themselves are props
 * ({@code client.prop.StarPropPainter}); this layer draws what happens between them. Palette: a PLACEHOLDER (gold-white and starlight blue), no canon still was available.
 *
 * <ul>
 *   <li>A STAR_NODE: a comet with a streak from the caster's hand to the place, a four-point pop when it arrives.</li>
 *   <li>A STAR_LINK: one constellation line that draws itself from star to star, with a twinkle at both ends.</li>
 *   <li>B STAR_CONJ: Conjunction: a lens flare and a ring where the mage leaves, a flare, ring, dust and speed lines where they arrive, a thin line between, the star that was used bursting above.</li>
 *   <li>A STAR_PANE: a hit ring and crack on the Quartile Scutum pane, or its shards when it breaks.</li>
 *   <li>A + B STAR_LANCE: Quartile Hasta: a spear of light with a pointed A head and a B laser tail, a flare at the head, a long afterglow.</li>
 *   <li>A STAR_WHIP: one leg of the whip: a tapered cel ribbon that lashes out, leg by leg.</li>
 *   <li>B STAR_DOME: Black Neverland: a ground sigil, a ring, twinkling stars on the rim, swirling dust.</li>
 * </ul>
 *
 * <p>With {@code vfx.element.star = V1} every shape is drawn as the old starlight beam ({@code ElementIdentityLayer}'s STAR_MAGIC look). Reduced flashes ({@code vfx.element.starReducedFlashes}): no hard flash
 * stars, no speed lines, no lens flare (a soft halo instead), no twinkle. Cost: at most {@link StarV2#worstQuads} quads per activation, counts shrink with {@code ctx.seg}; nothing is allocated per frame
 * (V2 path), nothing is sent or computed on a server.
 */
public class StarMagicLayer extends AbstractVfxLayer {
    private static final int STAR4 = 0, SPARK = 1, SIGIL = 2, LINE = 3, PANE = 4, RING = 5, SHARD = 6, BURST = 7, WHIP = 8, BEAM = 9, CRACK = 10, RAYS = 11,
            GLOW = 12, GLOW_BLUE = 13, FLARE = 14, BSTAR = 15, BBEAM = 16, BRING = 17, BRAYS = 18, DUST = 19, TRAIL = 22, BSIGIL = 23;
    private static final ResourceLocation[] TEX = new ResourceLocation[StarV2.TEXTURE_NAMES.length];

    static {
        for (int n = 0; n < StarV2.TEXTURE_NAMES.length; n++) TEX[n] = VfxTextures.byName(StarV2.fileName(StarV2.TEXTURE_NAMES[n]));
    }

    private static final Vector3f P0 = new Vector3f(), P1 = new Vector3f(), P2 = new Vector3f(), P3 = new Vector3f(), V = new Vector3f();
    /** the gold of the bloom halos and the pale blue of the soft ribbons (ARGB without alpha) */
    private static final int GOLD = 0xFFE08A, PALE = 0xFFF6D0, BLUE = 0xBFDFFF;

    private VfxRenderContext ctx;
    private VfxVertexBuffer buf;
    private VfxInstance inst;
    private boolean reduced;
    private int ia;
    private float ft, size;
    private float camX, camY, camZ;

    @Override public Set<VfxShape> shapes() { return EnumSet.copyOf(StarV2.SHAPES); }

    @Override public int defaultDuration(VfxShape s) {
        return switch (s) {
            case STAR_NODE -> 16;
            case STAR_LINK -> 24;
            case STAR_CONJ -> 20;
            case STAR_PANE -> 14;
            case STAR_LANCE -> 14;
            case STAR_WHIP -> 16;
            case STAR_DOME -> 100;
            default -> 24;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFFFF0A0; }

    @Override public int vertexBudget(VfxShape s) { return StarV2.v2Look() ? StarV2.vertexBudget(s) : 160; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        if (!StarV2.v2Look()) {
            oldBeam(inst, ctx, buf);
            return;
        }
        this.ctx = ctx;
        this.buf = buf;
        this.inst = inst;
        reduced = StarV2.reducedFlashes();
        ia = inst.age();
        ft = inst.ageTicks(ctx.partialTick);
        size = Math.max(0.4f, inst.power);
        camX = (float) ctx.camPos.x;
        camY = (float) ctx.camPos.y;
        camZ = (float) ctx.camPos.z;
        try {
            switch (inst.shape) {
                case STAR_NODE -> node();
                case STAR_LINK -> link();
                case STAR_CONJ -> conjunction();
                case STAR_PANE -> pane();
                case STAR_LANCE -> lance();
                case STAR_WHIP -> whip();
                case STAR_DOME -> dome();
                default -> { }
            }
        } finally {
            this.ctx = null;
            this.buf = null;
            this.inst = null;
        }
    }

    // ================================================================================================ V1: the old starlight beam (ElementIdentityLayer's STAR_MAGIC), kept for the option
    private void oldBeam(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = Mth.clamp(1f - age / Math.max(1f, inst.duration), 0f, 1f);
        float sz = Math.max(0.35f, inst.power);
        Vector3f from = ctx.rel(inst.from(ctx));
        Vector3f to = inst.shape == VfxShape.STAR_PANE || inst.shape == VfxShape.STAR_DOME ? new Vector3f(from).add(0f, 1f, 0f) : ctx.rel(inst.to(ctx));
        int tint = VfxVertexBuffer.withAlpha(inst.color, fade);
        Vector3f tip = new Vector3f(from).lerp(to, Mth.clamp(age / Math.max(1f, inst.duration), 0f, 1f));
        buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, tip, from, 0.16f * sz, 0.01f, 2, age * 0.08f, tint, tint);
        int count = ctx.seg(12, 5);
        Vector3f side = new Vector3f(ctx.camRight), up = new Vector3f(ctx.camUp);
        for (int n = 0; n < count; n++) {
            float t = (n + 0.5f) / count;
            Vector3f centre = new Vector3f(from).lerp(to, t);
            float angle = age * 0.17f + n * 2.4f;
            centre.add(new Vector3f(side).mul(Mth.cos(angle) * 0.55f * sz));
            centre.add(new Vector3f(up).mul(Mth.sin(angle) * 0.35f * sz));
            float glint = 0.45f + 0.55f * (0.5f + 0.5f * Mth.sin(age * 0.35f + n));
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, centre, 0.45f * sz, angle, VfxVertexBuffer.withAlpha(0xFFFFFFFF, fade * glint));
            buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, centre, 0.7f * sz, -angle, VfxVertexBuffer.withAlpha(inst.color, fade * glint));
        }
    }

    // ================================================================================================ STAR_NODE
    private void node() {
        float fx = rx(inst.from(ctx).x, camX), fy = rx(inst.from(ctx).y, camY), fz = rx(inst.from(ctx).z, camZ);
        float tx = rx(inst.to(ctx).x, camX), ty = rx(inst.to(ctx).y, camY), tz = rx(inst.to(ctx).z, camZ);
        float fly = Mth.clamp(ft / 8f, 0f, 1f), e = 1f - (1f - fly) * (1f - fly);
        float hx = fx + (tx - fx) * e, hy = fy + (ty - fy) * e, hz = fz + (tz - fz) * e;
        float out = Mth.clamp((inst.duration - ft) / 6f, 0f, 1f);
        // the comet: a soft streak behind the head (the head has not arrived yet) and a warm glow on it
        float tail = Math.max(0f, e - 0.45f);
        if (fly < 1f) {
            strip(TEX[TRAIL], VfxBlend.ADD, fx + (tx - fx) * tail, fy + (ty - fy) * tail, fz + (tz - fz) * tail, hx, hy, hz, 0.55f * size, whiteArgb(0.9f * out), false);
            sprite(TEX[GLOW], VfxBlend.ADD, hx, hy, hz, 0.8f * size, 0f, goldArgb(0.9f * out));
        }
        int sparks = ctx.seg(StarV2.NODE_COMET_SPARKS, 2);
        for (int i = 0; i < sparks; i++) {
            float k = (i + 1f) / (sparks + 1f), t = Math.min(e, k);
            float tw = StarV2.twinkle(ft + i * 3f, 0.9f, 0.6f, reduced);
            sprite(TEX[SPARK], VfxBlend.ALPHA, fx + (tx - fx) * t + (hash(inst.seed, i, 1) - 0.5f) * 0.6f, fy + (ty - fy) * t + (hash(inst.seed, i, 2) - 0.5f) * 0.6f,
                    fz + (tz - fz) * t + (hash(inst.seed, i, 3) - 0.5f) * 0.6f, 0.28f * size * tw, StarV2.spin(ft * 0.2f + i, reduced), whiteArgb(0.9f * out * tw));
        }
        // the pop where it arrives: a hard four-point star for a few ticks (a soft glow with reduced flashes)
        int landed = ia - 8;
        float hard = StarV2.hardFlash(landed, 5, reduced), soft = StarV2.softFlash(landed, 6, reduced);
        if (hard > 0f) sprite(TEX[BURST], VfxBlend.ALPHA, tx, ty, tz, 1.5f * size * (1f - landed * 0.1f), 0f, whiteArgb(hard * out));
        if (soft > 0f) sprite(TEX[GLOW], VfxBlend.ADD, tx, ty, tz, 1.4f * size, 0f, goldArgb(soft));
    }

    // ================================================================================================ STAR_LINK
    private void link() {
        float fx = rx(inst.from(ctx).x, camX), fy = rx(inst.from(ctx).y, camY), fz = rx(inst.from(ctx).z, camZ);
        float tx = rx(inst.to(ctx).x, camX), ty = rx(inst.to(ctx).y, camY), tz = rx(inst.to(ctx).z, camZ);
        float g = Mth.clamp(ft / 5f, 0f, 1f), grow = 1f - (1f - g) * (1f - g);
        float fade = Mth.clamp((inst.duration - ft) / 6f, 0f, 1f);
        float ex = fx + (tx - fx) * grow, ey = fy + (ty - fy) * grow, ez = fz + (tz - fz) * grow;
        // the soft ribbon first (it sits under the hard line), then the cel line
        strip(TEX[BBEAM], VfxBlend.ADD, fx, fy, fz, ex, ey, ez, 0.5f * size, whiteArgb(0.4f * fade), false);
        strip(TEX[LINE], VfxBlend.ALPHA, fx, fy, fz, ex, ey, ez, 0.13f * size, whiteArgb(fade), false);
        float tw = StarV2.twinkle(ft, 0.7f, 0.4f, reduced);
        sprite(TEX[GLOW], VfxBlend.ADD, fx, fy, fz, 0.7f * size, 0f, goldArgb(0.55f * fade * tw));
        sprite(TEX[GLOW], VfxBlend.ADD, ex, ey, ez, 0.7f * size, 0f, goldArgb(0.55f * fade * tw));
        int sp = ctx.seg(StarV2.LINK_SPARKS, 1);
        for (int i = 0; i < sp; i++) {
            float x = i == 0 ? fx : ex, y = i == 0 ? fy : ey, z = i == 0 ? fz : ez;
            sprite(TEX[SPARK], VfxBlend.ALPHA, x, y, z, 0.4f * size * tw, StarV2.spin(i * 0.8f + ft * 0.05f, reduced), whiteArgb(fade * tw));
        }
    }

    // ================================================================================================ STAR_CONJ (B)
    private void conjunction() {
        float fx = rx(inst.from(ctx).x, camX), fy = rx(inst.from(ctx).y, camY), fz = rx(inst.from(ctx).z, camZ);
        float tx = rx(inst.to(ctx).x, camX), ty = rx(inst.to(ctx).y, camY), tz = rx(inst.to(ctx).z, camZ);
        float dep = Mth.clamp(ft / 10f, 0f, 1f), arr = Mth.clamp((ft - 2f) / 12f, 0f, 1f);
        float out = Mth.clamp((inst.duration - ft) / 6f, 0f, 1f);
        // where the mage left: a flare that shrinks, a ring on the ground that spreads
        if (dep < 1f) {
            flare(fx, fy, fz, 3.4f * size * (1f - dep * 0.6f), (1f - dep));
            ground(TEX[BRING], VfxBlend.ADD, fx, fy - 0.95f, fz, 0.8f + 2.6f * dep, ft * 0.05f, goldArgb(0.9f * (1f - dep)));
        }
        // the line between the two points: thin and soft, it fades fast
        float lineA = Mth.clamp(1f - ft / 9f, 0f, 1f);
        if (lineA > 0f) strip(TEX[BBEAM], VfxBlend.ADD, fx, fy, fz, tx, ty, tz, 0.35f * size, whiteArgb(0.8f * lineA), false);
        // where the mage arrives: a flare that blooms, a ring, dust, speed lines
        if (ft >= 2f) {
            flare(tx, ty, tz, 3.0f * size * (0.5f + arr * 0.5f), (1f - arr) * out);
            ground(TEX[BRING], VfxBlend.ADD, tx, ty - 0.95f, tz, 0.6f + 3.0f * arr, -ft * 0.04f, goldArgb(0.9f * (1f - arr)));
            int dust = ctx.seg(StarV2.CONJ_DUST, 1);
            for (int i = 0; i < dust; i++) {
                float a = hash(inst.seed, i, 1) * Mth.TWO_PI + ft * 0.05f;
                sprite(TEX[DUST + (i % 3)], VfxBlend.ADD, tx + Mth.cos(a) * 0.9f * arr, ty - 0.4f + arr * 1.4f, tz + Mth.sin(a) * 0.9f * arr, (1.2f + 1.0f * arr) * size, a, goldArgb(0.45f * (1f - arr) * out));
            }
            if (!reduced) {
                float hard = StarV2.hardFlash(ia - 2, 5, reduced) * out;
                if (hard > 0f) sprite(TEX[RAYS], VfxBlend.ALPHA, tx, ty, tz, 3.2f * size, 0.2f, whiteArgb(hard));
                if (hard > 0f) sprite(TEX[BRAYS], VfxBlend.ADD, tx, ty, tz, 3.6f * size, ft * 0.03f, goldArgb(0.5f * hard));
            }
        }
        // the star that was used bursts above the arrival (the star hung about three blocks over the ground)
        if (ft >= 2f && ft < 12f) {
            float p = (ft - 2f) / 10f;
            sprite(TEX[BSTAR], VfxBlend.ADD, tx, ty + 2.5f, tz, 1.6f * size * (1f - p * 0.5f), 0f, whiteArgb(1f - p));
            float hard = StarV2.hardFlash(ia - 2, 4, reduced);
            if (hard > 0f) sprite(TEX[BURST], VfxBlend.ALPHA, tx, ty + 2.5f, tz, 1.8f * size, 0f, whiteArgb(hard * (1f - p)));
        }
    }

    /** The lens flare of Conjunction; with reduced flashes a plain soft glow (the flare's streaks flash). */
    private void flare(float x, float y, float z, float s, float a) {
        if (a <= 0.01f) return;
        if (reduced) sprite(TEX[GLOW], VfxBlend.ADD, x, y, z, s * 0.6f, 0f, goldArgb(0.5f * a));
        else sprite(TEX[FLARE], VfxBlend.ADD, x, y, z, s, 0f, whiteArgb(a));
    }

    // ================================================================================================ STAR_PANE
    private void pane() {
        float fx = rx(inst.from(ctx).x, camX), fy = rx(inst.from(ctx).y, camY), fz = rx(inst.from(ctx).z, camZ);
        float cx = rx(inst.to(ctx).x, camX), cy = rx(inst.to(ctx).y, camY), cz = rx(inst.to(ctx).z, camZ);
        float t = Mth.clamp(ft / Math.max(1f, inst.duration), 0f, 1f), out = 1f - t;
        if (inst.seed == 0L) {                                                  // a hit: a ring that spreads, a crack star, a blue glow
            sprite(TEX[RING], VfxBlend.ALPHA, fx, fy, fz, (0.6f + 1.5f * t) * size, 0f, whiteArgb(out));
            float hard = StarV2.hardFlash(ia, 4, reduced);
            if (hard > 0f) sprite(TEX[CRACK], VfxBlend.ALPHA, fx, fy, fz, 1.0f * size, 0.3f, whiteArgb(hard));
            sprite(TEX[GLOW_BLUE], VfxBlend.ADD, fx, fy, fz, 1.6f * size, 0f, whiteArgb(0.8f * out));
            return;
        }
        sprite(TEX[RING], VfxBlend.ALPHA, cx, cy, cz, (1.5f + 4.0f * t) * size, 0f, whiteArgb(out));        // it breaks: shards fly out
        float hard = StarV2.hardFlash(ia, 4, reduced);
        if (hard > 0f) sprite(TEX[BURST], VfxBlend.ALPHA, cx, cy, cz, 2.4f * size, 0f, whiteArgb(hard));
        sprite(TEX[GLOW_BLUE], VfxBlend.ADD, cx, cy, cz, 4.0f * size, 0f, whiteArgb(0.7f * out));
        int n = ctx.seg(StarV2.PANE_SHARDS, 3);
        for (int i = 0; i < n; i++) {
            float yaw = hash(inst.seed + 7, i, 1) * Mth.TWO_PI, up = (hash(inst.seed + 7, i, 2) - 0.3f), d = (1.2f + 2.4f * hash(inst.seed + 7, i, 3)) * (1f - out * out);
            sprite(TEX[SHARD], VfxBlend.ALPHA, cx + Mth.cos(yaw) * d, cy + up * d - t * t * 1.5f, cz + Mth.sin(yaw) * d, 0.4f * size, yaw + ft * 0.3f, whiteArgb(out));
        }
    }

    // ================================================================================================ STAR_LANCE (A spear head, B laser tail)
    private void lance() {
        float ax = rx(inst.from(ctx).x, camX), ay = rx(inst.from(ctx).y, camY), az = rx(inst.from(ctx).z, camZ);
        float bx = rx(inst.to(ctx).x, camX), by = rx(inst.to(ctx).y, camY), bz = rx(inst.to(ctx).z, camZ);
        float fly = Mth.clamp(ft / 6f, 0f, 1f), eh = 1f - (1f - fly) * (1f - fly);
        float tl = Mth.clamp((ft - 2f) / 6f, 0f, 1f);
        float hx = ax + (bx - ax) * eh, hy = ay + (by - ay) * eh, hz = az + (bz - az) * eh;
        float sx = ax + (bx - ax) * tl * tl, sy = ay + (by - ay) * tl * tl, sz = az + (bz - az) * tl * tl;
        float out = Mth.clamp((inst.duration - ft) / 6f, 0f, 1f);
        float w = Math.max(0.5f, size);
        if (fly < 1f || ft < 8f) {
            strip(TEX[BBEAM], VfxBlend.ADD, sx, sy, sz, hx, hy, hz, 0.7f * w, whiteArgb(0.85f * out), false);             // the laser tail
            strip(TEX[BEAM], VfxBlend.ALPHA, sx, sy, sz, hx, hy, hz, 0.3f * w, whiteArgb(out), false);                      // the spear: pointed head on the right
            strip(TEX[TRAIL], VfxBlend.ADD, ax + (hx - ax) * 0.6f, ay + (hy - ay) * 0.6f, az + (hz - az) * 0.6f, hx, hy, hz, 1.0f * w, whiteArgb(0.6f * out), false);
            if (reduced) sprite(TEX[GLOW], VfxBlend.ADD, hx, hy, hz, 1.4f * w, 0f, goldArgb(0.6f * out));
            else sprite(TEX[FLARE], VfxBlend.ADD, hx, hy, hz, 1.7f * w, 0f, whiteArgb(out));
        }
        float hard = StarV2.hardFlash(ia, 4, reduced);
        if (hard > 0f) sprite(TEX[BURST], VfxBlend.ALPHA, ax, ay, az, 1.8f * w, 0f, whiteArgb(hard));
        else if (StarV2.softFlash(ia, 5, reduced) > 0f) sprite(TEX[GLOW], VfxBlend.ADD, ax, ay, az, 1.6f * w, 0f, goldArgb(StarV2.softFlash(ia, 5, reduced)));
        int sp = ctx.seg(StarV2.LANCE_SPARKS, 1);
        for (int i = 0; i < sp; i++) {
            float k = Mth.clamp((i + 1f) / (sp + 1f), 0f, eh);
            float tw = StarV2.twinkle(ft + i * 2f, 0.8f, 0.6f, reduced);
            sprite(TEX[SPARK], VfxBlend.ALPHA, ax + (bx - ax) * k + (hash(inst.seed, i, 1) - 0.5f) * 0.5f, ay + (by - ay) * k + (hash(inst.seed, i, 2) - 0.5f) * 0.5f,
                    az + (bz - az) * k + (hash(inst.seed, i, 3) - 0.5f) * 0.5f, 0.3f * w * tw, StarV2.spin(ft * 0.3f + i, reduced), whiteArgb(out * tw));
        }
        if (ft > 5f) strip(TEX[BBEAM], VfxBlend.ADD, ax, ay, az, bx, by, bz, 0.35f * w, whiteArgb(0.55f * Mth.clamp(1f - (ft - 5f) / 9f, 0f, 1f)), false);      // the afterglow of the whole line
    }

    // ================================================================================================ STAR_WHIP (A)
    private void whip() {
        float ax = rx(inst.from(ctx).x, camX), ay = rx(inst.from(ctx).y, camY), az = rx(inst.from(ctx).z, camZ);
        float bx = rx(inst.to(ctx).x, camX), by = rx(inst.to(ctx).y, camY), bz = rx(inst.to(ctx).z, camZ);
        int leg = (int) Mth.clamp(inst.seed, 0L, 7L);
        float lash = Mth.clamp((ft - leg * 2f) / 4f, 0f, 1f);                  // the whip runs out leg by leg
        if (lash <= 0f) return;
        float out = Mth.clamp((inst.duration - ft) / 5f, 0f, 1f);
        float ex = ax + (bx - ax) * lash, ey = ay + (by - ay) * lash, ez = az + (bz - az) * lash;
        float len = Mth.sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay) + (bz - az) * (bz - az));
        float bulge = 0.18f * len * Mth.sin(ft * 0.45f + leg) * (1f - lash * 0.5f);
        float wide = 0.3f * (1f - 0.12f * leg);
        int pieces = Math.max(1, ctx.seg(StarV2.WHIP_PIECES, 2));
        float px = ax, py = ay, pz = az;
        for (int i = 1; i <= pieces; i++) {
            float k = i / (float) pieces, wob = Mth.sin(k * Mth.PI) * bulge;
            float qx = ax + (ex - ax) * k + ctx.camUp.x * wob, qy = ay + (ey - ay) * k + ctx.camUp.y * wob, qz = az + (ez - az) * k + ctx.camUp.z * wob;
            stripUV(TEX[WHIP], VfxBlend.ALPHA, px, py, pz, qx, qy, qz, wide * (1f - 0.8f * (i - 1f) / pieces), wide * (1f - 0.8f * k), (i - 1f) / pieces, k, whiteArgb(out));
            px = qx; py = qy; pz = qz;
        }
        strip(TEX[BBEAM], VfxBlend.ADD, ax, ay, az, ex, ey, ez, 0.55f * wide * 2f, whiteArgb(0.4f * out), false);
        float tw = StarV2.twinkle(ft, 0.9f, 0.5f, reduced);
        sprite(TEX[SPARK], VfxBlend.ALPHA, ex, ey, ez, 0.5f * tw, StarV2.spin(ft * 0.3f, reduced), whiteArgb(out * tw));
    }

    // ================================================================================================ STAR_DOME (B)
    private void dome() {
        float cx = rx(inst.from(ctx).x, camX), cy = rx(inst.from(ctx).y, camY), cz = rx(inst.from(ctx).z, camZ);
        float r = Math.max(2f, inst.power);
        float k = Mth.clamp(ft / 8f, 0f, 1f), out = Mth.clamp((inst.duration - ft) / 12f, 0f, 1f), a = Math.min(k, out);
        ground(TEX[BSIGIL], VfxBlend.ADD, cx, cy + 0.06f, cz, r * (0.4f + 0.6f * k), StarV2.spin(ft * 0.012f, reduced), goldArgb(0.75f * a));
        ground(TEX[RING], VfxBlend.ALPHA, cx, cy + 0.1f, cz, r * k * 1.02f, StarV2.spin(-ft * 0.01f, reduced), whiteArgb(0.8f * a));
        int stars = ctx.seg(StarV2.DOME_STARS, 3);
        for (int i = 0; i < stars; i++) {
            float ang = i * Mth.TWO_PI / stars + (reduced ? 0f : ft * 0.015f), h = 0.6f + 1.8f * hash(inst.seed, i, 1);
            float tw = StarV2.twinkle(ft + i * 5f, 0.35f, 0.7f, reduced);
            sprite(TEX[STAR4], VfxBlend.ALPHA, cx + Mth.cos(ang) * r * 0.97f, cy + h, cz + Mth.sin(ang) * r * 0.97f, 0.7f * tw, 0f, whiteArgb(a * tw));
        }
        int dust = ctx.seg(StarV2.DOME_DUST, 1);
        for (int i = 0; i < dust; i++) {
            float ang = hash(inst.seed, i, 2) * Mth.TWO_PI + (reduced ? 0f : ft * 0.02f * (i % 2 == 0 ? 1f : -1f));
            sprite(TEX[DUST + (i % 3)], VfxBlend.ADD, cx + Mth.cos(ang) * r * 0.55f, cy + 0.9f + i * 0.5f, cz + Mth.sin(ang) * r * 0.55f, r * 0.9f, ang, goldArgb(0.18f * a));
        }
        if (!reduced) sprite(TEX[BRAYS], VfxBlend.ADD, cx, cy + 1.2f, cz, r * 1.6f, ft * 0.004f, goldArgb(0.16f * a));
    }

    // ================================================================================================ helpers (no allocation)
    private static float rx(double world, float cam) { return (float) (world - cam); }

    private static int whiteArgb(float alpha) { return (Math.round(Mth.clamp(alpha, 0f, 1f) * 255f) << 24) | 0xFFFFFF; }

    private static int goldArgb(float alpha) { return (Math.round(Mth.clamp(alpha, 0f, 1f) * 255f) << 24) | GOLD; }

    /** A camera-facing square: {@code size} blocks across, rolled by {@code roll}. */
    private void sprite(ResourceLocation tex, VfxBlend blend, float x, float y, float z, float size, float roll, int argb) {
        V.set(x, y, z);
        buf.billboard(ctx, tex, blend, V, size, roll, argb);
    }

    /** A flat decal on the horizontal plane (a square of half-size rad, turned by spin). */
    private void ground(ResourceLocation tex, VfxBlend blend, float cx, float cy, float cz, float rad, float spin, int argb) {
        float c = Mth.cos(spin) * rad, s = Mth.sin(spin) * rad;
        P0.set(cx - c + s, cy, cz - s - c);
        P1.set(cx + c + s, cy, cz + s - c);
        P2.set(cx + c - s, cy, cz + s + c);
        P3.set(cx - c - s, cy, cz - s + c);
        buf.quad(tex, blend, P0, P1, P2, P3, 0f, 0f, 1f, 1f, argb, argb);
    }

    /** A camera-facing strip from a to b (camera-relative points); the texture's U runs from a to b. */
    private void strip(ResourceLocation tex, VfxBlend blend, float ax, float ay, float az, float bx, float by, float bz, float width, int argb, boolean flip) {
        stripUV(tex, blend, ax, ay, az, bx, by, bz, width, width, flip ? 1f : 0f, flip ? 0f : 1f, argb);
    }

    /** The same with a half-width at each end and a U range (a piece of a longer taper). */
    private void stripUV(ResourceLocation tex, VfxBlend blend, float ax, float ay, float az, float bx, float by, float bz, float w0, float w1, float u0, float u1, int argb) {
        float dx = bx - ax, dy = by - ay, dz = bz - az;
        if (dx * dx + dy * dy + dz * dz < 1e-6f) return;
        float mx = (ax + bx) * 0.5f, my = (ay + by) * 0.5f, mz = (az + bz) * 0.5f;
        float tx = dy * mz - dz * my, ty = dz * mx - dx * mz, tz = dx * my - dy * mx;          // top = d x mid (the camera is at the origin)
        float l = Mth.sqrt(tx * tx + ty * ty + tz * tz);
        if (l < 1e-6f) return;
        float k0 = w0 * 0.5f / l, k1 = w1 * 0.5f / l;
        P0.set(ax - tx * k0, ay - ty * k0, az - tz * k0);
        P1.set(bx - tx * k1, by - ty * k1, bz - tz * k1);
        P2.set(bx + tx * k1, by + ty * k1, bz + tz * k1);
        P3.set(ax + tx * k0, ay + ty * k0, az + tz * k0);
        buf.quad(tex, blend, P0, P1, P2, P3, u0, 0f, u1, 1f, argb, argb);
    }
}
