package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Wave 20 of the Relics of Many Worlds, part 4: the pure rules of the twelve weapons. */
class Wave20RulesTest {
    private static Wave20Rules.Bag bag() {
        Map<String, Long> m = new HashMap<>();
        return new Wave20Rules.Bag() {
            @Override public long get(String key) { return m.getOrDefault(key, 0L); }
            @Override public void put(String key, long value) { m.put(key, value); }
        };
    }

    @Test
    void theElementCyclesThroughAllFourAndComesBack() {
        Wave20Rules.Element e = Wave20Rules.Element.FIRE;
        for (int i = 0; i < 4; i++) e = e.next();
        assertEquals(Wave20Rules.Element.FIRE, e);
        assertEquals(Wave20Rules.Element.WIND, Wave20Rules.Element.of(3));
        assertEquals(Wave20Rules.Element.WIND, Wave20Rules.Element.of(-1), "a bad stored value still maps to an element");
    }

    @Test
    void aDropAttackNeedsARealFall() {
        assertTrue(Wave20Rules.isDropAttack(3.0, false));
        assertFalse(Wave20Rules.isDropAttack(3.0, true), "standing on the ground is not a drop");
        assertFalse(Wave20Rules.isDropAttack(0.5, false), "a hop is not a drop");
        assertTrue(Wave20Rules.isDraconic("ender_dragon"));
        assertTrue(Wave20Rules.isDraconic("wyvern"));
        assertFalse(Wave20Rules.isDraconic("zombie"));
    }

    @Test
    void theGrappleBurnsAtTheStartAndEveryTwoSecondsThenEnds() {
        long start = 1000;
        long next = start + Wave20Rules.GRAPPLE_STEP;
        assertFalse(Wave20Rules.grappleDamageDue(start + 10, next));
        assertTrue(Wave20Rules.grappleDamageDue(next, next));
        assertFalse(Wave20Rules.grappleOver(start + 119, start + Wave20Rules.GRAPPLE_TICKS));
        assertTrue(Wave20Rules.grappleOver(start + 120, start + Wave20Rules.GRAPPLE_TICKS));
    }

    @Test
    void theStarStepWindowIsFiveSecondsAfterTheThrow() {
        assertFalse(Wave20Rules.stepOpen(500, 0), "no throw at all");
        assertTrue(Wave20Rules.stepOpen(550, 500));
        assertTrue(Wave20Rules.stepOpen(600, 500));
        assertFalse(Wave20Rules.stepOpen(601, 500));
        assertFalse(Wave20Rules.stepOpen(400, 500), "a throw in the future is nothing");
    }

    @Test
    void carnageGrowsWithQuickKillsAndFadesAfterAGap() {
        int s = Wave20Rules.carnageAfterKill(0, 0, 100);
        assertEquals(1, s);
        s = Wave20Rules.carnageAfterKill(s, 100, 200);
        s = Wave20Rules.carnageAfterKill(s, 200, 300);
        assertEquals(3, s);
        assertEquals(3, Wave20Rules.carnageNow(3, 300, 300 + Wave20Rules.CARNAGE_WINDOW));
        assertEquals(0, Wave20Rules.carnageNow(3, 300, 300 + Wave20Rules.CARNAGE_WINDOW + 1), "the stacks fade after eight seconds");
        assertEquals(1, Wave20Rules.carnageAfterKill(4, 300, 300 + Wave20Rules.CARNAGE_WINDOW + 5), "a long gap starts again");
        for (int i = 0; i < 20; i++) s = Wave20Rules.carnageAfterKill(s, 1000L + i, 1001L + i);
        assertEquals(Wave20Rules.CARNAGE_MAX, s, "capped at five");
        assertEquals(5.0f, Wave20Rules.carnageBonus(9), 1e-6);
        assertEquals(300, Wave20Rules.bloodlustTicks(5));
        assertTrue(Wave20Rules.severs(0.04));
        assertFalse(Wave20Rules.severs(0.05));
    }

    @Test
    void deflectionWorksOnceEveryThreeSecondsAndNotAlways() {
        assertTrue(Wave20Rules.deflects(0.1, 1000, 0));
        assertFalse(Wave20Rules.deflects(0.9, 1000, 0), "the roll missed");
        assertFalse(Wave20Rules.deflects(0.1, 1030, 1000), "still on cooldown");
        assertTrue(Wave20Rules.deflects(0.1, 1060, 1000));
    }

