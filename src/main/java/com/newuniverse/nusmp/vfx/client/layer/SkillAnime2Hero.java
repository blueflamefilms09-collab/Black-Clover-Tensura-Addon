package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.HeroSolid;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

import static com.newuniverse.nusmp.vfx.client.layer.SkillAnimeLayer.*;
import static com.newuniverse.nusmp.vfx.client.layer.SkillAnime2Layer.*;

/**
 * ANIME2_HERO for {@link SkillAnime2Layer}: the hero weapon techniques (heroweapon/, 21 weapons, about 150 techniques) in one signature per MOTIF, tinted in the weapon's own colour (the server sends
 * {@code HeroColors.argb}). A motif is the way the weapon moves, a variant is its own form: SLASH (0 blade, 1 claw, 2 fan, 3 scythe, 4 katana), THRUST (0 spear, 1 harpoon, 2 arrow, 3 projectile),
 * SHOCK (0 hammer, 1 gauntlet, 2 carriage), WARD (the shield), CAST (0 staff, 1 book, 2 instrument), THREAD (0 whip, 1 chain, 2 sewing set), SHARD (the mirror). An ultimate technique is the same
 * motif bigger with a rune halo behind the wielder. {@code from} = the wielder's chest, {@code to} = where the technique is aimed. Worst case about 60 quads (the preview measures it).
 */
final class SkillAnime2Hero {
    private SkillAnime2Hero() {}

    static void draw(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = dirOr(a, b, new Vector3f(0, 0, 1));
        int v = SkillAnime2Vfx.heroVariant(inst.power);
        boolean ult = SkillAnime2Vfx.heroUltimate(inst.power);
        float k = ult ? 1.4f : 1f, fade = life(inst, age, 0, 7);
        if (ult) buf.plane(RUNE, VfxBlend.ADD, VfxPose.facing(new Vector3f(dir).mul(0.5f).add(a), dir).spin(age * 0.07f), 1.5f * VfxAnim.easeOutBack(ramp(age, 0, 5f)), al(inst.color, 0.6f * fade));
        switch (SkillAnime2Vfx.heroMotif(inst.power)) {
            case 1 -> thrust(inst, ctx, buf, age, a, b, dir, v, k, fade);
            case 2 -> shock(inst, ctx, buf, age, a, b, dir, v, k, fade);
            case 3 -> ward(inst, ctx, buf, age, a, dir, k, fade);
            case 4 -> cast(inst, ctx, buf, age, a, b, dir, v, k, fade);
            case 5 -> thread(inst, ctx, buf, age, a, b, dir, v, k, fade);
            case 6 -> shard(inst, ctx, buf, age, a, b, dir, k, fade);
            default -> slash(inst, ctx, buf, age, a, b, dir, v, k, fade);
        }
    }

    private static void impact(VfxRenderContext ctx, VfxVertexBuffer buf, float age, float from, Vector3f at, float size, int col, float fade) {
        float land = age - from;
        if (land < 0 || land > 8f) return;
        flash(ctx, buf, at, size, land * 0.35f, col, (1 - land / 8f) * fade);
    }

