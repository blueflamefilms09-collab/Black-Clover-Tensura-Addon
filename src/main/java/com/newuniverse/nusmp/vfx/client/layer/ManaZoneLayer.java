package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * The Mana Zone and the Grand Magic Zones, drawn at the Time Magic bar. Nothing here keeps state: every shape is a function of the ticks since
 * the zone's start tick (the payload carries the ticks already elapsed, so a refresh never restarts the growth), the zone's seed and the
 * camera, and the geometry comes from the cached templates of {@link ManaZoneGeometry} (an instanced shell with three levels of detail by
 * camera distance; no allocation per frame in this class: all scratch vectors are static, and the render thread is the only caller).
 * <ul>
 *   <li>MANA_ZONE_SHELL: the sphere of ambient mana round the caster, a diamond lattice that brightens towards the silhouette (a Fresnel
 *       term per quad), breathing slowly; it grows from the start tick to the radius cap (power). 96 quads at full detail.</li>
 *   <li>MANA_ZONE_GROUND: the plane of mana at the caster's feet: the floor disc, two counter-rotating rune rings, the growth front and the
 *       slow outward pulse, the hexagonal foothold under the feet (the invisible steps of the Mana Zone), rising motes in the colour of the
 *       caster's magic and streaks of mana flowing in from the edge. At most 344 vertices.</li>
 *   <li>MANA_ZONE_BURST: the opening pulse (to.x 0) and the collapse finisher (to.x 1: the front runs back in, sparks fall to the caster, a flash).</li>
 *   <li>MANA_ZONE_BUBBLE: a pressurised protective space: a small shell, a ring on the floor and motes.</li>
 *   <li>GRAND_ZONE_ENV: the scenery of a Grand Magic Zone round the viewer (mana veins in the ground, a mana storm's distant bolts, then by
 *       theme: seabed currents and bubbles, witch-glow spores, heat haze and embers, floating boulders and gravity ripples, a haven's slow motes).</li>
 * </ul>
 * Every instance stays under the vertex buffer's cap of 400 vertices; when the budget runs out the buffer drops the remaining quads.
 */
public class ManaZoneLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    static final ResourceLocation SHELL = t("mana_zone_shell"), RIM = t("mana_zone_rim"), RING = t("mana_zone_ring"), FLOOR = t("mana_zone_floor"), PAD = t("mana_zone_pad"),
            FLOW = t("mana_zone_flow"), MOTE = t("mana_zone_mote"), BURST = t("mana_zone_burst"),
            BOULDER = t("grand_zone_boulder"), SPORE = t("grand_zone_spore"), HAZE = t("grand_zone_haze"), CURRENT = t("grand_zone_current"),
            BOLT = t("grand_zone_bolt"), VEIN = t("grand_zone_vein"), HALO = t("grand_zone_halo");

    private static final int WHITE = 0xFFFFFFFF;
    /** The ring texture is 512 x 64: one repeat covers 8 band widths. */
    private static final float RING_ASPECT = 8f;
    private static final float TWO_PI = Mth.TWO_PI;

    // scratch (render thread only)
    private static final Vector3f A = new Vector3f(), B = new Vector3f(), C = new Vector3f(), D = new Vector3f();

    @Override
    public Set<VfxShape> shapes() {
        return EnumSet.of(VfxShape.MANA_ZONE_SHELL, VfxShape.MANA_ZONE_GROUND, VfxShape.MANA_ZONE_BURST, VfxShape.MANA_ZONE_BUBBLE, VfxShape.GRAND_ZONE_ENV);
    }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case MANA_ZONE_BURST -> 24;
            case MANA_ZONE_BUBBLE -> 160;
            case GRAND_ZONE_ENV -> 70;
            default -> 200;
        };
    }

    @Override public int defaultColor(VfxShape s) { return s == VfxShape.GRAND_ZONE_ENV ? 0xFF8C6BFF : 0xFF8A7CFF; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.MANA_ZONE_BURST) VfxShake.add(inst.payload.from(), inst.payload.to().x > 0.5 ? 1.2f : 0.7f, inst.payload.to().x > 0.5 ? 10 : 6);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case MANA_ZONE_SHELL -> field(inst, ctx, buf, true);
            case MANA_ZONE_GROUND -> field(inst, ctx, buf, false);
            case MANA_ZONE_BURST -> burst(inst, ctx, buf);
            case MANA_ZONE_BUBBLE -> bubble(inst, ctx, buf);
            case GRAND_ZONE_ENV -> env(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ the Mana Zone itself
    private static float growTicks(VfxInstance inst) {
        double g = inst.payload.to().z;
        return g >= 5 ? (float) g : 60f;
    }

    private void field(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, boolean shell) {
        if (ManaZoneGeometry.isEnded(inst.seed)) return;
        float elapsed = (float) inst.payload.to().x + inst.ageTicks(ctx.partialTick);
        float g = ManaZoneGeometry.ease(elapsed / growTicks(inst));
        float radius = inst.power * g;
        if (radius < 0.4f) return;
        float appear = Mth.clamp(elapsed / 8f, 0f, 1f);
        Vector3f feet = ctx.rel(inst.from(ctx));
        float cy = feet.y + 0.9f;                                         // the zone's centre is the caster's chest
        float dist = (float) Math.sqrt(feet.x * feet.x + cy * cy + feet.z * feet.z);
        int lvl = ManaZoneGeometry.level(dist, radius, ctx.detail);
        if (lvl < 0) return;
        int style = (int) inst.payload.to().y;
        if (shell) {
            float edge = (float) Math.max(0.25, Math.min(1.0, Math.abs(dist - radius) / 3.0));      // the shell goes see-through when the camera is on it
            if (dist > radius * 1.06f) {                                   // from outside: one round sprite for the true outline, the mesh sits inside it
                float s = radius * dist / (float) Math.sqrt(dist * dist - radius * radius);
                billboard(buf, ctx, RIM, VfxBlend.ADD, feet.x, cy, feet.z, s, s, 0f, VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(inst.color, 0.2f), 0.8f * appear * edge));
            }
            drawShell(buf, lvl, feet.x, cy, feet.z, radius, inst.color, 0.10f, 0.62f, appear * edge, elapsed * 0.0018f, elapsed * 0.04f, 6f, 3f);
            return;
        }
        int col = inst.color, pale = VfxVertexBuffer.whiten(col, 0.55f), acc = ManaZoneGeometry.accent(style, VfxVertexBuffer.whiten(col, 0.25f));
        float y0 = feet.y + 0.04f;
        float spin = elapsed * 0.0035f;

        // the disc under the whole zone, and the growth front while it is still growing
        flat(buf, FLOOR, VfxBlend.ADD, feet.x, y0, feet.z, radius, spin, VfxVertexBuffer.withAlpha(col, 0.55f * appear));
        float front = (1f - g) * 1.4f;
        if (front > 0.03f) flat(buf, BURST, VfxBlend.ADD, feet.x, y0 + 0.01f, feet.z, radius * 1.04f, 0f, VfxVertexBuffer.withAlpha(pale, Math.min(1f, front) * 0.85f));
        float wave = (elapsed % 90f) / 90f;                               // a slow breath outwards, one every 4.5 s
        if (g > 0.9f) flat(buf, BURST, VfxBlend.ADD, feet.x, y0 + 0.012f, feet.z, radius * (0.15f + 0.85f * wave), 0f,
                VfxVertexBuffer.withAlpha(col, (1f - wave) * (1f - wave) * 0.5f * appear));

        // two rune rings, turning against each other
        float w1 = Mth.clamp(radius * 0.035f, 0.5f, 1.4f), r1 = radius * 0.985f;
        ringBand(buf, RING, lvl, feet.x, y0 + 0.02f, feet.z, r1 - w1, r1 + w1 * 0.2f, Math.max(1f, Math.round(TWO_PI * r1 / (RING_ASPECT * w1 * 1.2f))), elapsed * 0.0030f,
                VfxVertexBuffer.withAlpha(pale, 0.9f * appear));
        float w2 = w1 * 0.7f, r2 = radius * 0.62f;
        ringBand(buf, RING, lvl, feet.x, y0 + 0.03f, feet.z, r2 - w2, r2 + w2 * 0.2f, Math.max(1f, Math.round(TWO_PI * r2 / (RING_ASPECT * w2 * 1.2f))), -elapsed * 0.0042f,
                VfxVertexBuffer.withAlpha(acc, 0.75f * appear));

        // the foothold: the hexagonal step of pressed mana under the caster's feet, with a pool of light under it
        flat(buf, PAD, VfxBlend.ADD, feet.x, feet.y + 0.07f, feet.z, 0.62f, elapsed * 0.03f, VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(col, 0.4f), 0.95f * appear));
        flat(buf, VfxTextures.GLOW, VfxBlend.ADD, feet.x, feet.y + 0.05f, feet.z, 1.5f, 0f, VfxVertexBuffer.withAlpha(col, 0.35f * appear));

        // motes rising off the plane in the colour of the caster's magic, and streaks of mana running in from the edge
        int motes = lvl == 0 ? ManaZoneGeometry.MOTES : lvl == 1 ? 12 : 6;
        float riseMax = Math.min(10f, radius * 0.45f);
        for (int i = 0; i < motes; i++) {
            float h1 = ManaZoneGeometry.hash01(inst.seed, i * 4), h2 = ManaZoneGeometry.hash01(inst.seed, i * 4 + 1), h3 = ManaZoneGeometry.hash01(inst.seed, i * 4 + 2), h4 = ManaZoneGeometry.hash01(inst.seed, i * 4 + 3);
            float a = h1 * TWO_PI + elapsed * 0.004f * (0.5f + h4), d = radius * Mth.sqrt(h2) * 0.95f;
            float ph = Mth.frac(elapsed * (0.006f + 0.010f * h3) + h4);
            float al = (float) Math.sin(Math.PI * ph) * appear * 0.9f;
            if (al < 0.03f) continue;
            billboard(buf, ctx, MOTE, VfxBlend.ADD, feet.x + Mth.cos(a) * d, y0 + 0.2f + ph * riseMax, feet.z + Mth.sin(a) * d, 0.3f + 0.4f * h3, 0.3f + 0.4f * h3, elapsed * 0.02f * (h4 - 0.5f), VfxVertexBuffer.withAlpha(acc, al));
        }
        int streaks = lvl == 0 ? ManaZoneGeometry.STREAKS : lvl == 1 ? 5 : 2;
        for (int i = 0; i < streaks; i++) {
            float h1 = ManaZoneGeometry.hash01(inst.seed ^ 0x5DEECE66DL, i * 3), h2 = ManaZoneGeometry.hash01(inst.seed ^ 0x5DEECE66DL, i * 3 + 1), h3 = ManaZoneGeometry.hash01(inst.seed ^ 0x5DEECE66DL, i * 3 + 2);
            float a = h1 * TWO_PI, f = Mth.frac(elapsed * 0.009f * (0.6f + 0.8f * h2) + h3);
            float start = radius * (0.55f + 0.4f * h2), d0 = start * (1f - f), d1 = start * Math.max(0f, 1f - f - 0.16f);
            float y = y0 + 0.5f + h3 * 3.2f + Mth.sin(elapsed * 0.05f + h1 * 9f) * 0.3f;
            float al = (float) Math.sin(Math.PI * f) * 0.85f * appear;
            if (al < 0.04f) continue;
            streakTex(buf, FLOW, VfxBlend.ADD, feet.x + Mth.cos(a) * d0, y, feet.z + Mth.sin(a) * d0, feet.x + Mth.cos(a) * d1, y, feet.z + Mth.sin(a) * d1,
                    0.22f + 0.2f * h3, VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(col, 0.3f), al));
        }
    }

    // ------------------------------------------------------------------ opening pulse and collapse finisher
    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float t = Mth.clamp(age / inst.duration, 0f, 1f);
        boolean collapse = inst.payload.to().x > 0.5;
        float R = Math.max(1f, inst.power);
        Vector3f c = ctx.rel(inst.from(ctx));
        int col = inst.color, pale = VfxVertexBuffer.whiten(col, 0.6f);
        float y0 = c.y + 0.05f;
        if (!collapse) {
            float e = ManaZoneGeometry.ease(t);
            float fade = 1f - t;
            flat(buf, BURST, VfxBlend.ADD, c.x, y0, c.z, Math.max(0.5f, R * 1.15f * e), age * 0.02f, VfxVertexBuffer.withAlpha(pale, fade * 0.95f));
            flat(buf, BURST, VfxBlend.ADD, c.x, y0 + 0.01f, c.z, Math.max(0.4f, R * 0.7f * e), -age * 0.03f, VfxVertexBuffer.withAlpha(col, fade * 0.7f));
            flat(buf, PAD, VfxBlend.ADD, c.x, y0 + 0.02f, c.z, 1.2f + 2f * e, age * 0.1f, VfxVertexBuffer.withAlpha(WHITE, fade));
            VfxBloom.glow(ctx, buf, new Vector3f(c.x, c.y + 1f, c.z), 1.5f + 2.5f * (1f - t), col, 0.9f * fade);
            for (int i = 0; i < 12; i++) {                                // sparks thrown out along the plane
                float a = ManaZoneGeometry.hash01(inst.seed, i) * TWO_PI, d = R * (0.2f + 0.9f * e) * (0.6f + 0.4f * ManaZoneGeometry.hash01(inst.seed, i + 40));
                billboard(buf, ctx, MOTE, VfxBlend.ADD, c.x + Mth.cos(a) * d, y0 + 0.3f + 1.2f * e, c.z + Mth.sin(a) * d, 0.5f, 0.5f, age * 0.1f, VfxVertexBuffer.withAlpha(pale, fade));
            }
            return;
        }
        // collapse: the edge runs back in, the motes fall to the caster, then a flash
        float e = ManaZoneGeometry.ease(t);
        float r = R * (1f - e);
        float fade = t < 0.8f ? 1f : (1f - t) / 0.2f;
        flat(buf, BURST, VfxBlend.ADD, c.x, y0, c.z, Math.max(0.6f, r), age * 0.05f, VfxVertexBuffer.withAlpha(pale, fade * 0.95f));
        flat(buf, BURST, VfxBlend.ADD, c.x, y0 + 0.01f, c.z, Math.max(0.5f, r * 0.6f), -age * 0.05f, VfxVertexBuffer.withAlpha(col, fade * 0.7f));
        for (int i = 0; i < 20; i++) {
            float a = ManaZoneGeometry.hash01(inst.seed, i) * TWO_PI, d0 = R * (0.3f + 0.7f * ManaZoneGeometry.hash01(inst.seed, i + 40));
            float d = d0 * (1f - e), h = 0.3f + 2.5f * ManaZoneGeometry.hash01(inst.seed, i + 80) * (1f - e);
            billboard(buf, ctx, MOTE, VfxBlend.ADD, c.x + Mth.cos(a) * d, y0 + h, c.z + Mth.sin(a) * d, 0.5f, 0.5f, age * 0.2f, VfxVertexBuffer.withAlpha(pale, fade * 0.9f));
        }
        if (t > 0.7f) VfxBloom.glow(ctx, buf, new Vector3f(c.x, c.y + 1f, c.z), 7f * (t - 0.7f) / 0.3f, WHITE, 1.2f * (1f - 0.6f * t));
    }

    // ------------------------------------------------------------------ the pressurised protective space
    private void bubble(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float elapsed = (float) inst.payload.to().x + age;
        float open = VfxAnim.easeOutBack(Mth.clamp(elapsed / 8f, 0f, 1f));
        float fade = Mth.clamp((inst.duration - age) / 14f, 0f, 1f);
        float radius = inst.power * open;
        if (radius < 0.2f || fade <= 0f) return;
        Vector3f c = ctx.rel(inst.from(ctx));
        float cy = c.y + radius * 0.55f;
        float dist = (float) Math.sqrt(c.x * c.x + cy * cy + c.z * c.z);
        int lvl = ManaZoneGeometry.level(dist, radius * 3f, ctx.detail);
        if (lvl < 0) return;
        int col = inst.color;
        drawShell(buf, Math.min(lvl + 1, 2), c.x, cy, c.z, radius, col, 0.16f, 0.7f, fade, elapsed * 0.004f, elapsed * 0.08f, 3f, 2f);
        flat(buf, FLOOR, VfxBlend.ADD, c.x, c.y + 0.05f, c.z, radius, elapsed * 0.01f, VfxVertexBuffer.withAlpha(col, 0.6f * fade));
        float w = Mth.clamp(radius * 0.06f, 0.25f, 0.5f);
        ringBand(buf, RING, Math.min(lvl + 1, 2), c.x, c.y + 0.07f, c.z, radius * 0.98f - w, radius * 0.98f + w * 0.2f, Math.max(1f, Math.round(TWO_PI * radius / (RING_ASPECT * w * 1.2f))), elapsed * 0.004f,
                VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(col, 0.5f), 0.9f * fade));
        int motes = lvl == 0 ? 12 : 5;
        for (int i = 0; i < motes; i++) {
            float h1 = ManaZoneGeometry.hash01(inst.seed, i * 3), h2 = ManaZoneGeometry.hash01(inst.seed, i * 3 + 1), h3 = ManaZoneGeometry.hash01(inst.seed, i * 3 + 2);
            float a = h1 * TWO_PI + elapsed * 0.01f, d = radius * Mth.sqrt(h2) * 0.9f, ph = Mth.frac(elapsed * 0.012f * (0.6f + h3) + h1);
            billboard(buf, ctx, MOTE, VfxBlend.ADD, c.x + Mth.cos(a) * d, c.y + 0.2f + ph * radius, c.z + Mth.sin(a) * d, 0.3f, 0.3f, 0f,
                    VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(col, 0.4f), (float) Math.sin(Math.PI * ph) * fade));
        }
    }

    // ------------------------------------------------------------------ Grand Magic Zone scenery
    private void env(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float elapsed = (float) inst.payload.to().x + inst.ageTicks(ctx.partialTick);
        int theme = (int) inst.payload.to().y, mask = (int) inst.payload.to().z;
        float k = Mth.clamp(inst.power, 0f, 1f);
        if (k <= 0.02f) return;
        int col = inst.color, pale = VfxVertexBuffer.whiten(col, 0.5f);
        Vector3f feet = ctx.rel(inst.from(ctx));
        long seed = inst.seed;
        boolean storm = (mask & ManaZoneGeometry.FX_STORM) != 0;

        // mana flowing through the ground: veins of light in two scales, scrolling slowly (world-anchored: they do not swim with the player)
        double wx = ctx.camPos.x, wz = ctx.camPos.z;
        float sx = (float) (Math.floor(wx / 8.0) * 8.0), sz = (float) (Math.floor(wz / 8.0) * 8.0);
        for (int layer = 0; layer < 2; layer++) {
            float half = layer == 0 ? 26f : 20f, scale = layer == 0 ? 22f : 11f, scroll = elapsed * (layer == 0 ? 0.0006f : -0.0009f);
            float u0 = (sx - half) / scale + scroll, v0 = (sz - half) / scale, u1 = (sx + half) / scale + scroll, v1 = (sz + half) / scale;
            float rx = (float) (sx - wx), rz = (float) (sz - wz), y = feet.y + 0.03f + layer * 0.01f;
            int c = VfxVertexBuffer.withAlpha(layer == 0 ? col : pale, (layer == 0 ? 0.30f : 0.20f) * k);
            q(buf, VEIN, VfxBlend.ADD, rx - half, y, rz - half, rx + half, y, rz - half, rx + half, y, rz + half, rx - half, y, rz + half, u0, v0, u1, v1, c, c);
        }

        if (storm) stormBolts(buf, ctx, seed, elapsed, col, k);

        switch (theme) {
            case ManaZoneGeometry.THEME_SEABED -> seabed(buf, ctx, seed, elapsed, col, pale, k);
            case ManaZoneGeometry.THEME_FOREST -> {
                scatter(buf, ctx, seed + 11, elapsed, SPORE, VfxBlend.ADD, 14, 2, 0.25f, 0.9f, 0.5f, 2.2f, 30f, VfxVertexBuffer.whiten(col, 0.3f), 0.9f * k, 36);
                scatter(buf, ctx, seed + 12, elapsed, HALO, VfxBlend.ADD, 30, 1, 9f, 16f, 0.0f, 3.5f, 55f, col, 0.11f * k, 8);
            }
            case ManaZoneGeometry.THEME_VOLCANO -> volcano(buf, ctx, seed, elapsed, col, pale, k);
            case ManaZoneGeometry.THEME_GRAVITO -> gravito(buf, ctx, seed, elapsed, col, pale, k, mask);
            case ManaZoneGeometry.THEME_HAVEN -> {
                scatter(buf, ctx, seed + 21, elapsed, MOTE, VfxBlend.ADD, 12, 2, 0.3f, 0.7f, 0.6f, 1.2f, 24f, VfxVertexBuffer.whiten(col, 0.5f), 0.8f * k, 40);
                flat(buf, BURST, VfxBlend.ADD, feet.x, feet.y + 0.05f, feet.z, 4.5f + 0.4f * Mth.sin(elapsed * 0.04f), elapsed * 0.004f, VfxVertexBuffer.withAlpha(col, 0.22f * k));
            }
            default -> scatter(buf, ctx, seed + 31, elapsed, MOTE, VfxBlend.ADD, 14, 1, 0.3f, 0.8f, 0.3f, 1.6f, 28f, pale, 0.7f * k, 24);
        }
        if ((mask & ManaZoneGeometry.FX_WITCH) != 0 && theme != ManaZoneGeometry.THEME_FOREST)
            scatter(buf, ctx, seed + 41, elapsed, SPORE, VfxBlend.ADD, 14, 2, 0.25f, 0.8f, 0.5f, 2.0f, 28f, pale, 0.8f * k, 24);
    }

    private void seabed(VfxVertexBuffer buf, VfxRenderContext ctx, long seed, float elapsed, int col, int pale, float k) {
        // violent currents: long glinting ribbons that sweep round the zone, a few bubbles rising through them
        double cx = ctx.camPos.x, cy = ctx.camPos.y, cz = ctx.camPos.z;
        final int cell = 18;
        int ix = Mth.floor(cx / cell), iy = Mth.floor(cy / cell), iz = Mth.floor(cz / cell), n = 0;
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++) {
                    long cs = ManaZoneGeometry.cellSeed(seed + 5, ix + dx, iy + dy, iz + dz);
                    if (ManaZoneGeometry.hash01(cs, 0) > 0.55f || n >= 14) continue;
                    float drift = Mth.frac(elapsed * 0.0035f * (0.7f + ManaZoneGeometry.hash01(cs, 4)) + ManaZoneGeometry.hash01(cs, 1)) * cell;
                    float ang = ManaZoneGeometry.hash01(cs, 5) * TWO_PI;
                    float x = (ix + dx) * cell + drift * Mth.cos(ang) - (float) cx, z = (iz + dz) * cell + drift * Mth.sin(ang) - (float) cz;
                    float y = (iy + dy + ManaZoneGeometry.hash01(cs, 2)) * cell - (float) cy;
                    float len = 5f + 6f * ManaZoneGeometry.hash01(cs, 3);
                    float fade = 1f - Mth.clamp((Mth.sqrt(x * x + y * y + z * z) - 14f) / 20f, 0f, 1f);
                    if (fade <= 0f) continue;
                    float sx = Mth.cos(ang + 1.2f) * len, sz = Mth.sin(ang + 1.2f) * len;
                    streakTex(buf, CURRENT, VfxBlend.ADD, x - sx, y, z - sz, x + sx, y, z + sz, 0.9f + 0.7f * ManaZoneGeometry.hash01(cs, 6),
                            VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(col, 0.35f), 0.55f * fade * k));
                    n++;
                }
        scatter(buf, ctx, seed + 6, elapsed, SPORE, VfxBlend.ADD, 12, 2, 0.12f, 0.35f, 2.4f, 0.8f, 26f, WHITE, 0.55f * k, 26);
    }

    private void volcano(VfxVertexBuffer buf, VfxRenderContext ctx, long seed, float elapsed, int col, int pale, float k) {
        // heat haze: tall wavering ribbons near the ground; embers rising fast
        double cx = ctx.camPos.x, cz = ctx.camPos.z;
        final int cell = 14;
        int ix = Mth.floor(cx / cell), iz = Mth.floor(cz / cell), n = 0;
        final float feetY = -1.62f;                                          // camera-relative: the haze stands at the viewer's feet (the floor's height is not known here)
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                long cs = ManaZoneGeometry.cellSeed(seed + 9, ix + dx, 0, iz + dz);
                if (ManaZoneGeometry.hash01(cs, 0) > 0.7f || n >= 8) continue;
                float x = (ix + dx + ManaZoneGeometry.hash01(cs, 1)) * cell - (float) cx, z = (iz + dz + ManaZoneGeometry.hash01(cs, 2)) * cell - (float) cz;
                float d = Mth.sqrt(x * x + z * z);
                float fade = 1f - Mth.clamp((d - 18f) / 14f, 0f, 1f);
                if (fade <= 0f || d < 2.5f) continue;
                float h = 5f + 5f * ManaZoneGeometry.hash01(cs, 3), sway = Mth.sin(elapsed * 0.05f + ManaZoneGeometry.hash01(cs, 4) * 6.28f) * 0.5f;
                billboard(buf, ctx, HAZE, VfxBlend.ADD, x + sway, feetY + h * 0.5f, z, 1.6f, h * 0.5f, 0f, VfxVertexBuffer.withAlpha(pale, 0.20f * fade * k));
                n++;
            }
        scatter(buf, ctx, seed + 10, elapsed, MOTE, VfxBlend.ADD, 12, 2, 0.12f, 0.3f, 3.2f, 1.0f, 24f, 0xFFFFA040, 0.9f * k, 36);
    }

    private void gravito(VfxVertexBuffer buf, VfxRenderContext ctx, long seed, float elapsed, int col, int pale, float k, int mask) {
        double cx = ctx.camPos.x, cy = ctx.camPos.y, cz = ctx.camPos.z;
        if ((mask & ManaZoneGeometry.FX_BOULDERS) != 0) {
            // massive boulders hung in the air on a coarse world grid, bobbing; the nearest are drawn with a seam of mana light behind them
            final int cell = 40;
            int ix = Mth.floor(cx / cell), iy = Mth.floor(cy / cell), iz = Mth.floor(cz / cell), n = 0;
            for (int dx = -1; dx <= 1; dx++)
                for (int dy = -1; dy <= 1; dy++)
                    for (int dz = -1; dz <= 1; dz++) {
                        long cs = ManaZoneGeometry.cellSeed(seed + 3, ix + dx, iy + dy, iz + dz);
                        if (ManaZoneGeometry.hash01(cs, 0) > 0.5f || n >= 12) continue;
                        float bob = Mth.sin(elapsed * 0.018f + ManaZoneGeometry.hash01(cs, 4) * 6.28f) * 1.6f, turn = elapsed * 0.002f * (ManaZoneGeometry.hash01(cs, 5) - 0.5f);
                        float x = (ix + dx + ManaZoneGeometry.hash01(cs, 1)) * cell - (float) cx, y = (iy + dy + ManaZoneGeometry.hash01(cs, 2)) * cell - (float) cy + bob, z = (iz + dz + ManaZoneGeometry.hash01(cs, 3)) * cell - (float) cz;
                        float d = Mth.sqrt(x * x + y * y + z * z), size = 4f + 11f * ManaZoneGeometry.hash01(cs, 6);
                        if (d < size * 0.8f + 2f) continue;
                        float fade = 1f - Mth.clamp((d - 55f) / 25f, 0f, 1f);
                        if (fade <= 0f) continue;
                        billboard(buf, ctx, HALO, VfxBlend.ADD, x, y - size * 0.15f, z, size * 0.95f, size * 0.95f, 0f, VfxVertexBuffer.withAlpha(col, 0.18f * fade * k));
                        billboard(buf, ctx, BOULDER, VfxBlend.ALPHA, x, y, z, size * 0.5f, size * 0.5f, turn, VfxVertexBuffer.withAlpha(ManaZoneGeometry.mix(0xFF6A6478, col, 0.18f), fade));
                        n++;
                    }
            scatter(buf, ctx, seed + 4, elapsed, BOULDER, VfxBlend.ALPHA, 16, 2, 0.25f, 0.9f, 0.35f, 2.5f, 30f, ManaZoneGeometry.mix(0xFF7A7488, col, 0.15f), 0.95f * k, 22);   // drifting debris
        }
        // gravity ripples: a faint ring leaves the viewer's feet every 3 seconds
        float ph = (elapsed % 70f) / 70f;
        flat(buf, BURST, VfxBlend.ADD, 0f, -1.58f, 0f, 1f + 14f * ph, 0f, VfxVertexBuffer.withAlpha(pale, (1f - ph) * (1f - ph) * 0.5f * k));
    }

    private void stormBolts(VfxVertexBuffer buf, VfxRenderContext ctx, long seed, float elapsed, int col, float k) {
        // the storm's distant lightning: up to three bolts at a time, each flashing for a few ticks, with a glow in the sky
        int slot = (int) (elapsed / 38f);
        for (int s = 0; s < 3; s++) {
            int n = slot - s;
            float born = n * 38f + ManaZoneGeometry.hash01(seed, n * 5) * 20f, age = elapsed - born;
            if (age < 0f || age > 9f || ManaZoneGeometry.hash01(seed, n * 5 + 1) > 0.65f) continue;
            float a = ManaZoneGeometry.hash01(seed, n * 5 + 2) * TWO_PI, d = 38f + 34f * ManaZoneGeometry.hash01(seed, n * 5 + 3);
            float flash = (1f - age / 9f) * (0.6f + 0.4f * Mth.sin(age * 2.7f));
            float x = Mth.cos(a) * d, z = Mth.sin(a) * d;
            streakTex(buf, BOLT, VfxBlend.ADD, x, -6f, z, x + 3f * Mth.cos(a + 1.6f), 58f, z + 3f * Mth.sin(a + 1.6f), 5.5f, VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(col, 0.6f), flash * k));
            billboard(buf, ctx, HALO, VfxBlend.ADD, x, 56f, z, 26f, 14f, 0f, VfxVertexBuffer.withAlpha(col, 0.35f * flash * k));
        }
    }

    // ------------------------------------------------------------------ primitives
    /** The shell: the cached unit sphere scaled to radius and moved to (cx, cy, cz). alpha = base + fres * Fresnel. */
    private static void drawShell(VfxVertexBuffer buf, int lvl, float cx, float cy, float cz, float radius, int col, float alphaBase, float fres, float appear,
                                  float uScroll, float phase, float repeatU, float repeatV) {
        int lon = ManaZoneGeometry.LON[lvl], lat = ManaZoneGeometry.LAT[lvl];
        float[] pos = ManaZoneGeometry.SHELL_POS[lvl], nrm = ManaZoneGeometry.SHELL_NRM[lvl];
        int stride = lon + 1;
        int rim = VfxVertexBuffer.whiten(col, 0.35f);
        for (int i = 0; i < lat; i++) {
            for (int j = 0; j < lon; j++) {
                int k = (i * lon + j) * 3;
                float nx = nrm[k], ny = nrm[k + 1], nz = nrm[k + 2];
                float qx = cx + nx * radius, qy = cy + ny * radius, qz = cz + nz * radius;
                float len = Mth.sqrt(qx * qx + qy * qy + qz * qz);
                float dot = len < 1e-3f ? 0f : -(nx * qx + ny * qy + nz * qz) / len;
                float f = 1f - Math.abs(dot);
                float a = (alphaBase + fres * f * Mth.sqrt(f)) * appear * (0.82f + 0.18f * Mth.sin(phase + (i * 3 + j) * 0.9f));
                if (a < 0.02f) continue;
                int c = VfxVertexBuffer.withAlpha(f > 0.55f ? rim : col, Math.min(1f, a));
                int p00 = (i * stride + j) * 3, p01 = (i * stride + j + 1) * 3, p10 = ((i + 1) * stride + j) * 3, p11 = ((i + 1) * stride + j + 1) * 3;
                A.set(cx + pos[p00] * radius, cy + pos[p00 + 1] * radius, cz + pos[p00 + 2] * radius);
                B.set(cx + pos[p01] * radius, cy + pos[p01 + 1] * radius, cz + pos[p01 + 2] * radius);
                C.set(cx + pos[p11] * radius, cy + pos[p11 + 1] * radius, cz + pos[p11 + 2] * radius);
                D.set(cx + pos[p10] * radius, cy + pos[p10 + 1] * radius, cz + pos[p10 + 2] * radius);
                float u0 = uScroll + repeatU * j / lon, u1 = uScroll + repeatU * (j + 1) / lon, v0 = repeatV * i / lat, v1 = repeatV * (i + 1) / lat;
                buf.quad(SHELL, VfxBlend.ADD, A, B, C, D, u0, v0, u1, v1, c, c);
            }
        }
    }

    /** A flat ring band on the plane y: the cached segment table, no trigonometry. */
    private static void ringBand(VfxVertexBuffer buf, ResourceLocation tex, int lvl, float cx, float y, float cz, float rIn, float rOut, float uRepeat, float uScroll, int argb) {
        int n = ManaZoneGeometry.RING[lvl];
        float[] cs = ManaZoneGeometry.RING_COS[lvl], sn = ManaZoneGeometry.RING_SIN[lvl];
        for (int s = 0; s < n; s++) {
            A.set(cx + cs[s] * rIn, y, cz + sn[s] * rIn);
            B.set(cx + cs[s + 1] * rIn, y, cz + sn[s + 1] * rIn);
            C.set(cx + cs[s + 1] * rOut, y, cz + sn[s + 1] * rOut);
            D.set(cx + cs[s] * rOut, y, cz + sn[s] * rOut);
            buf.quad(tex, VfxBlend.ADD, A, B, C, D, uScroll + uRepeat * s / n, 0f, uScroll + uRepeat * (s + 1) / n, 1f, argb, argb);
        }
    }

    /** A square lying flat at height y, turned by rot, half-size h. */
    private static void flat(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, float cx, float y, float cz, float h, float rot, int argb) {
        float cs = Mth.cos(rot) * h, sn = Mth.sin(rot) * h;
        A.set(cx - cs - sn, y, cz + sn - cs);
        B.set(cx + cs - sn, y, cz - sn - cs);
        C.set(cx + cs + sn, y, cz - sn + cs);
        D.set(cx - cs + sn, y, cz + sn + cs);
        buf.quad(tex, blend, A, B, C, D, 0f, 0f, 1f, 1f, argb, argb);
    }

    /** A free quad with explicit corners and UVs. */
    private static void q(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz,
                          float dx, float dy, float dz, float u0, float v0, float u1, float v1, int c0, int c1) {
        A.set(ax, ay, az);
        B.set(bx, by, bz);
        C.set(cx, cy, cz);
        D.set(dx, dy, dz);
        buf.quad(tex, blend, A, B, C, D, u0, v0, u1, v1, c0, c1);
    }

    /** A camera-facing sprite, half-sizes hw x hh, turned by rot. */
    private static void billboard(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, VfxBlend blend, float x, float y, float z, float hw, float hh, float rot, int argb) {
        float cs = Mth.cos(rot), sn = Mth.sin(rot);
        float rx = (ctx.camRight.x * cs + ctx.camUp.x * sn) * hw, ry = (ctx.camRight.y * cs + ctx.camUp.y * sn) * hw, rz = (ctx.camRight.z * cs + ctx.camUp.z * sn) * hw;
        float ux = (ctx.camUp.x * cs - ctx.camRight.x * sn) * hh, uy = (ctx.camUp.y * cs - ctx.camRight.y * sn) * hh, uz = (ctx.camUp.z * cs - ctx.camRight.z * sn) * hh;
        A.set(x - rx - ux, y - ry - uy, z - rz - uz);
        B.set(x + rx - ux, y + ry - uy, z + rz - uz);
        C.set(x + rx + ux, y + ry + uy, z + rz + uz);
        D.set(x - rx + ux, y - ry + uy, z - rz + uz);
        buf.quad(tex, blend, A, B, C, D, 0f, 0f, 1f, 1f, argb, argb);
    }

    /** A ribbon from a to b facing the camera (the camera is the origin), the head of the texture at b. */
    private static void streakTex(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, float ax, float ay, float az, float bx, float by, float bz, float width, int argb) {
        float dx = bx - ax, dy = by - ay, dz = bz - az;
        float mx = (ax + bx) * 0.5f, my = (ay + by) * 0.5f, mz = (az + bz) * 0.5f;
        float sx = dy * mz - dz * my, sy = dz * mx - dx * mz, sz = dx * my - dy * mx;        // dir x mid (the camera sits at the origin, so mid is the view direction)
        float l = Mth.sqrt(sx * sx + sy * sy + sz * sz);
        if (l < 1e-5f) return;
        float k = width * 0.5f / l;
        sx *= k;
        sy *= k;
        sz *= k;
        A.set(ax - sx, ay - sy, az - sz);
        B.set(ax + sx, ay + sy, az + sz);
        C.set(bx + sx, by + sy, bz + sz);
        D.set(bx - sx, by - sy, bz - sz);
        buf.quad(tex, blend, A, B, C, D, 0f, 0f, 1f, 1f, argb, argb);
    }

    /**
     * Sprites scattered on a world-anchored grid of {@code cell}-block cells round the camera (3 x 3 x 3 cells, {@code perCell} each at most), each
     * cycling vertically through its cell at {@code rise} and swaying: no state, the same scenery every frame and for every viewer.
     */
    private static void scatter(VfxVertexBuffer buf, VfxRenderContext ctx, long seed, float elapsed, ResourceLocation tex, VfxBlend blend, int cell, int perCell,
                                float sizeMin, float sizeMax, float rise, float sway, float range, int argb, float alpha, int max) {
        double cx = ctx.camPos.x, cy = ctx.camPos.y, cz = ctx.camPos.z;
        int ix = Mth.floor(cx / cell), iy = Mth.floor(cy / cell), iz = Mth.floor(cz / cell), n = 0;
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++)
                    for (int o = 0; o < perCell; o++) {
                        if (n >= max) return;
                        long cs = ManaZoneGeometry.cellSeed(seed + o * 7919L, ix + dx, iy + dy, iz + dz);
                        float h1 = ManaZoneGeometry.hash01(cs, 1), h2 = ManaZoneGeometry.hash01(cs, 2), h3 = ManaZoneGeometry.hash01(cs, 3), h4 = ManaZoneGeometry.hash01(cs, 4);
                        float fy = rise == 0f ? h2 : Mth.frac(h2 + elapsed * rise * (0.5f + h4) / cell);
                        float s = elapsed * 0.03f + h3 * 6.28f;
                        float x = (ix + dx + h1) * cell + Mth.sin(s) * sway - (float) cx, y = (iy + dy + fy) * cell - (float) cy, z = (iz + dz + h3) * cell + Mth.cos(s * 0.8f) * sway - (float) cz;
                        float d = Mth.sqrt(x * x + y * y + z * z);
                        float fade = (rise == 0f ? 1f : (float) Math.sin(Math.PI * fy)) * (1f - Mth.clamp((d - range * 0.6f) / (range * 0.4f), 0f, 1f));
                        if (fade < 0.04f || d < 0.8f) continue;
                        float size = (sizeMin + (sizeMax - sizeMin) * h4) * 0.5f;
                        billboard(buf, ctx, tex, blend, x, y, z, size, size, rise == 0f ? 0f : s * 0.3f, VfxVertexBuffer.withAlpha(argb, alpha * fade));
                        n++;
                    }
    }
}
