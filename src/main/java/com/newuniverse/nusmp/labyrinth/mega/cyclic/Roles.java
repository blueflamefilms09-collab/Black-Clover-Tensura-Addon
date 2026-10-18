package com.newuniverse.nusmp.labyrinth.mega.cyclic;

import com.newuniverse.nusmp.labyrinth.mega.RoomKind;

/**
 * Why: not every room with one door is a "dead end" in the sense the owner means. The arrival stairs, the forward stairs, the Guardian's hall and its vault
 * are DESTINATIONS: a player is meant to arrive there and turn round, and a Safe Zone is a place to rest. A dead-end metric that counted them would be
 * unsatisfiable (every floor has at least three of them) and a loop pass that tried to open them would break the design (a second door into the arena
 * would walk round the Gatekeeper). A role is a bit on a room; the planner reads three masks built from them: which rooms are TERMINALS (excluded from the
 * dead-end metric and never pruned), which are LOCKED (no new doorway may be cut into them) and which are PROTECTED (never pruned).
 */
public final class Roles {
    private Roles() {}

    /** The room where a party arrives (the arrival stairwell, or the Landing of the old model). */
    public static final int ENTRANCE = 1;
    /** A stairwell (the way on, or the way back). */
    public static final int STAIRS = 2;
    /** The boss chain: arena, antechamber, vault, Way Onward. Its doors are the Gatekeeper's business, so the loop pass never adds one. */
    public static final int BOSS = 4;
    /** A Safe Zone (a place to rest; a destination). */
    public static final int SAFE = 8;
    /** A room the recipe requires (treasure closet, trial ...) and that therefore must not be pruned away. */
    public static final int REQUIRED = 16;
    /** A hallway room added by the loop pass itself. */
    public static final int HALLWAY = 32;

    /** Rooms that are intended destinations: they are not dead ends and are never pruned. */
    public static final int TERMINAL = ENTRANCE | STAIRS | BOSS | SAFE;
    /** Rooms the pass may not cut a new doorway into. */
    public static final int LOCKED = ENTRANCE | STAIRS | BOSS;
    /** Rooms the pruner may not remove. */
    public static final int PROTECTED = TERMINAL | REQUIRED;

    /** The roles the generator's own kinds imply (the adapter adds REQUIRED for the recipe's minimum rooms). */
    public static int of(RoomKind kind) {
        if (kind == null) return 0;
        return switch (kind) {
            case LANDING -> ENTRANCE;
            case STAIRWELL -> STAIRS;
            case ARENA, ANTECHAMBER, VAULT, ONWARD -> BOSS;
            case SAFE -> SAFE;
            default -> 0;
        };
    }
}