    // ------------------------------------------------------------------ SLASH: a blade, claws, a fan, a scythe, a katana
    private static void slash(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f a, Vector3f b, Vector3f dir, int v, float k, float fade) {
        int col = inst.color;
        float reach = Math.min(3.2f, a.distance(b) * 0.7f + 1.2f), sweep = VfxAnim.easeOutCubic(ramp(age, 0, 6f));
        Vector3f place = new Vector3f(dir).mul(reach).add(a);
        Vector3f side = sideOf(dir);
        float size = (v == 3 ? 2.7f : 1.9f) * k;
        if (v == 1) {                                                                  // claws: three rake marks, one after another
            for (int c = 0; c < 3; c++) {
                float f = ramp(age, c * 1.5f, 3f) * Mth.clamp(1 - (age - c * 1.5f - 8f) / 5f, 0, 1) * fade;
                if (f <= 0.01f) continue;
                Vector3f at = new Vector3f(place).add(new Vector3f(side).mul((c - 1) * 0.5f * k)).add(0, 0.2f * (c - 1), 0);
                buf.billboard(ctx, CLAW, VfxBlend.ALPHA, at, size * 0.9f, 0.5f - c * 0.45f, al(INK, 0.8f * f));
                buf.billboard(ctx, CLAW, VfxBlend.ADD, at, size * 0.9f, 0.5f - c * 0.45f, al(hot(col, 0.6f), 0.95f * f));
            }
        } else if (v == 4) {                                                           // katana: one straight cut, a white line with a flash
            float f = ramp(age, 0, 2f) * Mth.clamp(1 - (age - 3f) / 6f, 0, 1) * fade;
            Vector3f lo = new Vector3f(place).add(new Vector3f(side).mul(-2.4f * k)).add(0, -0.9f * k, 0), hi = new Vector3f(place).add(new Vector3f(side).mul(2.4f * k)).add(0, 0.9f * k, 0);
            buf.beam(ctx, STREAK, VfxBlend.ADD, lo, hi, 0.5f * k, 0.5f * k, 1, 0, al(col, 0.6f * f), al(col, 0.6f * f));
            buf.beam(ctx, BOLT, VfxBlend.ADD, lo, hi, 0.16f, 0.16f, 1, 0, al(WHITE, f), al(WHITE, f));
            flash(ctx, buf, place, 1.4f * k, age * 0.2f, col, f);
        } else {                                                                       // blade, scythe, fan: a great crescent swings through
            float tilt = (v == 3 ? 0.9f : -0.5f) + (sweep - 0.5f) * 1.5f;
            Vector3f at = new Vector3f(dir).mul(0.9f * sweep).add(place);
            VfxPose p = VfxPose.facing(at, dir).spin(tilt);
            buf.plane(CRESCENT, VfxBlend.ALPHA, p, size * 0.95f * (0.6f + 0.4f * sweep), al(INK, 0.4f * fade));
            buf.plane(CRESCENT, VfxBlend.ADD, p, size * (0.6f + 0.4f * sweep), al(col, 0.85f * fade));
            buf.plane(CRESCENT, VfxBlend.ADD, p.spin(0.05f), size * 0.9f * (0.6f + 0.4f * sweep), al(hot(col, 0.8f), 0.9f * fade));
            if (v == 2) {                                                              // the fan: petals ride the gust
                int n = ctx.seg(8, 4);
                RandomSource r = inst.random();
                for (int i = 0; i < n; i++) {
                    float ph = Mth.frac(age * 0.07f + r.nextFloat()), ang = r.nextFloat() * 2f - 1f;
                    Vector3f pt = new Vector3f(dir).mul(reach + ph * 3.5f).add(new Vector3f(side).mul(ang * (0.8f + ph * 1.6f))).add(0, (r.nextFloat() - 0.5f) * 1.2f, 0).add(a);
                    buf.billboard(ctx, PETAL, VfxBlend.ALPHA, pt, 0.35f, age * 0.2f + i, al(hot(col, 0.3f), Mth.sin(ph * Mth.PI) * fade));
                }
            }
        }
        impact(ctx, buf, age, 4f, b, 1.2f * k, col, fade);
        int n = ctx.seg(4, 2);
        for (int i = 0; i < n; i++) {
            float ph = ramp(age, 3f + i, 8f);
            if (ph > 0 && ph < 1) buf.billboard(ctx, SPARKLE, VfxBlend.ADD, new Vector3f(b).add(0.5f * (i - 1.5f), 0.4f * ph, 0.2f * i), 0.5f, age * 0.3f + i, al(hot(col, 0.5f), (1 - ph) * fade));
        }
    }

