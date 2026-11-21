package com.newuniverse.nusmp.arc.goku;

import com.newuniverse.nusmp.arc.world.FloorScale;
import com.newuniverse.nusmp.boss.BossStatMath;
import com.newuniverse.nusmp.boss.god.ArenaScaling;

import java.util.Collection;

/**
 * Why: every number of "Goku Omega" (docs/goku_omega.md) as pure arithmetic with no game type, so the tests can prove it: the strongest-boss guarantee, the level curve, the training and
 * fight experience with their daily caps, the Great Sage adaptation, the hit cap of the Omega mode and the unlock table of his hacks. The game side ({@link GokuOmega}, {@link GokuWorld}) only
 * reads and applies these answers. Everything is clamped and overflow safe: a broken setting, NaN or a huge number can never give a negative, infinite or absurd result.
 */
public final class GokuOmegaMath {
    private GokuOmegaMath() {}

    // ================================================================ the strongest-boss guarantee
    /** The Omega ceilings of the factors (the classic ones are {@link GokuBossMath#MAX_HEALTH_FACTOR} 60 and {@link GokuBossMath#MAX_DAMAGE_FACTOR} 24). */
    public static final double OMEGA_MAX_HEALTH_FACTOR = 400.0, OMEGA_MAX_DAMAGE_FACTOR = 96.0;
    /** He must beat the strongest rival's ceiling by at least this much, so a tie never counts. */
    public static final double MARGIN = 1.25;
    /** What one point of armour and one point of speed are worth in the comparison (ehp = health x (1 + armour x ARMOR_K), dps = attack x (1 + speed)). */
    public static final double ARMOR_K = 0.04;

    /** The stat line of a boss, as the roster and the tables state it. */
    public record Sheet(String id, double health, double attack, double speed, double armor) {}

    static double pos(double v) { return Double.isNaN(v) || Double.isInfinite(v) || v < 0.0 ? 0.0 : v; }

    /** The effective health of a stat line: health x (1 + armour x ARMOR_K). */
    public static double ehp(Sheet s) { return pos(s.health()) * (1.0 + pos(s.armor()) * ARMOR_K); }

    /** The damage rate of a stat line: attack x (1 + speed). */
    public static double dps(Sheet s) { return pos(s.attack()) * (1.0 + pos(s.speed())); }

    /** The most any single global system of the mod can multiply another boss's health: the Black Clover buff (10), the arena party table (8), the floor depth (2.5). Read from those classes, so retuning one retunes the proof. */
    public static double rivalHealthMul() { return Math.max(BossStatMath.MAX, Math.max(ArenaScaling.MAX_TOTAL_HEALTH, FloorScale.MAX_HEALTH_FACTOR)); }

    /** The same for damage: 10, 5, 1.6. */
    public static double rivalDamageMul() { return Math.max(BossStatMath.MAX, Math.max(ArenaScaling.MAX_TOTAL_DAMAGE, FloorScale.MAX_DAMAGE_FACTOR)); }

    /** The strongest effective health any rival can reach (its sheet at the largest multiplier). */
    public static double rivalEhpCeiling(Collection<Sheet> rivals) {
        double best = 0.0;
        for (Sheet r : rivals) best = Math.max(best, ehp(r) * rivalHealthMul());
        return best;
    }

    /** The strongest damage rate any rival can reach. */
    public static double rivalDpsCeiling(Collection<Sheet> rivals) {
        double best = 0.0;
        for (Sheet r : rivals) best = Math.max(best, dps(r) * rivalDamageMul());
        return best;
    }

    /** The lowest health factor that keeps his effective health at least {@link #MARGIN} x the strongest rival's ceiling. */
    public static double healthFloor(Sheet goku, Collection<Sheet> rivals) { return MARGIN * rivalEhpCeiling(rivals) / Math.max(1.0, ehp(goku)); }

    /** The lowest damage factor that keeps his damage rate at least {@link #MARGIN} x the strongest rival's ceiling. */
    public static double damageFloor(Sheet goku, Collection<Sheet> rivals) { return MARGIN * rivalDpsCeiling(rivals) / Math.max(1.0, dps(goku)); }

