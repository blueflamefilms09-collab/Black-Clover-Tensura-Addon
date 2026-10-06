package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.blackclover.MagicType;
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
 * SPELL_CARD: a floating card showing the grimoire's icon in its frame.
 * WEAPON_CONSTRUCTS: spectral swords, axes and shields orbiting the caster.
 */
public class AuraLayer extends AbstractVfxLayer {
    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.SPIRIT_AURA, VfxShape.MANA_CHARGE, VfxShape.SPELL_CARD, VfxShape.WEAPON_CONSTRUCTS); }
    @Override public int defaultDuration(VfxShape s) { return s == VfxShape.SPELL_CARD ? 30 : 160; }
    @Override public int defaultColor(VfxShape s) { return 0xFF7FE8FF; }
    @Override public int vertexBudget(VfxShape s) { return 400; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case SPIRIT_AURA -> aura(inst, ctx, buf);
            case MANA_CHARGE -> charge(inst, ctx, buf);
            case SPELL_CARD -> card(inst, ctx, buf);
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

    private void card(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick);
        float alpha = VfxAnim.fadeInOut(t, 0.15f, 0.3f);
        Vector3f at = ctx.rel(inst.from(ctx).add(0, VfxAnim.easeOutCubic(t) * 0.5, 0));
        MagicType[] all = MagicType.values();
        int seed = (int) inst.seed;
        MagicType m = all[Math.floorMod(com.newuniverse.nusmp.core.magic.grimoire.SpellArchetype.magicOf(seed), all.length)];
        var archetype = com.newuniverse.nusmp.core.magic.grimoire.SpellArchetype.archetypeOf(seed);
        ResourceLocation icon = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/skill/grimoire/" + m.name().toLowerCase() + ".png");
        float wob = 0.15f * Mth.sin(age * 0.2f);
        VfxBloom.glow(ctx, buf, at, 0.6f, inst.color, alpha);
        buf.billboard(ctx, icon, VfxBlend.ALPHA, at, 0.7f, wob, VfxVertexBuffer.withAlpha(0xFFFFFFFF, alpha));
        if (archetype != null)   // the spell's archetype badge (offense / defense / buff / debuff) in the card's corner
            buf.billboard(ctx, ResourceLocation.fromNamespaceAndPath("nusmp", archetype.texture()), VfxBlend.ALPHA, at, 0.7f, wob, VfxVertexBuffer.withAlpha(0xFFFFFFFF, alpha));
        buf.billboard(ctx, VfxTextures.CARD_FRAME, VfxBlend.ADD, at, 0.82f, wob, VfxVertexBuffer.withAlpha(inst.color, alpha));
    }

    private void constructs(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick);
        float alpha = VfxAnim.fadeInOut(t, 0.08f, 0.15f);
        Vec3 c = inst.from(ctx);
        int col = inst.color;
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
}
