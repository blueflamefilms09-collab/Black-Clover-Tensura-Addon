package com.newuniverse.nusmp.arena;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Why: the Hourglass of the Final Hour is a teleport, and a teleport that can leave a boss fight, enter the Labyrinth or drag a stranger
 * along needs its rules written down in one place and tested. This pure class (no Minecraft types) answers three questions from plain facts:
 * <ul>
 *   <li><b>May it be used here, and what does it do?</b> ({@link #decide}) In the Labyrinth or a dungeon dimension: no. In the arena: it
 *       takes you home, with a 3-second channel, or a 10-second one while the boss fights (leaving mid-fight is allowed, but it costs time).
 *       Anywhere else it takes you to the arena with a 3-second channel, unless you were hurt in the last 5 seconds (it is not an escape
 *       from another fight) or the boss is already fighting in the arena (the fight is closed to latecomers).</li>
 *   <li><b>Who comes along?</b> ({@link #pickTravellers}) Players within 8 blocks, party members first, then the nearest, never one who is
 *       hostile to the user, at most 7 companions.</li>
 *   <li><b>How long is the channel?</b> The numbers are the constants below; the 60-second cooldown is vanilla's item cooldown, put on the
 *       hourglass when it is used to LEAVE (so entering never locks you in the arena).</li>
 * </ul>
 */
public final class HourglassRules {
    private HourglassRules() {}

    public static final int ENTER_TICKS = 60, LEAVE_TICKS = 60, LEAVE_FIGHT_TICKS = 200, COOLDOWN_TICKS = 1200, HURT_WINDOW = 100;
    public static final double COMPANION_RANGE = 8.0;
    public static final int MAX_COMPANIONS = 7;

    public enum Place { FREE, FORBIDDEN, ARENA }

    public enum Action { ENTER, LEAVE }

    public enum Denial { NONE, FORBIDDEN_DIMENSION, RECENTLY_HURT, FIGHT_IN_PROGRESS }

    /** The answer: either a denial, or an action with its channel in ticks. */
    public record Decision(Denial denial, Action action, int channelTicks) {
        public boolean allowed() { return denial == Denial.NONE; }
    }

    /**
     * @param place        where the user stands
     * @param fightRunning is the boss fighting in the arena right now
     * @param ticksSinceHurt ticks since the user last took damage from a creature (a big number for never)
     */
    public static Decision decide(Place place, boolean fightRunning, long ticksSinceHurt) {
        if (place == Place.FORBIDDEN) return new Decision(Denial.FORBIDDEN_DIMENSION, null, 0);
        if (place == Place.ARENA) return new Decision(Denial.NONE, Action.LEAVE, fightRunning ? LEAVE_FIGHT_TICKS : LEAVE_TICKS);
        if (ticksSinceHurt < HURT_WINDOW) return new Decision(Denial.RECENTLY_HURT, null, 0);
        if (fightRunning) return new Decision(Denial.FIGHT_IN_PROGRESS, null, 0);
        return new Decision(Denial.NONE, Action.ENTER, ENTER_TICKS);
    }

    /** A player near the user. */
    public record Candidate(UUID id, double distance, boolean partyMember, boolean hostile) {}

    /** The companions that travel: in range, not hostile, party first and then by distance, at most {@code max}. */
    public static List<UUID> pickTravellers(List<Candidate> candidates, double range, int max) {
        List<Candidate> ok = new ArrayList<>();
        if (candidates != null) {
            for (Candidate c : candidates) if (c != null && c.id() != null && !c.hostile() && c.distance() <= range && !Double.isNaN(c.distance())) ok.add(c);
        }
        ok.sort(Comparator.comparing((Candidate c) -> !c.partyMember()).thenComparingDouble(Candidate::distance));
        List<UUID> out = new ArrayList<>();
        for (Candidate c : ok) {
            if (out.size() >= Math.max(0, max)) break;
            if (!out.contains(c.id())) out.add(c.id());
        }
        return out;
    }
}
