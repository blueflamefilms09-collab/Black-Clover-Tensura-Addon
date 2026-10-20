package com.newuniverse.nusmp.labyrinth.mega.admin;

/**
 * Why: {@code /dungeon tp 700} must never freeze the server and must never simply fail because floor 700 has not been built yet. The Mega-Dungeon builds ONE floor at a time
 * over many ticks and a floor can only be queued once the floor before it stands, so reaching a far floor is a slow walk of small steps that has to be taken ACROSS ticks. This
 * class is the decision at each step, on plain facts about the player's road (no Minecraft type): is the target floor there (go), is it being built or is the newest floor being
 * built (wait), can the next floor be queued now (queue it), or must the request be given up and why (the generator is switched off, the dungeon reached its size limit, a floor
 * on the way failed, nothing has moved for too long). The service ({@code FloorTeleport}) gathers the facts from the saved data and carries the answer out, once every few ticks,
 * so a request for a floor that does not exist yet is QUEUED instead of refused, and a request that can never succeed ends with the reason instead of waiting for ever. Pure and
 * unit tested.
 */
public final class TpRules {
    private TpRules() {}

    /** No progress at all for this long (ticks, three minutes) and the request is given up: the generator is idle or stuck. */
    public static final long GIVE_UP_TICKS = 20L * 60L * 3L;

    /** What to do now. */
    public enum Step {
        /** The target floor is built: teleport. */
        ARRIVE,
        /** Something is being built (the target, or the newest floor on the way): check again later. */
        WAIT,
        /** The road has no floor at all yet: queue its first floor. */
        QUEUE_FIRST,
        /** The newest floor of the road is built and has no successor: queue the next one. */
        QUEUE_NEXT,
        /** A floor on the way (or the target) failed to generate. */
        FAIL_FAILED,
        /** The dungeon's size limit ({@code mega.maxSectors}) is reached before the target. */
        FAIL_LIMIT,
        /** Expansion is switched off ({@code mega.expansion}) and the target does not exist. */
        FAIL_OFF,
        /** No progress for {@link #GIVE_UP_TICKS}. */
        FAIL_STUCK;

        public boolean failed() { return name().startsWith("FAIL_"); }
    }

    /**
     * The facts about one player's road. {@code targetExists} is whether a sector stands at {@code targetIndex} in the chain at all; the {@code tail*} facts describe the newest
     * sector of the chain ({@code chainEmpty} when there is none, then the tail facts are ignored). {@code idleTicks} is how long nothing has changed (the tail, its state, the
     * generator's activity).
     */
    public record View(boolean chainEmpty, int tailIndex, boolean tailPlayable, boolean tailFailed, boolean tailHasNext, int targetIndex, boolean targetExists, boolean targetPlayable,
                       boolean targetFailed, int sectorCount, int maxSectors, boolean expansionOn, long idleTicks) {}

    public static Step decide(View v) {
        if (v.targetExists()) {
            if (v.targetPlayable()) return Step.ARRIVE;
            if (v.targetFailed()) return Step.FAIL_FAILED;
            return v.idleTicks() > GIVE_UP_TICKS ? Step.FAIL_STUCK : Step.WAIT;                    // the target itself is being built
        }
        if (v.chainEmpty()) {
            if (!v.expansionOn()) return Step.FAIL_OFF;
            if (v.sectorCount() >= v.maxSectors()) return Step.FAIL_LIMIT;
            return Step.QUEUE_FIRST;
        }
        if (v.tailFailed()) return Step.FAIL_FAILED;
        if (!v.tailPlayable() || v.tailHasNext()) return v.idleTicks() > GIVE_UP_TICKS ? Step.FAIL_STUCK : Step.WAIT;      // the newest floor is being built, or its successor already is
        if (v.tailIndex() >= v.targetIndex()) return Step.FAIL_STUCK;                              // the target should exist by now and does not: a lost link, nothing more to try
        if (!v.expansionOn()) return Step.FAIL_OFF;
        if (v.sectorCount() >= v.maxSectors()) return Step.FAIL_LIMIT;
        return Step.QUEUE_NEXT;
    }

    /** Floors still to build for a road whose newest sector has index {@code tailIndex} (-1 when it has none) before {@code targetIndex} stands. */
    public static int remaining(boolean chainEmpty, int tailIndex, int targetIndex) {
        return DungeonAdminMath.sectorsToBuild(chainEmpty ? -1 : tailIndex, targetIndex);
    }
}
