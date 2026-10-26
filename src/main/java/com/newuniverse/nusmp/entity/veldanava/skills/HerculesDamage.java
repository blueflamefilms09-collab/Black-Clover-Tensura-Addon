package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.balance.BalanceLaw;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: every blow of Hercules' actives (the tremor, the throw, the Strike) is a plain physical player attack, scaled the way every addon spell is (the Balance Law: EP of the caster, the per-hit cap on a player),
 * limited by the config's cap for one press, and flagged so that Hercules' own passives (God of Strength's multiplier and shockwave, the life steal) do not count it a second time. It is NOT a true hit: armour,
 * Adamantine Body, Michael's Castle Guard and the other defences apply, which is what World-Breaking Strike exists to break. Server thread only.
 */
public final class HerculesDamage {
    private HerculesDamage() {}

    private static boolean own;

    /** True while one of Hercules' own blows is being dealt (the passives read it so a blow is not multiplied twice, and a shockwave does not start another). */
    public static boolean own() { return own; }

    /** A blow of {@code base} damage from the owner on the creature, after the Balance Law and the cap of one press. Returns whether it landed. */
    public static boolean blow(ServerPlayer owner, LivingEntity target, double base, double mastery) {
        if (owner.level().isClientSide || !target.isAlive() || base <= 0.0) return false;
        float scaled = BalanceLaw.damage(owner, target, (float) base, mastery);
        scaled = (float) Math.min(JudgmentConfig.activeCap(), scaled);
        boolean before = own;
        own = true;
        try {
            target.invulnerableTime = 0;
            return target.hurt(owner.damageSources().playerAttack(owner), scaled);
        } finally {
            own = before;
        }
    }

    /** A blow of exactly {@code amount} (already scaled by the caller, as a shockwave's share of the blow that caused it), kept under the Balance Law's cap on a player. Returns whether it landed. */
    public static boolean raw(ServerPlayer owner, LivingEntity target, float amount) {
        if (owner.level().isClientSide || !target.isAlive() || amount <= 0f) return false;
        boolean before = own;
        own = true;
        try {
            return target.hurt(owner.damageSources().playerAttack(owner), BalanceLaw.cap(target, amount));
        } finally {
            own = before;
        }
    }

    /** The mastery fraction the Balance Law scales Hercules' actives by: full once mastered, a little under half before. */
    public static double mastery(ServerPlayer p) { return JudgmentState.mastered(p) ? 1.0 : 0.4; }
}
