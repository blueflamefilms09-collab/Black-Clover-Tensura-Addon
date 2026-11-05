package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.skill.rules.NenRules;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Why (3.57.0, docs/nen_rework.md): every effect of Nen goes out through this one class, so the three looks are decided in one place. {@code nenVfx = false}: the particle puffs of 3.46.0;
 * {@code nenVfx = true, nenVfxV2 = false}: the 3.48.5 shapes (NEN_AURA / EN / GYO / IMPACT / BEAM, still registered and drawn by NenLayer); both true (the default): the remade shapes
 * (NEN_FLOW ... NEN_DOMAIN, drawn by NenArtLayer). In hides every effect of its owner. Everything is cosmetic: sent to nearby players only through {@link SkillFx} and VfxSpawn (which keeps the
 * per-tick packet budget) and never throws. The caller bounds how often it asks (auras once every two seconds, hatsu once per move, a chain once per half second).
 */
final class NenFx {
    private NenFx() {}

    static final String IN_KEY = "nusmp_nen_in";
    /** Ticks an aura instance lives: resent every 40 ticks while its window is open, so a window ended early fades within about two and a half seconds. */
    static final int AURA_LIFE = 50;

    static boolean on() { return NUConfig.NEN_VFX.get(); }
    static boolean v2() { return NUConfig.NEN_VFX_V2.get(); }

    /** In hides the aura: while it is open none of this player's Nen effects are sent (the hatsu is still there, you just cannot see it). */
    static boolean hidden(ServerPlayer p) { return p.getPersistentData().getLong(IN_KEY) > p.level().getGameTime(); }

    private static boolean ok(ServerPlayer p) { return p != null && on() && !hidden(p); }

    /** The aura of one window: {@code kind} is the index in {@link NenRules#AURA_MODES}; {@code charge} is Ko's stored aura (0..1). */
    static void aura(ServerPlayer p, int kind, int type, double charge) {
        if (!ok(p)) return;
        if (v2()) SkillFx.follow(p.serverLevel(), VfxShape.NEN_FLOW, p, p.position(), NenRules.auraColor(kind, type), AURA_LIFE, (float) (kind + 1 + Math.min(0.9, Math.max(0.0, charge))));
        else SkillFx.follow(p.serverLevel(), VfxShape.NEN_AURA, p, p.position(), 0, AURA_LIFE, kind);
    }

    /** A flash and shock ring at a point (Ren's burst, a hit). {@code size} about 1 for a hit, 2 for Ren. */
    static void burst(ServerPlayer p, Vec3 at, int argb, float size, boolean ren) {
        if (!ok(p)) return;
        ServerLevel lv = p.serverLevel();
        if (v2()) SkillFx.shape(lv, VfxShape.NEN_BURST, at, at, argb, ren ? 22 : 16, size);
        else SkillFx.shape(lv, VfxShape.NEN_IMPACT, at, at, argb, 14, size);
    }

    /** The En sphere round the caster; {@code refresh} true when it is a resend of a sphere that is already open (no opening animation). */
    static void sense(ServerPlayer p, double radius, int argb, boolean refresh) {
        if (!ok(p)) return;
        if (v2()) SkillFx.follow(p.serverLevel(), VfxShape.NEN_SENSE, p, p.position(), argb, 50, (float) (radius + (refresh ? 0.5 : 0.0)));
        else if (!refresh) SkillFx.shape(p.serverLevel(), VfxShape.NEN_EN, p.position(), p.position(), 0xFF6AFFC0, 40, (float) radius);
    }

    static void eyes(ServerPlayer p, int argb) {
        if (!ok(p)) return;
        if (v2()) SkillFx.follow(p.serverLevel(), VfxShape.NEN_EYES, p, p.position(), argb, 26, 1f);
        else SkillFx.follow(p.serverLevel(), VfxShape.NEN_GYO, p, p.position(), 0xFF7AE0FF, 20, 1f);
    }

    /** Enhancement: the fist from the caster to the foe; {@code charge} 0..1 is the Ko aura it spent. */
    static void fist(ServerPlayer p, Vec3 from, Vec3 to, double charge, int argb) {
        if (!ok(p)) return;
        if (v2()) SkillFx.shape(p.serverLevel(), VfxShape.NEN_FIST, from, to, argb, 20, (float) (1 + Math.min(1.0, Math.max(0.0, charge))));
        else SkillFx.shape(p.serverLevel(), VfxShape.NEN_IMPACT, to, to, argb, 14, 1f);
    }

    /** An aura bullet (Emission), or a stolen buff flowing to the caster (Specialization). */
    static void bullet(ServerPlayer p, Vec3 from, Vec3 to, int argb) {
        if (!ok(p)) return;
        if (v2()) SkillFx.shape(p.serverLevel(), VfxShape.NEN_BULLET, from, to, argb, 10, 1f);
        else SkillFx.shape(p.serverLevel(), VfxShape.NEN_BEAM, from, to, argb, 14, 1f);
    }

    /** Transmutation: {@code property} is {@link NenRules#ELECTRIC}, GUM or RUBBER; for rubber {@code radius} is the shock radius. */
    static void surge(ServerPlayer p, int property, Vec3 from, Vec3 to, double radius, int argb) {
        if (!ok(p)) return;
        int life = property == NenRules.ELECTRIC ? 12 : property == NenRules.GUM ? 18 : 22;
        if (v2()) SkillFx.shape(p.serverLevel(), VfxShape.NEN_SURGE, from, to, argb, life, NenRules.surgePower(property, radius));
        else SkillFx.shape(p.serverLevel(), property == NenRules.RUBBER ? VfxShape.NEN_IMPACT : VfxShape.NEN_BEAM, from, to, argb, 14, property == NenRules.RUBBER ? 2.4f : 1f);
    }

    /** Conjuration: a chain from the caster's hand to the foe. */
    static void link(ServerPlayer p, LivingEntity target, Vec3 from, int argb, int life) {
        if (!ok(p) || target == null) return;
        Vec3 at = target.position().add(0, target.getBbHeight() * 0.5, 0);
        if (v2()) SkillFx.shape(p.serverLevel(), VfxShape.NEN_LINK, from, at, argb, life, 1f);      // resent every half second, so the end on the foe is never more than ten ticks old
        else SkillFx.shape(p.serverLevel(), VfxShape.NEN_IMPACT, at, at, argb, 14, 1.3f);
    }

    /** Manipulation: the needle flies from the caster to the foe and the strings stay. */
    static void needle(ServerPlayer p, Vec3 from, Vec3 to, int argb, int life) {
        if (!ok(p)) return;
        if (v2()) SkillFx.shape(p.serverLevel(), VfxShape.NEN_NEEDLE, from, to, argb, life, 1f);
        else SkillFx.shape(p.serverLevel(), VfxShape.NEN_IMPACT, to, to, argb, 14, 1.1f);
    }

    /** Specialization: the field round the caster, following. */
    static void domain(ServerPlayer p, double radius, int ticks, int argb) {
        if (!ok(p)) return;
        if (v2()) SkillFx.follow(p.serverLevel(), VfxShape.NEN_DOMAIN, p, p.position(), argb, ticks, (float) radius);
        else SkillFx.shape(p.serverLevel(), VfxShape.NEN_IMPACT, p.position().add(0, 1, 0), p.position().add(0, 1, 0), argb, 14, (float) (radius / 4.0));
    }

    /** The cast flourish: the old grimoire circle only when the remade look is off (Nen has no grimoire). */
    static void cast(ServerPlayer p, int argb) {
        if (!v2() || !on()) SkillUtil.castVfx(p, argb);
    }
}
