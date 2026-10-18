package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.balance.BalanceLaw;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: addon spells share one balance law ({@link BalanceLaw}: damage scales with the caster's EP and mastery, a player takes at most the
 * PvP hit cap of their max health per hit, mobs take the config multiplier). The old skills called {@code target.hurt(raw)} directly, so a
 * 20-point spell did 20 whatever the caster's power and could one-shot a player. Every skill hit now goes through this class, which also
 * applies the pack-wide DanMachi damage multiplier and credits the kill to the caster.
 */
final class SkillDamage {
    private SkillDamage() {}

    enum Kind { ATTACK, MAGIC }

    /** A hit from the caster. Returns the damage actually dealt to the target's health (0 if it was refused). */
    static float hit(ServerPlayer caster, LivingEntity target, double raw, double masteryFrac, Kind kind) {
        if (caster == null || target == null || !target.isAlive() || raw <= 0 || !SkillTargets.isAimable(caster, target)) return 0f;
        double scaled = raw * NUConfig.DM_DAMAGE_MULT.get();
        float dmg = BalanceLaw.damage(caster, target, (float) scaled, masteryFrac);
        if (dmg <= 0f) return 0f;
        float before = target.getHealth();
        target.hurt(source(caster, kind), dmg);
        return Math.max(0f, before - target.getHealth());
    }

    /** A hit scaled by the instance's mastery, with the default attack kind. */
    static float hit(ServerPlayer caster, LivingEntity target, double raw, double masteryFrac) {
        return hit(caster, target, raw, masteryFrac, Kind.ATTACK);
    }

    static DamageSource source(ServerPlayer caster, Kind kind) {
        return kind == Kind.MAGIC ? caster.damageSources().indirectMagic(caster, caster) : caster.damageSources().playerAttack(caster);
    }
}
