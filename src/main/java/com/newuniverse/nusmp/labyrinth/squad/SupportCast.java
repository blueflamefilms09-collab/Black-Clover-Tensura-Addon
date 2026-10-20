package com.newuniverse.nusmp.labyrinth.squad;

import com.newuniverse.nusmp.balance.BalanceLaw;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;

import java.util.List;

/**
 * Why: what a squad's healer or buffer DOES to its friends, apart from where it stands. It heals the most wounded ally within ten blocks (below 70 percent) with the
 * mod's own {@link BalanceLaw#heal}, so the 50 percent cap per cast and the decay of repeated heals that bind every healer in the mod bind this one too; it never
 * heals a boss (a boss is a mob with {@link BalanceLaw#isBoss}, and a squad that heals its boss would be an instakill-proof boss), and it never heals itself
 * unless it is below a third. When nobody needs healing it buffs up to three allies with strength or resistance for a few seconds, alternating. Both have a cooldown
 * per healer ({@code healCooldownTicks}, {@code buffCooldownTicks}) with a small stagger by slot, so two healers do not cast in the same tick. It is shared by the
 * support goal and by the driver of the squads whose mobs have no goal list. Hunger is never touched: the only effects used are strength and resistance.
 */
final class SupportCast {
    private SupportCast() {}

    static final double RANGE = 10.0;
    private static final double NEEDS_HEAL = 0.7, SELF_HEAL = 0.34;

    /** One look at the healer's cooldowns; casts when one is ready. Returns true if it cast. */
    static boolean tick(ServerLevel level, Squad squad, Mob healer, Squad.Member me, long now) {
        if (healer == null || me == null || !healer.isAlive()) return false;
        boolean healReady = now >= me.healReady, buffReady = now >= me.buffReady;
        if (!healReady && !buffReady) return false;
        List<Mob> allies = squad.alive();
        if (healReady) {
            Mob best = null;
            double bestFrac = NEEDS_HEAL;
            for (Mob a : allies) {
                if (!Squad.healable(a) || healer.distanceToSqr(a) > RANGE * RANGE) continue;
                double frac = a.getMaxHealth() <= 0 ? 1.0 : a.getHealth() / a.getMaxHealth();
                if (a == healer && frac >= SELF_HEAL) continue;
                if (frac < bestFrac) {
                    bestFrac = frac;
                    best = a;
                }
            }
            if (best != null) {
                heal(level, healer, best);
                me.healReady = now + SquadConfig.get(SquadConfig.HEAL_COOLDOWN) + me.slot;
                return true;
            }
        }
        if (buffReady && squad.fighting()) {
            buff(level, healer, allies, (int) (now / 200L) % 2 == 0);
            me.buffReady = now + SquadConfig.get(SquadConfig.BUFF_COOLDOWN) + me.slot * 3L;
            return true;
        }
        return false;
    }

    private static void heal(ServerLevel level, Mob healer, Mob ally) {
        float amount = (float) (ally.getMaxHealth() * SquadConfig.get(SquadConfig.HEAL_FRACTION));
        float healed = BalanceLaw.heal(ally, amount);
        if (healed <= 0.0F) return;
        level.sendParticles(ParticleTypes.HEART, ally.getX(), ally.getY() + ally.getBbHeight() + 0.2, ally.getZ(), 4, 0.3, 0.2, 0.3, 0.02);
        level.playSound(null, healer.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 0.7F, 1.5F);
        healer.getLookControl().setLookAt(ally, 30.0F, 30.0F);
    }

    private static void buff(ServerLevel level, Mob healer, List<Mob> allies, boolean strength) {
        int n = 0;
        for (Mob a : allies) {
            if (n >= 3) break;
            if (!Squad.healable(a) || a == healer || healer.distanceToSqr(a) > RANGE * RANGE) continue;
            a.addEffect(new MobEffectInstance(strength ? MobEffects.DAMAGE_BOOST : MobEffects.DAMAGE_RESISTANCE, 120, 0, true, true));
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, a.getX(), a.getY() + a.getBbHeight() * 0.6, a.getZ(), 3, 0.3, 0.3, 0.3, 0.02);
            n++;
        }
        if (n > 0) level.playSound(null, healer.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 0.6F, 1.2F);
    }
}
