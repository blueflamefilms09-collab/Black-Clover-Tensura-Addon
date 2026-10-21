package com.newuniverse.nusmp.labyrinth.mega.poise;

/**
 * Why: WHO carries a poise bar is one decision, and it must not drift between the places that ask (the squad hook, the join event, the first blow). A bearer is a monster of the
 * dungeon that is an ELITE (the {@code nusmp_elite} tag, or a Mob Foundry mob of the elite tier or above) or a squad CAPTAIN (the squad's leader). Bosses are never bearers (they have their
 * own phases and a DPS budget), nor are servants of a player, nor a summoner's minions (a summoner is not a farm and a minion is not an elite). A captain that is also an elite is a
 * captain (the bigger bar). Pure.
 */
public final class PoiseBearers {
    private PoiseBearers() {}

    public enum Kind { NONE, ELITE, CAPTAIN }

    /** The tag any system can put on a monster to make it an elite. */
    public static final String ELITE_TAG = "nusmp_elite";

    public static Kind classify(boolean inDungeon, boolean boss, boolean servant, boolean minion, boolean captain, boolean eliteTag, int foundryTier, int eliteTier) {
        if (!inDungeon || boss || servant || minion) return Kind.NONE;
        if (captain) return Kind.CAPTAIN;
        if (eliteTag || (foundryTier > 0 && foundryTier >= eliteTier)) return Kind.ELITE;
        return Kind.NONE;
    }
}
