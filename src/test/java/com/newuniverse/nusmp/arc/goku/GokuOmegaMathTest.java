package com.newuniverse.nusmp.arc.goku;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Goku Omega's pure rules: the strongest-boss guarantee, the level curve, autonomous training with its caps, fight experience, the hit cap, the dodge cap, adaptation, the hack order, the saved record. No game type is started. */
class GokuOmegaMathTest {
    private static final GokuOmegaMath.Sheet GOKU = new GokuOmegaMath.Sheet("goku", 30000, 40, 0.34, 20);
    private static final GokuOmegaMath.Sheet KAKASHI = new GokuOmegaMath.Sheet("kakashi", 28000, 36, 0.36, 16);
    private static final GokuOmegaMath.Sheet ZORO = new GokuOmegaMath.Sheet("zoro", 22000, 30, 0.40, 10);

    // ---------------------------------------------------------------- strongest guarantee
    @Test
    void theFloorsKeepGokuAboveTheStrongestRivalAtItsCeiling() {
        List<GokuOmegaMath.Sheet> rivals = List.of(KAKASHI, ZORO);
        double hf = GokuOmegaMath.healthFloor(GOKU, rivals), df = GokuOmegaMath.damageFloor(GOKU, rivals);
        assertTrue(GokuOmegaMath.strongest(GOKU, hf, df, rivals), "at exactly the floors he is already the strongest");
        assertFalse(GokuOmegaMath.strongest(GOKU, hf * 0.9, df, rivals), "less health than the floor fails");
        assertFalse(GokuOmegaMath.strongest(GOKU, hf, df * 0.9, rivals), "less damage than the floor fails");
        assertTrue(hf <= GokuOmegaMath.OMEGA_MAX_HEALTH_FACTOR && df <= GokuOmegaMath.OMEGA_MAX_DAMAGE_FACTOR, "the floors fit under the Omega ceilings: " + hf + " / " + df);
    }

    @Test
    void aNewStrongerBossRaisesTheFloorAndAnImpossibleOneIsCaught() {
        List<GokuOmegaMath.Sheet> now = List.of(KAKASHI);
        GokuOmegaMath.Sheet stronger = new GokuOmegaMath.Sheet("newcomer", 60000, 70, 0.4, 20);
        List<GokuOmegaMath.Sheet> later = List.of(KAKASHI, stronger);
        assertTrue(GokuOmegaMath.healthFloor(GOKU, later) > GokuOmegaMath.healthFloor(GOKU, now));
        assertTrue(GokuOmegaMath.damageFloor(GOKU, later) > GokuOmegaMath.damageFloor(GOKU, now));
        GokuOmegaMath.Sheet absurd = new GokuOmegaMath.Sheet("absurd", 5_000_000, 5000, 1, 20);
        assertTrue(GokuOmegaMath.healthFloor(GOKU, List.of(absurd)) > GokuOmegaMath.OMEGA_MAX_HEALTH_FACTOR, "a boss no factor can beat is visible to the test that guards the ceiling");
    }

    @Test
    void theOmegaFactorsNeverFallBelowTheFloorNorRiseAboveTheCeiling() {
        for (int level = 1; level <= GokuOmegaMath.HARD_MAX_LEVEL; level += 7) {
            double h = GokuOmegaMath.omegaHealth(1.0, 2.5, level, 11.0), d = GokuOmegaMath.omegaDamage(1.0, 1.5, level, 11.5);
            assertTrue(h >= 11.0 && h <= GokuOmegaMath.OMEGA_MAX_HEALTH_FACTOR);
            assertTrue(d >= 11.5 && d <= GokuOmegaMath.OMEGA_MAX_DAMAGE_FACTOR);
        }
        assertEquals(GokuOmegaMath.OMEGA_MAX_HEALTH_FACTOR, GokuOmegaMath.omegaHealth(1e9, 10, 200, 1), 1e-9);
        assertEquals(GokuOmegaMath.OMEGA_MAX_DAMAGE_FACTOR, GokuOmegaMath.omegaDamage(1e12, 10, 200, 1), 1e-9);
        assertEquals(1.0, GokuOmegaMath.omegaDamage(Double.POSITIVE_INFINITY, 10, 200, 1), 1e-9, "an infinite input counts as garbage and falls to the floor");
        assertTrue(GokuOmegaMath.omegaHealth(Double.NaN, Double.NaN, 5, Double.NaN) >= GokuBossMath.MIN_FACTOR, "garbage in, a sane factor out");
    }

