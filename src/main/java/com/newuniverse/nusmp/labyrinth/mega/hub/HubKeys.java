package com.newuniverse.nusmp.labyrinth.mega.hub;

/**
 * Why: the Hub stores a few things in places that outlive a single call: the way back to the Overworld in the player's persistent data, the gate
 * cooldown that stops a gate from taking a player straight back, a tag on the mobs an admin spawned, and the name of the saved plaza record. Their
 * names are plain strings that several classes, the self-tests and the unit tests must spell identically, and two of them are not ours: the gate
 * cooldown keys belong to {@code LabyrinthTravel} (package-private there), and writing a different string would silently stop the cooldown from
 * working. So they are spelled once, here, copied from where they are set, and a unit test reads {@code LabyrinthTravel.java} to prove the copy is
 * still true. Pure (no Minecraft import).
 */
public final class HubKeys {
    private HubKeys() {}

    /** Persistent data of a player: where they came from when they entered Floor 0 (dimension id, x, y, z, yaw, pitch), the pattern of {@code nusmp_labyrinth_origin}. */
    public static final String ORIGIN = "nusmp_hub_origin";
    public static final String DIM = "Dim", X = "X", Y = "Y", Z = "Z", YAW = "Yaw", PITCH = "Pitch";

    /** Persistent data of a player: game time until which no Labyrinth gate takes them (the key {@code LabyrinthTravel.GATE_USED} writes and {@code GateBlock} reads). */
    public static final String GATE_USED = "nusmp_labyrinth_gate";
    /** Persistent data of a player: the same stamp for the legacy Dungeon Maw and Root Gate ({@code LabyrinthTravel.LEGACY_GATE_USED}). */
    public static final String LEGACY_GATE_USED = "nusmp_dungeon_gate";

    /** Persistent data of a mob: set when an admin spawned a hostile mob in the Sanctuary, so the sweep leaves it alone. */
    public static final String ALLOWED = "nusmp_hub_allowed";

    /** Persistent data of a player: game time of the last "this is protected" message, so a held mouse button does not flood the action bar. */
    public static final String TOLD = "nusmp_hub_told";

    /** The saved-data file of the plaza record: {@code <sanctuary dimension>/data/nusmp_hub.dat}. */
    public static final String DATA_NAME = "nusmp_hub";
}
