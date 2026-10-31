package com.newuniverse.nusmp.beastwill;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the progress and the safety rules of the Beast Will state, tested without a player (3.45.0): the day budget stops farming, a swap never sheds strain or cooldowns, a rejection never takes more than
 * the rules allow and always ends the form, a damaged clock can not leave a cooldown of years, the wills a player may keep are bounded, and the data files can not pass the attribute caps even at double strength.
 */
class BeastWillStateTest {
    private static final long DAY = BeastWillRules.DAY_TICKS;

    @Test
    void theDayBudgetCutsAssimilationAndMasteryAndRollsOverAtTheNextDay() {
        BeastWillState s = new BeastWillState();
        s.bind("lion_will");
        BeastWillState.Progress p = s.progress("lion_will");
        double a = s.gainAssimilation(p, 0.5, 0.3, 100);
        assertEquals(0.3, a, 1e-6);
        assertEquals(0.0, s.gainAssimilation(p, 0.5, 0.3, 200), 1e-9, "nothing more the same day");
        assertEquals(0.3, s.gainAssimilation(p, 0.5, 0.3, DAY + 5), 1e-6, "a new day, a new allowance");
        assertEquals(0.6, p.assimilation, 1e-6);
        double m = s.gainMastery(p, 0.01, 0.3, DAY + 6);
        assertEquals(0.01, m, 1e-9);
        for (int i = 0; i < 200; i++) s.gainAssimilation(p, 0.4, 1.0, 10 * DAY + i);
        assertTrue(p.assimilation <= 1.0f + 1e-6, "assimilation never passes the whole");
    }

    @Test
    void swappingKeepsStrainAndEveryCooldown() {
        BeastWillState s = new BeastWillState();
        s.bind("lion_will");
        s.bind("dragon_will");
        s.activate("lion_will", 0);
        BeastWillState.Progress lion = s.progress("lion_will");
        s.startCooldown(lion, "world_howl", 1000, 900);
        s.addStrain(41);
        s.activate("dragon_will", 5000);
        assertEquals(41f, s.strain, 1e-6, "the strain belongs to the body");
        assertEquals(900, s.left(lion, "world_howl", 1000), "the cooldown stays with its will");
        assertTrue(s.swapLocked(1000) && !s.swapLocked(5000));
        s.activate("lion_will", 9000);
        assertEquals(900, s.left(lion, "world_howl", 1000));
        assertEquals(BeastWillRules.OFF, s.phase, "a swap always ends the form");
    }

    @Test
    void rejectionEndsTheFormGoesQuietCostsALimitedShareAndKeepsTheRest() {
        BeastWillState s = new BeastWillState();
        s.bind("dragon_will");
        s.activate("dragon_will", 0);
        BeastWillState.Progress p = s.activeProgress();
        p.assimilation = 0.8f;
        p.mastery = 0.6f;
        s.phase = BeastWillRules.INTEGRATE;
        s.strain = 100f;
        s.reject(1000, 12000, 0.10, BeastWillRules.EXHAUSTED_AT);
        assertEquals(BeastWillRules.OFF, s.phase);
        assertEquals(0.7f, p.assimilation, 1e-6);
        assertEquals(0.6f, p.mastery, 1e-6, "mastery is never touched");
        assertTrue(s.locked(1000) && !s.locked(13000));
        assertTrue(s.strain <= BeastWillRules.EXHAUSTED_AT, "the strain falls so the player is not locked out forever");
        s.reject(2000, 100, 5.0, 0);                                                           // a huge loss is cut to half
        assertTrue(p.assimilation >= 0.2f - 1e-6);
        assertTrue(s.has("dragon_will"), "the will is never unbound");
    }

    @Test
    void aWrongClockCanNotLeaveACooldownOfYears() {
        BeastWillState s = new BeastWillState();
        s.bind("lion_will");
        BeastWillState.Progress p = s.progress("lion_will");
        s.startCooldown(p, "mane_surge", 0, 2_000_000_000);
        s.lockUntil = 2_000_000_000L;
        s.swapLockUntil = 2_000_000_000L;
        s.clampClock(100, 24000);
        assertTrue(s.left(p, "mane_surge", 100) <= 24000);
        assertTrue(s.lockUntil <= 100 + 240000 && s.swapLockUntil <= 100 + 240000);
    }

    @Test
    void aPlayerKeepsAtMostEightWillsAndANewOneIsRefusedNotDropped() {
        BeastWillState s = new BeastWillState();
        for (int i = 0; i < BeastWillState.MAX_WILLS; i++) assertTrue(s.bind("w" + i));
        assertFalse(s.bind("one_more"));
        assertEquals(BeastWillState.MAX_WILLS, s.wills.size());
        assertTrue(s.bind("w3"), "binding a held will again keeps its progress and is fine");
    }

    @Test
    void anEmptyStateIsNotSavedAndStrainStaysInRange() {
        BeastWillState s = new BeastWillState();
        assertTrue(s.isEmpty());
        s.addStrain(500);
        assertEquals(BeastWillRules.STRAIN_MAX, s.strain);
        s.addStrain(-500);
        assertEquals(0f, s.strain);
        s.bind("lion_will");
        assertFalse(s.isEmpty());
    }

    @Test
    void noWillInTheDataFilesCanPassTheAttributeCapsEvenAtDoubleStrength() throws Exception {
        for (String id : new String[]{"dragon_will", "shadow_panther_will", "elderwood_guardian_will", "lion_will"}) {
            BeastWillDef d = BeastWillSchemaTest.load(id);
            for (int phase = 0; phase <= 2; phase++) {
                for (Map.Entry<String, Double> e : BeastWillAttributes.totals(d, phase, 2.0).entrySet()) {
                    BeastWillRules.AttrCap cap = BeastWillRules.ATTR_CAPS.get(e.getKey().substring(0, e.getKey().indexOf('|')));
                    assertNotNull(cap, id + " touches an attribute with no cap: " + e.getKey());
                    double max = e.getKey().endsWith("|ADD") ? cap.maxAdd() : cap.maxMult();
                    assertTrue(e.getValue() <= max + 1e-9, id + " phase " + phase + " passes the cap on " + e.getKey() + ": " + e.getValue() + " > " + max);
                }
            }
        }
    }

    @Test
    void theDataFilesSaveCompatibilityIdsNeverChange() throws Exception {
        for (String id : new String[]{"dragon_will", "shadow_panther_will", "elderwood_guardian_will", "lion_will"})
            assertTrue(Files.exists(Path.of("src/main/resources/data/nusmp/beast_will/" + id + ".json")), id + " is a save key: it must not be renamed or removed");
    }
}
