package com.newuniverse.nusmp.entity.zogratis;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.balance.BalanceLaw;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * How every Zogratis hit (the bosses, their disciples, blood beasts, clones) becomes damage: the base number goes through
 * {@link DamageMath} (power tuning, a per-boss scale, the victim's EP) and then through {@link BalanceLaw#damage}, which adds its
 * sliver of the victim's max health, applies the server's spell-damage factor and caps one hit at the pvp share of a player's max
 * health (40% by default), so a strong Tensura player is hurt for real and no hit is a one-shot.
 */
public final class ZogratisDamage {
    private ZogratisDamage() {}

    /**
     * The final damage of a hit.
     * @param powerMultiplier  usually {@code boss.tuning().damageMultiplier()}
     * @param bossScale        a per-boss factor (1 for most)
     * @param mastery          0..1, the attacker's power fraction (balance law mastery)
     */
    public static float amount(LivingEntity from, LivingEntity victim, float base, double powerMultiplier, double bossScale, double mastery) {
        double raw = DamageMath.raw(base, powerMultiplier, bossScale, Nullification.maxEP(victim));
        return BalanceLaw.damage(from, victim, (float) raw, Math.max(0, Math.min(1, mastery)));
    }

    /** Hurts 'victim' (magic damage ignores armour, a plain hit does not). False if it did not land. */
    public static boolean hit(LivingEntity from, LivingEntity victim, float base, double powerMultiplier, double bossScale, double mastery, boolean magic) {
        if (victim == null || !victim.isAlive()) return false;
        DamageSource src = magic ? from.damageSources().indirectMagic(from, from) : from.damageSources().mobAttack(from);
        return victim.hurt(src, amount(from, victim, base, powerMultiplier, bossScale, mastery));
    }
}
