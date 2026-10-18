package com.newuniverse.nusmp.labyrinth.mega.enc;

import java.util.Arrays;
import java.util.List;

/**
 * Why: the owner's rule is "the higher or lower you go in a dungeon, the stronger and smarter the mobs get". Two numbers decide that for every hostile mob that is born in the
 * Mega-Dungeon, and both come from ONE input: the DEPTH of the floor it is born on, which is the distance of the floor from the Hub ({@link com.newuniverse.nusmp.labyrinth.mega.FloorMap#threat},
 * the same for the Holy floors above and the Abyssal floors below, so +300 and -300 are equally deep). STRENGTH is a set of bonuses on the mob's own attributes that grow with the depth along
 * a saturating curve (fast at the start, slower later, never a runaway: each curve has a ceiling), so floor +2 is already a little harder than floor +1 and floor 1000 is a hard wall that is
 * still fair. SMARTS is the mob's MIND LEVEL, 0 to {@value #MAX_MIND}: a step function of the depth with six thresholds, each of which unlocks one more behaviour (see {@link Behaviour}), and the
 * deeper the mind the faster the mob thinks (its mind pass runs more often). This class holds only the arithmetic and the decisions, with no Minecraft type, so a JUnit test can pin every
 * rule: more depth is never weaker or dumber, plus and minus are the same, every curve is capped, the thresholds are honoured. The game side is {@code DepthMobs} and {@code DepthMind}.
 */
public final class DepthRules {
    private DepthRules() {}

    public static final int MAX_MIND = 6;
    /** The floors at which the mind level goes up by one, comma separated (the config key {@code mindFloors} overrides it). */
    public static final String DEFAULT_MIND_FLOORS = "6,16,40,100,250,500";

    // ---- the ceilings and half-way points of the strength curves: bonus(depth) = max * x / (x + half), x = depth - 1
    public static final double HEALTH_MAX = 4.0, HEALTH_HALF = 300.0;          // +400 percent health at the limit
    public static final double DAMAGE_MAX = 2.5, DAMAGE_HALF = 300.0;          // +250 percent damage
    public static final double SPEED_MAX = 0.22, SPEED_HALF = 400.0;           // +22 percent movement speed
    public static final double ARMOR_MAX = 12.0, ARMOR_HALF = 400.0;           // +12 armour points
    public static final double KNOCKBACK_MAX = 0.6, KNOCKBACK_HALF = 500.0;    // +0.6 knockback resistance
    public static final double FOLLOW_MAX = 24.0, FOLLOW_HALF = 150.0;         // +24 blocks of follow range

    /** The attribute bonuses of one depth: multipliers ({@code health}, {@code damage}, {@code speed}: 0.5 = +50 percent) and plain additions ({@code armor}, {@code knockback}, {@code follow}). */
    public record Strength(double health, double damage, double speed, double armor, double knockback, double follow) {
        public static final Strength NONE = new Strength(0, 0, 0, 0, 0, 0);

        /** Every bonus times {@code k} (the config's overall scale, or the share of a mob that already has its own scaling). */
        public Strength times(double k) {
            double f = Double.isNaN(k) ? 0.0 : Math.max(0.0, k);
            return new Strength(health * f, damage * f, speed * f, armor * f, knockback * f, follow * f);
        }
    }

