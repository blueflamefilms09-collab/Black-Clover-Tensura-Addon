package com.newuniverse.nusmp.labyrinth.mega.enc;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

/**
 * Why: this is the Dungeon Master's dice. "Vanilla spawning is too predictable" is the complaint, and the answer is the oldest tool of the
 * tabletop: a d20 with modifiers against a difficulty class. {@code roll = d20 + threat + noise + light + party (+ the table's own bonus)};
 * beating the DC by 0-1 is a minor encounter, 2-3 a standard one, 4-5 a hard one, 6 or more an ambush. A natural 1 is always quiet (a lucky
 * party gets a breather whatever it did), a natural 20 always makes the trouble one band worse (and turns a failed check into a minor
 * encounter). Why a d20 and bands rather than a spawn timer: the modifiers are the whole design. A deeper sector (threat) is met more often; a
 * party that fights, sprints and breaks blocks (noise) is met more often and sooner; a party that carries lights (light) is found; a bigger
 * party draws more attention. Every modifier is a small capped integer, so no single factor can drown the others and the d20 still matters.
 * The same roll then decides how much trouble, through a BUDGET (the base times the band, times 1 + a quarter per threat level above one,
 * times the party factor): the budget caps how many monsters an entry may bring, in "weak-monster equivalents" where a 20-health monster is
 * one point and a 100-health brute is five. Everything here is arithmetic on plain numbers and a {@link Random}, so the whole distribution is
 * unit-tested. Pure.
 */
public final class EncounterRoll {
    private EncounterRoll() {}

    /** The caps of the modifiers: no single factor adds more than this to the d20. */
    public record Caps(int threat, int noise, int party) {
        public static final Caps DEFAULT = new Caps(5, 4, 2);
    }

    /** The five modifiers of one roll. */
    public record Mods(int threat, int noise, int light, int party, int table) {
        public int total() { return threat + noise + light + party + table; }
    }

    /** What the dice said: the natural number, the modifiers, the total against the DC, the band, and whether luck changed it. */
    public record Result(int natural, Mods mods, int total, int dc, int margin, Band band, boolean quiet, boolean escalated) {}

    /** An entry chosen from a table and the band it was chosen for (which can be lower than the rolled one when the rolled band had nothing). */
    public record Choice(EncounterTable.Entry entry, Band band) {}

    /** One kind of monster after the game looked it up: which entity, and what one of them costs of the budget. */
    public record Pick(String entity, double cost) {}

    /** One line of the final plan: so many of this entity. */
    public record Planned(String entity, int count) {}

    // ---------------------------------------------------------------- the dice
    /** A d20: 1 to 20. */
    public static int d20(Random r) { return 1 + r.nextInt(20); }

    /** The threat modifier: one per two levels of the sector, capped (sector 1 gives 0, sector 4 gives 2, sector 10 gives 5). */
    public static int threatMod(int threat, int cap) { return Math.max(0, Math.min(Math.max(0, cap), threat / 2)); }

    public static Mods mods(int threat, double noiseReading, int lightMod, int partySize, int tableBonus, Caps caps) {
        Caps c = caps == null ? Caps.DEFAULT : caps;
        return new Mods(threatMod(threat, c.threat()), NoiseMeter.mod(noiseReading, c.noise()), Math.max(-3, Math.min(3, lightMod)),
                PartyMath.mod(partySize, c.party()), Math.max(-3, Math.min(3, tableBonus)));
    }

    /** Reads a d20 against the DC. See the class comment for the bands and the two natural numbers. */
    public static Result resolve(int natural, Mods mods, int dc) {
        int total = natural + mods.total();
        int margin = total - dc;
        if (natural <= 1) return new Result(natural, mods, total, dc, margin, Band.NOTHING, true, false);
        Band band = Band.ofMargin(margin);
        boolean escalated = false;
        if (natural >= 20) {
            band = band == Band.NOTHING ? Band.MINOR : band.up();
            escalated = true;
        }
        return new Result(natural, mods, total, dc, margin, band, false, escalated);
    }

    // ---------------------------------------------------------------- the budget and the cooldown
    /** What one health point-equivalent of a monster costs of the budget: a 20-health monster is one point, nothing is cheaper than one. */
    public static double cost(double toughness) { return Math.max(1.0, toughness / 20.0); }

