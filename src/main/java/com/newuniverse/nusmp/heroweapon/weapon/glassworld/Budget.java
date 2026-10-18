package com.newuniverse.nusmp.heroweapon.weapon.glassworld;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the owner's balance budget for the hero weapons (docs/heroweapon/design.md section 5) is a handful of numbers per kind of technique: how hard one hit
 * may be, how much one foe may take from one cast, how long a technique rests, what it costs, how many foes it may touch and how long it may keep working. The
 * four weapons of this package (Fan, Scythe, Katana, Book) state every technique as a {@link Spec} row, build the skill from that row, and one test per weapon
 * asks THIS class whether the rows obey the budget, so a later edit that makes a technique too strong, too cheap, too wide or too long fails a test instead of
 * reaching the players. The numbers are RAW damage, before the 1.0 to 2.2 level factor and before the balance law (which scales them by existence points,
 * mastery and the caps on players). No game imports, so the tests run anywhere.
 */
public final class Budget {
    private Budget() {}

    /** The four kinds of technique the budget knows. A utility may carry a small damage rider (a cage that cuts, a dash that brushes foes aside). */
    public enum Kind {
        /** Raw damage up to 12 per hit and per foe per cast; rests at least 60 ticks; costs 1 to 3 percent of the magicules. */
        ORDINARY(12.0, 12.0, 60, 0.01, 0.03),
        /** Raw damage up to 20; rests at least 60 ticks; costs 1 to 3 percent. */
        HEAVY(20.0, 20.0, 60, 0.01, 0.03),
        /** The strongest technique of a form: raw damage up to 32; rests at least 600 ticks; costs 5 to 8 percent. */
        ULTIMATE(32.0, 32.0, 600, 0.05, 0.08),
        /** Movement, guard, support, control: rests at least 100 ticks; costs 1 to 3 percent; a rider of up to 12 raw damage. */
        UTILITY(12.0, 12.0, 100, 0.01, 0.03);

        final double maxHit, maxTotal, minCost, maxCost;
        final int minCooldown;

        Kind(double maxHit, double maxTotal, int minCooldown, double minCost, double maxCost) {
            this.maxHit = maxHit;
            this.maxTotal = maxTotal;
            this.minCooldown = minCooldown;
            this.minCost = minCost;
            this.maxCost = maxCost;
        }

        public double maxHit() { return maxHit; }

        public double maxTotal() { return maxTotal; }

        public int minCooldown() { return minCooldown; }
    }

    /** The most foes one technique may touch (design.md section 5). */
    public static final int MAX_TARGETS = 12;

    /** The longest a technique may keep working after it was cast: 20 seconds (design.md section 5). */
    public static final int MAX_DURATION_TICKS = 400;

    private static final double EPS = 1e-9;

    /**
     * One technique as the budget reads it.
     *
     * @param id            the global skill id, for example {@code fan.flower_wind}
     * @param kind          the kind of technique
     * @param cooldown      ticks it rests before the level scaling
     * @param cost          share of the maximum magicules (0.02 is two percent)
     * @param rawPerHit     raw damage of one hit (0 when it does none)
     * @param hitsPerTarget most hits one foe can take from one cast
     * @param maxTargets    most foes one cast can touch (0 for a technique that touches no foe)
     * @param durationTicks the longest the technique keeps working after the cast (0 for an instant one)
     */
    public record Spec(String id, Kind kind, int cooldown, double cost, double rawPerHit, int hitsPerTarget, int maxTargets, int durationTicks) {
        /** The most raw damage one foe can take from one cast. */
        public double total() { return rawPerHit * Math.max(0, hitsPerTarget); }
    }

    /** What is wrong with a row, one sentence each; empty when the row obeys the budget. */
    public static List<String> violations(Spec s) {
        List<String> bad = new ArrayList<>();
        if (s == null) {
            bad.add("no row");
            return bad;
        }
        String id = s.id() == null ? "?" : s.id();
        if (s.id() == null || s.id().isBlank()) bad.add("a row has no id");
        if (s.rawPerHit() < 0) bad.add(id + ": negative damage");
        if (s.rawPerHit() > s.kind().maxHit + EPS) bad.add(id + ": a hit of " + s.rawPerHit() + " is over the " + s.kind() + " limit of " + s.kind().maxHit);
        if (s.total() > s.kind().maxTotal + EPS) bad.add(id + ": " + s.total() + " raw damage on one foe is over the " + s.kind() + " limit of " + s.kind().maxTotal);
        if (s.cooldown() < s.kind().minCooldown) bad.add(id + ": rests " + s.cooldown() + " ticks, a " + s.kind() + " technique rests at least " + s.kind().minCooldown);
        if (s.cost() < s.kind().minCost - EPS || s.cost() > s.kind().maxCost + EPS) bad.add(id + ": costs " + s.cost() + ", a " + s.kind() + " technique costs " + s.kind().minCost + " to " + s.kind().maxCost);
        if (s.maxTargets() < 0 || s.maxTargets() > MAX_TARGETS) bad.add(id + ": touches " + s.maxTargets() + " foes, the most is " + MAX_TARGETS);
        if (s.durationTicks() < 0 || s.durationTicks() > MAX_DURATION_TICKS) bad.add(id + ": works for " + s.durationTicks() + " ticks, the most is " + MAX_DURATION_TICKS);
        if (s.kind() == Kind.ULTIMATE && s.total() <= 0) bad.add(id + ": an ultimate that does no damage");
        if (s.rawPerHit() > 0 && s.hitsPerTarget() < 1) bad.add(id + ": deals damage but says it never hits");
        return bad;
    }

    /** What is wrong with a whole table: every row's violations plus repeated ids. */
    public static List<String> violations(List<Spec> table) {
        List<String> bad = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Spec s : table) {
            bad.addAll(violations(s));
            if (s != null && s.id() != null && !seen.add(s.id())) bad.add(s.id() + ": listed twice");
        }
        return bad;
    }
}
