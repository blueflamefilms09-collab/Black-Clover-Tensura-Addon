package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Song Magic (1.93.0 rebuild): brass, rose light, staff lines and notes, drawn after the Time Magic layers' way of working (layered quads, back to front, anticipation
 * and an afterglow). Textures: tools/gen_song_rebuild_textures.py (particle/song_*.png). Shapes, all three of the shape protocol plus the two older Kahono shapes:
 * <ul>
 *   <li>SONG_FX1, CAST / PROJECTILE, "Staccato". A small staff ring and a clef flash contract on 'from' (charge, to 22 percent of the life); then a head of rose light
 *       runs 'from' to 'to' dragging two crossed staff-line ribbons and three note sprites that spin along it; at the end a note burst and a sound ring open at 'to'.
 *       power = size (the cast shapes are drawn three times larger by the manager: 1 normal, 0.6 to 1.5 usual), duration = flight ticks (default 16).</li>
 *   <li>SONG_FX2, ZONE / FIELD, "Choir Field". A turning staff ring on the ground out to the radius, sound rings rolling out every 26 ticks, eight standing staff lines
 *       round the rim, a hovering treble clef in the middle and notes rising in spirals. from = centre on the ground, power = radius (the zone shapes are drawn 1.5 times
 *       larger), duration = life (default 80; in 8, out 12).</li>
 *   <li>SONG_FX3, IMPACT / SIGNATURE, "Finale". A white flash, a note burst that opens and turns, two sound rings (flat and upright), a staff ring flaring on the ground,
 *       notes flung out along arcs and a clef rising out of the middle. from = centre, to = optional direction hint, power = scale, duration = life (default 28).</li>
 *   <li>SONG_NOTES (Kahono's singing), notes with real glyph sprites and halos spiralling up from the singer with a clef at the top of the spiral;
 *       SONG_RING, the pulse: two staff rings running out to the radius on the ground, a flash under the singer and note sparks on the leading edge.</li>
 * </ul>
 * SongNotesLayer and SongRingLayer (the registered layers of the two older shapes) extend this class and draw those two shapes with the same code.
 * The colour tints everything (default rose 0xFFFFB4E8); the brass gold accent is fixed. Budget: well under the 400 vertices of an activation.
 */
public class SongFxLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation STAFF_V = t("song_staff_v"), WAVE = t("song_wave"), NOTE = t("song_note"), CLEF = t("song_clef"),
            BURST = t("song_burst"), RING = t("song_staffring"), GLOW = VfxTextures.GLOW, SPARK = VfxTextures.SPARK;

    private static final int WHITE = 0xFFFFFFFF, GOLD = 0xFFFFD27A;

    @Override
    public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.SONG_FX1, VfxShape.SONG_FX2, VfxShape.SONG_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case SONG_FX1 -> 16;
            case SONG_FX2 -> 80;
            case SONG_NOTES -> 40;
            case SONG_RING -> 24;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFFFB4E8; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case SONG_FX1 -> cast(inst, ctx, buf);
            case SONG_FX2 -> zone(inst, ctx, buf);
            case SONG_FX3 -> finale(inst, ctx, buf);
            case SONG_NOTES -> notes(inst, ctx, buf);
            case SONG_RING -> ring(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ helpers
    private static int a(int argb, float k) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(k, 0f, 1f)); }
    private static int hot(int argb, float k) { return VfxVertexBuffer.whiten(argb, k); }
    private static float h(VfxInstance inst, int i, int salt) { return ElementFx.hash(inst.seed, i, salt); }
    private static float ramp(float t, float s, float e) { return Mth.clamp((t - s) / Math.max(1e-4f, e - s), 0f, 1f); }
    private static float out(float t) { return VfxAnim.easeOutCubic(Mth.clamp(t, 0f, 1f)); }
    private static float life(VfxInstance inst, float age, float in, float outT) {
        return Math.min(in <= 0 ? 1f : Mth.clamp(age / in, 0f, 1f), outT <= 0 ? 1f : Mth.clamp((inst.duration - age) / outT, 0f, 1f));
    }

    private static void glow(VfxVertexBuffer buf, VfxRenderContext ctx, Vector3f p, float size, int col, float k) {
        if (size > 0.01f && k > 0.01f) buf.billboard(ctx, GLOW, VfxBlend.ADD, p, size, 0f, a(col, k));
    }

    private static void ground(VfxVertexBuffer buf, ResourceLocation tex, Vector3f c, float lift, float half, float spin, int col) {
        if (half > 0.02f) buf.plane(tex, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, lift, 0)).spin(spin), half, col);
    }

    /** A staff-line ribbon from a (tail) to b (head) turned to the camera. */
    private static void streak(VfxVertexBuffer buf, VfxRenderContext ctx, Vector3f tail, Vector3f head, float w, int cTail, int cHead) {
        buf.beam(ctx, STAFF_V, VfxBlend.ADD, head, tail, w, w * 0.6f, 1, 0f, cHead, cTail);
    }

    // ------------------------------------------------------------------ FX1: Staccato
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), d = Math.max(6f, inst.duration);
        float u = Mth.clamp(inst.power, 0.5f, 6f) / 3f;
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(from);
        float dist = dir.length();
        boolean still = dist < 0.4f;
        if (still) dir.set(0, 0, 1); else dir.mul(1f / dist);
        int col = inst.color | 0xFF000000;
        float tl = 0.22f * d, ta = 0.78f * d;

        // charge: a small staff ring and a clef contracting on 'from'
        float tc = ramp(age, 0f, tl), cf = tc * (1f - ramp(age, tl, tl * 1.5f + 1f));
        if (still) cf = Math.max(cf, 1f - ramp(age, 0f, d));
        if (cf > 0.02f) {
            VfxPose face = VfxPose.facing(new Vector3f(from), new Vector3f(dir).lerp(new Vector3f(ctx.camRight).cross(ctx.camUp), 0.5f));
            buf.plane(RING, VfxBlend.ADD, face.spin(age * 0.2f), (1.1f - 0.5f * tc) * u * 0.9f, a(col, 0.85f * cf));
            buf.billboard(ctx, CLEF, VfxBlend.ADD, from, 0.85f * u * (0.4f + 0.6f * tc), age * 0.05f, a(hot(col, 0.5f), 0.9f * cf));
            glow(buf, ctx, from, 0.9f * u, col, 0.6f * cf);
        }
        if (still) return;

        // flight: a head of light, two crossed staff ribbons, three notes along the trail
        float s = ramp(age, tl, ta), se = 0.4f * s + 0.6f * s * s;
        Vector3f head = new Vector3f(dir).mul(dist * se).add(from);
        float trail = Math.min(dist * se, 2.6f * u + 0.6f);
        Vector3f tail = new Vector3f(head).sub(new Vector3f(dir).mul(trail));
        float fly = s > 0f && s < 1f ? 1f : 0f, fadeOut = 1f - ramp(age, ta, d);
        if (fly > 0f) {
            streak(buf, ctx, tail, head, 0.55f * u, a(col, 0f), a(hot(col, 0.4f), 0.95f));
            Vector3f off = new Vector3f(ctx.camUp).mul(0.1f * u);
            streak(buf, ctx, new Vector3f(tail).add(off), new Vector3f(head).sub(off), 0.4f * u, a(col, 0f), a(WHITE, 0.7f));
            glow(buf, ctx, head, 0.8f * u, col, 0.8f);
            glow(buf, ctx, head, 0.3f * u, WHITE, 0.95f);
            for (int k = 0; k < 3; k++) {
                float f = 0.15f + 0.28f * k;
                Vector3f p = new Vector3f(head).sub(new Vector3f(dir).mul(trail * f)).add(new Vector3f(ctx.camUp).mul(Mth.sin(age * 0.5f + k * 2.1f) * 0.18f * u));
                buf.billboard(ctx, NOTE, VfxBlend.ADD, p, 0.42f * u * (1f - 0.25f * k), age * 0.18f * (k % 2 == 0 ? 1 : -1), a(k == 1 ? GOLD : col, 0.9f * (1f - 0.2f * k)));
            }
        }
        // impact: a note burst and a sound ring open at 'to'
        float ti = ramp(age, ta - 1f, d);
        if (ti > 0f) {
            float k = out(ti);
            buf.billboard(ctx, BURST, VfxBlend.ADD, to, 1.7f * u * k, age * 0.12f, a(hot(col, 0.4f), 0.9f * (1f - ti)));
            VfxPose face = VfxPose.facing(new Vector3f(to), new Vector3f(dir).negate());
            buf.plane(WAVE, VfxBlend.ADD, face.spin(age * 0.1f), 1.3f * u * k, a(col, 0.8f * (1f - ti)));
            glow(buf, ctx, to, 1.2f * u * (1f - 0.4f * ti), col, 0.7f * (1f - ti));
            for (int i = 0; i < 4; i++) {
                float ang = h(inst, i, 1) * Mth.TWO_PI;
                Vector3f p = new Vector3f(to).add(new Vector3f(ctx.camRight).mul(Mth.cos(ang) * k * 1.1f * u)).add(new Vector3f(ctx.camUp).mul(Mth.sin(ang) * k * 1.1f * u + 0.3f * k * u));
                buf.billboard(ctx, NOTE, VfxBlend.ADD, p, 0.34f * u, ang + age * 0.2f, a(i % 2 == 0 ? col : GOLD, 0.9f * (1f - ti)));
            }
        }
    }

    // ------------------------------------------------------------------ FX2: Choir Field
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float R = Math.max(1.2f, inst.power / 1.5f);
        float fade = life(inst, age, 8f, 12f), open = VfxAnim.easeOutBack(Mth.clamp(age / 10f, 0f, 1f));
        if (fade <= 0.01f) return;
        Vector3f c = ctx.rel(inst.from(ctx));
        int col = inst.color | 0xFF000000;
        float r = R * open;
        // ground: the staff ring (turning) and a faint second one turning back, an inner glow
        ground(buf, GLOW, c, 0.04f, r * 1.05f, 0f, a(col, 0.22f * fade));
        ground(buf, RING, c, 0.05f, r * 1.04f, age * 0.012f, a(col, 0.85f * fade));
        ground(buf, RING, c, 0.07f, r * 0.62f, -age * 0.02f, a(hot(col, 0.5f), 0.5f * fade));
        // sound rings rolling out every 26 ticks
        for (int k = 0; k < 2; k++) {
            float ph = ((age + k * 13f) % 26f) / 26f;
            ground(buf, WAVE, c, 0.08f, r * (0.25f + 0.8f * out(ph)), age * 0.03f * (k == 0 ? 1 : -1), a(col, 0.7f * (1f - ph) * fade));
        }
        // eight standing staff lines round the rim, swaying
        int n = ctx.seg(8, 4);
        for (int i = 0; i < n; i++) {
            float ang = Mth.TWO_PI * i / n + age * 0.01f, sway = Mth.sin(age * 0.1f + i * 1.3f);
            float hgt = (1.2f + 0.5f * h(inst, i, 2) + 0.15f * sway) * Math.min(1.6f, 0.6f + R * 0.12f) * open;
            Vector3f foot = new Vector3f(Mth.cos(ang) * r * 0.98f, 0.05f, Mth.sin(ang) * r * 0.98f).add(c);
            Vector3f top = new Vector3f(foot).add(0, hgt, 0);
            buf.beam(ctx, STAFF_V, VfxBlend.ADD, foot, top, 0.3f, 0.12f, 1, age * 0.01f * (i % 2 == 0 ? 1 : -1), a(col, 0.75f * fade), a(hot(col, 0.5f), 0.1f));
        }
        // a clef hovering in the middle
        Vector3f mid = new Vector3f(c).add(0, 1.7f + 0.12f * Mth.sin(age * 0.08f), 0);
        glow(buf, ctx, mid, 1.3f, col, 0.35f * fade);
        buf.billboard(ctx, CLEF, VfxBlend.ADD, mid, 1.35f * Math.min(1.4f, 0.7f + R * 0.1f), 0f, a(hot(col, 0.6f), 0.9f * fade));
        // notes rising in spirals
        int m = ctx.seg(12, 5);
        for (int i = 0; i < m; i++) {
            float ph = (age * 0.014f + h(inst, i, 3)) % 1f, ang = h(inst, i, 4) * Mth.TWO_PI + ph * 3.2f, rr = r * (0.2f + 0.7f * h(inst, i, 5));
            Vector3f p = new Vector3f(Mth.cos(ang) * rr, 0.2f + ph * 2.6f, Mth.sin(ang) * rr).add(c);
            buf.billboard(ctx, NOTE, VfxBlend.ADD, p, 0.4f + 0.1f * h(inst, i, 6), Mth.sin(ph * 6f + i) * 0.3f, a(i % 3 == 0 ? GOLD : col, 0.9f * Mth.sin(ph * Mth.PI) * fade));
        }
    }

    // ------------------------------------------------------------------ FX3: Finale
    private void finale(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), d = Math.max(8f, inst.duration), t = Mth.clamp(age / d, 0f, 1f);
        float u = Mth.clamp(inst.power, 0.4f, 6f) / 3f;
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f dirHint = ctx.rel(inst.to(ctx)).sub(c);
        int col = inst.color | 0xFF000000;
        float flash = 1f - ramp(t, 0f, 0.3f);
        Vector3f mid = new Vector3f(c).add(0, 0.9f * u, 0);
        glow(buf, ctx, mid, (1.6f + 1.4f * out(t)) * u, col, 0.6f * (1f - t));
        glow(buf, ctx, mid, 1.3f * u * flash, WHITE, 0.95f * flash);
        // the burst of rays, opening and turning
        buf.billboard(ctx, BURST, VfxBlend.ADD, mid, (0.6f + 2.6f * out(ramp(t, 0f, 0.6f))) * u, age * 0.08f, a(hot(col, 0.4f), 0.95f * (1f - ramp(t, 0.35f, 1f))));
        buf.billboard(ctx, BURST, VfxBlend.ADD, mid, (0.4f + 1.8f * out(ramp(t, 0f, 0.6f))) * u, -age * 0.1f, a(WHITE, 0.6f * (1f - ramp(t, 0.2f, 0.8f))));
        // the ground: the staff ring flares, a flat sound ring and an upright one
        float k1 = out(ramp(t, 0f, 0.7f));
        ground(buf, RING, c, 0.05f, 2.4f * u * k1, age * 0.05f, a(col, 0.9f * (1f - ramp(t, 0.5f, 1f))));
        ground(buf, WAVE, c, 0.07f, 3.2f * u * out(ramp(t, 0.05f, 0.85f)), 0f, a(hot(col, 0.3f), 0.8f * (1f - ramp(t, 0.3f, 1f))));
        buf.plane(WAVE, VfxBlend.ADD, VfxPose.facing(new Vector3f(mid), new Vector3f(ctx.camRight).cross(ctx.camUp)).spin(age * 0.06f),
                2.0f * u * out(ramp(t, 0.1f, 0.9f)), a(col, 0.7f * (1f - ramp(t, 0.3f, 1f))));
        // notes flung along arcs and landing
        int n = ctx.seg(12, 6);
        for (int i = 0; i < n; i++) {
            float ang = h(inst, i, 1) * Mth.TWO_PI, sp = 0.8f + 1.8f * h(inst, i, 2);
            if (dirHint.lengthSquared() > 0.5f) ang = ang * 0.5f + (float) Math.atan2(dirHint.z, dirHint.x) * 0.5f;
            float f = ramp(t, 0.02f, 1f), x = Mth.cos(ang) * sp * out(f) * u * 1.3f, z = Mth.sin(ang) * sp * out(f) * u * 1.3f;
            float y = (0.4f + 1.9f * Mth.sin(Mth.PI * f * 0.9f) * (0.5f + h(inst, i, 3))) * u;
            buf.billboard(ctx, NOTE, VfxBlend.ADD, new Vector3f(x, y, z).add(c), (0.36f + 0.14f * h(inst, i, 4)) * u, Mth.sin(f * 7f + i) * 0.5f, a(i % 3 == 0 ? GOLD : col, 0.95f * (1f - ramp(f, 0.6f, 1f))));
        }
        // a clef rises out of the middle
        float rise = out(ramp(t, 0.1f, 0.8f));
        buf.billboard(ctx, CLEF, VfxBlend.ADD, new Vector3f(mid).add(0, 0.6f * u + 1.6f * u * rise, 0), 1.5f * u * (0.5f + 0.5f * rise), 0f, a(hot(col, 0.5f), 0.9f * (1f - ramp(t, 0.55f, 1f))));
    }

    // ------------------------------------------------------------------ SONG_NOTES: the singer's notes (power = size)
    private void notes(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), S = Math.max(0.5f, inst.power);
        int col = inst.color | 0xFF000000;
        Vector3f o = ctx.rel(inst.to(ctx));
        int n = ctx.seg(Math.round(6 * S), 3);
        for (int k = 0; k < n; k++) {
            float start = h(inst, k, 1) * 0.35f, ph = (age / inst.duration - start) / (1f - start);
            if (ph <= 0f || ph >= 1f) continue;
            float ang = h(inst, k, 2) * Mth.TWO_PI + ph * 2.4f, r = 0.5f + 0.9f * ph + 0.3f * h(inst, k, 3), fade = Mth.sin(Mth.PI * ph), size = 0.4f * Math.min(S, 1.8f);
            Vector3f p = new Vector3f(o).add(new Vector3f(ctx.camRight).mul(Mth.cos(ang) * r)).add(new Vector3f(ctx.camUp).mul(ph * 1.8f * S + Mth.sin(ang) * r * 0.25f));
            glow(buf, ctx, p, size * 2.2f, col, 0.35f * fade);
            buf.billboard(ctx, NOTE, VfxBlend.ADD, p, size, Mth.sin(ph * 5f + k) * 0.35f, a(k % 3 == 0 ? hot(GOLD, 0.2f) : hot(col, 0.4f), 0.95f * fade));
        }
        float top = ramp(age / inst.duration, 0.3f, 0.7f) * (1f - ramp(age / inst.duration, 0.7f, 1f));       // a clef crowns the spiral
        if (top > 0.02f) buf.billboard(ctx, CLEF, VfxBlend.ADD, new Vector3f(o).add(0, 1.9f * S + 0.2f, 0), 0.9f * Math.min(S, 1.6f), 0f, a(hot(col, 0.5f), 0.8f * top));
    }

    // ------------------------------------------------------------------ SONG_RING: one pulse (power = radius)
    private void ring(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), R = Math.max(1f, inst.power), t = Mth.clamp(age / inst.duration, 0f, 1f);
        int col = inst.color | 0xFF000000;
        Vector3f c = ctx.rel(inst.from(ctx));
        for (int k = 0; k < 2; k++) {
            float tt = ramp(t, k * 0.18f, 1f);
            if (tt <= 0f) continue;
            float r = R * out(tt), fade = (1f - tt) * (k == 0 ? 1f : 0.6f);
            ground(buf, RING, c, 0.06f + 0.01f * k, r, age * 0.04f * (k == 0 ? 1 : -1), a(k == 0 ? hot(col, 0.3f) : col, 0.9f * fade));
            ground(buf, WAVE, c, 0.08f, r * 0.96f, age * 0.05f, a(col, 0.5f * fade));
        }
        float flash = 1f - ramp(t, 0f, 0.35f);
        if (flash > 0f) ground(buf, GLOW, c, 0.05f, 1.6f + 0.1f * R, age * 0.05f, a(col, 0.55f * flash));
        float edge = R * out(t);
        int n = ctx.seg(10, 4);
        for (int k = 0; k < n; k++) {
            float ang = h(inst, k, 1) * Mth.TWO_PI, rise = (1f - t) * h(inst, k, 2) * 0.9f + 0.1f;
            Vector3f p = new Vector3f(c).add(Mth.cos(ang) * edge, rise * (0.4f + t), Mth.sin(ang) * edge);
            buf.billboard(ctx, NOTE, VfxBlend.ADD, p, 0.4f, age * 0.1f + k, a(k % 3 == 0 ? WHITE : col, 0.85f * (1f - t)));
        }
    }
}
