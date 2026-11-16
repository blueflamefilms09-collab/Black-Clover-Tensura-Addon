package com.newuniverse.nusmp.dungeon.rework;

import com.newuniverse.nusmp.dungeon.rework.DungeonReworkRules.Move;

import java.util.List;
import java.util.Locale;

/**
 * Why: dungeon-boss rework 2 (docs/dungeon_bosses_rework2.md) gives the Labyrinth Guardians (the nine sector bosses and, riding on the same entity, the 200 Decennial bosses) a telegraphed move set of
 * their own. Every NUMBER and every choice of it lives here as plain data and plain arithmetic (no Minecraft class), so it is unit tested without a world: the five moves (a Slam, a Lance, a Rain of
 * marks, a Wave in phase 2 and the Verdict, the ultimate of phase 3), what each does besides damage in each of the nine KITS (one per sector trait: gravity, blood, spatial, flame, frost, verdant,
 * storm, shadow, astral), which sound cue and which particle theme each kit uses, and the geometry (is a player on the Lance, inside a Rain mark, inside the Verdict's safe sigil or keep-out ring).
 * The Minecraft-facing half is {@link GuardianBrain}; it only asks this class. Damage is written as a SHARE of the Guardian's own attack damage (the scaled attribute), so a high threat sector hits
 * harder exactly as it did, and every hit then goes through the boss damage law and the player's hit cap (docs/boss_dps_budget.md, BalanceLaw).
 */
public final class GuardianRules {
    private GuardianRules() {}

    public static final int G_SLAM = 0, G_LANCE = 1, G_RAIN = 2, G_WAVE = 3, G_VERDICT = 4;

    /** The Guardian's book, in the order of the {@code G_*} indexes (appended to, never reordered). {@code damage} is a share of the attack damage, not a number of hearts. */
    public static final List<Move> BOOK = List.of(
            new Move("slam", 0.0, 7.0, 26, 100, 1, 0.9, 1, 5, false),
            new Move("lance", 4.0, 24.0, 24, 160, 1, 0.7, 1, 3, false),
            new Move("rain", 3.0, 28.0, 36, 220, 1, 0.8, 1, 3, false),
            new Move("wave", 0.0, 18.0, 30, 260, 2, 0.6, 3, 4, false),
            new Move("verdict", 0.0, 40.0, 80, 1000, 3, 2.2, 1, 0, true));

    /** Ticks of the pause after a move: no two telegraphs back to back; the Verdict leaves the boss winded for this much longer. */
    public static final int GAP = 20, VERDICT_WINDED = 60;
    /** Slam radius (blocks, before the size factor), Lance length and half width, Rain mark radius and how many marks, Wave band. */
    public static final double SLAM_RADIUS = 5.5, LANCE_LENGTH = 22.0, LANCE_HALF_WIDTH = 1.6, MARK_RADIUS = 2.4;
    public static final int MAX_MARKS = 3;
    /** The ticks of the Rain's windup in which the marks still follow their player; after that they are locked and a step out of one is enough. */
    public static final int MARK_FOLLOW_TICKS = 20;
    /** The Verdict: safe sigil radius and distance (as the Floor Warden's), the keep-out radius of the other kind, and the graze share inside a sigil. */
    public static final double SIGIL_RADIUS = 3.0, SIGIL_DISTANCE = 9.0, KEEP_OUT_RADIUS = 12.0, SIGIL_GRAZE = 0.2;

    /** What a move does to a player it hit, besides the damage. All are short, bounded and go through the existing laws (a slow through the control law, a push is one impulse). */
    public enum Flavour { NONE, KNOCK, PULL, LAUNCH, SLOW, BURN, WEAKEN, POISON, WITHER }

    /** The two Verdicts: stand inside a sigil (SAFE_ZONE) or get out of a ring around the boss (KEEP_OUT). */
    public enum Verdict { SAFE_ZONE, KEEP_OUT }

    /**
     * One kit: the trait id it belongs to, whether its telegraphs use the shadowed particle set, what its four moves add to the damage, its Verdict, the sound cue it winds up with, the cue of its impacts
     * and the cue of its accents (the Rain's landing, the Wave's rings), and its particle theme id.
     */
    public record Kit(String id, boolean dark, Flavour slam, Flavour lance, Flavour rain, Flavour wave, Verdict verdict, String beginCue, String hitCue, String accentCue) {
        public String theme() { return "dungeon.boss.guardian." + id; }

        public Flavour flavour(int move) {
            return switch (move) {
                case G_SLAM -> slam;
                case G_LANCE -> lance;
                case G_RAIN -> rain;
                case G_WAVE -> wave;
                default -> Flavour.NONE;
            };
        }
    }

