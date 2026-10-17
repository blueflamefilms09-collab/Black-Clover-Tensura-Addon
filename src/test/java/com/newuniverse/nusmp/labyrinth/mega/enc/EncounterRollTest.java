package com.newuniverse.nusmp.labyrinth.mega.enc;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the dice are the whole design of the Dungeon Master (how often, how bad, which entry, how many monsters), and every number in them is a balance
 * decision. These tests pin the arithmetic (the bands by margin, the natural 1 and 20, the budget, the cooldown, the choice with its filters and fallback, the
 * headcount) and print the outcome distributions the documentation quotes.
 */
class EncounterRollTest {
    private static final int DC = 17;

    private static EncounterRoll.Mods mods(int threat, int noise, int light, int party, int table) {
        return new EncounterRoll.Mods(threat, noise, light, party, table);
    }

    /** The exact distribution of a scenario: the band for each of the twenty natural numbers, counted. */
    private static Map<Band, Integer> distribution(EncounterRoll.Mods m) {
        Map<Band, Integer> out = new EnumMap<>(Band.class);
        for (Band b : Band.values()) out.put(b, 0);
        for (int n = 1; n <= 20; n++) out.merge(EncounterRoll.resolve(n, m, DC).band(), 1, Integer::sum);
        return out;
    }

    private static int count(Map<Band, Integer> d, Band b) { return d.get(b); }

    private static EncounterTable.Entry entry(String id, int weight, Band band, int minThreat, int maxThreat, EncounterTable.Needs needs, int minParty, boolean scale, String... counts) {
        List<EncounterTable.MobSpec> mobs = new ArrayList<>();
        for (int i = 0; i < counts.length; i++) mobs.add(new EncounterTable.MobSpec(EncounterTable.Kind.ENTITY, "minecraft:mob" + i, "", CountRange.parse(counts[i])));
        return new EncounterTable.Entry(id, weight, band, minThreat, maxThreat, "", mobs, needs, scale, minParty, "", 0, 0, 2, -1, EncounterTable.Cue.NONE);
    }

    private static EncounterTable.Entry simple(String id, int weight, Band band) {
        return entry(id, weight, band, 1, 100000, EncounterTable.Needs.NONE, 1, true, "1");
    }

    // ---------------------------------------------------------------- the check
    @Test
    void theMarginBecomesABandTwoPointsAtATime() {
        assertEquals(Band.NOTHING, Band.ofMargin(-1));
        assertEquals(Band.MINOR, Band.ofMargin(0));
        assertEquals(Band.MINOR, Band.ofMargin(1));
        assertEquals(Band.STANDARD, Band.ofMargin(2));
        assertEquals(Band.STANDARD, Band.ofMargin(3));
        assertEquals(Band.HARD, Band.ofMargin(4));
        assertEquals(Band.HARD, Band.ofMargin(5));
        assertEquals(Band.AMBUSH, Band.ofMargin(6));
        assertEquals(Band.AMBUSH, Band.ofMargin(40));
    }

    @Test
    void aNaturalOneIsAlwaysQuietWhateverTheModifiersAre() {
        EncounterRoll.Result r = EncounterRoll.resolve(1, mods(5, 4, 3, 2, 3), DC);
        assertEquals(Band.NOTHING, r.band());
        assertTrue(r.quiet());
        assertFalse(r.escalated());
        assertEquals(1 + 17, r.total());
    }

    @Test
    void aNaturalTwentyIsOneBandWorseAndTurnsAFailureIntoAMinorEncounter() {
        EncounterRoll.Result fail = EncounterRoll.resolve(20, mods(0, 0, -3, 0, 0), DC);
        assertEquals(17, fail.total());
        assertEquals(Band.STANDARD, fail.band(), "total 17 is a minor result, one worse is standard");
        assertTrue(fail.escalated());
        EncounterRoll.Result hopeless = EncounterRoll.resolve(20, mods(0, 0, -3, 0, -3), DC);
        assertEquals(14, hopeless.total());
        assertEquals(Band.MINOR, hopeless.band(), "a natural 20 never leaves the party alone");
        EncounterRoll.Result top = EncounterRoll.resolve(20, mods(5, 4, 3, 2, 3), DC);
        assertEquals(Band.AMBUSH, top.band(), "an ambush cannot get worse");
        assertTrue(top.escalated());
    }

