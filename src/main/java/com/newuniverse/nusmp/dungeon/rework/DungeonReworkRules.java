package com.newuniverse.nusmp.dungeon.rework;

import java.util.List;

/**
 * Why: the dungeon-boss rework (docs/dungeon_bosses_rework.md) gives the Floor Warden a real move set and the Direwolf, the Basilisk and the Living Armor one or two signature moves each. Every
 * NUMBER of those moves, and every rule about when one may start, lives here as plain data and plain arithmetic (no Minecraft class), so it is unit tested without a world: the phase from the health
 * share (50 % and 25 %), the cooldown after the phase and the soft-timer enrage, the weighted choice of the next move from the distance, the cooldowns and a roll, and the worst-case base damage per
 * second of a book (the test that keeps the Warden under the boss damage budget of docs/boss_dps_budget.md). The Minecraft-facing halves are {@link WardenBrain} and {@link BeastBrain}; they only
 * ask this class.
 */
public final class DungeonReworkRules {
    private DungeonReworkRules() {}

    /**
     * One move: its id (also the lang key tail), the distance window it starts in, the wind-up (the telegraph) and the cooldown in ticks, the first phase it may be used in, its base damage per
     * hit before the balance law, how many times ONE victim can be hit by one cast, its weight in the draw, and whether it is the phase-3 ultimate (chosen first when ready, never drawn by weight).
     */
    public record Move(String id, double minRange, double maxRange, int windup, int cooldown, int minPhase, double damage, int hits, int weight, boolean ultimate) {
        public boolean fits(double dist, int phase) { return phase >= minPhase && dist >= minRange && dist <= maxRange; }
    }

    // ---------------------------------------------------------------- the Floor Warden
    public static final int W_SLAM = 0, W_LASH = 1, W_STEP = 2, W_PULSE = 3, W_TOLL = 4;

    /** The Warden's book, in the order of the {@code W_*} indexes (appended to, never reordered). */
    public static final List<Move> WARDEN = List.of(
            new Move("hammer_slam", 0.0, 5.5, 24, 90, 1, 9.0, 1, 5, false),
            new Move("chain_lash", 5.0, 22.0, 22, 180, 1, 5.0, 1, 3, false),
            new Move("shadow_step", 7.0, 26.0, 14, 200, 1, 6.0, 1, 3, false),
            new Move("seal_pulse", 0.0, 16.0, 30, 260, 2, 7.0, 3, 4, false),
            new Move("key_toll", 0.0, 40.0, 70, 900, 3, 22.0, 1, 0, true));

    /** Ticks of the pause after a move ends before the next may start: the boss never chains two telegraphs without a breath. */
    public static final int WARDEN_GAP = 20;
    /** The soft timer: after this many ticks of one fight (150 s) the Warden is enraged. */
    public static final int ENRAGE_TICKS = 3000;
    /** The two health shares that end phase 1 and phase 2. */
    public static final double PHASE2_AT = 0.5, PHASE3_AT = 0.25;
    /** The least a cooldown may be after every speed-up (1.5 s). */
    public static final int MIN_COOLDOWN = 30;
    /** The Key Toll: two safe sigils of this radius, this far from the Warden, opposite each other; inside one the toll is a graze of this share of its damage. */
    public static final double SIGIL_RADIUS = 3.0, SIGIL_DISTANCE = 7.0, SIGIL_GRAZE = 0.2;
    /** The most players a move looks at (bounds every query and loop). */
    public static final int MAX_VICTIMS = 8;

    /** 1, 2 or 3: the phase of a boss at this health share. */
    public static int phase(double healthShare) {
        if (healthShare <= PHASE3_AT) return 3;
        return healthShare <= PHASE2_AT ? 2 : 1;
    }

    /** The cooldown of a move after the phase (15 % shorter in phase 2, 30 % in phase 3) and the enrage (20 % shorter), never under {@link #MIN_COOLDOWN}. */
    public static int cooldown(int base, int phase, boolean enraged) {
        double f = phase >= 3 ? 0.7 : phase == 2 ? 0.85 : 1.0;
        if (enraged) f *= 0.8;
        return (int) Math.max(MIN_COOLDOWN, Math.round(base * f));
    }

    /** The damage factor of the enrage (the soft timer): 1.2 once it is on. */
    public static double damageFactor(boolean enraged) { return enraged ? 1.2 : 1.0; }

    public static boolean enraged(int fightTicks) { return fightTicks >= ENRAGE_TICKS; }

