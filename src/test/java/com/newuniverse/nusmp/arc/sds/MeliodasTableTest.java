package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Meliodas's move table, and a played-out fight through the framework's scheduler: what he does at each power and distance. */
class MeliodasTableTest {
    static final List<MeliodasTable.Spec> SPECS = MeliodasTable.specs();

    @Test
    void theTableIsWellFormed() {
        assertTrue(SPECS.size() >= 10 && SPECS.size() <= 14, "8 to 14 moves");
        Set<String> ids = new HashSet<>();
        for (MeliodasTable.Spec s : SPECS) {
            assertTrue(ids.add(s.id()), "unique id " + s.id());
            assertFalse(s.label().isBlank());
            assertTrue(s.minPower() >= 20 && s.minPower() <= 100 && s.minPower() % 20 == 0, s.id() + " opens at a power step");
            assertTrue(s.windUpTicks() > 0, s.id() + " has a telegraph");
            if (s.ultimate()) assertTrue(Math.round(s.windUpTicks() * 0.75f) >= 30, s.id() + " keeps 30 ticks of tell even at the Gates");
            else assertTrue(Math.round(s.windUpTicks() * 0.75f) >= 12, s.id() + " keeps 12 ticks of tell even at the Gates");
            assertTrue(s.cooldownTicks() >= 60 && s.weight() >= 1);
            assertTrue(s.minRange() >= 0 && s.maxRange() > s.minRange());
            assertTrue(s.recovery() >= 8, "a beat to breathe after every move");
            assertTrue(s.manaCost() > 0);
        }
        assertEquals(MeliodasTable.SLASH, SPECS.get(0).id(), "the order is the index the client reads");
        assertNull(MeliodasTable.byId("nope"));
    }

    @Test
    void theMixIsMeleeRangedAreaMobilityAndTwoUltimates() {
        long ultimates = SPECS.stream().filter(MeliodasTable.Spec::ultimate).count();
        assertEquals(2, ultimates);
        for (String id : new String[]{MeliodasTable.PURGATORY, MeliodasTable.WRATH}) {
            MeliodasTable.Spec u = MeliodasTable.byId(id);
            assertTrue(u.windUpTicks() >= 56, id + " is a ritual, not a flick");
            assertTrue(u.recovery() >= 40, "he is exposed afterwards");
            assertTrue(u.cooldownTicks() >= PowerCurve.tuning(100).ultimateGapTicks());
        }
        assertEquals(60, MeliodasTable.byId(MeliodasTable.PURGATORY).minPower());
        assertEquals(100, MeliodasTable.byId(MeliodasTable.WRATH).minPower(), "the Demon King's own move waits for the Gates");
        assertTrue(SPECS.stream().anyMatch(s -> s.maxRange() <= 10), "melee");
        assertTrue(SPECS.stream().anyMatch(s -> s.minRange() >= 5 && s.maxRange() >= 26), "ranged");
        assertTrue(SPECS.stream().anyMatch(MeliodasTable.Spec::crowd), "crowd moves");
        for (String id : new String[]{MeliodasTable.FULL_COUNTER, MeliodasTable.HELLBLAZE, MeliodasTable.KAMICHIGIRI, MeliodasTable.DEMON_STEP,
                MeliodasTable.BURST, MeliodasTable.IMAGES, MeliodasTable.RUSH, MeliodasTable.LUNGE, MeliodasTable.WAVE})
            assertNotNull(MeliodasTable.byId(id), id);
        assertTrue(MeliodasTable.byId(MeliodasTable.RUSH).minPower() >= 80, "Assault Mode");
        assertTrue(MeliodasTable.byId(MeliodasTable.IMAGES).minPower() >= 60, "the mark has spread");
    }

    @Test
    void thereIsNoDeadRangeAtTheOpening() {
        for (double d = 0; d <= 32; d += 0.5) {
            double dist = d;
            assertTrue(SPECS.stream().anyMatch(s -> !s.ultimate() && s.minPower() <= 20 && dist >= s.minRange() && dist <= s.maxRange()
                            && !s.id().equals(MeliodasTable.FULL_COUNTER)),
                    "an attack to throw at " + d + " blocks at 20%");
        }
    }

