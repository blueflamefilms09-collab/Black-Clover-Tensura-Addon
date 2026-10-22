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
 * SPIRIT_AURA: glowing spirit spirals orbiting head and shoulders + a body glow (Spirit Dive look).
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

    private void aura(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick);
        float alpha = VfxAnim.fadeInOut(t, 0.06f, 0.15f);
        Vec3 base = inst.from(ctx);
        int col = inst.color, light = VfxVertexBuffer.whiten(col, 0.5f);
        float p = inst.power;
        // Spirals: two rings (head, shoulders) bobbing and spinning, like the reference image.
        int n = ctx.seg(7, 4);
        for (int i = 0; i < n; i++) {
            float a = VfxAnim.runeOrbit(i, n, age, 1.4f);
            float h = (i % 2 == 0 ? 1.95f : 1.35f) + 0.12f * Mth.sin(age * 0.15f + i);
            float r = (i % 2 == 0 ? 0.55f : 0.8f) * p;
            Vector3f q = ctx.rel(base.add(Mth.cos(a) * r, h - 1, Mth.sin(a) * r));
            buf.billboard(ctx, VfxTextures.SPIRIT_SPIRAL, VfxBlend.ADD, q, 0.42f * p, -age * 0.25f + i, VfxVertexBuffer.withAlpha(col, alpha));
        }
        // Body glow + arm wisps
        Vector3f chest = ctx.rel(base.add(0, 0.2, 0));
        VfxBloom.glow(ctx, buf, chest, 0.9f * p, col, alpha * (0.6f + 0.2f * VfxAnim.pulse(age, 1.5f)));
        RandomSource r = RandomSource.create(inst.seed + inst.age() / 3);
        int motes = ctx.seg(10, 4);
        for (int i = 0; i < motes; i++) {
            float life = ((age * 0.04f) + i / (float) motes) % 1f;
            float a = i * 2.4f;
            Vector3f q = ctx.rel(base.add(Mth.cos(a) * 0.45 * p, -0.9 + life * 2.4, Mth.sin(a) * 0.45 * p));
            buf.billboard(ctx, VfxTextures.MANA_MOTE, VfxBlend.ADD, q, 0.16f * (1 - life), 0, VfxVertexBuffer.withAlpha(light, alpha * (1 - life)));
        }
        // Wind ring at the feet
        VfxPose feet = VfxPose.ground(ctx.rel(base.add(0, -0.95, 0))).spin(age * 0.1f);
        buf.ring(VfxTextures.WIND_SLASH, VfxBlend.WIND, feet, 0.7f * p, 1.0f * p, ctx.seg(16, 8), 2, age * 0.05f, VfxVertexBuffer.withAlpha(col, alpha * 0.8f));
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
