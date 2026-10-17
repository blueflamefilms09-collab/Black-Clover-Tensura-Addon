package com.newuniverse.nusmp.labyrinth.mega.gear;

import com.newuniverse.nusmp.dungeon.sentient.Dice;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Why: everything about WHAT standard gear drops, as plain immutable data read from {@code data/nusmp/mega/gear/tables.json}: how the grade mix shifts
 * with depth (a curve per grade), the caps on every chance, how likely each slot is, and, for each grade, the vanilla base items per slot, how much
 * stronger and tougher the piece is, which enchantments and Tensura engravings it may carry and how much magic resistance it has. The owner wants the
 * drops tunable without a mod update, so the numbers are data and a datapack may replace the file; the roller reads only this class, never a file or a
 * Minecraft type, so the whole drop logic runs in the unit tests with the real shipped file. Everything here is immutable once built by
 * {@link GearTablesParser}; a lookup of something absent answers with a harmless default (an empty list, a flat curve, a zero weight) instead of null,
 * so a half-valid datapack degrades to "fewer drops" and never to an exception in a death event. Since 1.55.0 the same file also says which loot tables are chests that
 * may hold a piece ({@link ChestRules}, the {@code chests} section), because the chest side reads the same curves, caps and items.
 */
public final class GearTables {
    /** Nothing offered: no grade can drop. What the game uses when the file is missing or unusable (it logs why once). */
    public static final GearTables EMPTY = new GearTables(Map.of(), Map.of(), Map.of(), 0.5, Map.of());

    /** A whole-number range, both ends included. A reversed pair is swapped. */
    public record IntRange(int min, int max) {
        public static final IntRange ZERO = new IntRange(0, 0);

        public IntRange {
            if (min > max) {
                int t = min;
                min = max;
                max = t;
            }
        }

        /** A uniform integer in [min, max]; one random draw unless the range is a single value. */
        public int pick(Dice d) { return min + d.nextInt(max - min + 1); }

        public boolean isZero() { return min == 0 && max == 0; }
    }

    /** One enchantment or engraving a piece may roll: its id and the level range it may take. */
    public record Pool(String id, int min, int max) {
        public Pool {
            min = Math.max(1, min);
            max = Math.max(min, max);
        }
    }

    /**
     * A piecewise-linear multiplier over the depth progress x in 0..1: it holds the first value before the first point and the last value after the
     * last. The points are sorted by x when built. An empty curve is flat 1 (the grade's chance does not change with depth).
     */
    public static final class Curve {
        public static final Curve FLAT = new Curve(new double[0], new double[0]);

        private final double[] xs, ys;

        /** The arrays must have the same length and xs must be ascending (the parser sorts them). */
        public Curve(double[] xs, double[] ys) {
            this.xs = xs.clone();
            this.ys = ys.clone();
        }

        public int size() { return xs.length; }

        public double x(int i) { return xs[i]; }

        public double y(int i) { return ys[i]; }

        public double at(double x) {
            int n = xs.length;
            if (n == 0) return 1.0;
            if (x <= xs[0]) return ys[0];
            for (int i = 1; i < n; i++) {
                if (x <= xs[i]) {
                    double span = xs[i] - xs[i - 1];
                    if (span <= 0.0) return ys[i];
                    double f = (x - xs[i - 1]) / span;
                    return ys[i - 1] + (ys[i] - ys[i - 1]) * f;
                }
            }
            return ys[n - 1];
        }
    }