    @Test
    void aHigherLevelNeverLowersHisFactors() {
        double ph = 0, pd = 0;
        for (int level = 1; level <= GokuOmegaMath.HARD_MAX_LEVEL; level++) {
            double h = GokuOmegaMath.omegaHealth(8.0, 2.5, level, 1.0), d = GokuOmegaMath.omegaDamage(8.0, 1.5, level, 1.0);
            assertTrue(h >= ph && d >= pd, "level " + level);
            ph = h;
            pd = d;
        }
    }

    // ---------------------------------------------------------------- level curve
    @Test
    void theLevelCurveRisesAndIsConsistent() {
        long prev = -1;
        for (int l = 1; l <= GokuOmegaMath.HARD_MAX_LEVEL + 1; l++) {
            long t = GokuOmegaMath.totalXpFor(l);
            assertTrue(t > prev, "total xp rises at " + l);
            prev = t;
        }
        for (int l = 1; l < GokuOmegaMath.HARD_MAX_LEVEL; l++) {
            assertEquals(GokuOmegaMath.totalXpFor(l + 1) - GokuOmegaMath.totalXpFor(l), GokuOmegaMath.xpToNext(l), "step " + l);
            assertTrue(GokuOmegaMath.xpToNext(l + 1) >= GokuOmegaMath.xpToNext(l));
            assertEquals(l, GokuOmegaMath.levelFor(GokuOmegaMath.totalXpFor(l), GokuOmegaMath.HARD_MAX_LEVEL));
            assertEquals(l, GokuOmegaMath.levelFor(GokuOmegaMath.totalXpFor(l + 1) - 1, GokuOmegaMath.HARD_MAX_LEVEL));
            assertTrue(GokuOmegaMath.levelHealthMul(l + 1) > GokuOmegaMath.levelHealthMul(l));
            assertTrue(GokuOmegaMath.levelDamageMul(l + 1) > GokuOmegaMath.levelDamageMul(l));
            assertTrue(GokuOmegaMath.levelEpMul(l + 1) > GokuOmegaMath.levelEpMul(l));
        }
        assertEquals(1.0, GokuOmegaMath.levelHealthMul(1), 1e-12);
    }

    @Test
    void theLevelCapHoldsAndTheExperienceIsBounded() {
        assertEquals(50, GokuOmegaMath.levelFor(Long.MAX_VALUE, 50));
        assertEquals(GokuOmegaMath.HARD_MAX_LEVEL, GokuOmegaMath.levelFor(Long.MAX_VALUE, 10_000));
        assertEquals(1, GokuOmegaMath.levelFor(-5, 50));
        assertEquals(1, GokuOmegaMath.levelFor(0, -3));
        assertTrue(GokuOmegaMath.xpCeiling() > 0 && GokuOmegaMath.xpCeiling() < Long.MAX_VALUE / 1000);
    }

    // ---------------------------------------------------------------- training
    @Test
    void trainingPaysTheRatePerInGameHour() {
        GokuOmegaMath.Trained t = GokuOmegaMath.training(0, 3000, 60, 1500, -1, 0);
        assertEquals(180, t.xp());
        assertEquals(180, t.usedToday());
        assertEquals(0, t.day());
    }

    @Test
    void trainingStopsAtTheDailyCapAndStartsFreshTheNextDay() {
        GokuOmegaMath.Trained t = GokuOmegaMath.training(0, 24000, 600, 1500, -1, 0);   // 14,400 uncapped
        assertEquals(1500, t.xp());
        GokuOmegaMath.Trained w = GokuOmegaMath.training(0, 72000, 600, 1500, -1, 0);   // three days
        assertEquals(4500, w.xp());
        GokuOmegaMath.Trained spent = GokuOmegaMath.training(1000, 2000, 600, 1500, 0, 1500);   // the cap of day 0 is already used up
        assertEquals(0, spent.xp());
        GokuOmegaMath.Trained part = GokuOmegaMath.training(1000, 2000, 600, 1500, 0, 1400);
        assertEquals(100, part.xp());
    }

