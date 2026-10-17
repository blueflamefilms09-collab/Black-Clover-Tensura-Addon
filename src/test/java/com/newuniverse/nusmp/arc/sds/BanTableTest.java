package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Ban's move table, and a played-out fight through the framework's scheduler: what he does at each power and distance. */
class BanTableTest {
    static final List<BanTable.Spec> SPECS = BanTable.specs();

    @Test
    void theTableIsWellFormed() {
        assertTrue(SPECS.size() >= 8 && SPECS.size() <= 14, "8 to 14 moves");
        Set<String> ids = new HashSet<>();
        for (BanTable.Spec s : SPECS) {
            assertTrue(ids.add(s.id()), "unique id " + s.id());
            assertFalse(s.label().isBlank());
            assertTrue(s.minPower() >= 20 && s.minPower() <= 100 && s.minPower() % 20 == 0, s.id() + " opens at a power step");
            if (s.ultimate()) assertTrue(Math.round(s.windUpTicks() * 0.75f) >= 30, s.id() + " keeps 30 ticks of tell even at the Gates");
            else assertTrue(Math.round(s.windUpTicks() * 0.75f) >= 12, s.id() + " keeps 12 ticks of tell even at the Gates");
            assertTrue(s.cooldownTicks() >= 60 && s.weight() >= 1);
            assertTrue(s.minRange() >= 0 && s.maxRange() > s.minRange());
            assertTrue(s.recovery() >= 8);
            assertTrue(s.manaCost() > 0);
        }
        assertEquals(BanTable.COMBO, SPECS.get(0).id());
        assertNull(BanTable.byId("nope"));
    }

    @Test
    void theMixIsStaffStealGapCloserHoldBuffAndOneUltimate() {
        assertEquals(1, SPECS.stream().filter(BanTable.Spec::ultimate).count());
        BanTable.Spec hunt = BanTable.byId(BanTable.HUNT);
        assertTrue(hunt.ultimate() && hunt.windUpTicks() >= 56 && hunt.recovery() >= 40 && hunt.cooldownTicks() >= PowerCurve.tuning(100).ultimateGapTicks());
        assertEquals(60, hunt.minPower());
        assertTrue(SPECS.stream().anyMatch(s -> s.maxRange() <= 6), "melee");
        assertTrue(SPECS.stream().anyMatch(s -> s.minRange() >= 4 && s.maxRange() >= 16), "a steal at range");
        assertTrue(SPECS.stream().anyMatch(BanTable.Spec::crowd));
        for (String id : new String[]{BanTable.SNATCH, BanTable.BARRAGE, BanTable.FOX_STEP, BanTable.HUNTER_FEST, BanTable.CONCENTRATION, BanTable.LASH, BanTable.SWEEP, BanTable.GEYSER})
            assertNotNull(BanTable.byId(id), id);
        assertTrue(BanTable.byId(BanTable.SNATCH).minPower() <= 20, "he steals from the first second");
        assertTrue(BanTable.byId(BanTable.BARRAGE).minPower() >= 60, "the barrage snatches from several, later");
    }

    @Test
    void thereIsNoDeadRangeAtTheOpening() {
        for (double d = 0; d <= 26; d += 0.5) {
            double dist = d;
            assertTrue(SPECS.stream().anyMatch(s -> !s.ultimate() && s.minPower() <= 20 && dist >= s.minRange() && dist <= s.maxRange()),
                    "an attack to throw at " + d + " blocks at 20%");
        }
    }

    @Test
    void damageNumbersAreSane() {
        assertTrue(BanTable.HUNTER_DAMAGE > BanTable.STEP_DAMAGE && BanTable.STEP_DAMAGE > BanTable.COMBO_DAMAGE, "the heart-grab is his heaviest blow");
        assertTrue(BanTable.COMBO_DAMAGE * 3 > BanTable.LASH_DAMAGE, "three strikes of the combo outweigh one extended strike");
        assertTrue(BanTable.SNATCH_DAMAGE < BanTable.COMBO_DAMAGE, "the Snatch is for stealing, not for damage");
    }

    @Test
    void atTheOpeningHeUsesOnlyTheOpeningMovesAndNeverTheUltimate() {
        for (long seed = 1; seed <= 5; seed++) {
            Map<String, Integer> used = SdsaFightSim.play(SPECS, seed, 20, 20000);
            for (String id : used.keySet()) assertTrue(BanTable.byId(id).minPower() <= 20, id + " is not an opening move");
            assertNull(used.get(BanTable.HUNT));
            assertTrue(used.size() >= 4, "variety at 20%: " + used.keySet());
            assertNotNull(used.get(BanTable.SNATCH), "he snatches in the opening");
        }
    }

    @Test
    void theHuntNeedsSixtyPercentAndTheGeyserEighty() {
        for (long seed = 1; seed <= 5; seed++) {
            assertNull(SdsaFightSim.play(SPECS, seed, 59, 20000).get(BanTable.HUNT));
            assertNotNull(SdsaFightSim.play(SPECS, seed, 60, 20000).get(BanTable.HUNT));
            assertNull(SdsaFightSim.play(SPECS, seed, 79, 30000).get(BanTable.GEYSER));
            assertNotNull(SdsaFightSim.play(SPECS, seed, 80, 30000).get(BanTable.GEYSER));
        }
    }

    @Test
    void atTheGatesEveryMoveGetsUsed() {
        for (long seed = 1; seed <= 3; seed++) {
            Map<String, Integer> used = SdsaFightSim.play(SPECS, seed, 100, 60000);
            for (BanTable.Spec s : SPECS) assertNotNull(used.get(s.id()), s.id() + " is used at the Gates (seed " + seed + ")");
            assertTrue((long) used.get(BanTable.HUNT) * PowerCurve.tuning(100).ultimateGapTicks() <= 60000 + PowerCurve.tuning(100).ultimateGapTicks(), "never faster than the gap");
        }
    }
}