    /** His health factor in Omega mode: the classic one x the Omega bonus x the level, never below the floor, never above {@link #OMEGA_MAX_HEALTH_FACTOR}. */
    public static double omegaHealth(double classic, double bonus, int level, double floor) {
        double f = pos(classic) * Math.max(1.0, Math.min(10.0, Double.isNaN(bonus) ? 1.0 : bonus)) * levelHealthMul(level);
        return Math.max(GokuBossMath.MIN_FACTOR, Math.min(OMEGA_MAX_HEALTH_FACTOR, Math.max(f, pos(floor))));
    }

    /** His damage factor in Omega mode (same rule, {@link #OMEGA_MAX_DAMAGE_FACTOR}). */
    public static double omegaDamage(double classic, double bonus, int level, double floor) {
        double f = pos(classic) * Math.max(1.0, Math.min(10.0, Double.isNaN(bonus) ? 1.0 : bonus)) * levelDamageMul(level);
        return Math.max(GokuBossMath.MIN_FACTOR, Math.min(OMEGA_MAX_DAMAGE_FACTOR, Math.max(f, pos(floor))));
    }

    /** The power of a stat line at the given factors: effective health x damage rate. */
    public static double power(Sheet s, double healthFactor, double damageFactor) { return ehp(s) * healthFactor * dps(s) * damageFactor; }

    /** Is Goku, with these factors, above every rival at its own ceiling by the margin (health, damage and power all)? */
    public static boolean strongest(Sheet goku, double healthFactor, double damageFactor, Collection<Sheet> rivals) {
        if (ehp(goku) * healthFactor < MARGIN * rivalEhpCeiling(rivals) - 1e-9) return false;
        if (dps(goku) * damageFactor < MARGIN * rivalDpsCeiling(rivals) - 1e-9) return false;
        double top = 0.0;
        for (Sheet r : rivals) top = Math.max(top, power(r, rivalHealthMul(), rivalDamageMul()));
        return power(goku, healthFactor, damageFactor) >= MARGIN * top - 1e-6;
    }

    // ================================================================ levels
    public static final int START_LEVEL = 5, HARD_MAX_LEVEL = 200, DEFAULT_LEVEL_CAP = 50;

    /** The experience that takes level {@code level} to the next one: 150 x level^1.5, rounded; level 1 needs 150, level 10 about 4,700, level 50 about 53,000. */
    public static long xpToNext(int level) { return Math.round(150.0 * Math.pow(Math.max(1, Math.min(HARD_MAX_LEVEL, level)), 1.5)); }

    private static final long[] TOTAL = new long[HARD_MAX_LEVEL + 2];
    static {
        for (int l = 1; l <= HARD_MAX_LEVEL; l++) TOTAL[l + 1] = TOTAL[l] + xpToNext(l);
    }

    /** The total experience at which a level begins (level 1 = 0). */
    public static long totalXpFor(int level) { return TOTAL[Math.max(1, Math.min(HARD_MAX_LEVEL + 1, level))]; }

    /** The most experience that is ever stored (the start of the last level; more would change nothing). */
    public static long xpCeiling() { return TOTAL[HARD_MAX_LEVEL]; }

    /** The level of an amount of experience, at most {@code cap} (itself 1..{@link #HARD_MAX_LEVEL}). Monotone in xp. */
    public static int levelFor(long xp, int cap) {
        int c = Math.max(1, Math.min(HARD_MAX_LEVEL, cap));
        int l = 1;
        while (l < c && xp >= TOTAL[l + 1]) l++;
        return l;
    }

    /** What a level adds: +4% health, +3% damage and +10% to the EP he is read at, per level above 1. */
    public static double levelHealthMul(int level) { return 1.0 + 0.04 * (Math.max(1, Math.min(HARD_MAX_LEVEL, level)) - 1); }

    public static double levelDamageMul(int level) { return 1.0 + 0.03 * (Math.max(1, Math.min(HARD_MAX_LEVEL, level)) - 1); }

    public static double levelEpMul(int level) { return 1.0 + 0.10 * (Math.max(1, Math.min(HARD_MAX_LEVEL, level)) - 1); }

