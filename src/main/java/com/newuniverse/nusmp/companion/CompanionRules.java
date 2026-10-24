package com.newuniverse.nusmp.companion;

import java.util.UUID;

/**
 * Why: the numbers and rules of the bond between a player and a companion in ONE pure class (no Minecraft type), so they are unit tested and cannot drift between the gift code, the high five, the talk
 * screen and the pouch. Rapport (friendship) is a number 0..99 kept per player; it has four tiers (stranger under 3, acquaintance under 10, friend under 25, close from 25). A high five earns 1 the
 * first time each in-game day; a loved gift earns 5 (the first two a day, then 1), a plain food gift 2, a hated gift costs 1; something that cannot be eaten and is not on her list is refused and
 * not taken. Days are counted from the game clock ({@code gameTime / 24000}), which never goes back, so sleeping or {@code /time set} cannot reset a daily allowance. The pouch holds nine slots; a
 * companion that walks with the player may use one on them when their health is below two fifths, at most once every 30 seconds.
 */
public final class CompanionRules {
    private CompanionRules() {}

    // ---------------------------------------------------------------- rapport
    public static final int STRANGER = 0, ACQUAINTANCE = 1, FRIEND = 2, CLOSE = 3;
    /** Rapport at which each tier starts. */
    public static final int ACQUAINTANCE_AT = 3, FRIEND_AT = 10, CLOSE_AT = 25;
    public static final int RAPPORT_MAX = 99;

    /** The tier of a rapport number: 0 stranger (under 3), 1 acquaintance (under 10), 2 friend (under 25), 3 close. */
    public static int tier(int rapport) {
        if (rapport >= CLOSE_AT) return CLOSE;
        if (rapport >= FRIEND_AT) return FRIEND;
        if (rapport >= ACQUAINTANCE_AT) return ACQUAINTANCE;
        return STRANGER;
    }

    /** Rapport kept inside 0..99. */
    public static int clampRapport(long rapport) { return (int) Math.max(0L, Math.min(RAPPORT_MAX, rapport)); }

    /** The rapport after adding {@code delta} (which may be negative), kept inside 0..99. */
    public static int addRapport(int rapport, int delta) { return clampRapport((long) rapport + delta); }

    // ---------------------------------------------------------------- gifts
    /** How a companion takes a gift. */
    public enum Gift {
        /** On her loved list. */ LOVED,
        /** On her accepted list (something that is not food but she is glad of). */ ACCEPTED,
        /** Any other food. */ FOOD,
        /** On her hated list. */ HATED,
        /** Not food and not on a list: she cannot take it, nothing is used up. */ REFUSED;

        /** Is the item used up? (Only a refused gift stays with the giver.) */
        public boolean taken() { return this != REFUSED; }
    }

    public static final int LOVED_FULL = 5, LOVED_FULL_PER_DAY = 2, LOVED_REDUCED = 1, PLAIN_GIFT = 2, HATED_GIFT = -1, HIGH_FIVE = 1;

    /** Classifies an item. A hated item stays hated even when it is food; a loved one is loved even when it is not food. */
    public static Gift classify(boolean loved, boolean accepted, boolean hated, boolean food) {
        if (hated) return Gift.HATED;
        if (loved) return Gift.LOVED;
        if (accepted) return Gift.ACCEPTED;
        return food ? Gift.FOOD : Gift.REFUSED;
    }

    /** The rapport a gift earns (negative for a hated one); {@code lovedToday} is how many loved gifts she already took from this player today. */
    public static int giftRapport(Gift gift, int lovedToday) {
        return switch (gift) {
            case LOVED -> lovedToday < LOVED_FULL_PER_DAY ? LOVED_FULL : LOVED_REDUCED;
            case ACCEPTED, FOOD -> PLAIN_GIFT;
            case HATED -> HATED_GIFT;
            case REFUSED -> 0;
        };
    }

    // ---------------------------------------------------------------- days
    public static final long DAY_TICKS = 24000L;

    /** The day number of a game time. */
    public static long day(long gameTime) { return Math.max(0L, gameTime) / DAY_TICKS; }

    /** How many loved gifts count for today: the stored count when it was stored today, else none. */
    public static int lovedToday(long storedDay, int storedCount, long today) { return storedDay == today ? Math.max(0, storedCount) : 0; }

    /** Does a high five earn rapport now (the first one each day)? */
    public static boolean fiveEarns(long lastFiveDay, long today) { return lastFiveDay != today; }

    // ---------------------------------------------------------------- the high five
    /** The two kinds of social pose: none, and the high five. */
    public static final int NONE = 0, FIVE = 1;
    /** How long the high five lasts. */
    public static final int FIVE_TICKS = 40;
    /** A kind outside the known ones reads as none (a synced value from an older or newer build). */
    public static int cleanKind(int kind) { return kind == FIVE ? FIVE : NONE; }

    // ---------------------------------------------------------------- ownership
    /** Are the stance buttons shown to this viewer? Only for a fighter, and only to the player she walks with (to anyone while she is nobody's). */
    public static boolean stanceOffered(boolean fighter, UUID owner, UUID viewer) {
        return fighter && viewer != null && (owner == null || owner.equals(viewer));
    }

    /** May this viewer use the pouch? She must walk with them, or be nobody's and a friend (tier 2) of theirs. */
    public static boolean pouchOpen(UUID owner, UUID viewer, int tier) {
        if (viewer == null) return false;
        return owner == null ? tier >= FRIEND : owner.equals(viewer);
    }

    // ---------------------------------------------------------------- the pouch
    public static final int POUCH_SLOTS = 9;
    /** She looks after the player when their health is below this share of the maximum (two fifths). */
    public static final float USE_BELOW = 0.4f;
    /** Ticks between two uses (30 seconds). */
    public static final int USE_GAP = 600;

    /** Should she use something from the pouch on the player now? */
    public static boolean usePouchNow(boolean allowed, boolean switchedOn, float health, float maxHealth, long now, long lastUse) {
        if (!allowed || !switchedOn || maxHealth <= 0f || health <= 0f) return false;
        if (now - lastUse < USE_GAP && now >= lastUse) return false;
        return health < maxHealth * USE_BELOW;
    }

    /** Item kinds for {@link #bestHeal}. */
    public static final int NOT_USEFUL = 0, FOOD_KIND = 1, POTION_KIND = 2;

    /**
     * Which pouch slot to use: a healing potion first, else the most nourishing food; -1 when nothing helps. {@code kinds[i]} is {@link #NOT_USEFUL}, {@link #FOOD_KIND} or {@link #POTION_KIND};
     * {@code value[i]} is the nutrition of a food or the strength of a potion (higher is better). The first slot wins a tie.
     */
    public static int bestHeal(int[] kinds, int[] value) {
        int best = -1;
        for (int i = 0; i < kinds.length && i < value.length; i++) {
            if (kinds[i] <= NOT_USEFUL) continue;
            if (best < 0 || kinds[i] > kinds[best] || (kinds[i] == kinds[best] && value[i] > value[best])) best = i;
        }
        return best;
    }
}
