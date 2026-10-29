package com.newuniverse.nusmp.relics.wave;

import java.util.Set;

/**
 * Why: the pure rules behind wave 101 (the seven Millennium Items and eight more Genesis relics), kept free of Minecraft types so the numbers are unit-tested: the set bonus of the
 * Millennium Items (how many of the seven are worn, and what that lifts), the fatal-blow test of the Puzzle's second soul, the Necklace's foresight cut, the marks the Eye, the Puzzle
 * and the Scale leave on a foe, and who the Scale calls sinful. {@link Wave101Events}, {@link Wave101Set} and {@link Wave101Acts} read the facts off the game and ask these.
 */
public final class Wave101Rules {
    private Wave101Rules() {}

    /** The seven Millennium Items, in the order of the series' list: the Puzzle, the Eye, the Ring, the Rod, the Key, the Scale and the Necklace. */
    public static final String[] SET = {"millennium_puzzle", "millennium_eye", "millennium_ring", "millennium_rod", "millennium_key", "millennium_scale", "millennium_necklace"};
    private static final Set<String> SET_IDS = Set.of(SET);

    public static boolean inSet(String id) { return id != null && SET_IDS.contains(id); }

    /** Pieces worn and working: from this many the set lifts its wearer. */
    public static final int RESIST_AT = 3, REGEN_AT = 4, STRENGTH_AT = 5, SPEED_AT = 6, FULL = 7;

    /** Amplifier of the ambient resistance the set gives (-1 = none): level I from three pieces, II with all seven. */
    public static int resistAmp(int count) { return count >= FULL ? 1 : count >= RESIST_AT ? 0 : -1; }

    public static int regenAmp(int count) { return count >= FULL ? 1 : count >= REGEN_AT ? 0 : -1; }

    public static int strengthAmp(int count) { return count >= FULL ? 1 : count >= STRENGTH_AT ? 0 : -1; }

    public static int speedAmp(int count) { return count >= SPEED_AT ? 0 : -1; }

    /** Seconds an ambient set effect is refreshed for each second it is kept alive (a short tail, so it fades soon after a piece is taken off). */
    public static final int EFFECT_TICKS = 100;

    /** The factor the damage and the healing of every Millennium act grow by: 5% for each piece beyond the first, 30% with all seven. */
    public static double power(int count) {
        int c = Math.max(1, Math.min(FULL, count));
        return 1.0 + 0.05 * (c - 1);
    }

    // ---------------------------------------------------------------- the Puzzle's second soul
    /** Ticks before the second soul can take another deadly blow (five minutes). */
    public static final int SOUL_COOLDOWN = 6000;
    /** The share of the maximum health the wearer wakes with after the second soul took the blow. */
    public static final double SOUL_WAKE = 0.40;

    /** Is the blow deadly: more than the wearer's health and absorption together? */
    public static boolean fatal(float amount, float health, float absorption) { return amount > 0f && amount >= health + absorption; }

    /** Health the wearer is brought up to: {@link #SOUL_WAKE} of the maximum, never lower than they have. */
    public static float soulHealth(float maxHealth, float health) { return Math.max(health, (float) (maxHealth * SOUL_WAKE)); }

    // ---------------------------------------------------------------- the Necklace's foresight
    /** Ticks between two foreseen blows (eight seconds), and the share a foreseen blow is cut by. */
    public static final int FORESIGHT_COOLDOWN = 160;
    public static final float FORESIGHT_CUT = 0.5f;

    /** A blow smaller than this is not worth foreseeing (a nettle, a stumble): it does not start the cooldown. */
    public static final float FORESIGHT_MIN = 3.0f;

    public static float foreseen(float amount) { return amount < FORESIGHT_MIN ? amount : amount * (1f - FORESIGHT_CUT); }

    // ---------------------------------------------------------------- marks on a foe
    /** The kinds of mark a Millennium act leaves, and the extra share of damage the marker deals to the marked foe while it lasts. */
    public enum Mark {
        READ(0.25), PENALTY(0.20), WEIGHED(0.30);

        public final double bonus;

        Mark(double bonus) { this.bonus = bonus; }
    }

    /** Most extra damage all marks of one marker on one foe give together. */
    public static final double MARK_CAP = 0.6;

    public static double markBonus(boolean read, boolean penalty, boolean weighed) {
        double b = (read ? Mark.READ.bonus : 0.0) + (penalty ? Mark.PENALTY.bonus : 0.0) + (weighed ? Mark.WEIGHED.bonus : 0.0);
        return Math.min(MARK_CAP, b);
    }

    // ---------------------------------------------------------------- the Scale
    /**
     * Whom the Scale weighs: a monster, an undead, a fiend or a boss. Never a player, never an animal, a villager or a pet: their hearts are left alone.
     */
    public static boolean sinful(boolean player, boolean monster, boolean undead, boolean fiend, boolean boss) {
        return !player && (monster || undead || fiend || boss);
    }

    /** Hearts the verdict devours at most, and the life each gives back. */
    public static final int VERDICT_HEARTS = 5;
    public static final float VERDICT_HEAL = 12f;

    public static float verdictHeal(int hearts) { return VERDICT_HEAL * Math.max(0, Math.min(VERDICT_HEARTS, hearts)); }

    // ---------------------------------------------------------------- the Ring
    /** Share of the maximum magicules each foe's power gives back to the Ring's wearer, and the most one draw gives. */
    public static final double DRAW_PER_FOE = 0.04, DRAW_MAX = 0.24;

    public static double draw(int foes) { return Math.min(DRAW_MAX, DRAW_PER_FOE * Math.max(0, foes)); }

    // ---------------------------------------------------------------- 3.35.0: the wiki powers (seal, fuse, puppet)
    /** Longest a sealed or controlled mob stays so (five minutes), and the shortest any hold lasts (one second). */
    public static final int MAX_SEAL_TICKS = 6000, MIN_HOLD_TICKS = 20;

    public static int holdTicks(int asked) { return Math.max(MIN_HOLD_TICKS, Math.min(MAX_SEAL_TICKS, asked)); }

    /** Most foes the Scale fuses in one blow, and the extra share of damage each foe in the knot adds for every other. */
    public static final int FUSE_MAX = 5, FUSE_MIN = 2;
    public static final float FUSE_STEP = 0.2f;

    public static int fuseCount(int foes) { return Math.max(0, Math.min(FUSE_MAX, foes)); }

    /** The factor a fusion of {@code foes} monsters hits each of them with: 1.0 for one, 1.8 for the full five (never more). */
    public static float fuseFactor(int foes) { return 1.0f + FUSE_STEP * Math.max(0, fuseCount(foes) - 1); }

    /** Heaviest soul the Ring can seal (it is capped at 200 health whatever the caller asks); a boss or a player is never sealed. */
    public static final float SEAL_SOUL_CAP = 200f;

    public static boolean sealable(boolean boss, boolean player, float maxHealth, float limit) {
        return !boss && !player && maxHealth <= Math.min(limit, SEAL_SOUL_CAP);
    }
}
