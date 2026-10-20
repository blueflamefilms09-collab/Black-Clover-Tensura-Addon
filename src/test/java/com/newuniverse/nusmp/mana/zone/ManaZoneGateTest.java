package com.newuniverse.nusmp.mana.zone;

import com.newuniverse.nusmp.mana.zone.ManaZoneGate.Progress;
import com.newuniverse.nusmp.mana.zone.ManaZoneGate.Req;
import com.newuniverse.nusmp.mana.zone.ManaZoneGate.Verdict;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** The late-game gate of the Mana Zone: every requirement, the floors, the Wizard King, and the shipped data file. */
class ManaZoneGateTest {
    private static final ManaZoneRules.Gate G = ManaZoneRules.DEFAULT_GATE;

    private static Progress ready() { return new Progress(3_000_000, 14, 1.0, 0.0, 20, 45, 2000, false); }

    @Test
    void aFreshPlayerHasNoneOfIt() {
        Verdict v = ManaZoneGate.evaluate(G, 1.0, Progress.NONE);
        assertFalse(v.open());
        assertEquals(6, v.checks().size());
        assertEquals(6, v.unmet().size(), "every one of the six requirements is open");
    }

    @Test
    void anEarlyGamePlayerIsRefused() {
        // 100k EP, a Junior knight, half-mastered grimoire, a handful of bosses: strong for the mid game, nowhere near the Mana Zone
        Progress mid = new Progress(100_000, 4, 0.5, 0.0, 4, 8, 120, false);
        assertFalse(ManaZoneGate.passes(G, 1.0, mid));
    }

    @Test
    void everyRequirementIsNeeded() {
        assertTrue(ManaZoneGate.passes(G, 1.0, ready()));
        Progress p = ready();
        assertFalse(ManaZoneGate.passes(G, 1.0, new Progress(1_999_999, p.knightStep(), p.grimoireMastery(), p.reinforcementMastery(), p.bossKills(), p.deepestFloor(), p.attuneSeconds(), false)), "EP");
        assertFalse(ManaZoneGate.passes(G, 1.0, new Progress(p.ep(), 13, p.grimoireMastery(), p.reinforcementMastery(), p.bossKills(), p.deepestFloor(), p.attuneSeconds(), false)), "rank");
        assertFalse(ManaZoneGate.passes(G, 1.0, new Progress(p.ep(), p.knightStep(), 0.99, 0.59, p.bossKills(), p.deepestFloor(), p.attuneSeconds(), false)), "mana skin");
        assertFalse(ManaZoneGate.passes(G, 1.0, new Progress(p.ep(), p.knightStep(), p.grimoireMastery(), p.reinforcementMastery(), 14, p.deepestFloor(), p.attuneSeconds(), false)), "bosses");
        assertFalse(ManaZoneGate.passes(G, 1.0, new Progress(p.ep(), p.knightStep(), p.grimoireMastery(), p.reinforcementMastery(), p.bossKills(), 39, p.attuneSeconds(), false)), "depth");
        assertFalse(ManaZoneGate.passes(G, 1.0, new Progress(p.ep(), p.knightStep(), p.grimoireMastery(), p.reinforcementMastery(), p.bossKills(), p.deepestFloor(), 1799, false)), "attunement");
    }

    @Test
    void reinforcementMasteryAlsoCountsAsTheManaSkin() {
        Progress p = new Progress(3_000_000, 14, 0.3, 0.6, 20, 45, 2000, false);
        Verdict v = ManaZoneGate.evaluate(G, 1.0, p);
        assertTrue(v.open());
        assertTrue(v.checks().stream().filter(c -> c.req() == Req.MANA_SKIN).findFirst().orElseThrow().met());
    }

    @Test
    void theWizardKingIsOpenByTheCrown() {
        Progress king = new Progress(0, 16, 0, 0, 0, 0, 0, true);
        Verdict v = ManaZoneGate.evaluate(G, 1.0, king);
        assertTrue(v.open());
        assertFalse(v.unmet().isEmpty(), "the checklist is still honest about what the king lacks");
    }

    @Test
    void theFloorsHoldWhateverTheFileSays() {
        ManaZoneRules.Gate cheap = new ManaZoneRules.Gate(1_000, 0, 0, 0, 0.0, 0.0, 0);
        assertEquals(ManaZoneGate.FLOOR_EP, ManaZoneGate.needEp(cheap, 1.0), 0.0);
        assertEquals(ManaZoneGate.FLOOR_STEP, ManaZoneGate.needStep(cheap, 1.0));
        assertEquals(ManaZoneGate.FLOOR_BOSS_KILLS, ManaZoneGate.needBosses(cheap, 1.0));
        assertEquals(ManaZoneGate.FLOOR_FLOOR, ManaZoneGate.needFloor(cheap, 1.0));
        assertEquals(ManaZoneGate.FLOOR_ATTUNE_SECONDS, ManaZoneGate.needAttune(cheap, 1.0));
        assertFalse(ManaZoneGate.passes(cheap, 0.0, new Progress(500_000, 5, 1.0, 1.0, 100, 100, 99999, false)), "500k EP at Intermediate is never enough, however cheap the file");
    }

