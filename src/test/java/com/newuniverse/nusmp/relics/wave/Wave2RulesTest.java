package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Wave 2 (charms and cloaks): the pure rules. */
class Wave2RulesTest {
    @Test
    void feetBecomeBlocks() {
        assertEquals(10.0, Wave2Rules.blocks(30), 1e-9);
        assertEquals(20.0, Wave2Rules.blocks(60), 1e-9);
        assertEquals(5.0, Wave2Rules.blocks(15), 1e-9);
    }

    @Test
    void savingThrowsScaleWithSizeAndNeverReachTheEnds() {
        assertEquals(0.05, Wave2Rules.saveChance(4, false), 1e-9);
        assertEquals(0.05, Wave2Rules.saveChance(20, false), 1e-9);
        assertEquals(0.5, Wave2Rules.saveChance(100, false), 1e-9);
        assertEquals(0.6, Wave2Rules.saveChance(900, false), 1e-9);
        assertEquals(0.6, Wave2Rules.saveChance(20, true), 1e-9);
        for (double hp = 0; hp < 1000; hp += 7) {
            double c = Wave2Rules.saveChance(hp, false);
            assertTrue(c >= 0.05 && c <= 0.6, "chance " + c + " at " + hp);
        }
        assertTrue(Wave2Rules.saved(0.5, 0.49));
        assertFalse(Wave2Rules.saved(0.5, 0.5));
        assertFalse(Wave2Rules.saved(0.05, 0.99));
    }

    @Test
    void theConeHoldsWhatIsAheadAndInRange() {
        // looking along +x
        assertTrue(Wave2Rules.inCone(2, 0, 0, 1, 0, 0, 3.5, 0.5));
        assertFalse(Wave2Rules.inCone(4, 0, 0, 1, 0, 0, 3.5, 0.5), "too far");
        assertFalse(Wave2Rules.inCone(-2, 0, 0, 1, 0, 0, 3.5, 0.5), "behind");
        assertFalse(Wave2Rules.inCone(0, 0, 2, 1, 0, 0, 3.5, 0.5), "beside");
        assertTrue(Wave2Rules.inCone(2, 0, 1, 1, 0, 0, 3.5, 0.5), "26 degrees off the axis");
        assertFalse(Wave2Rules.inCone(1, 0, 2, 1, 0, 0, 3.5, 0.5), "63 degrees off the axis");
        assertTrue(Wave2Rules.inCone(0, 0, 0, 1, 0, 0, 3.5, 0.5), "on top of the caster");
        assertFalse(Wave2Rules.inCone(1, 0, 0, 0, 0, 0, 3.5, 0.5), "no look direction");
    }

    @Test
    void sandBlindsForARoundAndHalfThatForABoss() {
        assertEquals(60, Wave2Rules.sandTicks(false));
        assertEquals(30, Wave2Rules.sandTicks(true));
        assertTrue(Wave2Rules.eyeless("minecraft:warden"));
        assertFalse(Wave2Rules.eyeless("minecraft:zombie"));
        assertFalse(Wave2Rules.eyeless(null));
    }

    @Test
    void evilIsReadFromTheNameInWholeWords() {
        for (String p : new String[]{"lesser_daemon", "greater_daemon", "arch_daemon", "zombie", "skeleton", "hell_moth", "wither_skeleton", "lich", "dark_devil"})
            assertTrue(Wave2Rules.evilPath(p), p);
        for (String p : new String[]{"cow", "slime", "imperial_guard", "goblin", "hound_dog", "villager", "shimp", "", "tensura_player"})
            assertFalse(Wave2Rules.evilPath(p), p);
        assertFalse(Wave2Rules.evilPath(null));
    }

    @Test
    void tentaclesGrowEveryTenLevels() {
        assertEquals(1, Wave2Rules.tentacles(0));
        assertEquals(1, Wave2Rules.tentacles(9));
        assertEquals(2, Wave2Rules.tentacles(10));
        assertEquals(3, Wave2Rules.tentacles(25));
        assertEquals(4, Wave2Rules.tentacles(30));
        assertEquals(4, Wave2Rules.tentacles(500));
    }

