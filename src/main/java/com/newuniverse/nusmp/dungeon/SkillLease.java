package com.newuniverse.nusmp.dungeon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The bookkeeping of a skill lent by a meal ({@link MealSkills}): what a new grant does, and which lent skills have run out. Pure. A
 * lease is only ever written for a skill the player did not own, so ending it can never take away a skill that was theirs.
 */
public final class SkillLease {
    private SkillLease() {}

    public enum Action { NONE, LEARN, EXTEND }

    /** What to do, and the end of the lease to store (meaningful unless the action is NONE). */
    public record Plan(Action action, long until) {}

    /**
     * A grant of {@code ticks} at game time {@code now}. {@code have}: the skill is on the player now. {@code leased}: a lease from an
     * earlier meal exists and ends at {@code leaseUntil}. A skill the player owns for good is left alone; a lease is only extended,
     * never shortened.
     */
    public static Plan grant(boolean have, boolean leased, long leaseUntil, long now, int ticks) {
        if (ticks <= 0) return new Plan(Action.NONE, 0L);
        long end = now + ticks;
        if (!leased) return have ? new Plan(Action.NONE, 0L) : new Plan(Action.LEARN, end);
        return new Plan(have ? Action.EXTEND : Action.LEARN, Math.max(leaseUntil, end));
    }

    /** The skills whose lease has run out by {@code now}, in the map's order. */
    public static List<String> expired(Map<String, Long> until, long now) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Long> e : until.entrySet()) if (e.getValue() <= now) out.add(e.getKey());
        return out;
    }
}
