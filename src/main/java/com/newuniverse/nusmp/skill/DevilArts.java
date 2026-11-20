package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.devil.DevilAttribute;
import com.newuniverse.nusmp.devil.DevilTier;
import com.newuniverse.nusmp.devil.dual.DevilMagic;
import com.newuniverse.nusmp.devil.dual.DualConfig;
import com.newuniverse.nusmp.devil.dual.DualRules;
import com.newuniverse.nusmp.skill.rules.UltimateClass;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: the work of the arts of the Ultimate skill Devil Magic ({@link DevilMagicSkill}), kept apart from the skill's price and cooldown so the CI self-test can fire the art of every attribute on a stand-in
 * (docs/devil_double_magic.md). One art hits the foes in a cone in front of the caster (at most {@link DualRules#MAX_TARGETS}, range {@link DualRules#RANGE}): the damage goes through the balance law, the
 * attribute puts its effect on the foe and pushes or pulls it, and the double art adds what the caster's own grimoire magic does on a hit. The blow is of the damage type {@code anime_multiverse:devil_magic}
 * (so Adaptability does not learn it) and every effect it puts on a foe is remembered as devil magic ({@link DevilMagic#markEffect}). Server side only; it spawns nothing and moves nobody but the foes it hit.
 * Worst case of one cast: one entity query, six hits, six effects, six small particle bursts.
 */
public final class DevilArts {
    private DevilArts() {}

    /**
     * Fires the art. {@code grimoireHit} is a {@code MagicType.HitEffect} name for the double art ("" = the devil's art alone). Returns how many foes were hit (0 = nobody in the cone).
     */
    public static int fire(ServerPlayer p, DevilAttribute attr, DevilTier tier, boolean combo, String grimoireHit, double mastery, UltimateClass cls) {
        if (p == null || !p.isAlive() || tier == DevilTier.NONE || attr == DevilAttribute.NONE) return 0;
        DualRules.Art art = DualRules.art(attr);
        DualRules.Hit extra = combo ? DualRules.hit(grimoireHit) : DualRules.hit("");
        double share = com.newuniverse.nusmp.devil.phase2.Phase2.power(p, com.newuniverse.nusmp.devil.DevilStore.peek(p));      // phase 2: the kind's share, split between the holders, times the debuff (1.0 when off)
        double raw = DualRules.damage(tier, art.bonus() + extra.bonus(), combo, DualConfig.damageScale() * share);
        List<LivingEntity> foes = SkillTargets.foesInCone(p, DualRules.RANGE, DualRules.CONE);
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        int hits = 0;
        for (LivingEntity t : foes) {
            if (hits >= DualRules.MAX_TARGETS) break;
            float dmg = BalanceLaw.damage(p, t, (float) raw, mastery, cls);
            if (dmg <= 0f) continue;
            float before = t.getHealth();
            t.hurt(DevilMagic.source(p), dmg);
            float dealt = Math.max(0f, before - t.getHealth());
            hits++;
            apply(p, t, art.effectId(), art.ticks(), art.amp(), art.special(), art.strip(), now);
            if (art.heal() > 0) com.newuniverse.nusmp.relics.Fx.heal(p, (float) Math.min(6.0, dealt * art.heal()));
            if (DualRules.isTime(attr)) apply(p, t, "minecraft:mining_fatigue", 30, 3, "", false, now);     // a foe whose time is stopped can not swing either
            if (combo) {
                apply(p, t, extra.effectId(), extra.ticks(), extra.amp(), extra.special(), "strip".equals(extra.special()), now);
                if ("drain".equals(extra.special())) com.newuniverse.nusmp.relics.Fx.heal(p, (float) Math.min(4.0, dealt * 0.1));
            }
            SkillFx.burst(level, t.getBoundingBox().getCenter(), false, 6, 0.3);
        }
        SkillUtil.castVfx(p, 0xFF000000 | DualRules.color(attr));
        return hits;
    }

    static void apply(ServerPlayer p, LivingEntity t, String effectId, int ticks, int amp, String special, boolean strip, long now) {
        if (!effectId.isEmpty() && ticks > 0) {
            com.newuniverse.nusmp.relics.Fx.addEffect(t, effectId, amp >= 2 ? BalanceLaw.controlTicks(t, ticks) : ticks, amp);
            DevilMagic.markEffect(t, effectId, now + ticks);
        }
        if (strip) {
            t.removeEffect(MobEffects.DAMAGE_RESISTANCE);
            t.removeEffect(MobEffects.ABSORPTION);
            t.removeEffect(MobEffects.REGENERATION);
            t.removeEffect(MobEffects.FIRE_RESISTANCE);
        }
        switch (special) {
            case "burn" -> t.igniteForSeconds(5);
            case "pull" -> shove(t, p.position().add(0, 0.5, 0), 0.9);
            case "push" -> shove(t, p.position().add(0, 0.5, 0), -0.9);
            default -> { }
        }
    }

    /** Moves the foe toward {@code point} (strength > 0) or away from it (strength < 0); a bounded velocity, nothing teleports. */
    static void shove(LivingEntity t, Vec3 point, double strength) {
        Vec3 d = point.subtract(t.position());
        double len = Math.max(0.5, d.length());
        Vec3 v = d.scale(strength / len);
        t.setDeltaMovement(t.getDeltaMovement().add(v.x, Math.abs(strength) * 0.25, v.z));
        t.hurtMarked = true;
    }
}
