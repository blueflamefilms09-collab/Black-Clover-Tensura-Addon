package com.newuniverse.nusmp.labyrinth.mega.guard;

/**
 * Why: the Guardian Altar is a three-state machine (IDLE, ACTIVE, DONE) copied from the Peak Altar, which has run since 1.35.0, with the
 * changes the Mega-Dungeon needs; its DECISIONS are written here as one pure function of what the block entity sees, so every branch can be
 * unit-tested without a world and the block entity only gathers the facts and carries the decision out. The one deliberate difference from
 * the Peak Altar: a boss that is simply GONE (discarded by a command, removed by another mod, its chunk reloaded without it) is a RESET, not
 * a victory. The Peak Altar finishes in that case because its door opens for free; here finishing forges a Mana Core and unseals the
 * sector, which must only ever follow a real death, so the Guardian reports its own death ({@code bossReportedDead}) and the altar also
 * trusts what it sees ({@code bossDead}); nothing else finishes the fight.
 */
public final class AltarRules {
    private AltarRules() {}

    public enum State { IDLE, ACTIVE, DONE }

    /** What the block entity does next. */
    public enum Act { NONE, SUMMON, FINISH, RESET }

    /** The altar polls this often (ticks): cheap, and shorter than the 20 ticks a dying entity stays visible. */
    public static final int POLL_TICKS = 10;
    /** After a summon, how long (ticks) a missing boss is given to show up before it counts as gone. */
    public static final int SPAWN_GRACE_TICKS = 100;
    /** A party must stay away this many blocks beyond the approach radius before the fight counts as abandoned. */
    public static final int LEAVE_MARGIN = 16;

    /**
     * What the block entity saw this poll.
     * @param state             the altar's state
     * @param playerNear        a survival player is within the approach radius
     * @param playerNearWide    a survival player is within radius + {@link #LEAVE_MARGIN}
     * @param sectorReady       the sector (if any) may host a fight (not still generating, not failed)
     * @param bossFound         the boss entity was found by its UUID
     * @param bossDead          the found boss is dead or dying
     * @param bossReportedDead  the Guardian told the altar it died (it saves the death even if the entity is already gone)
     * @param bossPosKnown      the altar knows where the boss was last seen
     * @param bossPosLoaded     that spot is loaded (an unloaded spot proves nothing about the boss)
     * @param now               the game time
     * @param since             when the current state began
     * @param idleSince         the last time a player was within the wide radius while ACTIVE (below 0 = never)
     * @param abandonTicks      how long the party must be away before the fight is abandoned
     */
    public record Look(State state, boolean playerNear, boolean playerNearWide, boolean sectorReady, boolean bossFound, boolean bossDead,
                       boolean bossReportedDead, boolean bossPosKnown, boolean bossPosLoaded, long now, long since, long idleSince, long abandonTicks) {}

    public static Act decide(Look l) {
        switch (l.state()) {
            case IDLE:
                return l.playerNear() && l.sectorReady() ? Act.SUMMON : Act.NONE;
            case ACTIVE:
                if (l.bossReportedDead()) return Act.FINISH;
                if (l.bossFound()) {
                    if (l.bossDead()) return Act.FINISH;
                    return abandoned(l) ? Act.RESET : Act.NONE;
                }
                // not found: either not spawned yet / its chunk is unloaded (wait), or it vanished without dying (start over)
                if (l.now() - l.since() > SPAWN_GRACE_TICKS && (!l.bossPosKnown() || l.bossPosLoaded())) return Act.RESET;
                return Act.NONE;
            default:
                return Act.NONE;
        }
    }

    /** The party left: nobody within the wide radius for {@code abandonTicks}. */
    static boolean abandoned(Look l) {
        return !l.playerNearWide() && l.idleSince() >= 0 && l.now() - l.idleSince() >= l.abandonTicks();
    }

    /** The new "last time a player was near" after a poll: now if someone is within the wide radius, else unchanged. */
    public static long idleSince(long previous, boolean playerNearWide, long now) { return playerNearWide ? now : previous; }

    /** An ACTIVE altar that lost its boss id (an old save, a hand-edited block entity) cannot finish: it starts over as IDLE. */
    public static State sanitize(State loaded, boolean hasBossId) { return loaded == State.ACTIVE && !hasBossId ? State.IDLE : loaded; }

    /** Seconds from the config to ticks, never below one poll. */
    public static long abandonTicks(int seconds) { return Math.max(POLL_TICKS, (long) seconds * 20L); }
}
