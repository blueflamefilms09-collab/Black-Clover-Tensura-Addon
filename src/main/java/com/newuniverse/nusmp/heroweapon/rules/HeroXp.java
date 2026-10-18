package com.newuniverse.nusmp.heroweapon.rules;

import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: the weapon's own experience (the series' level of the weapon) must be granted through ONE method, because the interference rule has to be able to refuse
 * it. Blows, kills, skill hits and mastery all call {@link #gain}; the method adds the experience to the bond unless the player is blocked by
 * {@link HeroInterference}, applies the config multiplier, and returns what was really added. Owned by the core builder, who replaces this stub (the stub adds
 * the amount as it is).
 */
public final class HeroXp {
    private HeroXp() {}

    /** Gives the player's bond {@code amount} experience (before the config multiplier); returns the experience actually added, 0 when blocked or unbound. */
    public static long gain(ServerPlayer player, long amount) {
        HeroBond b = HeroBonds.of(player);
        if (!b.isBound() || amount <= 0 || HeroInterference.blocked(player)) return 0;
        b.addExp(amount);
        return amount;
    }
}
