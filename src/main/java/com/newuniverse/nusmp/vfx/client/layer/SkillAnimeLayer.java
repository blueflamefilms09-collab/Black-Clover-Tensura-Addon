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
 * VFX wave 3: the Tensura-style Unique / Ultimate skills drawn after the anime they come from (docs/vfx_remake/skill_anime.md) instead of the generic {@code SkillFx} sprays, which stay under them as the
 * accent. Techniques of TimeMagicLayer (the quality standard) copied here, not its code: life ramps, eased opening (ease-out-back), hoops (ribbons on a circle), seeded per-effect randomness so every client
 * sees the same picture. Textures: {@code tools/gen_skill_anime_vfx_textures.py} -> {@code skillanime_*.png}; the weapons of the Gate and the screws of All Fiction are the shaded low-poly solids of
 * {@link SolidParts} (never a flat cut-out, docs/visual/flat_props.md). Powers carrying a mode are decoded by {@link SkillAnimeVfx#mode} (steps of 20).
 * <ul>
 *   <li>SKILL_GATE, the Gate of Babylon. Mode 0 (power = gates, 1..8): golden ripples open in the air behind {@code from} one after another and each fires a weapon at {@code to}; the first two are solid
 *       swords / axes, the rest golden streaks; a flash where each lands. Mode 1 (20 + radius): gates open over {@code from} (the sky) facing down and weapons rain on the ground round {@code to}, one great
 *       sword falls in the middle, a golden ring on the ground. Mode 2: Enkidu, four gates round {@code to} and golden chains shoot from them and coil round it. Mode 3: Ea, a gate at {@code from} and a
 *       crimson spiral of wind (swirl hoops round an ink core) drills to {@code to}.</li>
 *   <li>SKILL_FIREBOLT: from -> to in one tick (the anime's instant "Firebolt"): a jagged fire-lightning bolt, a white-hot core in an orange glow, re-rolled every 2 ticks, two forks, flame tongues licking
 *       along it, a burst of flames and a ring where it lands. power = width.</li>
 *   <li>SKILL_ARGONAUT. Mode 0 (power = charge, follows the caster): the chime, white motes of light spiral into the right hand, a bell of light rings there, a chime ring runs out. Mode 1 (20 + charge):
 *       the release, a great white crescent sweeps from -> to with sparkles and a flash. Mode 2 (40 + charge): the Hero's beam, a white-gold beam with bell rings running along it.</li>
 *   <li>SKILL_FROST, Wynn Fimbulvetr round from (power = radius): a snowflake frost circle opens on the ground, ice spikes erupt one after another, two blizzard bands spin, mist and snow drift.</li>
 *   <li>SKILL_HOLY_FIRE. Mode 0 (power = radius): Hestia's sacred hearth, a sigil on the ground, a pillar of golden flame, a ring of small flames, embers, a column of light. Mode 1 (20 + radius, follows):
 *       Rea Laevateinn, flame pillars erupt round from one after another inside a great ring.</li>
 *   <li>SKILL_ALLURE, Freya round from (power = radius): a silver glow, silver waves spreading, rose petals spiralling out, silver twinkles, a charm mark over her head.</li>
 *   <li>SKILL_SCREW, All Fiction. from -> to: one giant solid screw is driven into {@code to} and stays, a violet flash. from = to: power screws (3..6) slam down round from, a dark ring.</li>
 *   <li>SKILL_WINGS (no skill sends it since the Black Wings skill was removed; kept so the shape ordinals do not move), was Black Wings on the followed player's back (power = span): jagged ink-black spikes fan out from the shoulders and jitter, a dark glow behind them.</li>
 *   <li>SKILL_SIGIL: a smaller skill's signature at from, see {@link SkillAnimeSigils}.</li>
 * </ul>
 * <b>Budgets (rule 11).</b> The engine caps every activation at 100 quads (400 vertices) and this layer keeps the default; every arc, ring, hoop, beam loop and solid model checks the budget (a solid is
 * drawn whole or not at all), every loop is a constant or a {@code ctx.seg} (distance and particle setting), the worst case per shape measured by the preview is in the doc. Nothing is kept between
 * frames (short-lived pose vectors only, as every layer); a followed effect whose creature is gone draws nothing. With {@code skills.classicVfx} on the layer draws nothing (the old effects still come).
 */
public class SkillAnimeLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(SkillAnimeVfx.fileName(n)); }

    public static final ResourceLocation CORE = t("core");
    public static final ResourceLocation GLOW = t("glow");
    public static final ResourceLocation FLASH = t("flash");
    public static final ResourceLocation STREAK = t("streak");
    public static final ResourceLocation BOLT = t("bolt");
    public static final ResourceLocation RING = t("ring");
    public static final ResourceLocation FLAME = t("flame");
    public static final ResourceLocation RIPPLE = t("ripple");
    public static final ResourceLocation CHAIN = t("chain");
    public static final ResourceLocation BELL = t("bell");
    public static final ResourceLocation SNOWFLAKE = t("snowflake");
    public static final ResourceLocation SHARD = t("shard");
    public static final ResourceLocation MIST = t("mist");
    public static final ResourceLocation PETAL = t("petal");
    public static final ResourceLocation HALO = t("halo");
    public static final ResourceLocation RAYS = t("rays");
    public static final ResourceLocation SIGIL = t("sigil");
    public static final ResourceLocation SPARKLE = t("sparkle");
    public static final ResourceLocation WING = t("wing");
    public static final ResourceLocation SWIRL = t("swirl");

    static final int WHITE = 0xFFFFFFFF;
    static final int GOLD = 0xFFFFD060;
    /** Near-black ink (drawn with the alpha blend so it stays black on a bright sky). */
    static final int INK = 0xFF07060B;
    static final int SILVER = 0xFFE4E8F6;
    static final int ICE_DEEP = 0xFF7FC8F0;
    static final int CRIMSON = 0xFFD8202A;

    @Override
    public Set<VfxShape> shapes() {
        return EnumSet.of(VfxShape.SKILL_GATE, VfxShape.SKILL_FIREBOLT, VfxShape.SKILL_ARGONAUT, VfxShape.SKILL_FROST, VfxShape.SKILL_HOLY_FIRE, VfxShape.SKILL_ALLURE, VfxShape.SKILL_SCREW,
                VfxShape.SKILL_WINGS, VfxShape.SKILL_SIGIL);
    }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case SKILL_FIREBOLT -> 12;
            case SKILL_ARGONAUT -> 16;
            case SKILL_FROST -> 50;
            case SKILL_WINGS -> 42;
            default -> 30;
        };
    }

    @Override
    public int defaultColor(VfxShape s) { return GOLD; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (SkillAnimeVfx.classic()) return;
        float p = inst.power;
        switch (inst.shape) {
            case SKILL_GATE -> {
                int m = SkillAnimeVfx.mode(p, 4);
                if (m == SkillAnimeVfx.GATE_EA) VfxShake.add(inst.payload.from(), 1.4f, 10);
                else if (m == SkillAnimeVfx.GATE_RAIN) VfxShake.add(inst.payload.to(), 0.7f, 8);
            }
            case SKILL_ARGONAUT -> { if (SkillAnimeVfx.mode(p, 3) == SkillAnimeVfx.ARGO_BEAM) VfxShake.add(inst.payload.from(), 1.1f, 8); }
            case SKILL_HOLY_FIRE -> { if (SkillAnimeVfx.mode(p, 2) == SkillAnimeVfx.FIRE_FIELD) VfxShake.add(inst.payload.from(), 0.6f, 10); }
            default -> { }
        }
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        if (SkillAnimeVfx.classic()) return;
        if (inst.payload.followEntity() >= 0 && Float.isNaN(inst.followYaw(ctx))) return;        // the followed creature is gone (died, left): the effect ends with it
        switch (inst.shape) {
            case SKILL_GATE -> gate(inst, ctx, buf);
            case SKILL_FIREBOLT -> firebolt(inst, ctx, buf);
            case SKILL_ARGONAUT -> argonaut(inst, ctx, buf);
            case SKILL_FROST -> frost(inst, ctx, buf);
            case SKILL_HOLY_FIRE -> holyFire(inst, ctx, buf);
            case SKILL_ALLURE -> allure(inst, ctx, buf);
            case SKILL_SCREW -> screw(inst, ctx, buf);
            case SKILL_WINGS -> wings(inst, ctx, buf);
            case SKILL_SIGIL -> SkillAnimeSigils.draw(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ shared pieces (SkillAnimeSigils uses them too)
    /** 1 while running, ramping 0 -> 1 over the first {@code in} ticks and 1 -> 0 over the last {@code out}. */
    static float life(VfxInstance inst, float age, float in, float out) {
        float a = in <= 0 ? 1 : Mth.clamp(age / in, 0, 1);
        float b = out <= 0 ? 1 : Mth.clamp((inst.duration - age) / out, 0, 1);
        return Math.min(a, b);
    }

    static float ramp(float age, float start, float len) { return Mth.clamp((age - start) / len, 0, 1); }

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

    /** A horizontal unit vector square to {@code dir} (the "side" of a shot); x when dir is vertical. */
    static Vector3f sideOf(Vector3f dir) {
        Vector3f s = new Vector3f(dir).cross(0, 1, 0);
        return s.lengthSquared() < 1e-6f ? new Vector3f(1, 0, 0) : s.normalize();
    }

    /** The facing of a followed creature (its body yaw), else the horizontal direction away from the camera (so its back is seen). */
    static Vector3f forward(VfxInstance inst, VfxRenderContext ctx, Vector3f feet) {
        float yaw = inst.followYaw(ctx);
        if (Float.isNaN(yaw)) {
            Vector3f f = new Vector3f(feet.x, 0, feet.z);
            return f.lengthSquared() < 1e-6f ? new Vector3f(0, 0, 1) : f.normalize();
        }
        float rad = yaw * Mth.DEG_TO_RAD;
        return new Vector3f(-Mth.sin(rad), 0, Mth.cos(rad));
    }

    /** A plane at {@code c} turned to the camera (c is camera-relative, so -c points at the camera). */
    static VfxPose camPose(Vector3f c) { return VfxPose.facing(c, new Vector3f(c).negate()); }

    /** A figure standing upright at {@code feet}, turned to the camera round the vertical only (flames, spikes). 1 quad. */
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

    /** The flicker seed: the same for 2 ticks, then a new zigzag. */
    static long flicker(VfxInstance inst, float age, int k) { return inst.seed * 31L + k * 7919L + (long) (age * 0.5f); }

    /**
     * A jagged bolt a -> b: a coloured glow ribbon (alpha blend, so the fire stays saturated on a bright sky), then an additive white-hot core over the same zigzag (two passes so every glow piece lies
     * under every core piece). 2 quads per piece.
     */
    static void zigzag(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f a, Vector3f b, int pieces, float jitter, float width, long seed, int glowCol, float alpha) {
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
                if (pass == 0) buf.beam(ctx, BOLT, VfxBlend.ALPHA, prev, p, width * 5f, width * 5f, 1, 0, al(glowCol, 0.9f * alpha), al(glowCol, 0.9f * alpha));
                else buf.beam(ctx, BOLT, VfxBlend.ADD, prev, p, width * 1.6f, width * 1.6f, 1, 0, al(hot(glowCol, 0.85f), alpha), al(hot(glowCol, 0.85f), alpha));
                prev = p;
            }
        }
    }

    /** A golden ripple gate at {@code c} facing {@code facing}: the ripple in solid gold (alpha blend, so it stays gold on a bright sky), its shine and a glow. 3 quads. */
    static void portal(VfxVertexBuffer buf, Vector3f c, Vector3f facing, float size, float open, float spin, int col, float alpha) {
        if (open <= 0.01f || alpha <= 0.01f) return;
        VfxPose p = VfxPose.facing(c, facing).spin(spin);
        buf.plane(RIPPLE, VfxBlend.ALPHA, p, size * open, al(col, 0.95f * alpha));
        buf.plane(GLOW, VfxBlend.ADD, p, size * 1.2f * open, al(col, 0.3f * alpha));
        buf.plane(RIPPLE, VfxBlend.ADD, p.spin(0.6f), size * 0.97f * open, al(hot(col, 0.4f), 0.45f * alpha));
    }

    /** The yaw / tilt that turn a model's local +y onto {@code d} (SolidParts: rotateY(yaw) then rotateZ(tilt)). */
    static float yawOf(Vector3f d) { return (float) Math.atan2(d.z, -d.x); }

    static float tiltOf(Vector3f d) { return (float) Math.acos(Mth.clamp(d.y / Math.max(1e-6f, d.length()), -1f, 1f)); }

    /** A giant All Fiction screw: a flat head (local +y), a shaft with a thread ridge, a sharp tip (local -y). About 13 quads once the hidden faces are dropped. */
    static boolean screwModel(VfxVertexBuffer buf, Vector3f c, float yaw, float tilt, float scale, float alpha, int accent) {
        SolidParts.Model m = SolidParts.begin(c, yaw, tilt, scale, alpha);
        if (m.culled()) return false;
        int steel = SolidModels.tint(accent, 0.1f), ridge = SolidModels.tint(accent, 0.25f);
        m.box(-0.2f, 0.38f, -0.2f, 0.2f, 0.48f, 0.2f, SolidParts.STEEL, steel);
        m.box(-0.065f, -0.28f, -0.065f, 0.065f, 0.38f, 0.065f, SolidParts.STEEL, steel);
        m.box(-0.095f, 0.02f, -0.095f, 0.095f, 0.08f, 0.095f, SolidParts.SILVER, ridge);
        m.pyramid(0f, -0.28f, 0f, 0.065f, 0.065f, 0f, -0.5f, 0f, SolidParts.STEEL, steel);
        return m.draw(buf);
    }

    // ------------------------------------------------------------------ SKILL_GATE
    private void gate(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        int mode = SkillAnimeVfx.mode(inst.power, 4);
        float rest = SkillAnimeVfx.rest(inst.power, 4);
        switch (mode) {
            case SkillAnimeVfx.GATE_RAIN -> gateRain(inst, ctx, buf, rest);
            case SkillAnimeVfx.GATE_CHAINS -> enkidu(inst, ctx, buf);
            case SkillAnimeVfx.GATE_EA -> ea(inst, ctx, buf);
            default -> barrage(inst, ctx, buf, rest);
        }
    }

    /** Gates behind from, each firing one weapon at to. */
    private void barrage(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float count) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = dirOr(a, b, new Vector3f(0, 0, 1)), side = sideOf(dir);
        int n = Mth.clamp(Math.round(count), 1, 8);
        float fade = life(inst, age, 0, 6);
        RandomSource r = inst.random();
        for (int i = 0; i < n; i++) {
            float ox = (i - (n - 1) * 0.5f) * 1.5f + (r.nextFloat() - 0.5f) * 0.4f, oy = 0.4f + 1.6f * r.nextFloat() + Math.abs(ox) * 0.12f;
            Vector3f gate = new Vector3f(side).mul(ox).add(0, oy, 0).add(new Vector3f(dir).mul(-0.9f)).add(a);
            Vector3f aim = new Vector3f(b).add((r.nextFloat() - 0.5f) * 0.8f, (r.nextFloat() - 0.5f) * 0.8f, (r.nextFloat() - 0.5f) * 0.8f);
            Vector3f d = dirOr(gate, aim, dir);
            float openT = i * 0.8f, fireT = openT + 4f + r.nextFloat() * 2f, flyT = Mth.clamp(gate.distance(aim) / 9f, 2f, 5f);
            float open = VfxAnim.easeOutBack(ramp(age, openT, 4f));
            float closing = Mth.clamp(1 - (age - fireT - 3f) / 5f, 0, 1) * fade;
            portal(buf, gate, d, 0.65f, open, age * 0.05f + i, col, closing);
            float local = age - fireT;
            boolean solid = i < 2;
            Vector3f pos;
            float wAlpha = fade;
            if (local < 0) {
                float emerge = ramp(age, fireT - 3f, 3f);
                if (emerge <= 0) continue;
                pos = new Vector3f(d).mul(-0.55f + 0.45f * emerge).add(gate);
                wAlpha *= emerge;
            } else if (local < flyT) {
                float u = local / flyT;
                pos = lerp(gate, aim, u);
                Vector3f tail = lerp(gate, aim, Math.max(0, u - 0.4f));
                buf.beam(ctx, STREAK, VfxBlend.ADD, tail, pos, 0.1f, solid ? 0.7f : 0.45f, 1, 0, al(col, 0.15f * fade), al(hot(col, 0.3f), 0.8f * fade));
            } else {
                float land = local - flyT;
                pos = new Vector3f(d).mul(-0.25f).add(aim);
                if (land < 5) flash(ctx, buf, aim, 1.1f + 0.2f * land, land * 0.4f + i, col, 1 - land / 5f);
                wAlpha *= Mth.clamp(1 - (land - 6f) / 6f, 0, 1);
            }
            if (wAlpha <= 0.02f) continue;
            if (solid) {
                if (i == 0) SolidModels.sword(buf, pos, yawOf(d), tiltOf(d), 1.5f, wAlpha, col);
                else SolidModels.axe(buf, pos, yawOf(d), tiltOf(d), 1.3f, wAlpha, col);
            } else {
                Vector3f hilt = new Vector3f(d).mul(-1.4f).add(pos);                     // a golden blade of light: a solid gold core, a white-hot edge, a twinkle at the point
                buf.beam(ctx, BOLT, VfxBlend.ALPHA, hilt, pos, 0.3f, 0.12f, 1, 0, al(col, wAlpha), al(col, wAlpha));
                buf.beam(ctx, BOLT, VfxBlend.ADD, hilt, pos, 0.16f, 0.06f, 1, 0, al(hot(col, 0.6f), wAlpha), al(hot(col, 0.6f), wAlpha));
                buf.billboard(ctx, SPARKLE, VfxBlend.ADD, pos, 0.8f, age * 0.3f, al(hot(col, 0.5f), wAlpha));
            }
        }
    }

    /** Gates over from (the sky) facing down, weapons raining on the ground round to, one great sword in the middle. */
    private void gateRain(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float radius) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        Vector3f sky = ctx.rel(inst.from(ctx)), ground = ctx.rel(inst.to(ctx));
        float R = Mth.clamp(radius, 2f, 16f), fade = life(inst, age, 0, 6);
        Vector3f down = new Vector3f(0, -1, 0);
        RandomSource r = inst.random();
        int n = ctx.seg(9, 5);
        float span = Math.max(6f, inst.duration - 12f);
        for (int i = 0; i < n; i++) {
            float ang = i * 2.39996f + r.nextFloat() * 0.5f, rad = R * 0.85f * Mth.sqrt((i + 0.5f) / n);
            Vector3f gate = new Vector3f(Mth.cos(ang) * rad, (r.nextFloat() - 0.5f) * 1.2f, Mth.sin(ang) * rad).add(sky);
            Vector3f hit = new Vector3f(gate.x, ground.y, gate.z);
            float openT = i * 0.5f;
            portal(buf, gate, down, 1.3f, VfxAnim.easeOutBack(ramp(age, openT, 4f)), age * 0.04f + i, col, fade);
            for (int k = 0; k < 2; k++) {                                         // each gate fires twice
                float fireT = openT + 3f + k * span * 0.5f + r.nextFloat() * 3f, local = age - fireT, fall = 3f;
                if (local < 0 || local > fall + 5f) continue;
                if (local < fall) {
                    float u = local / fall;
                    Vector3f head = lerp(gate, hit, u), tail = lerp(gate, hit, Math.max(0, u - 0.45f));
                    buf.beam(ctx, STREAK, VfxBlend.ADD, tail, head, 0.15f, 0.9f, 1, 0, al(col, 0.2f * fade), al(hot(col, 0.3f), 0.85f * fade));
                    buf.beam(ctx, BOLT, VfxBlend.ALPHA, new Vector3f(head).add(0, 1.5f, 0), head, 0.34f, 0.14f, 1, 0, al(col, fade), al(col, fade));
                    buf.billboard(ctx, SPARKLE, VfxBlend.ADD, head, 1.0f, age * 0.3f, al(hot(col, 0.5f), fade));
                } else {
                    float land = local - fall;
                    flash(ctx, buf, new Vector3f(hit).add(0, 0.3f, 0), 1.2f + 0.2f * land, land * 0.3f + i, col, 1 - land / 5f);
                }
            }
        }
        // the great sword: it leaves the middle gate at tick 6 and stands in the ground until the end
        Vector3f top = new Vector3f(sky), stuck = new Vector3f(ground).add(0, 1.0f, 0);
        float u = ramp(age, 6f, 3f);
        if (age > 3f) {
            portal(buf, top, down, 2.0f, VfxAnim.easeOutBack(ramp(age, 2f, 4f)), -age * 0.03f, col, fade);
            Vector3f pos = lerp(new Vector3f(top).add(0, 0.6f, 0), stuck, VfxAnim.easeInCubic(u));
            SolidModels.sword(buf, pos, 0f, Mth.PI, 3.2f, fade, col);
            if (u >= 1f && age < 15f) flash(ctx, buf, new Vector3f(ground).add(0, 0.4f, 0), 3f, age * 0.2f, col, 1 - (age - 9f) / 6f);
        }
        float rr = R * (0.6f + 0.4f * VfxAnim.easeOutCubic(ramp(age, 0, 10f)));
        buf.ring(RING, VfxBlend.ADD, VfxPose.ground(new Vector3f(ground).add(0, 0.06f, 0)), rr * 0.9f, rr, ctx.seg(20, 10), 5, age * 0.02f, al(col, 0.7f * fade));
    }

    /** Enkidu: four gates round to, golden chains shoot from them and coil round it. */
    private void enkidu(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        Vector3f c = ctx.rel(inst.to(ctx));
        float fade = life(inst, age, 0, 6);
        RandomSource r = inst.random();
        float base = r.nextFloat() * Mth.TWO_PI;
        int links = ctx.seg(6, 3);
        for (int i = 0; i < 4; i++) {
            float ang = base + Mth.HALF_PI * i;
            Vector3f gate = new Vector3f(Mth.cos(ang) * 2.8f, (i % 2 == 0 ? 1.7f : 0.5f), Mth.sin(ang) * 2.8f).add(c);
            Vector3f d = dirOr(gate, c, new Vector3f(0, 0, 1));
            portal(buf, gate, d, 0.75f, VfxAnim.easeOutBack(ramp(age, i * 0.6f, 4f)), age * 0.05f, col, fade);
            float reach = VfxAnim.easeOutCubic(ramp(age, 2f + i * 0.6f, 3f));
            if (reach <= 0) continue;
            Vector3f end = lerp(gate, c, reach * 0.88f);
            Vector3f prev = new Vector3f(gate);
            for (int k = 1; k <= links; k++) {                                     // the chain in pieces of two links each, so the links keep their size
                Vector3f p = lerp(gate, end, k / (float) links);
                p.y -= Mth.sin(Mth.PI * k / links) * 0.25f * (1 - reach);
                buf.beam(ctx, CHAIN, VfxBlend.ALPHA, prev, p, 0.24f, 0.24f, 1, 0, al(col, fade), al(col, fade));
                prev = p;
            }
            buf.beam(ctx, STREAK, VfxBlend.ADD, gate, end, 0.5f, 0.5f, 1, 0, al(col, 0.25f * fade), al(col, 0.25f * fade));
        }
        float coil = VfxAnim.easeOutBack(ramp(age, 5f, 4f));
        if (coil > 0.01f) {                                                         // the coils round the bound creature
            int seg = ctx.seg(12, 6);
            for (int k = 0; k < 2; k++) {
                VfxPose p = VfxPose.facing(new Vector3f(c).add(0, -0.35f + 0.7f * k, 0), new Vector3f(0.15f * (k == 0 ? 1 : -1), 1f, 0.1f));
                buf.ring(RING, VfxBlend.ALPHA, p, 0.62f * coil, 0.8f * coil, seg, 3, age * 0.05f, al(col, fade));
            }
            if (age < 11f) flash(ctx, buf, c, 1.6f, age * 0.3f, col, 1 - (age - 5f) / 6f);
        }
    }

    /** Ea: a gate at from and a crimson spiral of wind round an ink core drilling to to. */
    private void ea(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = dirOr(a, b, new Vector3f(0, 0, 1));
        float len = a.distance(b), fade = life(inst, age, 0, 8);
        float extend = VfxAnim.easeOutCubic(ramp(age, 2f, 4f));
        a = new Vector3f(dir).mul(1.2f).add(a);                                          // the gate opens a step in front of the king
        len = Math.max(0.5f, len - 1.2f);
        portal(buf, a, dir, 1.4f, VfxAnim.easeOutBack(ramp(age, 0, 3f)), age * 0.08f, GOLD, fade);
        if (extend <= 0) return;
        Vector3f head = new Vector3f(dir).mul(len * extend).add(a);
        float w = 1f + 0.15f * Mth.sin(age * 0.8f);
        buf.beam(ctx, STREAK, VfxBlend.ADD, a, head, 4.6f * w, 4.2f * w, 1, 0, al(col, 0.7f * fade), al(col, 0.5f * fade));
        buf.beam(ctx, BOLT, VfxBlend.ALPHA, a, head, 1.6f, 1.3f, ctx.seg(5, 2), -age * 0.3f, al(INK, 0.92f * fade), al(INK, 0.92f * fade));
        int hoops = ctx.seg(6, 3), seg = ctx.seg(8, 5);
        for (int k = 0; k < hoops; k++) {
            float tk = (k + 0.5f) / hoops;
            if (tk > extend) break;
            Vector3f at = new Vector3f(dir).mul(len * tk).add(a);
            VfxPose p = VfxPose.facing(at, dir).spin(age * 0.45f + k * 1.3f);
            hoop(buf, SWIRL, VfxBlend.ALPHA, p, 1.3f + 0.3f * Mth.sin(age * 0.5f + k), 0.8f, seg, 2, -age * 0.18f, al(col, fade));
        }
        if (extend < 1f) buf.billboard(ctx, GLOW, VfxBlend.ADD, head, 3.5f, 0, al(col, fade));
        else flash(ctx, buf, b, 4f, age * 0.2f, col, Mth.clamp(1 - (age - 6f) / 10f, 0, 1));
    }

    // ------------------------------------------------------------------ SKILL_FIREBOLT
    private void firebolt(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        float w = Mth.clamp(inst.power, 0.4f, 3f), len = a.distance(b);
        float fade = life(inst, age, 0, 6);
        int pieces = ctx.seg(10, 5);
        float jitter = Math.min(1.1f, 0.05f * len + 0.25f);
        zigzag(ctx, buf, a, b, pieces, jitter, 0.1f * w, flicker(inst, age, 0), col, fade);
        RandomSource fr = RandomSource.create(flicker(inst, age, 1));                // two forks, re-rolled with the flicker
        for (int k = 0; k < 2; k++) {
            Vector3f s = lerp(a, b, 0.3f + 0.5f * fr.nextFloat());
            Vector3f e = randomDir(fr).mul(1.2f + 1.4f * fr.nextFloat()).add(s);
            zigzag(ctx, buf, s, e, 3, 0.35f, 0.04f * w, flicker(inst, age, 2 + k), col, 0.8f * fade);
        }
        RandomSource r = inst.random();
        int tongues = ctx.seg(6, 3);                                              // flame tongues licking along the bolt
        for (int i = 0; i < tongues; i++) {
            float ph = Mth.frac(age * 0.12f + r.nextFloat()), along = r.nextFloat();
            float h = (0.5f + 0.5f * ph) * w;
            upright(buf, FLAME, VfxBlend.ADD, lerp(a, b, along).add(0, -0.15f, 0), h * 0.6f, h, al(col, Mth.sin(ph * Mth.PI) * 0.8f * fade));
        }
        buf.billboard(ctx, GLOW, VfxBlend.ADD, a, 1.2f * w, 0, al(col, 0.7f * fade));
        // the strike: a burst of flame and a ring facing back along the bolt
        Vector3f dir = dirOr(a, b, new Vector3f(0, 0, 1));
        flash(ctx, buf, b, 2.2f * w, age * 0.3f, col, Mth.clamp(1 - age / 7f, 0, 1));
        float rr = w * (0.6f + 2f * VfxAnim.easeOutCubic(ramp(age, 0, 7f)));
        buf.ring(RING, VfxBlend.ADD, VfxPose.facing(b, new Vector3f(dir).negate()), rr * 0.7f, rr, ctx.seg(14, 8), 3, 0, al(col, fade * (1 - ramp(age, 2f, 8f))));
        int burst = ctx.seg(5, 3);
        for (int i = 0; i < burst; i++) {
            float ang = Mth.TWO_PI * i / burst + r.nextFloat(), h = (0.8f + 0.6f * r.nextFloat()) * w * (1 - ramp(age, 3f, 8f));
            if (h <= 0.05f) continue;
            upright(buf, FLAME, VfxBlend.ADD, new Vector3f(Mth.cos(ang) * 0.4f * w, -0.4f * w, Mth.sin(ang) * 0.4f * w).add(b), h * 0.7f, h, al(col, 0.9f * fade));
        }
    }

    // ------------------------------------------------------------------ SKILL_ARGONAUT
    private void argonaut(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        int mode = SkillAnimeVfx.mode(inst.power, 3);
        float charge = Mth.clamp(SkillAnimeVfx.rest(inst.power, 3), 1f, 12f);
        switch (mode) {
            case SkillAnimeVfx.ARGO_STRIKE -> argoStrike(inst, ctx, buf, charge);
            case SkillAnimeVfx.ARGO_BEAM -> argoBeam(inst, ctx, buf, charge);
            default -> chime(inst, ctx, buf, charge);
        }
    }

    /** The chime: motes of light spiral into the right hand and a bell of light rings there. */
    private void chime(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float c) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        Vector3f feet = ctx.rel(inst.from(ctx));
        Vector3f fwd = forward(inst, ctx, feet), right = new Vector3f(-fwd.z, 0, fwd.x);
        Vector3f hand = new Vector3f(feet).add(0, 1.05f, 0).add(new Vector3f(right).mul(0.38f)).add(new Vector3f(fwd).mul(0.25f));
        float fade = life(inst, age, 0, 5), k = c / 10f;
        RandomSource r = inst.random();
        int motes = ctx.seg(10, 5);
        for (int i = 0; i < motes; i++) {
            float ph = ramp(age, i * 0.6f, 7f);
            if (ph <= 0 || ph >= 1) continue;
            float e = VfxAnim.easeInCubic(ph), ang = r.nextFloat() * Mth.TWO_PI + ph * 4f, rad = Mth.lerp(e, 1.4f + 0.4f * k, 0.05f);
            Vector3f at = new Vector3f(Mth.cos(ang) * rad, (r.nextFloat() - 0.3f) * 1.4f * (1 - e), Mth.sin(ang) * rad).add(hand);
            buf.billboard(ctx, CORE, VfxBlend.ADD, at, 0.14f + 0.04f * k, 0, al(WHITE, Mth.sin(ph * Mth.PI) * fade));
        }
        float ring = VfxAnim.pulse(age, 1.5f);
        buf.billboard(ctx, GLOW, VfxBlend.ADD, hand, (0.9f + 0.9f * k) * (0.85f + 0.15f * ring), 0, al(col, 0.7f * fade));
        buf.billboard(ctx, BELL, VfxBlend.ADD, new Vector3f(hand).add(0, 0.45f + 0.1f * k, 0), 0.5f + 0.25f * k, 0.15f * Mth.sin(age * 0.9f), al(hot(col, 0.6f), fade * (age < 5 ? 1f : 0.75f)));
        buf.billboard(ctx, SPARKLE, VfxBlend.ADD, hand, 0.7f + 0.4f * k, age * 0.08f, al(WHITE, fade));
        float rr = 0.3f + (1.2f + 0.8f * k) * VfxAnim.easeOutCubic(ramp(age, 0, 10f));
        buf.ring(RING, VfxBlend.ADD, camPose(hand), rr * 0.8f, rr, ctx.seg(14, 8), 3, 0, al(col, fade * (1 - ramp(age, 2f, 12f))));
        buf.ring(RING, VfxBlend.ADD, VfxPose.ground(new Vector3f(feet).add(0, 0.05f, 0)), 0.6f + 0.15f * c * 0.5f, 0.8f + 0.15f * c * 0.5f, ctx.seg(12, 6), 3, age * 0.03f, al(col, 0.45f * fade));
    }

    /** The release: a great white crescent sweeps from -> to. */
    private void argoStrike(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float c) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = dirOr(a, b, new Vector3f(0, 0, 1));
        float reach = Math.max(1.5f, a.distance(b)), fade = life(inst, age, 0, 6), k = c / 10f;
        VfxPose plane = VfxPose.facing(new Vector3f(a).add(0, -0.45f, 0), new Vector3f(0, 1, 0).add(new Vector3f(dir).mul(-0.25f)));
        float mid = (float) Math.atan2(dir.dot(plane.up()), dir.dot(plane.right()));
        float sweep = 2.6f * VfxAnim.easeOutCubic(ramp(age, 0, 3f)), rad = reach * 0.62f, width = 1.0f + 0.9f * k;
        int segs = ctx.seg(12, 6);
        buf.arc(RING, VfxBlend.ALPHA, plane, rad, width * 1.6f, mid + 1.3f - sweep, sweep, segs, al(col, 0.85f * fade));
        buf.arc(RING, VfxBlend.ADD, plane, rad, width, mid + 1.3f - sweep, sweep, segs, al(hot(col, 0.7f), fade));
        RandomSource r = inst.random();
        int sparks = ctx.seg(6, 3);
        for (int i = 0; i < sparks; i++) {
            float ang = mid + 1.3f - sweep * r.nextFloat(), ph = ramp(age, 2f, 10f), d = rad + (r.nextFloat() - 0.5f) * width + ph * 1.2f;
            Vector3f at = plane.point(Mth.cos(ang) * d, Mth.sin(ang) * d).add(0, ph * 0.8f, 0);
            buf.billboard(ctx, SPARKLE, VfxBlend.ADD, at, 0.5f + 0.3f * k, ang + age * 0.2f, al(WHITE, fade * (1 - ph)));
        }
        Vector3f tip = new Vector3f(dir).mul(rad).add(a);
        flash(ctx, buf, tip, 1.6f + 1.4f * k, age * 0.3f, col, 1 - ramp(age, 2f, 6f));
        if (age < 8f) buf.billboard(ctx, BELL, VfxBlend.ADD, new Vector3f(a).add(new Vector3f(dir).mul(0.8f)), 0.7f, 0, al(hot(col, 0.6f), 1 - age / 8f));
    }

    /** The Hero's beam: a white-gold beam with bell rings running along it. */
    private void argoBeam(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float c) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = dirOr(a, b, new Vector3f(0, 0, 1));
        float len = a.distance(b), fade = life(inst, age, 0, 6), k = c / 10f;
        float extend = VfxAnim.easeOutCubic(ramp(age, 0, 3f)), W = (0.6f + 0.8f * k) * (0.5f + 0.5f * fade);
        Vector3f head = new Vector3f(dir).mul(len * extend).add(a);
        if (len * extend > 0.05f) {
            buf.beam(ctx, STREAK, VfxBlend.ADD, a, head, W * 4f, W * 4f, 1, 0, al(col, 0.55f * fade), al(col, 0.45f * fade));
            buf.beam(ctx, BOLT, VfxBlend.ADD, a, head, W * 2.2f, W * 1.8f, ctx.seg(6, 3), -age * 0.4f, al(WHITE, fade), al(WHITE, fade));
        }
        int rings = ctx.seg(5, 3), seg = ctx.seg(12, 6);
        for (int i = 0; i < rings; i++) {
            float ph = Mth.frac(age * 0.07f + i / (float) rings);
            if (ph > extend) continue;
            Vector3f at = new Vector3f(dir).mul(len * ph).add(a);
            float rr = W * (1.4f + 0.6f * ph);
            buf.ring(RING, VfxBlend.ADD, VfxPose.facing(at, dir), rr * 0.75f, rr, seg, 3, 0, al(hot(col, 0.4f), fade * Mth.sin(ph * Mth.PI)));
        }
        RandomSource r = inst.random();
        int sparks = ctx.seg(6, 3);
        for (int i = 0; i < sparks; i++) {
            float along = Mth.frac(r.nextFloat() + age * 0.05f) * extend;
            Vector3f at = new Vector3f(dir).mul(len * along).add(a).add(randomDir(r).mul(W * 1.6f));
            buf.billboard(ctx, SPARKLE, VfxBlend.ADD, at, 0.6f, age * 0.2f + i, al(WHITE, fade));
        }
        buf.billboard(ctx, BELL, VfxBlend.ADD, new Vector3f(a).add(0, 0.6f, 0), 0.8f + 0.4f * k, 0, al(hot(col, 0.6f), fade));
        if (extend >= 1f) flash(ctx, buf, b, W * 4f, age * 0.2f, col, Mth.clamp(1 - (age - 3f) / 9f, 0, 1));
        else buf.billboard(ctx, GLOW, VfxBlend.ADD, head, W * 5f, 0, al(WHITE, fade));
    }

    // ------------------------------------------------------------------ SKILL_FROST
    private void frost(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        Vector3f c = ctx.rel(inst.from(ctx));
        float R = Mth.clamp(inst.power, 2f, 14f), fade = life(inst, age, 2, 10);
        float open = VfxAnim.easeOutBack(ramp(age, 0, 6f));
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, 0.06f, 0));
        buf.plane(GLOW, VfxBlend.ADD, ground, R * 1.1f * open, al(col, 0.35f * fade));
        buf.plane(SNOWFLAKE, VfxBlend.ADD, ground.spin(age * 0.012f), R * open, al(hot(col, 0.3f), 0.9f * fade));
        buf.ring(RING, VfxBlend.ADD, ground, R * 0.95f * open, R * 1.05f * open, ctx.seg(20, 10), 6, age * 0.01f, al(col, 0.6f * fade));
        RandomSource r = inst.random();
        int spikes = ctx.seg(12, 6);
        for (int i = 0; i < spikes; i++) {                                            // ice spikes erupt one after another
            float ang = r.nextFloat() * Mth.TWO_PI, rad = R * (0.15f + 0.75f * Mth.sqrt(r.nextFloat())), delay = 3f + 10f * r.nextFloat();
            float h = (1.6f + 2.0f * r.nextFloat()) * (1.15f - 0.4f * rad / R), lean = 0.15f + 0.25f * r.nextFloat();
            float grow = VfxAnim.easeOutBack(ramp(age, delay, 3f));
            if (grow <= 0.01f) continue;
            Vector3f base = new Vector3f(Mth.cos(ang) * rad, 0, Mth.sin(ang) * rad).add(c);
            Vector3f tip = new Vector3f(Mth.cos(ang) * lean, 1f, Mth.sin(ang) * lean).normalize().mul(h * grow).add(base);
            buf.beam(ctx, SHARD, VfxBlend.ALPHA, base, tip, 0.55f * h, 0.55f * h, 1, 0, al(hot(ICE_DEEP, 0.35f), 0.92f * fade), al(hot(ICE_DEEP, 0.35f), 0.92f * fade));
            if (age - delay < 4f) buf.billboard(ctx, SPARKLE, VfxBlend.ADD, tip, 0.9f, ang, al(WHITE, (1 - (age - delay) / 4f) * fade));
        }
        int seg = ctx.seg(12, 6);                                                     // the blizzard: two bands of wind spinning round the circle
        for (int k = 0; k < 2; k++) {
            VfxPose p = VfxPose.ground(new Vector3f(c).add(0, 1.0f + 1.3f * k, 0)).spin(age * (k == 0 ? 0.09f : -0.07f));
            hoop(buf, SWIRL, VfxBlend.ADD, p, R * (0.55f + 0.3f * k) * open, 0.7f, seg, 3, age * 0.02f, al(WHITE, 0.55f * fade));
        }
        int mists = ctx.seg(6, 3);
        for (int i = 0; i < mists; i++) {
            float ph = Mth.frac(age * 0.02f + r.nextFloat()), ang = r.nextFloat() * Mth.TWO_PI + age * 0.03f, rad = R * (0.2f + 0.7f * r.nextFloat());
            buf.billboard(ctx, MIST, VfxBlend.ALPHA, new Vector3f(Mth.cos(ang) * rad, 0.5f + ph * 1.5f, Mth.sin(ang) * rad).add(c), 2.4f + 1.5f * ph, ang, al(WHITE, 0.35f * Mth.sin(ph * Mth.PI) * fade));
        }
        int snow = ctx.seg(10, 5);
        for (int i = 0; i < snow; i++) {
            float ph = Mth.frac(age * 0.04f + r.nextFloat()), ang = r.nextFloat() * Mth.TWO_PI + age * 0.08f, rad = R * Mth.sqrt(r.nextFloat());
            buf.billboard(ctx, CORE, VfxBlend.ADD, new Vector3f(Mth.cos(ang) * rad, 3.5f * (1 - ph), Mth.sin(ang) * rad).add(c), 0.16f, 0, al(WHITE, fade));
        }
    }

    // ------------------------------------------------------------------ SKILL_HOLY_FIRE
    private void holyFire(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        if (SkillAnimeVfx.mode(inst.power, 2) == SkillAnimeVfx.FIRE_FIELD) { flameField(inst, ctx, buf, SkillAnimeVfx.rest(inst.power, 2)); return; }
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        Vector3f c = ctx.rel(inst.from(ctx));
        float R = Mth.clamp(inst.power, 1.5f, 16f), fade = life(inst, age, 2, 8);
        float open = VfxAnim.easeOutBack(ramp(age, 0, 5f)), sig = Math.min(R * 0.6f, 6f);
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, 0.06f, 0));
        buf.plane(SIGIL, VfxBlend.ADD, ground.spin(age * 0.015f), sig * open, al(hot(col, 0.2f), 0.85f * fade));
        float rise = VfxAnim.easeOutBack(ramp(age, 1f, 5f));
        RandomSource r = inst.random();
        int core = ctx.seg(5, 3);                                                     // the pillar of sacred flame
        for (int i = 0; i < core; i++) {
            float sway = Mth.sin(age * 0.4f + i * 1.9f) * 0.18f, h = (2.4f + 1.4f * r.nextFloat() + 0.25f * Mth.sin(age * 0.6f + i)) * rise * fade;
            Vector3f at = new Vector3f(sway, 0, (r.nextFloat() - 0.5f) * 0.4f).add(c);
            upright(buf, FLAME, VfxBlend.ADD, at, h * 0.5f, h, al(col, 0.85f * fade));
            upright(buf, FLAME, VfxBlend.ADD, at, h * 0.25f, h * 0.7f, al(hot(col, 0.75f), fade));
        }
        buf.beam(ctx, STREAK, VfxBlend.ADD, c, new Vector3f(c).add(0, 7f * rise, 0), 1.8f, 1.2f, 1, 0, al(hot(col, 0.4f), 0.55f * fade), al(col, 0.1f * fade));
        int rim = ctx.seg(12, 6);                                                     // the ring of small flames round the sigil
        for (int i = 0; i < rim; i++) {
            float ang = Mth.TWO_PI * i / rim + age * 0.02f, h = (0.7f + 0.25f * Mth.sin(age * 0.5f + i * 1.7f)) * open * fade;
            upright(buf, FLAME, VfxBlend.ADD, new Vector3f(Mth.cos(ang) * sig * 0.9f, 0, Mth.sin(ang) * sig * 0.9f).add(c), h * 0.6f, h, al(col, 0.9f * fade));
        }
        int embers = ctx.seg(10, 5);
        for (int i = 0; i < embers; i++) {
            float ang = r.nextFloat() * Mth.TWO_PI, rad = R * 0.7f * Mth.sqrt(r.nextFloat()), ph = Mth.frac(age * 0.04f + r.nextFloat());
            buf.billboard(ctx, CORE, VfxBlend.ADD, new Vector3f(Mth.cos(ang) * rad, ph * 4f, Mth.sin(ang) * rad).add(c), 0.16f, 0, al(hot(col, 0.5f), Mth.sin(ph * Mth.PI) * fade));
        }
        buf.billboard(ctx, GLOW, VfxBlend.ADD, new Vector3f(c).add(0, 1.2f, 0), 3.2f * rise, 0, al(col, 0.5f * fade));
        float rr = 0.5f + R * VfxAnim.easeOutCubic(ramp(age, 0, 12f));
        buf.ring(RING, VfxBlend.ADD, ground, rr * 0.88f, rr, ctx.seg(16, 8), 4, 0, al(col, fade * (1 - ramp(age, 4f, 14f))));
    }

    /** Rea Laevateinn: flame pillars erupt round from one after another inside a great ring. */
    private void flameField(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float radius) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        Vector3f c = ctx.rel(inst.from(ctx));
        float R = Mth.clamp(radius, 3f, 20f), fade = life(inst, age, 2, 8);
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, 0.06f, 0));
        float open = VfxAnim.easeOutBack(ramp(age, 0, 6f));
        buf.plane(SIGIL, VfxBlend.ADD, ground.spin(-age * 0.01f), R * open, al(col, 0.3f * fade));
        buf.ring(RING, VfxBlend.ADD, ground, R * 0.96f * open, R * 1.04f * open, ctx.seg(20, 10), 6, age * 0.02f, al(col, 0.75f * fade));
        RandomSource r = inst.random();
        int n = ctx.seg(12, 6);
        float span = Math.max(4f, inst.duration - 18f), life = 14f;
        for (int i = 0; i < n; i++) {
            float ang = r.nextFloat() * Mth.TWO_PI, rad = 1.5f + (R - 1.5f) * Mth.sqrt(r.nextFloat()), start = 3f + span * i / n + r.nextFloat() * 2f;
            float local = age - start;
            if (local < 0 || local > life) continue;
            float erupt = VfxAnim.easeOutBack(Mth.clamp(local / 3f, 0, 1)), f = Mth.clamp((life - local) / 5f, 0, 1);
            float h = (6f + 3f * r.nextFloat()) * erupt;
            Vector3f at = new Vector3f(Mth.cos(ang) * rad, 0, Mth.sin(ang) * rad).add(c);
            upright(buf, FLAME, VfxBlend.ALPHA, at, h * 0.5f, h, al(col, 0.85f * f));
            upright(buf, FLAME, VfxBlend.ADD, at, h * 0.2f, h * 0.7f, al(hot(col, 0.7f), f));
            if (local < 4f) flash(ctx, buf, new Vector3f(at).add(0, 0.3f, 0), 1.6f, ang, col, 1 - local / 4f);
        }
        int embers = ctx.seg(8, 4);
        for (int i = 0; i < embers; i++) {
            float ang = r.nextFloat() * Mth.TWO_PI, rad = R * Mth.sqrt(r.nextFloat()), ph = Mth.frac(age * 0.05f + r.nextFloat());
            buf.billboard(ctx, CORE, VfxBlend.ADD, new Vector3f(Mth.cos(ang) * rad, ph * 5f, Mth.sin(ang) * rad).add(c), 0.2f, 0, al(hot(col, 0.4f), Mth.sin(ph * Mth.PI) * fade));
        }
    }

    // ------------------------------------------------------------------ SKILL_ALLURE
    private void allure(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        Vector3f feet = ctx.rel(inst.from(ctx)), chest = new Vector3f(feet).add(0, 1.1f, 0);
        float R = Mth.clamp(inst.power, 2f, 16f), fade = life(inst, age, 2, 8);
        buf.billboard(ctx, GLOW, VfxBlend.ADD, chest, 3.4f * (0.9f + 0.1f * VfxAnim.pulse(age, 1.2f)), 0, al(SILVER, 0.5f * fade));
        int seg = ctx.seg(14, 8);
        for (int k = 0; k < 2; k++) {                                                 // the silver waves of her charm
            float ph = ramp(age, k * 7f, 14f);
            if (ph <= 0 || ph >= 1) continue;
            float rr = 0.6f + R * VfxAnim.easeOutCubic(ph);
            buf.ring(RING, VfxBlend.ADD, VfxPose.ground(new Vector3f(feet).add(0, 0.9f, 0)), rr * 0.86f, rr, seg, 4, age * 0.02f, al(SILVER, 0.8f * (1 - ph) * fade));
        }
        buf.plane(HALO, VfxBlend.ADD, VfxPose.ground(new Vector3f(feet).add(0, 0.05f, 0)), Math.min(R, 8f) * VfxAnim.easeOutBack(ramp(age, 0, 6f)), al(SILVER, 0.35f * fade));
        RandomSource r = inst.random();
        int petals = ctx.seg(18, 8);
        for (int i = 0; i < petals; i++) {                                           // rose petals spiral out
            float ph = Mth.frac(age * 0.035f + r.nextFloat()), ang = r.nextFloat() * Mth.TWO_PI + ph * 2.2f, rad = 0.4f + R * 0.75f * ph;
            Vector3f at = new Vector3f(Mth.cos(ang) * rad, 0.4f + 1.6f * ph + 0.3f * Mth.sin(age * 0.2f + i), Mth.sin(ang) * rad).add(feet);
            buf.billboard(ctx, PETAL, VfxBlend.ALPHA, at, 0.55f, ang * 2f + age * 0.1f, al(col, Mth.sin(ph * Mth.PI) * fade));
        }
        int twinkles = ctx.seg(10, 5);
        for (int i = 0; i < twinkles; i++) {
            float ang = r.nextFloat() * Mth.TWO_PI, rad = R * 0.8f * Mth.sqrt(r.nextFloat()), tw = Mth.sin(age * 0.5f + i * 2.3f);
            if (tw <= 0) continue;
            buf.billboard(ctx, SPARKLE, VfxBlend.ADD, new Vector3f(Mth.cos(ang) * rad, 0.5f + 1.7f * r.nextFloat(), Mth.sin(ang) * rad).add(feet), 0.5f * tw + 0.1f, 0.3f, al(SILVER, tw * fade));
        }
        float mark = VfxAnim.easeOutBack(ramp(age, 1f, 5f));                           // the charm mark over her head
        buf.billboard(ctx, PETAL, VfxBlend.ADD, new Vector3f(feet).add(0, 2.4f, 0), 0.6f * mark, Mth.PI, al(hot(col, 0.3f), fade * (0.7f + 0.3f * VfxAnim.pulse(age, 2f))));
    }

    // ------------------------------------------------------------------ SKILL_SCREW
    private void screw(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        float fade = life(inst, age, 0, 6);
        if (a.distanceSquared(b) > 1f) {                                              // one screw driven into the target
            Vector3f dir = dirOr(a, b, new Vector3f(0, 0, 1)), back = new Vector3f(dir).negate();
            float flyT = Mth.clamp(a.distance(b) / 10f, 2f, 5f), u = VfxAnim.easeInCubic(ramp(age, 0, flyT));
            Vector3f stuck = new Vector3f(dir).mul(-0.55f).add(b);
            Vector3f pos = lerp(new Vector3f(dir).mul(0.8f).add(a), stuck, u);
            if (u < 1f) buf.beam(ctx, STREAK, VfxBlend.ADD, lerp(a, pos, 0.4f), pos, 0.1f, 0.8f, 1, 0, al(col, 0.1f * fade), al(hot(col, 0.3f), 0.6f * fade));
            buf.billboard(ctx, GLOW, VfxBlend.ALPHA, b, 1.8f, 0, al(INK, 0.35f * fade * u));
            screwModel(buf, pos, yawOf(back), tiltOf(back), 2.6f, fade, col);
            float land = age - flyT;
            if (land >= 0) {
                flash(ctx, buf, b, 1.8f, land * 0.3f, col, 1 - land / 6f);
                float rr = 0.4f + 1.4f * VfxAnim.easeOutCubic(Mth.clamp(land / 8f, 0, 1));
                buf.ring(RING, VfxBlend.ADD, VfxPose.facing(b, back), rr * 0.75f, rr, ctx.seg(12, 6), 3, 0, al(col, fade * Mth.clamp(1 - land / 10f, 0, 1)));
            }
            return;
        }
        int n = Mth.clamp(Math.round(inst.power), 3, 5);                               // screws slam down round the caster (5 solids and the rings stay under the 100-quad cap)
        RandomSource r = inst.random();
        float base = r.nextFloat() * Mth.TWO_PI;
        Vector3f ground = new Vector3f(a).add(0, 0.05f, 0);
        buf.ring(RING, VfxBlend.ALPHA, VfxPose.ground(ground), 1.3f, 2.3f, ctx.seg(12, 6), 4, age * 0.02f, al(INK, 0.6f * fade));
        buf.ring(RING, VfxBlend.ADD, VfxPose.ground(new Vector3f(ground).add(0, 0.02f, 0)), 1.9f, 2.25f, ctx.seg(12, 6), 4, -age * 0.02f, al(col, 0.8f * fade));
        for (int i = 0; i < n; i++) {
            float ang = base + Mth.TWO_PI * i / n, drop = i * 1.5f, u = VfxAnim.easeInCubic(ramp(age, drop, 3f));
            if (age < drop) continue;
            float lean = 0.18f;
            Vector3f out = new Vector3f(Mth.cos(ang), 0, Mth.sin(ang));
            Vector3f axis = new Vector3f(out).mul(lean).add(0, 1f, 0).normalize();          // local +y: the head up, the screw leaning out a little
            Vector3f land = new Vector3f(out).mul(1.8f).add(ground).add(new Vector3f(axis).mul(0.5f * 1.6f));
            Vector3f pos = new Vector3f(land).add(0, 4.5f * (1 - u), 0);
            screwModel(buf, pos, yawOf(axis), tiltOf(axis), 1.6f, fade, col);
            float hit = age - drop - 3f;
            if (hit >= 0 && hit < 5f) flash(ctx, buf, new Vector3f(out).mul(1.8f).add(ground).add(0, 0.2f, 0), 1.2f, ang, col, 1 - hit / 5f);
        }
        buf.billboard(ctx, GLOW, VfxBlend.ADD, new Vector3f(a).add(0, 1f, 0), 3f, 0, al(col, 0.35f * fade));
    }

    // ------------------------------------------------------------------ SKILL_WINGS
    private void wings(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color;
        Vector3f feet = ctx.rel(inst.from(ctx));
        Vector3f fwd = forward(inst, ctx, feet), right = new Vector3f(-fwd.z, 0, fwd.x);
        Vector3f back = new Vector3f(feet).add(0, 1.35f, 0).sub(new Vector3f(fwd).mul(0.22f));
        float S = Mth.clamp(inst.power, 1f, 4f), fade = life(inst, age, 2, 2);
        RandomSource jr = RandomSource.create(flicker(inst, age, 3));
        int spikes = ctx.seg(5, 3);
        for (int s = -1; s <= 1; s += 2) {
            Vector3f root = new Vector3f(right).mul(0.12f * s).add(back);
            for (int k = 0; k < spikes; k++) {
                float phi = -0.4f + 1.4f * k / Math.max(1, spikes - 1);
                Vector3f d = new Vector3f(fwd).mul(-0.45f).add(new Vector3f(right).mul(s * Mth.cos(phi))).add(0, Mth.sin(phi), 0);
                d.add((jr.nextFloat() - 0.5f) * 0.12f, (jr.nextFloat() - 0.5f) * 0.12f, (jr.nextFloat() - 0.5f) * 0.12f).normalize();
                float len = S * (0.75f + 0.3f * Mth.sin(k * 1.7f + 0.4f)) * (1f + 0.06f * Mth.sin(age * 0.6f + k));
                Vector3f tip = new Vector3f(d).mul(len).add(root);
                buf.beam(ctx, STREAK, VfxBlend.ADD, root, tip, 0.9f, 0.25f, 1, 0, al(col, 0.3f * fade), al(col, 0.05f * fade));
                buf.beam(ctx, WING, VfxBlend.ALPHA, root, tip, 0.5f, 0.5f, 1, 0, al(INK, 0.96f * fade), al(INK, 0.96f * fade));
            }
        }
        RandomSource r = inst.random();
        int flakes = ctx.seg(4, 2);
        for (int i = 0; i < flakes; i++) {
            float ph = Mth.frac(age * 0.05f + r.nextFloat()), s = r.nextFloat() < 0.5f ? -1 : 1;
            Vector3f at = new Vector3f(right).mul(s * S * (0.5f + 0.4f * r.nextFloat())).add(back).add(new Vector3f(fwd).mul(-0.4f)).add(0, -ph * 1.2f, 0);
            buf.billboard(ctx, CORE, VfxBlend.ALPHA, at, 0.14f, 0, al(INK, Mth.sin(ph * Mth.PI) * fade));
        }
    }
}
