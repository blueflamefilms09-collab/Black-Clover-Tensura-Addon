package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Demon Fire Magic: corrupted fire with its colours turned inside out. Ordinary fire is white-hot in the middle and red at the rim; Demon Fire is
 * pitch black in the middle and burns with light only along its edge, violet that tips into lilac-white where a flame is thinnest. Every flame
 * sprite (tools/gen_demon_fire_textures.py) is a grey body with a lit rim, so one quad gives both halves of the look: drawn with ALPHA blending the
 * black body covers the scene, drawn again with ADD only the light of the rim is added. The colour of the effect tints the rim; the palette
 * (violet, orchid, lilac, ink) is always mixed in, so the magic reads as itself even when the tint is white. The inverted palette is also literal:
 * the flashes use the NEGATIVE blend, which turns the picture behind them into its own negative for a few ticks.
 * <ul>
 *   <li>DEMON_FIRE_FX1, CAST / PROJECTILE (Demon Flame Bullet): 'from' = hand / eye, 'to' = target, power = size (1 = a bullet the size of a head),
 *       duration = flight ticks (16). The first quarter is the charge: a flat sigil and a black vortex open in front of the caster while violet
 *       motes spiral into a black sun that swells (an eclipse: a black disc with curling licks of flame and a lit crescent). Then the sun leaves 'from'
 *       with a comet of black flame behind it (a ragged stream, tongues of flame streaming back, bright fibres), echo rings of afterimage along the path
 *       and embers shed off the trail. It arrives at 'to' in the last 16 percent: an inverted flash (NEGATIVE star), a shock ring and sparks.</li>
 *   <li>DEMON_FIRE_FX2, ZONE / FIELD (Demon Fire Field, Hellfire Twister): 'from' = centre on the ground, power = RADIUS in blocks, duration = life
 *       (80). A scorched, cracked black patch with a slowly turning sigil and a counter-turning inner one; a wall of black flame rises round the rim;
 *       in the middle a twister of flame tongues whirls up (and a few lone tongues burn across the floor); runes orbit at mid height, a pulse ring
 *       sweeps out every second, ash and embers climb. Fades in over 8 ticks and out over the last 12.</li>
 *   <li>DEMON_FIRE_FX3, IMPACT / BURST / SIGNATURE (Demon Fire Eruption): 'from' = centre, 'to' = optional direction hint (a jet leans that way),
 *       power = scale, duration = life (28). A black sun and an inverted flash on the first ticks, three shock rings, a crown of black flame
 *       thrown outward over a spinning vortex on the ground, a tower of flame in the middle, shards of burnt glass and embers flying out in arcs,
 *       rays of streaking fire, smoke, and a scorched cracked patch that glows on after the fire has gone.</li>
 * </ul>
 */