    /** The nine kits, one per sector trait (the order of {@code GuardianScaling.Trait}); a trait this table does not know gets the gravity kit. */
    public static final List<Kit> KITS = List.of(
            new Kit("gravity", false, Flavour.PULL, Flavour.KNOCK, Flavour.SLOW, Flavour.KNOCK, Verdict.KEEP_OUT, "rise", "slam", "slam"),
            new Kit("blood", true, Flavour.WEAKEN, Flavour.WITHER, Flavour.WEAKEN, Flavour.POISON, Verdict.SAFE_ZONE, "growl", "slam", "hiss"),
            new Kit("spatial", false, Flavour.LAUNCH, Flavour.KNOCK, Flavour.LAUNCH, Flavour.SLOW, Verdict.SAFE_ZONE, "shimmer", "clang", "step"),
            new Kit("flame", false, Flavour.BURN, Flavour.BURN, Flavour.BURN, Flavour.KNOCK, Verdict.KEEP_OUT, "crackle", "blast", "crackle"),
            new Kit("frost", false, Flavour.SLOW, Flavour.SLOW, Flavour.SLOW, Flavour.SLOW, Verdict.SAFE_ZONE, "shimmer", "clang", "shimmer"),
            new Kit("verdant", false, Flavour.POISON, Flavour.POISON, Flavour.SLOW, Flavour.POISON, Verdict.SAFE_ZONE, "hiss", "slam", "spit"),
            new Kit("storm", false, Flavour.KNOCK, Flavour.KNOCK, Flavour.LAUNCH, Flavour.KNOCK, Verdict.SAFE_ZONE, "crackle", "blast", "crackle"),
            new Kit("shadow", true, Flavour.WEAKEN, Flavour.WITHER, Flavour.WEAKEN, Flavour.WITHER, Verdict.KEEP_OUT, "step", "slam", "chain"),
            new Kit("astral", false, Flavour.LAUNCH, Flavour.KNOCK, Flavour.KNOCK, Flavour.WEAKEN, Verdict.SAFE_ZONE, "shimmer", "toll", "shimmer"));

    /** The kit of a trait id ({@code GuardianScaling.Trait#name()} in any case); the gravity kit for anything unknown. */
    public static Kit kit(String traitId) {
        String id = traitId == null ? "" : traitId.toLowerCase(Locale.ROOT);
        for (Kit k : KITS) if (k.id().equals(id)) return k;
        return KITS.get(0);
    }

    /** The damage factor of a Decennial boss's tier (1 to 5) on the Verdict: the later decades hit their ultimate a little harder (never over 1.25). Tier 0 and 1 are 1.0. */
    public static double tierFactor(int tier) { return 1.0 + 0.0625 * Math.max(0, Math.min(5, tier) - 1); }

    /** The size factor of a Guardian from its SCALE attribute: 1.0 at the configured 1.8 and under, up to 1.6 for the biggest. Radii of the Slam and the Wave grow with it. */
    public static double sizeFactor(double scale) { return Math.max(1.0, Math.min(1.6, scale / 1.8)); }

    /** The base damage of one hit of a move: its share of the attack damage, the enrage-free factor of the tier on the ultimate. */
    public static double damage(int move, double attack, int tier) {
        double share = BOOK.get(move).damage();
        return Math.max(0.0, attack) * share * (move == G_VERDICT ? tierFactor(tier) : 1.0);
    }

    /**
     * The upper bound of the base damage per second ONE player takes in attack-damage units if every move of the book were used as often as its cooldown allows (the moves share one body and one pause, so
     * the real figure is lower). Cooldowns shorten like the Warden's: 15 % in phase 2, 30 % in phase 3.
     */
    public static double worstCaseSharePerSecond(int phase, int tier) {
        double sum = 0;
        for (int i = 0; i < BOOK.size(); i++) {
            Move m = BOOK.get(i);
            if (m.minPhase() > phase) continue;
            double seconds = Math.max(1, DungeonReworkRules.cooldown(m.cooldown(), phase, false)) / 20.0;
            sum += m.damage() * m.hits() * (i == G_VERDICT ? tierFactor(tier) : 1.0) / seconds;
        }
        return sum;
    }

    // ---------------------------------------------------------------- geometry
    /** Is (px, pz) on the Lance that starts at (ox, oz) and runs along the unit direction (dx, dz) for {@code length} blocks, {@code halfWidth} either side? */
    public static boolean onLine(double ox, double oz, double dx, double dz, double px, double pz, double length, double halfWidth) {
        double rx = px - ox, rz = pz - oz;
        double along = rx * dx + rz * dz;
        if (along < 0 || along > length) return false;
        double across = Math.abs(rx * -dz + rz * dx);
        return across <= halfWidth;
    }

    /** Is (px, pz) inside the circle of radius r around (cx, cz)? */
    public static boolean inCircle(double cx, double cz, double r, double px, double pz) {
        double dx = px - cx, dz = pz - cz;
        return dx * dx + dz * dz <= r * r;
    }

    /** The centres {x1, z1, x2, z2} of the two safe sigils of the Verdict: opposite each other, {@link #SIGIL_DISTANCE} out times the size factor, the first at {@code angle} radians. */
    public static double[] sigils(double cx, double cz, double angle, double size) {
        double d = SIGIL_DISTANCE * size;
        return new double[]{cx + Math.cos(angle) * d, cz + Math.sin(angle) * d, cx - Math.cos(angle) * d, cz - Math.sin(angle) * d};
    }

    public static boolean insideSigil(double[] sigils, double x, double z) {
        for (int i = 0; i + 1 < sigils.length; i += 2) if (inCircle(sigils[i], sigils[i + 1], SIGIL_RADIUS, x, z)) return true;
        return false;
    }

    /** The share of the Verdict's damage a player takes: SAFE_ZONE all of it outside a sigil and a graze inside; KEEP_OUT all of it inside the ring and none outside. */
    public static double verdictShare(Verdict v, boolean insideSigil, double distFromBoss, double size) {
        if (v == Verdict.SAFE_ZONE) return insideSigil ? SIGIL_GRAZE : 1.0;
        return distFromBoss <= KEEP_OUT_RADIUS * size ? 1.0 : 0.0;
    }

    /** The radius of the n-th ring of the Wave after {@code age} ticks of its life (two blocks a tick, times the size factor, at most 16 times it). */
    public static double waveRadius(int age, double size) { return Math.min(16.0 * size, (age + 1) * 2.0 * size); }
}