    // ------------------------------------------------------------------ THRUST: a spear, a harpoon, an arrow, a thrown orb
    private static void thrust(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f a, Vector3f b, Vector3f dir, int v, float k, float fade) {
        int col = inst.color;
        float grow = VfxAnim.easeOutCubic(ramp(age, 0, 5f));
        Vector3f head = lerp(a, b, grow), tail = lerp(a, b, Math.max(0, grow - 0.7f));
        if (v == 3) {                                                                  // the thrown orb
            buf.billboard(ctx, GLOW, VfxBlend.ADD, head, 1.7f * k, 0, al(col, 0.6f * fade));
            buf.billboard(ctx, CORE, VfxBlend.ALPHA, head, 0.7f * k, 0, al(col, fade));
            buf.billboard(ctx, CORE, VfxBlend.ADD, head, 0.4f * k, 0, al(WHITE, fade));
            buf.beam(ctx, STREAK, VfxBlend.ADD, tail, head, 0.3f, 1.8f * k, 1, 0, al(col, 0.1f * fade), al(col, 0.7f * fade));
        } else {
            buf.beam(ctx, STREAK, VfxBlend.ALPHA, tail, head, 0.3f, 1.6f * k, 1, 0, al(col, 0.1f * fade), al(col, 0.8f * fade));
            buf.beam(ctx, BOLT, VfxBlend.ADD, tail, head, 0.1f, 0.6f * k, 1, 0, al(col, 0.1f * fade), al(WHITE, fade));
            buf.billboard(ctx, SPARKLE, VfxBlend.ADD, head, 1.0f * k, age * 0.4f, al(hot(col, 0.7f), fade));
            if (grow > 0.02f && fade > 0.05f && HeroSolid.solid() && HeroSolid.takeSlot(ctx)) solidPoint(buf, head, dir, v, k, col, Math.min(1f, fade * 3f));   // docs/heroweapon/flat_parts.md: a real tip / harpoon head / arrow leads the streak
        }
        for (int j = 1; j <= 4; j++) {                                                 // sonic rings shed behind the point
            float u = grow - 0.12f * j;
            if (u <= 0) continue;
            float r = 0.25f * j * k + 0.05f * age;
            buf.ring(RING, VfxBlend.ADD, VfxPose.facing(lerp(a, b, u), dir), r * 0.8f, r, ctx.seg(10, 6), 2, 0, al(col, 0.55f * fade * (1 - 0.18f * j)));
        }
        RandomSource r = inst.random();
        int n = ctx.seg(v == 1 || v == 2 ? 6 : 3, 2);
        for (int i = 0; i < n; i++) {                                                  // harpoon: water spray, arrow: feathers shed
            float ph = Mth.frac(age * 0.08f + r.nextFloat());
            Vector3f at = lerp(a, head, 0.2f + 0.7f * r.nextFloat()).add((r.nextFloat() - 0.5f) * 0.8f, (r.nextFloat() - 0.5f) * 0.8f - ph * 0.5f, (r.nextFloat() - 0.5f) * 0.8f);
            if (v == 1) buf.billboard(ctx, DROPLET, VfxBlend.ALPHA, at, 0.3f, i * 1.1f, al(col, (1 - ph) * fade));
            else if (v == 2) buf.billboard(ctx, FEATHER, VfxBlend.ALPHA, at, 0.5f, i * 1.1f + age * 0.1f, al(col, (1 - ph) * fade));
            else buf.billboard(ctx, SPARKLE, VfxBlend.ADD, at, 0.3f, age * 0.3f, al(hot(col, 0.5f), (1 - ph) * fade));
        }
        impact(ctx, buf, age, 5f, b, 1.5f * k, col, fade);
        if (grow >= 1f) buf.ring(RING, VfxBlend.ADD, VfxPose.ground(new Vector3f(b).add(0, 0.08f, 0)), 0.3f * k + (age - 5f) * 0.14f, 0.6f * k + (age - 5f) * 0.3f, ctx.seg(12, 6), 3, 0, al(col, Math.max(0, 1 - (age - 5f) / 9f) * fade));
    }

    /** The solid point of a thrust: 0 spear tip, 1 harpoon head (barbed), 2 arrow. Same models as HERO_SPEAR / HERO_ARROW (at most {@link HeroSolid#MAX_QUADS_EACH} quads, one slot of the frame budget). */
    private static void solidPoint(VfxVertexBuffer buf, Vector3f head, Vector3f dir, int v, float k, int col, float alpha) {
        Vector3f centre = new Vector3f(head);
        if (v == 2) {
            float s = Mth.clamp(0.7f + 0.35f * k, 0.8f, 1.5f);
            centre.fma(-0.5f * s, dir);                                                // the arrow model is 1.0 long, centred, the point at +0.5
            SolidModels.arrow(buf, centre, dir, s, alpha, col);
        } else {
            float s = Mth.clamp(0.8f + 0.4f * k, 0.9f, 1.8f);
            centre.fma(-0.56f * s, dir);                                               // the collar is the origin, the point 0.56 ahead of it
            SolidModels.spearTip(buf, centre, dir, s, alpha, col, v == 1);
        }
    }

