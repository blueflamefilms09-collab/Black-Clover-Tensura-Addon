package com.newuniverse.nusmp.companion.bond;

/**
 * Why: "who may a companion attack" in one pure rule, so every companion, every goal and the damage guard agree: a foe is something HOSTILE, or something that struck the player (or was struck by them
 * a moment ago, or is going for them); it is NEVER a player, a person (a villager, a character of the arcs), another companion or anything that can be talked to, a pet of the same player, or an ally
 * (a squadmate in the same FTB team, or anything the game itself calls allied). The game-side {@code Bond.foe} only collects the facts.
 */
public final class FoeRules {
    private FoeRules() {}

    /** How long (ticks) a blow to or from the player still counts as "tied up with them". */
    public static final int INVOLVED_TICKS = 200;

    public static boolean isFoe(boolean alive, boolean isPlayer, boolean isPerson, boolean isCompanionOrTalkable, boolean isOwnersPet, boolean isAlly, boolean hostile, boolean involvedWithOwner) {
        if (!alive || isPlayer || isPerson || isCompanionOrTalkable || isOwnersPet || isAlly) return false;
        return hostile || involvedWithOwner;
    }

    /** Is a blow at {@code stamp} (the owner's {@code tickCount} then) recent enough at {@code now}? */
    public static boolean recent(int stamp, int now) { return stamp > 0 && now >= stamp && now - stamp < INVOLVED_TICKS; }
}