    @Test
    void trainingIsBoundedByTheCatchUpWindowAndCannotOverflow() {
        GokuOmegaMath.Trained t = GokuOmegaMath.training(0, Long.MAX_VALUE / 4, 1_000_000, 10_000_000, -1, 0);
        assertTrue(t.xp() <= (GokuOmegaMath.MAX_CATCHUP_DAYS + 1L) * 10_000_000L, "at most the window's caps: " + t.xp());
        assertTrue(t.xp() >= 0);
        GokuOmegaMath.Trained neg = GokuOmegaMath.training(100, 50, 60, 1500, 0, 0);
        assertEquals(0, neg.xp(), "time going backwards pays nothing");
        assertEquals(0, GokuOmegaMath.training(0, 24000, 0, 1500, 0, 0).xp(), "rate 0 = training off");
        assertEquals(0, GokuOmegaMath.training(0, 24000, 60, 0, 0, 0).xp(), "cap 0 = training off");
        assertEquals(0, GokuOmegaMath.training(0, 24000, -9, -9, 0, -9).xp(), "negative settings are clamped to off");
    }

    @Test
    void aMonthOfTrainingAtTheDefaultsAddsUpAsStated() {
        GokuOmegaMath.Progress p = new GokuOmegaMath.Progress();
        long gained = 0;
        p.train(0, 60, 1500);
        for (int hour = 1; hour <= 24 * 5; hour++) gained += p.train(hour * 1000L, 60, 1500);
        assertEquals(1440 * 5, gained, "60 XP x 24 hours x 5 days, under the 1,500 daily cap");
        assertTrue(p.level(50) > GokuOmegaMath.START_LEVEL, "five idle days make him stronger");
    }

    // ---------------------------------------------------------------- fights
    @Test
    void fightExperienceByOutcomeIsBoundedAndOrdered() {
        long win = GokuOmegaMath.fightXp(GokuOmegaMath.Outcome.WIN, 3, 4), far = GokuOmegaMath.fightXp(GokuOmegaMath.Outcome.FAREWELL, 0, 4), sur = GokuOmegaMath.fightXp(GokuOmegaMath.Outcome.SURVIVED, 0, 4);
        assertEquals(GokuOmegaMath.WIN_BASE + 3 * GokuOmegaMath.WIN_PER_KILL, win);
        assertEquals(GokuOmegaMath.FAREWELL_BASE + 4 * GokuOmegaMath.FAREWELL_PER_FIGHTER, far);
        assertEquals(GokuOmegaMath.SURVIVED, sur);
        assertTrue(win > far && far > sur, "beating players teaches more than a farewell, more than nobody left");
        for (GokuOmegaMath.Outcome o : GokuOmegaMath.Outcome.values()) {
            assertTrue(GokuOmegaMath.fightXp(o, 9999, 9999) <= GokuOmegaMath.MAX_PER_FIGHT);
            assertTrue(GokuOmegaMath.fightXp(o, -5, -5) >= 0);
        }
    }

    @Test
    void theDailyFightCapStopsFarming() {
        GokuOmegaMath.Progress p = new GokuOmegaMath.Progress();
        p.rest(0);
        long total = 0;
        for (int i = 0; i < 100; i++) total += p.fight(GokuOmegaMath.Outcome.WIN, 5, 4, 100, 6000);
        assertEquals(6000, total, "a hundred fights in one day give at most the daily cap");
        long nextDay = p.fight(GokuOmegaMath.Outcome.WIN, 1, 1, 25000, 6000);
        assertTrue(nextDay > 0, "the next day he learns again");
        assertEquals(0, GokuOmegaMath.capFightXp(500, 6000, 6000));
        assertEquals(0, GokuOmegaMath.capFightXp(500, 0, 0));
    }

    @Test
    void aWholeCareerStaysUnderTheCeilingAndKeepsItsCounters() {
        GokuOmegaMath.Progress p = new GokuOmegaMath.Progress();
        long t = 0;
        p.rest(0);
        for (int day = 0; day < 2000; day++) {
            t = day * 24000L + 5;
            p.train(t, 1_000_000, 10_000_000);
            p.fight(day % 3 == 0 ? GokuOmegaMath.Outcome.FAREWELL : GokuOmegaMath.Outcome.WIN, 5, 10, t, 10_000_000);
        }
        assertTrue(p.xp <= GokuOmegaMath.xpCeiling());
        assertEquals(GokuOmegaMath.HARD_MAX_LEVEL, p.level(10_000));
        assertEquals(50, p.level(50));
        assertTrue(p.wins > 0 && p.farewells > 0 && p.kills > 0);
        p.setLevel(12);
        assertEquals(12, p.level(50));
        p.setLevel(9999);
        assertEquals(GokuOmegaMath.HARD_MAX_LEVEL, p.level(10_000));
    }

