package com.newuniverse.nusmp.labyrinth.trial;

/**
 * Why: the life of a trial is a three-state machine (idle, sealed and fighting, cleared for good) and the whole of it is written as ONE pure
 * function, {@link #step}, so every transition is a line in {@code TrialStateTest} and the block entity cannot drift from the rules by
 * accident: it feeds the inputs it measured (players inside, a player in a doorway, mobs still alive, ticks since the last player, the
 * reset length) and performs whatever {@link Action} comes back. CLEARED is terminal by design: a room that was beaten stays open in the
 * saved world, the way a beaten dungeon room does in the games the Labyrinth borrows from. IDLE is re-entered only through a RESET, which
 * is how an abandoned trial can never soft-lock the room (the barriers fall, the orphaned mobs are discarded, the next party fights it fresh).
 * The arming rule exists because of the Spire's geometry: a chamber's keystone sits in the centre of its ceiling with the radius set to half
 * the footprint, and the Warden hall's arrival stair comes up through the middle of that box, so "a player is inside" alone would seal a
 * party that never walked through a door. A chamber WITH Ward Runes therefore activates only after some player has been seen standing in a
 * rune column since the last IDLE (the {@code armed} flag, saved in NBT, cleared on clear and reset), and nobody is standing in one now. A
 * chamber without runes has no doors to walk through and keeps the plain rule.
 */
public enum TrialState {
    IDLE, ACTIVE, CLEARED;

    /** What the block entity must do on a transition. */
    public enum Action { NONE, ACTIVATE, CLEAR, RESET }

    public record Step(TrialState next, Action action) {}

    /**
     * One decision of the machine.
     *
     * @param state           the state now
     * @param playersInside   is at least one survival or adventure player inside the box
     * @param playerInDoorway is one of them standing where a barrier column would rise (nobody is ever sealed inside a wall)
     * @param hasRunes        does the box hold any Ward Rune (a chamber with doors)
     * @param armed           has some player been seen in a rune column since the last IDLE (only read while IDLE and {@code hasRunes})
     * @param alive           how many of the mobs this trial spawned are still alive (only read while ACTIVE)
     * @param ticksSincePlayer ticks since a player was last inside (only read while ACTIVE)
     * @param resetTicks      how long an ACTIVE trial may be empty before it resets
     */
    public static Step step(TrialState state, boolean playersInside, boolean playerInDoorway, boolean hasRunes, boolean armed, int alive, long ticksSincePlayer, int resetTicks) {
        switch (state) {
            case IDLE:
                return playersInside && !playerInDoorway && (!hasRunes || armed) ? new Step(ACTIVE, Action.ACTIVATE) : new Step(IDLE, Action.NONE);
            case ACTIVE:
                if (alive <= 0) return new Step(CLEARED, Action.CLEAR);
                if (!playersInside && TrialMath.abandoned(ticksSincePlayer, 0, resetTicks)) return new Step(IDLE, Action.RESET);
                return new Step(ACTIVE, Action.NONE);
            default:
                return new Step(CLEARED, Action.NONE);
        }
    }

    /** The state saved under a name, IDLE for anything unknown (an old or hand-edited tag never breaks a chunk). */
    public static TrialState byName(String name) {
        if (name != null) for (TrialState s : values()) if (s.name().equalsIgnoreCase(name)) return s;
        return IDLE;
    }
}