    // ================================================================ training and fight experience
    public static final long TICKS_PER_HOUR = 1000L, TICKS_PER_DAY = 24000L;
    /** A training sum looks back at most this many days (so a world that was off for a year does one bounded loop, not a long one). */
    public static final int MAX_CATCHUP_DAYS = 31;
    /** Fight experience: for each outcome, and the most one fight and one day can give. */
    public static final long WIN_BASE = 400, WIN_PER_KILL = 150, FAREWELL_BASE = 300, FAREWELL_PER_FIGHTER = 60, SURVIVED = 100, MAX_PER_FIGHT = 2500;
    public static final int MAX_KILLS = 5, MAX_FIGHTERS = 10;
    public static final long DEFAULT_FIGHT_DAILY_CAP = 6000;

    /** How a fight ended for him. */
    public enum Outcome { WIN, FAREWELL, SURVIVED }

    /** The experience of one fight: he beat players (WIN), the players beat him and he took his bow (FAREWELL), or nobody was left to fight him (SURVIVED). */
    public static long fightXp(Outcome o, int kills, int fighters) {
        long xp = switch (o) {
            case WIN -> WIN_BASE + WIN_PER_KILL * Math.max(0, Math.min(MAX_KILLS, kills));
            case FAREWELL -> FAREWELL_BASE + FAREWELL_PER_FIGHTER * Math.max(0, Math.min(MAX_FIGHTERS, fighters));
            case SURVIVED -> SURVIVED;
        };
        return Math.min(MAX_PER_FIGHT, xp);
    }

    /** The day a game time falls in. */
    public static long dayOf(long tick) { return Math.max(0L, tick) / TICKS_PER_DAY; }

    /** The result of a training sum: the experience, and the day and the amount of that day's cap it leaves used. */
    public record Trained(long xp, long day, long usedToday) {}

    /**
     * What he trained between two game times: {@code xpPerHour} for every in-game hour (1,000 ticks), at most {@code dailyCap} per in-game day, looking back at most
     * {@link #MAX_CATCHUP_DAYS} days. {@code day}/{@code used} say how much of that day's cap was already spent. A bounded loop (at most 32 steps), long arithmetic that cannot overflow
     * (a segment is at most 24,000 ticks times a rate of at most 1,000,000), pure.
     */
    public static Trained training(long lastTick, long now, long xpPerHour, long dailyCap, long day, long used) {
        long rate = Math.max(0L, Math.min(1_000_000L, xpPerHour)), cap = Math.max(0L, Math.min(10_000_000L, dailyCap));
        long to = Math.max(0L, now);
        long nowDay = dayOf(to);
        long spent = day == nowDay ? Math.max(0L, used) : 0L;
        if (rate == 0L || cap == 0L || to <= lastTick) return new Trained(0L, nowDay, spent);
        long from = Math.max(Math.max(0L, lastTick), to - MAX_CATCHUP_DAYS * TICKS_PER_DAY);
        long total = 0L;
        for (long d = dayOf(from); d <= nowDay; d++) {
            long a = Math.max(from, d * TICKS_PER_DAY), b = Math.min(to, (d + 1) * TICKS_PER_DAY);
            if (b <= a) continue;
            long gain = (b - a) * rate / TICKS_PER_HOUR;
            long room = cap - (d == day ? Math.max(0L, used) : 0L);
            gain = Math.max(0L, Math.min(gain, room));
            total += gain;
            if (d == nowDay) spent = (d == day ? Math.max(0L, used) : 0L) + gain;
        }
        return new Trained(total, nowDay, Math.min(cap, spent));
    }

    /** The experience he keeps of a fight's {@code xp} when {@code usedToday} of the daily fight cap is already spent. */
    public static long capFightXp(long xp, long usedToday, long dailyCap) { return Math.max(0L, Math.min(xp, Math.max(0L, dailyCap) - Math.max(0L, usedToday))); }

