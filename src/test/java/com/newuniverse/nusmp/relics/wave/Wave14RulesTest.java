package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.wave.Wave14Rules.Breath;
import com.newuniverse.nusmp.relics.wave.Wave14Rules.Habitat;
import com.newuniverse.nusmp.relics.wave.Wave14Rules.HighGround;
import com.newuniverse.nusmp.relics.wave.Wave14Rules.Mark;
import com.newuniverse.nusmp.relics.wave.Wave14Rules.Pool;
import com.newuniverse.nusmp.relics.wave.Wave14Rules.Spell;
import com.newuniverse.nusmp.relics.wave.Wave14Rules.Zone;
import com.newuniverse.nusmp.relics.wave.Wave14Rules.ZoneBook;
import com.newuniverse.nusmp.relics.wave.Wave14Rules.ZoneKind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.IntUnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

/** Wave 14: the pure rules of the head and footwear relics. */
class Wave14RulesTest {
    /** A "die" that always shows the highest face. */
    private static final IntUnaryOperator HIGH = bound -> bound - 1;
    private static final IntUnaryOperator LOW = bound -> 0;

    @Test
    void daysTurnOverAtDawnOrAtMidnight() {
        assertEquals(0, Wave14Rules.dayOf(23999));
        assertEquals(1, Wave14Rules.dayOf(24000));
        assertEquals(0, Wave14Rules.midnightDayOf(17999));
        assertEquals(1, Wave14Rules.midnightDayOf(18000), "midnight is the turn");
        assertEquals(1, Wave14Rules.midnightDayOf(41999));
        assertTrue(Wave14Rules.dailyReady(0, 0), "never used");
        long used = Wave14Rules.dailyMark(5);
        assertFalse(Wave14Rules.dailyReady(used, 5));
        assertTrue(Wave14Rules.dailyReady(used, 6));
        assertFalse(Wave14Rules.dailyReady(Wave14Rules.dailyMark(0), 0), "day zero is a day too");
    }

    @Test
    void diceAreSummedAndBounded() {
        assertEquals(4, Wave14Rules.dice(LOW, 4, 8));
        assertEquals(32, Wave14Rules.dice(HIGH, 4, 8));
        assertEquals(0, Wave14Rules.dice(HIGH, 0, 8));
        assertEquals(3, Wave14Rules.regainD3(HIGH));
        assertEquals(1, Wave14Rules.regainD3(LOW));
        assertEquals(3, Wave14Rules.regainConjurer(LOW), "1d4+2 at its least");
        assertEquals(6, Wave14Rules.regainConjurer(HIGH));
        assertEquals(4, Wave14Rules.regainWind(LOW), "1d4+3 at its least");
        assertEquals(7, Wave14Rules.regainWind(HIGH));
        assertEquals(4, Wave14Rules.healingTouch(LOW), "2d8+2");
        assertEquals(18, Wave14Rules.healingTouch(HIGH));
    }

    @Test
    void breathGrowsWithTheCircletsCarriedAndTheConeIsAFrontCone() {
        assertEquals(1, Wave14Rules.circletsCombined(true, false, false));
        assertEquals(3, Wave14Rules.circletsCombined(true, true, true));
        assertEquals(8f, Wave14Rules.breathDamage(1));
        assertEquals(10f, Wave14Rules.breathDamage(2));
        assertEquals(12f, Wave14Rules.breathDamage(3));
        assertEquals(12f, Wave14Rules.breathDamage(9), "three circlets exist");
        assertEquals(8f, Wave14Rules.breathDamage(0));
        assertFalse(Wave14Rules.trinity(2));
        assertTrue(Wave14Rules.trinity(3));
        assertTrue(Wave14Rules.inCone(0, 0, 5, 0, 0, 1, Wave14Rules.BREATH_COS_HALF, Wave14Rules.BREATH_RANGE), "straight ahead");
        assertFalse(Wave14Rules.inCone(0, 0, -5, 0, 0, 1, Wave14Rules.BREATH_COS_HALF, Wave14Rules.BREATH_RANGE), "behind");
        assertFalse(Wave14Rules.inCone(5, 0, 1, 0, 0, 1, Wave14Rules.BREATH_COS_HALF, Wave14Rules.BREATH_RANGE), "beside");
        assertFalse(Wave14Rules.inCone(0, 0, 12, 0, 0, 1, Wave14Rules.BREATH_COS_HALF, Wave14Rules.BREATH_RANGE), "too far");
        assertTrue(Wave14Rules.inCone(0, 0, 0, 0, 0, 1, Wave14Rules.BREATH_COS_HALF, Wave14Rules.BREATH_RANGE), "inside you");
        assertEquals(3, Breath.values().length);
        assertTrue(Wave14Rules.BREATH_BASE * 1.5f < 20f * 0.7f, "a trinity breath is far from a one-hit kill");
    }

