package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.ArcaneSpellLayer.panel;
import static com.newuniverse.nusmp.vfx.client.layer.ArcaneSpellLayer.stand;
import static com.newuniverse.nusmp.vfx.client.layer.ArcaneSpellLayer.t;
import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.*;

/**
 * 0.34, part two: Spatial (Red Room), Ash, Mirror, Cotton / Food, Shadow, Recombination and Rouge.
 * <ul>
 *   <li>SPACE_CUBE, Unopening Red Room: a box of warped space folds shut round the target, edges of light, shards drifting.</li>
 *   <li>ASH_FORMATION, Ash Absorbing Formation: a ring of ash turns on the ground, flakes spiralling in and smoke rising.</li>
 *   <li>MIRROR_RAY, Reflect Ray: a mirror pane catches the light and fires it as a beam.</li>
 *   <li>MIRROR_DOUBLE, Real Double: mirror-glass doubles of the caster circle them, glinting (power = how many).</li>
 *   <li>COTTON_SHEEP: Sleeping Sheep Strike (a cotton sheep bounds from 'from' to 'to' and bursts into cotton) or, when from ==
 *       to, Sheep Cook (sheep chefs bobbing round the caster).</li>
 *   <li>FOOD_MAW, Glutton's Banquet: a giant maw opens, sucks everything in, snaps shut.</li>
 *   <li>SHADOW_POOL, Dark Garden Invitation / Kids' Playground: an inky pool spreads, shadow hands reach up out of it.</li>
 *   <li>SHADOW_UNITE, Unite Mode: a devil-dark aura round the caster, wisps rising, the devil's eyes in the dark.</li>
 *   <li>RECOMBINE_CONSTRUCT, The Raging Black Bull: planks of the magic house fly in and lock into a hulking frame.</li>
 *   <li>ROUGE_CAT, Red Thread of Fate: Rouge, woven of red thread, sits on the caster's head while threads cross through space
 *       around her, sparkling where they meet; power >= 2 plays her unravelling into flying threads.</li>
 * </ul>
 */
public class ArcaneSpellLayer2 extends AbstractVfxLayer {
    static final ResourceLocation PORTAL = t("arc_portal"), SMOKE = t("arc_smoke"), EMBER = t("arc_ember"), GLASS = t("arc_glass"),
            SILHOUETTE = t("arc_silhouette"), COTTON = t("arc_cotton"), SHEEP = t("arc_sheep"), MAW = t("arc_maw"), HAND = t("arc_hand"),
            PLANK = t("arc_plank"), ROUGE = t("arc_rouge"), ALCHEMY = t("arc_alchemy");

