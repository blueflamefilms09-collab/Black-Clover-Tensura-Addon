package com.newuniverse.nusmp.arc.tier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the house rules of the boss framework (a telegraph that survives the power tuning, one ultimate that leaves the boss exposed, no spam, something to answer at
 * every distance) are held for the early bosses by {@code MoveRow.problems}; a light boss has fewer moves, so it gets the same rules scaled to 4 to 6 moves, plus the
 * bounds that keep a data row cheap (counts, sizes, damage, status lengths). One list of sentences (empty = fine) that a unit test and the in-game self-test both read.
 */
public final class TierRules {
    private TierRules() {}

    /** The framework shortens a wind-up to 75% at the Gates; a shorter telegraph than this is unfair there. */
    public static final int MIN_TELL = 12, MIN_ULTIMATE_TELL = 30;
    public static final double GATES_WINDUP = 0.75;

    /** Everything wrong with a row (empty = fine). */
    public static List<String> problems(TierRow row) {
        List<String> bad = new ArrayList<>();
        String who = row.id();
        var c = row.character();
        if (!c.isBoss()) bad.add(who + " is not a boss");
        if (c.floorMin() < row.letter().floorMin() || c.floorMax() > row.letter().floorMax() || c.floorMin() > c.floorMax())
            bad.add(who + " stands on floors " + c.floorMin() + ".." + c.floorMax() + ", outside the " + row.letter().label() + " band " + row.letter().floorMin() + ".." + row.letter().floorMax());
        if (c.opening().isBlank() || c.gates().isBlank() || c.death().isBlank()) bad.add(who + " is missing a line (opening, gates or death)");
        if (row.height() < 1.2f || row.height() > 6f) bad.add(who + " has a height of " + row.height());
        bad.addAll(moveProblems(who, row.moves()));
        return bad;
    }

    public static List<String> moveProblems(String who, List<TierMove> moves) {
        List<String> bad = new ArrayList<>();
        if (moves.size() < 4 || moves.size() > 6) bad.add(who + " has " + moves.size() + " moves, a light boss has 4 to 6");
        Set<String> ids = new HashSet<>();
        int ultimates = 0;
        for (TierMove m : moves) {
            String w = who + "/" + m.id();
            if (m.id() == null || !m.id().matches("[a-z][a-z0-9_]*") || !ids.add(m.id())) bad.add(w + ": the move id is missing, not snake_case or repeated");
            if (m.label() == null || m.label().isBlank()) bad.add(w + " has no label");
            if (m.minPower() < 20 || m.minPower() > 80) bad.add(w + " needs power " + m.minPower() + ", outside 20..80");
            int floor = m.ultimate() ? MIN_ULTIMATE_TELL : MIN_TELL;
            if (Math.round(m.windUp() * GATES_WINDUP) < floor) bad.add(w + " has a wind-up of " + m.windUp() + ", under " + floor + " ticks at the Gates");
            if (m.cooldown() < 60) bad.add(w + " has a cooldown under three seconds");
            if (m.maxRange() <= m.minRange()) bad.add(w + " has an empty range");
            if (m.weight() < 1) bad.add(w + " has no weight");
            if (m.damage() <= 0 || m.damage() > TierMove.MAX_DAMAGE) bad.add(w + " has a damage of " + m.damage() + " times melee");
            double maxSize = m.kind() == TierMove.Kind.VOLLEY ? TierMove.MAX_SPREAD : TierMove.MAX_SIZE;
            if (m.size() <= 0 || m.size() > maxSize) bad.add(w + " has a size of " + m.size());
            if (m.count() < 1 || m.count() > TierMove.MAX_COUNT) bad.add(w + " has a count of " + m.count());
            if (m.knock() < 0 || m.knock() > TierMove.MAX_KNOCK) bad.add(w + " has a knock of " + m.knock());
            if (m.effectTicks() < 0 || m.effectTicks() > TierMove.MAX_EFFECT_TICKS || (m.effect() == TierMove.Effect.NONE) != (m.effectTicks() == 0))
                bad.add(w + " has a status of " + m.effect() + " for " + m.effectTicks() + " ticks");
            if (m.kind() == TierMove.Kind.RUSH && m.size() > 20) bad.add(w + " rushes for more than 20 ticks");
            if (m.ultimate()) {
                ultimates++;
                if (m.minPower() < 60) bad.add(w + " is an ultimate before 60% power");
            }
        }
        if (ultimates != 1) bad.add(who + " has " + ultimates + " ultimates, a light boss has exactly one");
        for (double d = 0; d <= 20; d += 0.5) {
            final double dist = d;
            boolean any = moves.stream().anyMatch(m -> !m.ultimate() && m.minPower() <= 20 && dist >= m.minRange() && dist <= m.maxRange() && m.maxRange() - m.minRange() < 40);
            if (!any) { bad.add(who + " has nothing to throw at " + d + " blocks at 20% power"); break; }
        }
        if (moves.stream().noneMatch(m -> m.maxRange() <= 10)) bad.add(who + " has no melee move");
        if (moves.stream().noneMatch(m -> m.minRange() >= 3 && m.maxRange() >= 20)) bad.add(who + " has no ranged move");
        return bad;
    }
}
