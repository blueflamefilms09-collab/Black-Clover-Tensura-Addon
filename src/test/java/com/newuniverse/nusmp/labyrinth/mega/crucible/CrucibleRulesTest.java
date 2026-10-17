package com.newuniverse.nusmp.labyrinth.mega.crucible;

import com.newuniverse.nusmp.labyrinth.mega.crucible.CrucibleRules.Phase;
import com.newuniverse.nusmp.labyrinth.mega.crucible.CrucibleRules.Pick;
import com.newuniverse.nusmp.labyrinth.mega.crucible.CrucibleRules.Verdict;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Every transition of the Crucible and the one-pick rule, as pure functions: the offer count, the lowest grade the vault offers (and that the grade
 * names stay in the order of {@code relics.Grade}), the legal clicks (including the double click and a second player in the same tick), the phases,
 * the deterministic and weighted catastrophe draw, and the scaling of the flood, the collapse pulse and the Cursed Spirit.
 */
class CrucibleRulesTest {
    private static List<String> offer(String... ids) { return new ArrayList<>(java.util.Arrays.asList(ids)); }

    // ---------------------------------------------------------------- the offer
    @Test
    void theOfferIsTheConfiguredCountWithinThePedestals() {
        assertEquals(3, CrucibleRules.offerCount(3, 3), "the default: three relics on three pedestals");
        assertEquals(1, CrucibleRules.offerCount(1, 3), "an owner who wants a stingier vault");
        assertEquals(2, CrucibleRules.offerCount(3, 2), "never more relics than pedestals");
        assertEquals(1, CrucibleRules.offerCount(0, 3), "a count below 1 is read as 1");
        assertEquals(3, CrucibleRules.offerCount(9, 5), "never more than three");
        assertEquals(0, CrucibleRules.offerCount(2, 0), "no pedestal, no offer");
    }

    @Test
    void onlyAnUnopenedVaultRolls() {
        assertTrue(CrucibleRules.shouldRoll(Phase.UNOPENED));
        assertFalse(CrucibleRules.shouldRoll(Phase.ROLLED));
        assertFalse(CrucibleRules.shouldRoll(Phase.CHOSEN));
        assertFalse(CrucibleRules.shouldRoll(Phase.RESOLVED));
    }

    @Test
    void theGradeNamesAreInTheOrderOfTheGradeEnum() throws IOException {
        // Grade.java is the source of truth; the pure mirror must list the same names in the same order, so a grade appended there is noticed here
        Path file = Path.of("src/main/java/com/newuniverse/nusmp/relics/Grade.java");
        String src = Files.readString(file, StandardCharsets.UTF_8);
        int open = src.indexOf("public enum Grade {");
        assertTrue(open >= 0, "Grade.java has no enum declaration");
        int end = src.indexOf(";", src.indexOf("GENESIS(", open));
        String body = src.substring(open, end);
        List<String> names = new ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("([A-Z_]+)\\(\"").matcher(body);
        while (m.find()) names.add(m.group(1).toLowerCase(java.util.Locale.ROOT));
        assertEquals(names, CrucibleRules.GRADES, "CrucibleRules.GRADES must mirror relics.Grade (append new grades at the end of both)");
        assertEquals(CrucibleRules.GRADE_MAX, CrucibleRules.GRADES.size() - 1);
        assertEquals("unique", CrucibleRules.GRADES.get(CrucibleRules.GRADE_UNIQUE));
    }

    @Test
    void aGradeIsRecognisedByNameInAnyCase() {
        assertEquals(3, CrucibleRules.gradeOrdinal("UNIQUE"));
        assertEquals(3, CrucibleRules.gradeOrdinal("unique"));
        assertEquals(4, CrucibleRules.gradeOrdinal("Legend Grade"));
        assertEquals(4, CrucibleRules.gradeOrdinal("  legend  "));
        assertEquals(6, CrucibleRules.gradeOrdinal("Genesis"));
        assertEquals(0, CrucibleRules.gradeOrdinal("normal"));
        assertEquals(5, CrucibleRules.gradeOrdinal("god"));
        assertEquals(5, CrucibleRules.gradeOrdinal("Mythical"), "the owner's name for GOD");
        assertEquals(-1, CrucibleRules.gradeOrdinal("epic"));
        assertEquals(-1, CrucibleRules.gradeOrdinal(""));
        assertEquals(-1, CrucibleRules.gradeOrdinal(null));
    }