    @Test
    void theRecordSurvivesTheSavedFile() {
        GokuOmegaMath.Progress a = new GokuOmegaMath.Progress();
        a.rest(1234);
        a.train(1234 + 5000, 60, 1500);
        a.fight(GokuOmegaMath.Outcome.WIN, 2, 3, 7000, 6000);
        a.fight(GokuOmegaMath.Outcome.FAREWELL, 0, 3, 7100, 6000);
        GokuWorld w = new GokuWorld();
        w.progress.xp = a.xp;
        w.progress.lastTrain = a.lastTrain;
        w.progress.day = a.day;
        w.progress.trainToday = a.trainToday;
        w.progress.fightToday = a.fightToday;
        w.progress.wins = a.wins;
        w.progress.farewells = a.farewells;
        w.progress.kills = a.kills;
        CompoundTag tag = w.save(new CompoundTag(), null);
        GokuWorld b = GokuWorld.load(tag, null);
        assertEquals(a.xp, b.progress.xp);
        assertEquals(a.lastTrain, b.progress.lastTrain);
        assertEquals(a.day, b.progress.day);
        assertEquals(a.trainToday, b.progress.trainToday);
        assertEquals(a.fightToday, b.progress.fightToday);
        assertEquals(a.wins, b.progress.wins);
        assertEquals(a.farewells, b.progress.farewells);
        assertEquals(a.kills, b.progress.kills);
        GokuWorld fresh = GokuWorld.load(new CompoundTag(), null);
        assertEquals(GokuOmegaMath.totalXpFor(GokuOmegaMath.START_LEVEL), fresh.progress.xp, "an old world starts at the start level");
        CompoundTag bad = new CompoundTag();
        bad.putLong("xp", Long.MAX_VALUE);
        bad.putInt("wins", -4);
        GokuWorld clamped = GokuWorld.load(bad, null);
        assertEquals(GokuOmegaMath.xpCeiling(), clamped.progress.xp);
        assertEquals(0, clamped.progress.wins);
    }

    // ---------------------------------------------------------------- hits, dodge, adaptation
    @Test
    void theHitShareIsTheClassicOneUnlessOmegaAndNeverAboveTheHardShare() {
        assertEquals(0.3, GokuOmegaMath.hitShare(false, 0.3, 0.9), 1e-12, "off: the classic cap, whatever Omega says");
        assertEquals(0.5, GokuOmegaMath.hitShare(true, 0.3, 0.5), 1e-12);
        assertEquals(0.3, GokuOmegaMath.hitShare(true, 0.3, 0.1), 1e-12, "Omega never lowers the classic cap");
        assertEquals(GokuOmegaMath.HARD_HIT_SHARE, GokuOmegaMath.hitShare(true, 0.3, 5.0), 1e-12);
        assertEquals(0.3, GokuOmegaMath.hitShare(true, 0.3, Double.NaN), 1e-12);
        assertEquals(2, GokuOmegaMath.hitsToKill(0.5));
        assertEquals(4, GokuOmegaMath.hitsToKill(0.3));
        assertEquals(2, GokuOmegaMath.hitsToKill(GokuOmegaMath.HARD_HIT_SHARE), "even the hard share leaves one hit of margin: nobody is one-shot by the cap");
        assertTrue(GokuOmegaMath.hitsToKill(0) >= 1);
    }

    @Test
    void theDodgeIsCappedAndMasteredIsNearPerfect() {
        assertEquals(0.85, GokuOmegaMath.dodgeChance(GokuOmegaMath.MASTERED_CHANCE, 0.85), 1e-12);
        assertEquals(0.95, GokuOmegaMath.dodgeChance(1.0, 5.0), 1e-12);
        assertEquals(0.0, GokuOmegaMath.dodgeChance(-1.0, 0.5), 1e-12);
        assertEquals(0.0, GokuOmegaMath.dodgeChance(Double.NaN, 0.5), 1e-12);
        assertTrue(GokuOmegaMath.MASTERED_CHANCE > 0.9 && GokuOmegaMath.MASTERED_CHANCE < 1.0, "near-perfect, never perfect");
        assertTrue(GokuOmegaMath.MASTERED_COOLDOWN > 0 && GokuOmegaMath.MASTERED_PER_WINDOW > 0 && GokuOmegaMath.MASTERED_WINDOW > 0);
    }

