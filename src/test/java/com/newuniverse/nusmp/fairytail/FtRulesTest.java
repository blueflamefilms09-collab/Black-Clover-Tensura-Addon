package com.newuniverse.nusmp.fairytail;

import com.newuniverse.nusmp.skill.rules.Tier;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the Fairy Tail skills are driven by tables and a few pure rules (which spells a mastery opens, what they cost and recover in, how
 * a form and eaten flame scale the damage, when the Fire Dragon Slayer may evolve). This states those rules so a change to a table or a
 * formula that breaks one fails here, without a game.
 */
class FtRulesTest {
    @Test
    void everyLineHasSixToTwelveSpellsWithUniqueIds() {
        for (FtLine line : FtLine.values()) {
            assertTrue(line.modes() >= 6 && line.modes() <= 12, line.id + " has " + line.modes() + " spells");
            Set<String> ids = new HashSet<>();
            for (FtMove mv : line.moves()) assertTrue(ids.add(mv.id()), line.id + " repeats " + mv.id());
        }
    }

    @Test
    void theFirstSpellOfEveryLineIsOpenAtZeroMastery() {
        for (FtLine line : FtLine.values()) assertTrue(FtRules.unlocked(line.move(0), 0.0), line.id);
    }

    @Test
    void masteryOpensSpellsInSteps() {
        FtLine fire = FtLine.FIRE_DRAGON_SLAYER;
        assertTrue(FtRules.openCount(fire, 0.0) < FtRules.openCount(fire, 0.5));
        assertEquals(fire.modes(), FtRules.openCount(fire, 1.0));
        assertFalse(FtRules.unlocked(fire.move(6), 0.69));       // Crimson Lotus waits for 70 percent
        assertTrue(FtRules.unlocked(fire.move(6), 0.70));
        assertFalse(FtRules.unlocked(null, 1.0));
    }

    @Test
    void cyclingModesSkipsTheClosedOnes() {
        FtLine fire = FtLine.FIRE_DRAGON_SLAYER;
        int next = FtRules.nextOpen(fire, 0.0, 2, false);
        assertTrue(FtRules.unlocked(fire.move(next), 0.0));
        assertEquals(0, FtRules.nextOpen(FtLine.CRASH, 0.0, 0, false));                  // only one spell open: it stays
        assertEquals(7, FtRules.nextOpen(fire, 0.0, 2, false));                          // Eat Flame is the next open one
        int back = FtRules.nextOpen(fire, 1.0, 0, true);
        assertEquals(fire.modes() - 1, back);
    }

    @Test
    void theKingRepeatsTheSlayersSpellsInOrderAndAddsTheThreeForms() {
        FtLine base = FtLine.FIRE_DRAGON_SLAYER, king = FtLine.FIRE_DRAGON_KING;
        for (int i = 0; i < base.modes(); i++) assertEquals(base.move(i).id(), king.move(i).id());
        assertEquals(base.modes() + 3, king.modes());
        assertEquals("dragon_force", king.move(base.modes()).id());
        assertEquals("lightning_flame_dragon_mode", king.move(base.modes() + 1).id());
        assertEquals("fire_dragon_kings_mode", king.move(base.modes() + 2).id());
        assertEquals(FtLine.FIRE_DRAGON_KING, base.evolvesInto());
        assertNull(king.evolvesInto());
        assertTrue(king.ultimate);
        assertFalse(base.ultimate);
    }

    @Test
    void formsAreOrderedByPowerAndEveryFormMoveNamesAValidStyle() {
        assertTrue(FtForms.get(FtForms.DRAGON_FORCE).dmgMul() < FtForms.get(FtForms.LIGHTNING_FLAME).dmgMul());
        assertTrue(FtForms.get(FtForms.LIGHTNING_FLAME).dmgMul() < FtForms.get(FtForms.FIRE_KING).dmgMul());
        for (FtLine line : FtLine.values()) {
            for (FtMove mv : line.moves()) {
                if (mv.kind() == FtKind.FORM) assertNotNull(FtForms.get(mv.form()), line.id + "." + mv.id());
                else assertEquals(-1, mv.form());
            }
        }
        assertNull(FtForms.get(-1));
        assertNull(FtForms.get(FtForms.ALL.size()));
        assertEquals(10, FtForms.ALL.size());
        assertEquals(FtForms.ALL.size(), new HashSet<>(FtForms.ALL.stream().map(FtForms::key).toList()).size());
    }