    /** The mutable record of his progress (the saved data holds one): experience, the clocks and the counters. Pure, so the tests can run a whole career. */
    public static final class Progress {
        public long xp = totalXpFor(START_LEVEL), lastTrain = -1L, day = -1L, trainToday, fightToday;
        public int wins, farewells, survived, kills, seenLevel = START_LEVEL;

        private void saturate() { xp = Math.max(0L, Math.min(xpCeiling(), xp)); }

        private void rollDay(long now) {
            long d = dayOf(now);
            if (d != day) {
                day = d;
                trainToday = 0L;
                fightToday = 0L;
            }
        }

        /** Credits the training since the last credit. Returns the experience gained. The first call only starts the clock. */
        public long train(long now, long xpPerHour, long dailyCap) {
            if (lastTrain < 0L || now < lastTrain) {
                lastTrain = Math.max(0L, now);
                rollDay(now);
                return 0L;
            }
            Trained t = training(lastTrain, now, xpPerHour, dailyCap, day, trainToday);
            lastTrain = now;
            day = t.day();
            trainToday = t.usedToday();
            long before = xp;
            xp += t.xp();
            saturate();
            return xp - before;
        }

        /** Moves the training clock without crediting anything (a fight ran, or training is off). */
        public void rest(long now) {
            lastTrain = Math.max(0L, now);
            rollDay(now);
        }

        /** Credits a fight. Returns the experience gained (after the daily fight cap). */
        public long fight(Outcome o, int killCount, int fighters, long now, long dailyCap) {
            rollDay(now);
            long gain = capFightXp(fightXp(o, killCount, fighters), fightToday, dailyCap);
            fightToday += gain;
            xp += gain;
            saturate();
            switch (o) {
                case WIN -> { wins++; kills += Math.max(0, Math.min(MAX_KILLS, killCount)); }
                case FAREWELL -> farewells++;
                case SURVIVED -> survived++;
            }
            lastTrain = Math.max(0L, now);
            return gain;
        }

        /** Sets the level directly (the operator's command): experience becomes the start of that level. */
        public void setLevel(int level) {
            xp = totalXpFor(Math.max(1, Math.min(HARD_MAX_LEVEL, level)));
            seenLevel = Math.max(1, Math.min(HARD_MAX_LEVEL, level));
        }

        public int level(int cap) { return levelFor(xp, cap); }
    }

    // ================================================================ hits
    public static final double HARD_HIT_SHARE = 0.95;

    /** The share of a player's maximum health one hit may take: the classic cap, or in Omega mode the higher Omega cap (never lower than the classic one, never above 95%). */
    public static double hitShare(boolean omega, double classic, double omegaCap) {
        double c = Double.isNaN(classic) ? 0.3 : Math.max(0.01, Math.min(1.0, classic));
        if (!omega) return c;
        double o = Double.isNaN(omegaCap) ? c : Math.max(0.01, Math.min(HARD_HIT_SHARE, omegaCap));
        return Math.max(c, o);
    }

    /** The fewest hits that can kill a player from full health at this share (ignoring everything but the cap). */
    public static int hitsToKill(double share) { return (int) Math.ceil(1.0 / Math.max(0.01, Math.min(1.0, share))); }

    /** The Omega dodge: the chance of a form, never above the Omega cap (itself at most 0.95), never negative. */
    public static double dodgeChance(double formChance, double cap) { return Math.min(Math.min(0.95, pos(cap)), pos(formChance)); }

    /** Mastered Ultra Instinct's own dodge row: the chance, the ticks before the next, the most per window and the window (ticks). */
    public static final double MASTERED_CHANCE = 0.92;
    public static final int MASTERED_COOLDOWN = 10, MASTERED_PER_WINDOW = 8, MASTERED_WINDOW = 120;

    // ================================================================ Great Sage: adaptation
    /** The damage kinds he learns to resist. */
    public static final int MELEE = 0, PROJECTILE = 1, MAGIC = 2, FIRE = 3, EXPLOSION = 4, LIGHTNING = 5, OTHER = 6, KINDS = 7;
    /** The first hits of a kind are free; every one after adds this much resistance. */
    public static final double FREE_HITS = 3.0, STEP = 0.06, MAX_HITS = 40.0, DECAY = 0.90;
    /** The resistance ceiling no level lifts: 0.75. */
    public static final double HARD_ADAPT_CAP = 0.75;

