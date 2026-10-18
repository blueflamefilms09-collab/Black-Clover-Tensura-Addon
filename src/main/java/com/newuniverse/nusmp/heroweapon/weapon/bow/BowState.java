package com.newuniverse.nusmp.heroweapon.weapon.bow;

import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import net.minecraft.nbt.CompoundTag;

/**
 * Why: a drawn weapon's techniques change the NEXT shot, so the Bow has to remember which change is armed and until when, when the last arrow left the
 * string, and how long Law Fanatic lasts. The bond already offers each system its own saved compound for exactly that; the compound {@code bow} lives in
 * the bond (saved with the player, copied to the new body, cleared when the weapon is released; every key optional, so an old save or a hand-edited
 * one loads and reads as "nothing armed"). Reading never creates it, so a wielder who never used these techniques keeps a clean save. Server thread only.
 */
final class BowState {
    private BowState() {}

    private static final String SYSTEM = "bow", ARMED = "armed", ARMED_UNTIL = "armedUntil", LAST_SHOT = "lastShot", LAW_UNTIL = "lawUntil";

    private static CompoundTag tag(HeroCtx c) { return c.bond.extra(SYSTEM); }

    // ---- the armed change
    static void arm(HeroCtx c, ArmedMode mode, long until) {
        CompoundTag t = tag(c);
        t.putString(ARMED, mode.id());
        t.putLong(ARMED_UNTIL, until);
    }

    /** The mode stored, whether or not it has run out ({@link ArmedMode#NONE} when nothing is stored). */
    static ArmedMode stored(HeroCtx c) { return c.bond.hasExtra(SYSTEM) ? ArmedMode.byId(tag(c).getString(ARMED)) : ArmedMode.NONE; }

    static long armedUntil(HeroCtx c) { return c.bond.hasExtra(SYSTEM) ? tag(c).getLong(ARMED_UNTIL) : 0L; }

    /** The mode armed at game time {@code now}: the stored one while it has not run out, else nothing. */
    static ArmedMode armed(HeroCtx c, long now) {
        ArmedMode m = stored(c);
        return m != ArmedMode.NONE && now <= armedUntil(c) ? m : ArmedMode.NONE;
    }

    static void disarm(HeroCtx c) {
        if (!c.bond.hasExtra(SYSTEM)) return;
        CompoundTag t = tag(c);
        t.remove(ARMED);
        t.remove(ARMED_UNTIL);
    }

    // ---- the string
    static boolean shotLocked(HeroCtx c, long now) {
        return c.bond.hasExtra(SYSTEM) && now >= tag(c).getLong(LAST_SHOT) && now - tag(c).getLong(LAST_SHOT) < BowNumbers.SHOT_LOCK;
    }

    static void markShot(HeroCtx c, long now) { tag(c).putLong(LAST_SHOT, now); }

    // ---- Law Fanatic
    static void raiseLaw(HeroCtx c, long until) { tag(c).putLong(LAW_UNTIL, until); }

    static boolean lawUp(HeroCtx c, long now) { return c.bond.hasExtra(SYSTEM) && now <= tag(c).getLong(LAW_UNTIL); }
}
