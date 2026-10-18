package com.newuniverse.nusmp.elite.races.state;

/**
 * Why: one way into a state. A state lists several of these and is entered when ANY one holds, and every condition inside one entry must hold (EP threshold, hit points low,
 * the moon, an item, the race tier). So "the Great Ape rises on a full moon, OR at will once you are a High Saiyan" is two entries. Pure; reads a {@link TransformContext}.
 *
 * @param epMin     the player's maximum EP must be at least this (times the server's gate scale)
 * @param hpBelow   the hit points must be at most this fraction of the maximum (1.0 = no condition)
 * @param moon      the sky condition
 * @param needsSky  the player must be under the open sky
 * @param item      an item the player must carry (empty = none)
 * @param itemCount how many
 * @param consume   whether entering takes the item
 * @param minTier   the race tier at least (1 = any Saiyan tier)
 * @param auto      this entry may fire BY ITSELF (the rage awakening at low health, the rising of the moon); the other entries only answer the hotkey and the commands
 */
public record EntryCondition(double epMin, double hpBelow, MoonRule moon, boolean needsSky, String item, int itemCount, boolean consume, int minTier, boolean auto) {
    public static final EntryCondition ALWAYS = new EntryCondition(0, 1.0, MoonRule.ANY, false, "", 0, false, 1, false);

    /** The first failing reason, or null when this entry holds. */
    public Reason check(TransformContext c) {
        if (c.tier() < minTier) return Reason.TIER;
        if (epMin > 0 && c.ep() < epMin * Math.max(0.0, c.epScale())) return Reason.EP;
        if (hpBelow < 1.0 && c.hpFraction() > hpBelow) return Reason.HEALTH;
        if (moon == MoonRule.NIGHT && !c.night()) return Reason.MOON;
        if (moon == MoonRule.FULL_MOON && !(c.night() && c.fullMoon())) return Reason.MOON;
        if (needsSky && !c.skyVisible()) return Reason.SKY;
        if (!item.isEmpty() && c.itemCount(item) < Math.max(1, itemCount)) return Reason.ITEM;
        return null;
    }

    /** Why a state cannot be entered. */
    public enum Reason {
        TIER, EP, HEALTH, MOON, SKY, ITEM, COOLDOWN, FROM, ALREADY, AURA, NOT_AVAILABLE, DISABLED
    }
}
