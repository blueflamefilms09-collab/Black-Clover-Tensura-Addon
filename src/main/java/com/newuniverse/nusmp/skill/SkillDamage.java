package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.balance.BalanceLaw;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.TensuraSkill;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: addon spells share one balance law ({@link BalanceLaw}: damage scales with the caster's EP and mastery, a player takes at most the
 * PvP hit cap of their max health per hit, mobs take the config multiplier). The old skills called {@code target.hurt(raw)} directly, so a
 * 20-point spell did 20 whatever the caster's power and could one-shot a player. Every skill hit now goes through this class, which also
 * applies the pack-wide DanMachi damage multiplier and credits the kill to the caster.
 * Guide P1 #2: with the skill instance the hit goes through Tensura's {@code createSource} (MAGIC = Tensura magic, ATTACK = a physical
 * player attack), so Tensura resistances, nullifications and skill kill credit apply; SERVER {@code tensuraSkillDamageSource=false} or a
 * call without an instance keeps the old vanilla source. The amount is the same BalanceLaw number either way.
 */
final class SkillDamage {
    private SkillDamage() {}

    enum Kind { ATTACK, MAGIC }

    /** A hit from the caster through the skill's Tensura source. Returns the damage actually dealt to the target's health (0 if it was refused). */
    static float hit(ServerPlayer caster, LivingEntity target, double raw, double masteryFrac, Kind kind, ManasSkillInstance instance) {
        if (caster == null || target == null || !target.isAlive() || raw <= 0 || !SkillTargets.isAimable(caster, target)) return 0f;
        double scaled = raw * NUConfig.DM_DAMAGE_MULT.get();
        float dmg = BalanceLaw.damage(caster, target, (float) scaled, masteryFrac);
        if (dmg <= 0f) return 0f;
        float before = target.getHealth();
        target.hurt(source(caster, kind, instance), dmg);
        return Math.max(0f, before - target.getHealth());
    }

    /** A hit with the default attack kind through the skill's Tensura source. */
    static float hit(ServerPlayer caster, LivingEntity target, double raw, double masteryFrac, ManasSkillInstance instance) {
        return hit(caster, target, raw, masteryFrac, Kind.ATTACK, instance);
    }

    /** Old form, no skill instance: the vanilla source (DbMoves). */
    static float hit(ServerPlayer caster, LivingEntity target, double raw, double masteryFrac, Kind kind) {
        return hit(caster, target, raw, masteryFrac, kind, null);
    }

    /** A hit scaled by the instance's mastery, with the default attack kind (old form, vanilla source). */
    static float hit(ServerPlayer caster, LivingEntity target, double raw, double masteryFrac) {
        return hit(caster, target, raw, masteryFrac, Kind.ATTACK, null);
    }

    static DamageSource source(ServerPlayer caster, Kind kind, ManasSkillInstance instance) {
        // shortcut: mode 0 is attached (hit() does not know the pressed mode), pass the mode through once a skill needs per-mode attribution
        if (instance != null && NUConfig.TENSURA_SKILL_DAMAGE_SOURCE.get() && instance.getSkill() instanceof TensuraSkill skill)
            return skill.createSource(instance, caster, kind == Kind.MAGIC ? TensuraDamageTypes.MAGIC_GENERIC : DamageTypes.PLAYER_ATTACK, 0);
        return source(caster, kind);
    }

    /** A MAGIC hit of this class while the Tensura source is on (Tensura magic, as a grimoire hit is): Nen keeps such hits from counting as blows, as it did with the vanilla source. */
    static boolean isSkillMagic(DamageSource source) {
        return NUConfig.TENSURA_SKILL_DAMAGE_SOURCE.get() && source.is(TensuraDamageTypes.MAGIC_GENERIC);
    }

    static DamageSource source(ServerPlayer caster, Kind kind) {
        return kind == Kind.MAGIC ? caster.damageSources().indirectMagic(caster, caster) : caster.damageSources().playerAttack(caster);
    }
}