    // ------------------------------------------------------------------ SHOCK: a hammer, a gauntlet, a carriage
    private static void shock(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f a, Vector3f b, Vector3f dir, int v, float k, float fade) {
        int col = inst.color;
        float open = VfxAnim.easeOutCubic(ramp(age, 2f, 9f)), R = 2.6f * k;
        Vector3f c = new Vector3f(b.x, Math.min(b.y, a.y - 1.0f), b.z);                    // the ground under the aim (the wielder's feet level)
        Vector3f ground = new Vector3f(c).add(0, 0.08f, 0);
        VfxPose ring = v == 1 ? VfxPose.facing(new Vector3f(b), dir) : VfxPose.ground(ground);
        buf.ring(RING, VfxBlend.ADD, ring, R * open * 0.85f, R * open, ctx.seg(18, 9), 4, 0, al(col, 0.85f * fade * (1 - 0.5f * open)));
        buf.ring(RING, VfxBlend.ALPHA, ring.lift(0.02f), R * open * 0.5f, R * open * 0.6f, ctx.seg(14, 7), 3, age * 0.03f, al(col, 0.5f * fade));
        if (v == 2) buf.plane(RUNE, VfxBlend.ADD, VfxPose.ground(new Vector3f(ground).add(0, 0.03f, 0)).spin(age * 0.12f), R * 0.8f * open, al(col, 0.6f * fade));
        flash(ctx, buf, new Vector3f(b).add(0, 0.4f, 0), 2.0f * k, age * 0.25f, col, (1 - ramp(age, 2f, 11f)) * fade);
        RandomSource r = inst.random();
        int n = ctx.seg(7, 4);
        for (int i = 0; i < n; i++) {                                                  // rock chunks fly up and fall back, dust blooms
            float ph = ramp(age, 2f + i * 0.2f, 14f);
            if (ph <= 0 || ph >= 1) continue;
            float ang = Mth.TWO_PI * i / n + r.nextFloat(), rad = R * 0.7f * ph * (0.5f + r.nextFloat());
            Vector3f at = new Vector3f(Mth.cos(ang) * rad, 2.2f * Mth.sin(ph * Mth.PI) * (0.5f + r.nextFloat()), Mth.sin(ang) * rad).add(c);
            buf.billboard(ctx, ROCK, VfxBlend.ALPHA, at, 0.42f * k, ang + age * 0.2f, al(VfxVertexBuffer.lerpColor(col, 0xFF8A7A66, 0.6f), (1 - ph * 0.4f) * fade));
            if (i % 2 == 0) buf.billboard(ctx, MIST, VfxBlend.ALPHA, new Vector3f(at.x, c.y + 0.3f + 0.5f * ph, at.z), 1.1f * k, ang, al(0xFFC8B8A0, 0.35f * (1 - ph) * fade));
        }
        int cracks = ctx.seg(5, 3);
        for (int i = 0; i < cracks && v != 1; i++) {                                    // cracks run out along the ground
            float ang = Mth.TWO_PI * i / cracks + 0.5f;
            buf.beam(ctx, STREAK, VfxBlend.ALPHA, ground, new Vector3f(Mth.cos(ang), 0, Mth.sin(ang)).mul(R * 1.1f * open).add(ground), 0.3f, 0.06f, 1, 0, al(INK, 0.7f * fade), al(INK, 0.05f));
        }
    }