    /** What one grade is made of; see {@code tables.json} for the meaning of every field. */
    public record GradeTable(Map<GearSlot, List<String>> items, IntRange power, IntRange durability,
                             IntRange enchantCount, int enchantLevelCap, Map<GearSlot, List<Pool>> enchantPool,
                             IntRange engraveCount, Map<GearSlot, List<Pool>> engravePool,
                             IntRange resistArmour, IntRange resistHand) {
        public GradeTable {
            items = copy(items);
            enchantPool = copy(enchantPool);
            engravePool = copy(engravePool);
        }

        private static <V> Map<GearSlot, V> copy(Map<GearSlot, V> in) {
            Map<GearSlot, V> m = new EnumMap<>(GearSlot.class);
            if (in != null) m.putAll(in);
            return java.util.Collections.unmodifiableMap(m);
        }

        /** The base item ids offered for a slot (empty when none; an id listed twice is twice as likely). */
        public List<String> items(GearSlot s) {
            List<String> l = items.get(s);
            return l == null ? List.of() : l;
        }

        public boolean offers(GearSlot s) { return !items(s).isEmpty(); }

        public boolean offersAnything() {
            for (GearSlot s : GearSlot.values()) if (offers(s)) return true;
            return false;
        }

        public List<Pool> enchantPool(GearSlot s) {
            List<Pool> l = enchantPool.get(s);
            return l == null ? List.of() : l;
        }

        public List<Pool> engravePool(GearSlot s) {
            List<Pool> l = engravePool.get(s);
            return l == null ? List.of() : l;
        }

        /** The magic resistance range of a piece of this slot: the armour range for armour, the hand range for weapons and shields. */
        public IntRange resist(GearSlot s) { return s.isArmour() ? resistArmour : resistHand; }
    }

    private final Map<GearGrade, GradeTable> grades;
    private final Map<GearGrade, Curve> curves;
    private final Map<GearGrade, Double> maxChance;
    private final double totalCap;
    private final Map<GearSlot, Double> slotWeights;
    private final ChestRules chests;

    public GearTables(Map<GearGrade, GradeTable> grades, Map<GearGrade, Curve> curves, Map<GearGrade, Double> maxChance, double totalCap,
                      Map<GearSlot, Double> slotWeights) {
        this(grades, curves, maxChance, totalCap, slotWeights, ChestRules.DEFAULT);
    }

    /** 1.55.0 (chests): the same tables plus the rules that say which loot tables are chests worth a piece (the {@code chests} section of the file). */
    public GearTables(Map<GearGrade, GradeTable> grades, Map<GearGrade, Curve> curves, Map<GearGrade, Double> maxChance, double totalCap,
                      Map<GearSlot, Double> slotWeights, ChestRules chests) {
        this.chests = chests == null ? ChestRules.DEFAULT : chests;
        this.grades = new EnumMap<>(GearGrade.class);
        this.grades.putAll(grades);
        this.curves = new EnumMap<>(GearGrade.class);
        this.curves.putAll(curves);
        this.maxChance = new EnumMap<>(GearGrade.class);
        this.maxChance.putAll(maxChance);
        this.totalCap = Math.max(0.0, Math.min(1.0, totalCap));
        this.slotWeights = new EnumMap<>(GearSlot.class);
        this.slotWeights.putAll(slotWeights);
    }

    /** The table of one grade, or null when the file gives none (that grade then never drops). */
    public GradeTable grade(GearGrade g) { return grades.get(g); }

    /** Can this grade drop at all: has it at least one base item for at least one slot? */
    public boolean offers(GearGrade g) {
        GradeTable t = grades.get(g);
        return t != null && t.offersAnything();
    }

    /** True when at least one grade can drop. */
    public boolean isUsable() {
        for (GearGrade g : GearGrade.values()) if (offers(g)) return true;
        return false;
    }

    public Curve curve(GearGrade g) {
        Curve c = curves.get(g);
        return c == null ? Curve.FLAT : c;
    }

    /** The most one grade's chance per kill can ever be (1 when the file sets none). */
    public double maxChance(GearGrade g) {
        Double d = maxChance.get(g);
        return d == null ? 1.0 : d;
    }

    /** The most the three chances together can ever be, after the depth shift and Looting. */
    public double totalCap() { return totalCap; }

    /** Which loot tables are chests that may hold a piece, which are treasure chests, which are never touched (never null; the defaults when the file says nothing). */
    public ChestRules chests() { return chests; }

    /** The relative weight of a slot (0 when the file lists none). */
    public double slotWeight(GearSlot s) {
        Double d = slotWeights.get(s);
        return d == null ? 0.0 : d;
    }
}
