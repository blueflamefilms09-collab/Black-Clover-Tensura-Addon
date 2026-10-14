package com.newuniverse.nusmp.relics.wave;

import java.util.function.IntUnaryOperator;

/**
 * Wave 16: the small formulas of the necklace pieces and charms, pure. Dice are passed in as {@code roll(sides) -> 0..sides-1} (like
 * RandomSource.nextInt) so the tests can fix them.
 */
public final class Wave16Rules {
    private Wave16Rules() {}

    // ---------------------------------------------------------------- Holy Handgrenade ("roll a d4+1")
    public enum Grenade { DUD, BLAST, NOTHING, BACKFIRE }

    /** 2 never explodes, 3 is the blessing, 4 stays in the hand, 5 goes off in the hand. Anything else is clamped into that range. */
    public static Grenade grenade(int roll) {
        int r = Math.max(2, Math.min(5, roll));
        return r == 2 ? Grenade.DUD : r == 3 ? Grenade.BLAST : r == 4 ? Grenade.NOTHING : Grenade.BACKFIRE;
    }

    /** d4+1; a thrower who is not good (an innocent fell to them lately) always rolls the worst. */
    public static int grenadeRoll(IntUnaryOperator roll, boolean tainted) { return tainted ? 5 : 2 + Math.max(0, Math.min(3, roll.applyAsInt(4))); }

    /** The grenade's own blast never kills the thrower: at most {@code 33}, and always leaving a heart. */
    public static float backfireSelf(float health) { return Math.max(0f, Math.min(Wave16Tune.BACKFIRE_DAMAGE, health - 1f)); }

    // ---------------------------------------------------------------- Sigil of the Faint Flame
    /** The charges are lost at sunset: they only answer while the sun is up. */
    public static boolean sunUp(long dayTime) {
        long t = Math.floorMod(dayTime, 24000L);
        return t < Wave16Tune.SUNSET;
    }

    /** A spell's "level" read off the blow: one per 5 damage, 1 to 3 (a fireball is level 3). */
    public static int reprisalCost(float incoming) { return Math.max(1, Math.min(3, (int) Math.ceil(incoming / 5.0))); }

    /** The fireball thrown back: 6, 8 or 10 by level (the 8d6 of the card, scaled to a blow a player can survive). */
    public static float reprisalDamage(int cost) { return 4f + 2f * Math.max(1, Math.min(3, cost)); }

    /** 1d8+2 charges at dawn. */
    public static int sigilGain(IntUnaryOperator roll) { return 3 + Math.max(0, Math.min(7, roll.applyAsInt(8))); }

    // ---------------------------------------------------------------- Paralyzing Perfume (DC 17 Constitution save)
    /** The save bonus of a creature: one per 25 maximum health, at most 8. */
    public static int saveBonus(float maxHealth) { return Math.max(0, Math.min(8, (int) (maxHealth / 25f))); }

    /** {@code d20} is 1 to 20. A creature of 400 health or more cannot be held at all. */
    public static boolean saved(float maxHealth, int d20) { return maxHealth >= 400f || d20 + saveBonus(maxHealth) >= Wave16Tune.PERFUME_DC; }

    // ---------------------------------------------------------------- Brooch of Po'eur (an intelligence check against DC 12 to 20)
    /** The DC of a spell by what it cost (percent of maximum magicules): 12 from 5%, plus 2 for each further 5%, at most 20; 0 = too small to count. */
    public static int poeurDc(double costPercent) {
        if (costPercent < Wave16Tune.POEUR_MIN_COST) return 0;
        return (int) Math.min(20, 12 + 2 * Math.floor((costPercent - Wave16Tune.POEUR_MIN_COST) / 5.0));
    }

    public static boolean freeCast(double costPercent, int d20, int bonus) {
        int dc = poeurDc(costPercent);
        return dc > 0 && d20 + bonus >= dc;
    }

    // ---------------------------------------------------------------- Coin of Draconic Luck
    public static boolean heads(int d2) { return d2 <= 0; }

    /** Penalties in a row on one day: a tail adds one, a head ends the run, a new day starts again. */
    public static int streakAfter(boolean heads, int streak, boolean sameDay) {
        int s = sameDay ? streak : 0;
        return heads ? 0 : s + 1;
    }

    /** Three penalties in a row, then a d20 (1 to 20): on a 1 the coin cracks. */
    public static boolean cracks(int streak, int d20) { return streak >= 3 && d20 == 1; }

    // ---------------------------------------------------------------- Strange Idol
    /** Roll twice, keep the higher: the boon's level, 0 to 3. */
    public static int idolLevel(IntUnaryOperator roll) {
        int a = Math.max(0, Math.min(3, roll.applyAsInt(4))), b = Math.max(0, Math.min(3, roll.applyAsInt(4)));
        return Math.max(a, b);
    }

    // ---------------------------------------------------------------- Portable God
    /** The wild magic table: d100 of 100 is the Wish (-1), else one of ten rows by tens. */
    public static int wildRow(int d100) { return d100 >= 100 ? -1 : Math.max(0, Math.min(9, (d100 - 1) / 10)); }

    // ---------------------------------------------------------------- Wallet of the Slayer (GP = Challenge Rating x 2d6)
    /** Challenge Rating from health: about one per 15, at least a quarter. */
    public static double challenge(float maxHealth) { return Math.max(0.25, maxHealth / 15.0); }

    /** {@code d6a} and {@code d6b} are 1 to 6. At least 1, at most 600 per kill. */
    public static int gp(float maxHealth, int d6a, int d6b) {
        long v = Math.round(challenge(maxHealth) * (Math.max(1, d6a) + Math.max(1, d6b)));
        return (int) Math.max(1, Math.min(600, v));
    }

    public static int goldCoins(int gp) { return Math.max(0, gp) / Wave16Tune.GOLD_COIN_GP; }

    public static int silverCoins(int gp) { return Math.max(0, gp) % Wave16Tune.GOLD_COIN_GP; }

    // ---------------------------------------------------------------- Satchel of Unmet Needs
    public enum Slot { FREE, OUT, MISSING }

    /** An object is out while its 24 hours run, missing until the dawn of day {@code goneUntilDay}. */
    public static Slot satchelSlot(long outUntil, long goneUntilDay, long now, long today) {
        if (goneUntilDay > today) return Slot.MISSING;
        if (outUntil > now) return Slot.OUT;
        return Slot.FREE;
    }

    // ---------------------------------------------------------------- Dryad's Wedding Band
    /** The vines let go after three game days without the ring on the finger. */
    public static boolean vinesLetGo(long offSince, long now) { return offSince > 0 && now - offSince > Wave16Tune.DRYAD_FREE_AFTER; }
}