    // ------------------------------------------------------------------ WARD: the shield's hex barrier
    private static void ward(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f a, Vector3f dir, float k, float fade) {
        int col = inst.color;
        float open = VfxAnim.easeOutBack(ramp(age, 0, 5f)), size = 1.9f * k;
        Vector3f at = new Vector3f(dir).mul(1.9f).add(a);
        VfxPose p = VfxPose.facing(at, dir);
        buf.plane(HEX, VfxBlend.ALPHA, p.spin(age * 0.01f), size * open, al(col, 0.85f * fade));
        buf.plane(HEX, VfxBlend.ADD, p, size * 1.08f * open, al(hot(col, 0.5f), 0.55f * fade));
        buf.billboard(ctx, GLOW, VfxBlend.ADD, at, size * 2f, 0, al(col, 0.2f * fade));
        for (int j = 0; j < 2; j++) {                                                  // ripples run out over the barrier
            float ph = ramp(age, j * 4f, 9f);
            if (ph > 0 && ph < 1) buf.ring(RING, VfxBlend.ADD, p.lift(0.03f), size * ph * 0.8f, size * ph * 0.9f, ctx.seg(14, 7), 3, 0, al(col, (1 - ph) * fade));
        }
        flash(ctx, buf, at, 1.6f * k, age * 0.2f, col, 1 - ramp(age, 0, 8f));
        RandomSource r = inst.random();
        int n = ctx.seg(5, 3);
        for (int i = 0; i < n; i++) {
            float ph = Mth.frac(age * 0.06f + r.nextFloat());
            buf.billboard(ctx, SPARKLE, VfxBlend.ADD, new Vector3f(p.point((r.nextFloat() - 0.5f) * size * 1.6f, (r.nextFloat() - 0.5f) * size * 1.6f)).add(0, ph * 0.5f, 0), 0.35f, age * 0.2f + i, al(hot(col, 0.5f), Mth.sin(ph * Mth.PI) * fade));
        }
    }

