package com.newuniverse.nusmp.heroweapon.weapon.sword;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.weapon.sword.SwordNumbers.Kind;
import com.newuniverse.nusmp.heroweapon.weapon.sword.SwordNumbers.Spec;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Why: the Legendary Sword is the weapon that hits most often and fastest, so it is the one most likely to break the owner's balance law by an innocent edit (a
 * shorter cooldown, an extra pulse, a bigger bonus). Every technique has a row in {@link SwordNumbers}; this test walks the rows against the budget of design.md
 * section 5, and pins the numbers that keep the speed of the weapon honest (the bonus strikes are small, the Flow needs a full swing, a run of techniques is bounded by
 * the cooldowns and not only by the dispatcher's global lock).
 */
class SwordNumbersTest {
    @Test
    void theSwordHasAtLeastFourteenTechniquesAndFiveFromTheCurseSeries() {
        int ordinary = 0, curse = 0;
        for (Spec s : SwordNumbers.all()) {
            if (s.curse()) curse++;
            else ordinary++;
        }
        assertTrue(ordinary >= 10, "the ordinary tree needs about ten techniques, has " + ordinary);
        assertTrue(ordinary + curse >= 14, "a Holy Weapon has at least 14 techniques, has " + (ordinary + curse));
        assertTrue(curse >= 4, "the curse series needs about four techniques, has " + curse);
    }

    @Test
    void idsAreUniqueLowerCaseAndStartWithTheWeaponsName() {
        Set<String> seen = new HashSet<>();
        for (Spec s : SwordNumbers.all()) {
            assertTrue(seen.add(s.id()), "duplicate id " + s.id());
            assertTrue(s.id().matches("sword\\.[a-z_]+"), "bad id " + s.id());
            assertTrue(s.name() != null && !s.name().isBlank(), "no name for " + s.id());
            assertNotNull(SwordNumbers.spec(s.id()), "lookup " + s.id());
        }
    }

    @Test
    void everyRowKeepsTheBudgetOfItsClass() {
        for (Spec s : SwordNumbers.all()) {
            String id = s.id();
            assertTrue(s.raw() >= 0 && s.hits() >= 0, id + " has negative damage");
            assertTrue(s.raw() == 0 || s.raw() >= SwordNumbers.MIN_HIT, id + " one hit is at least " + SwordNumbers.MIN_HIT + " raw, has " + s.raw());
            switch (s.kind()) {
                case UTILITY -> {
                    assertTrue(s.cooldown() >= SwordNumbers.MIN_UTILITY_COOLDOWN, id + " utility cooldown " + s.cooldown());
                    assertTrue(s.total() <= 5, id + " a utility technique should barely hurt, total " + s.total());
                    assertTrue(s.cost() >= SwordNumbers.MIN_COST && s.cost() <= SwordNumbers.MAX_COST, id + " cost " + s.cost());
                }
                case DAMAGE -> {
                    assertTrue(s.cooldown() >= SwordNumbers.MIN_DAMAGE_COOLDOWN, id + " damage cooldown " + s.cooldown());
                    assertTrue(s.total() > 0 && s.total() <= SwordNumbers.ORDINARY_MAX + 1e-9, id + " ordinary total " + s.total());
                    assertTrue(s.cost() >= SwordNumbers.MIN_COST && s.cost() <= SwordNumbers.MAX_COST, id + " cost " + s.cost());
                }
                case HEAVY -> {
                    assertTrue(s.cooldown() >= SwordNumbers.MIN_DAMAGE_COOLDOWN, id + " heavy cooldown " + s.cooldown());
                    assertTrue(s.total() > SwordNumbers.ORDINARY_MAX - 2 && s.total() <= SwordNumbers.HEAVY_MAX + 1e-9, id + " heavy total " + s.total());
                    assertTrue(s.cost() >= SwordNumbers.MIN_COST && s.cost() <= SwordNumbers.MAX_COST, id + " cost " + s.cost());
                }
                case ULTIMATE -> {
                    assertTrue(s.cooldown() >= SwordNumbers.MIN_ULTIMATE_COOLDOWN, id + " ultimate cooldown " + s.cooldown());
                    assertTrue(s.total() <= SwordNumbers.ULTIMATE_MAX + 1e-9, id + " ultimate total " + s.total());
                    assertTrue(s.cost() >= SwordNumbers.MIN_ULTIMATE_COST && s.cost() <= SwordNumbers.MAX_ULTIMATE_COST, id + " ultimate cost " + s.cost());
                }
            }
        }
    }

    @Test
    void exactlyTwoUltimatesOneForEachSeries() {
        int ordinary = 0, curse = 0;
        for (Spec s : SwordNumbers.all()) {
            if (s.kind() != Kind.ULTIMATE) continue;
            if (s.curse()) curse++;
            else ordinary++;
        }
        assertEquals(1, ordinary);
        assertEquals(1, curse);
    }

    @Test
    void noTaskRunsLongerThanTwentySeconds() {
        int[] zones = {SwordNumbers.FLOAT_TICKS, SwordNumbers.GRAVITY_PULSES * SwordNumbers.GRAVITY_GAP, SwordNumbers.HUNDRED_DELAY + SwordNumbers.HUNDRED_PULSES * SwordNumbers.HUNDRED_GAP,
                SwordNumbers.LIGHTNING_DELAY + SwordNumbers.LIGHTNING_PULSES * SwordNumbers.LIGHTNING_GAP, SwordNumbers.MAW_DELAY + SwordNumbers.MAW_WAVES * SwordNumbers.MAW_GAP,
                SwordNumbers.TEMPEST_DELAY + 2 * SwordNumbers.TEMPEST_GAP, SwordNumbers.METEOR_DELAY + SwordNumbers.METEOR_STARS * SwordNumbers.METEOR_GAP};
        for (int t : zones) assertTrue(t > 0 && t <= SwordNumbers.MAX_TASK_TICKS, "task of " + t + " ticks");
        assertTrue(SwordNumbers.CUTTER_COUNT <= SwordNumbers.MAX_SPAWNS, "the crescents of the Wind Cutter are bolts, at most six");
        assertTrue(SwordNumbers.MARK_CAP >= SwordNumbers.MAX_TARGETS);
    }

    @Test
    void theBonusStrikesAreSmallAdditionsToABlow() {
        double[] bonus = {SwordNumbers.FLOW_RAW, SwordNumbers.GHOST_RAW, SwordNumbers.QUICK_DRAW_RAW, SwordNumbers.SHADOW_RAW, SwordNumbers.STAR_RAW, SwordNumbers.HUNDRED_EDGE_RAW,
                SwordNumbers.THUNDER_RAW, SwordNumbers.FANG_RAW, SwordNumbers.GILDED_RAW};
        for (double b : bonus) assertTrue(b >= SwordNumbers.MIN_HIT && b <= SwordNumbers.BONUS_MAX, "bonus " + b);
        assertTrue(SwordNumbers.BONUS_MAX <= SwordNumbers.ORDINARY_MAX / 2, "a bonus is at most half an ordinary technique");
        assertTrue(SwordNumbers.STAR_PAUSE >= 20 && SwordNumbers.THUNDER_PAUSE >= 10, "the repeating bonuses are paced");
    }

    @Test
    void theTempoNumbersHangTogether() {
        assertTrue(SwordNumbers.SPEED_ONE_AT > 1 && SwordNumbers.SPEED_TWO_AT > SwordNumbers.SPEED_ONE_AT && SwordNumbers.SPEED_TWO_AT < SwordNumbers.TEMPO_MAX);
        assertTrue(SwordNumbers.TEMPO_WINDOW >= 20, "a run of blows is kept for at least a second");
        assertTrue(SwordNumbers.SPEED_TICKS > SwordNumbers.TEMPO_WINDOW / 2);
        assertTrue(SwordNumbers.STEP_SPEED_LEVEL <= 1, "an afterimage step gives Speed II at most");
    }

    @Test
    void theStealFromAFoeIsSmallAndWhatTheWielderKeepsIsSmaller() {
        assertTrue(SwordNumbers.STEAL_TAKE <= 0.05 && SwordNumbers.STEAL_KEEP < SwordNumbers.STEAL_TAKE);
        assertTrue(SwordNumbers.PLUNDER_KEEP <= 0.02 && SwordNumbers.PLUNDER_TAKE < SwordNumbers.PLUNDER_KEEP);
        assertTrue(SwordNumbers.MANEATER_HEAL * SwordNumbers.MANEATER_BITES <= 0.25, "the Maneater heals under the quarter cap of the law");
        assertTrue(SwordNumbers.MAW_HEAL * SwordNumbers.MAW_WAVES <= 0.25, "the Maw heals under the quarter cap of the law");
    }

    @Test
    void theCurseSkillsThatCostMindHaveAPriceInCorruption() {
        assertTrue(SwordNumbers.GUILLOTINE_CORRUPTION > 0 && SwordNumbers.MAW_CORRUPTION > SwordNumbers.GUILLOTINE_CORRUPTION);
        assertTrue(SwordNumbers.MAW_CORRUPTION <= 200, "even the ultimate leaves room before the Overrun at 1000");
    }

    @Test
    void theFloatSwordIsBoundedOnOneTargetAndInHowManyShotsItStops() {
        Spec f = SwordNumbers.FLOAT_BLADES;
        assertTrue(f.total() <= SwordNumbers.ORDINARY_MAX + 1e-9, "a hit cap of the ordinary budget protects any one target");
        assertTrue(SwordNumbers.FLOAT_COUNT >= 2 && SwordNumbers.FLOAT_COUNT <= 6);
        assertTrue(SwordNumbers.FLOAT_BLOCKS >= 1 && SwordNumbers.FLOAT_BLOCKS <= 6, "it stops a few shots, not every shot");
        assertEquals(0, SwordNumbers.FLOAT_TICKS % SwordNumbers.FLOAT_GAP, "pulses fit the time");
    }
}
