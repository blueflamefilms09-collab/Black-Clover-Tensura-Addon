package com.newuniverse.nusmp.labyrinth.mega.system;

/**
 * Why: the owner's rule is that the System itself (window, quests, rewards, shadows) is for everyone in the dungeon, but the BUFF of the stat points works only
 * inside the dungeon dimensions: the Mega-Dungeon and its Hub (Floor 0, the Sanctuary). "Is the buff on here?" is one yes/no question asked from several places (a
 * dimension change, a login, a respawn, a stat reward, the periodic safety check, the status command), so the answer lives in one pure function with no Minecraft
 * type: a dimension id and the {@code system.buffOnlyInDungeon} switch go in, a boolean comes out, and a unit test pins every case. With the switch off (the old
 * behaviour of 1.55.0) the answer is always yes. The ids are plain strings on purpose (so the class is testable without the game); a self-test in
 * {@link SystemSelfTests} compares them with the real dimension keys, so a renamed dimension fails the CI smoke test instead of silently turning the buff off.
 * Pure.
 */
public final class BuffGate {
    private BuffGate() {}

    /** {@code nusmp:mega_dungeon}: both roads of the Mega-Dungeon. */
    public static final String MEGA = "nusmp:mega_dungeon";
    /** {@code nusmp:sanctuary}: Floor 0, the Hub. */
    public static final String HUB = "nusmp:sanctuary";

    /** Is this one of the dungeon dimensions (the Mega-Dungeon or the Hub)? A null or unknown id is not. */
    public static boolean isDungeonDimension(String dimensionId) {
        return MEGA.equals(dimensionId) || HUB.equals(dimensionId);
    }

    /**
     * Should the stat modifiers be on the player right now?
     *
     * @param dimensionId       the id of the dimension the player is in ({@code namespace:path}), or null
     * @param buffOnlyInDungeon the {@code system.buffOnlyInDungeon} switch: true = only in the dungeon dimensions, false = everywhere (the 1.55.0 behaviour)
     */
    public static boolean active(String dimensionId, boolean buffOnlyInDungeon) {
        return !buffOnlyInDungeon || isDungeonDimension(dimensionId);
    }

    /**
     * Does the player's modifier state have to change? {@code applied} is what was last put on the player (null = never looked at), {@code wanted} is what
     * {@link #active} says now. A player nobody has looked at yet always needs one look.
     */
    public static boolean needsChange(Boolean applied, boolean wanted) {
        return applied == null || applied != wanted;
    }
}