    @Test
    void anOrdinaryRollIsReadAgainstTheDifficultyClass() {
        EncounterRoll.Mods none = mods(0, 0, 0, 0, 0);
        assertEquals(Band.NOTHING, EncounterRoll.resolve(16, none, DC).band());
        assertEquals(Band.MINOR, EncounterRoll.resolve(17, none, DC).band());
        assertEquals(Band.STANDARD, EncounterRoll.resolve(19, none, DC).band());
        EncounterRoll.Result r = EncounterRoll.resolve(12, mods(1, 2, 1, 1, 0), DC);
        assertEquals(17, r.total());
        assertEquals(0, r.margin());
        assertEquals(Band.MINOR, r.band());
    }

    @Test
    void theDistributionForAQuietSoloPartyInSectorOneIsFourFifthsNothing() {
        Map<Band, Integer> d = distribution(mods(EncounterRoll.threatMod(1, 5), NoiseMeter.mod(0.6, 4), LightRules.mod(9), PartyMath.mod(1, 2), 0));
        System.out.println("quiet solo, sector 1, light 9:        " + d);
        assertEquals(16, count(d, Band.NOTHING));
        assertEquals(2, count(d, Band.MINOR));
        assertEquals(1, count(d, Band.STANDARD));
        assertEquals(1, count(d, Band.HARD));
        assertEquals(0, count(d, Band.AMBUSH));
    }

    @Test
    void aSneakingPartyInTheDarkIsAlmostNeverFound() {
        Map<Band, Integer> d = distribution(mods(EncounterRoll.threatMod(1, 5), 0, LightRules.mod(0), 0, 0));
        System.out.println("sneaking solo in the dark, sector 1:  " + d);
        assertEquals(19, count(d, Band.NOTHING));
        assertEquals(1, count(d, Band.STANDARD));
    }

    @Test
    void aLoudBrightPartyDeepInTheDungeonIsMetNearlyEveryTime() {
        Map<Band, Integer> brawl = distribution(mods(EncounterRoll.threatMod(10, 5), NoiseMeter.mod(14.0, 4), LightRules.mod(12), PartyMath.mod(3, 2), 0));
        System.out.println("brawling trio, torch, sector 10:      " + brawl);
        assertEquals(3, count(brawl, Band.NOTHING), "+13 against a DC of 17: only the naturals 1 to 3 are quiet");
        assertEquals(11, count(brawl, Band.AMBUSH));
        Map<Band, Integer> sprint = distribution(mods(EncounterRoll.threatMod(4, 5), NoiseMeter.mod(3.0, 4), LightRules.mod(13), PartyMath.mod(2, 2), 0));
        System.out.println("sprinting duo, torch, sector 4:       " + sprint);
        int quiet = count(sprint, Band.NOTHING);
        assertEquals(10, quiet, "+6 against a DC of 17: a sprinting pair with a torch meets something on half the naturals");
    }

    @Test
    void everyModifierMakesTroubleMoreLikelyNeverLess() {
        EncounterRoll.Mods base = mods(0, 0, 0, 0, 0);
        int nothing = distribution(base).get(Band.NOTHING);
        for (EncounterRoll.Mods up : new EncounterRoll.Mods[] {mods(2, 0, 0, 0, 0), mods(0, 2, 0, 0, 0), mods(0, 0, 2, 0, 0), mods(0, 0, 0, 2, 0), mods(0, 0, 0, 0, 2)})
            assertTrue(distribution(up).get(Band.NOTHING) < nothing, up.toString());
        for (EncounterRoll.Mods down : new EncounterRoll.Mods[] {mods(0, 0, -2, 0, 0), mods(0, 0, 0, 0, -2)})
            assertTrue(distribution(down).get(Band.NOTHING) >= nothing, down.toString());
    }