    @Test
    void theMinimumGradeIsUniqueUnlessTheConfigSaysAnotherRealGrade() {
        assertEquals(3, CrucibleRules.minGrade("UNIQUE"));
        assertEquals(3, CrucibleRules.minGrade(null), "no value: UNIQUE");
        assertEquals(3, CrucibleRules.minGrade("banana"), "a bad value falls back to UNIQUE");
        assertEquals(3, CrucibleRules.minGrade(""));
        assertEquals(4, CrucibleRules.minGrade("legend"), "an owner can make the vault stricter");
        assertEquals(2, CrucibleRules.minGrade("rare"), "or laxer");
        assertEquals(6, CrucibleRules.minGrade("GENESIS"));
        for (String g : CrucibleRules.GRADES) assertTrue(CrucibleRules.minGrade(g) >= 0 && CrucibleRules.minGrade(g) <= CrucibleRules.GRADE_MAX);
    }

    @Test
    void theVaultBoxIsFourteenAcrossAndEightHigh() {
        assertTrue(CrucibleRules.inVaultBox(0, 0, 0));
        assertTrue(CrucibleRules.inVaultBox(14, 8, -14));
        assertFalse(CrucibleRules.inVaultBox(15, 0, 0));
        assertFalse(CrucibleRules.inVaultBox(0, 0, -15));
        assertFalse(CrucibleRules.inVaultBox(0, 9, 0));
        assertFalse(CrucibleRules.inVaultBox(0, -9, 0));
    }

    // ---------------------------------------------------------------- the pick
    @Test
    void nothingCanBeTakenBeforeTheOfferIsRolled() {
        Pick p = CrucibleRules.take(Phase.UNOPENED, 0, offer("a", "b"));
        assertEquals(Verdict.NOT_OFFERED, p.verdict());
        assertEquals("", p.relicId());
        assertEquals(List.of(), p.crumble());
    }

    @Test
    void aLegalPickNamesTheRelicAndTheOthersThatVanish() {
        Pick p = CrucibleRules.take(Phase.ROLLED, 1, offer("mjolnir", "frostfang", "windcutter"));
        assertEquals(Verdict.PICK, p.verdict());
        assertEquals(1, p.slot());
        assertEquals("frostfang", p.relicId());
        assertEquals(List.of(0, 2), p.crumble());
    }

    @Test
    void aSlotThatAlreadyCrumbledIsNotCrumbledAgain() {
        Pick p = CrucibleRules.take(Phase.ROLLED, 0, offer("a", "", "c"));
        assertEquals(Verdict.PICK, p.verdict());
        assertEquals(List.of(2), p.crumble());
        assertEquals(List.of(), CrucibleRules.take(Phase.ROLLED, 0, offer("only")).crumble(), "a single relic leaves nothing to crumble");
    }

    @Test
    void theDoubleClickAndTheSecondPlayerFindTheVaultChosen() {
        List<String> offered = offer("a", "b", "c");
        Phase phase = Phase.ROLLED;
        Pick first = CrucibleRules.take(phase, 0, offered);
        assertEquals(Verdict.PICK, first.verdict());
        phase = CrucibleRules.afterPick();                                          // the caller writes CHOSEN FIRST
        for (int i : first.crumble()) offered.set(i, "");
        Pick second = CrucibleRules.take(phase, 0, offered);                        // the same slot again (a double click)
        assertEquals(Verdict.TOO_LATE, second.verdict());
        assertEquals(Verdict.TOO_LATE, CrucibleRules.take(phase, 1, offered).verdict(), "another player, another pedestal, the same tick");
        assertEquals(Verdict.TOO_LATE, CrucibleRules.take(phase, 2, offered).verdict());
        int picks = 0;
        for (int slot = 0; slot < 3; slot++) if (CrucibleRules.take(phase, slot, offered).verdict() == Verdict.PICK) picks++;
        assertEquals(0, picks, "after the first pick nobody can pick again, whatever the slot");
    }

    @Test
    void exactlyOneOfManyCallsIsEverAPick() {
        for (int clickers = 2; clickers <= 6; clickers++) {
            List<String> offered = offer("a", "b", "c");
            Phase phase = Phase.ROLLED;
            int picks = 0;
            for (int c = 0; c < clickers; c++) {
                Pick p = CrucibleRules.take(phase, c % 3, offered);
                if (p.verdict() == Verdict.PICK) {
                    picks++;
                    phase = CrucibleRules.afterPick();
                    for (int i : p.crumble()) offered.set(i, "");
                }
            }
            assertEquals(1, picks, clickers + " clickers must produce exactly one pick");
        }
    }

