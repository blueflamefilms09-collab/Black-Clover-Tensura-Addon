package com.newuniverse.nusmp.relics.djinn;

/**
 * The exclusive bond, as a truth table (the owner's rule): a player holds ONE of a Black Clover Spirit, a Devil (contract) or a Djinn. The one
 * exception is the multi-bond relic (Astralis, the Eye of the Universe), which lets a player hold more than one kind at a heavy price. Pure: the
 * kinds travel as a bit mask ({@link #SPIRIT}, {@link #DEVIL}, {@link #DJINN}); {@link Bonds} reads the real state of the player.
 *
 * <p>The price of holding more than one kind (all numbers here, so the doc and the tests agree):
 * <ul>
 *   <li>every bond's Magoi drain is {@value #MULTI_DRAIN}x;</li>
 *   <li>no natural mana regeneration while in Full Djinn Equip;</li>
 *   <li>each extra bond halves the trust a bond gains.</li>
 * </ul>
 */
public final class BondRules {
    private BondRules() {}

    public static final int SPIRIT = 1, DEVIL = 2, DJINN = 4;
    public static final double MULTI_DRAIN = 1.4;

    /** How many kinds are in the mask. */
    public static int count(int held) { return Integer.bitCount(held & (SPIRIT | DEVIL | DJINN)); }

    /**
     * May the player take a bond of kind {@code want}? Yes if they hold nothing, or hold only this kind (a bond of the same kind is the caller's
     * business: one spirit, one Djinn), or wear the multi-bond relic.
     */
    public static boolean canBind(int held, int want, boolean multiBond) {
        int others = held & (SPIRIT | DEVIL | DJINN) & ~want;
        return others == 0 || multiBond;
    }

    /** Bonds beyond the first that the price applies to: 0 for a player with one kind or none, 1 for two kinds, 2 for three. */
    public static int extraBonds(int held, boolean multiBond) { return multiBond ? Math.max(0, count(held) - 1) : 0; }

    /** The factor on every Magoi drain of a bond: 1.4 for a multi-bonded player who really holds two kinds or more. */
    public static double drainMultiplier(int held, boolean multiBond) { return extraBonds(held, multiBond) > 0 ? MULTI_DRAIN : 1.0; }

    /** The factor on the trust a bond gains: halved once per extra bond (1, 0.5, 0.25). */
    public static double trustGainMultiplier(int held, boolean multiBond) { return Math.pow(0.5, extraBonds(held, multiBond)); }

    /** True when natural mana regeneration stays off: a multi-bonded player in Full Djinn Equip. */
    public static boolean regenBlocked(int held, boolean multiBond, int phase) {
        return phase >= DjinnPhaseRules.FULL && extraBonds(held, multiBond) > 0;
    }
}