    @Test
    void theModifiersAreCappedAndSummed() {
        EncounterRoll.Mods m = EncounterRoll.mods(99, 99.0, 9, 99, 9, EncounterRoll.Caps.DEFAULT);
        assertEquals(5, m.threat());
        assertEquals(4, m.noise());
        assertEquals(3, m.light());
        assertEquals(2, m.party());
        assertEquals(3, m.table());
        assertEquals(17, m.total());
        EncounterRoll.Mods low = EncounterRoll.mods(0, -4.0, -9, 0, -9, null);
        assertEquals(0, low.threat());
        assertEquals(0, low.noise());
        assertEquals(-3, low.light());
        assertEquals(-3, low.table());
        assertEquals(0, EncounterRoll.threatMod(1, 5));
        assertEquals(1, EncounterRoll.threatMod(2, 5));
        assertEquals(2, EncounterRoll.threatMod(4, 5));
        assertEquals(5, EncounterRoll.threatMod(10, 5));
        assertEquals(5, EncounterRoll.threatMod(100, 5));
        assertEquals(0, EncounterRoll.threatMod(100, 0));
    }

    @Test
    void theD20StaysInRangeAndIsFairOverManyRolls() {
        Random r = new Random(7);
        int[] seen = new int[21];
        for (int i = 0; i < 20000; i++) seen[EncounterRoll.d20(r)]++;
        assertEquals(0, seen[0]);
        for (int n = 1; n <= 20; n++) assertTrue(seen[n] > 800 && seen[n] < 1200, n + " came up " + seen[n] + " times");
    }

    // ---------------------------------------------------------------- budget and cooldown
    @Test
    void theBudgetScalesWithBandThreatPartyAndTable() {
        assertEquals(8.0, EncounterRoll.budget(8.0, Band.STANDARD, 1, 1.0, 1.0), 1e-9);
        assertEquals(4.0, EncounterRoll.budget(8.0, Band.MINOR, 1, 1.0, 1.0), 1e-9);
        assertEquals(8.0 * 1.6, EncounterRoll.budget(8.0, Band.HARD, 1, 1.0, 1.0), 1e-9);
        assertEquals(8.0 * 2.2, EncounterRoll.budget(8.0, Band.AMBUSH, 1, 1.0, 1.0), 1e-9);
        assertEquals(8.0 * 1.25, EncounterRoll.budget(8.0, Band.STANDARD, 2, 1.0, 1.0), 1e-9, "a quarter more per level above one");
        assertEquals(8.0 * 2.5, EncounterRoll.budget(8.0, Band.STANDARD, 7, 1.0, 1.0), 1e-9);
        assertEquals(8.0 * 2.1, EncounterRoll.budget(8.0, Band.STANDARD, 1, 2.1, 1.0), 1e-9);
        assertEquals(8.0 * 0.5, EncounterRoll.budget(8.0, Band.STANDARD, 1, 1.0, 0.5), 1e-9);
        assertEquals(0.0, EncounterRoll.budget(8.0, Band.NOTHING, 5, 3.0, 2.0), 0.0);
        assertEquals(8.0, EncounterRoll.budget(8.0, Band.STANDARD, 0, 1.0, 1.0), 1e-9, "threat below one is read as one");
        assertEquals(0.0, EncounterRoll.budget(-8.0, Band.HARD, 1, 1.0, 1.0), 0.0);
    }

    @Test
    void aLoudPartyIsRolledForSoonerButNeverFasterThanTheMinimum() {
        assertEquals(1800, EncounterRoll.cooldown(1800, 400, 0.0));
        assertEquals(900, EncounterRoll.cooldown(1800, 400, 5.0));
        assertEquals(600, EncounterRoll.cooldown(1800, 400, 10.0));
        assertEquals(400, EncounterRoll.cooldown(1800, 400, 40.0), "the floor");
        assertEquals(400, EncounterRoll.cooldown(300, 400, 0.0), "a base below the floor still waits the floor");
        assertEquals(1800, EncounterRoll.cooldown(1800, 400, -7.0));
        assertTrue(EncounterRoll.cooldown(1800, 400, 3.0) > EncounterRoll.cooldown(1800, 400, 6.0));
    }