    /** The depth of a signed floor: its distance from the Hub, at least 1 (0, "unknown", counts as 1 and is filtered out by the caller). */
    public static int depthOf(int floor) {
        long a = Math.abs((long) floor);
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, a));
    }

    private static double curve(int depth, double max, double half) {
        double x = Math.max(0, depth - 1);
        return max * x / (x + half);
    }

    /** The strength bonuses of a depth. Depth 1 gets none; every number only ever grows with the depth, and none passes its ceiling. */
    public static Strength strength(int depth) {
        if (depth <= 1) return Strength.NONE;
        return new Strength(curve(depth, HEALTH_MAX, HEALTH_HALF), curve(depth, DAMAGE_MAX, DAMAGE_HALF), curve(depth, SPEED_MAX, SPEED_HALF),
                curve(depth, ARMOR_MAX, ARMOR_HALF), curve(depth, KNOCKBACK_MAX, KNOCKBACK_HALF), curve(depth, FOLLOW_MAX, FOLLOW_HALF));
    }

    // ================================================================ the mind
    /** What a mob learns to do, in the order the depth teaches it. The ordinal + 1 is the mind level that unlocks it. */
    public enum Behaviour {
        /** Mind 1: it keeps hunting where it last saw its target instead of giving up the moment the sight line breaks. */
        MEMORY,
        /** Mind 2: when it is hurt or sees a player it calls the monsters near it, which then hunt the same player. */
        ALERT,
        /** Mind 3: with friends on the same target it does not queue in a line: it comes round the target from its own side. */
        FLANK,
        /** Mind 4: badly hurt, it backs off, recovers and comes back (once), instead of fighting to the death. */
        REGROUP,
        /** Mind 5: a target that keeps its distance (kites it with a bow, pillars, hit-and-step-back) is closed down with a burst of speed. */
        CLOSE_DOWN,
        /** Mind 6: it picks the weakest player in reach, the one with the least health and armour, not the nearest. */
        HUNTER;

        public int level() { return ordinal() + 1; }
    }

    /** Does a mob of this mind level know the behaviour? */
    public static boolean knows(int mind, Behaviour b) { return mind >= b.level(); }

    /** Parses the thresholds ("6,16,40,..."); a bad list, a list of the wrong length, or one that does not strictly rise falls back to the default. */
    public static int[] parseThresholds(String text) {
        int[] fallback = parseRaw(DEFAULT_MIND_FLOORS);
        int[] got = parseRaw(text);
        if (got == null || got.length != MAX_MIND) return fallback;
        for (int i = 0; i < got.length; i++) if (got[i] < 2 || (i > 0 && got[i] <= got[i - 1])) return fallback;
        return got;
    }

    private static int[] parseRaw(String text) {
        if (text == null) return null;
        try {
            return Arrays.stream(text.split(",")).map(String::trim).filter(s -> !s.isEmpty()).mapToInt(Integer::parseInt).toArray();
        } catch (NumberFormatException bad) {
            return null;
        }
    }

    /** The mind level of a depth: how many of the thresholds it has reached (0 to {@value #MAX_MIND}). */
    public static int mind(int depth, int[] thresholds) {
        int m = 0;
        for (int t : thresholds) if (depth >= t) m++;
        return Math.min(MAX_MIND, m);
    }

    public static int mind(int depth) { return mind(depth, parseRaw(DEFAULT_MIND_FLOORS)); }

    /** Ticks between two mind passes: 20 for a mind of 0, down to 4 for the deepest; a sharper mind reacts sooner. */
    public static int thinkEvery(int mind) { return Math.max(4, 20 - 3 * Math.max(0, mind)); }

    // ---- the decisions of the behaviours (each answers from plain numbers, so the tests need no world)
    /** How far an alarm carries: 12 blocks, 4 more for every mind level above the one that teaches it. */
    public static double alertRadius(int mind) { return Math.min(40.0, 12.0 + 4.0 * Math.max(0, mind - Behaviour.ALERT.level())); }

    /** The most a mob keeps hunting an unseen target: 5 seconds at mind 1, 2 more for each level, in ticks. */
    public static int memoryTicks(int mind) { return 100 + 40 * Math.max(0, mind - 1); }

    /** A regroup starts at 30 percent health or less and ends at 60 percent or more; it can happen once in a mob's life. */
    public static boolean startRegroup(double healthFraction, boolean used) { return !used && healthFraction <= 0.30; }

    public static boolean endRegroup(double healthFraction, int ticksIn) { return healthFraction >= 0.60 || ticksIn > 200; }

    /** Is the target kiting this mob: far, not hitting it, and not coming closer? */
    public static boolean isKited(double distance, int ticksSinceHurt, boolean targetClosing) { return distance > 7.0 && ticksSinceHurt > 60 && !targetClosing; }

    /** Radians of the place round the target this mob takes when {@code count} mobs hunt it together; {@code index} is the mob's rank among them. */
    public static double flankAngle(int index, int count) {
        int n = Math.max(1, count);
        return 2.0 * Math.PI * (Math.floorMod(index, n)) / n;
    }

    /** How easy a player is to kill: health plus armour counted at 1.5 health each. The Hunter goes for the lowest. */
    public static double softness(double health, double armor) { return Math.max(0.0, health) + 1.5 * Math.max(0.0, armor); }

    /** The index of the softest of these scores, or -1 for none. A tie keeps the first (the nearer, as the caller lists them by distance). */
    public static int softest(List<Double> scores) {
        int best = -1;
        for (int i = 0; i < scores.size(); i++) if (best < 0 || scores.get(i) < scores.get(best)) best = i;
        return best;
    }
}
