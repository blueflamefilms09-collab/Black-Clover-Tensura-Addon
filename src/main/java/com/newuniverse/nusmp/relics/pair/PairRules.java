package com.newuniverse.nusmp.relics.pair;

/**
 * The pure rules of weapon PAIRS (no Minecraft types, unit tested). A pair is two hand items that come together: the right half keeps the card's id
 * ({@code twin_hook_swords}), the left half is {@code twin_hook_swords_left}. The pair bonus is active only while the main hand holds one half and the
 * off hand the other; the off-hand half then adds a real second blow.
 *
 * Why these numbers (docs/relic_pairs.md): a second blow of six tenths of the first makes a pair worth about 1.6 swords, which is the price of
 * occupying both hands and (for the attuned pair) two items for one place; the cooldown of twelve ticks stops a fast weapon from striking twice every
 * swing at full speed; a lone half keeps all its abilities but loses the second blow and the pair-only passives, so losing one half is a small
 * penalty and never a lock-out.
 */
public final class PairRules {
    private PairRules() {}

    /** The suffix of the left half's id (the same as RelicDef.LEFT_SUFFIX; repeated here so this class has no game types). */
    public static final String LEFT_SUFFIX = "_left";
    /** The off-hand blow: six tenths of the main blow. */
    public static final float OFFHAND_FRACTION = 0.6f;
    /** Least ticks between two off-hand blows of one player. */
    public static final int OFFHAND_COOLDOWN_TICKS = 12;
    /** The largest off-hand blow in health points (a cap on top of the PvP cap the damage helper applies). */
    public static final float OFFHAND_MAX = 40f;
    /** What a lone half does of its pair-only numbers (the spin, the sweep): a small penalty. */
    public static final float LONE_FACTOR = 0.85f;

    /** The id both halves share. */
    public static String baseId(String id) {
        return id != null && id.endsWith(LEFT_SUFFIX) && id.length() > LEFT_SUFFIX.length() ? id.substring(0, id.length() - LEFT_SUFFIX.length()) : id == null ? "" : id;
    }

    public static boolean isLeftId(String id) { return id != null && id.endsWith(LEFT_SUFFIX) && id.length() > LEFT_SUFFIX.length(); }

    public static String leftId(String baseId) { return baseId + LEFT_SUFFIX; }

    /** The other half's id. */
    public static String partnerId(String id) { return isLeftId(id) ? baseId(id) : leftId(id); }

    /** Do these two ids (main hand, off hand) make a pair: the same base, one right and one left? Empty or null ids never do. */
    public static boolean pairActive(String mainId, String offId) {
        if (mainId == null || offId == null || mainId.isEmpty() || offId.isEmpty()) return false;
        if (isLeftId(mainId) == isLeftId(offId)) return false;
        return baseId(mainId).equals(baseId(offId));
    }

    /** The off-hand blow for a main blow of this size. */
    public static float offHandDamage(float mainBlow) {
        if (!(mainBlow > 0f) || Float.isInfinite(mainBlow)) return 0f;
        return Math.min(OFFHAND_MAX, mainBlow * OFFHAND_FRACTION);
    }

    /** May the off-hand blow fall now? {@code nextAllowed} is the game time stored from the last one (a time far in the future, as after a clock change, does not lock it). */
    public static boolean ready(long now, long nextAllowed) { return now >= nextAllowed || nextAllowed - now > 10L * OFFHAND_COOLDOWN_TICKS; }

    public static long nextAllowed(long now) { return now + OFFHAND_COOLDOWN_TICKS; }

    /** The pair-only numbers of an ability: full with the pair in hand, {@link #LONE_FACTOR} with one half. */
    public static float factor(boolean pairActive) { return pairActive ? 1.0f : LONE_FACTOR; }

    /** Should the left half be granted to a player? They hold a right half, and no left half was ever issued in this world. */
    public static boolean needsLeftGrant(boolean holdsRight, boolean leftEverIssued) { return holdsRight && !leftEverIssued; }

    /** Which halves of a pair are still to issue: bit 1 = right, bit 2 = left (a half that lives is not issued again). Zero means the pair is complete. */
    public static int missingHalves(boolean rightLive, boolean leftLive) { return (rightLive ? 0 : 1) | (leftLive ? 0 : 2); }

    /** May a loot roll hand out this pair? Only when neither half lives (a half that lives means one pair exists already). */
    public static boolean rollable(boolean rightLive, boolean leftLive) { return !rightLive && !leftLive; }
}
