package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.nexus.zone.HiveTactics.State;

/**
 * The signs the hive gives of what it is about to do, so its intelligence is never a surprise: a horn or a howl and a line on the action bar
 * when it changes state, a colour over its banner that says which state it is in, and the warning a fake house gives. Pure: the glue turns the
 * words into sounds and particles.
 */
public final class HiveCues {
    private HiveCues() {}

    /** The call of a change of state: which sound the glue plays and how high. */
    public enum Call { NONE, ALARM, CHARGE, CLOSE_IN, RETREAT, REGROUP, QUIET }

    /** The call for a change of state. */
    public static Call call(State from, State to) {
        if (from == to) return Call.NONE;
        return switch (to) {
            case ASSEMBLE -> Call.ALARM;
            case ENGAGE -> Call.CHARGE;
            case SURROUND -> Call.CLOSE_IN;
            case FALL_BACK -> Call.RETREAT;
            case REFORM -> Call.REGROUP;
            case SEARCH -> Call.QUIET;
        };
    }

    /** The line shown to the players in the village, or an empty string when nothing is to be said. */
    public static String line(State from, State to) {
        if (from == to) return "";
        return switch (to) {
            case ASSEMBLE -> "Horns sound over the village: the swarm is rallying to its banner.";
            case ENGAGE -> "The swarm charges!";
            case SURROUND -> "The swarm is closing a ring round you!";
            case FALL_BACK -> "The swarm falls back to its banner.";
            case REFORM -> "The swarm regroups at its banner, mending its wounds.";
            case SEARCH -> from == State.SEARCH ? "" : "The swarm has lost your trail.";
        };
    }

    /** The colour over the banner for a state, {r, g, b} in 0 to 255: green while it searches, yellow gathering, red fighting, dark red ringing, blue falling back, teal regrouping. */
    public static int[] colour(State s) {
        return switch (s) {
            case SEARCH -> new int[]{80, 200, 110};
            case ASSEMBLE -> new int[]{240, 200, 60};
            case ENGAGE -> new int[]{230, 50, 40};
            case SURROUND -> new int[]{150, 10, 30};
            case FALL_BACK -> new int[]{70, 130, 230};
            case REFORM -> new int[]{70, 210, 190};
        };
    }

    /** The warning when someone steps near a house that is a fake, and what is said when it gives way. */
    public static final String FAKE_WARNING = "The walls of this house shimmer: it is not real!";
    public static final String FAKE_DISSOLVES = "The house dissolves into haze, and the swarm comes out of it!";

    /** The line on the action bar when the swarm's leader learns where the shots come from. */
    public static final String MARKED = "The swarm has marked the place you shoot from.";

    /** The line when the Nexus falls and the swarm unravels with the mirage. */
    public static final String UNRAVELS = "The mirage tears apart, and the swarm dissolves into haze.";
}