public class DemonFireLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    static final ResourceLocation TONGUE = t("demon_fire_tongue"), ORB = t("demon_fire_orb"), CORONA = t("demon_fire_corona"), TRAIL = t("demon_fire_trail"),
            SIGIL = t("demon_fire_sigil"), SCORCH = t("demon_fire_scorch"), WALL = t("demon_fire_wall"), SWIRL = t("demon_fire_swirl"),
            FLASH = t("demon_fire_flash"), INVERSE = t("demon_fire_inverse"), RING = t("demon_fire_ring"), EMBER = t("demon_fire_ember"),
            SHARD = t("demon_fire_shard"), SMOKE = t("demon_fire_smoke"), RUNES = t("demon_fire_runes");

    /** The magic's own palette. VIOLET is the owner's glow colour; the tint of an effect is mixed into it, never swapped for it. */
    static final int VIOLET = 0xFF8A2AD0, ORCHID = 0xFFB85CFF, LILAC = 0xFFEBD2FF, DUSK = 0xFF4A2A6A;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.DEMON_FIRE_FX1, VfxShape.DEMON_FIRE_FX2, VfxShape.DEMON_FIRE_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case DEMON_FIRE_FX1 -> 16;
            case DEMON_FIRE_FX2 -> 80;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return VIOLET; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.DEMON_FIRE_FX3 && inst.power >= 1.2f) VfxShake.add(inst.payload.from(), Math.min(1.8f, 0.55f * inst.power), 10);
        else if (inst.shape == VfxShape.DEMON_FIRE_FX2 && inst.power >= 6f) VfxShake.add(inst.payload.from(), 0.45f, 8);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case DEMON_FIRE_FX1 -> cast(inst, ctx, buf);
            case DEMON_FIRE_FX2 -> zone(inst, ctx, buf);
            case DEMON_FIRE_FX3 -> burst(inst, ctx, buf);
            default -> { }
        }
    }

    // ================================================================== FX1: Demon Flame Bullet
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        final float age = inst.ageTicks(ctx.partialTick), D = inst.duration, p = Math.max(0.35f, inst.power), u = age / D;
        final Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        final Vector3f path = new Vector3f(b).sub(a);
        final float len = path.length();
        final Vector3f dir = len > 0.05f ? new Vector3f(path).div(len) : new Vector3f(0, 0, 1);
        final Vector3f sd = perp(dir), up = new Vector3f(sd).cross(dir).normalize();
        final int rim = tint(inst), hot = VfxVertexBuffer.whiten(rim, 0.55f);

        final float charge = sat(u / 0.24f);                               // the gathering
        final float fly = sat((u - 0.20f) / 0.66f);                        // the flight
        final float hit = sat((u - 0.84f) / 0.16f);                        // the arrival
        final float travel = flightCurve(fly);
        final Vector3f head = new Vector3f(dir).mul(len * travel).add(a);
        final float orbS = 0.95f * p * VfxAnim.easeOutBack(sat(age / (0.2f * D))) * (1f - 0.92f * hit);
        final float tailLen = Math.min(len * travel, 5.2f * p);
        final float live = 1f - hit;

        // ---- ALPHA layer: everything black
        if (len * travel > 1.0f && live > 0.02f) {                          // the long faint scar left along the whole path
            float afL = Math.min(len * travel, 24f);
            buf.beam(ctx, TRAIL, VfxBlend.ALPHA, new Vector3f(head).sub(new Vector3f(dir).mul(afL)), head, 0.20f * p, 0.46f * p, 1, -age * 0.07f,
                    VfxVertexBuffer.withAlpha(rim, 0f), VfxVertexBuffer.withAlpha(rim, 0.55f * live));
        }
        if (charge < 1f) {                                                  // the black vortex opening in front of the caster
            float s = 1.5f * p * (1f - 0.45f * charge) * VfxAnim.easeOutCubic(sat(age / 4f));
            VfxPose vp = VfxPose.facing(new Vector3f(a).add(new Vector3f(dir).mul(0.12f * p)), dir).spin(age * 0.55f);
            buf.plane(SWIRL, VfxBlend.ALPHA, vp, s * 0.5f, VfxVertexBuffer.withAlpha(rim, 0.95f * (1f - charge * charge)));
            buf.plane(SWIRL, VfxBlend.ADD, vp, s * 0.5f, VfxVertexBuffer.withAlpha(hot, 0.8f * (1f - charge * charge)));
        }
        if (tailLen > 0.15f) {
            Vector3f tail = new Vector3f(head).sub(new Vector3f(dir).mul(tailLen));
            float tf = sat(travel * 12f) * live;
            buf.beam(ctx, TRAIL, VfxBlend.ALPHA, tail, head, 0.12f * p, 1.15f * p, 3, -age * 0.28f, VfxVertexBuffer.withAlpha(rim, 0f), VfxVertexBuffer.withAlpha(rim, 0.97f * tf));
            // tongues of flame streaming back off the head: each grows, flickers and dies on its own beat
            int nt = ctx.seg(8, 4);
            for (int i = 0; i < nt; i++) {
                float s = (i + 0.4f + 0.4f * hash(inst.seed, i, 8)) / nt;
                float cyc = (age * 0.085f + hash(inst.seed, i, 1)) % 1f;
                float env = Mth.sin(Mth.PI * cyc);
                if (env < 0.05f) continue;
                Vector3f base = new Vector3f(head).sub(new Vector3f(dir).mul(0.15f * p + s * tailLen * 0.85f))
                        .add(new Vector3f(sd).mul((hash(inst.seed, i, 2) - 0.5f) * 0.55f * p)).add(new Vector3f(up).mul((hash(inst.seed, i, 3) - 0.5f) * 0.4f * p));
                Vector3f td = new Vector3f(dir).negate().add(new Vector3f(up).mul(0.30f + 0.3f * hash(inst.seed, i, 4))).add(new Vector3f(sd).mul((hash(inst.seed, i, 5) - 0.5f) * 0.6f)).normalize();
                float h = (0.75f + 0.95f * hash(inst.seed, i, 6)) * p * (1f - 0.45f * s) * (0.35f + 0.65f * env);
                tongue(buf, VfxBlend.ALPHA, i & 1, base, td, h, h * 0.58f, (i & 2) != 0, VfxVertexBuffer.withAlpha(rim, 0.95f * tf * Math.min(1f, env * 2f)), VfxVertexBuffer.withAlpha(rim, 0.8f * tf * env));
            }
        }
        if (orbS > 0.02f) buf.billboard(ctx, ORB, VfxBlend.ALPHA, head, orbS, age * 0.30f, VfxVertexBuffer.withAlpha(rim, 1f));
        if (hit > 0f) {                                                     // a few shards of burnt glass thrown off the target
            float e = VfxAnim.easeOutCubic(hit);
            for (int i = 0; i < 3; i++) {
                Vector3f d = new Vector3f(hash(inst.seed, i, 31) - 0.5f, 0.2f + hash(inst.seed, i, 32), hash(inst.seed, i, 33) - 0.5f).normalize();
                Vector3f q = new Vector3f(d).mul(1.5f * p * e).add(b).sub(0, 1.6f * hit * hit * p, 0);
                atlas(buf, ctx, SHARD, VfxBlend.ALPHA, q, 0.34f * p * live, age * 0.5f + i * 2f, (i + 1) & 3, VfxVertexBuffer.withAlpha(rim, live));
            }
        }

        // ---- NEGATIVE layer: the inverted flash of the arrival
        if (hit > 0f) {
            float g = Mth.clamp(0.95f * live * live, 0f, 1f);
            if (g > 0.05f) buf.billboard(ctx, INVERSE, VfxBlend.NEGATIVE, b, 3.4f * p * (0.35f + 0.65f * VfxAnim.easeOutCubic(hit)), hash(inst.seed, 0, 40) * Mth.TWO_PI, negative(g));
        }

        // ---- ADD layer: all the light
        if (u < 0.5f) {                                                      // the muzzle sigil and its counter-turning twin
            float sa = (float) Math.pow(Mth.sin(Mth.PI * sat(u / 0.5f)), 0.7);
            float sr = 0.78f * p * (0.55f + 0.45f * VfxAnim.easeOutCubic(sat(u / 0.18f)));
            buf.plane(SIGIL, VfxBlend.ADD, VfxPose.facing(new Vector3f(a).add(new Vector3f(dir).mul(0.28f * p)), dir).spin(age * 0.22f), sr, VfxVertexBuffer.withAlpha(hot, 0.9f * sa));
            buf.plane(SIGIL, VfxBlend.ADD, VfxPose.facing(new Vector3f(a).add(new Vector3f(dir).mul(0.50f * p)), dir).spin(-age * 0.36f), sr * 0.62f, VfxVertexBuffer.withAlpha(rim, 0.7f * sa));
        }
        if (charge < 1f) {                                                   // violet motes spiralling into the black sun
            int n = ctx.seg(8, 4);
            for (int i = 0; i < n; i++) {
                float born = hash(inst.seed, i, 11) * 0.4f, k = sat((charge - born) / (1f - born));
                if (k <= 0f || k >= 1f) continue;
                float ang = hash(inst.seed, i, 12) * Mth.TWO_PI + k * 2.6f, el = (hash(inst.seed, i, 13) - 0.5f) * 2.0f;
                Vector3f o = new Vector3f(sd).mul(Mth.cos(ang)).add(new Vector3f(up).mul(Mth.sin(ang))).add(new Vector3f(dir).mul(el * 0.6f)).normalize();
                float rr = (1.5f + 0.8f * hash(inst.seed, i, 14)) * p * (1f - k * k);
                ember(buf, ctx, new Vector3f(o).mul(rr).add(a), 0.30f * p * (1f - 0.5f * k), screenAngle(ctx, o), VfxVertexBuffer.withAlpha(hot, (float) Math.pow(Mth.sin(Mth.PI * k), 0.6)));
            }
        }
        if (u > 0.17f && u < 0.34f) {                                       // the launch: a ring thrown forward off the muzzle
            float k = sat((u - 0.17f) / 0.17f);
            buf.plane(RING, VfxBlend.ADD, VfxPose.facing(new Vector3f(a).add(new Vector3f(dir).mul((0.3f + 1.4f * k) * p)), dir), (0.35f + 1.1f * VfxAnim.easeOutCubic(k)) * p, VfxVertexBuffer.withAlpha(hot, (1f - k) * 0.9f));
        }
        if (tailLen > 0.15f) {
            Vector3f tail = new Vector3f(head).sub(new Vector3f(dir).mul(tailLen));
            float tf = sat(travel * 12f) * live;
            buf.beam(ctx, TRAIL, VfxBlend.ADD, tail, head, 0.12f * p, 1.15f * p, 3, -age * 0.28f, VfxVertexBuffer.withAlpha(rim, 0f), VfxVertexBuffer.withAlpha(hot, 0.9f * tf));
            int nt = ctx.seg(8, 4);
            for (int i = 0; i < nt; i += 2) {                               // the rim light of the biggest tongues
                float s = (i + 0.4f + 0.4f * hash(inst.seed, i, 8)) / nt;
                float cyc = (age * 0.085f + hash(inst.seed, i, 1)) % 1f;
                float env = Mth.sin(Mth.PI * cyc);
                if (env < 0.2f) continue;
                Vector3f base = new Vector3f(head).sub(new Vector3f(dir).mul(0.15f * p + s * tailLen * 0.85f))
                        .add(new Vector3f(sd).mul((hash(inst.seed, i, 2) - 0.5f) * 0.55f * p)).add(new Vector3f(up).mul((hash(inst.seed, i, 3) - 0.5f) * 0.4f * p));
                Vector3f td = new Vector3f(dir).negate().add(new Vector3f(up).mul(0.30f + 0.3f * hash(inst.seed, i, 4))).add(new Vector3f(sd).mul((hash(inst.seed, i, 5) - 0.5f) * 0.6f)).normalize();
                float h = (0.75f + 0.95f * hash(inst.seed, i, 6)) * p * (1f - 0.45f * s) * (0.35f + 0.65f * env);
                tongue(buf, VfxBlend.ADD, i & 1, base, td, h, h * 0.58f, (i & 2) != 0, VfxVertexBuffer.withAlpha(hot, 0.8f * tf * env), VfxVertexBuffer.withAlpha(hot, 0.6f * tf * env));
            }
        }
        if (orbS > 0.02f) {
            buf.billboard(ctx, ORB, VfxBlend.ADD, head, orbS, age * 0.30f, VfxVertexBuffer.withAlpha(hot, 0.9f));
            float breathe = 0.9f + 0.1f * Mth.sin(age * 0.9f);
            buf.billboard(ctx, CORONA, VfxBlend.ADD, head, orbS * 3f * breathe, -age * 0.17f, VfxVertexBuffer.withAlpha(rim, 0.85f));
            for (int k = 1; k <= 4; k++) {                                   // afterimage: echoes of the sun, each a little smaller and fainter
                float fk = sat(((u - k * 1.5f / D) - 0.20f) / 0.66f);
                if (fk <= 0f || u - k * 1.5f / D < 0.12f) continue;
                Vector3f g = new Vector3f(dir).mul(len * flightCurve(fk)).add(a);
                if (g.distanceSquared(head) < 0.04f * p * p) continue;
                buf.billboard(ctx, ORB, VfxBlend.ADD, g, orbS * (1f - 0.13f * k), age * 0.30f - k * 0.5f, VfxVertexBuffer.withAlpha(rim, 0.62f / k));
            }
        }
        if (len * travel > 0.5f) {                                            // embers shed along the path, drifting up and out
            int ne = ctx.seg(10, 5);
            for (int i = 0; i < ne; i++) {
                float life = (age * 0.075f + hash(inst.seed, i, 21)) % 1f;
                float back = (0.2f + 1.6f * hash(inst.seed, i, 22)) * tailLen / Math.max(0.5f, 5.2f * p);
                Vector3f drift = new Vector3f(sd).mul((hash(inst.seed, i, 23) - 0.5f) * 1.4f).add(new Vector3f(up).mul(0.5f + hash(inst.seed, i, 24))).add(0, 0.5f, 0);
                Vector3f q = new Vector3f(head).sub(new Vector3f(dir).mul(back * p * 3f)).add(new Vector3f(drift).mul(life * 1.1f * p));
                ember(buf, ctx, q, 0.24f * p * (1f - 0.6f * life), screenAngle(ctx, new Vector3f(drift).negate()), VfxVertexBuffer.withAlpha(life < 0.5f ? hot : rim, (1f - life) * live));
            }
        }
        if (hit > 0f) {                                                       // the arrival: a star of light, a shock ring, sparks
            float e = VfxAnim.easeOutCubic(hit), fa = 1f - hit;
            buf.billboard(ctx, FLASH, VfxBlend.ADD, b, 4.0f * p * (0.45f + 0.55f * e), age * 0.1f, VfxVertexBuffer.withAlpha(hot, (float) Math.pow(fa, 0.8)));
            buf.billboard(ctx, RING, VfxBlend.ADD, b, (0.7f + 3.3f * e) * p, age * 0.05f, VfxVertexBuffer.withAlpha(rim, fa));
            VfxBloom.glow(ctx, buf, b, 1.1f * p * fa, rim, 0.9f * fa);
            for (int i = 0; i < 6; i++) {
                float ang = hash(inst.seed, i, 41) * Mth.TWO_PI, el = (hash(inst.seed, i, 42) - 0.35f) * 1.4f;
                Vector3f d = new Vector3f(sd).mul(Mth.cos(ang)).add(new Vector3f(up).mul(Mth.sin(ang))).add(new Vector3f(dir).mul(-0.4f + el * 0.3f)).normalize();
                ember(buf, ctx, new Vector3f(d).mul(1.9f * p * e * (0.6f + 0.8f * hash(inst.seed, i, 43))).add(b), 0.26f * p * fa, screenAngle(ctx, new Vector3f(d).negate()), VfxVertexBuffer.withAlpha(hot, fa));
            }
        }
    }

    /** The bullet does not leave at full speed: it slides out of the charge and is fastest on arrival. */
    private static float flightCurve(float f) { return (float) Math.pow(f, 1.5); }

    // ================================================================== FX2: Demon Fire Field
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        final float age = inst.ageTicks(ctx.partialTick), R = Math.max(1.5f, inst.power);
        final float fade = life(inst, age, 8f, 12f);
        if (fade <= 0.01f) return;
        final float open = VfxAnim.easeOutCubic(sat(age / 10f)), r = R * open;
        final float rise = VfxAnim.easeOutBack(sat((age - 2f) / 12f));
        final float H = Mth.clamp(0.30f * R + 0.9f, 1.5f, 4.4f) * rise;
        final Vector3f c = ctx.rel(inst.from(ctx));
        final VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, 0.05f, 0));
        final int rim = tint(inst), hot = VfxVertexBuffer.whiten(rim, 0.5f);
        final float flick = 0.88f + 0.12f * Mth.sin(age * 0.9f);
        final float sz = Mth.clamp(0.55f * Mth.sqrt(R), 0.85f, 2.0f);

        // ---- ALPHA layer: the burnt ground, the wall, the twister, loose flames, ash
        buf.plane(SCORCH, VfxBlend.ALPHA, ground.spin(0.4f), r * 1.06f, VfxVertexBuffer.withAlpha(rim, 0.92f * fade));
        int seg = ctx.seg(Mth.clamp(Math.round(R * 1.6f), 12, 20), 8);
        float repeats = Math.max(3, Math.round(Mth.TWO_PI * r / (2f * Math.max(0.8f, H))));
        if (H > 0.2f) hoop(buf, WALL, VfxBlend.ALPHA, VfxPose.ground(new Vector3f(c).add(0, H * 0.5f, 0)), r * 0.97f, H * 0.5f, seg, repeats, age * 0.0035f, VfxVertexBuffer.withAlpha(rim, 0.97f * fade));

        final float Ht = Mth.clamp(0.85f * R, 2.6f, 7.5f) * rise;
        final int nTw = ctx.seg(9, 5);
        for (int i = 0; i < nTw; i++) {                                      // the twister: tongues on a rising spiral, trailing their own spin
            float f = (i + 0.5f) / nTw;
            float rho = 0.10f * R + 0.25f + (0.20f * R + 0.5f) * (float) Math.pow(f, 1.15f);
            float th = i * 2.39996f + age * 0.22f + f * 2.4f;
            Vector3f base = new Vector3f(Mth.cos(th) * rho, f * Ht * 0.75f, Mth.sin(th) * rho).mul(open).add(c);
            Vector3f td = new Vector3f(Mth.sin(th), 0.8f, -Mth.cos(th)).normalize();
            float h = sz * (1.4f + 1.3f * f) * (0.85f + 0.3f * Mth.sin(age * 0.5f + i * 1.7f)) * rise;
            tongue(buf, VfxBlend.ALPHA, i & 1, base, td, h, h * 0.60f, (i & 2) != 0, VfxVertexBuffer.withAlpha(rim, 0.95f * fade), VfxVertexBuffer.withAlpha(rim, 0.85f * fade));
        }
        final int nLone = ctx.seg(6, 3);
        for (int i = 0; i < nLone; i++) {                                    // lone flames burning across the floor
            float ang = hash(inst.seed, i, 51) * Mth.TWO_PI, d = (0.28f + 0.6f * hash(inst.seed, i, 52)) * r;
            float cyc = (age * 0.05f + hash(inst.seed, i, 53)) % 1f, env = Mth.sin(Mth.PI * cyc);
            if (env < 0.08f) continue;
            Vector3f base = new Vector3f(Mth.cos(ang) * d, 0.02f, Mth.sin(ang) * d).add(c);
            float h = sz * (0.8f + 0.9f * hash(inst.seed, i, 54)) * (0.3f + 0.7f * env) * rise;
            tongue(buf, VfxBlend.ALPHA, (i + 1) & 1, base, new Vector3f(0.12f * Mth.sin(age * 0.3f + i), 1f, 0.1f).normalize(), h, h * 0.62f, (i & 1) != 0,
                    VfxVertexBuffer.withAlpha(rim, 0.9f * fade * Math.min(1f, env * 2f)), VfxVertexBuffer.withAlpha(rim, 0.8f * fade * env));
        }
        final int nAsh = ctx.seg(5, 2);
        for (int i = 0; i < nAsh; i++) {                                     // ash flakes of burnt glass drifting up
            float life = (age * 0.022f + hash(inst.seed, i, 61)) % 1f;
            float ang = hash(inst.seed, i, 62) * Mth.TWO_PI + life * 1.6f, d = Mth.sqrt(hash(inst.seed, i, 63)) * r * 0.9f;
            Vector3f q = new Vector3f(Mth.cos(ang) * d, 0.4f + life * (Ht + 1.5f), Mth.sin(ang) * d).add(c);
            atlas(buf, ctx, SHARD, VfxBlend.ALPHA, q, 0.30f * sz * (1f - 0.3f * life), age * 0.12f + i * 1.9f, i & 3, VfxVertexBuffer.withAlpha(rim, fade * Mth.sin(Mth.PI * life)));
        }

        // ---- ADD layer: the light
        VfxBloom.planeGlow(buf, ground, r, rim, 0.55f * fade);
        buf.plane(SCORCH, VfxBlend.ADD, ground.lift(0.004f).spin(0.4f), r * 1.06f, VfxVertexBuffer.withAlpha(hot, (0.5f + 0.35f * Mth.sin(age * 0.17f)) * fade));
        buf.plane(SIGIL, VfxBlend.ADD, ground.lift(0.008f).spin(age * 0.014f), r * 0.99f, VfxVertexBuffer.withAlpha(hot, 0.88f * fade));
        buf.plane(SIGIL, VfxBlend.ADD, ground.lift(0.012f).spin(-age * 0.034f + 0.7f), r * 0.56f, VfxVertexBuffer.withAlpha(rim, 0.6f * fade));
        for (int k = 0; k < 2; k++) {                                        // a pulse ring sweeping out from the middle, two staggered
            float ph = ((age + k * 12f) % 24f) / 24f;
            buf.plane(RING, VfxBlend.ADD, ground.lift(0.016f + 0.002f * k), r * (0.12f + 0.88f * VfxAnim.easeOutCubic(ph)), VfxVertexBuffer.withAlpha(hot, (1f - ph) * 0.8f * fade));
        }
        if (H > 0.2f) hoop(buf, WALL, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, H * 0.5f, 0)), r * 0.97f, H * 0.5f, seg, repeats, age * 0.0035f, VfxVertexBuffer.withAlpha(hot, 0.85f * fade * flick));
        for (int i = 0; i < nTw; i += 2) {                                   // the rim light of the twister
            float f = (i + 0.5f) / nTw;
            float rho = 0.10f * R + 0.25f + (0.20f * R + 0.5f) * (float) Math.pow(f, 1.15f);
            float th = i * 2.39996f + age * 0.22f + f * 2.4f;
            Vector3f base = new Vector3f(Mth.cos(th) * rho, f * Ht * 0.75f, Mth.sin(th) * rho).mul(open).add(c);
            Vector3f td = new Vector3f(Mth.sin(th), 0.8f, -Mth.cos(th)).normalize();
            float h = sz * (1.4f + 1.3f * f) * (0.85f + 0.3f * Mth.sin(age * 0.5f + i * 1.7f)) * rise;
            tongue(buf, VfxBlend.ADD, i & 1, base, td, h, h * 0.60f, (i & 2) != 0, VfxVertexBuffer.withAlpha(hot, 0.75f * fade), VfxVertexBuffer.withAlpha(hot, 0.55f * fade));
        }
        final int nRune = ctx.seg(6, 3);
        for (int i = 0; i < nRune; i++) {                                    // runes orbiting at mid height
            float th = Mth.TWO_PI * i / nRune + age * 0.035f;
            Vector3f q = new Vector3f(Mth.cos(th) * 0.8f * r, 0.9f + 0.35f * H + 0.25f * Mth.sin(age * 0.11f + i * 2.2f), Mth.sin(th) * 0.8f * r).add(c);
            atlas(buf, ctx, RUNES, VfxBlend.ADD, q, 0.62f * sz, 0f, i & 3, VfxVertexBuffer.withAlpha(hot, 0.85f * fade * (0.7f + 0.3f * Mth.sin(age * 0.2f + i))));
        }
        final int nEm = ctx.seg(12, 6);
        for (int i = 0; i < nEm; i++) {                                      // embers climbing out of the field
            float life = (age * 0.032f + hash(inst.seed, i, 71)) % 1f;
            float ang = hash(inst.seed, i, 72) * Mth.TWO_PI + life * 1.2f, d = Mth.sqrt(hash(inst.seed, i, 73)) * r * 0.95f;
            Vector3f q = new Vector3f(Mth.cos(ang) * d, 0.2f + life * (Ht + 2.0f), Mth.sin(ang) * d).add(c);
            ember(buf, ctx, q, (0.22f + 0.1f * hash(inst.seed, i, 74)) * sz, -Mth.HALF_PI, VfxVertexBuffer.withAlpha(life < 0.4f ? hot : rim, fade * Mth.sin(Mth.PI * life) * flick));
        }
    }

    // ================================================================== FX3: Demon Fire Eruption
    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        final float age = inst.ageTicks(ctx.partialTick), D = inst.duration, p = Math.max(0.4f, inst.power);
        final float T = age * 28f / D;                                        // time in "standard" ticks, whatever the real duration is
        final Vector3f c = ctx.rel(inst.from(ctx));
        final Vector3f hint = new Vector3f(ctx.rel(inst.to(ctx))).sub(c);
        final boolean jet = hint.length() > 0.4f;
        final Vector3f jd = jet ? new Vector3f(hint).normalize() : new Vector3f(0, 1, 0);
        final int rim = tint(inst), hot = VfxVertexBuffer.whiten(rim, 0.55f);
        final float fadeAll = 1f - VfxAnim.easeInCubic(sat((T - 16f) / 12f));   // everything dims over the last 12 ticks
        final VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, -0.45f * p, 0));

        // ---- ALPHA layer
        float scorchK = VfxAnim.easeOutCubic(sat(T / 5f));
        float scorchA = 0.9f * (1f - sat((T - 18f) / 10f));
        if (scorchA > 0.02f) buf.plane(SCORCH, VfxBlend.ALPHA, ground.spin(hash(inst.seed, 0, 80) * Mth.TWO_PI), 2.5f * p * scorchK, VfxVertexBuffer.withAlpha(rim, scorchA));
        float sunK = 1f - smooth(1.5f, 7f, T);
        if (sunK > 0.02f) buf.billboard(ctx, ORB, VfxBlend.ALPHA, c, 2.1f * p * (0.4f + 0.6f * VfxAnim.easeOutCubic(sat(T / 2.5f))) * sunK, T * 0.35f, VfxVertexBuffer.withAlpha(rim, 1f));
        float vor = sat(T / 3f) * (1f - sat((T - 6f) / 10f));                   // the vortex spinning on the ground
        if (vor > 0.02f) buf.plane(SWIRL, VfxBlend.ALPHA, ground.lift(0.02f).spin(T * 0.34f), 2.9f * p * (0.35f + 0.65f * VfxAnim.easeOutCubic(sat(T / 10f))), VfxVertexBuffer.withAlpha(rim, 0.9f * vor));

        // the crown: black flames thrown out of the middle, and the tower rising in it
        final int nCrown = ctx.seg(12, 6);
        for (int i = 0; i < nCrown; i++) {
            float ang = Mth.TWO_PI * i / nCrown + hash(inst.seed, i, 81) * 0.5f;
            float born = 0.3f * hash(inst.seed, i, 82) * 4f, tt = T - born;
            if (tt <= 0f) continue;
            float grow = VfxAnim.easeOutCubic(sat(tt / 6f)) * (1f - smooth(10f, 24f, tt));
            if (grow < 0.03f) continue;
            float rho = (0.25f + 1.5f * VfxAnim.easeOutCubic(sat(tt / 12f))) * p * (0.75f + 0.5f * hash(inst.seed, i, 83));
            Vector3f out = new Vector3f(Mth.cos(ang), 0f, Mth.sin(ang));
            Vector3f base = new Vector3f(out).mul(rho).add(c).sub(0, 0.35f * p, 0);
            float lean = jet ? 0.55f * Math.max(0f, out.dot(jd)) + 0.0f : 0f;
            Vector3f td = new Vector3f(out).mul(0.55f).add(0, 1.0f, 0).add(new Vector3f(jd).mul(lean)).normalize();
            float h = (1.5f + 1.5f * hash(inst.seed, i, 84)) * p * grow * (jet ? 1f + 0.5f * Math.max(0f, out.dot(jd)) : 1f);
            tongue(buf, VfxBlend.ALPHA, i & 1, base, td, h, h * 0.62f, (i & 2) != 0, VfxVertexBuffer.withAlpha(rim, 0.95f * fadeAll), VfxVertexBuffer.withAlpha(rim, 0.8f * fadeAll));
        }
        for (int i = 0; i < 3; i++) {
            float tt = T - i * 1.2f;
            float grow = VfxAnim.easeOutBack(sat(tt / 6f)) * (1f - smooth(11f, 22f, tt));
            if (grow < 0.03f) continue;
            float h = (4.6f - 0.9f * i) * p * grow;
            Vector3f td = new Vector3f(0.1f * (i - 1), 1f, 0.08f * (1 - i)).add(jet ? new Vector3f(jd).mul(0.35f) : new Vector3f()).normalize();
            tongue(buf, VfxBlend.ALPHA, (i + 1) & 1, new Vector3f(c).sub(0, 0.45f * p, 0), td, h, h * 0.5f, i == 1, VfxVertexBuffer.withAlpha(rim, 0.98f * fadeAll), VfxVertexBuffer.withAlpha(rim, 0.85f * fadeAll));
        }
        final int nSm = ctx.seg(4, 2);
        for (int i = 0; i < nSm; i++) {
            float tt = T - 5f - i * 1.5f;
            if (tt <= 0f) continue;
            float k = sat(tt / 18f);
            Vector3f q = new Vector3f((hash(inst.seed, i, 85) - 0.5f) * 2.2f * p, 0.6f * p + k * 3.2f * p, (hash(inst.seed, i, 86) - 0.5f) * 2.2f * p).add(c);
            buf.billboard(ctx, SMOKE, VfxBlend.ALPHA, q, (1.4f + 1.8f * k) * p, i * 1.7f + k, VfxVertexBuffer.withAlpha(DUSK, 0.75f * Mth.sin(Mth.PI * k) * fadeAll));
        }
        final int nSh = ctx.seg(10, 5);
        for (int i = 0; i < nSh; i++) {                                         // shards of burnt glass in ballistic arcs
            float tt = sat(T / 24f);
            float ang = hash(inst.seed, i, 87) * Mth.TWO_PI, el = 0.3f + 0.9f * hash(inst.seed, i, 88);
            Vector3f d = new Vector3f(Mth.cos(ang), el, Mth.sin(ang)).add(new Vector3f(jd).mul(jet ? 0.6f : 0f)).normalize();
            float spd = (4.2f + 3.4f * hash(inst.seed, i, 89)) * p, sec = T / 20f;
            Vector3f q = new Vector3f(d).mul(spd * sec * (1f - 0.35f * sec)).add(c).sub(0, 4.5f * sec * sec, 0);
            atlas(buf, ctx, SHARD, VfxBlend.ALPHA, q, (0.30f + 0.2f * hash(inst.seed, i, 90)) * p, T * 0.4f + i * 1.7f, i & 3, VfxVertexBuffer.withAlpha(rim, fadeAll * (1f - 0.6f * tt)));
        }

        // ---- NEGATIVE layer: the inverted flash
        float g = Mth.clamp(0.95f * (float) Math.pow(1f - sat(T / 5.5f), 1.4f), 0f, 1f);
        if (g > 0.05f) buf.billboard(ctx, INVERSE, VfxBlend.NEGATIVE, c, 4.4f * p * (0.4f + 0.6f * VfxAnim.easeOutCubic(sat(T / 3f))), hash(inst.seed, 0, 91) * Mth.TWO_PI, negative(g));

        // ---- ADD layer
        float fl = 1f - sat(T / 9f);
        if (fl > 0.02f) {
            buf.billboard(ctx, FLASH, VfxBlend.ADD, c, 5.0f * p * (0.45f + 0.55f * VfxAnim.easeOutCubic(sat(T / 4f))), T * 0.05f, VfxVertexBuffer.withAlpha(hot, (float) Math.pow(fl, 0.8)));
            if (sunK > 0.02f) buf.billboard(ctx, CORONA, VfxBlend.ADD, c, 6.0f * p * (0.4f + 0.6f * VfxAnim.easeOutCubic(sat(T / 2.5f))) * sunK, -T * 0.2f, VfxVertexBuffer.withAlpha(rim, 0.9f));
        }
        VfxBloom.glow(ctx, buf, new Vector3f(c).add(0, 0.5f * p, 0), 1.5f * p * (0.4f + 0.6f * (1f - sat(T / 24f))), rim, 0.8f * fadeAll * (0.35f + 0.65f * fl));
        for (int k = 0; k < 3; k++) {                                          // shock rings: ground, ground (late), upright
            float tt = T - k * 2.5f;
            if (tt <= 0f || tt > 16f) continue;
            float e = VfxAnim.easeOutCubic(sat(tt / 14f)), fa = 1f - sat(tt / 16f);
            if (k < 2) buf.plane(RING, VfxBlend.ADD, ground.lift(0.03f + 0.01f * k).spin(k * 1.1f), (4.4f - 1.0f * k) * p * e, VfxVertexBuffer.withAlpha(k == 0 ? hot : rim, fa));
            else buf.billboard(ctx, RING, VfxBlend.ADD, c, 7.2f * p * e, T * 0.04f, VfxVertexBuffer.withAlpha(hot, fa * 0.8f));
        }
        if (vor > 0.02f) buf.plane(SWIRL, VfxBlend.ADD, ground.lift(0.02f).spin(T * 0.34f), 2.9f * p * (0.35f + 0.65f * VfxAnim.easeOutCubic(sat(T / 10f))), VfxVertexBuffer.withAlpha(hot, 0.8f * vor));
        float crk = (0.5f + 0.5f * Mth.sin(T * 0.45f)) * (1f - sat((T - 12f) / 16f)) * sat(T / 4f);
        buf.plane(SCORCH, VfxBlend.ADD, ground.lift(0.004f).spin(hash(inst.seed, 0, 80) * Mth.TWO_PI), 2.5f * p * scorchK, VfxVertexBuffer.withAlpha(hot, 0.35f + 0.55f * crk));
        for (int i = 0; i < nCrown; i += 2) {                                   // the rim light of the crown
            float ang = Mth.TWO_PI * i / nCrown + hash(inst.seed, i, 81) * 0.5f;
            float born = 0.3f * hash(inst.seed, i, 82) * 4f, tt = T - born;
            if (tt <= 0f) continue;
            float grow = VfxAnim.easeOutCubic(sat(tt / 6f)) * (1f - smooth(10f, 24f, tt));
            if (grow < 0.03f) continue;
            float rho = (0.25f + 1.5f * VfxAnim.easeOutCubic(sat(tt / 12f))) * p * (0.75f + 0.5f * hash(inst.seed, i, 83));
            Vector3f out = new Vector3f(Mth.cos(ang), 0f, Mth.sin(ang));
            Vector3f base = new Vector3f(out).mul(rho).add(c).sub(0, 0.35f * p, 0);
            float lean = jet ? 0.55f * Math.max(0f, out.dot(jd)) : 0f;
            Vector3f td = new Vector3f(out).mul(0.55f).add(0, 1.0f, 0).add(new Vector3f(jd).mul(lean)).normalize();
            float h = (1.5f + 1.5f * hash(inst.seed, i, 84)) * p * grow * (jet ? 1f + 0.5f * Math.max(0f, out.dot(jd)) : 1f);
            tongue(buf, VfxBlend.ADD, i & 1, base, td, h, h * 0.62f, (i & 2) != 0, VfxVertexBuffer.withAlpha(hot, 0.7f * fadeAll), VfxVertexBuffer.withAlpha(hot, 0.5f * fadeAll));
        }
        for (int i = 0; i < 3; i++) {                                           // the rim light of the tower
            float tt = T - i * 1.2f;
            float grow = VfxAnim.easeOutBack(sat(tt / 6f)) * (1f - smooth(11f, 22f, tt));
            if (grow < 0.03f) continue;
            float h = (4.6f - 0.9f * i) * p * grow;
            Vector3f td = new Vector3f(0.1f * (i - 1), 1f, 0.08f * (1 - i)).add(jet ? new Vector3f(jd).mul(0.35f) : new Vector3f()).normalize();
            tongue(buf, VfxBlend.ADD, (i + 1) & 1, new Vector3f(c).sub(0, 0.45f * p, 0), td, h, h * 0.5f, i == 1, VfxVertexBuffer.withAlpha(hot, 0.8f * fadeAll), VfxVertexBuffer.withAlpha(hot, 0.55f * fadeAll));
        }
        final int nRay = ctx.seg(6, 3);
        for (int i = 0; i < nRay; i++) {                                        // rays of streaking fire thrown out of the flash
            float ang = hash(inst.seed, i, 92) * Mth.TWO_PI, el = (hash(inst.seed, i, 93) - 0.2f) * 1.2f;
            Vector3f d = new Vector3f(Mth.cos(ang), el, Mth.sin(ang)).add(new Vector3f(jd).mul(jet ? 0.8f : 0f)).normalize();
            float k = sat((T - 0.5f) / 8f);
            if (k <= 0f || k >= 1f) continue;
            float e = VfxAnim.easeOutCubic(k);
            Vector3f tail = new Vector3f(d).mul((0.5f + 4.5f * Math.max(0f, e - 0.35f)) * p).add(c);
            Vector3f head = new Vector3f(d).mul((0.8f + 5.5f * e) * p).add(c);
            buf.beam(ctx, TRAIL, VfxBlend.ADD, tail, head, 0.05f * p, 0.30f * p, 1, 0f, VfxVertexBuffer.withAlpha(rim, 0f), VfxVertexBuffer.withAlpha(hot, 0.95f * (1f - k)));
        }
        for (int i = 0; i < nSh; i += 2) {                                      // the edges of the shards catch the light
            float ang = hash(inst.seed, i, 87) * Mth.TWO_PI, el = 0.3f + 0.9f * hash(inst.seed, i, 88);
            Vector3f d = new Vector3f(Mth.cos(ang), el, Mth.sin(ang)).add(new Vector3f(jd).mul(jet ? 0.6f : 0f)).normalize();
            float spd = (4.2f + 3.4f * hash(inst.seed, i, 89)) * p, sec = T / 20f;
            Vector3f q = new Vector3f(d).mul(spd * sec * (1f - 0.35f * sec)).add(c).sub(0, 4.5f * sec * sec, 0);
            atlas(buf, ctx, SHARD, VfxBlend.ADD, q, (0.30f + 0.2f * hash(inst.seed, i, 90)) * p, T * 0.4f + i * 1.7f, i & 3, VfxVertexBuffer.withAlpha(hot, 0.9f * fadeAll));
        }
        final int nEm = ctx.seg(16, 8);
        for (int i = 0; i < nEm; i++) {                                         // embers flying out, falling, going dark
            float ang = hash(inst.seed, i, 94) * Mth.TWO_PI, el = -0.1f + 1.1f * hash(inst.seed, i, 95);
            Vector3f d = new Vector3f(Mth.cos(ang), el, Mth.sin(ang)).add(new Vector3f(jd).mul(jet ? 0.5f : 0f)).normalize();
            float spd = (2.5f + 4.5f * hash(inst.seed, i, 96)) * p, sec = T / 20f, lifeK = sat(T / (14f + 12f * hash(inst.seed, i, 97)));
            if (lifeK >= 1f) continue;
            Vector3f v = new Vector3f(d).mul(spd * sec * (1f - 0.3f * sec)).sub(0, 3.0f * sec * sec, 0);
            ember(buf, ctx, new Vector3f(v).add(c), (0.30f - 0.15f * hash(inst.seed, i, 98)) * p * (1f - 0.7f * lifeK), screenAngle(ctx, new Vector3f(v).negate()),
                    VfxVertexBuffer.withAlpha(lifeK < 0.4f ? hot : rim, 1f - lifeK));
        }
    }

    // ================================================================== building blocks
    /** A flame tongue standing on {@code base} (camera-relative) with its tip {@code height} along the unit vector {@code dir}, turned to face the camera. */
    private static void tongue(VfxVertexBuffer buf, VfxBlend blend, int variant, Vector3f base, Vector3f dir, float height, float width, boolean flip, int cBase, int cTip) {
        if (height < 0.02f || !buf.hasBudget(4)) return;
        Vector3f mid = new Vector3f(dir).mul(height * 0.5f).add(base);
        Vector3f side = new Vector3f(dir).cross(new Vector3f(mid).negate());
        if (side.lengthSquared() < 1e-8f) return;
        side.normalize().mul(width * 0.5f);
        Vector3f tip = new Vector3f(dir).mul(height).add(base);
        float u0 = (variant & 1) * 0.5f, u1 = u0 + 0.5f;
        if (flip) { float s = u0; u0 = u1; u1 = s; }
        buf.quad(TONGUE, blend, new Vector3f(base).sub(side), new Vector3f(base).add(side), new Vector3f(tip).add(side), new Vector3f(tip).sub(side), u0, 0f, u1, 1f, cBase, cTip);
    }

    /** A spark whose tail trails along screen angle {@code tailAngle} (0 = screen right, PI/2 = up). */
    private static void ember(VfxVertexBuffer buf, VfxRenderContext ctx, Vector3f at, float size, float tailAngle, int argb) {
        if (size < 0.01f) return;
        buf.billboard(ctx, EMBER, VfxBlend.ADD, at, size, tailAngle - Mth.HALF_PI, argb);
    }

    /** One cell (0..3) of a 2 x 2 sprite sheet as a camera-facing square. */
    private static void atlas(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, VfxBlend blend, Vector3f c, float size, float rot, int idx, int argb) {
        if (size < 0.01f || !buf.hasBudget(4)) return;
        float cs = Mth.cos(rot) * size * 0.5f, sn = Mth.sin(rot) * size * 0.5f;
        Vector3f rx = new Vector3f(ctx.camRight).mul(cs).add(new Vector3f(ctx.camUp).mul(sn));
        Vector3f ry = new Vector3f(ctx.camUp).mul(cs).sub(new Vector3f(ctx.camRight).mul(sn));
        float u0 = (idx & 1) * 0.5f, v0 = ((idx >> 1) & 1) * 0.5f;
        buf.quad(tex, blend, new Vector3f(c).sub(rx).sub(ry), new Vector3f(c).add(rx).sub(ry), new Vector3f(c).add(rx).add(ry), new Vector3f(c).sub(rx).add(ry),
                u0, v0, u0 + 0.5f, v0 + 0.5f, argb, argb);
    }

    /** A ribbon standing on a circle, width along the pose normal (the base is the lower edge); U wraps {@code repeats} times round and scrolls. */
    private static void hoop(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, VfxPose pose, float radius, float halfWidth, int segments, float repeats, float uScroll, int argb) {
        Vector3f n = new Vector3f(pose.normal()).mul(halfWidth);
        for (int i = 0; i < segments; i++) {
            float a0 = Mth.TWO_PI * i / segments, a1 = Mth.TWO_PI * (i + 1) / segments;
            Vector3f m0 = pose.point(Mth.cos(a0) * radius, Mth.sin(a0) * radius), m1 = pose.point(Mth.cos(a1) * radius, Mth.sin(a1) * radius);
            float u0 = uScroll + repeats * i / segments, u1 = uScroll + repeats * (i + 1) / segments;
            buf.quad(tex, blend, new Vector3f(m0).sub(n), new Vector3f(m1).sub(n), new Vector3f(m1).add(n), new Vector3f(m0).add(n), u0, 0f, u1, 1f, argb, argb);
        }
    }

    // ================================================================== small helpers
    /** The rim colour of this effect: the owner's violet with the spell's tint mixed in (never replaced by it). */
    private static int tint(VfxInstance inst) { return VfxVertexBuffer.lerpColor(VIOLET, 0xFF000000 | (inst.color & 0xFFFFFF), 0.30f); }

    /** Vertex colour for the NEGATIVE blend: how hard the picture behind is inverted, with a violet lean. */
    private static int negative(float g) {
        return 0xFF000000 | ((int) (g * 225f) << 16) | ((int) (g * 170f) << 8) | (int) (g * 255f);
    }

    private static float sat(float x) { return Mth.clamp(x, 0f, 1f); }

    private static float smooth(float a, float b, float x) { float k = sat((x - a) / (b - a)); return k * k * (3f - 2f * k); }

    /** Angle on the screen (0 = right, PI/2 = up) of a camera-relative direction. */
    private static float screenAngle(VfxRenderContext ctx, Vector3f dir) { return (float) Math.atan2(dir.dot(ctx.camUp), dir.dot(ctx.camRight)); }

    /** A unit vector perpendicular to {@code dir}, horizontal when possible. */
    private static Vector3f perp(Vector3f dir) {
        Vector3f s = new Vector3f(dir).cross(0, 1, 0);
        if (s.lengthSquared() < 1e-5f) s.set(1, 0, 0);
        return s.normalize();
    }

    /** 1 while running, ramping 0 -> 1 over the first {@code in} ticks and 1 -> 0 over the last {@code out}. */
    private static float life(VfxInstance inst, float age, float in, float out) {
        float a = in <= 0 ? 1 : Mth.clamp(age / in, 0, 1);
        float b = out <= 0 ? 1 : Mth.clamp((inst.duration - age) / out, 0, 1);
        return Math.min(a, b);
    }

    /** Deterministic 0..1 hash of (effect seed, index, salt): fixed per-piece randomness without any state. */
    private static float hash(long seed, int i, int salt) {
        long x = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L + salt * 0x94D049BB133111EBL;
        x ^= x >>> 31; x *= 0x7FB5D329728EA185L; x ^= x >>> 27; x *= 0x81DADEF4BC2DD44DL; x ^= x >>> 33;
        return (x >>> 40) / (float) (1L << 24);
    }
}
