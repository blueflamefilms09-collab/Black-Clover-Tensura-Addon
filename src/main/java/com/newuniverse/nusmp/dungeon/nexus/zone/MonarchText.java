package com.newuniverse.nusmp.dungeon.nexus.zone;

/** The words of the Monarch's fight, apart from the code so the wording is checked: what the server is told, the title, the lines between phases. */
public final class MonarchText {
    private MonarchText() {}

    public static final String NAME = "The Monarch", SOLDIER = "Shadow Soldier";

    /** The server-wide line on its first waking. */
    public static String woken(String player) {
        String who = player == null || player.isBlank() ? "Something" : player;
        return "The Monarch of the deep vault has woken from its throne. " + who + " disturbed its sleep.";
    }

    /** The server-wide line when it falls. */
    public static String fallen(String player) {
        return player == null || player.isBlank() ? "The Monarch has fallen. Its hall begins to come down."
                : "The Monarch has fallen to " + player + ". Its hall begins to come down.";
    }

    /** The server-wide line when it falls after the hall has already begun to come down (a relic was taken first). */
    public static String fallenDuringCollapse(String player) {
        return player == null || player.isBlank() ? "The Monarch has fallen." : "The Monarch has fallen to " + player + ".";
    }

    public static String title() { return "THE MONARCH"; }

    public static String subtitle() { return "It rises from its throne"; }

    /** The line when the fight turns to a new phase (2 or 3); empty for phase 1. */
    public static String phase(int phase) {
        return switch (phase) {
            case 2 -> "The Monarch is enraged. Shadows gather at its feet.";
            case 3 -> "The Monarch will not be denied. The dark itself answers it.";
            default -> "";
        };
    }

    /** The name on the boss bar: the phase, and how many soldiers stand with it. */
    public static String bar(int phase, int soldiers) {
        StringBuilder sb = new StringBuilder(NAME).append("  ·  Phase ").append(Math.max(1, Math.min(3, phase)));
        if (soldiers > 0) sb.append("  ·  Shadows ").append(soldiers);
        return sb.toString();
    }

    /** The warning shown to the foes while a move winds up (action bar). */
    public static String warning(MonarchMove m) {
        return switch (m) {
            case SLAM -> "The floor shudders: jump the shockwave!";
            case RAIN -> "Shadows pool on the floor: leave the circles!";
            case SUMMON -> "Its shadow stirs: soldiers are coming.";
            case BEAM -> "A line is drawn across the hall: step off it!";
            case AUTHORITY -> "The air grows heavy: leave the field.";
        };
    }
}