    @Test
    void wintersFuryIsAtHalfHealthAndHeldAgainstPlayers() {
        assertTrue(Wave14Rules.wintersFury(10f, 20f));
        assertTrue(Wave14Rules.wintersFury(3f, 20f));
        assertFalse(Wave14Rules.wintersFury(11f, 20f));
        assertFalse(Wave14Rules.wintersFury(1f, 0f));
        assertEquals(8f, Wave14Rules.furyExtra(8, false));
        assertEquals(6f, Wave14Rules.furyExtra(8, true));
        assertEquals(3f, Wave14Rules.furyExtra(3, true));
    }

    @Test
    void siegeMonsterIsBrokenByAHardBlow() {
        assertTrue(Wave14Rules.siegeBreaks(10f));
        assertFalse(Wave14Rules.siegeBreaks(9.9f));
        assertEquals(3 * 60 * 20, Wave14Rules.SIEGE_TICKS);
    }

    @Test
    void poolsStartFullAndRecoverOncePerNewDay() {
        Pool p = Pool.read(-1, 0, 3, 7);
        assertEquals(3, p.have(), "a fresh pool is full");
        assertEquals(7, p.day());
        p = p.spend(1).spend(1).spend(1);
        assertEquals(0, p.have());
        assertFalse(p.canSpend(1));
        assertEquals(0, p.at(7, 3, 3).have(), "the same day gives nothing");
        assertEquals(0, p.at(6, 3, 3).have(), "a clock set back owes nothing");
        Pool next = p.at(8, 2, 3);
        assertEquals(2, next.have());
        assertEquals(8, next.day());
        assertEquals(3, next.at(9, 5, 3).have(), "never above the maximum");
        assertEquals(1, new Pool(3, 0).spend(2).have());
        assertEquals(0, new Pool(1, 0).spend(5).have(), "never below nothing");
        assertEquals(3, Pool.read(9, 4, 3, 5).have(), "a stored count over the maximum is trimmed");
        assertTrue(new Pool(5, 0).canSpend(5));
        assertFalse(new Pool(5, 0).canSpend(0), "a cost of nothing is not a spend");
    }

    @Test
    void voidBeamHalvesOnASave() {
        assertEquals(24f, Wave14Rules.voidBeam(HIGH, false));
        assertEquals(12f, Wave14Rules.voidBeam(HIGH, true));
        assertEquals(4f, Wave14Rules.voidBeam(LOW, false));
        assertEquals(2f, Wave14Rules.voidBeam(LOW, true));
        assertTrue(Wave14Rules.voidBeam(HIGH, false) <= 20f * 1.3f, "capped on players by Fx anyway");
    }

    @Test
    void conjuringCostsAndStrength() {
        assertEquals(1, Wave14Rules.conjureCost(0, false));
        assertEquals(4, Wave14Rules.conjureCost(3, false));
        assertEquals(3, Wave14Rules.conjureCost(0, true), "two more to skip the circle");
        assertEquals(6, Wave14Rules.conjureCost(3, true), "the full strength, quick, is the six charges");
        assertEquals(6, Wave14Rules.CONJURER_CHARGES);
        assertEquals(1, Wave14Rules.conjurePower(0));
        assertEquals(4, Wave14Rules.conjurePower(3));
        assertEquals(4, Wave14Rules.conjurePower(30), "clamped");
        assertTrue(Wave14Rules.conjureLife(4) > Wave14Rules.conjureLife(1));
        assertTrue(Wave14Rules.conjureLife(4) <= 6000, "Fx.summon holds a creature to five minutes");
        assertEquals(1500, Wave14Rules.conjureLife(1));
    }