    @Test
    void theConfigMultiplierIsClampedAndOnlyMovesTheQuantities() {
        assertEquals(ManaZoneGate.MIN_STRICTNESS, ManaZoneGate.clampStrictness(0.0), 0.0);
        assertEquals(ManaZoneGate.MAX_STRICTNESS, ManaZoneGate.clampStrictness(99), 0.0);
        assertEquals(1.0, ManaZoneGate.clampStrictness(Double.NaN), 0.0);
        assertEquals(4_000_000.0, ManaZoneGate.needEp(G, 2.0), 0.0);
        assertEquals(G.minKnightStep(), ManaZoneGate.needStep(G, 3.0), "the rank ladder does not stretch");
        assertEquals(1_000_000.0, ManaZoneGate.needEp(new ManaZoneRules.Gate(1_500_000, 14, 15, 40, 1, 0.6, 1800), 0.5), 0.0, "half strictness cannot go under the EP floor");
    }

    @Test
    void theSummaryNamesWhatIsMissing() {
        Verdict v = ManaZoneGate.evaluate(G, 1.0, new Progress(12_000, 3, 0.2, 0.0, 1, 2, 60, false));
        String s = v.summary();
        assertTrue(s.contains("EP 12,000 of 2,000,000"), s);
        assertTrue(s.contains("rank Junior 2nd Class of Senior 1st Class"), s);
        assertTrue(s.contains("attunement 1 min 0 s of 30 min 0 s"), s);
    }

    @Test
    void rankNames() {
        assertEquals("Junior 5th Class", ManaZoneGate.rank(0));
        assertEquals("Intermediate 5th Class", ManaZoneGate.rank(5));
        assertEquals("Senior 1st Class", ManaZoneGate.rank(14));
        assertEquals("Grand Magic Knight", ManaZoneGate.rank(15));
        assertEquals("Wizard King", ManaZoneGate.rank(16));
    }

    @Test
    void attunementOnlyGrowsInDenseManaWithTheSkinHeld() {
        assertEquals(1.0, ManaZoneGate.attuneGain(true, 2.4, true, 0.1), 0.0);
        assertEquals(0.25, ManaZoneGate.attuneGain(false, 2.0, true, 0.0), 0.0, "a naturally dense biome counts a quarter");
        assertEquals(0.0, ManaZoneGate.attuneGain(false, 1.0, true, 0.0), 0.0);
        assertEquals(0.0, ManaZoneGate.attuneGain(true, 2.4, false, 0.0), 0.0, "no mana skin held, nothing learned");
        assertEquals(0.0, ManaZoneGate.attuneGain(true, 2.4, true, 0.8), 0.0, "too weak to stand in it, nothing learned");
        // 30 minutes inside a named zone is the whole ritual
        assertEquals(1800.0, 1800 * ManaZoneGate.attuneGain(true, 2.4, true, 0.0), 0.0);
        assertEquals(7200.0, 1800 / ManaZoneGate.attuneGain(false, 2.0, true, 0.0), 0.0, "in a dense biome the same ritual takes two hours");
    }

    @Test
    void theShippedDataFileParsesToTheDefaultsAndStaysLate() throws Exception {
        String text = Files.readString(Path.of("src/main/resources/data/nusmp/mana_zone.json"));
        ManaZoneRules r = ManaZoneRules.parse(text);
        assertEquals(ManaZoneRules.DEFAULT_GATE, r.gate(), "the file and the in-code defaults say the same");
        assertEquals(ManaZoneRules.DEFAULT_ZONE, r.zone());
        assertTrue(r.gate().minEp() >= ManaZoneGate.FLOOR_EP * 2, "the shipped gate is well over the floor");
        assertTrue(r.gate().minKnightStep() >= 14);
        // the shipped numbers are late game by the mod's own scale: epScale x1 at 1k EP ... x11 at 10M, so 2M EP is on the upper half of it
        double scale = Math.min(12, 1 + Math.max(0, Math.log10(r.gate().minEp()) - 3) * 2.5);
        assertTrue(scale > 9.0 && scale < 11.0, "BalanceLaw.epScale at the gate is x" + scale);
    }

    @Test
    void aBrokenFileGivesTheDefaults() {
        assertEquals(ManaZoneRules.defaults(), ManaZoneRules.parse("not json"));
        assertEquals(ManaZoneRules.defaults(), ManaZoneRules.parse("[1,2]"));
        ManaZoneRules partial = ManaZoneRules.parse("{\"gate\":{\"min_ep\":5000000},\"zone\":{\"max_radius\":999}}");
        assertEquals(5_000_000.0, partial.gate().minEp(), 0.0);
        assertEquals(64.0, partial.zone().maxRadius(), 0.0, "a typo cannot make a lag machine");
        assertEquals(ManaZoneRules.DEFAULT_GATE.minBossKills(), partial.gate().minBossKills());
    }
}
