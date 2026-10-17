package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Escanor's move table, and fights played through the framework's scheduler at different hours: what the sun lets him do. */
class EscanorTableTest {
    static final List<EscanorTable.Spec> SPECS = EscanorTable.specs();

    @Test
    void theTableIsWellFormed() {
        assertTrue(SPECS.size() >= 8 && SPECS.size() <= 14, "8 to 14 moves");
        Set<String> ids = new HashSet<>();
        for (EscanorTable.Spec s : SPECS) {
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
        assertEquals(EscanorTable.SWING, SPECS.get(0).id());
        assertNull(EscanorTable.byId("nope"));
    }

    @Test
    void theMixIsAxeFlameChargeCrowdAndTwoSunUltimates() {
        assertEquals(2, SPECS.stream().filter(EscanorTable.Spec::ultimate).count());
        EscanorTable.Spec cruel = EscanorTable.byId(EscanorTable.CRUEL_SUN), zenith = EscanorTable.byId(EscanorTable.ZENITH);
        assertEquals(80, cruel.minPower(), "needs the afternoon sun");
        assertEquals(100, zenith.minPower(), "needs noon with the Gates open");
        for (EscanorTable.Spec u : List.of(cruel, zenith)) {
            assertTrue(u.windUpTicks() >= 56 && u.recovery() >= 40 && u.cooldownTicks() >= PowerCurve.tuning(100).ultimateGapTicks());
        }
        assertTrue(SPECS.stream().anyMatch(s -> s.maxRange() <= 10), "melee");
        assertTrue(SPECS.stream().anyMatch(s -> s.minRange() >= 5 && s.maxRange() >= 26), "ranged");
        assertTrue(SPECS.stream().anyMatch(EscanorTable.Spec::crowd));
        assertTrue(SPECS.stream().anyMatch(s -> s.minRange() >= 8 && s.maxRange() >= 26), "a charge to close the gap");
        assertNotNull(EscanorTable.byId(EscanorTable.CHARGE));
        assertNotNull(EscanorTable.byId(EscanorTable.WAVE));
    }

    @Test
    void thereIsNoDeadRangeAtNight() {
        for (double d = 0; d <= 32; d += 0.5) {
            double dist = d;
            assertTrue(SPECS.stream().anyMatch(s -> !s.ultimate() && s.minPower() <= 20 && dist >= s.minRange() && dist <= s.maxRange()),
                    "an attack to throw at " + d + " blocks even at the darkest hour");
        }
    }

    @Test
    void atNightHeUsesOnlyTheTwentyPercentMovesWhateverHisHealth() {
        for (long seed = 1; seed <= 4; seed++) {
            Map<String, Integer> used = SdsaFightSim.play(SPECS, seed, 100, t -> Sunshine.effectivePower(100, SunClock.NIGHT_FLOOR), 30000);
            for (String id : used.keySet()) assertTrue(EscanorTable.byId(id).minPower() <= 20, id + " is not a night move");
            assertTrue(used.size() >= 3, "variety even at night: " + used.keySet());
        }
    }

    @Test
    void cruelSunNeedsTheAfternoonSunAndZenithNeedsNoon() {
        for (long seed = 1; seed <= 4; seed++) {
            assertNull(SdsaFightSim.play(SPECS, seed, 100, t -> Sunshine.effectivePower(100, 0.71), 40000).get(EscanorTable.CRUEL_SUN));
            assertNotNull(SdsaFightSim.play(SPECS, seed, 100, t -> Sunshine.effectivePower(100, 0.75), 40000).get(EscanorTable.CRUEL_SUN));
            assertNull(SdsaFightSim.play(SPECS, seed, 100, t -> Sunshine.effectivePower(100, 0.89), 40000).get(EscanorTable.ZENITH));
            assertNotNull(SdsaFightSim.play(SPECS, seed, 100, t -> Sunshine.effectivePower(100, 0.95), 40000).get(EscanorTable.ZENITH));
            assertNull(SdsaFightSim.play(SPECS, seed, 80, t -> Sunshine.effectivePower(80, 1.0), 40000).get(EscanorTable.ZENITH), "health must have reached the Gates too");
        }
    }

    @Test
    void anOpeningFightAtNoonIsStillHeldBackByHisHealth() {
        for (long seed = 1; seed <= 4; seed++) {
            Map<String, Integer> used = SdsaFightSim.play(SPECS, seed, 20, t -> Sunshine.effectivePower(20, 1.0), 20000);
            for (String id : used.keySet()) assertTrue(EscanorTable.byId(id).minPower() <= 20, id);
        }
    }

    @Test
    void aWholeDayOfFightingUsesEveryMove() {
        for (long seed = 1; seed <= 3; seed++) {
            SunClock clock = new SunClock();
            clock.start(SunClock.Start.MIDNIGHT, 0, 0, 3.0);
            // the fight is long (60000 ticks), at the Gates, the sun running at triple speed from midnight
            Map<String, Integer> used = SdsaFightSim.play(SPECS, seed, 100,
                    t -> Sunshine.effectivePower(100, Sunshine.effective(clock.sunshine(0, t), true, Sunshine.GATES_FLOOR)), 60000);
            for (EscanorTable.Spec s : SPECS) assertNotNull(used.get(s.id()), s.id() + " is used over a long fight (seed " + seed + ")");
        }
    }

    @Test
    void damageNumbersAreSane() {
        assertTrue(EscanorTable.SMASH_DAMAGE > EscanorTable.SWING_DAMAGE, "the smash is the heavy blow");
        assertTrue(EscanorTable.SWING_DAMAGE > EscanorTable.WAVE_DAMAGE, "the wave is a poke from range");
        assertTrue(EscanorTable.CHARGE_DAMAGE > EscanorTable.CHARGE_RING_DAMAGE);
        assertTrue(EscanorTable.PILLAR_DAMAGE > EscanorTable.SPIN_DAMAGE, "a pillar is a single blow, a spin has three");
        assertTrue(EscanorTable.SPIN_DAMAGE * 3 > EscanorTable.SWING_DAMAGE);
        assertTrue(EscanorTable.HAZE_DAMAGE < 5);
    }
}