    /** {@code base * band * (1 + 0.25 * (threat - 1)) * partyFactor * tableScale}. */
    public static double budget(double base, Band band, int threat, double partyFactor, double tableScale) {
        double t = 1.0 + 0.25 * Math.max(0, threat - 1);
        return Math.max(0.0, base) * band.budgetFactor() * t * Math.max(0.0, partyFactor) * Math.max(0.0, tableScale);
    }

    /** Ticks before the same party is rolled for again after an encounter: shorter when it is loud ({@code base / (1 + noise / 5)}), never below {@code min}. */
    public static long cooldown(long base, long min, double noiseReading) {
        double n = Math.max(0.0, Math.min(NoiseMeter.CEILING, noiseReading));
        return Math.max(Math.max(0L, min), Math.round(Math.max(0L, base) / (1.0 + n / 5.0)));
    }

    // ---------------------------------------------------------------- choosing an entry
    /**
     * Picks an entry of the rolled band by weight, among those whose threat window holds the sector's threat, whose party-size minimum is met,
     * whose {@code needs} match the party's state, and which the game can actually spawn ({@code usable}: the ids exist, the faction tag is
     * not empty). When the rolled band has no such entry the next band down is tried, so a thin table degrades to a smaller fight instead of
     * to nothing; null when even the minor band has none.
     */
    public static Choice choose(List<EncounterTable.Entry> entries, Band band, int threat, int partySize, boolean dark, boolean lit, boolean noisy,
                                Predicate<EncounterTable.Entry> usable, Random r) {
        for (Band b = band; b != Band.NOTHING; b = b.down()) {
            List<EncounterTable.Entry> pool = new ArrayList<>();
            int total = 0;
            for (EncounterTable.Entry e : entries) {
                if (e.band() != b || e.weight() <= 0 || !e.fitsThreat(threat) || partySize < e.minParty() || !e.needs().met(dark, lit, noisy)) continue;
                if (usable != null && !usable.test(e)) continue;
                pool.add(e);
                total += e.weight();
            }
            if (pool.isEmpty()) continue;
            int roll = r.nextInt(total);
            for (EncounterTable.Entry e : pool) {
                roll -= e.weight();
                if (roll < 0) return new Choice(e, b);
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- turning an entry into a headcount
    /**
     * The final headcount of an entry. Each kind of monster rolls its count range (scaled by the party factor when the entry says
     * {@code scaleWithParty}); then, while the whole group costs more than the budget or is bigger than {@code maxMobs}, the last kind loses one
     * (the first kind never goes below one, it is what the entry is about). An empty list when one of the first kind alone is more than the
     * budget: that fight is not for this party at this strength. {@code picks} has one slot per {@code entry.mobs()}, null where the game could
     * not resolve the kind (the entity is not installed, the faction tag is empty): those are skipped.
     */
    public static List<Planned> plan(EncounterTable.Entry entry, List<Pick> picks, double partyFactor, double budget, int maxMobs, Random r) {
        int n = entry.mobs().size();
        int[] count = new int[n];
        int first = -1;
        for (int i = 0; i < n; i++) {
            Pick p = i < picks.size() ? picks.get(i) : null;
            if (p == null) continue;
            if (first < 0) first = i;
            CountRange range = entry.mobs().get(i).count();
            count[i] = (entry.scaleWithParty() ? range.scaled(partyFactor) : range).roll(r);
        }
        if (first < 0) return List.of();
        if (count[first] < 1) count[first] = 1;
        int cap = Math.max(1, maxMobs);
        while (total(count) > cap || spent(count, picks) > budget + 1e-9) {
            int k = -1;
            for (int i = n - 1; i >= 0; i--) {
                int floor = i == first ? 1 : 0;
                if (count[i] > floor) {
                    k = i;
                    break;
                }
            }
            if (k < 0) break;
            count[k]--;
        }
        if (total(count) > cap || spent(count, picks) > budget + 1e-9) return List.of();
        List<Planned> out = new ArrayList<>();
        for (int i = 0; i < n; i++) if (count[i] > 0) out.add(new Planned(picks.get(i).entity(), count[i]));
        return out;
    }

    private static int total(int[] count) {
        int t = 0;
        for (int c : count) t += c;
        return t;
    }

    private static double spent(int[] count, List<Pick> picks) {
        double s = 0.0;
        for (int i = 0; i < count.length; i++) if (count[i] > 0 && i < picks.size() && picks.get(i) != null) s += count[i] * picks.get(i).cost();
        return s;
    }
}
