package com.newuniverse.nusmp.dungeon.nexus.collapse;

/** The words of the Treasury Collapse and how they are laid out (clock, compass word, action-bar line). Pure, so the wording is checked in tests. */
public final class CollapseText {
    private CollapseText() {}

    /** The server-wide line when a collapse begins (the owner's wording, docs/living_dungeon.md section 8). */
    public static final String BROADCAST = "[Server] The Mana Zone has destabilized! The Nexus is collapsing!";
    public static final String GASP = "The air is gone. Breathe through a Chalice, or run.";
    public static final String EXPELLED = "The Nexus is gone. The roots throw you out, bruised and sick with magicules.";
    public static final String OPENED = "A Root Gate has opened at each way out. Reach one before the clock runs out.";
    public static final String NO_EXIT = "There is no Root Gate in reach: follow the dungeon the way you came.";
    public static final String GUARDS = "Something mutated stirs at the exits.";
    public static final String WARN = "The ceiling above you is giving way: move!";

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
            case 3600 -> "The Nexus is coming down. " + clock + " to reach a Root Gate at the exits.";
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
        String head = "Nexus collapse " + clock(ticksLeft);
        if (Double.isNaN(distance)) return head;
        return head + " | Root Gate " + Math.round(distance) + " m " + direction;
    }

    /** A line for the status command: the stage, the clock and the progress of the fall in percent. */
    public static String status(CollapsePlan.Stage stage, long ticksLeft, int percent) {
        return stage.name().toLowerCase(java.util.Locale.ROOT) + ", " + clock(ticksLeft) + " left, " + Math.max(0, Math.min(100, percent)) + "% of the swept ground down";
    }
}