    @Test
    void adaptationHasFreeHitsGrowsAndIsCappedAndDecays() {
        GokuOmegaMath.Adapt a = new GokuOmegaMath.Adapt();
        double cap = GokuOmegaMath.adaptCap(20);
        for (int i = 0; i < 3; i++) a.hit(GokuOmegaMath.MELEE);
        assertEquals(0.0, a.resist(GokuOmegaMath.MELEE, cap), 1e-12, "the first hits cost him nothing");
        for (int i = 0; i < 4; i++) a.hit(GokuOmegaMath.MELEE);
        double r = a.resist(GokuOmegaMath.MELEE, cap);
        assertTrue(r > 0 && r <= cap);
        assertEquals(0.0, a.resist(GokuOmegaMath.FIRE, cap), 1e-12, "a kind he has not met is untouched");
        for (int i = 0; i < 1000; i++) a.hit(GokuOmegaMath.MELEE);
        assertEquals(cap, a.resist(GokuOmegaMath.MELEE, cap), 1e-12, "capped");
        assertTrue(a.hits(GokuOmegaMath.MELEE) <= GokuOmegaMath.MAX_HITS + 1e-9, "the counter itself is bounded");
        for (int i = 0; i < 100; i++) a.decay();
        assertEquals(0.0, a.resist(GokuOmegaMath.MELEE, cap), 1e-6, "he forgets slowly");
        a.hit(GokuOmegaMath.LIGHTNING);
        a.clear();
        assertEquals(0.0, a.hits(GokuOmegaMath.LIGHTNING), 1e-12);
        assertEquals(0.0, a.resist(-4, cap) + a.resist(99, cap), 1e-12, "a bad kind index is clamped, not thrown");
    }

    @Test
    void theAdaptationCapRisesWithTheLevelButNeverPastTheHardCap() {
        double prev = 0;
        for (int l = 1; l <= GokuOmegaMath.HARD_MAX_LEVEL; l++) {
            double c = GokuOmegaMath.adaptCap(l);
            assertTrue(c >= prev && c <= GokuOmegaMath.HARD_ADAPT_CAP);
            prev = c;
        }
        assertEquals(GokuOmegaMath.HARD_ADAPT_CAP, GokuOmegaMath.adaptCap(9999), 1e-12);
    }

    // ---------------------------------------------------------------- hacks
    @Test
    void theHacksUnlockInOrderAndEveryAimedOneIsBounded() {
        int prev = 0;
        for (GokuOmegaMath.Hack h : GokuOmegaMath.Hack.values()) {
            assertTrue(h.unlock >= prev, h + " is out of unlock order");
            prev = h.unlock;
            assertTrue(h.cooldown >= 0 && h.fightCap >= 0 && h.perPlayer >= 0);
            if (h.perPlayer > 0) assertTrue(h.cooldown > 0 && h.fightCap > 0, h + " is aimed at players and needs a cooldown and a fight cap");
            if (h.cooldown > 0 && h != GokuOmegaMath.Hack.KI_SENSE) assertTrue(h.fightCap > 0, h + " needs a fight cap");
        }
        assertEquals(2, GokuOmegaMath.unlockedCount(1));
        assertEquals(GokuOmegaMath.Hack.values().length, GokuOmegaMath.unlockedCount(GokuOmegaMath.HARD_MAX_LEVEL));
        for (int l = 2; l <= GokuOmegaMath.HARD_MAX_LEVEL; l++) assertTrue(GokuOmegaMath.unlockedCount(l) >= GokuOmegaMath.unlockedCount(l - 1));
    }

    @Test
    void drainAndSealGrowWithTheLevelUnderTheirCeilings() {
        for (int l = 1; l <= GokuOmegaMath.HARD_MAX_LEVEL; l++) {
            double d = GokuOmegaMath.drainFraction(l);
            int s = GokuOmegaMath.sealSeconds(l);
            assertTrue(d > 0 && d <= 0.35, "drain fraction " + d);
            assertTrue(s > 0 && s <= 30, "seal seconds " + s);
        }
        assertTrue(GokuOmegaMath.drainFraction(50) > GokuOmegaMath.drainFraction(1));
        assertTrue(GokuOmegaMath.WIND_SHARE * 5 <= GokuOmegaMath.WIND_FIGHT_CAP + 1e-9, "five Second Winds fit exactly into the fight cap");
    }
}