    @Test
    void aFinishedVaultIsTooLate() {
        assertEquals(Verdict.TOO_LATE, CrucibleRules.take(Phase.RESOLVED, 0, offer("a")).verdict());
        assertEquals(Verdict.TOO_LATE, CrucibleRules.take(Phase.CHOSEN, 0, offer("a")).verdict());
    }

    @Test
    void anEmptyOrUnknownSlotIsRefused() {
        assertEquals(Verdict.EMPTY_SLOT, CrucibleRules.take(Phase.ROLLED, 1, offer("a", "")).verdict());
        assertEquals(Verdict.EMPTY_SLOT, CrucibleRules.take(Phase.ROLLED, 0, offer((String) null)).verdict());
        assertEquals(Verdict.UNKNOWN_SLOT, CrucibleRules.take(Phase.ROLLED, 2, offer("a", "b")).verdict(), "a third pedestal with only two relics offered");
        assertEquals(Verdict.UNKNOWN_SLOT, CrucibleRules.take(Phase.ROLLED, -1, offer("a")).verdict());
        assertEquals(Verdict.UNKNOWN_SLOT, CrucibleRules.take(Phase.ROLLED, 0, null).verdict());
    }

    // ---------------------------------------------------------------- the phases
    @Test
    void thePhasesFollowEachOther() {
        assertEquals(Phase.ROLLED, CrucibleRules.afterRoll(3));
        assertEquals(Phase.ROLLED, CrucibleRules.afterRoll(1));
        assertEquals(Phase.RESOLVED, CrucibleRules.afterRoll(0), "the world has no relic of that depth left: the vault is done, empty");
        assertEquals(Phase.CHOSEN, CrucibleRules.afterPick());
        assertEquals(Phase.RESOLVED, CrucibleRules.afterCatastrophe(Phase.CHOSEN));
        assertEquals(Phase.ROLLED, CrucibleRules.afterCatastrophe(Phase.ROLLED), "a catastrophe that was never started changes nothing");
        assertEquals(Phase.UNOPENED, CrucibleRules.afterCatastrophe(Phase.UNOPENED));
        assertEquals(Phase.RESOLVED, CrucibleRules.afterRestart(Phase.CHOSEN), "a restart mid-catastrophe ends it");
        assertEquals(Phase.ROLLED, CrucibleRules.afterRestart(Phase.ROLLED), "but a rolled offer survives a restart");
    }

    @Test
    void anUnknownSavedPhaseReadsAsUnopened() {
        assertEquals(Phase.CHOSEN, Phase.of("CHOSEN"));
        assertEquals(Phase.UNOPENED, Phase.of("nonsense"));
        assertEquals(Phase.UNOPENED, Phase.of(null));
        assertEquals(4, Phase.values().length);
    }

    // ---------------------------------------------------------------- the catastrophe
    private static Map<String, Integer> weights(int collapse, int flood, int spirit) {
        Map<String, Integer> w = new LinkedHashMap<>();
        w.put("collapse", collapse);
        w.put("flood", flood);
        w.put("spirit", spirit);
        return w;
    }

    @Test
    void theCatastropheIsTheSameForTheSameSectorAndChooser() {
        UUID who = UUID.fromString("00000000-0000-0000-0000-00000000002a");
        String first = CrucibleRules.chooseCatastrophe(weights(2, 1, 3), 123456789L, who, true);
        for (int i = 0; i < 20; i++) assertEquals(first, CrucibleRules.chooseCatastrophe(weights(2, 1, 3), 123456789L, who, true));
        assertTrue(CrucibleRules.KINDS.contains(first));
    }

    @Test
    void theCatastropheFollowsTheWeights() {
        int[] seen = new int[3];
        UUID who = UUID.fromString("00000000-0000-0000-0000-000000000007");
        int n = 12000;
        for (long seed = 0; seed < n; seed++) seen[CrucibleRules.KINDS.indexOf(CrucibleRules.chooseCatastrophe(weights(2, 1, 3), seed, who, true))]++;
        assertEquals(2.0 / 6.0, seen[0] / (double) n, 0.03, "collapse");
        assertEquals(1.0 / 6.0, seen[1] / (double) n, 0.03, "flood");
        assertEquals(3.0 / 6.0, seen[2] / (double) n, 0.03, "spirit");
    }

