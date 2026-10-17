package com.newuniverse.nusmp.arc.early;

import com.newuniverse.nusmp.entity.zogratis.MoveSpec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: every early boss of Saga 1 keeps its moves as a plain table (the numbers in one place, readable and testable without Minecraft), and the boss class
 * only adds the telegraph and the code that runs. The Zogratis bosses each carry a private copy of the same record; five more copies would be five places to
 * keep in step with {@code Move}, so the early bosses share this one. It is a {@link MoveSpec} (what the scheduler reads), it repeats the framework's mana
 * formula so a test can reason about eligibility, and {@link #problems} is the one list of house rules every table is held to (a telegraph that survives the
 * power tuning, an ultimate that is a ritual and leaves the boss exposed, no range where the boss has nothing to throw at 20% power).
 *
 * @param id          unique move id (also what the boss class switches on)
 * @param label       name shown on the action bar when an ultimate starts
 * @param minPower    least power (percent) at which the boss may use it
 * @param windUpTicks base telegraph in ticks (the framework shortens it with power, to 75% at the Gates)
 * @param cooldownTicks base cooldown (the framework shortens it with power)
 * @param minRange    nearest distance to the target at which the move is allowed
 * @param maxRange    farthest distance
 * @param weight      relative chance among the allowed moves
 * @param ultimate    at most one per power-tuned gap; exposes the boss afterwards
 * @param crowd       preferred against two or more foes
 * @param recovery    ticks after the move in which the boss starts nothing else (40 or more also exposes it)
 */
public record MoveRow(String id, String label, int minPower, int windUpTicks, int cooldownTicks, double minRange, double maxRange, int weight,
                      boolean ultimate, boolean crowd, int recovery) implements MoveSpec {

    /** The framework's shortest telegraph of an ordinary move and of an ultimate, and the factor the Gates apply to a wind-up. */
    public static final int MIN_TELL = 12, MIN_ULTIMATE_TELL = 30;
    public static final double GATES_WINDUP = 0.75;

    /** The same mana rule as {@code Move}: 6 plus a sixteenth of the cooldown, at most 32; 60 for an ultimate. */
    @Override public double manaCost() { return ultimate ? 60 : Math.min(32, 6 + cooldownTicks / 16.0); }

    /** An ordinary move with the usual recovery of 10 ticks. */
    public static MoveRow of(String id, String label, int minPower, int windUp, int cooldown, double minRange, double maxRange, int weight) {
        return new MoveRow(id, label, minPower, windUp, cooldown, minRange, maxRange, weight, false, false, 10);
    }

    /** The same, preferred against two or more foes. */
    public static MoveRow crowd(String id, String label, int minPower, int windUp, int cooldown, double minRange, double maxRange, int weight) {
        return new MoveRow(id, label, minPower, windUp, cooldown, minRange, maxRange, weight, false, true, 12);
    }

    /** An ultimate: the boss is exposed for 60 ticks afterwards. */
    public static MoveRow ultimate(String id, String label, int minPower, int windUp, int cooldown, double minRange, double maxRange, int weight) {
        return new MoveRow(id, label, minPower, windUp, cooldown, minRange, maxRange, weight, true, false, 60);
    }

    /** The row with this id, or null. */
    public static MoveRow find(List<MoveRow> rows, String id) {
        for (MoveRow r : rows) if (r.id().equals(id)) return r;
        return null;
    }

    /**
     * The house rules of an early boss's table, as readable sentences (empty = fine): 8 to 14 moves with unique ids; every telegraph still at least
     * {@value #MIN_TELL} ticks at the Gates (an ultimate {@value #MIN_ULTIMATE_TELL}); exactly one ultimate, usable from 60% power, that leaves the boss
     * exposed; cooldowns of three seconds or more; at power 20 there is an attack for every distance up to 22 blocks (a boss that has nothing to do at
     * a distance just stands there); a melee, a ranged and a crowd move exist.
     */
    public static List<String> problems(List<MoveRow> rows) {
        List<String> bad = new ArrayList<>();
        if (rows.size() < 8 || rows.size() > 14) bad.add("a table has 8 to 14 moves, not " + rows.size());
        Set<String> ids = new HashSet<>();
        int ultimates = 0;
        for (MoveRow r : rows) {
            if (r.id() == null || r.id().isBlank() || !ids.add(r.id())) bad.add("move id missing or repeated: " + r.id());
            if (r.label() == null || r.label().isBlank()) bad.add(r.id() + " has no label");
            if (r.minPower() < 20 || r.minPower() > 80) bad.add(r.id() + " needs power " + r.minPower() + ", which is outside 20 to 80");
            int floor = r.ultimate() ? MIN_ULTIMATE_TELL : MIN_TELL;
            if (Math.round(r.windUpTicks() * GATES_WINDUP) < floor) bad.add(r.id() + " has a wind-up of " + r.windUpTicks() + ", under " + floor + " ticks at the Gates");
            if (r.cooldownTicks() < 60) bad.add(r.id() + " has a cooldown under three seconds");
            if (r.maxRange() <= r.minRange()) bad.add(r.id() + " has an empty range");
            if (r.weight() < 1) bad.add(r.id() + " has no weight");
            if (r.recovery() < 8) bad.add(r.id() + " has no recovery");
            if (r.ultimate()) {
                ultimates++;
                if (r.minPower() < 60) bad.add(r.id() + " is an ultimate before 60% power");
                if (r.recovery() < 40) bad.add(r.id() + " is an ultimate that does not leave the boss exposed");
            }
        }
        if (ultimates != 1) bad.add("a table has exactly one ultimate, not " + ultimates);
        for (double d = 0; d <= 22; d += 0.5) {
            final double dist = d;
            boolean any = rows.stream().anyMatch(r -> !r.ultimate() && r.minPower() <= 20 && dist >= r.minRange() && dist <= r.maxRange() && r.maxRange() - r.minRange() < 40);
            if (!any) { bad.add("nothing to throw at " + d + " blocks at 20% power"); break; }
        }
        if (rows.stream().noneMatch(r -> r.maxRange() <= 10)) bad.add("no melee move");
        if (rows.stream().noneMatch(r -> r.minRange() >= 3 && r.maxRange() >= 22)) bad.add("no ranged move");
        if (rows.stream().noneMatch(MoveRow::crowd)) bad.add("no crowd move");
        return bad;
    }
}
