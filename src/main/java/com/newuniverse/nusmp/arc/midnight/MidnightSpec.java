package com.newuniverse.nusmp.arc.midnight;

import com.newuniverse.nusmp.entity.zogratis.MoveSpec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the four bosses of the Eye of the Midnight Sun (Fana, Patolli, Rades Spirito, Rhya) each keep their move numbers in a pure table, and every one of
 * those tables must obey the same fairness laws the Zogratis framework and the owner's brief set: a telegraph of at least twelve ticks even at the Gates
 * (the framework shortens a wind-up to 75% there, so the table writes sixteen), an ultimate of at least thirty ticks (so forty on paper), a breath after
 * every move, and an ultimate that leaves the boss exposed. One record for the row and one checker for the laws means the four tables cannot drift apart,
 * and a mistake in a number is caught by a unit test instead of by a player. The row is a {@link MoveSpec}, so the framework's scheduler reads it directly.
 *
 * @param id          snake_case move id, unique within a boss (the scheduler keeps cooldowns by id)
 * @param label       the name the party reads on the action bar and in the chat
 * @param minPower    least power step (20, 40, 60, 80 or 100) at which the move is allowed
 * @param windUpTicks the telegraph before the move lands
 * @param recovery    ticks the boss does nothing else afterwards (40 or more also exposes it, see {@code Move.EXPOSED_FROM})
 */
public record MidnightSpec(String id, String label, int minPower, int windUpTicks, int cooldownTicks, double minRange, double maxRange,
                           int weight, boolean ultimate, boolean crowd, int recovery) implements MoveSpec {

    /** The shortest telegraph of an ordinary move on paper: 75% of it (the Gates) is still twelve ticks. */
    public static final int MIN_WINDUP = 16;
    /** The shortest telegraph of an ultimate on paper: 75% of it is still thirty ticks. */
    public static final int MIN_ULTIMATE_WINDUP = 40;
    /** The power steps of the framework's curve. */
    public static final Set<Integer> STEPS = Set.of(20, 40, 60, 80, 100);

    /** The mana an ordinary move costs below 60% power (the framework's own rule): 6 plus a sixteenth of its cooldown, at most 32; 60 for an ultimate. */
    @Override public double manaCost() { return ultimate ? 60 : Math.min(32, 6 + cooldownTicks / 16.0); }

    /**
     * The laws every table obeys, as a list of readable complaints (empty when the table is lawful). Used by the unit tests of the four tables and by
     * nothing at run time, so a bad number can never take a fight down.
     */
    public static List<String> problems(List<MidnightSpec> table) {
        List<String> out = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        int ultimates = 0;
        for (MidnightSpec s : table) {
            String who = s.id();
            if (s.id() == null || !s.id().matches("[a-z][a-z0-9_]*")) out.add("bad id " + who);
            if (!ids.add(s.id())) out.add("repeated id " + who);
            if (s.label() == null || s.label().isBlank()) out.add(who + " has no label");
            if (!STEPS.contains(s.minPower())) out.add(who + " is gated on a power that is not a step: " + s.minPower());
            if (s.ultimate()) {
                ultimates++;
                if (s.windUpTicks() < MIN_ULTIMATE_WINDUP) out.add(who + " is an ultimate with a telegraph under " + MIN_ULTIMATE_WINDUP);
                if (s.recovery() < 40) out.add(who + " is an ultimate that leaves its caster unexposed");
                if (s.cooldownTicks() < 900) out.add(who + " is an ultimate with a short cooldown");
            } else {
                if (s.windUpTicks() < MIN_WINDUP) out.add(who + " has a telegraph under " + MIN_WINDUP);
                if (s.recovery() < 8) out.add(who + " has no beat to breathe");
                if (s.cooldownTicks() < 60) out.add(who + " has a cooldown under three seconds");
            }
            if (s.weight() < 1) out.add(who + " has no weight");
            if (s.minRange() < 0 || s.maxRange() <= s.minRange()) out.add(who + " has an empty range band");
        }
        if (ultimates < 1) out.add("no ultimate");
        return out;
    }

    /** Is there some ordinary move at this power that reaches a target this far away (a boss with a dead range just walks up and waits)? */
    public static boolean reaches(List<MidnightSpec> table, int power, double distance) {
        for (MidnightSpec s : table) {
            if (!s.ultimate() && s.minPower() <= power && distance >= s.minRange() && distance <= s.maxRange()) return true;
        }
        return false;
    }
}
