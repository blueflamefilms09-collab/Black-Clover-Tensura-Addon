package com.newuniverse.nusmp.heroweapon.weapon.kit;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Why: section 5 of the design fixes what a hero weapon's technique may do (raw damage 3 to 12 for an ordinary one, 10 to 20 for a heavy one, at most 32 for an
 * ultimate; at most 12 targets; a cooldown of at least 60 ticks for damage, 100 for a utility and 600 for an ultimate; a cost of 1 to 3 percent of the
 * magicules, 5 to 8 percent for an ultimate; a heal of at most a quarter of the target's health), and the four weapons of this builder have about forty
 * techniques. Writing the numbers of every technique into one {@link Spec} (the weapon's {@code Numbers} class) and checking each Spec here means the budget
 * is a table a test reads, not a promise in a comment: the glue builds the live skill from the Spec (so the cooldown and the cost the dispatcher sees ARE the
 * table's) and a later edit that breaks the balance fails {@code NumbersTest}. A Spec states the damage the technique can deal to one target at its strongest
 * ({@code rawMax} per application, {@code hitsPerTarget} applications), because "up to 12 raw" has to hold for the sum a victim can actually take.
 */
public final class Budget {
    private Budget() {}

    /** How strong a technique is allowed to be. */
    public enum Kind { ORDINARY, HEAVY, ULTIMATE, UTILITY }

    public static final int MAX_TARGETS = 12;
    private static final Pattern ID = Pattern.compile("[a-z]+\\.[a-z][a-z0-9_]*");

    /**
     * One technique's numbers.
     *
     * @param id            global skill id {@code <kind>.<name>}
     * @param kind          the budget class
     * @param cooldown      nominal cooldown in ticks (before the bond level's scaling)
     * @param cost          share of the maximum magicules (0.02 is two percent)
     * @param rawBase       raw damage of one application at the weakest (0 for a technique that deals none)
     * @param rawMax        raw damage of one application at the strongest (a point pool, a mark, a passive all counted)
     * @param hitsPerTarget the most applications one foe can take from one cast
     * @param maxTargets    the most foes one cast can touch
     * @param heal          the share of the target's maximum health one cast can heal (0 for none)
     */
    public record Spec(String id, Kind kind, int cooldown, double cost, double rawBase, double rawMax, int hitsPerTarget, int maxTargets, double heal) {
        /** The most raw damage one foe can take from one cast. */
        public double totalPerTarget() { return rawMax * hitsPerTarget; }

        public boolean ultimate() { return kind == Kind.ULTIMATE; }

        /** The same technique with a heal. */
        public Spec withHeal(double share) { return new Spec(id, kind, cooldown, cost, rawBase, rawMax, hitsPerTarget, maxTargets, share); }
    }

    /** An ordinary damage technique. */
    public static Spec damage(String id, int cooldown, double cost, double rawBase, double rawMax, int hitsPerTarget, int maxTargets) {
        return new Spec(id, Kind.ORDINARY, cooldown, cost, rawBase, rawMax, hitsPerTarget, maxTargets, 0.0);
    }

    /** A heavy damage technique. */
    public static Spec heavy(String id, int cooldown, double cost, double rawBase, double rawMax, int hitsPerTarget, int maxTargets) {
        return new Spec(id, Kind.HEAVY, cooldown, cost, rawBase, rawMax, hitsPerTarget, maxTargets, 0.0);
    }

    /** The strongest technique of a form. */
    public static Spec ultimate(String id, int cooldown, double cost, double rawBase, double rawMax, int hitsPerTarget, int maxTargets) {
        return new Spec(id, Kind.ULTIMATE, cooldown, cost, rawBase, rawMax, hitsPerTarget, maxTargets, 0.0);
    }

    /** A technique that deals no damage (a buff, a heal, a summon, a mark). */
    public static Spec utility(String id, int cooldown, double cost, int maxTargets) {
        return new Spec(id, Kind.UTILITY, cooldown, cost, 0.0, 0.0, 0, maxTargets, 0.0);
    }

    /** A utility technique that also deals a little damage (a counter, a returned bolt). */
    public static Spec utilityHit(String id, int cooldown, double cost, double raw, int hitsPerTarget, int maxTargets) {
        return new Spec(id, Kind.UTILITY, cooldown, cost, raw, raw, hitsPerTarget, maxTargets, 0.0);
    }

    /** Everything wrong with the Spec against the design's budget; empty when it is within it. */
    public static List<String> violations(Spec s) {
        List<String> bad = new ArrayList<>();
        String who = s.id() + ": ";
        if (!ID.matcher(s.id()).matches()) bad.add(who + "the id must be <kind>.<snake_name>");
        if (s.maxTargets() < 1 || s.maxTargets() > MAX_TARGETS) bad.add(who + "maxTargets " + s.maxTargets() + " is outside 1.." + MAX_TARGETS);
        if (s.heal() < 0.0 || s.heal() > 0.25) bad.add(who + "heal " + s.heal() + " is above a quarter of the target's health");
        boolean deals = s.rawMax() > 0.0;
        if (deals && s.hitsPerTarget() < 1) bad.add(who + "a damaging technique needs at least one hit per target");
        if (deals && s.rawBase() > s.rawMax()) bad.add(who + "rawBase above rawMax");
        switch (s.kind()) {
            case ORDINARY -> {
                if (s.rawBase() < 3.0 || s.rawMax() > 12.0) bad.add(who + "an ordinary technique deals 3..12 raw per application, not " + s.rawBase() + ".." + s.rawMax());
                if (s.totalPerTarget() > 12.0) bad.add(who + "one foe takes " + s.totalPerTarget() + " raw, above the ordinary 12");
                if (s.cooldown() < 60) bad.add(who + "a damage technique waits at least 60 ticks, not " + s.cooldown());
                cost(s, 0.01, 0.03, bad);
            }
            case HEAVY -> {
                if (s.rawBase() < 10.0 || s.rawMax() > 20.0) bad.add(who + "a heavy technique deals 10..20 raw per application, not " + s.rawBase() + ".." + s.rawMax());
                if (s.totalPerTarget() > 20.0) bad.add(who + "one foe takes " + s.totalPerTarget() + " raw, above the heavy 20");
                if (s.cooldown() < 60) bad.add(who + "a damage technique waits at least 60 ticks, not " + s.cooldown());
                cost(s, 0.01, 0.03, bad);
            }
            case ULTIMATE -> {
                if (!deals) bad.add(who + "an ultimate here deals damage");
                if (s.rawMax() > 32.0 || s.totalPerTarget() > 32.0) bad.add(who + "an ultimate deals at most 32 raw to one foe, not " + s.totalPerTarget());
                if (s.cooldown() < 600) bad.add(who + "an ultimate waits at least 600 ticks, not " + s.cooldown());
                cost(s, 0.05, 0.08, bad);
            }
            case UTILITY -> {
                if (deals && (s.rawBase() < 3.0 || s.rawMax() > 12.0 || s.totalPerTarget() > 12.0)) bad.add(who + "a utility technique that hits deals 3..12 raw in all, not " + s.totalPerTarget());
                if (s.cooldown() < 100) bad.add(who + "a utility technique waits at least 100 ticks, not " + s.cooldown());
                cost(s, 0.01, 0.03, bad);
            }
        }
        return bad;
    }

    private static void cost(Spec s, double lo, double hi, List<String> bad) {
        if (s.cost() < lo - 1.0e-9 || s.cost() > hi + 1.0e-9) bad.add(s.id() + ": the cost " + s.cost() + " is outside " + lo + ".." + hi + " of the magicules");
    }

    /** A cooldown scaled by a factor and never below a floor (haste shortens a wait, it never removes the budget's minimum). */
    public static int scaled(int base, double factor, int floor) { return Math.max(floor, (int) Math.round(base * factor)); }

    /** The floor a technique's cooldown may be hasted down to: the budget's minimum for its kind. */
    public static int floor(Kind kind) {
        return switch (kind) {
            case ORDINARY, HEAVY -> 60;
            case UTILITY -> 100;
            case ULTIMATE -> 600;
        };
    }
}
