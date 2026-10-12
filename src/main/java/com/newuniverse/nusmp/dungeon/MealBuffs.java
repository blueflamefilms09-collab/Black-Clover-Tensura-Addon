package com.newuniverse.nusmp.dungeon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * 1.19.0: what a cooked monster meal does to a wizard's magic for a while (docs/living_dungeon.md section 4): it multiplies the power
 * and the mana cost of every spell, read by mana.ManaLaw like the Signature and the mage's stage. A new meal replaces the old one.
 */
public final class MealBuffs {
    private MealBuffs() {}

    private static final String KEY = "nusmp_meal";

    /** Gives a meal's buff: spell power x{@code power}, mana cost x{@code cost}, for {@code ticks}. */
    public static void give(Player p, double power, double cost, int ticks) {
        CompoundTag t = new CompoundTag();
        t.putDouble("Power", power);
        t.putDouble("Cost", cost);
        t.putLong("Until", p.level().getGameTime() + ticks);
        p.getPersistentData().put(KEY, t);
    }

    private static CompoundTag live(Player p) {
        CompoundTag t = p.getPersistentData().getCompound(KEY);
        return !t.isEmpty() && t.getLong("Until") > p.level().getGameTime() ? t : null;
    }

    public static double power(Player p) {
        CompoundTag t = live(p);
        return t == null ? 1.0 : t.getDouble("Power");
    }

    public static double cost(Player p) {
        CompoundTag t = live(p);
        return t == null ? 1.0 : t.getDouble("Cost");
    }
}