    @Test
    void thePerCostOfAMonsterIsItsToughnessInTwentyHealthUnits() {
        assertEquals(1.0, EncounterRoll.cost(4), 1e-9);
        assertEquals(1.0, EncounterRoll.cost(20), 1e-9);
        assertEquals(2.5, EncounterRoll.cost(50), 1e-9);
        assertEquals(5.0, EncounterRoll.cost(100), 1e-9);
    }

    // ---------------------------------------------------------------- choosing
    @Test
    void anEntryIsChosenOnlyFromTheRolledBand() {
        List<EncounterTable.Entry> t = List.of(simple("a", 10, Band.MINOR), simple("b", 10, Band.STANDARD), simple("c", 10, Band.HARD));
        Random r = new Random(1);
        for (int i = 0; i < 200; i++) {
            EncounterRoll.Choice c = EncounterRoll.choose(t, Band.STANDARD, 1, 1, false, false, false, null, r);
            assertEquals("b", c.entry().id());
            assertEquals(Band.STANDARD, c.band());
        }
    }

    @Test
    void weightsDecideWithinTheBand() {
        List<EncounterTable.Entry> t = List.of(simple("rare", 1, Band.HARD), simple("common", 9, Band.HARD));
        Random r = new Random(3);
        int common = 0, n = 10000;
        for (int i = 0; i < n; i++) if (EncounterRoll.choose(t, Band.HARD, 1, 1, false, false, false, null, r).entry().id().equals("common")) common++;
        assertTrue(common > 8700 && common < 9300, "common was chosen " + common + " of " + n);
    }

    @Test
    void theThreatWindowAndThePartySizeFilterTheEntries() {
        List<EncounterTable.Entry> t = List.of(
                entry("early", 10, Band.STANDARD, 1, 3, EncounterTable.Needs.NONE, 1, true, "1"),
                entry("late", 10, Band.STANDARD, 4, 100, EncounterTable.Needs.NONE, 1, true, "1"),
                entry("crowd", 10, Band.STANDARD, 1, 100, EncounterTable.Needs.NONE, 3, true, "1"));
        Random r = new Random(5);
        for (int i = 0; i < 100; i++) {
            assertEquals("early", EncounterRoll.choose(t, Band.STANDARD, 2, 1, false, false, false, null, r).entry().id());
            String late = EncounterRoll.choose(t, Band.STANDARD, 5, 2, false, false, false, null, r).entry().id();
            assertEquals("late", late);
        }
        boolean sawCrowd = false;
        for (int i = 0; i < 100; i++) sawCrowd |= EncounterRoll.choose(t, Band.STANDARD, 2, 3, false, false, false, null, r).entry().id().equals("crowd");
        assertTrue(sawCrowd, "a party of three can meet the crowd entry");
    }

    @Test
    void needsDecideWhichEntriesAreOnTheTable() {
        EncounterTable.Entry stalker = entry("stalker", 10, Band.HARD, 1, 100, new EncounterTable.Needs(true, null, null), 1, true, "1");
        EncounterTable.Entry moth = entry("moth", 10, Band.HARD, 1, 100, new EncounterTable.Needs(null, true, null), 1, true, "1");
        EncounterTable.Entry brute = entry("brute", 10, Band.HARD, 1, 100, new EncounterTable.Needs(null, null, true), 1, true, "1");
        EncounterTable.Entry plain = entry("plain", 10, Band.HARD, 1, 100, new EncounterTable.Needs(false, null, null), 1, true, "1");
        List<EncounterTable.Entry> t = List.of(stalker, moth, brute, plain);
        Random r = new Random(11);
        for (int i = 0; i < 300; i++) {
            String calm = EncounterRoll.choose(t, Band.HARD, 1, 1, false, false, false, null, r).entry().id();
            assertEquals("plain", calm, "a party that is neither dark, lit nor noisy can only meet the entry that asks for no darkness");
            assertFalse(EncounterRoll.choose(t, Band.HARD, 1, 1, true, false, false, null, r).entry().id().equals("plain"), "plain needs the party not to be dark");
        }
        java.util.Set<String> inDark = new java.util.HashSet<>(), lit = new java.util.HashSet<>(), loud = new java.util.HashSet<>();
        for (int i = 0; i < 600; i++) {
            inDark.add(EncounterRoll.choose(t, Band.HARD, 1, 1, true, false, false, null, r).entry().id());
            lit.add(EncounterRoll.choose(t, Band.HARD, 1, 1, false, true, false, null, r).entry().id());
            loud.add(EncounterRoll.choose(t, Band.HARD, 1, 1, false, false, true, null, r).entry().id());
        }
        assertEquals(java.util.Set.of("stalker"), inDark);
        assertEquals(java.util.Set.of("moth", "plain"), lit);
        assertEquals(java.util.Set.of("brute", "plain"), loud);
    }