    /**
     * The index of the next move, or -1: the ultimate first when it is ready and in range, else a weighted draw among the ready moves whose window holds {@code dist}.
     * {@code readyAt[i]} is the game tick move i may start again; {@code roll} is any int (negative allowed).
     */
    public static int choose(List<Move> book, double dist, int phase, long now, long[] readyAt, int roll) {
        int total = 0;
        for (int i = 0; i < book.size(); i++) {
            Move m = book.get(i);
            if (!m.fits(dist, phase) || now < readyAt[i]) continue;
            if (m.ultimate()) return i;
            total += Math.max(0, m.weight());
        }
        if (total <= 0) return -1;
        int r = Math.floorMod(roll, total);
        for (int i = 0; i < book.size(); i++) {
            Move m = book.get(i);
            if (m.ultimate() || !m.fits(dist, phase) || now < readyAt[i]) continue;
            if (r < m.weight()) return i;
            r -= m.weight();
        }
        return -1;
    }

    /** The base damage per second ONE player would take if every move of the book were used as often as its cooldown allows (an upper bound: the moves share one body and one pause). */
    public static double worstCaseBasePerSecond(List<Move> book, int phase, boolean enraged) {
        double sum = 0;
        for (Move m : book) {
            if (m.minPhase() > phase) continue;
            double seconds = Math.max(1, cooldown(m.cooldown(), phase, enraged)) / 20.0;
            sum += m.damage() * m.hits() * damageFactor(enraged) / seconds;
        }
        return sum;
    }

    /** The centres {x1, z1, x2, z2} of the two safe sigils of the Key Toll for a Warden at (cx, cz): opposite each other, {@link #SIGIL_DISTANCE} out, the first at {@code angle} radians. */
    public static double[] sigils(double cx, double cz, double angle) {
        return new double[]{cx + Math.cos(angle) * SIGIL_DISTANCE, cz + Math.sin(angle) * SIGIL_DISTANCE, cx - Math.cos(angle) * SIGIL_DISTANCE, cz - Math.sin(angle) * SIGIL_DISTANCE};
    }

    /** Is (x, z) inside one of the two sigils (the array of {@link #sigils})? */
    public static boolean insideSigil(double[] sigils, double x, double z) {
        for (int i = 0; i + 1 < sigils.length; i += 2) {
            double dx = x - sigils[i], dz = z - sigils[i + 1];
            if (dx * dx + dz * dz <= SIGIL_RADIUS * SIGIL_RADIUS) return true;
        }
        return false;
    }

    /** The share of the toll's damage a player takes: a graze inside a sigil, all of it outside. */
    public static double tollShare(boolean inside) { return inside ? SIGIL_GRAZE : 1.0; }

    /** Is a player standing in the band of an expanding wave (the Seal Pulse), and not above it (a jump clears the wave)? */
    public static boolean inWave(double dist, double waveRadius, double playerY, double wardenY) {
        return Math.abs(dist - waveRadius) <= 1.6 && playerY <= wardenY + 0.9;
    }

    // ---------------------------------------------------------------- the beasts
    public static final int B_POUNCE = 0, B_HOWL = 1, B_GAZE = 2, B_SPIT = 3, B_SWEEP = 4, B_BASH = 5, B_RALLY = 6;

    /** The three creatures' moves, in the order of the {@code B_*} indexes; a creature uses only its own (see {@link #beastMoves}). */
    public static final List<Move> BEASTS = List.of(
            new Move("pounce", 4.0, 11.0, 14, 120, 1, 5.0, 1, 4, false),
            new Move("pack_howl", 0.0, 24.0, 24, 600, 1, 0.0, 0, 2, false),
            new Move("petrifying_gaze", 6.0, 16.0, 30, 260, 1, 3.0, 1, 3, false),
            new Move("venom_spit", 5.0, 18.0, 16, 140, 1, 3.0, 1, 4, false),
            new Move("tail_sweep", 0.0, 3.6, 12, 120, 1, 5.0, 1, 4, false),
            new Move("shield_bash", 0.0, 3.2, 12, 100, 1, 4.0, 1, 5, false),
            new Move("rally_rattle", 0.0, 24.0, 20, 700, 2, 0.0, 0, 2, false));

    /** The indexes of {@link #BEASTS} each creature kind uses: Direwolf, Basilisk, Living Armor. */
    public static int[] beastMoves(BeastKind kind) {
        return switch (kind) {
            case DIREWOLF -> new int[]{B_POUNCE, B_HOWL};
            case BASILISK -> new int[]{B_GAZE, B_SPIT, B_SWEEP};
            case ARMOR -> new int[]{B_BASH, B_RALLY};
        };
    }

    public enum BeastKind { DIREWOLF, BASILISK, ARMOR }
}
