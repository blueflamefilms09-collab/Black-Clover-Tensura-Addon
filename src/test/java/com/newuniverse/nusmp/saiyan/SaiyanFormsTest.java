package com.newuniverse.nusmp.saiyan;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the form roster, its unlock rules, the mastery efficiency, the Kaio-ken strain and the Ultra Instinct dodges are plain data and arithmetic, so they are pinned here with no game. */
class SaiyanFormsTest {
    private static SaiyanForms.Progress progress(double bp, int zenkai, boolean goku, Set<String> flags, Map<String, Double> training, Map<String, Double> mastery) {
        return new SaiyanForms.Progress(bp, zenkai, goku, flags, training, mastery);
    }

    @Test
    void theRosterIsTheOwnersChartWithoutTheApes() {
        assertEquals(16, SaiyanForms.ALL.size(), "sixteen forms beside Base: the chart's 19 minus Base and the two apes");
        List<String> want = List.of("kaioken", "false_super_saiyan", "super_saiyan", "super_saiyan_full_power", "super_saiyan_2", "super_saiyan_3", "super_saiyan_4",
                "super_full_power_super_saiyan_4", "super_saiyan_4_limit_breaker", "super_saiyan_god", "super_saiyan_blue", "perfected_super_saiyan_blue", "super_saiyan_blue_kaioken",
                "ultra_instinct_sign", "perfected_ultra_instinct", "true_ultra_instinct");
        assertEquals(want, SaiyanForms.ALL.stream().map(SaiyanForms.Form::id).toList());
        for (SaiyanForms.Form f : SaiyanForms.ALL) {
            assertFalse(f.id().contains("ape") || f.id().contains("oozaru"), "the owner said no ape forms: " + f.id());
            assertNotNull(SaiyanForms.byId(f.id()));
            assertEquals("nusmp.saiyan.form." + f.id(), f.key());
        }
    }

    @Test
    void idsAreUniqueAndCodesRoundTrip() {
        Set<String> seen = new HashSet<>();
        for (SaiyanForms.Form f : SaiyanForms.ALL) {
            assertTrue(seen.add(f.id()), "duplicate id " + f.id());
            assertEquals(f, SaiyanForms.ofCode(SaiyanForms.code(f.id())));
        }
        assertEquals(0, SaiyanForms.code(SaiyanForms.BASE));
        assertEquals(0, SaiyanForms.code("nope"));
        assertNull(SaiyanForms.ofCode(0));
        assertNull(SaiyanForms.ofCode(99));
        assertNull(SaiyanForms.byId(null));
    }

    @Test
    void everyParentExistsAndComesBeforeItsChild() {
        for (int i = 0; i < SaiyanForms.ALL.size(); i++) {
            SaiyanForms.Form f = SaiyanForms.ALL.get(i);
            for (String parent : new String[] {f.parent(), f.parent2()}) {
                if (parent == null) continue;
                SaiyanForms.Form p = SaiyanForms.byId(parent);
                assertNotNull(p, f.id() + " names a missing parent " + parent);
                assertTrue(SaiyanForms.ALL.indexOf(p) < i, f.id() + " comes before its parent " + parent);
            }
        }
    }

    @Test
    void theSpecTableValuesStandWhereTheSpecGivesThem() {
        assertEquals(0.75, SaiyanForms.SUPER_SAIYAN.damage(), 1e-9);
        assertEquals(0.35, SaiyanForms.SUPER_SAIYAN.defense(), 1e-9);
        assertEquals(1.10, SaiyanForms.SUPER_SAIYAN_2.damage(), 1e-9);
        assertEquals(1.60, SaiyanForms.SUPER_SAIYAN_3.damage(), 1e-9);
        assertEquals(1.80, SaiyanForms.SUPER_SAIYAN_4.damage(), 1e-9);
        assertEquals(1.90, SaiyanForms.SUPER_SAIYAN_GOD.damage(), 1e-9);
        assertEquals(2.30, SaiyanForms.SUPER_SAIYAN_BLUE.damage(), 1e-9);
        assertEquals(2.40, SaiyanForms.PERFECTED_ULTRA_INSTINCT.damage(), 1e-9);
        assertEquals(2.60, SaiyanForms.TRUE_ULTRA_INSTINCT.damage(), 1e-9);
        assertEquals(1.05, SaiyanForms.TRUE_ULTRA_INSTINCT.defense(), 1e-9);
        assertEquals(0.40, SaiyanForms.KAIOKEN.damage(), 1e-9);
        assertTrue(SaiyanForms.SUPER_SAIYAN_3.speed() < 0 && SaiyanForms.SUPER_SAIYAN_3.attackSpeed() < 0, "Super Saiyan 3 trades mobility and attack speed for strength");
        assertTrue(SaiyanForms.SUPER_SAIYAN_FULL_POWER.drain() < SaiyanForms.SUPER_SAIYAN.drain(), "Full Power wastes less");
        assertTrue(SaiyanForms.PERFECTED_SUPER_SAIYAN_BLUE.drain() < SaiyanForms.SUPER_SAIYAN_BLUE.drain(), "Perfected Blue is more efficient");
    }