    @Test
    void theScepterWardHealsTwoPerBlowTwentyADayAndSoulgateOnceADay() {
        Wave20Rules.Bag b = bag();
        long now = 5 * Wave20Rules.DAY + 100;
        long total = 0;
        for (int i = 0; i < 20; i++) {
            int g = Wave20Rules.wardGain(Wave20Rules.spentToday(b, "ward", now));
            total += g;
            Wave20Rules.spend(b, "ward", now, g);
        }
        assertEquals(20, total, "twenty a day, no more");
        assertEquals(0, Wave20Rules.wardGain(Wave20Rules.spentToday(b, "ward", now)));
        assertEquals(2, Wave20Rules.wardGain(Wave20Rules.spentToday(b, "ward", now + Wave20Rules.DAY)), "a new day, a fresh allowance");
        assertEquals(1, Wave20Rules.wardGain(19));
        assertTrue(Wave20Rules.soulgateSaves(5.0f, 7.0f, false));
        assertFalse(Wave20Rules.soulgateSaves(5.0f, 3.0f, false), "a blow that does not kill needs no soulgate");
        assertFalse(Wave20Rules.soulgateSaves(5.0f, 7.0f, true), "once a day");
    }

    @Test
    void theRayDistanceFindsFoesAlongTheLineAndNotBehindOrBeyond() {
        assertEquals(1.0, Wave20Rules.distToRay(5, 1, 0, 0, 0, 0, 1, 0, 0, 24), 1e-9);
        assertEquals(0.0, Wave20Rules.distToRay(10, 0, 0, 0, 0, 0, 1, 0, 0, 24), 1e-9);
        assertEquals(-1.0, Wave20Rules.distToRay(-3, 0, 0, 0, 0, 0, 1, 0, 0, 24), 1e-9, "behind the thrower");
        assertEquals(-1.0, Wave20Rules.distToRay(30, 0, 0, 0, 0, 0, 1, 0, 0, 24), 1e-9, "beyond the range");
        assertEquals(18.0f, Wave20Rules.boltDamage(12.0f, true), 1e-6);
        assertEquals(12.0f, Wave20Rules.boltDamage(12.0f, false), 1e-6);
    }

    @Test
    void theHoverStrikesOncePerTurnWhileItLastsAndOnlyAtCloseFoes() {
        long until = 5000;
        assertTrue(Wave20Rules.hoverStrikes(1000, until, 0, 2.0));
        assertFalse(Wave20Rules.hoverStrikes(1010, until, 1000, 2.0), "already struck this turn");
        assertTrue(Wave20Rules.hoverStrikes(1040, until, 1000, 2.0));
        assertFalse(Wave20Rules.hoverStrikes(1000, until, 0, 6.0), "out of reach");
        assertFalse(Wave20Rules.hoverStrikes(6000, until, 0, 2.0), "the hover has ended");
    }

    @Test
    void voidCreaturesAreTheEndsFolkAndWhateverSaysVoid() {
        assertTrue(Wave20Rules.isVoidCreature("minecraft", "enderman"));
        assertTrue(Wave20Rules.isVoidCreature("minecraft", "shulker"));
        assertTrue(Wave20Rules.isVoidCreature("mod", "void_walker"));
        assertTrue(Wave20Rules.isVoidCreature("tensura", "spatial_beast"));
        assertFalse(Wave20Rules.isVoidCreature("minecraft", "cow"));
        assertFalse(Wave20Rules.isVoidCreature("mod", "spatial_thing"), "only Tensura's spatial things count");
    }

    @Test
    void tollTheDeadDoublesOnTheHurtAndHalvesOnASave() {
        assertEquals(4.5f, Wave20Rules.tollDamage(20, 20, false), 1e-6);
        assertEquals(9.0f, Wave20Rules.tollDamage(10, 20, false), 1e-6);
        assertEquals(4.5f, Wave20Rules.tollDamage(10, 20, true), 1e-6);
        assertTrue(Wave20Rules.saveHolds(0.39));
        assertFalse(Wave20Rules.saveHolds(0.41));
        assertTrue(Wave20Rules.dying(5, 20));
        assertFalse(Wave20Rules.dying(15, 20));
    }
}