    @Test
    void manaMatchesTheFramework() {
        for (MeliodasTable.Spec s : SPECS) {
            if (s.ultimate()) assertEquals(60, s.manaCost(), 0.0);
            else assertEquals(Math.min(32, 6 + s.cooldownTicks() / 16.0), s.manaCost(), 1e-9);
        }
        double all = SPECS.stream().filter(s -> !s.ultimate() && s.minPower() <= 20).mapToDouble(MeliodasTable.Spec::manaCost).sum();
        assertTrue(all > PowerCurve.tuning(20).manaCap() * 0.15 && all < PowerCurve.tuning(20).manaCap() * 3, "the opening pool pays for a few moves, not everything at once");
    }

    @Test
    void damageNumbersAreSane() {
        assertTrue(MeliodasTable.KAMICHIGIRI_DAMAGE > MeliodasTable.SLASH_DAMAGE, "the Hellblaze-coated cut is his heaviest blow");
        assertTrue(MeliodasTable.WAVE_DAMAGE * 5 < MeliodasTable.KAMICHIGIRI_DAMAGE * 3, "a full volley of waves is no Kamichigiri");
        assertTrue(DemonKingMath.novaDamage(0) > MeliodasTable.KAMICHIGIRI_DAMAGE, "the nova hits harder than anything ordinary");
        assertTrue(MeliodasTable.PURGATORY_DAMAGE >= MeliodasTable.BURST_DAMAGE);
    }

    @Test
    void atTheOpeningHeUsesOnlyTheOpeningMovesAndNeverAnUltimate() {
        for (long seed = 1; seed <= 5; seed++) {
            Map<String, Integer> used = SdsaFightSim.play(SPECS, seed, 20, 20, 20000);
            for (String id : used.keySet()) assertTrue(MeliodasTable.byId(id).minPower() <= 20, id + " is not an opening move");
            assertNull(used.get(MeliodasTable.PURGATORY));
            assertNull(used.get(MeliodasTable.HELLBLAZE), "Hellblaze waits for the Demon Mark");
            assertTrue(used.size() >= 3, "variety at 20%: " + used.keySet());
        }
    }

    @Test
    void thePurgatoryNeedsSixtyPercentAndTheWrathNeedsTheGates() {
        for (long seed = 1; seed <= 5; seed++) {
            assertNull(SdsaFightSim.play(SPECS, seed, 59, 59, 20000).get(MeliodasTable.PURGATORY));
            assertNotNull(SdsaFightSim.play(SPECS, seed, 60, 60, 20000).get(MeliodasTable.PURGATORY), "he uses it at 60%");
            assertNull(SdsaFightSim.play(SPECS, seed, 80, 80, 40000).get(MeliodasTable.WRATH), "not in Assault Mode");
            assertNotNull(SdsaFightSim.play(SPECS, seed, 100, 100, 40000).get(MeliodasTable.WRATH), "the Demon King uses it");
        }
    }

    @Test
    void atTheGatesEveryMoveGetsUsedAndUltimatesAreSpaced() {
        for (long seed = 1; seed <= 3; seed++) {
            Map<String, Integer> used = SdsaFightSim.play(SPECS, seed, 100, 100, 60000);
            for (MeliodasTable.Spec s : SPECS) assertNotNull(used.get(s.id()), s.id() + " is used at the Gates (seed " + seed + ")");
            int ults = used.get(MeliodasTable.PURGATORY) + used.get(MeliodasTable.WRATH);
            assertTrue(ults >= 3, "several ultimates in a long fight");
            assertTrue((long) ults * PowerCurve.tuning(100).ultimateGapTicks() <= 60000 + PowerCurve.tuning(100).ultimateGapTicks(), "never faster than the gap");
        }
    }

    @Test
    void closeInHeFightsWithTheSwordNotTheFlame() {
        Map<String, Integer> used = SdsaFightSim.play(SPECS, 11, 20, 20, 4000);
        int slash = used.getOrDefault(MeliodasTable.SLASH, 0), wave = used.getOrDefault(MeliodasTable.WAVE, 0);
        assertTrue(slash + wave > 0);
        MeliodasTable.Spec s = MeliodasTable.byId(MeliodasTable.SLASH);
        assertTrue(s.minRange() == 0 && s.maxRange() <= 5, "the slash is a close move");
        MeliodasTable.Spec w = MeliodasTable.byId(MeliodasTable.WAVE);
        assertTrue(w.minRange() >= 5, "the wave is for distance");
    }
}