    @Test
    void aBandWithNothingUsableFallsBackToTheNextBandDown() {
        List<EncounterTable.Entry> t = List.of(simple("minor", 10, Band.MINOR), simple("standard", 10, Band.STANDARD));
        Random r = new Random(2);
        EncounterRoll.Choice c = EncounterRoll.choose(t, Band.AMBUSH, 1, 1, false, false, false, null, r);
        assertEquals("standard", c.entry().id());
        assertEquals(Band.STANDARD, c.band());
        assertEquals("minor", EncounterRoll.choose(t, Band.AMBUSH, 1, 1, false, false, false, e -> e.band() == Band.MINOR, r).entry().id());
        assertNull(EncounterRoll.choose(t, Band.AMBUSH, 1, 1, false, false, false, e -> false, r));
        assertNull(EncounterRoll.choose(t, Band.NOTHING, 1, 1, false, false, false, null, r));
        assertNull(EncounterRoll.choose(List.of(), Band.HARD, 1, 1, false, false, false, null, r));
    }

    @Test
    void anEntryTheGameCannotSpawnIsNeverChosen() {
        List<EncounterTable.Entry> t = List.of(simple("missing", 100, Band.STANDARD), simple("ok", 1, Band.STANDARD));
        Random r = new Random(9);
        for (int i = 0; i < 100; i++) assertEquals("ok", EncounterRoll.choose(t, Band.STANDARD, 1, 1, false, false, false, e -> !e.id().equals("missing"), r).entry().id());
    }

    // ---------------------------------------------------------------- the headcount
    private static List<EncounterRoll.Pick> picks(double... costs) {
        List<EncounterRoll.Pick> out = new ArrayList<>();
        for (int i = 0; i < costs.length; i++) out.add(costs[i] < 0 ? null : new EncounterRoll.Pick("minecraft:mob" + i, costs[i]));
        return out;
    }

    @Test
    void aCountRangeIsRolledAndScaledByTheParty() {
        EncounterTable.Entry e = entry("e", 1, Band.STANDARD, 1, 100, EncounterTable.Needs.NONE, 1, true, "2-4");
        Random r = new Random(4);
        int lo = 99, hi = 0;
        for (int i = 0; i < 500; i++) {
            List<EncounterRoll.Planned> p = EncounterRoll.plan(e, picks(1.0), 1.0, 100, 100, r);
            lo = Math.min(lo, p.get(0).count());
            hi = Math.max(hi, p.get(0).count());
        }
        assertEquals(2, lo);
        assertEquals(4, hi);
        int lo2 = 99, hi2 = 0;
        for (int i = 0; i < 500; i++) {
            List<EncounterRoll.Planned> p = EncounterRoll.plan(e, picks(1.0), 2.0, 100, 100, r);
            lo2 = Math.min(lo2, p.get(0).count());
            hi2 = Math.max(hi2, p.get(0).count());
        }
        assertEquals(4, lo2, "a party factor of two doubles the range");
        assertEquals(8, hi2);
    }

