package com.newuniverse.nusmp.dungeon.nexus.collapse;

/**
 * The words of the Treasury Collapse and how they are laid out (clock, compass word, action-bar line). It is the Dungeon that collapses,
 * and the Dungeon that speaks of it: no line a player reads says "Nexus". Pure, so the wording is checked in tests.
 */
public final class CollapseText {
    private CollapseText() {}

    /** The server-wide line when a collapse begins (the owner's wording, docs/living_dungeon.md section 8). */
    public static final String BROADCAST = "[Server] The Dungeon wails in agony! The floor is collapsing!";
    /** The title and the line under it that the players inside get when it begins. */
    public static final String TITLE = "THE DUNGEON IS COLLAPSING", TITLE_SUB = "3:00 to reach a Root Gate";
    public static final String GASP = "The air is gone. Breathe through a Chalice, or run.";
    /** The end: the title and the line under it, and the chat line. */
    public static final String EXPEL_TITLE = "THE DUNGEON IS DONE WITH YOU", EXPEL_SUB = "the roots throw you out";
    public static final String EXPELLED = "The Dungeon is done with you. The roots throw you out, bruised and sick with magicules.";
    public static final String OPENED = "The Dungeon has opened a Root Gate at each way out. Reach one before the floor is gone.";
    public static final String NO_EXIT = "There is no Root Gate in reach: follow the dungeon the way you came.";
    public static final String GUARDS = "Something frenzied stirs in the walls.";
    public static final String WARN = "The Dungeon's ceiling is giving way above you: move!";
    /** The Spatial Surge: the tear opening under a player, the throw, the Anchor holding, the tear closing unused, and a throw that found no place. */
    public static final String SURGE = "Space tears open under you!";
    public static final String SURGE_LAND = "Space folds. The Dungeon flings you across the floor.";
    public static final String SURGE_HELD = "The Anchor holds you. The tear finds nothing to take.";
    public static final String SURGE_CLOSED = "The tear closes.";
    public static final String SURGE_FIZZLED = "The tear ripples, and settles.";

    /** "3:00" from the ticks left (rounded up to whole seconds, so 0:01 shows until the very end). */
    public static String clock(long ticks) {
        long s = Math.max(0L, (ticks + 19L) / 20L);
        return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
    }

    /** The line sent to the players inside when a countdown mark (an index of {@link CollapsePlan#MARKS}) is reached. */
    public static String mark(int index) {
        if (index < 0 || index >= CollapsePlan.MARKS.length) return "";
        int left = CollapsePlan.MARKS[index];
        String clock = clock(left);
        return switch (left) {
            case 3600 -> "The Dungeon is coming down. " + clock + " to reach a Root Gate at the exits.";
            case 2400 -> clock + " left. The walls begin to roll.";
            case 1200 -> clock + " left. Run for the Root Gates.";
            case 600 -> clock + " left. Soon only the corridors will be left standing.";
            case 200 -> clock + ".";
            default -> String.valueOf(left / 20);
        };
    }

    /** The eight compass words, north first, clockwise. */
    private static final String[] WORDS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    /** The compass word of an offset (dx east, dz south, as in the world: north is negative z); "here" for no offset. */
    public static String direction(double dx, double dz) {
        if (Math.abs(dx) < 0.5 && Math.abs(dz) < 0.5) return "here";
        double deg = Math.toDegrees(Math.atan2(dx, -dz));                    // 0 north, 90 east
        int sector = (int) Math.floor((deg + 360.0 + 22.5) / 45.0) % 8;
        return WORDS[sector];
    }

    /** The action-bar line for a player inside: the clock and the way to the nearest gate (a blank distance means none is known). */
    public static String bar(long ticksLeft, double distance, String direction) {
        String head = "Collapse " + clock(ticksLeft);
        if (Double.isNaN(distance)) return head;
        return head + " | Root Gate " + Math.round(distance) + " m " + direction;
    }

    /** A line for the status command: the stage, the clock and the progress of the fall in percent. */
    public static String status(CollapsePlan.Stage stage, long ticksLeft, int percent) {
        return stage.name().toLowerCase(java.util.Locale.ROOT) + ", " + clock(ticksLeft) + " left, " + Math.max(0, Math.min(100, percent)) + "% of the swept ground down";
    }
}