    /** The most resistance a level may reach: 30% plus 1% per level, at most {@link #HARD_ADAPT_CAP}. */
    public static double adaptCap(int level) { return Math.min(HARD_ADAPT_CAP, 0.30 + 0.01 * Math.max(1, Math.min(HARD_MAX_LEVEL, level))); }

    /** The resistance after {@code hits} hits of one kind: (hits - 3) x 6%, between 0 and {@code cap}. */
    public static double adaptResist(double hits, double cap) {
        return Math.max(0.0, Math.min(Math.min(HARD_ADAPT_CAP, pos(cap)), (pos(hits) - FREE_HITS) * STEP));
    }

    /** What he has learned in one fight: a hit count per kind. At most {@link #KINDS} numbers; reset when the fight ends. */
    public static final class Adapt {
        private final double[] hits = new double[KINDS];

        public void hit(int kind) {
            int k = Math.max(0, Math.min(KINDS - 1, kind));
            hits[k] = Math.min(MAX_HITS, hits[k] + 1.0);
        }

        /** Forgets a tenth of everything (called every ten seconds). */
        public void decay() { for (int i = 0; i < KINDS; i++) hits[i] *= DECAY; }

        public double hits(int kind) { return hits[Math.max(0, Math.min(KINDS - 1, kind))]; }

        public double resist(int kind, double cap) { return adaptResist(hits(kind), cap); }

        public void clear() { java.util.Arrays.fill(hits, 0.0); }
    }

    // ================================================================ the hacks
    /**
     * The hacks of Omega mode. {@code unlock}: the level that gives it. {@code cooldown}: ticks between two uses (0 = passive or per form). {@code fightCap}: uses per fight (0 = no cap needed).
     * {@code perPlayer}: uses on one player per fight (0 = not aimed at a player). The order is the order of the unlock levels. Append only.
     */
    public enum Hack {
        KI_SENSE(1, 40, 0, 0),
        COUNTER(1, 0, 40, 0),
        ENERGY_DRAIN(3, 300, 12, 5),
        MIRROR(4, 0, 11, 0),
        ADAPTATION(5, 0, 0, 0),
        SKILL_SEAL(6, 400, 8, 3),
        TIME_SKIP(8, 240, 12, 0),
        SECOND_WIND(10, 0, 5, 0),
        KI_BARRIER(12, 600, 6, 0);

        public final int unlock, cooldown, fightCap, perPlayer;

        Hack(int unlock, int cooldown, int fightCap, int perPlayer) {
            this.unlock = unlock;
            this.cooldown = cooldown;
            this.fightCap = fightCap;
            this.perPlayer = perPlayer;
        }

        public boolean unlocked(int level) { return level >= unlock; }
    }

    /** The hacks he has at a level. */
    public static int unlockedCount(int level) {
        int n = 0;
        for (Hack h : Hack.values()) if (h.unlocked(level)) n++;
        return n;
    }

    /** The share of a player's maximum magicule and aura one Energy Drain takes: 10% + 0.4% per level, at most 35%. */
    public static double drainFraction(int level) { return Math.min(0.35, 0.10 + 0.004 * Math.max(1, Math.min(HARD_MAX_LEVEL, level))); }

    /** How long one Skill Seal lasts (seconds, the unit of Tensura's skill cooldown): 12 + level / 2, at most 30. */
    public static int sealSeconds(int level) { return Math.min(30, 12 + Math.max(1, Math.min(HARD_MAX_LEVEL, level)) / 2); }

    /** The share of his maximum health one Second Wind gives, and the most it gives in a fight. */
    public static final double WIND_SHARE = 0.03, WIND_FIGHT_CAP = 0.15;
    /** A hit of this share of his maximum health is turned away by the Ki Barrier; this share of damage within {@link #BURST_TICKS} makes him skip through time. */
    public static final double BARRIER_SHARE = 0.05, BURST_SHARE = 0.06;
    public static final int BURST_TICKS = 40;
}