    @Test
    void aWeightOfZeroIsNeverDrawnAndANoSpiritGameNeverDrawsOne() {
        UUID who = UUID.randomUUID();
        for (long seed = 0; seed < 2000; seed++) {
            assertNotEquals("flood", CrucibleRules.chooseCatastrophe(weights(1, 0, 1), seed, who, true));
            assertNotEquals("spirit", CrucibleRules.chooseCatastrophe(weights(1, 1, 5), seed, who, false));
        }
        assertEquals("collapse", CrucibleRules.chooseCatastrophe(weights(0, 0, 0), 5L, who, true), "nothing drawable: the collapse");
        assertEquals("collapse", CrucibleRules.chooseCatastrophe(weights(0, 0, 9), 5L, who, false), "only the spirit, and no spirit available: the collapse");
        assertEquals("collapse", CrucibleRules.chooseCatastrophe(null, 5L, null, true));
        assertEquals("spirit", CrucibleRules.chooseCatastrophe(weights(0, 0, 9), 5L, who, true));
    }

    @Test
    void theDrawDependsOnTheSectorAndTheChooser() {
        UUID a = UUID.fromString("00000000-0000-0000-0000-000000000001"), b = UUID.fromString("00000000-0000-0000-0000-000000000002");
        assertNotEquals(CrucibleRules.drawSeed(100L, a), CrucibleRules.drawSeed(101L, a));
        assertNotEquals(CrucibleRules.drawSeed(100L, a), CrucibleRules.drawSeed(100L, b));
        assertEquals(CrucibleRules.drawSeed(100L, a), CrucibleRules.drawSeed(100L, a));
    }

    // ---------------------------------------------------------------- how hard
    @Test
    void theFloodGetsHarderWithThreatButStaysAtFour() {
        assertEquals(0, CrucibleRules.floodAmplifier(1, 0.34));
        assertEquals(0, CrucibleRules.floodAmplifier(2, 0.34));
        assertEquals(1, CrucibleRules.floodAmplifier(3, 0.34));
        assertEquals(3, CrucibleRules.floodAmplifier(10, 0.34));
        assertEquals(4, CrucibleRules.floodAmplifier(500, 0.34));
        assertEquals(0, CrucibleRules.floodAmplifier(10, 0.0));
        int last = 0;
        for (int threat = 1; threat < 60; threat++) {
            int a = CrucibleRules.floodAmplifier(threat, 0.34);
            assertTrue(a >= last, "monotone");
            last = a;
        }
    }

    @Test
    void aCollapsePulseIsAWarningNotAnExecution() {
        assertEquals(2.0f, CrucibleRules.collapsePulseDamage(1, 2.0, 0.6, 20f), 0.0001f);
        assertEquals(5.0f, CrucibleRules.collapsePulseDamage(6, 2.0, 0.6, 20f), 0.0001f);
        assertEquals(7.0f, CrucibleRules.collapsePulseDamage(200, 2.0, 0.6, 20f), 0.0001f, "capped at 35 % of a 20 HP victim");
        assertEquals(0.7f, CrucibleRules.collapsePulseDamage(200, 2.0, 0.6, 2f), 0.0001f, "and of a 2 HP victim");
        for (float hp = 1f; hp <= 400f; hp += 7f)
            for (int threat = 1; threat < 80; threat += 3) assertTrue(CrucibleRules.collapsePulseDamage(threat, 2.0, 0.6, hp) <= 0.35f * hp + 0.0001f, "hp " + hp + " threat " + threat);
    }

    @Test
    void theSpiritScalesWithThreatWithinBounds() {
        assertEquals(1.0, CrucibleRules.spiritHealthMultiplier(1, 1.0, 0.25), 0.0001);
        assertEquals(2.0, CrucibleRules.spiritHealthMultiplier(5, 1.0, 0.25), 0.0001);
        assertEquals(3.0, CrucibleRules.spiritHealthMultiplier(5, 1.5, 0.25), 0.0001, "the roster's healthScale multiplies the threat scaling");
        assertEquals(1.0, CrucibleRules.spiritHealthMultiplier(1, 0.0, 0.25), 0.0001, "a scale of 0 is read as 1");
        assertEquals(100.0, CrucibleRules.spiritHealthMultiplier(100000, 20.0, 1.0), 0.0001);
        assertEquals(0.1, CrucibleRules.spiritHealthMultiplier(1, 0.001, 0.0), 0.0001);
        assertEquals(1.0, CrucibleRules.spiritDamageMultiplier(1, 0.08), 0.0001);
        assertEquals(1.8, CrucibleRules.spiritDamageMultiplier(11, 0.08), 0.0001);
        assertEquals(20.0, CrucibleRules.spiritDamageMultiplier(1000, 0.5), 0.0001);
    }
}