    @Test
    void anEntryThatDoesNotScaleWithThePartyKeepsItsCount() {
        EncounterTable.Entry e = entry("e", 1, Band.STANDARD, 1, 100, EncounterTable.Needs.NONE, 1, false, "3");
        List<EncounterRoll.Planned> p = EncounterRoll.plan(e, picks(1.0), 2.5, 100, 100, new Random(1));
        assertEquals(3, p.get(0).count());
    }

    @Test
    void theBudgetTrimsTheLastKindFirstAndKeepsAtLeastOneOfTheFirst() {
        EncounterTable.Entry e = entry("e", 1, Band.STANDARD, 1, 100, EncounterTable.Needs.NONE, 1, false, "3", "4");
        // 3 x 2.0 + 4 x 1.0 = 10 against a budget of 8: two of the second kind go
        List<EncounterRoll.Planned> p = EncounterRoll.plan(e, picks(2.0, 1.0), 1.0, 8.0, 100, new Random(1));
        assertEquals(2, p.size());
        assertEquals(3, p.get(0).count());
        assertEquals(2, p.get(1).count());
        // a budget of 4: the second kind is gone entirely, the first is cut to two
        List<EncounterRoll.Planned> q = EncounterRoll.plan(e, picks(2.0, 1.0), 1.0, 4.0, 100, new Random(1));
        assertEquals(1, q.size());
        assertEquals(2, q.get(0).count());
        // a budget of 2.5: exactly one of the first kind is left
        List<EncounterRoll.Planned> s = EncounterRoll.plan(e, picks(2.0, 1.0), 1.0, 2.5, 100, new Random(1));
        assertEquals(1, s.size());
        assertEquals(1, s.get(0).count());
    }

    @Test
    void aFightTooBigForTheBudgetIsNotFought() {
        EncounterTable.Entry e = entry("e", 1, Band.STANDARD, 1, 100, EncounterTable.Needs.NONE, 1, false, "2");
        assertTrue(EncounterRoll.plan(e, picks(6.0), 1.0, 5.0, 100, new Random(1)).isEmpty(), "one of them already costs more than the budget");
        assertEquals(1, EncounterRoll.plan(e, picks(6.0), 1.0, 6.0, 100, new Random(1)).get(0).count());
    }

    @Test
    void theMobCapTrimsTooAndNeverGoesBelowOne() {
        EncounterTable.Entry e = entry("e", 1, Band.AMBUSH, 1, 100, EncounterTable.Needs.NONE, 1, false, "6", "6");
        List<EncounterRoll.Planned> p = EncounterRoll.plan(e, picks(1.0, 1.0), 1.0, 1000, 8, new Random(1));
        int sum = 0;
        for (EncounterRoll.Planned x : p) sum += x.count();
        assertEquals(8, sum);
        assertEquals(6, p.get(0).count(), "the second kind gave way");
        assertEquals(1, EncounterRoll.plan(e, picks(1.0, 1.0), 1.0, 1000, 0, new Random(1)).get(0).count());
    }

    @Test
    void kindsTheGameCouldNotResolveAreSkippedAndTheNextBecomesTheFirst() {
        EncounterTable.Entry e = entry("e", 1, Band.STANDARD, 1, 100, EncounterTable.Needs.NONE, 1, false, "2", "3");
        List<EncounterRoll.Planned> p = EncounterRoll.plan(e, picks(-1, 1.0), 1.0, 100, 100, new Random(1));
        assertEquals(1, p.size());
        assertEquals("minecraft:mob1", p.get(0).entity());
        assertEquals(3, p.get(0).count());
        assertTrue(EncounterRoll.plan(e, picks(-1, -1), 1.0, 100, 100, new Random(1)).isEmpty());
        assertTrue(EncounterRoll.plan(e, List.of(), 1.0, 100, 100, new Random(1)).isEmpty());
    }

    @Test
    void anOptionalSecondKindWithAZeroRollIsNotListed() {
        EncounterTable.Entry e = entry("e", 1, Band.STANDARD, 1, 100, EncounterTable.Needs.NONE, 1, false, "2", "0");
        List<EncounterRoll.Planned> p = EncounterRoll.plan(e, picks(1.0, 1.0), 1.0, 100, 100, new Random(1));
        assertEquals(1, p.size());
    }
}
