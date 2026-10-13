package com.newuniverse.nusmp.relics.djinn;

/**
 * The binding ritual as numbers and a checklist (pure). A Djinn Seal is used with a Metal Vessel (a relic of Legend grade or above) in the other
 * hand. The price, and why it is this and not another:
 * <ul>
 *   <li><b>A full Magoi bar</b> (a Djinn answers someone who can feed it) and <b>25% of current EP</b> (the pool is spent, never emptied: at least
 *       10% of the maximum is always left);</li>
 *   <li><b>Three high-quality magic crystals</b>: the only part that has to be earned in the Dungeon, so a Djinn cannot be had from the pool alone;</li>
 *   <li><b>An attuned vessel</b>: the Djinn sleeps in something that is yours;</li>
 *   <li><b>A trial of trust</b>: it can fail, from 5% (an Arcane or Saint stage mage) to 27.5% (Stage 9); a failure costs half the price and a short
 *       Weakness and a minute's wait, and never touches the Seal or the vessel.</li>
 * </ul>
 */
public final class BindingRules {
    private BindingRules() {}

    public static final double EP_FRACTION = 0.25;
    public static final double EP_FLOOR = 0.10;
    public static final double FULL_BAR = 0.99;
    public static final int CRYSTALS = 3;
    public static final int RETRY_COOLDOWN = 1200;
    public static final int FAIL_WEAKNESS_TICKS = 600;
    public static final double MIN_FAIL = 0.05, MAX_FAIL = 0.30;

    /** How much of a pool the ritual takes: a quarter of what is there, but never below the floor of the pool's maximum. */
    public static double epCost(double current, double max, double fraction) {
        if (current <= 0 || max <= 0) return 0.0;
        return Math.max(0.0, Math.min(current * fraction, current - max * EP_FLOOR));
    }

    /** The chance the Djinn refuses: 0.05 + 0.025 per Black Clover stage number (Stage 9 is the lowest), flat 0.05 for Arcane and Saint. */
    public static double failChance(int stageNumber, boolean arcaneOrSaint) {
        if (arcaneOrSaint) return MIN_FAIL;
        int s = Math.max(0, Math.min(9, stageNumber));
        return Math.max(MIN_FAIL, Math.min(MAX_FAIL, MIN_FAIL + 0.025 * s));
    }

    /** A failed binding: the Djinn said no. {@code roll} is a uniform [0,1). */
    public static boolean fails(double chance, double roll) { return roll < chance; }

    /** Crystals a failed binding burns: half the three, rounded down. */
    public static int failedCrystals() { return CRYSTALS / 2; }

    public enum Refusal {
        NONE, ALREADY_OWNED, ALREADY_HOLDS_DJINN, EXCLUSIVE_BOND, HOUSEHOLD, NO_VESSEL, VESSEL_TAKEN, NOT_ATTUNED, BAR_NOT_FULL, NO_CRYSTALS
    }

    /** What the ritual checks, in order; the first false answer is the refusal (and nothing is paid). */
    public record Inputs(boolean djinnFree, boolean holdsDjinn, boolean bondAllowed, boolean inHousehold, boolean vesselLegal, boolean vesselFree,
                         boolean attuned, double bar, int crystals, boolean creative) {}

    public static Refusal check(Inputs in) {
        if (!in.djinnFree()) return Refusal.ALREADY_OWNED;
        if (in.holdsDjinn()) return Refusal.ALREADY_HOLDS_DJINN;
        if (in.inHousehold()) return Refusal.HOUSEHOLD;
        if (!in.bondAllowed()) return Refusal.EXCLUSIVE_BOND;
        if (!in.vesselLegal()) return Refusal.NO_VESSEL;
        if (!in.vesselFree()) return Refusal.VESSEL_TAKEN;
        if (!in.attuned()) return Refusal.NOT_ATTUNED;
        if (in.creative()) return Refusal.NONE;
        if (in.bar() < FULL_BAR) return Refusal.BAR_NOT_FULL;
        if (in.crystals() < CRYSTALS) return Refusal.NO_CRYSTALS;
        return Refusal.NONE;
    }
}