    @Test
    void hurtingSpellsHaveDamageAndFloorCostAppliesToBigPools() {
        for (FtLine line : FtLine.values()) {
            for (FtMove mv : line.moves()) {
                if (mv.hurts()) assertTrue(mv.damage() > 0, line.id + "." + mv.id());
                assertTrue(mv.cooldown() > 0, line.id + "." + mv.id());
            }
        }
        FtMove roar = FtLine.FIRE_DRAGON_SLAYER.move(0);
        assertEquals(FtMove.COST_FLOOR, FtRules.cost(roar, 10.0, Tier.BASE), 1e-9);          // a tiny pool still pays the floor
        assertEquals(600.0, FtRules.cost(roar, 10000.0, Tier.BASE), 1e-9);                   // 6 percent of 10,000
        assertTrue(FtRules.cost(roar, 10000.0, Tier.MASTERED) < FtRules.cost(roar, 10000.0, Tier.BASE));
        assertTrue(FtRules.cost(roar, 10000.0, Tier.ULTIMATE) > FtRules.cost(roar, 10000.0, Tier.BASE));
        assertEquals(0.0, FtRules.cost(FtLine.FIRE_DRAGON_SLAYER.move(7), 10000.0, Tier.BASE), 1e-9);   // eating costs nothing
    }

    @Test
    void masteryAndFormsShortenCooldownsButNeverBelowTenTicks() {
        FtMove m = FtLine.FIRE_DRAGON_SLAYER.move(0);
        assertEquals(100, FtRules.cooldown(m, 0.0, false));
        assertEquals(75, FtRules.cooldown(m, 1.0, false));
        assertEquals(65, FtRules.cooldown(m, 1.0, true));
        assertTrue(FtRules.cooldown(FtLine.FIRE_DRAGON_SLAYER.move(1), 1.0, true) >= 10);
    }

    @Test
    void damageGrowsWithTierFormAndCharge() {
        FtMove m = FtLine.FIRE_DRAGON_SLAYER.move(0);
        double base = FtRules.rawDamage(m, Tier.BASE, 1.0, 0);
        assertEquals(11.0, base, 1e-9);
        assertTrue(FtRules.rawDamage(m, Tier.MASTERED, 1.0, 0) > base);
        assertTrue(FtRules.rawDamage(m, Tier.ULTIMATE, 1.0, 0) > FtRules.rawDamage(m, Tier.MASTERED, 1.0, 0));
        assertEquals(base * 2.2, FtRules.rawDamage(m, Tier.BASE, 2.2, 0), 1e-9);
        assertEquals(base * 1.5, FtRules.rawDamage(m, Tier.BASE, 1.0, FtRules.MAX_CHARGE), 1e-9);
        assertEquals(base * 1.5, FtRules.rawDamage(m, Tier.BASE, 1.0, 5000), 1e-9);            // the charge is capped
    }

    @Test
    void eatenChargeStacksCapsAndDecays() {
        assertEquals(12.0, FtRules.eatCharge(0, 1), 1e-9);
        assertEquals(FtRules.MAX_CHARGE, FtRules.eatCharge(95, 8), 1e-9);
        assertEquals(0.0, FtRules.eatCharge(0, -3), 1e-9);
        assertEquals(50.0 - 2.0 * 5, FtRules.decayedCharge(50, 100), 1e-9);                    // five seconds
        assertEquals(0.0, FtRules.decayedCharge(5, 2000), 1e-9);
        assertEquals(40.0, FtRules.decayedCharge(40, -5), 1e-9);
        assertEquals(0.6, FtRules.eatRestoreFrac(50), 1e-9);
        assertEquals(0.0, FtRules.eatRestoreFrac(0), 1e-9);
    }

    @Test
    void evolutionNeedsMasteryAndTheEp() {
        assertTrue(FtRules.canEvolve(true, 50000, 10000));
        assertFalse(FtRules.canEvolve(false, 50000, 10000));
        assertFalse(FtRules.canEvolve(true, 500, 10000));
        assertTrue(FtRules.canEvolve(true, 0, 0));                                            // the pack may switch the EP gate off
    }

    @Test
    void formsLastLongerForHigherTiersAndBlowsAreTurnedAsidePartly() {
        FtMove f = FtLine.FIRE_DRAGON_KING.move(8);
        assertEquals(1200, FtRules.formTicks(f, Tier.BASE));
        assertEquals(1440, FtRules.formTicks(f, Tier.MASTERED));
        assertEquals(1680, FtRules.formTicks(f, Tier.ULTIMATE));
        assertEquals(20.0, FtRules.afterCut(100.0, 0.8), 1e-9);
        assertEquals(5.0, FtRules.afterCut(100.0, 5.0), 1e-9);                                // never more than 95 percent
        assertEquals(100.0, FtRules.afterCut(100.0, -1.0), 1e-9);
    }

    @Test
    void everyElementNamesItsThreeEffectsAndEveryLineHasAnId() {
        for (FtElement e : FtElement.values()) {
            assertNotNull(e.cast);
            assertNotNull(e.zone);
            assertNotNull(e.burst);
            assertTrue((e.argb >>> 24) == 0xFF);
        }
        assertEquals(8, FtLine.values().length);
        assertSame(FtLine.CRASH, FtLine.byId("crash"));
        assertNull(FtLine.byId("nope"));
    }
}