    @Override
    public Set<VfxShape> shapes() {
        return EnumSet.of(VfxShape.SPACE_CUBE, VfxShape.ASH_FORMATION, VfxShape.MIRROR_RAY, VfxShape.MIRROR_DOUBLE, VfxShape.COTTON_SHEEP,
                VfxShape.FOOD_MAW, VfxShape.SHADOW_POOL, VfxShape.SHADOW_UNITE, VfxShape.RECOMBINE_CONSTRUCT, VfxShape.ROUGE_CAT);
    }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case SPACE_CUBE -> 100;
            case ASH_FORMATION -> 160;
            case MIRROR_RAY -> 16;
            case MIRROR_DOUBLE -> 200;
            case COTTON_SHEEP -> 24;
            case FOOD_MAW -> 40;
            case SHADOW_POOL -> 60;
            case SHADOW_UNITE -> 400;
            case RECOMBINE_CONSTRUCT -> 40;
            case ROUGE_CAT -> 120;
            default -> 30;
        };
    }

    @Override
    public int defaultColor(VfxShape s) {
        return switch (s) {
            case SPACE_CUBE -> 0xFFE0304A;
            case ASH_FORMATION -> 0xFF8A847C;
            case MIRROR_RAY, MIRROR_DOUBLE -> 0xFFC8F0FF;
            case COTTON_SHEEP -> 0xFFFFF6FA;
            case FOOD_MAW -> 0xFFFF9AB0;
            case SHADOW_POOL, SHADOW_UNITE -> 0xFF2A2440;
            case RECOMBINE_CONSTRUCT -> 0xFFB08050;
            case ROUGE_CAT -> 0xFFCC1C3F;
            default -> 0xFFFFFFFF;
        };
    }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.FOOD_MAW) VfxShake.add(inst.payload.from(), 0.6f, 10);
        else if (inst.shape == VfxShape.RECOMBINE_CONSTRUCT) VfxShake.add(inst.payload.from(), 0.9f, 12);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case SPACE_CUBE -> cube(inst, ctx, buf);
            case ASH_FORMATION -> ash(inst, ctx, buf);
            case MIRROR_RAY -> ray(inst, ctx, buf);
            case MIRROR_DOUBLE -> doubles(inst, ctx, buf);
            case COTTON_SHEEP -> sheep(inst, ctx, buf);
            case FOOD_MAW -> maw(inst, ctx, buf);
            case SHADOW_POOL -> pool(inst, ctx, buf);
            case SHADOW_UNITE -> unite(inst, ctx, buf);
            case RECOMBINE_CONSTRUCT -> construct(inst, ctx, buf);
            case ROUGE_CAT -> rouge(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ Unopening Red Room
    private void cube(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), hs = inst.power;
        float fold = VfxAnim.easeOutBack(Mth.clamp(age / 10f, 0, 1));
        float fade = life(inst, age, 0, 12);
        Vector3f c = ctx.rel(inst.from(ctx)).add(0, hs, 0);
        int col = inst.color, light = VfxVertexBuffer.whiten(col, 0.5f);
        float h = hs * fold;
        float spin = age * 0.01f;
        Vector3f x = new Vector3f(Mth.cos(spin), 0, Mth.sin(spin)), z = new Vector3f(-Mth.sin(spin), 0, Mth.cos(spin)), y = new Vector3f(0, 1, 0);
        // the six faces: translucent warped space
        Vector3f[][] faces = {{x, y, z}, {new Vector3f(x).negate(), y, z}, {z, y, x}, {new Vector3f(z).negate(), y, x}, {y, x, z}, {new Vector3f(y).negate(), x, z}};
        for (Vector3f[] f : faces) {
            Vector3f centre = new Vector3f(c).add(new Vector3f(f[0]).mul(h));
            panel(buf, PORTAL, VfxBlend.ALPHA, centre, f[2], f[1], h, h, VfxVertexBuffer.withAlpha(ElementFx.mulRgb(col, 0.5f), 0.35f * fade));
        }
        // the twelve edges of light
        for (int i = 0; i < 12; i++) {
            int axis = i / 4, a = (i & 1) == 0 ? -1 : 1, b = (i & 2) == 0 ? -1 : 1;
            Vector3f ax = axis == 0 ? x : axis == 1 ? y : z, u = axis == 0 ? y : x, v = axis == 2 ? y : z;
            Vector3f base = new Vector3f(c).add(new Vector3f(u).mul(a * h)).add(new Vector3f(v).mul(b * h));
            streak(buf, ctx, WIND_STREAK, VfxBlend.ADD, new Vector3f(base).sub(new Vector3f(ax).mul(h)), new Vector3f(base).add(new Vector3f(ax).mul(h)), 0.12f * hs,
                    VfxVertexBuffer.withAlpha(light, fade));
        }
        int n = ctx.seg(8, 4);
        for (int i = 0; i < n; i++) {
            float lt = ((age * 0.03f) + hash(inst.seed, i, 1)) % 1f;
            Vector3f q = new Vector3f(c).add((hash(inst.seed, i, 2) - 0.5f) * 2 * h, (lt - 0.5f) * 2 * h, (hash(inst.seed, i, 3) - 0.5f) * 2 * h);
            buf.billboard(ctx, VfxTextures.SHARD, VfxBlend.ADD, q, 0.3f * hs, age * 0.1f + i, VfxVertexBuffer.withAlpha(light, 0.7f * fade * Mth.sin(lt * Mth.PI)));
        }
        VfxBloom.glow(ctx, buf, c, 0.8f * hs, col, 0.4f * fade);
    }

    // ------------------------------------------------------------------ Ash Absorbing Formation
    private void ash(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), r = inst.power;
        float open = VfxAnim.easeOutCubic(Mth.clamp(age / 12f, 0, 1));
        float fade = life(inst, age, 0, 16);
        Vector3f g = ctx.rel(inst.from(ctx)).add(0, 0.05f, 0);
        int col = inst.color, dark = ElementFx.mulRgb(col, 0.45f), light = VfxVertexBuffer.whiten(col, 0.3f);
        VfxPose ground = VfxPose.ground(g);
        buf.plane(ALCHEMY, VfxBlend.ALPHA, ground.spin(-age * 0.02f), r * open, VfxVertexBuffer.withAlpha(dark, 0.7f * fade));
        hoop(buf, SMOKE, VfxBlend.ALPHA, ground.lift(0.35f), r * 1.05f * open, r * 0.9f * open, 0.35f, ctx.seg(14, 8), 3, age * 0.03f,
                VfxVertexBuffer.withAlpha(col, 0.75f * fade));
        // flakes spiralling into the centre
        int n = ctx.seg(22, 10);
        for (int i = 0; i < n; i++) {
            float ph = hash(inst.seed, i, 1) * 30f, lt = ((age + ph) % 30f) / 30f;
            float a = hash(inst.seed, i, 2) * Mth.TWO_PI + lt * 5f, rad = r * (1.1f - lt) * open;
            Vector3f q = new Vector3f(g).add(Mth.cos(a) * rad, 0.2f + 1.4f * lt * lt, Mth.sin(a) * rad);
            buf.billboard(ctx, EMBER, VfxBlend.ALPHA, q, 0.22f, age * 0.3f + i, VfxVertexBuffer.withAlpha(i % 4 == 0 ? light : dark, fade * Mth.sin(lt * Mth.PI)));
        }
        // a column of grey smoke rising out of the middle
        int m = ctx.seg(5, 3);
        for (int i = 0; i < m; i++) {
            float lt = ((age * 0.02f) + i / (float) m) % 1f;
            buf.billboard(ctx, SMOKE, VfxBlend.ALPHA, new Vector3f(g).add(0, 0.5f + 2.5f * lt, 0), (1 + 1.5f * lt) * Math.min(r, 3f) * 0.6f, i + age * 0.01f,
                    VfxVertexBuffer.withAlpha(col, 0.6f * fade * (1 - lt)));
        }
    }

    // ------------------------------------------------------------------ Reflect Ray
    private void ray(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(b).sub(a);
        float len = dir.length();
        if (len < 0.01f) return;
        dir.div(len);
        float charge = Mth.clamp(age / 4f, 0, 1), fire = Mth.clamp((age - 4) / 3f, 0, 1);
        float fade = life(inst, age, 0, 8);
        int col = inst.color, light = VfxVertexBuffer.whiten(col, 0.6f);
        // the mirror: an upright pane facing along the ray
        Vector3f right = side(dir), up = new Vector3f(right).cross(dir).normalize();
        panel(buf, GLASS, VfxBlend.ALPHA, a, right, up, 0.7f * p * charge, 1.05f * p * charge, VfxVertexBuffer.withAlpha(light, 0.9f * fade));
        panel(buf, GLASS, VfxBlend.ADD, new Vector3f(a).add(new Vector3f(dir).mul(0.03f)), right, up, 0.7f * p * charge, 1.05f * p * charge,
                VfxVertexBuffer.withAlpha(col, 0.5f * fade));
        // the ray
        if (fire > 0) {
            Vector3f end = new Vector3f(a).add(new Vector3f(dir).mul(len * fire));
            buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, a, end, 1.4f * p, 1.0f * p, 3, 0, VfxVertexBuffer.withAlpha(col, 0.7f * fade), VfxVertexBuffer.withAlpha(col, 0.5f * fade));
            buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, a, end, 0.45f * p, 0.35f * p, 3, 0, VfxVertexBuffer.withAlpha(WHITE, fade), VfxVertexBuffer.withAlpha(WHITE, fade));
            int n = ctx.seg(8, 4);
            for (int i = 0; i < n; i++) {
                float f = (hash(inst.seed, i, 1) + age * 0.08f) % 1f;
                if (f > fire) continue;
                Vector3f q = new Vector3f(a).add(new Vector3f(dir).mul(len * f)).add(new Vector3f(right).mul((hash(inst.seed, i, 2) - 0.5f) * 0.8f * p));
                buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, q, 0.25f * p, age * 0.3f, VfxVertexBuffer.withAlpha(light, fade));
            }
            VfxBloom.glow(ctx, buf, end, 0.9f * p, col, fade);
        }
        VfxBloom.glow(ctx, buf, a, 0.8f * p * charge, col, fade);
    }

    // ------------------------------------------------------------------ Real Double
    private void doubles(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int count = Math.max(1, Math.min(4, Math.round(inst.power)));
        float appear = VfxAnim.easeOutBack(Mth.clamp(age / 8f, 0, 1));
        float fade = life(inst, age, 0, 12);
        Vector3f g = ctx.rel(inst.from(ctx));
        int col = inst.color, light = VfxVertexBuffer.whiten(col, 0.5f);
        for (int k = 0; k < count; k++) {
            float a = Mth.TWO_PI * k / count + age * 0.02f, rad = 1.6f * appear;
            Vector3f foot = new Vector3f(g).add(Mth.cos(a) * rad, 0.02f + 0.05f * Mth.sin(age * 0.1f + k), Mth.sin(a) * rad);
            float shimmer = 0.75f + 0.25f * Mth.sin(age * 0.25f + k * 2);
            stand(ctx, buf, SILHOUETTE, VfxBlend.ALPHA, foot, 0.9f, 1.85f, VfxVertexBuffer.withAlpha(light, 0.55f * fade * shimmer));
            stand(ctx, buf, SILHOUETTE, VfxBlend.ADD, foot, 0.95f, 1.9f, VfxVertexBuffer.withAlpha(col, 0.35f * fade));
            // a glint sweeping up the glass
            float gy = ((age * 0.04f) + k * 0.25f) % 1f;
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, new Vector3f(foot).add(0, 0.2f + 1.6f * gy, 0), 0.35f, age * 0.2f, VfxVertexBuffer.withAlpha(WHITE, fade * Mth.sin(gy * Mth.PI)));
            buf.ring(VfxTextures.GLOW, VfxBlend.ADD, VfxPose.ground(new Vector3f(foot).add(0, 0.03f, 0)), 0.35f, 0.5f, ctx.seg(10, 8), 1, 0, VfxVertexBuffer.withAlpha(col, 0.6f * fade));
        }
    }

    // ------------------------------------------------------------------ Sleeping Sheep Strike / Sheep Cook
    private void sheep(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        float fade = life(inst, age, 0, 8);
        int col = inst.color;
        if (new Vector3f(b).sub(a).lengthSquared() < 0.25f) {                   // Sheep Cook: chefs bobbing round the caster
            int n = 3;
            for (int k = 0; k < n; k++) {
                float ang = Mth.TWO_PI * k / n + age * 0.015f;
                Vector3f q = new Vector3f(a).add(Mth.cos(ang) * 1.5f, 0.6f + 0.25f * Math.abs(Mth.sin(age * 0.18f + k)), Mth.sin(ang) * 1.5f);
                buf.billboard(ctx, SHEEP, VfxBlend.ALPHA, q, 1.1f * p, 0.08f * Mth.sin(age * 0.2f + k), VfxVertexBuffer.withAlpha(col, fade));
                buf.billboard(ctx, COTTON, VfxBlend.ALPHA, new Vector3f(q).add(0, 0.55f * p, 0), 0.5f * p, 0, VfxVertexBuffer.withAlpha(WHITE, fade));   // chef's hat
            }
            int s = ctx.seg(8, 4);
            for (int i = 0; i < s; i++) {
                float lt = ((age * 0.03f) + hash(inst.seed, i, 1)) % 1f, ang = hash(inst.seed, i, 2) * Mth.TWO_PI;
                buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, new Vector3f(a).add(Mth.cos(ang) * 1.2f, 0.5f + 1.5f * lt, Mth.sin(ang) * 1.2f), 0.2f, 0,
                        VfxVertexBuffer.withAlpha(0xFFFFE8A0, fade * (1 - lt)));
            }
            return;
        }
        // Sleeping Sheep Strike: a sheep bounding along the line, a trail of cotton, a cotton burst on arrival
        float t = Mth.clamp(age / (inst.duration * 0.6f), 0, 1), arrive = Mth.clamp((age - inst.duration * 0.6f) / (inst.duration * 0.4f), 0, 1);
        Vector3f pos = new Vector3f(a).lerp(b, VfxAnim.easeInOutSine(t)).add(0, Math.abs(Mth.sin(t * Mth.PI * 3)) * 0.8f * p, 0);
        if (arrive <= 0) buf.billboard(ctx, SHEEP, VfxBlend.ALPHA, pos, 1.5f * p, 0.15f * Mth.sin(age * 0.6f), VfxVertexBuffer.withAlpha(col, fade));
        int n = ctx.seg(8, 4);
        for (int i = 0; i < n; i++) {
            float f = (i + 0.5f) / n * t;
            Vector3f q = new Vector3f(a).lerp(b, VfxAnim.easeInOutSine(f)).add(0, 0.2f, 0);
            float lt = Mth.clamp((t - f) * 3, 0, 1);
            buf.billboard(ctx, COTTON, VfxBlend.ALPHA, q, (0.5f + 0.6f * lt) * p, i, VfxVertexBuffer.withAlpha(col, fade * (1 - lt) * 0.8f));
        }
        if (arrive > 0) {
            int k = ctx.seg(10, 5);
            for (int i = 0; i < k; i++) {
                float ang = Mth.TWO_PI * i / k, d = 1.8f * p * VfxAnim.easeOutCubic(arrive);
                buf.billboard(ctx, COTTON, VfxBlend.ALPHA, new Vector3f(b).add(Mth.cos(ang) * d, 0.4f + 0.6f * arrive, Mth.sin(ang) * d), (0.8f + 0.6f * arrive) * p, i,
                        VfxVertexBuffer.withAlpha(col, fade * (1 - arrive)));
            }
            buf.billboard(ctx, COTTON, VfxBlend.ALPHA, new Vector3f(b).add(0, 0.6f, 0), 2.2f * p * (1 - arrive * 0.5f), 0, VfxVertexBuffer.withAlpha(col, fade * (1 - arrive)));
        }
    }

    // ------------------------------------------------------------------ Glutton's Banquet
    private void maw(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power;
        Vector3f c = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f face = new Vector3f(to).sub(c);
        if (face.lengthSquared() < 1e-4f) face.set(new Vector3f(ctx.camRight).cross(0, 1, 0));
        face.normalize();
        Vector3f right = side(face), up = new Vector3f(right).cross(face).normalize();
        float t = age / inst.duration;
        float open = t < 0.7f ? VfxAnim.easeOutBack(Mth.clamp(t / 0.2f, 0, 1)) : 1 - VfxAnim.easeInCubic(Mth.clamp((t - 0.7f) / 0.12f, 0, 1));
        float fade = life(inst, age, 0, 6);
        int col = inst.color, light = VfxVertexBuffer.whiten(col, 0.5f);
        // the maw: wide, its height opening and snapping shut
        panel(buf, MAW, VfxBlend.ALPHA, c, right, up, 2.2f * p, 1.9f * p * Math.max(0.05f, open), VfxVertexBuffer.withAlpha(col, fade));
        // everything around is sucked into it
        int n = ctx.seg(16, 8);
        for (int i = 0; i < n; i++) {
            float ph = hash(inst.seed, i, 1) * 12f, lt = ((age + ph) % 12f) / 12f;
            float a = hash(inst.seed, i, 2) * Mth.TWO_PI + lt * 2f, rad = 4.5f * p * (1 - VfxAnim.easeInCubic(lt));
            Vector3f q = new Vector3f(c).add(new Vector3f(right).mul(Mth.cos(a) * rad)).add(new Vector3f(up).mul(Mth.sin(a) * rad))
                    .add(new Vector3f(face).mul(2.5f * (1 - lt)));
            buf.billboard(ctx, VfxTextures.MANA_MOTE, VfxBlend.ADD, q, 0.3f * p, 0, VfxVertexBuffer.withAlpha(i % 2 == 0 ? light : 0xFFFFE8A0, fade * Mth.sin(lt * Mth.PI) * open));
        }
        hoop(buf, WIND_STREAK, VfxBlend.ADD, VfxPose.facing(new Vector3f(c).add(new Vector3f(face).mul(0.8f)), face).spin(-age * 0.3f), 2.8f * p, 1.4f * p, 0.6f,
                ctx.seg(12, 8), 3, age * 0.1f, VfxVertexBuffer.withAlpha(light, 0.6f * fade * open));
        VfxBloom.glow(ctx, buf, c, 1.2f * p, col, 0.4f * fade);
    }

    // ------------------------------------------------------------------ Dark Garden Invitation / Kids' Playground
    private void pool(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), r = inst.power;
        float spread = VfxAnim.easeOutCubic(Mth.clamp(age / 10f, 0, 1));
        float fade = life(inst, age, 0, 12);
        Vector3f g = ctx.rel(inst.from(ctx)).add(0, 0.04f, 0);
        int col = inst.color, ink = 0xFF06050A, edge = VfxVertexBuffer.whiten(col, 0.3f);
        VfxPose ground = VfxPose.ground(g);
        buf.plane(SMOKE, VfxBlend.ALPHA, ground.spin(age * 0.01f), r * 1.15f * spread, VfxVertexBuffer.withAlpha(ink, 0.95f * fade));
        buf.plane(SMOKE, VfxBlend.ALPHA, ground.lift(0.01f).spin(-age * 0.015f), r * spread, VfxVertexBuffer.withAlpha(col, 0.8f * fade));
        hoop(buf, SMOKE, VfxBlend.ALPHA, ground.lift(0.2f), r * spread, r * 1.05f * spread, 0.25f, ctx.seg(14, 8), 3, age * 0.02f, VfxVertexBuffer.withAlpha(edge, 0.6f * fade));
        // hands reaching up out of it, rising and sinking out of step
        int n = ctx.seg(6, 3);
        for (int i = 0; i < n; i++) {
            float ph = hash(inst.seed, i, 1) * 24f, lt = ((age + ph) % 24f) / 24f;
            float rise = Mth.sin(lt * Mth.PI), a = hash(inst.seed, i, 2) * Mth.TWO_PI, rad = r * spread * (0.2f + 0.7f * hash(inst.seed, i, 3));
            Vector3f foot = new Vector3f(g).add(Mth.cos(a) * rad, -0.3f, Mth.sin(a) * rad);
            stand(ctx, buf, HAND, VfxBlend.ALPHA, foot, 0.8f, 1.6f * rise, VfxVertexBuffer.withAlpha(ink, fade));
        }
        // wisps curling up
        int w = ctx.seg(8, 4);
        for (int i = 0; i < w; i++) {
            float lt = ((age * 0.025f) + hash(inst.seed, i, 4)) % 1f, a = hash(inst.seed, i, 5) * Mth.TWO_PI + lt * 2;
            buf.billboard(ctx, SMOKE, VfxBlend.ALPHA, new Vector3f(g).add(Mth.cos(a) * r * 0.6f, 0.2f + 1.5f * lt, Mth.sin(a) * r * 0.6f), 0.8f, i,
                    VfxVertexBuffer.withAlpha(col, 0.7f * fade * (1 - lt)));
        }
    }

    // ------------------------------------------------------------------ Unite Mode
    private void unite(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power;
        float form = VfxAnim.easeOutBack(Mth.clamp(age / 10f, 0, 1));
        float fade = life(inst, age, 0, 12);
        Vector3f g = ctx.rel(inst.from(ctx));
        int col = inst.color, eye = 0xFFE02040;
        // dark wisps streaming up round the body
        int n = ctx.seg(12, 6);
        for (int i = 0; i < n; i++) {
            float lt = ((age * 0.04f) + hash(inst.seed, i, 1)) % 1f, a = hash(inst.seed, i, 2) * Mth.TWO_PI + lt * 3;
            float rad = (0.5f + 0.3f * lt) * p * form;
            buf.billboard(ctx, SMOKE, VfxBlend.ALPHA, new Vector3f(g).add(Mth.cos(a) * rad, 0.1f + 2.2f * lt, Mth.sin(a) * rad), (0.7f + 0.6f * lt) * p, i + age * 0.02f,
                    VfxVertexBuffer.withAlpha(col, 0.85f * fade * Mth.sin(lt * Mth.PI)));
        }
        // the devil's eyes: two red points hovering behind the shoulder, blinking
        float blink = (age % 70) < 4 ? 0.1f : 1f;
        Vector3f head = new Vector3f(g).add(-ctx.camRight.x * 0.1f, 2.3f * p, -ctx.camRight.z * 0.1f);
        for (int s = -1; s <= 1; s += 2) {
            Vector3f e = new Vector3f(head).add(new Vector3f(ctx.camRight).mul(0.18f * s));
            buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, e, 0.35f, 0, VfxVertexBuffer.withAlpha(eye, fade * blink * form));
        }
        // shadow ring at the feet, slowly turning
        VfxPose foot = VfxPose.ground(new Vector3f(g).add(0, 0.05f, 0)).spin(age * 0.02f);
        buf.plane(VfxTextures.DEVIL_CIRCLE, VfxBlend.ALPHA, foot, 1.3f * p * form, VfxVertexBuffer.withAlpha(col, 0.8f * fade));
        hoop(buf, SMOKE, VfxBlend.ALPHA, VfxPose.ground(new Vector3f(g).add(0, 0.4f, 0)), 0.9f * p * form, 0.7f * p * form, 0.4f, ctx.seg(12, 8), 3, -age * 0.04f,
                VfxVertexBuffer.withAlpha(col, 0.6f * fade));
    }

    // ------------------------------------------------------------------ The Raging Black Bull
    private void construct(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power;
        float fade = life(inst, age, 0, 10);
        Vector3f g = ctx.rel(inst.from(ctx));
        int col = inst.color;
        float w = 0.9f * p, h = 2.6f * p;
        // planks flying in from all round and locking into a hulking frame (four walls, a roof, horns)
        int n = ctx.seg(20, 10);
        for (int i = 0; i < n; i++) {
            float delay = hash(inst.seed, i, 1) * 12f, lt = VfxAnim.easeOutCubic(Mth.clamp((age - delay) / 10f, 0, 1));
            if (lt <= 0) continue;
            int wall = Math.min(4, i / 4), slot = i % 4;                    // four walls of four boards each, then the roof
            Vector3f target, right, up = new Vector3f(0, 1, 0);
            if (wall < 4) {
                float a = wall * Mth.HALF_PI;
                Vector3f out = new Vector3f(Mth.cos(a), 0, Mth.sin(a)), along = new Vector3f(-Mth.sin(a), 0, Mth.cos(a));
                target = new Vector3f(g).add(new Vector3f(out).mul(w)).add(0, 0.35f + (slot + 0.5f) / 4f * h * 0.9f, 0);
                right = along;
            } else {
                target = new Vector3f(g).add(0, h, ((slot + 0.5f) / 4f * 2 - 1) * w);
                right = new Vector3f(1, 0, 0);
                up = new Vector3f(0, 0, 1);
            }
            float ang = hash(inst.seed, i, 4) * Mth.TWO_PI;
            Vector3f from = new Vector3f(g).add(Mth.cos(ang) * 6 * p, 3 * p * hash(inst.seed, i, 5), Mth.sin(ang) * 6 * p);
            Vector3f at = new Vector3f(from).lerp(target, lt);
            float tumble = (1 - lt) * 4 + i;
            Vector3f r2 = new Vector3f(right).mul(Mth.cos(tumble)).add(new Vector3f(up).mul(Mth.sin(tumble) * (1 - lt)));
            panel(buf, PLANK, VfxBlend.ALPHA, at, r2.normalize(), up, w * 0.98f, h * 0.9f / 8f + 0.02f, VfxVertexBuffer.withAlpha(ElementFx.mulRgb(col, 0.8f + 0.2f * hash(inst.seed, i, 6)), fade));
        }
        // horns on the roof
        float hornT = Mth.clamp((age - 14) / 8f, 0, 1);
        for (int s = -1; s <= 1; s += 2) {
            Vector3f base = new Vector3f(g).add(s * w * 0.6f, h, 0), tip = new Vector3f(base).add(s * 0.9f * p, 0.9f * p * hornT, 0);
            if (hornT > 0) buf.beam(ctx, PLANK, VfxBlend.ALPHA, base, tip, 0.35f * p, 0.08f * p, 1, 0, VfxVertexBuffer.withAlpha(0xFFE8E0D0, fade), VfxVertexBuffer.withAlpha(0xFFE8E0D0, fade));
        }
        // dust as it lands
        int d = ctx.seg(6, 3);
        for (int i = 0; i < d; i++) {
            float dt = Mth.clamp((age - 10) / 20f, 0, 1), a = Mth.TWO_PI * i / d;
            if (dt <= 0) continue;
            buf.billboard(ctx, EARTH_DUST, VfxBlend.ALPHA, new Vector3f(g).add(Mth.cos(a) * (1 + 2 * dt) * p, 0.3f + dt, Mth.sin(a) * (1 + 2 * dt) * p), (1 + 1.5f * dt) * p, i,
                    VfxVertexBuffer.withAlpha(0xC0B8A890, (1 - dt) * fade));
        }
    }

    // ------------------------------------------------------------------ Rouge, Red Thread of Fate
    private void rouge(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        boolean unravel = inst.power >= 2f;
        float appear = VfxAnim.easeOutBack(Mth.clamp(age / 10f, 0, 1));
        float fade = life(inst, age, 0, 12);
        float undo = unravel ? VfxAnim.easeInCubic(Mth.clamp(age / (inst.duration * 0.7f), 0, 1)) : 0;
        Vector3f g = ctx.rel(inst.from(ctx));
        int col = inst.color, light = VfxVertexBuffer.whiten(col, 0.35f);
        Vector3f seat = new Vector3f(g).add(0, 1.95f, 0);                       // on the caster's head, as in the anime
        // Rouge herself (woven body; while unravelling she thins out from the top)
        float bob = 0.03f * Mth.sin(age * 0.12f);
        stand(ctx, buf, ROUGE, VfxBlend.ALPHA, new Vector3f(seat).add(0, bob, 0), 0.62f * appear, 0.78f * appear * (1 - undo), VfxVertexBuffer.withAlpha(col, fade * (1 - undo * 0.6f)));
        stand(ctx, buf, ROUGE, VfxBlend.ADD, new Vector3f(seat).add(0, bob, 0), 0.66f * appear, 0.82f * appear * (1 - undo), VfxVertexBuffer.withAlpha(light, 0.25f * fade));
        // the red threads of fate crossing through space round her, long and almost straight, sparkling where they cross
        int n = ctx.seg(7, 4);
        Vector3f[] ends = new Vector3f[n * 2];
        for (int i = 0; i < n; i++) {
            float a = hash(inst.seed, i, 1) * Mth.TWO_PI + age * 0.004f * (i % 2 == 0 ? 1 : -1), tilt = (hash(inst.seed, i, 2) - 0.5f) * 0.7f;
            float reach = 5.5f * appear * (unravel ? 1 + undo : 1);
            Vector3f d = new Vector3f(Mth.cos(a), tilt, Mth.sin(a)).normalize();
            Vector3f mid = new Vector3f(seat).add(0, 0.2f + (hash(inst.seed, i, 3) - 0.5f) * 0.8f, 0);
            Vector3f p0 = new Vector3f(mid).sub(new Vector3f(d).mul(reach)), p1 = new Vector3f(mid).add(new Vector3f(d).mul(reach));
            ends[i * 2] = p0; ends[i * 2 + 1] = p1;
            streak(buf, ctx, WIND_STREAK, VfxBlend.ADD, p0, p1, 0.06f, VfxVertexBuffer.withAlpha(light, 0.9f * fade));
            // a star-sparkle travelling along each thread
            float s = ((age * 0.02f) + hash(inst.seed, i, 4)) % 1f;
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, new Vector3f(p0).lerp(p1, s), 0.32f, age * 0.1f, VfxVertexBuffer.withAlpha(WHITE, fade * Mth.sin(s * Mth.PI)));
        }
        // crossing points near her: bright sparkles
        int k = ctx.seg(5, 3);
        for (int i = 0; i < k; i++) {
            Vector3f q = new Vector3f(seat).add((hash(inst.seed, i, 5) - 0.5f) * 2.6f, 0.2f + (hash(inst.seed, i, 6) - 0.5f) * 1.2f, (hash(inst.seed, i, 7) - 0.5f) * 2.6f);
            float tw = 0.5f + 0.5f * Mth.sin(age * 0.3f + i * 2.3f);
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, q, 0.45f * tw, i, VfxVertexBuffer.withAlpha(WHITE, fade * tw));
        }
        // red orbs of fate drifting round (the anime's glowing red lights)
        for (int i = 0; i < 3; i++) {
            float a = Mth.TWO_PI * i / 3 + age * 0.015f;
            buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, new Vector3f(seat).add(Mth.cos(a) * 2.2f, 0.3f * Mth.sin(age * 0.05f + i), Mth.sin(a) * 2.2f), 0.6f, 0,
                    VfxVertexBuffer.withAlpha(col, 0.7f * fade));
        }
        // unravelling: her body comes apart into threads flying out
        if (unravel) {
            int u = ctx.seg(10, 5);
            for (int i = 0; i < u; i++) {
                float a = hash(inst.seed, i, 8) * Mth.TWO_PI, e = (hash(inst.seed, i, 9) - 0.2f);
                Vector3f d = new Vector3f(Mth.cos(a), e, Mth.sin(a)).normalize();
                Vector3f p0 = new Vector3f(seat).add(0, 0.35f, 0).add(new Vector3f(d).mul(0.3f + 2.5f * undo));
                streak(buf, ctx, WIND_STREAK, VfxBlend.ADD, new Vector3f(seat).add(0, 0.35f, 0), p0, 0.05f, VfxVertexBuffer.withAlpha(col, fade * (1 - undo * 0.5f)));
            }
        }
        VfxBloom.glow(ctx, buf, new Vector3f(seat).add(0, 0.35f, 0), 0.5f, col, 0.4f * fade);
    }
}
