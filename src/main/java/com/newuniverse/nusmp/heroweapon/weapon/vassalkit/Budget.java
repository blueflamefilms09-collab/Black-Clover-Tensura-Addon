package com.newuniverse.nusmp.heroweapon.weapon.vassalkit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Why: the owner's balance budget for the hero weapons (design.md section 5) is a handful of numbers per technique (raw damage per hit, damage on one target, targets,
 * cooldown, cost, heal), and a later edit to one skill must not be able to break it without a test failing. Each of the five weapons built with this kit
 * (Mirror, Musical Instrument, Harpoon, Sewing Set, Chain) describes every skill by one {@link Row} in its numbers class; the weapon's logic builds its skills FROM
 * those rows (cooldown, cost and the damage constants it casts with), so the table and the game cannot differ, and the weapon's test hands the rows to
 * {@link #violations}. Pure Java (no game classes) so the test runs anywhere.
 */
public final class Budget {
    private Budget() {}

    /** The class of a technique: it decides which limits of the budget apply. */
    public enum Kind {
        /** A damage technique of the ordinary size: raw 3..12 per hit, at most 12 on one target in all. */
        ORDINARY,
        /** A heavy damage technique: raw 10..20 per hit, at most 20 on one target in all. */
        HEAVY,
        /** Support, control or movement: little or no damage (raw 3..12 when it has any), a cooldown of at least 100 ticks. */
        UTILITY,
        /** The strongest technique of a form: at most 32 per hit and in all, a cooldown of at least 600 ticks, a cost of 5 to 8 percent. */
        ULTIMATE
    }

    /** At most this many foes (or allies) one technique may touch. */
    public static final int MAX_TARGETS = 12;
    /** The largest share of a target's maximum health one technique may heal. */
    public static final double MAX_HEAL_SHARE = 0.25;
    /**
     * Ticks during which a creature that was just hurt ignores a second blow that is not larger (the game's hurt-resistance window). Two separate hits of one
     * technique on one target (pulses, an echo) must be MORE than this many ticks apart or the second does nothing.
     */
    public static final int HURT_WINDOW = 10;

    /**
     * One technique's numbers. {@code raw} is the largest single hit before the level factor; {@code total} the most damage one target can take from the
     * technique in all (several hits, pulses, an echo); {@code targets} the most targets it touches; {@code cooldown} in ticks before level scaling; {@code cost}
     * a share of the maximum magicules (0.02 is two percent); {@code heal} the largest share of a target's maximum health it heals (0 for none).
     */
    public record Row(String id, Kind kind, double raw, double total, int targets, int cooldown, double cost, double heal) {
        /** A technique that deals damage. */
        public static Row damage(String id, Kind kind, double raw, double total, int targets, int cooldown, double cost) {
            return new Row(id, kind, raw, total, targets, cooldown, cost, 0.0);
        }

        /** A technique that deals no damage at all. */
        public static Row utility(String id, int cooldown, double cost) {
            return new Row(id, Kind.UTILITY, 0.0, 0.0, 0, cooldown, cost, 0.0);
        }

        /** The same technique, also healing up to {@code share} of each target's maximum health. */
        public Row withHeal(double share) {
            return new Row(id, kind, raw, total, targets, cooldown, cost, share);
        }

        /** The same technique touching up to {@code n} targets without damage (support and control reach). */
        public Row reaching(int n) {
            return new Row(id, kind, raw, total, n, cooldown, cost, heal);
        }
    }

    /** Every way one row breaks the budget, as short sentences; empty when the row is fine. */
    public static List<String> violations(Row r) {
        List<String> bad = new ArrayList<>();
        String who = r.id() == null ? "(no id)" : r.id();
        if (r.id() == null || !r.id().matches("[a-z_]+\\.[a-z0-9_]+")) bad.add(who + ": the id must look like <weapon>.<snake_name>");
        double rawMax, totalMax;
        int minCooldown;
        double minCost, maxCost;
        switch (r.kind()) {
            case ORDINARY -> { rawMax = 12; totalMax = 12; minCooldown = 60; minCost = 0.01; maxCost = 0.03; }
            case HEAVY -> { rawMax = 20; totalMax = 20; minCooldown = 60; minCost = 0.01; maxCost = 0.03; }
            case UTILITY -> { rawMax = 12; totalMax = 12; minCooldown = 100; minCost = 0.01; maxCost = 0.03; }
            default -> { rawMax = 32; totalMax = 32; minCooldown = 600; minCost = 0.05; maxCost = 0.08; }
        }
        if (r.raw() < 0) bad.add(who + ": negative raw damage");
        if (r.raw() > rawMax + 1e-9) bad.add(who + ": raw " + r.raw() + " is above the " + r.kind().name().toLowerCase(Locale.ROOT) + " limit " + rawMax);
        if (r.kind() == Kind.HEAVY && r.raw() < 10 - 1e-9) bad.add(who + ": a heavy technique hits for 10 to 20, not " + r.raw());
        if (r.raw() > 0 && r.raw() < 3 - 1e-9) bad.add(who + ": raw " + r.raw() + " is below the ordinary floor of 3");
        if (r.total() > totalMax + 1e-9) bad.add(who + ": " + r.total() + " on one target in all is above the " + totalMax + " limit");
        if (r.total() + 1e-9 < r.raw()) bad.add(who + ": the total on one target is below one hit");
        if (r.raw() > 0 && r.targets() < 1) bad.add(who + ": it deals damage but lists no target");
        if (r.targets() > MAX_TARGETS) bad.add(who + ": " + r.targets() + " targets is above " + MAX_TARGETS);
        if (r.cooldown() < minCooldown) bad.add(who + ": cooldown " + r.cooldown() + " is below " + minCooldown + " ticks");
        if (r.cost() < minCost - 1e-9 || r.cost() > maxCost + 1e-9) bad.add(who + ": cost " + r.cost() + " is outside " + minCost + ".." + maxCost);
        if (r.heal() < 0 || r.heal() > MAX_HEAL_SHARE + 1e-9) bad.add(who + ": heals " + r.heal() + " of a target's health, the limit is " + MAX_HEAL_SHARE);
        return bad;
    }

    /** Every violation of every row, plus repeated ids; empty when the whole table is fine. */
    public static List<String> violations(List<Row> rows) {
        List<String> bad = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Row r : rows) {
            bad.addAll(violations(r));
            if (r.id() != null && !seen.add(r.id())) bad.add(r.id() + ": listed twice");
        }
        return bad;
    }
}
