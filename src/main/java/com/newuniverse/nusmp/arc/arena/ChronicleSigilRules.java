package com.newuniverse.nusmp.arc.arena;

/**
 * Why: the Chronicle Sigil is a teleport that carries a party into a fight, so its rules are written down and tested like the Hourglass's, from which they are
 * copied in spirit: it works only at the Chronicle (in the Hub, near the lectern) to go, and only inside a story arena to come home; it will not be used to run
 * from another fight (a blow in the last five seconds), it carries nobody to a fight that is already under way (the arena is closed to latecomers), it needs a
 * boss to carry the party to, and leaving a fight in progress costs ten seconds against the three of a quiet one. Pure: the item reads the facts, this class
 * answers.
 */
public final class ChronicleSigilRules {
    private ChronicleSigilRules() {}

    public static final int ENTER_TICKS = 60, LEAVE_TICKS = 60, LEAVE_FIGHT_TICKS = 200, HURT_WINDOW = 100, COOLDOWN_TICKS = 600;

    /** Where the user stands: the Hub, a story arena, or anywhere else. */
    public enum Place { HUB, ARENA, ELSEWHERE }

    public enum Action { ENTER, LEAVE }

    public enum Denial { NONE, WRONG_PLACE, TOO_FAR, RECENTLY_HURT, NOTHING_TO_FIGHT, FIGHT_IN_PROGRESS, SWITCHED_OFF, RESTING }

    /** Either a denial, or an action with its channel in ticks. */
    public record Decision(Denial denial, Action action, int channelTicks) {
        public boolean allowed() { return denial == Denial.NONE; }
    }

    /**
     * @param enabled        the arenas are switched on ({@code arc.arenas})
     * @param place          where the user stands
     * @param nearChronicle  (Hub only) the user is within reach of the lectern
     * @param fightRunning   (arena: the one the user stands in; Hub: the one the chosen boss would be fought in) a fight is under way
     * @param ticksSinceHurt ticks since the user last took a blow from a creature (a large number for never)
     * @param hasPick        the Chronicle has a boss to carry the party to
     */
    public static Decision decide(boolean enabled, Place place, boolean nearChronicle, boolean fightRunning, long ticksSinceHurt, boolean hasPick) {
        return decide(enabled, place, nearChronicle, fightRunning, ticksSinceHurt, hasPick, false);
    }

    /**
     * The same, with {@code resting}: the chosen boss's arena is resting after a fight (the party would arrive to nothing for minutes), so the sigil refuses
     * to go and says so, instead of carrying people to an empty room.
     */
    public static Decision decide(boolean enabled, Place place, boolean nearChronicle, boolean fightRunning, long ticksSinceHurt, boolean hasPick, boolean resting) {
        if (!enabled) return new Decision(Denial.SWITCHED_OFF, null, 0);
        if (place == Place.ELSEWHERE) return new Decision(Denial.WRONG_PLACE, null, 0);
        if (place == Place.ARENA) return new Decision(Denial.NONE, Action.LEAVE, fightRunning ? LEAVE_FIGHT_TICKS : LEAVE_TICKS);
        if (!nearChronicle) return new Decision(Denial.TOO_FAR, null, 0);
        if (ticksSinceHurt < HURT_WINDOW) return new Decision(Denial.RECENTLY_HURT, null, 0);
        if (!hasPick) return new Decision(Denial.NOTHING_TO_FIGHT, null, 0);
        if (fightRunning) return new Decision(Denial.FIGHT_IN_PROGRESS, null, 0);
        if (resting) return new Decision(Denial.RESTING, null, 0);
        return new Decision(Denial.NONE, Action.ENTER, ENTER_TICKS);
    }
}
