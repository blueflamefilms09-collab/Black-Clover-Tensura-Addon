package com.newuniverse.nusmp.heroweapon.weapon.spear;

import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import net.minecraft.nbt.CompoundTag;

/**
 * Why: a few things about the Spear have to be remembered between two moments (the counter stance is up until a game time, the Lust blows have piled up to
 * some stacks until another), and the bond already offers each system its own saved compound for exactly that. The compound {@code spear} lives in the
 * bond (saved with the player, copied to the new body, cleared when the weapon is released; every key optional, so an old save or a hand-edited one loads).
 * Reading never creates it, so a wielder who never used these techniques keeps a clean save. Server thread only.
 */
final class SpearState {
    private SpearState() {}

    private static final String SYSTEM = "spear", RIPOSTE_UNTIL = "riposteUntil", FERVOR = "fervor", FERVOR_UNTIL = "fervorUntil";

    private static CompoundTag tag(HeroCtx c) { return c.bond.extra(SYSTEM); }

    // ---- the counter stance
    static void raiseStance(HeroCtx c, long until) { tag(c).putLong(RIPOSTE_UNTIL, until); }

    /** True while the stance is up at game time {@code now}. */
    static boolean stanceUp(HeroCtx c, long now) { return c.bond.hasExtra(SYSTEM) && now <= tag(c).getLong(RIPOSTE_UNTIL); }

    static void dropStance(HeroCtx c) {
        if (c.bond.hasExtra(SYSTEM)) tag(c).remove(RIPOSTE_UNTIL);
    }

    // ---- the Lust blows
    /** The stacks of fervor alive at game time {@code now} (0 when they have run out). */
    static int fervor(HeroCtx c, long now) {
        if (!c.bond.hasExtra(SYSTEM)) return 0;
        CompoundTag t = tag(c);
        return now <= t.getLong(FERVOR_UNTIL) ? Math.max(0, t.getInt(FERVOR)) : 0;
    }

    static void setFervor(HeroCtx c, int stacks, long until) {
        CompoundTag t = tag(c);
        t.putInt(FERVOR, Math.max(0, stacks));
        t.putLong(FERVOR_UNTIL, until);
    }

    static void clearFervor(HeroCtx c) {
        if (!c.bond.hasExtra(SYSTEM)) return;
        CompoundTag t = tag(c);
        t.remove(FERVOR);
        t.remove(FERVOR_UNTIL);
    }
}
