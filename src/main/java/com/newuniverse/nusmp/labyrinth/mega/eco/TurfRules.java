package com.newuniverse.nusmp.labyrinth.mega.eco;

/**
 * Why: whether a goblin may go for an ogre, and from how far, is the heart of the turf wars, and it is a handful of comparisons that must be
 * right in every corner (allies, the same faction, the native bonus, the constructs that only defend their own room). Written as pure
 * functions of the {@link FactionGraph} and a few numbers, the goal that runs it every second per monster does no more than collect
 * positions, and the whole rule set is tested without a server. {@link Relation#RIVAL} means "hunt on sight anywhere"; {@link Relation#INTRUDER}
 * means "not a rival, but standing in my own room, and I am territorial". The reach is {@code range} blocks, plus the native bonus when the
 * other mob stands inside the room that is native to the one deciding, so a faction defends its home farther than it hunts abroad. Pure (no
 * Minecraft types).
 */
public final class TurfRules {
    private TurfRules() {}

    /** Why: the turf rules have exactly three answers and naming them keeps the callers honest: leave it alone, hunt it anywhere, or defend the home room against it. */
    public enum Relation { NONE, RIVAL, INTRUDER }

    /** How a member of {@code own} regards a member of {@code other} that does (or does not) stand inside {@code own}'s native room. */
    public static Relation relation(FactionGraph graph, FactionDef own, String other, boolean otherInOwnRoom) {
        if (own == null || other == null || own.id().equals(other) || graph.allied(own.id(), other)) return Relation.NONE;
        if (graph.hostile(own.id(), other)) return Relation.RIVAL;
        return own.territorial() && otherInOwnRoom ? Relation.INTRUDER : Relation.NONE;
    }

    /** Blocks within which a member of {@code own} notices a member of {@code other}; 0 means it leaves it alone. */
    public static double reach(FactionGraph graph, FactionDef own, String other, boolean otherInOwnRoom, int range, int nativeBonus) {
        if (relation(graph, own, other, otherInOwnRoom) == Relation.NONE) return 0.0;
        return otherInOwnRoom ? range + nativeBonus : range;
    }

    /** The scan range of a faction: its file's value, else the server config's, kept inside the hard ceiling. */
    public static int range(FactionDef def, int configRange) {
        int r = def.range() != FactionDef.UNSET ? def.range() : configRange;
        return Math.max(0, Math.min(FactionDef.MAX_RANGE, r));
    }

    /** The native bonus of a faction: its file's value, else the server config's, kept inside the hard ceiling. */
    public static int nativeBonus(FactionDef def, int configBonus) {
        int b = def.nativeBonus() != FactionDef.UNSET ? def.nativeBonus() : configBonus;
        return Math.max(0, Math.min(FactionDef.MAX_BONUS, b));
    }

    /** The gap between hunter and target beyond which the hunt is dropped (a chase may run past the notice range, but not for ever). */
    public static double giveUpDistance(int range, int nativeBonus) {
        return (range + nativeBonus) * 1.5 + 4.0;
    }
}