    @Test
    void ducksComeAtDawnOnAFiftyFiftyAndAssistInFives() {
        assertFalse(Wave14Rules.ducksCome(49));
        assertTrue(Wave14Rules.ducksCome(50));
        assertTrue(Wave14Rules.ducksCome(99));
        assertEquals(4, Habitat.NATURAL.dice);
        assertEquals(2, Habitat.UNLIKELY.dice);
        assertEquals(1, Habitat.HIGHLY_UNLIKELY.dice);
        assertEquals(32, Wave14Rules.flockSize(Habitat.NATURAL, HIGH));
        assertEquals(8, Wave14Rules.flockSize(Habitat.HIGHLY_UNLIKELY, HIGH));
        assertEquals(2, Wave14Rules.flockSize(Habitat.UNLIKELY, LOW));
        assertEquals(0, Wave14Rules.assistsLeft(4));
        assertEquals(1, Wave14Rules.assistsLeft(5));
        assertEquals(6, Wave14Rules.assistsLeft(32));
        assertEquals(0, Wave14Rules.flockAfterAssist(3));
        assertEquals(7, Wave14Rules.flockAfterAssist(12));
        assertFalse(Wave14Rules.followOver(0));
        assertFalse(Wave14Rules.followOver(17999));
        assertTrue(Wave14Rules.followOver(18000), "midnight");
        assertTrue(Wave14Rules.followOver(23999));
        assertFalse(Wave14Rules.followOver(24000), "the next dawn");
    }

    @Test
    void theRubyBootsKnowFourPlacesAndWalkTheCircle() {
        List<Mark> marks = new ArrayList<>();
        for (int i = 0; i < 6; i++) marks = Wave14Rules.addMark(marks, new Mark("minecraft:overworld", i, 64, 0));
        assertEquals(Wave14Rules.MAX_MARKS, marks.size());
        assertEquals(2, marks.get(0).x(), "the two oldest are forgotten");
        marks = Wave14Rules.addMark(marks, new Mark("minecraft:overworld", 3, 64, 0));
        assertEquals(4, marks.size(), "a repeat is not a new place");
        assertEquals(3, marks.get(marks.size() - 1).x(), "but it becomes the newest");
        assertEquals(0, Wave14Rules.nextPlace(0, 0), "no marks: only home");
        assertEquals(1, Wave14Rules.nextPlace(0, 2));
        assertEquals(2, Wave14Rules.nextPlace(1, 2));
        assertEquals(0, Wave14Rules.nextPlace(2, 2), "back home");
        assertTrue(Wave14Rules.nextPlace(7, 2) <= 2, "a stale index after forgetting places is brought back into the circle");
    }

    @Test
    void indomitableFallIsOncePerDayAndFromSixBlocks() {
        assertTrue(Wave14Rules.indomitableFall(6.0, true));
        assertFalse(Wave14Rules.indomitableFall(5.9, true));
        assertFalse(Wave14Rules.indomitableFall(20, false));
        assertEquals(0f, Wave14Rules.fallDamage(2));
        assertEquals(7f, Wave14Rules.fallDamage(10));
        assertTrue(Wave14Rules.fallSaved(bound -> 49));
        assertFalse(Wave14Rules.fallSaved(bound -> 50));
    }