    // ------------------------------------------------------------------ CAST: a staff, a book, an instrument
    private static void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f a, Vector3f b, Vector3f dir, int v, float k, float fade) {
        int col = inst.color;
        float open = VfxAnim.easeOutBack(ramp(age, 0, 6f)), R = 1.3f * k;
        Vector3f at = new Vector3f(dir).mul(1.3f).add(a);
        VfxPose p = VfxPose.facing(at, dir);
        buf.plane(RUNE, VfxBlend.ALPHA, p.spin(age * 0.06f), R * open, al(col, 0.85f * fade));
        buf.plane(RUNE, VfxBlend.ADD, p.spin(-age * 0.09f), R * 0.75f * open, al(hot(col, 0.5f), 0.7f * fade));
        buf.billboard(ctx, GLOW, VfxBlend.ADD, at, R * 2.2f, 0, al(col, 0.25f * fade));
        int n = ctx.seg(5, 3);
        for (int i = 0; i < n; i++) {                                                  // sparks, pages or notes orbit the circle
            float ang = Mth.TWO_PI * i / n + age * 0.12f, rr = R * (1.05f + 0.1f * Mth.sin(age * 0.3f + i));
            Vector3f o = p.point(Mth.cos(ang) * rr, Mth.sin(ang) * rr);
            if (v == 1) buf.billboard(ctx, CARD, VfxBlend.ALPHA, o, 0.5f, ang, al(hot(col, 0.6f), 0.9f * fade));
            else if (v == 2) buf.billboard(ctx, NOTE, VfxBlend.ALPHA, o, 0.55f, 0.3f * Mth.sin(ang), al(col, fade));
            else buf.billboard(ctx, SPARKLE, VfxBlend.ADD, o, 0.45f, ang, al(hot(col, 0.5f), fade));
        }
        float shot = ramp(age, 5f, 5f);
        if (shot > 0f) {                                                               // the spell leaves the circle for the aim
            Vector3f head = lerp(at, b, VfxAnim.easeOutCubic(shot));
            buf.beam(ctx, STREAK, VfxBlend.ADD, at, head, 0.7f * k, 0.3f * k, 1, 0, al(col, 0.55f * fade), al(hot(col, 0.6f), 0.8f * fade));
            buf.billboard(ctx, GLOW, VfxBlend.ADD, head, 1.2f * k, 0, al(col, 0.6f * fade));
            impact(ctx, buf, age, 10f, b, 1.5f * k, col, fade);
        }
    }

    // ------------------------------------------------------------------ THREAD: a whip, a chain, a sewing set
    private static void thread(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f a, Vector3f b, Vector3f dir, int v, float k, float fade) {
        int col = inst.color;
        float grow = VfxAnim.easeOutCubic(ramp(age, 0, 5f));
        Vector3f head = lerp(a, b, grow), side = sideOf(dir);
        int seg = ctx.seg(8, 4), strands = v == 2 ? 3 : 1;
        for (int s = 0; s < strands; s++) {
            Vector3f prev = new Vector3f(a);
            for (int i = 1; i <= seg; i++) {
                float t = i / (float) seg;
                float wave = v == 0 ? Mth.sin(t * 9f - age * 0.9f) * 0.6f * (1 - t * 0.6f) : v == 1 ? Mth.sin(t * 3f) * 0.25f : (s - 1) * 0.5f * (1 - t);
                Vector3f p = lerp(a, head, t).add(new Vector3f(side).mul(wave)).add(0, Mth.sin(t * Mth.PI) * (v == 0 ? 0.5f : 0.2f), 0);
                if (v == 1) buf.beam(ctx, CHAIN, VfxBlend.ALPHA, prev, p, 0.26f * k, 0.26f * k, 1, 0, al(col, fade), al(col, fade));
                else if (v == 2) buf.beam(ctx, STITCH, VfxBlend.ADD, prev, p, 0.2f * k, 0.2f * k, 1, 0, al(hot(col, 0.4f), 0.9f * fade), al(col, 0.9f * fade));
                else {
                    buf.beam(ctx, STREAK, VfxBlend.ALPHA, prev, p, 0.16f * k, 0.06f * k, 1, 0, al(INK, 0.8f * fade), al(INK, 0.8f * fade));
                    buf.beam(ctx, STREAK, VfxBlend.ADD, prev, p, 0.08f * k, 0.04f * k, 1, 0, al(col, 0.9f * fade), al(hot(col, 0.6f), 0.9f * fade));
                }
                prev = p;
            }
        }
        buf.billboard(ctx, CORE, VfxBlend.ADD, head, 0.5f * k, 0, al(hot(col, 0.6f), fade));
        impact(ctx, buf, age, v == 0 ? 5f : 4f, b, (v == 0 ? 1.8f : 1.2f) * k, col, fade);          // the whip's crack is the big flash
        if (v == 0 && age >= 5f && age < 9f) buf.ring(RING, VfxBlend.ADD, camPose(b), 0.3f + (age - 5f) * 0.25f, 0.5f + (age - 5f) * 0.4f, ctx.seg(10, 6), 2, 0, al(WHITE, (1 - (age - 5f) / 4f) * fade));
    }

    // ------------------------------------------------------------------ SHARD: the mirror's facets
    private static void shard(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f a, Vector3f b, Vector3f dir, float k, float fade) {
        int col = inst.color;
        Vector3f side = sideOf(dir), up = new Vector3f(dir).cross(side).normalize();
        Vector3f at = new Vector3f(dir).mul(1.1f).add(a);
        buf.ring(RING, VfxBlend.ADD, VfxPose.facing(at, dir), 0.9f * k * VfxAnim.easeOutBack(ramp(age, 0, 5f)), 1.05f * k * VfxAnim.easeOutBack(ramp(age, 0, 5f)), ctx.seg(16, 8), 4, age * 0.04f, al(col, 0.8f * fade));
        RandomSource r = inst.random();
        int n = ctx.seg(8, 4);
        for (int i = 0; i < n; i++) {                                                  // facets fly out of the ring, turn and glint
            float t0 = i * 0.6f, u = VfxAnim.easeOutCubic(ramp(age, t0, 8f));
            if (u <= 0) continue;
            float ang = Mth.TWO_PI * i / n;
            Vector3f from = new Vector3f(side).mul(Mth.cos(ang) * 0.8f * k).add(new Vector3f(up).mul(Mth.sin(ang) * 0.8f * k)).add(at);
            Vector3f to = new Vector3f(b).add((r.nextFloat() - 0.5f) * 1.8f, (r.nextFloat() - 0.5f) * 1.4f, (r.nextFloat() - 0.5f) * 1.8f);
            Vector3f p = lerp(from, to, u);
            buf.billboard(ctx, FACET, VfxBlend.ALPHA, p, 0.55f * k, age * 0.25f + i, al(col, fade * (1 - 0.5f * ramp(age, t0 + 8f, 6f))));
            if (((int) (age * 0.5f) + i) % 3 == 0) buf.billboard(ctx, SPARKLE, VfxBlend.ADD, p, 0.6f * k, age * 0.4f, al(WHITE, 0.9f * fade));
        }
        impact(ctx, buf, age, 7f, b, 1.5f * k, col, fade);
    }
}
