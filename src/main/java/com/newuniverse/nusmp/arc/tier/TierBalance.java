package com.newuniverse.nusmp.arc.tier;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Why: "how are the bosses balanced" must be a number the owner can read and a test can hold, not a feeling (docs/bosses/tier_powers.md, section 3). This pure class turns a roster row into its
 * <b>pressure</b>: the damage per second it is expected to deal, found by running the real scheduler rules (one move at a time, a weighted pick, cooldowns, the ultimate when it is ready and a
 * long exposed recovery after it) for three minutes against a target that is hit half the time (60% for the ultimate), then adding what its traits add. The rules it enforces:
 * <ul>
 *   <li>inside a tier every boss lies within &plusmn;{@value #BAND_PERCENT}% of the tier's mean pressure at {@value #REF_POWER}% power;</li>
 *   <li>an ordinary move deals at most {@value #MAX_ORDINARY}x the boss's attack and an ultimate at most {@value #MAX_ULTIMATE}x;</li>
 *   <li>the traits cost at most the tier's {@link TierLetter#traitBudget()} points, no trait twice, at most two per boss, a trait that hangs on a move names a move of the row.</li>
 * </ul>
 * The numbers are an estimate to compare bosses with each other and to catch a spike, not a prediction of a real fight; the real knobs are the roster rows, the trait constants in
 * {@link TierTrait} and the two server settings. The scheduler model is deterministic (no random) so the Python twin {@code tools/tier_bosses/tier_pressure.py} gives the same numbers.
 */
public final class TierBalance {
    private TierBalance() {}

    public static final int REF_POWER = 60, BAND_PERCENT = 25, SECONDS = 180, MAX_TRAITS = 2;
    public static final double BAND = BAND_PERCENT / 100.0, MAX_ORDINARY = 1.6, MAX_ULTIMATE = 2.0, HIT_RATE = 0.5, ULTIMATE_HIT_RATE = 0.6;
    /** Ticks a move keeps the boss busy after its wind-up, and the extra it is exposed after an ultimate. */
    public static final int RECOVERY_TICKS = 20, ULTIMATE_EXPOSED_TICKS = 60;
    /** What the damage-dealing traits add (an estimate): damage per second for steam and poison, the share of the time a mark / frenzy is up. */
    public static final double STEAM_DPS = 0.4, POISON_DPS = 0.3, KARMA_UPTIME = 0.4, FRENZY_UPTIME = 0.3;

    /** The health fraction a power step stands for (the middle of the power curve's band: 20 above 85%, 40 to 65%, 60 to 45%, 80 to 25%, 100 below). */
    public static double healthAt(int power) {
        if (power >= 100) return 0.15;
        if (power >= 80) return 0.35;
        if (power >= 60) return 0.55;
        if (power >= 40) return 0.75;
        return 0.92;
    }

    /** Targets hit by one use of a move (every bolt, circle or wisp counts once). */
    public static int hits(TierMove m) {
        return switch (m.kind()) {
            case VOLLEY, MARKED, SWARM -> Math.max(1, m.count());
            default -> 1;
        };
    }

    /** Expected damage per second of the row at this power (see the class comment). */
    public static double pressure(TierRow row, int power) {
        double attack = row.character().attack();
        List<TierMove> moves = row.moves();
        int n = moves.size();
        long horizon = SECONDS * 20L;
        long[] cooldownUntil = new long[n];
        long[] lastUse = new long[n];
        java.util.Arrays.fill(lastUse, -400);
        long t = 0;
        double total = 0;
        String echoed = row.moveOf(TierTrait.ECHO);
        while (t < horizon) {
            int pick = -1;
            double best = -1;
            for (int i = 0; i < n; i++) {
                TierMove m = moves.get(i);
                if (m.minPower() > power || cooldownUntil[i] > t) continue;
                if (m.ultimate()) { pick = i; break; }                       // the ultimate goes as soon as it is ready
                double score = m.weight() * (1.0 + (t - lastUse[i]) / 20.0);
                if (score > best) { best = score; pick = i; }
            }
            if (pick < 0) { t += 20; continue; }
            TierMove m = moves.get(pick);
            double use = m.damage() * attack * hits(m) * (m.ultimate() ? ULTIMATE_HIT_RATE : HIT_RATE);
            if (m.id().equals(echoed)) use *= 2.0;                                // the Echo repeats the move once, with the same telegraph
            total += use;
            lastUse[pick] = t;
            cooldownUntil[pick] = t + m.windUp() + m.cooldown();
            t += m.windUp() + RECOVERY_TICKS + (m.ultimate() ? ULTIMATE_EXPOSED_TICKS : 0);
        }
        double base = total / SECONDS;
        return base * traitMultiplier(row, power) + traitDps(row);
    }

    /** What the row's traits multiply the damage by at this power (climb, fusion, frenzy, karma), at the setting's default strength. */
    public static double traitMultiplier(TierRow row, int power) {
        double m = TierTrait.selfDamage(row.traits(), power, healthAt(power), 1.0, false);
        if (row.has(TierTrait.SOUL_MARK)) m *= 1.0 + (TierTrait.FRENZY_DAMAGE - 1.0) * FRENZY_UPTIME;
        if (row.has(TierTrait.KARMA)) m *= 1.0 + (TierTrait.KARMA_DAMAGE - 1.0) * KARMA_UPTIME;
        return m;
    }

    /** Damage per second the row's traits deal by themselves (steam, poison). */
    public static double traitDps(TierRow row) {
        double d = 0;
        if (row.has(TierTrait.STEAM)) d += STEAM_DPS;
        if (row.has(TierTrait.SPORE_POISON)) d += POISON_DPS;
        return d;
    }

    /** Mean pressure of the rows of one tier at this power (0 if there are none). */
    public static double mean(List<TierRow> rows, TierLetter letter, int power) {
        double s = 0;
        int n = 0;
        for (TierRow r : rows) if (r.letter() == letter) { s += pressure(r, power); n++; }
        return n == 0 ? 0 : s / n;
    }

    // ---------------------------------------------------------------- the rules

    /** Everything wrong with one row's traits and hit sizes (empty = fine). */
    public static List<String> problems(TierRow row) {
        List<String> bad = new ArrayList<>();
        String who = row.id();
        Set<TierTrait> seen = new HashSet<>();
        Set<String> moveIds = new HashSet<>();
        for (TierMove m : row.moves()) moveIds.add(m.id());
        if (row.traits().size() > MAX_TRAITS) bad.add(who + " has " + row.traits().size() + " traits, at most " + MAX_TRAITS);
        for (TierTraitSpec s : row.traits()) {
            TierTrait t = s.trait();
            if (!seen.add(t)) bad.add(who + " has the trait " + t + " twice");
            if (t.needsMove() && (s.moveId() == null || !moveIds.contains(s.moveId()))) bad.add(who + "/" + t + " must name a move of the row, not " + s.moveId());
            if (!t.needsMove() && s.moveId() != null) bad.add(who + "/" + t + " stands alone and must not name a move");
        }
        if (row.traitPoints() > row.letter().traitBudget())
            bad.add(who + " spends " + row.traitPoints() + " trait points, the " + row.letter().label() + " tier has " + row.letter().traitBudget());
        double attack = row.character().attack();
        for (TierMove m : row.moves()) {
            double cap = m.ultimate() ? MAX_ULTIMATE : MAX_ORDINARY;
            if (m.damage() > cap) bad.add(who + "/" + m.id() + " hits for " + m.damage() + "x, over the " + cap + "x cap");
        }
        for (TierTraitSpec s : row.traits()) {
            if (s.moveId() == null) continue;
            for (TierMove m : row.moves()) {
                if (!m.id().equals(s.moveId()) || !m.ultimate()) continue;
                // an ultimate already leaves him exposed (no armour or guard then) and an echoed ultimate would double the biggest hit
                if (s.trait() == TierTrait.ECHO || s.trait() == TierTrait.MIRROR || s.trait() == TierTrait.UNTOUCHABLE)
                    bad.add(who + "/" + s.trait() + " cannot hang on the ultimate " + m.id());
            }
        }
        if (attack <= 0) bad.add(who + " has no attack");
        return bad;
    }

    /** Every boss of a tier within the band of the tier's mean at the reference power (empty = fine). Tiers with fewer than three bosses are not compared. */
    public static List<String> bandProblems(List<TierRow> rows) {
        List<String> bad = new ArrayList<>();
        Map<TierLetter, Integer> count = new EnumMap<>(TierLetter.class);
        for (TierRow r : rows) count.merge(r.letter(), 1, Integer::sum);
        for (TierLetter l : TierLetter.values()) {
            if (count.getOrDefault(l, 0) < 3) continue;
            double mean = mean(rows, l, REF_POWER), lo = mean * (1 - BAND), hi = mean * (1 + BAND);
            for (TierRow r : rows) {
                if (r.letter() != l) continue;
                double p = pressure(r, REF_POWER);
                if (p < lo || p > hi)
                    bad.add(String.format(Locale.ROOT, "%s pressure %.2f is outside the %s band %.2f..%.2f (mean %.2f)", r.id(), p, l.label(), lo, hi, mean));
            }
        }
        return bad;
    }

    /** A plain text table (id, tier, health, attack, pressure at 20 / 60 / 100, trait points, traits) for the docs and the test log. */
    public static String table(List<TierRow> rows) {
        StringBuilder b = new StringBuilder();
        b.append(String.format(Locale.ROOT, "%-20s %-2s %6s %5s %7s %7s %7s %3s  %s%n", "boss", "T", "hp", "atk", "p@20", "p@60", "p@100", "pts", "traits"));
        for (TierRow r : rows) {
            StringBuilder tr = new StringBuilder();
            for (TierTraitSpec s : r.traits()) tr.append(tr.length() == 0 ? "" : ", ").append(s.trait()).append(s.moveId() == null ? "" : "(" + s.moveId() + ")");
            b.append(String.format(Locale.ROOT, "%-20s %-2s %6.0f %5.1f %7.2f %7.2f %7.2f %3d  %s%n", r.id(), r.letter().label(), r.character().health(), r.character().attack(),
                    pressure(r, 20), pressure(r, 60), pressure(r, 100), r.traitPoints(), tr));
        }
        return b.toString();
    }
}