    @Test
    void theHighGroundEndsAfterAMinuteOrWhenSteppingOff() {
        HighGround hg = new HighGround(1200, 70.0);
        assertFalse(hg.over(10, 70.0));
        assertFalse(hg.over(10, 71.5), "up is fine");
        assertFalse(hg.over(10, 68.6), "a step down is still the high ground");
        assertTrue(hg.over(10, 68.4), "more than a step down: off it");
        assertTrue(hg.over(1200, 70.0), "the minute is over");
        assertTrue(Wave14Rules.reactionReady(0, 20));
        assertFalse(Wave14Rules.reactionReady(10, 20));
        assertTrue(Wave14Rules.HIGH_ADVANTAGE <= 1.5f);
    }

    @Test
    void crashesNeverKill() {
        assertEquals(4f, Wave14Rules.crashDamage(20f));
        assertEquals(2f, Wave14Rules.crashDamage(3f));
        assertEquals(0f, Wave14Rules.crashDamage(1f), "one heart left: no damage");
        assertEquals(0f, Wave14Rules.crashDamage(0.5f));
        assertTrue(Wave14Rules.crashes(bound -> 39));
        assertFalse(Wave14Rules.crashes(bound -> 40));
        assertTrue(Wave14Rules.FIREBALL_DAMAGE <= 20f * 0.3f * 2, "a fireball is not a one-hit kill");
    }

    @Test
    void zonesAreBoundedExpireAndOnlyDevilsMove() {
        ZoneBook book = new ZoneBook();
        UUID o = UUID.randomUUID();
        for (int i = 0; i < 40; i++) book.add(new Zone(ZoneKind.GREASE, "minecraft:overworld", i, 64, 0, 0, 0, 1.8, 100 + i, o));
        assertEquals(Wave14Rules.MAX_ZONES, book.size(), "the oldest go");
        book.prune(120);
        assertTrue(book.snapshot().stream().allMatch(z -> z.until() > 120));
        Zone grease = new Zone(ZoneKind.GREASE, "d", 1, 2, 3, 1, 0, 1, 99, o);
        assertSame(grease, grease.moved(1.0), "mud stays");
        Zone devil = new Zone(ZoneKind.DEVIL, "d", 0, 64, 0, 2, 0, 2.5, 99, o);
        assertEquals(1.0, devil.moved(0.5).x(), 1.0e-9);
        assertEquals(64.0, devil.moved(0.5).y(), 1.0e-9);
        assertTrue(devil.live(98));
        assertFalse(devil.live(99));
        ZoneBook b2 = new ZoneBook();
        b2.add(devil);
        b2.replace(devil, devil.moved(0.5));
        assertEquals(1.0, b2.snapshot().get(0).x(), 1.0e-9);
        b2.clear();
        assertTrue(b2.isEmpty());
    }

    @Test
    void aWindWallStopsWhatCrossesIt() {
        Zone wall = new Zone(ZoneKind.WALL, "d", 0, 64, 10, 0, 1, 5.0, 999, UUID.randomUUID());   // facing +z, so the wall runs along x
        assertTrue(Wave14Rules.inWall(wall, 0, 65, 10));
        assertTrue(Wave14Rules.inWall(wall, 4.5, 65, 11.5), "along the wall, within its width");
        assertFalse(Wave14Rules.inWall(wall, 6.5, 65, 10), "past its end");
        assertFalse(Wave14Rules.inWall(wall, 0, 65, 14.5), "in front of it");
        assertFalse(Wave14Rules.inWall(wall, 0, 70, 10), "over it");
    }

    @Test
    void theWindSpellsFitInFiveCharges() {
        int most = 0;
        for (Spell s : Spell.values()) most = Math.max(most, s.cost);
        assertEquals(Wave14Rules.WIND_CHARGES, most, "Wind Walk takes every charge");
        assertEquals(0, Spell.GUST.cost, "the cantrip is free");
        assertEquals(8, Spell.values().length);
        assertEquals(4f, Wave14Rules.wardedProjectile(10f), 1.0e-6f);
    }

    @Test
    void hairColoursCycle() {
        int at = 0;
        for (int i = 0; i < Wave14Rules.HAIR.length; i++) at = Wave14Rules.nextHair(at);
        assertEquals(0, at);
        assertEquals(0, Wave14Rules.nextHair(Wave14Rules.HAIR.length - 1));
    }
}
