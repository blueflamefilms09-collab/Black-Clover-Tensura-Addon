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
 * Recombination Magic (Henry Legolant, the Black Bulls' magic house), 1.93.0 rebuild: matter taken apart and put back together as a building. Boards that fly in and lock, a blueprint
 * that turns on the ground, posts that rise, smoke from a chimney, nails that spark. Textures (tools/gen_recombination_vfx_textures.py): recombination_plank, _blueprint, _smoke,
 * _post, _spark, _ring.
 * <ul>
 *   <li>RECOMBINATION_FX1, CAST / PROJECTILE ("Mana Corkscrew"). A small blueprint opens at 'from'; a drill of boards spirals along the path 'from' to 'to', each board turning on
 *       the axis with a glowing core at the head and nail sparks shed behind it. from = origin, to = target, power = size (1.0 about a block), duration = flight ticks (16).</li>
 *   <li>RECOMBINATION_FX2, ZONE / FIELD ("Bulwark", "Room Swap"). The blueprint turns on the ground inside an octagonal footprint ring that turns the other way; twelve boards fly in
 *       from outside one after another and lock on the footprint; four posts rise at the corners; smoke puffs out of the middle. from = centre on the ground, power = RADIUS,
 *       duration = life ticks (default 80; fades in over 8, out over 12).</li>
 *   <li>RECOMBINATION_FX3, IMPACT / SIGNATURE ("The Raging Black Bull" arrives, a swap lands). An orange flash, the octagon ring as a shock wave, ten boards and nails thrown out
 *       spinning and falling, dust puffs rolling out, crossed nail sparks. from = centre, to = optional direction hint, power = scale, duration = life ticks (28).</li>
 * </ul>
 * inst.color is mixed 30 percent into the orange; the wood colour is fixed.
 */
public class RecombinationLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation PLANK = t("recombination_plank");
    public static final ResourceLocation BLUEPRINT = t("recombination_blueprint");
    public static final ResourceLocation SMOKE = t("recombination_smoke");
    public static final ResourceLocation POST = t("recombination_post");
    public static final ResourceLocation SPARK = t("recombination_spark");
    public static final ResourceLocation RING = t("recombination_ring");

    private static final int ORANGE = 0xFFFF9A3C, WOOD = 0xFFC8985E, DARK = 0xFF8A6238, WHITE = 0xFFFFFFFF;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.RECOMBINATION_FX1, VfxShape.RECOMBINATION_FX2, VfxShape.RECOMBINATION_FX3); }

    @Override
    public int defaultDuration(VfxShape s) { return s == VfxShape.RECOMBINATION_FX1 ? 16 : s == VfxShape.RECOMBINATION_FX2 ? 80 : 28; }

    @Override public int defaultColor(VfxShape s) { return ORANGE; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case RECOMBINATION_FX1 -> drill(inst, ctx, buf);
            case RECOMBINATION_FX2 -> zone(inst, ctx, buf);
            case RECOMBINATION_FX3 -> impact(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ helpers
    private static float life(VfxInstance inst, float age, float in, float out) {
        float a = in <= 0 ? 1 : Mth.clamp(age / in, 0, 1), b = out <= 0 ? 1 : Mth.clamp((inst.duration - age) / out, 0, 1);
        return Math.min(a, b);
    }

    private static float fract(float x) { return x - (float) Math.floor(x); }

    private static Vector3f lerp(Vector3f a, Vector3f b, float t) { return new Vector3f(a).lerp(b, t); }

    private static int orange(VfxInstance inst) { return VfxVertexBuffer.lerpColor(ORANGE, inst.color | 0xFF000000, 0.3f); }

    // ------------------------------------------------------------------ FX1: the drill
    private void drill(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.progress(ctx.partialTick), size = Mth.clamp(inst.power, 0.4f, 4f) * 0.55f;
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(from);
        float dist = dir.length();
        if (dist < 0.05f) return;
        dir.div(dist);
        int or = orange(inst);
        float open = VfxAnim.easeOutBack(Mth.clamp(p / 0.2f, 0, 1)) * Mth.clamp((0.45f - p) / 0.25f, 0, 1);
        if (open > 0.02f) {
            buf.plane(BLUEPRINT, VfxBlend.ADD, VfxPose.facing(from, dir).spin(age * 0.12f), 0.6f * size * open, VfxVertexBuffer.withAlpha(or, 0.9f * open));
            VfxBloom.glow(ctx, buf, from, 0.45f * size, or, 0.5f * open);
        }
        float head = VfxAnim.easeOutCubic(Mth.clamp((p - 0.1f) / 0.72f, 0, 1)), fade = Mth.clamp((1f - p) / 0.22f, 0, 1);
        if (head <= 0.01f) return;
        Vector3f side = new Vector3f(dir).cross(0, 1, 0);
        if (side.lengthSquared() < 1e-4f) side.set(1, 0, 0);
        side.normalize();
        Vector3f up = new Vector3f(side).cross(dir).normalize();
        float tail = Math.max(0f, head - 0.38f);
        int n = ctx.seg(14, 8);
        for (int i = 0; i < n && buf.hasBudget(4); i++) {
            float s = tail + (head - tail) * (i + 0.5f) / n;
            float ang = i * 0.95f + age * 0.75f, rad = 0.3f * size * (0.35f + 0.65f * (i + 1f) / n);
            Vector3f q = lerp(from, to, s).add(new Vector3f(side).mul(Mth.cos(ang) * rad)).add(new Vector3f(up).mul(Mth.sin(ang) * rad));
            float sz = (0.28f + 0.3f * (i + 1f) / n) * size;
            buf.billboard(ctx, PLANK, VfxBlend.ALPHA, q, sz * 1.05f, ang + 0.6f, VfxVertexBuffer.withAlpha(i % 3 == 0 ? DARK : WOOD, fade * (0.5f + 0.5f * (i + 1f) / n)));
        }
        Vector3f hp = lerp(from, to, head);
        VfxBloom.glow(ctx, buf, hp, 0.4f * size, or, 0.9f * fade);
        buf.billboard(ctx, SPARK, VfxBlend.ADD, hp, 0.7f * size, age * 0.3f, VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(or, 0.5f), fade));
        RandomSource r = inst.random();
        for (int i = 0; i < 5 && buf.hasBudget(4); i++) {                                        // nail sparks shed behind the head
            float s = Mth.clamp(head - 0.05f - r.nextFloat() * 0.3f, 0, 1), off = (r.nextFloat() - 0.5f) * 0.9f * size;
            Vector3f q = lerp(from, to, s).add(new Vector3f(side).mul(off)).add(0, -0.15f * size * (head - s) * 6f, 0);
            buf.billboard(ctx, SPARK, VfxBlend.ADD, q, 0.22f * size, r.nextFloat() * 6f, VfxVertexBuffer.withAlpha(or, fade * Mth.clamp(1f - (head - s) * 3f, 0, 1)));
        }
    }

    // ------------------------------------------------------------------ FX2: the house is laid out
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), fade = life(inst, age, 8, 12), R = Math.max(0.8f, inst.power);
        float open = VfxAnim.easeOutBack(Mth.clamp(age / 10f, 0, 1));
        if (open <= 0.01f) return;
        Vector3f c = ctx.rel(inst.from(ctx));
        int or = orange(inst);
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, 0.05f, 0));
        VfxBloom.planeGlow(buf, ground, R * open, or, 0.3f * fade);
        buf.plane(BLUEPRINT, VfxBlend.ADD, ground.spin(age * 0.012f), R * open, VfxVertexBuffer.withAlpha(or, 0.85f * fade));
        buf.plane(RING, VfxBlend.ADD, ground.lift(0.01f).spin(-age * 0.02f), R * 0.98f * open, VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(or, 0.3f), 0.8f * fade));
        RandomSource r = inst.random();
        for (int i = 0; i < 12 && buf.hasBudget(4); i++) {                                       // boards fly in from outside and lock on the footprint
            float a = Mth.TWO_PI * i / 12f + 0.26f, delay = i * 2.2f, land = Mth.clamp((age - delay) / 12f, 0, 1);
            if (land <= 0f) continue;
            float ease = VfxAnim.easeOutCubic(land), rad = R * (1.0f + 1.4f * (1f - ease)) * 0.96f, h = 2.4f * (1f - ease) * (1f - ease) + 0.12f;
            Vector3f q = new Vector3f(Mth.cos(a) * rad, h, Mth.sin(a) * rad).add(c);
            float rot = -a + Mth.HALF_PI + (1f - ease) * 3f;
            buf.billboard(ctx, PLANK, VfxBlend.ALPHA, q, Math.min(1.6f, 0.6f + R * 0.18f), rot * 0f + (i % 2) * 0.4f, VfxVertexBuffer.withAlpha(i % 2 == 0 ? WOOD : DARK, fade * Mth.clamp(land * 3f, 0, 1)));
            if (land > 0.92f) buf.billboard(ctx, SPARK, VfxBlend.ADD, q, 0.35f, 0, VfxVertexBuffer.withAlpha(or, (1f - land) * 12f * fade));
        }
        for (int i = 0; i < 4 && buf.hasBudget(4); i++) {                                        // four posts rise at the corners
            float a = Mth.HALF_PI * i + Mth.PI / 4f, show = VfxAnim.easeOutBack(Mth.clamp((age - 18f - i * 3f) / 10f, 0, 1));
            if (show <= 0.01f) continue;
            float hh = Math.min(2.6f, 1.2f + R * 0.3f);
            Vector3f q = new Vector3f(Mth.cos(a) * R * 0.78f, hh * 0.5f * show, Mth.sin(a) * R * 0.78f).add(c);
            buf.billboard(ctx, POST, VfxBlend.ALPHA, q, hh * show, 0, VfxVertexBuffer.withAlpha(WOOD, fade));
        }
        for (int i = 0; i < 3 && buf.hasBudget(4); i++) {                                        // chimney smoke
            float rise = fract(age / 36f + i / 3f);
            Vector3f q = new Vector3f(0.25f * Mth.sin(rise * 6f + i), 0.4f + rise * 3.0f, 0.2f * Mth.cos(rise * 5f)).add(c);
            buf.billboard(ctx, SMOKE, VfxBlend.ALPHA, q, 0.7f + 1.1f * rise, i, VfxVertexBuffer.withAlpha(0xFFB8B0A8, Mth.sin(rise * Mth.PI) * 0.65f * fade));
        }
        VfxBloom.glow(ctx, buf, new Vector3f(c).add(0, 0.5f, 0), 0.6f, or, 0.3f * fade * (0.7f + 0.3f * Mth.sin(age * 0.2f)));
    }

    // ------------------------------------------------------------------ FX3: boards and nails thrown out
    private void impact(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.progress(ctx.partialTick), size = Mth.clamp(inst.power, 0.5f, 4f) * 0.55f;
        Vector3f c = ctx.rel(inst.from(ctx)), hint = new Vector3f(ctx.rel(inst.to(ctx))).sub(c);
        int or = orange(inst);
        float fade = Mth.clamp((1f - p) / 0.4f, 0, 1), flash = Mth.clamp(1f - p / 0.2f, 0, 1), grow = VfxAnim.easeOutCubic(Mth.clamp(p / 0.5f, 0, 1));
        if (flash > 0.01f) VfxBloom.glow(ctx, buf, new Vector3f(c).add(0, 0.5f * size, 0), 1.1f * size, VfxVertexBuffer.whiten(or, 0.5f), flash);
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, 0.05f, 0));
        buf.plane(RING, VfxBlend.ADD, ground.spin(age * 0.05f), (0.3f + 2.0f * grow) * size, VfxVertexBuffer.withAlpha(or, (1f - grow * 0.85f) * fade));
        buf.plane(BLUEPRINT, VfxBlend.ADD, ground.lift(0.01f).spin(-age * 0.08f), 1.2f * size * VfxAnim.easeOutBack(Mth.clamp(p / 0.2f, 0, 1)), VfxVertexBuffer.withAlpha(or, 0.8f * Mth.clamp((1f - p) / 0.5f, 0, 1)));
        RandomSource r = inst.random();
        for (int i = 0; i < 10 && buf.hasBudget(8); i++) {
            float a = Mth.TWO_PI * i / 10f + r.nextFloat() * 0.5f, up = 0.5f + r.nextFloat() * 1.0f, spd = (0.9f + r.nextFloat() * 1.0f) * size;
            Vector3f d = new Vector3f(Mth.cos(a), up, Mth.sin(a));
            if (hint.lengthSquared() > 0.01f) d.add(new Vector3f(hint).normalize().mul(0.6f));
            float air = VfxAnim.easeOutCubic(Mth.clamp(p, 0, 1));
            Vector3f q = new Vector3f(c).add(d.x * spd * air * 1.5f, d.y * spd * air * 1.2f - 2.2f * size * p * p, d.z * spd * air * 1.5f);
            if (i % 3 == 2) buf.billboard(ctx, SPARK, VfxBlend.ADD, q, 0.3f * size, age * 0.4f + i, VfxVertexBuffer.withAlpha(or, fade));
            else buf.billboard(ctx, PLANK, VfxBlend.ALPHA, q, 0.5f * size, age * 0.35f * (i % 2 == 0 ? 1 : -1) + i, VfxVertexBuffer.withAlpha(i % 2 == 0 ? WOOD : DARK, fade));
        }
        for (int i = 0; i < 5 && buf.hasBudget(4); i++) {                                        // dust rolling out along the ground
            float a = Mth.TWO_PI * i / 5f + 0.5f, rr = (0.3f + 1.7f * grow) * size;
            Vector3f q = new Vector3f(c).add(Mth.cos(a) * rr, 0.2f * size + 0.3f * size * p, Mth.sin(a) * rr);
            buf.billboard(ctx, SMOKE, VfxBlend.ALPHA, q, (0.6f + 0.9f * grow) * size, i, VfxVertexBuffer.withAlpha(0xFFB8B0A8, 0.6f * fade * (1f - p)));
        }
    }
}
