package com.newuniverse.nusmp.labyrinth.mega.enc;

import com.newuniverse.nusmp.labyrinth.mega.RoomKind;

/**
 * Why: the Dungeon Master and the wandering bosses must leave certain rooms alone, and "which rooms" is one rule that both must agree on. The
 * Landing and the Safe Zones are the rest places (rest needs a room where nothing spawns and nothing hunts). The Guardian's hall, the room
 * behind its Gatekeeper and the Crucible Vault are the boss chain: a fight there is already scripted, the Crucible's catastrophe is running
 * there, and a second wave of monsters, or a Warden walking in on a pick-your-relic moment, would make the scripted moment unreadable. Nothing
 * is spawned in these rooms, no party standing in them is rolled for, no wanderer enters them, and a player in one is invisible to a wanderer.
 * Pure (the room kinds are plain enum values).
 */
public final class Sanctuary {
    private Sanctuary() {}

    /** Is this kind of room off limits to the Dungeon Master's monsters and to wandering bosses? */
    public static boolean is(RoomKind kind) {
        return kind != null && (kind.isSafe() || kind == RoomKind.ARENA || kind == RoomKind.ANTECHAMBER || kind == RoomKind.VAULT);
    }
}