    @Test
    void everyFormHasSaneNumbers() {
        for (SaiyanForms.Form f : SaiyanForms.ALL) {
            assertTrue(f.kiCost() > 0 && f.drain() > 0 && f.staminaDrain() >= 0, f.id());
            assertTrue(f.damage() > 0 && f.defense() >= 0 && f.defense() < 2.0, f.id());
            assertTrue(f.speed() > -1.0 && f.speed() < 1.0 && f.attackSpeed() > -1.0 && f.attackSpeed() < 1.0, f.id());
            assertTrue(f.cooldown() >= 0 && f.masterySeconds() > 0 && f.minBp() >= SaiyanRules.START_BP, f.id());
            assertTrue(f.minBp() <= SaiyanRules.BP_HARD_CAP, f.id());
        }
    }

    @Test
    void theHigherBranchesNeedTheGokuUltimate() {
        for (SaiyanForms.Form f : SaiyanForms.ALL) {
            boolean high = f.branch() == SaiyanForms.Branch.PRIMAL || f.branch() == SaiyanForms.Branch.GOD || f.branch() == SaiyanForms.Branch.INSTINCT;
            assertEquals(high, f.goku(), f.id());
        }
        SaiyanForms.Progress none = progress(1.0e12, 99, false, Set.of("limit_breaker"), Map.of("physical", 1e9, "ki", 1e9, "martial", 1e9, "divine", 1e9), allMastered());
        assertTrue(SaiyanForms.missing(SaiyanForms.SUPER_SAIYAN_4, SaiyanForms.SUPER_SAIYAN_4.minBp(), none).contains("the Son Goku Ultimate Skill"));
        assertTrue(SaiyanForms.meets(SaiyanForms.SUPER_SAIYAN, SaiyanForms.SUPER_SAIYAN.minBp(), none), "the first branches need no Ultimate");
        SaiyanForms.Progress goku = progress(1.0e12, 99, true, Set.of("limit_breaker"), Map.of("physical", 1e9, "ki", 1e9, "martial", 1e9, "divine", 1e9), allMastered());
        for (SaiyanForms.Form f : SaiyanForms.ALL) assertTrue(SaiyanForms.meets(f, f.minBp(), goku), f.id() + " must be reachable by a complete Saiyan");
    }

    private static Map<String, Double> allMastered() {
        Map<String, Double> m = new HashMap<>();
        for (SaiyanForms.Form f : SaiyanForms.ALL) m.put(f.id(), 1.0);
        return m;
    }

    @Test
    void superSaiyanNeedsBattlePowerABreakthroughAndTraining() {
        SaiyanForms.Form f = SaiyanForms.SUPER_SAIYAN;
        SaiyanForms.Progress p = progress(5_000, 0, false, Set.of(), Map.of(), Map.of());
        List<String> missing = SaiyanForms.missing(f, f.minBp(), p);
        assertEquals(3, missing.size());
        assertFalse(SaiyanForms.meets(f, f.minBp(), p));
        SaiyanForms.Progress ready = progress(25_000, 1, false, Set.of(), Map.of("physical", 60.0), Map.of());
        assertTrue(SaiyanForms.meets(f, f.minBp(), ready));
    }

    @Test
    void aChildNeedsItsParentMastered() {
        SaiyanForms.Form f = SaiyanForms.SUPER_SAIYAN_2;
        Map<String, Double> training = Map.of("ki", 500.0);
        assertFalse(SaiyanForms.meets(f, f.minBp(), progress(1e6, 5, false, Set.of(), training, Map.of("super_saiyan", 0.3))));
        assertTrue(SaiyanForms.meets(f, f.minBp(), progress(1e6, 5, false, Set.of(), training, Map.of("super_saiyan", 0.5))));
        SaiyanForms.Form bk = SaiyanForms.SUPER_SAIYAN_BLUE_KAIOKEN;
        Map<String, Double> t = Map.of("martial", 9999.0);
        assertFalse(SaiyanForms.meets(bk, bk.minBp(), progress(1e8, 9, true, Set.of(), t, Map.of("super_saiyan_blue", 0.9))), "needs Kaio-ken too");
        assertTrue(SaiyanForms.meets(bk, bk.minBp(), progress(1e8, 9, true, Set.of(), t, Map.of("super_saiyan_blue", 0.9, "kaioken", 0.9))));
    }

    @Test
    void theLimitBreakerNeedsItsOwnUnlock() {
        SaiyanForms.Form f = SaiyanForms.SUPER_SAIYAN_4_LIMIT_BREAKER;
        Map<String, Double> m = Map.of("super_full_power_super_saiyan_4", 1.0);
        assertFalse(SaiyanForms.meets(f, f.minBp(), progress(1e9, 9, true, Set.of(), Map.of(), m)));
        assertTrue(SaiyanForms.meets(f, f.minBp(), progress(1e9, 9, true, Set.of("limit_breaker"), Map.of(), m)));
    }

