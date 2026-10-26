package com.newuniverse.nusmp.skilltree.line;

import com.newuniverse.nusmp.skilltree.data.RouteEvolution;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why (3.26.0, the Evolution skills): the rules of what counts, how far, and when the Ultimate opens are the part of the feature that every player of a public server depends on, so they are pure
 * functions and are tested to the edge here: a counter never goes above its count, below zero or past an int; damage is counted in whole points with the fraction carried; only hostile creatures and elites
 * count for kills and damage; the near-lethal rule; the Ultimate gate in every combination of (evolved, mastered, feature on, has an evolution); the card state; the mirror's rate limit; and that the list of
 * requirements in code is exactly the catalogue's.
 */
class EvolutionRulesTest {
    @Test
    void theRequirementsAreExactlyTheCataloguesAndTheCountCapIsThePythonOne() throws IOException {
        Set<String> catalogue = new TreeSet<>(LineCatalog.get().evolution().keySet());
        assertEquals(catalogue, new TreeSet<>(EvolutionRules.REQUIREMENTS), "EvolutionRules.REQUIREMENTS must be catalog.EVOLUTION");
        String py = Files.readString(Path.of("tools/lines/catalog.py"), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("EVOLUTION_MAX_COUNT\\s*=\\s*(\\d+)").matcher(py);
        assertTrue(m.find());
        assertEquals(Integer.parseInt(m.group(1)), EvolutionRules.MAX_COUNT);
        assertEquals(RouteEvolution.MAX_COUNT, EvolutionRules.MAX_COUNT);
    }

    @Test
    void aCounterIsClampedFromZeroToTheCount() {
        assertEquals(0, EvolutionRules.clamp(-5, 10));
        assertEquals(7, EvolutionRules.clamp(7, 10));
        assertEquals(10, EvolutionRules.clamp(11, 10));
        assertEquals(10, EvolutionRules.clamp(Long.MAX_VALUE, 10));
        assertEquals(0, EvolutionRules.clamp(Long.MIN_VALUE, 10));
        assertEquals(EvolutionRules.MAX_COUNT, EvolutionRules.clamp(Long.MAX_VALUE, Integer.MAX_VALUE), "a count above the cap is cut to the cap");
        assertEquals(0, EvolutionRules.clamp(5, 0), "a count of 0 leaves no room");
        assertEquals(0, EvolutionRules.clamp(5, -3));
    }

    @Test
    void addingNeverGoesDownAndNeverOverflows() {
        assertEquals(1, EvolutionRules.add(0, 1, 5));
        assertEquals(5, EvolutionRules.add(4, 10, 5));
        assertEquals(5, EvolutionRules.add(5, 1, 5), "at the count it stays");
        assertEquals(3, EvolutionRules.add(3, -4, 5), "a negative delta is ignored");
        assertEquals(3, EvolutionRules.add(3, 0, 5));
        assertEquals(0, EvolutionRules.add(-9, 0, 5), "a broken negative counter is repaired to 0");
        assertEquals(5, EvolutionRules.add(Integer.MAX_VALUE, Long.MAX_VALUE, 5));
        assertEquals(EvolutionRules.MAX_COUNT, EvolutionRules.add(EvolutionRules.MAX_COUNT - 1, Long.MAX_VALUE, EvolutionRules.MAX_COUNT));
        int c = 0;
        for (int i = 0; i < 20; i++) c = EvolutionRules.add(c, 1, 12);
        assertEquals(12, c, "twenty kills for a requirement of twelve stop at twelve");
    }

    @Test
    void evolvedMeansTheCounterReachedTheCountAndABrokenCountNeverEvolves() {
        assertFalse(EvolutionRules.evolved(0, 3));
        assertFalse(EvolutionRules.evolved(2, 3));
        assertTrue(EvolutionRules.evolved(3, 3));
        assertTrue(EvolutionRules.evolved(4, 3));
        assertFalse(EvolutionRules.evolved(0, 0), "a count of 0 is not free evolution");
        assertFalse(EvolutionRules.evolved(5, -1));
    }

    @Test
    void damageIsCountedInWholePointsAndTheFractionIsCarried() {
        double carry = 0;
        int points = 0;
        for (int i = 0; i < 100; i++) {
            EvolutionRules.Whole w = EvolutionRules.takeWhole(carry, 0.4);
            points += w.whole();
            carry = w.rest();
        }
        assertEquals(40, points, "a hundred hits of 0.4 are forty points, not zero");
        assertEquals(3, EvolutionRules.takeWhole(0.5, 2.7).whole());
        assertEquals(0.2, EvolutionRules.takeWhole(0.5, 2.7).rest(), 1e-9);
        assertEquals(0, EvolutionRules.takeWhole(0.0, Double.NaN).whole());
        assertEquals(0, EvolutionRules.takeWhole(0.0, Double.POSITIVE_INFINITY).whole());
        assertEquals(0, EvolutionRules.takeWhole(0.0, -4).whole());
        assertEquals(0, EvolutionRules.takeWhole(Double.NaN, 0.3).whole());
        assertEquals(EvolutionRules.MAX_COUNT, EvolutionRules.takeWhole(0.0, 1e300).whole(), "a huge number is cut to the cap, never an overflow");
    }

    @Test
    void onlyFoesAndElitesCountAndAnEliteIsABossTagOrAHundredHealth() {
        assertTrue(EvolutionRules.isElite(true, 20));
        assertTrue(EvolutionRules.isElite(false, 100));
        assertTrue(EvolutionRules.isElite(false, 400));
        assertFalse(EvolutionRules.isElite(false, 99.9));
        assertTrue(EvolutionRules.countsAsFoe(true, false));
        assertTrue(EvolutionRules.countsAsFoe(false, true), "a tough neutral creature counts as an elite");
        assertFalse(EvolutionRules.countsAsFoe(false, false), "a cow never evolves a skill");
    }

    @Test
    void aKillCountsForTheRequirementThatNamesIt() {
        assertTrue(EvolutionRules.killCounts("kills", true, false, false));
        assertFalse(EvolutionRules.killCounts("kills", false, false, false));
        assertTrue(EvolutionRules.killCounts("elite_kills", true, true, false));
        assertFalse(EvolutionRules.killCounts("elite_kills", true, false, false));
        assertTrue(EvolutionRules.killCounts("boss_kills", true, true, true));
        assertFalse(EvolutionRules.killCounts("boss_kills", true, true, false));
        assertFalse(EvolutionRules.killCounts("damage_dealt", true, true, true), "a kill is no damage counter");
        assertFalse(EvolutionRules.killCounts(null, true, true, true));
    }

    @Test
    void aDamageKindFilterOfAnyOrNothingCountsEverythingAndOtherwiseAsksTheKind() {
        assertTrue(EvolutionRules.damageKindMatches(null, k -> false));
        assertTrue(EvolutionRules.damageKindMatches("", k -> false));
        assertTrue(EvolutionRules.damageKindMatches("any", k -> false));
        assertTrue(EvolutionRules.damageKindMatches("fire", "fire"::equals));
        assertFalse(EvolutionRules.damageKindMatches("fire", "magic"::equals));
        assertFalse(EvolutionRules.damageKindMatches("fire", null));
    }

    @Test
    void aBlowSurvivedCountsOnlyWhenItLeftTwoHealthOrLessAndTookAHeart() {
        assertTrue(EvolutionRules.lethalSurvived(2.0f, 6f, true));
        assertTrue(EvolutionRules.lethalSurvived(0.5f, 2f, true));
        assertFalse(EvolutionRules.lethalSurvived(2.5f, 6f, true), "left with more than two health");
        assertFalse(EvolutionRules.lethalSurvived(1.0f, 1.9f, true), "a sliver of damage at one health is not a lethal blow");
        assertFalse(EvolutionRules.lethalSurvived(0f, 20f, false), "dead is not survived");
        assertEquals(100, EvolutionRules.LETHAL_REST_TICKS, "one every five seconds");
    }

    @Test
    void theUltimateGateInEveryCombination() {
        // feature on, a line with an evolution, the art is the Ultimate
        assertEquals(EvolutionRules.Gate.NOT_EVOLVED, EvolutionRules.gate(true, true, true, 0, 10, 1.0));
        assertEquals(EvolutionRules.Gate.NOT_EVOLVED, EvolutionRules.gate(true, true, true, 9, 10, 1.0), "one short");
        assertEquals(EvolutionRules.Gate.NOT_MASTERED, EvolutionRules.gate(true, true, true, 10, 10, 0.99));
        assertEquals(EvolutionRules.Gate.NOT_MASTERED, EvolutionRules.gate(true, true, true, 10, 10, Double.NaN));
        assertEquals(EvolutionRules.Gate.OPEN, EvolutionRules.gate(true, true, true, 10, 10, 1.0));
        assertEquals(EvolutionRules.Gate.OPEN, EvolutionRules.gate(true, true, true, 12, 10, 1.0));
        assertEquals(EvolutionRules.Gate.NOT_EVOLVED, EvolutionRules.gate(true, true, true, 0, 10, 0.0), "what to go and do comes first");
        // not the Ultimate: never gated
        assertEquals(EvolutionRules.Gate.OPEN, EvolutionRules.gate(true, false, true, 0, 10, 0.0));
        // the feature off: the Ultimate is an ordinary finisher
        assertEquals(EvolutionRules.Gate.OPEN, EvolutionRules.gate(false, true, true, 0, 10, 0.0));
        // a flagged art in a route with no evolution block is never locked (the schema warns about it)
        assertEquals(EvolutionRules.Gate.OPEN, EvolutionRules.gate(true, true, false, 0, 0, 0.0));
    }

    @Test
    void theCardShowsProgressThenNeedsMasteryThenReady() {
        assertEquals(EvolutionRules.Card.PROGRESS, EvolutionRules.card(4, 10, true));
        assertEquals(EvolutionRules.Card.NEEDS_MASTERY, EvolutionRules.card(10, 10, false));
        assertEquals(EvolutionRules.Card.READY, EvolutionRules.card(10, 10, true));
        assertEquals("4 / 10", EvolutionRules.progressText(4, 10));
        assertEquals("10 / 10", EvolutionRules.progressText(99, 10), "the progress is shown bounded by the count");
        assertEquals("0 / 10", EvolutionRules.progressText(-3, 10));
    }

    @Test
    void theMirrorIsRateLimitedButTheMomentOfEvolutionIsSentAtOnce() {
        assertFalse(EvolutionRules.shouldMirror(5, 5, 100, 0, 10_000), "nothing changed");
        assertFalse(EvolutionRules.shouldMirror(5, 6, 100, 1000, 1000 + EvolutionRules.MIRROR_TICKS - 1), "too soon");
        assertTrue(EvolutionRules.shouldMirror(5, 6, 100, 1000, 1000 + EvolutionRules.MIRROR_TICKS));
        assertTrue(EvolutionRules.shouldMirror(99, 100, 100, 1000, 1001), "the moment it evolves is always sent");
        assertTrue(EvolutionRules.shouldMirror(5, 6, 100, 5000, 100), "a clock that went backwards (a reloaded world) does not freeze the mirror");
    }

    @Test
    void theGateOpensTheUltimateOnlyAtFullMasteryOfTheTensuraSkill() {
        assertTrue(com.newuniverse.nusmp.skilltree.unique.MasteryLevels.mastered(1.0));
        assertTrue(com.newuniverse.nusmp.skilltree.unique.MasteryLevels.mastered(0.9999999999));
        assertFalse(com.newuniverse.nusmp.skilltree.unique.MasteryLevels.mastered(0.999));
        assertFalse(com.newuniverse.nusmp.skilltree.unique.MasteryLevels.mastered(Double.NaN));
        List<Double> samples = new ArrayList<>(List.of(0.0, 0.5, 0.9, 0.99, 1.0));
        int open = 0;
        for (double f : samples) if (EvolutionRules.gate(true, true, true, 10, 10, f) == EvolutionRules.Gate.OPEN) open++;
        assertEquals(1, open);
    }
}
