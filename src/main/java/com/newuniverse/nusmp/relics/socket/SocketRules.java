package com.newuniverse.nusmp.relics.socket;

import com.newuniverse.nusmp.relics.djinn.BondRules;

/**
 * The rules of the sockets, pure and tested (tier numbers are {@code Grade.tier()}: 1 Normal ... 7 Genesis).
 * <ul>
 *   <li>{@link #slots}: weapons and armour relics of tier 3 (Rare) and up have one socket, tier 5 (Legend) and up have two; other relics have none
 *       unless a wave opted them in ({@link SocketCatalog#allowSocket}), which gives them one (two from Legend);</li>
 *   <li>{@link #insert}: the checks of putting a Core in, in the order the player is told about them;</li>
 *   <li>{@link #castSlot}: which socket's ability a sneak-use fires;</li>
 *   <li>{@link #lifesteal}, {@link #actAmount}: the numbers of the passive life steal and of an ability's damage and healing.</li>
 * </ul>
 */
public final class SocketRules {
    private SocketRules() {}

    public static final int RARE_TIER = 3, LEGEND_TIER = 5;
    /** A life steal never returns more than this many health points from one hit. */
    public static final float LIFESTEAL_HIT_CAP = 4f;
    /** ... nor more than this share of the wearer's maximum health in one second. */
    public static final float LIFESTEAL_SECOND_CAP = 0.15f;
    /** Looking this far up (pitch at or below the negative of it) while sneak-using fires the second socket. */
    public static final float LOOK_UP_PITCH = 60f;

    /** How many sockets a relic has. {@code eligibleSlot}: a weapon in the hand or armour on the head, chest or feet. */
    public static int slots(int tier, boolean eligibleSlot, boolean optIn) {
        if (!eligibleSlot && !optIn) return 0;
        if (tier >= LEGEND_TIER) return 2;
        if (tier >= RARE_TIER || optIn) return 1;
        return 0;
    }

    public enum Outcome { OK, NO_SLOTS, FULL, DUPLICATE, BOND_CONFLICT }

    /**
     * May this Core go into the relic? {@code heldBonds} is the player's own bond mask ({@link BondRules}); a Spirit Core is refused to a player who holds
     * a Devil or a Djinn and a Devil Core to one who holds a Spirit or a Djinn, unless the multi-bond relic is worn. A relic-bound Spirit never adds to
     * the player's own bonds (the mask is only ever read, never written, here).
     */
    public static Outcome insert(SocketData cur, int slots, SocketDef core, int heldBonds, boolean multiBond) {
        if (core == null || slots <= 0) return Outcome.NO_SLOTS;
        if (cur.has(core.id())) return Outcome.DUPLICATE;
        if (cur.size() >= Math.min(slots, SocketData.MAX)) return Outcome.FULL;
        if (!BondRules.canBind(heldBonds, core.kind().bondBit(), multiBond)) return Outcome.BOND_CONFLICT;
        return Outcome.OK;
    }

    /** Does a bound socket of this kind still work for a player with these bonds? The same exclusivity as binding: a conflicting bond silences the socket (it is not lost). */
    public static boolean usable(SocketKind kind, int heldBonds, boolean multiBond) {
        return BondRules.canBind(heldBonds, kind.bondBit(), multiBond);
    }

    /**
     * Which socket a sneak-use fires, or -1 for none. Plain sneak-use fires the first socket when the relic has fewer than two acts of its own (the
     * second act of a relic is already on sneak-use); looking steeply up while sneaking fires the second socket (the first if there is only one).
     */
    public static int castSlot(boolean sneaking, float pitch, int relicActs, int bound) {
        if (!sneaking || bound <= 0) return -1;
        if (pitch <= -LOOK_UP_PITCH) return bound >= 2 ? 1 : 0;
        return relicActs < 2 ? 0 : -1;
    }

    /** Health back from a hit of {@code dealt} with {@code fraction} life steal: never above the per-hit cap, and never below 0. */
    public static float lifesteal(float dealt, double fraction) {
        if (!(dealt > 0f) || !(fraction > 0.0)) return 0f;
        return (float) Math.min(LIFESTEAL_HIT_CAP, dealt * fraction);
    }

    /** What a life steal may still return this second, given what it returned already. */
    public static float lifestealLeft(float maxHealth, float returnedThisSecond) {
        return Math.max(0f, maxHealth * LIFESTEAL_SECOND_CAP - returnedThisSecond);
    }

    /** An ability's figure (damage or healing) at a tier: the spec's power, up to 60% more at Genesis; the balance law caps what it does after that. */
    public static double actAmount(double power, int tier) {
        int t = Math.max(1, Math.min(7, tier));
        return power * (1.0 + 0.1 * (t - 1));
    }

    /** The share of an incoming damage that is kept after a set of resistances (each a fraction): multiplied, and never below 10%. */
    public static double keep(double... fractions) {
        double k = 1.0;
        for (double f : fractions) k *= 1.0 - Math.max(0.0, Math.min(0.9, f));
        return Math.max(0.1, k);
    }
}