    @Test
    void theServersBattlePowerSettingReplacesTheDefault() {
        SaiyanForms.Form f = SaiyanForms.KAIOKEN;
        SaiyanForms.Progress p = progress(200, 0, false, Set.of(), Map.of("martial", 100.0), Map.of());
        assertFalse(SaiyanForms.meets(f, f.minBp(), p));
        assertTrue(SaiyanForms.meets(f, 100.0, p));
    }

    @Test
    void masteryMakesAFormMoreEfficientAndStronger() {
        assertEquals(0.85, SaiyanForms.effect(0.0), 1e-9);
        assertEquals(1.0, SaiyanForms.effect(1.0), 1e-9);
        assertEquals(1.0, SaiyanForms.effect(9.0), 1e-9);
        assertEquals(0.85, SaiyanForms.effect(Double.NaN), 1e-9);
        assertEquals(1.0, SaiyanForms.drainFactor(0.0), 1e-9);
        assertEquals(0.6, SaiyanForms.drainFactor(1.0), 1e-9);
        assertEquals(0.5, SaiyanForms.masteryGain(600, 1200), 1e-9);
        assertEquals(1.0, SaiyanForms.masteryGain(5, 0), 1e-9);
    }

    @Test
    void kaiokenStagesComeFromMasteryAndStrainFillsAndEmpties() {
        assertEquals(1, SaiyanForms.kaioMaxStage(0.0));
        assertEquals(2, SaiyanForms.kaioMaxStage(0.34));
        assertEquals(3, SaiyanForms.kaioMaxStage(0.67));
        assertEquals(3, SaiyanForms.kaioMaxStage(5.0));
        assertTrue(SaiyanForms.kaioDamageMul(3) > SaiyanForms.kaioDamageMul(1));
        assertTrue(SaiyanForms.kaioDrainMul(3) > SaiyanForms.kaioDrainMul(1));
        assertEquals(1.0, SaiyanForms.kaioDamageMul(-4), 1e-9, "a bad stage is stage one");
        assertEquals(100.0, SaiyanForms.strainAfter(50.0, true, 3, 100.0), 1e-9, "strain is capped");
        assertEquals(51.0, SaiyanForms.strainAfter(50.0, true, 1, 1.0), 1e-9);
        assertEquals(48.0, SaiyanForms.strainAfter(50.0, false, 1, 1.0), 1e-9);
        assertEquals(0.0, SaiyanForms.strainAfter(1.0, false, 1, 10.0), 1e-9);
        assertEquals(0.4, SaiyanForms.strainPenalty(100.0), 1e-9);
        assertEquals(0.0, SaiyanForms.strainPenalty(-5.0), 1e-9);
    }

    @Test
    void ultraInstinctDodgesAreLimitedAndOnlyForTheInstinctForms() {
        assertNull(SaiyanForms.dodge("super_saiyan", 1.0));
        assertNull(SaiyanForms.dodge(null, 1.0));
        assertEquals(1, SaiyanForms.uiLevel(0.0));
        assertEquals(2, SaiyanForms.uiLevel(0.4));
        assertEquals(3, SaiyanForms.uiLevel(0.7));
        SaiyanForms.Dodge s1 = SaiyanForms.dodge("ultra_instinct_sign", 0.0), s3 = SaiyanForms.dodge("ultra_instinct_sign", 1.0);
        assertTrue(s3.chance() > s1.chance(), "each awakening dodges more");
        assertEquals(0.0, s1.counter(), 1e-9, "the first awakening opens no counter window");
        assertTrue(s3.counter() > 0.0);
        for (String id : new String[] {"ultra_instinct_sign", "perfected_ultra_instinct", "true_ultra_instinct"}) {
            SaiyanForms.Dodge d = SaiyanForms.dodge(id, 1.0);
            assertNotNull(d, id);
            assertTrue(d.chance() <= SaiyanCombatRules.HARD_DODGE_CAP, id + " must stay under the hard cap");
            assertTrue(d.cooldown() > 0 && d.stamina() > 0 && d.perWindow() > 0 && d.window() > 0, id + " must have limits, a cooldown and a stamina cost");
        }
        assertTrue(SaiyanForms.dodge("true_ultra_instinct", 1.0).flowPerStack() > 0.0, "True Ultra Instinct builds a rhythm");
        assertTrue(SaiyanCombatRules.capped(SaiyanForms.dodge("true_ultra_instinct", 1.0).chance(), 5 * SaiyanForms.dodge("true_ultra_instinct", 1.0).flowPerStack()) <= SaiyanCombatRules.HARD_DODGE_CAP);
    }

    @Test
    void normalisingACommandArgument() {
        assertEquals("super_saiyan_2", SaiyanForms.norm(" Super Saiyan-2 "));
        assertEquals("", SaiyanForms.norm(null));
        assertEquals(16, SaiyanForms.byIdMap().size());
    }
}