    @Test
    void theShriekStaysPvpSafe() {
        assertEquals(10.0, Wave2Rules.shriekDamage(false, false), 1e-9);
        assertEquals(5.0, Wave2Rules.shriekDamage(false, true), 1e-9);
        assertEquals(6.0, Wave2Rules.shriekDamage(true, false), 1e-9, "30% of a 20 HP player");
        assertEquals(3.0, Wave2Rules.shriekDamage(true, true), 1e-9);
        assertTrue(Wave2Rules.wingDashBlocks(true) > Wave2Rules.wingDashBlocks(false));
    }

    @Test
    void oneMonsterInFiveLooksAwayFromTheMothRider() {
        int n = 0;
        for (int h = -500; h < 500; h++) if (Wave2Rules.intimidated(h)) n++;
        assertEquals(200, n);
        assertEquals(Wave2Rules.intimidated(12345), Wave2Rules.intimidated(12345), "stable per monster");
    }

    @Test
    void dichotomySortsBlows() {
        assertEquals(Wave2Rules.Hit.MELEE, Wave2Rules.classify("minecraft:player_attack", true));
        assertEquals(Wave2Rules.Hit.OTHER, Wave2Rules.classify("minecraft:player_attack", false), "a thrown or indirect blow is no smite");
        assertEquals(Wave2Rules.Hit.SPELL, Wave2Rules.classify("minecraft:magic", false));
        assertEquals(Wave2Rules.Hit.SPELL, Wave2Rules.classify("minecraft:indirect_magic", false));
        assertEquals(Wave2Rules.Hit.SPELL, Wave2Rules.classify("tensura:fire_ball", false));
        assertEquals(Wave2Rules.Hit.SPELL, Wave2Rules.classify("nusmp:grimoire", false));
        assertEquals(Wave2Rules.Hit.OTHER, Wave2Rules.classify("minecraft:arrow", false));
        assertEquals(Wave2Rules.Hit.OTHER, Wave2Rules.classify("minecraft:fall", false));
        assertEquals(Wave2Rules.Hit.OTHER, Wave2Rules.classify(null, true));
    }

    @Test
    void theSmiteBonusIsOneDieEightAndMoreAgainstEvil() {
        assertEquals(0.6, Wave2Rules.smiteBonus(1, false), 1e-9);
        assertEquals(4.8, Wave2Rules.smiteBonus(8, false), 1e-9);
        assertEquals(7.2, Wave2Rules.smiteBonus(8, true), 1e-9);
        assertEquals(0.6, Wave2Rules.smiteBonus(-3, false), 1e-9, "clamped");
        assertEquals(4.8, Wave2Rules.smiteBonus(99, false), 1e-9, "clamped");
        assertEquals(1.2, Wave2Rules.SPELL_BOOST, 1e-9);
    }

    @Test
    void theHungryCloakTakesLessThanAllYouHave() {
        Wave2Rules.Tax full = Wave2Rules.hungerTax(20);
        assertEquals(4, full.food());
        assertEquals(0f, full.health());
        assertFalse(full.exhausted());
        Wave2Rules.Tax low = Wave2Rules.hungerTax(3);
        assertEquals(3, low.food());
        assertTrue(low.exhausted());
        assertEquals(0f, low.health());
        Wave2Rules.Tax none = Wave2Rules.hungerTax(0);
        assertEquals(0, none.food());
        assertEquals(4f, none.health(), 1e-6);
        assertTrue(none.exhausted());
        for (int f = 0; f <= 20; f++) assertTrue(Wave2Rules.hungerTax(f).food() <= f, "never takes more food than there is");
    }

    @Test
    void acidIsFoundInTheDamageTypeName() {
        assertTrue(Wave2Rules.acid("acid_splash"));
        assertTrue(Wave2Rules.acid("corrosion"));
        assertFalse(Wave2Rules.acid("fire_ball"));
        assertFalse(Wave2Rules.acid(null));
    }

    @Test
    void stylesCycleAround() {
        assertEquals(1, Wave2Rules.nextStyle(0, 6));
        assertEquals(0, Wave2Rules.nextStyle(5, 6));
        assertEquals(0, Wave2Rules.nextStyle(3, 0));
        assertEquals(0, Wave2Rules.nextStyle(-1, 6));
    }
}
