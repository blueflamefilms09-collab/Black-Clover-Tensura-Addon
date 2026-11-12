package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.FlatProps;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Caster-attached effects (all follow the caster):
 * SPIRIT_AURA: a rising sheath of flame tongues in the caster's colour with a bright core, climbing sparks, a body glow and a rune band on the ground (3.77.0: the spirals were removed on the owner's order).
 * MANA_CHARGE: mana motes converging into the caster while chanting.
 * SPELL_CARD: removed on the owner's order (1.110.0): it drew a flat picture of a card in the world and nothing draws it any more (the enum constant stays for saves and ordinals).
 * WEAPON_CONSTRUCTS: spectral swords, axes and shields orbiting the caster.
 */
public class AuraLayer extends AbstractVfxLayer {
    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.SPIRIT_AURA, VfxShape.MANA_CHARGE, VfxShape.WEAPON_CONSTRUCTS); }
    @Override public int defaultDuration(VfxShape s) { return 160; }
    @Override public int defaultColor(VfxShape s) { return 0xFF7FE8FF; }
    @Override public int vertexBudget(VfxShape s) { return 400; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case SPIRIT_AURA -> aura(inst, ctx, buf);
            case MANA_CHARGE -> charge(inst, ctx, buf);
            default -> constructs(inst, ctx, buf);
        }
    }

    /**
     * 3.77.0, remade on the owner's order (the orbiting spiral glyphs are gone): a real aura. A sheath of flame tongues in the caster's colour rises from the feet past the head and
     * flickers, a brighter thin core runs inside it, sparks climb through it, the body glows, and a rune band turns on the ground while a pulse runs out from the feet. It ignites in
     * the first tenth of its life and dies down in the last. At most about 230 of the shape's 400 vertices.
     */
    private void aura(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick);
        float alpha = VfxAnim.fadeInOut(t, 0.06f, 0.15f);
        float ignite = VfxAnim.easeOutBack(Mth.clamp(t * 10f, 0f, 1f));
        Vec3 base = inst.from(ctx);
        int col = inst.color, light = VfxVertexBuffer.whiten(col, 0.55f), clear = VfxVertexBuffer.withAlpha(light, 0f);
        float p = inst.power;
        double feetY = -0.95;
        // the sheath: tongues of flame standing round the body, each with its own height and flicker
        int n = ctx.seg(8, 5);
        for (int i = 0; i < n; i++) {
            float a = Mth.TWO_PI * i / n + age * 0.035f;
            float flick = 0.82f + 0.18f * Mth.sin(age * 0.55f + i * 2.1f);
            float r = (0.36f + 0.06f * Mth.sin(age * 0.2f + i)) * p;
            float h = (2.25f + 0.55f * Mth.sin(i * 1.7f + age * 0.11f)) * p * flick * ignite;
            Vector3f foot = ctx.rel(base.add(Mth.cos(a) * r, feetY, Mth.sin(a) * r));
            Vector3f tip = ctx.rel(base.add(Mth.cos(a) * r * 0.35, feetY + h, Mth.sin(a) * r * 0.35));
            buf.beam(ctx, VfxTextures.FLAME, VfxBlend.ADD, foot, tip, 0.62f * p, 0.06f * p, 3, -age * 0.09f - i * 0.37f, VfxVertexBuffer.withAlpha(col, alpha * 0.85f), clear);
        }
        // the core: fewer, thinner, brighter, faster
        int m = ctx.seg(4, 3);
        for (int i = 0; i < m; i++) {
            float a = Mth.TWO_PI * i / m - age * 0.06f;
            float r = 0.16f * p;
            float h = (1.9f + 0.4f * Mth.sin(i * 2.3f + age * 0.17f)) * p * ignite;
            Vector3f foot = ctx.rel(base.add(Mth.cos(a) * r, feetY + 0.1, Mth.sin(a) * r));
            Vector3f tip = ctx.rel(base.add(0, feetY + h, 0));
            buf.beam(ctx, VfxTextures.FLAME, VfxBlend.ADD, foot, tip, 0.34f * p, 0.04f * p, 3, -age * 0.15f - i * 0.5f, VfxVertexBuffer.withAlpha(light, alpha * 0.9f), clear);
        }
        // the body's glow, breathing
        Vector3f chest = ctx.rel(base.add(0, 0.2, 0));
        VfxBloom.glow(ctx, buf, chest, 1.15f * p, col, alpha * (0.55f + 0.2f * VfxAnim.pulse(age, 1.2f)));
        // sparks climbing through the sheath
        int sparks = ctx.seg(8, 4);
        for (int i = 0; i < sparks; i++) {
            float life = ((age * 0.045f) + i / (float) sparks) % 1f;
            float a = i * 2.39996f + life * 2.2f;
            float r = (0.5f - 0.25f * life) * p;
            Vector3f q = ctx.rel(base.add(Mth.cos(a) * r, feetY + life * 2.6 * p, Mth.sin(a) * r));
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, q, 0.2f * p * (1 - life * 0.7f), age * 0.3f + i, VfxVertexBuffer.withAlpha(light, alpha * (1 - life)));
        }
        // the ground: a rune band turning under the feet, and a pulse running out
        VfxPose feet = VfxPose.ground(ctx.rel(base.add(0, feetY, 0))).spin(age * 0.06f);
        buf.ring(VfxTextures.RUNE_RING, VfxBlend.ADD, feet, 0.78f * p, 1.02f * p, ctx.seg(16, 8), 2, age * 0.01f, VfxVertexBuffer.withAlpha(col, alpha * 0.9f));
        float wave = (age % 30f) / 30f;
        VfxBloom.planeGlow(buf, feet, (0.5f + 1.3f * wave) * p, col, alpha * 0.45f * (1 - wave));
    }

    private void charge(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick);
        Vec3 c = inst.from(ctx);
        int col = inst.color;
        int n = ctx.seg(24, 8);
        for (int i = 0; i < n; i++) {
            float phase = ((age * 0.05f) + i / (float) n) % 1f;
            float a = i * 2.39996f, r = 3f * (1 - phase);
            Vector3f q = ctx.rel(c.add(Mth.cos(a) * r, Mth.sin(i * 1.7f) * r * 0.5, Mth.sin(a) * r));
            buf.billboard(ctx, VfxTextures.MANA_MOTE, VfxBlend.ADD, q, 0.22f, 0, VfxVertexBuffer.withAlpha(col, phase));
        }
        VfxPose feet = VfxPose.ground(ctx.rel(c.add(0, -0.95, 0)));
        buf.plane(VfxTextures.MAGIC_CIRCLE, VfxBlend.ADD, feet.spin(age * 0.08f), 1.3f * VfxAnim.circleOpen(t), VfxVertexBuffer.withAlpha(col, 1 - t * 0.5f));
        VfxBloom.glow(ctx, buf, ctx.rel(c), 0.6f, col, 0.5f + 0.5f * t);
    }

    private void constructs(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick);
        float alpha = VfxAnim.fadeInOut(t, 0.08f, 0.15f);
        Vec3 c = inst.from(ctx);
        int col = inst.color;
        if (!FlatProps.drawFlat()) {                                           // the default: real solid models, never the flat white cut-outs (docs/visual/flat_props.md)
            solidConstructs(inst, ctx, buf, t, age, alpha, c, col);
            return;
        }
        ResourceLocation[] tex = {VfxTextures.CONSTRUCT_SWORD, VfxTextures.CONSTRUCT_AXE, VfxTextures.CONSTRUCT_SHIELD};
        int n = ctx.seg(6, 3);
        float r = 1.6f * inst.power;
        for (int i = 0; i < n; i++) {
            float a = VfxAnim.runeOrbit(i, n, age, 0.8f);
            Vector3f q = ctx.rel(c.add(Mth.cos(a) * r, 0.2 + 0.25 * Mth.sin(age * 0.1f + i), Mth.sin(a) * r));
            float size = i % 3 == 2 ? 1.3f : 1.1f;
            buf.billboard(ctx, tex[i % 3], VfxBlend.ADD, q, size * inst.power, 0.3f * Mth.sin(age * 0.08f + i), VfxVertexBuffer.withAlpha(col, alpha));
            VfxBloom.glow(ctx, buf, q, 0.35f, col, alpha * 0.5f);
        }
        VfxPose feet = VfxPose.ground(ctx.rel(c.add(0, -0.95, 0))).spin(age * 0.04f);
        buf.plane(VfxTextures.MAGIC_CIRCLE, VfxBlend.ADD, feet, r * 1.1f, VfxVertexBuffer.withAlpha(col, alpha * 0.6f));
    }

    /**
     * The solid constructs: a sword, an axe and a shield (then a sword again) circling the caster as shaded low-poly models, leaning and turning, a small glow on each and the rune circle underfoot.
     * Worst case about 4 models x 19 quads + 4 glows (8 vertices each) + the circle: under 330 vertices, inside the 400 of an effect; a model that no longer fits is skipped whole, one further than 40
     * blocks is not built, and nothing is allocated per model.
     */
    private static final Vector3f SCRATCH = new Vector3f();                // render thread only

    private void solidConstructs(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float t, float age, float alpha, Vec3 c, int col) {
        int n = ctx.seg(4, 3);
        float r = 1.6f * inst.power;
        float grow = alpha >= 1f ? 1f : alpha;                                  // appear and vanish by growing and shrinking, so the solids never turn see-through
        float s = Mth.clamp(1.15f * inst.power, 0.7f, 1.9f) * grow;
        Vector3f mid = ctx.rel(c);                                              // one conversion; each weapon position is a scratch vector (nothing allocated per weapon)
        Vector3f q = SCRATCH;
        for (int i = 0; i < n; i++) {
            float a = VfxAnim.runeOrbit(i, n, age, 0.8f);
            q.set(mid.x + Mth.cos(a) * r, mid.y + 0.2f + 0.25f * Mth.sin(age * 0.1f + i), mid.z + Mth.sin(a) * r);
            int kind = i % 3;
            boolean ok = switch (kind) {
                case 0 -> SolidModels.sword(buf, q, age * 0.05f + i, 0.35f, s, 1f, col);
                case 1 -> SolidModels.axe(buf, q, age * 0.05f + i, -0.3f, s, 1f, col);
                default -> SolidModels.shield(buf, q, Mth.HALF_PI - a, 0.12f, s * 0.95f, 1f, col);
            };
            if (ok) VfxBloom.glow(ctx, buf, q, 0.3f, col, alpha * 0.4f);
        }
        float r2 = r * 1.1f;
        VfxPose feet = VfxPose.ground(new Vector3f(mid.x, mid.y - 0.95f, mid.z)).spin(age * 0.04f);
        buf.plane(VfxTextures.MAGIC_CIRCLE, VfxBlend.ADD, feet, r2, VfxVertexBuffer.withAlpha(col, alpha * 0.6f));
    }
}
