package com.newuniverse.nusmp.greed;

import com.newuniverse.nusmp.relics.pair.PairRules;

import java.util.Set;

/**
 * Why: the Black Sword Greed's gauntlet form and its dual-blade form are meant for TWO hands. Before they were one mesh holding a pair of gloves (floating in front of the fist) and one hilt with two blades. Now
 * the item is drawn once per hand (a glove / a sword in each, {@code client/greed/TwinHands}) and its blow lands twice: the main hand strikes as it always did, and the off hand strikes a second, smaller blow, the
 * same arithmetic as a weapon pair ({@link PairRules}: 60% of the first, 12 tick cooldown). Pure rules, no game types, so the numbers are unit-tested ({@code GreedHandsTest}); the server work is
 * {@code GreedForms#offBlow}. Settings: server {@code dualStance} (false = the old single blow, models stay twin), client {@code classicHands} (true = the old one-mesh looks).
 */
public final class GreedHands {
    private GreedHands() {}

    public static final String GAUNTLETS = "black_sword_greed_gauntlets";
    public static final String DUAL_BLADES = "black_sword_greed_dual_blades";
    /** The mesh id of the left-hand glove (the gauntlet item id itself is the right glove). */
    public static final String GAUNTLETS_LEFT = GAUNTLETS + "_left";

    private static final Set<String> TWIN = Set.of(GAUNTLETS, DUAL_BLADES);

    /** Is this item path one of the two forms that are drawn and swung with both hands? */
    public static boolean isTwin(String itemPath) { return itemPath != null && TWIN.contains(itemPath); }

    public static boolean isGloves(String itemPath) { return GAUNTLETS.equals(itemPath); }

    /** The mesh id for an item path in a hand: the left glove for the left hand, every other mesh as it is. */
    public static String meshFor(String itemPath, boolean leftHand) { return leftHand && GAUNTLETS.equals(itemPath) ? GAUNTLETS_LEFT : itemPath; }

    /** The off-hand blow for a main blow of this size (0 = none). The same share and cap as a relic pair. */
    public static float offBlow(float mainBlow) { return PairRules.offHandDamage(mainBlow); }

    /** May the off-hand blow fall now? */
    public static boolean ready(long now, long nextAllowed) { return PairRules.ready(now, nextAllowed); }

    public static long next(long now) { return PairRules.nextAllowed(now); }

    /** The off-hand blow needs an empty off hand (a held item there is its own thing) and a wielder who is not creative or spectating (as for pairs). */
    public static boolean offHandFree(boolean offHandEmpty, boolean creativeOrSpectator) { return offHandEmpty && !creativeOrSpectator; }
}
