package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Relics of Many Worlds, wave 7: the armours' pure rules (hunger, stillness, stealth, starscape, soul hit points, modes...). */
class Wave7MathTest {
    @Test
    void stillnessBuildsUpAndAnyMovementResetsIt() {
        int still = 0;
        for (int i = 0; i < Wave7Math.TRANCE_TICKS / Wave7Math.SAMPLE - 1; i++) {
            still = Wave7Math.stillNext(still, false);
            assertFalse(Wave7Math.tranced(still), "not yet at sample " + i);
        }
        still = Wave7Math.stillNext(still, false);
        assertTrue(Wave7Math.tranced(still), "8 seconds of stillness is the trance");
        assertEquals(0, Wave7Math.stillNext(still, true), "a step breaks it");
        assertTrue(Wave7Math.stillNext(Integer.MAX_VALUE - 1, false) <= 100_000, "the counter is capped");
    }

    @Test
    void theSentinelKeepsHalfOfTheExhaustion() {
        assertEquals(1.5f, Wave7Math.halfHunger(1.0f, 2.0f), 1e-6);
        assertEquals(2.0f, Wave7Math.halfHunger(2.0f, 2.0f), 1e-6, "no gain, no change");
        assertEquals(0.5f, Wave7Math.halfHunger(3.9f, 0.5f), 1e-6, "a vanilla wrap-around is left alone");
        float last = 0f, now = 0f;
        for (int i = 0; i < 20; i++) {            // 20 samples of +0.4 exhaustion each
            now = last + 0.4f;
            last = Wave7Math.halfHunger(last, now);
        }
        assertEquals(4.0f, last, 1e-4, "twice the activity for the same exhaustion: 8.0 gained, 4.0 kept");
    }

    @Test
    void hungerSamplesAreOnlyTrustedWhileFoodIsUnchangedAndFresh() {
        assertTrue(Wave7Math.hungerSynced(18, 18, 2f, 2f, 20));
        assertFalse(Wave7Math.hungerSynced(18, 17, 2f, 2f, 20), "food dropped");
        assertFalse(Wave7Math.hungerSynced(18, 18, 2f, 1f, 20), "saturation dropped");
        assertFalse(Wave7Math.hungerSynced(18, 18, 2f, 2f, Wave7Math.HUNGER_STALE + 1), "stale: the armour was off");
        assertFalse(Wave7Math.hungerSynced(18, 18, 2f, 2f, -1));
    }

    @Test
    void stealthMultipliesAndIsClamped() {
        assertEquals(1.0, Wave7Math.visibility(false, 0, false, false, 1.0, false, false), 1e-9);
        assertEquals(Wave7Math.VIS_HEAVY, Wave7Math.visibility(false, 0, false, false, 1.0, false, true), 1e-9, "heavy plate is noisy");
        assertEquals(Wave7Math.VIS_GOLD, Wave7Math.visibility(false, 0, false, true, 1.0, false, false), 1e-9);
        assertEquals(Wave7Math.VIS_GOLD * Wave7Math.VIS_SNEAK_EXTRA, Wave7Math.visibility(true, 0, false, true, 1.0, false, false), 1e-9);
        assertEquals(Wave7Math.VIS_HEAVY, Wave7Math.visibility(true, 0, false, false, 1.0, false, true), 1e-9, "sneaking only helps stealthy armours");
        assertEquals(Wave7Math.VIS_TRANCE, Wave7Math.visibility(false, 1, false, false, 1.0, false, false), 1e-9);
        assertEquals(Wave7Math.VIS_DEEP, Wave7Math.visibility(false, 2, false, false, 1.0, false, false), 1e-9, "the deep trance");
        assertEquals(Wave7Math.VIS_MIN, Wave7Math.visibility(true, 1, true, true, 1.0, true, false), 1e-9, "never invisible to mobs");
        assertTrue(Wave7Math.visibility(false, 0, false, true, 0.5, false, false) > Wave7Math.visibility(false, 0, false, true, 1.0, false, false), "untrained: less stealthy");
        assertTrue(Wave7Math.visibility(false, 0, false, true, 1.5, false, false) < Wave7Math.visibility(false, 0, false, true, 1.0, false, false), "expert: more stealthy");
        for (boolean s : new boolean[]{false, true})
            for (int m = 0; m < 96; m++) {
                double v = Wave7Math.visibility(s, m % 3, (m & 2) != 0, (m & 4) != 0, 0.5 + (m % 5) * 0.25, (m & 8) != 0, (m & 16) != 0);
                assertTrue(v >= Wave7Math.VIS_MIN && v <= Wave7Math.VIS_MAX);
            }
    }

    @Test
    void theStarscapeGrowsAtNight() {
        assertFalse(Wave7Math.isNight(0));
        assertFalse(Wave7Math.isNight(6000));
        assertFalse(Wave7Math.isNight(12000));
        assertTrue(Wave7Math.isNight(13000));
        assertTrue(Wave7Math.isNight(18000));
        assertFalse(Wave7Math.isNight(23600));
        assertTrue(Wave7Math.isNight(24000L * 5 + 14000), "any day");
        assertTrue(Wave7Math.isNight(-24000L + 14000), "negative day time wraps");
        assertEquals(Wave7Math.STAR_NIGHT_RADIUS, Wave7Math.starRadius(15000), 1e-9);
        assertEquals(Wave7Math.STAR_DAY_RADIUS, Wave7Math.starRadius(3000), 1e-9);
        assertTrue(Wave7Math.STAR_NIGHT_RADIUS > Wave7Math.STAR_DAY_RADIUS);
    }

    @Test
    void radiantRetaliationIsPvpSafe() {
        assertTrue(Wave7Math.starRetaliation(false) <= 6f, "at most 30% of 20 HP");
        assertTrue(Wave7Math.starRetaliation(true) < Wave7Math.starRetaliation(false));
        assertTrue(Wave7Math.STAR_BOLT <= 6f);
        assertTrue(Wave7Math.TAIL_HIT <= 6f);
    }

    @Test
    void theDuelistOnlyDodgesMeleeWithAFreeHand() {
        assertEquals(Wave7Math.DUEL_TAKEN, Wave7Math.duelistTaken(true, true), 1e-6);
        assertEquals(1f, Wave7Math.duelistTaken(false, true), 1e-6, "a shield or a second weapon: no bonus");
        assertEquals(1f, Wave7Math.duelistTaken(true, false), 1e-6, "arrows and spells are not parried by footwork");
    }

    @Test
    void sneakAttackIsBoundedAndNeverFreeInPvp() {
        assertEquals(3f, Wave7Math.sneakBonus(1f), 1e-6, "the reroll of a 1: never below 3");
        assertEquals(4f, Wave7Math.sneakBonus(10f), 1e-6);
        assertEquals(8f, Wave7Math.sneakBonus(500f), 1e-6, "never above 8");
        assertEquals(4f, Wave7Math.sneakBonus(10f, 1.0), 1e-6);
        assertEquals(2f, Wave7Math.sneakBonus(10f, 0.5), 1e-6, "untrained: half");
        assertEquals(12f, Wave7Math.sneakBonus(500f, 1.5), 1e-6, "expert: half as much again, still bounded");
        assertEquals(6f, Wave7Math.sneakBonus(10f, 99.0), 1e-6, "a silly training value is clamped to 1.5");
        assertTrue(Wave7Math.isSneakAttack(true, false, true), "cloaked players always");
        assertTrue(Wave7Math.isSneakAttack(false, true, false), "an unaware mob");
        assertFalse(Wave7Math.isSneakAttack(false, true, true), "a mob that is fighting you");
        assertFalse(Wave7Math.isSneakAttack(false, false, false), "an uncloaked player is never a free target");
    }

    @Test
    void lionsResolveMovesTheGuard() {
        assertEquals(1f, Wave7Math.resolveFactor(false, false), 1e-6);
        assertEquals(Wave7Math.RESOLVE_PROTECT, Wave7Math.resolveFactor(true, false), 1e-6);
        assertEquals(Wave7Math.RESOLVE_EXPOSED, Wave7Math.resolveFactor(false, true), 1e-6);
        assertEquals(Wave7Math.RESOLVE_PROTECT * Wave7Math.RESOLVE_EXPOSED, Wave7Math.resolveFactor(true, true), 1e-6);
        assertTrue(Wave7Math.RESOLVE_PROTECT < 1f && Wave7Math.RESOLVE_EXPOSED > 1f);
    }

    @Test
    void theHeadlessKnightFearsNoFlyingAxe() {
        assertTrue(Wave7Math.headlessProjectile(true) < Wave7Math.headlessProjectile(false));
        assertTrue(Wave7Math.headlessProjectile(false) < 1f);
        assertTrue(Wave7Math.headlessBlocks("EAT"));
        assertTrue(Wave7Math.headlessBlocks("DRINK"));
        assertFalse(Wave7Math.headlessBlocks("BLOCK"));
        assertFalse(Wave7Math.headlessBlocks(null));
    }

    @Test
    void soulHitPointsStackToACapAndFade() {
        assertEquals(1f, Wave7Math.soulRoll(0));
        assertEquals(4f, Wave7Math.soulRoll(3));
        assertEquals(4f, Wave7Math.soulRoll(7), "any roll maps to 1d4");
        assertEquals(1f, Wave7Math.soulRoll(-4));
        float[] r = Wave7Math.soulApply(0f, 0f, 3f, 8f);
        assertEquals(3f, r[0]);
        assertEquals(3f, r[1]);
        r = Wave7Math.soulApply(r[0], r[1], 4f, 8f);
        assertEquals(7f, r[0]);
        assertEquals(7f, r[1]);
        r = Wave7Math.soulApply(r[0], r[1], 4f, 8f);
        assertEquals(8f, r[0], "the cap");
        assertEquals(8f, r[1]);
        r = Wave7Math.soulApply(8f, 2f, 1f, 8f);          // blows ate most of the borrowed points
        assertEquals(3f, r[0], "tracked shrinks to what is left, then grows");
        assertEquals(3f, r[1]);
        r = Wave7Math.soulApply(4f, 10f, 2f, 8f);          // a golden-apple heart pool on top of the borrowed points
        assertEquals(6f, r[0]);
        assertEquals(12f, r[1], "other absorption is kept");
        assertEquals(0f, Wave7Math.soulFade(8f, 8f));
        assertEquals(6f, Wave7Math.soulFade(4f, 10f), "only the borrowed points fade");
        assertEquals(0f, Wave7Math.soulFade(4f, 1f));
        assertEquals(5f, Wave7Math.soulFade(0f, 5f));
    }

    @Test
    void arcaneModesCycleAndTradeOffs() {
        int m = Wave7Math.MODE_BASE;
        for (int i = 0; i < Wave7Math.MODES; i++) m = Wave7Math.nextMode(m);
        assertEquals(Wave7Math.MODE_BASE, m, "four steps come full circle");
        assertEquals(Wave7Math.MODE_FORTIFIED, Wave7Math.nextMode(Wave7Math.MODE_BASE));
        assertEquals(Wave7Math.MODE_BASE, Wave7Math.nextMode(Wave7Math.MODE_BURST));
        assertEquals(Wave7Math.MODE_BASE, Wave7Math.nextMode(-1), "bad saved values wrap");
        assertEquals("Burst Mode", Wave7Math.modeName(Wave7Math.MODE_BURST));
        assertEquals("Base Mode", Wave7Math.modeName(-4));
        assertEquals(Wave7Math.FORCE_TAKEN, Wave7Math.arcaneTaken(Wave7Math.MODE_BASE, true, 0.0), 1e-6);
        assertEquals(1f, Wave7Math.arcaneTaken(Wave7Math.MODE_BASE, false, 0.0), 1e-6);
        assertTrue(Wave7Math.arcaneTaken(Wave7Math.MODE_FORTIFIED, false, 0.0) < 1f);
        assertTrue(Wave7Math.arcaneTaken(Wave7Math.MODE_BURST, false, 0.0) > 1f, "Burst is fragile");
        assertEquals(1f, Wave7Math.arcaneTaken(Wave7Math.MODE_STEALTH, true, 0.0), 1e-6);
        assertEquals(0.9f, Wave7Math.arcaneTaken(Wave7Math.MODE_STEALTH, false, 0.1), 1e-6, "the Intelligence bonus works in every mode");
        assertEquals(0.8f, Wave7Math.arcaneTaken(Wave7Math.MODE_STEALTH, false, 7.0), 1e-6, "but is capped at 20%");
        assertEquals(Wave7Math.BURST_DEALT, Wave7Math.arcaneDealt(Wave7Math.MODE_BURST, true), 1e-6);
        assertEquals(1f, Wave7Math.arcaneDealt(Wave7Math.MODE_BURST, false), 1e-6);
        assertEquals(1f, Wave7Math.arcaneDealt(Wave7Math.MODE_BASE, true), 1e-6);
    }

    @Test
    void damageAndEntityTypesAreRecognisedByName() {
        assertTrue(Wave7Math.isForceLike("magic"));
        assertTrue(Wave7Math.isForceLike("indirect_magic"));
        assertTrue(Wave7Math.isForceLike("sonic_boom"));
        assertTrue(Wave7Math.isForceLike("Spatial_Rend"));
        assertFalse(Wave7Math.isForceLike("player_attack"));
        assertFalse(Wave7Math.isForceLike(null));
        assertTrue(Wave7Math.isDragonish("ender_dragon"));
        assertTrue(Wave7Math.isDragonish("fire_drake"));
        assertFalse(Wave7Math.isDragonish("zombie"));
        assertFalse(Wave7Math.isDragonish(null));
    }

    @Test
    void immunitiesNameTheRightEffects() {
        assertTrue(Wave7Math.isFear("tensura", "fear"));
        assertFalse(Wave7Math.isFear("minecraft", "fear"));
        assertFalse(Wave7Math.isFear("tensura", "silence"));
        assertTrue(Wave7Math.isHeadEffect("minecraft", "blindness"));
        assertTrue(Wave7Math.isHeadEffect("minecraft", "nausea"));
        assertTrue(Wave7Math.isHeadEffect("minecraft", "darkness"));
        assertFalse(Wave7Math.isHeadEffect("minecraft", "slowness"));
    }

    @Test
    void timersAreAbsolute() {
        assertTrue(Wave7Math.active(100, 99));
        assertFalse(Wave7Math.active(100, 100));
        assertFalse(Wave7Math.active(0, 0));
        assertTrue(Wave7Math.WINGS_TICKS > 0 && Wave7Math.WINGS_TICKS <= 24000, "one use a day, never longer than a day");
        assertTrue(Wave7Math.HEAD_TICKS < 1200 * 2);
    }

    @Test
    void statGatesScaleOnALogScale() {
        double g = Wave7Math.GATE_ROGUE_AURA;
        assertEquals(1.0, Wave7Math.training(g, g), 1e-9);
        assertEquals(1.5, Wave7Math.training(g * 10, g), 1e-9);
        assertEquals(1.5, Wave7Math.training(g * 1e6, g), 1e-9, "capped");
        assertEquals(0.5, Wave7Math.training(g / 10, g), 1e-9);
        assertEquals(0.5, Wave7Math.training(0, g), 1e-9, "nothing: untrained, not locked out");
        assertEquals(0.5, Wave7Math.training(-5, g), 1e-9);
        assertEquals(0.5, Wave7Math.training(100, 0), 1e-9, "a bad gate does not blow up");
        assertEquals(0.5, Wave7Math.training(Double.NaN, g), 1e-9);
        double last = 0;
        for (double v = 1; v < 1e9; v *= 3) {
            double t = Wave7Math.training(v, g);
            assertTrue(t >= last - 1e-12, "monotone");
            last = t;
        }
    }

    @Test
    void theSpellcasterGateOpensTheModesAndTheIntelligenceBonus() {
        assertFalse(Wave7Math.isCaster(0));
        assertFalse(Wave7Math.isCaster(Wave7Math.GATE_CASTER_MAGICULE - 1));
        assertTrue(Wave7Math.isCaster(Wave7Math.GATE_CASTER_MAGICULE));
        assertEquals(0.0, Wave7Math.intBonus(999), 1e-9);
        assertEquals(0.04, Wave7Math.intBonus(1000), 1e-9);
        assertEquals(0.08, Wave7Math.intBonus(10_000), 1e-9);
        assertEquals(0.12, Wave7Math.intBonus(100_000), 1e-9);
        assertEquals(0.12, Wave7Math.intBonus(1e12), 1e-9, "capped");
        assertEquals(0.0, Wave7Math.intBonus(Double.NaN), 1e-9);
        assertTrue(Wave7Math.modeAllowed(Wave7Math.MODE_BASE, false), "everyone gets Base");
        assertFalse(Wave7Math.modeAllowed(Wave7Math.MODE_FORTIFIED, false));
        assertFalse(Wave7Math.modeAllowed(Wave7Math.MODE_STEALTH, false));
        assertFalse(Wave7Math.modeAllowed(Wave7Math.MODE_BURST, false));
        for (int m = 0; m < Wave7Math.MODES; m++) assertTrue(Wave7Math.modeAllowed(m, true));
    }

    @Test
    void heavyPlateDragsBelowTheStrengthGate() {
        assertTrue(Wave7Math.weighedDown(100));
        assertTrue(Wave7Math.weighedDown(Wave7Math.GATE_STRENGTH_EP - 1));
        assertFalse(Wave7Math.weighedDown(Wave7Math.GATE_STRENGTH_EP));
        assertFalse(Wave7Math.weighedDown(0), "unknown existence is not punished");
        assertTrue(Wave7Math.tailPush(1.5) > Wave7Math.tailPush(0.5));
        assertEquals(1.1, Wave7Math.tailPush(1.0), 1e-9);
    }

    @Test
    void theTranceDeepensAfterTenMinutes() {
        assertEquals(0, Wave7Math.tranceLevel(0));
        assertEquals(0, Wave7Math.tranceLevel(Wave7Math.TRANCE_TICKS - 1));
        assertEquals(1, Wave7Math.tranceLevel(Wave7Math.TRANCE_TICKS));
        assertEquals(1, Wave7Math.tranceLevel(Wave7Math.DEEP_TRANCE_TICKS - 1));
        assertEquals(2, Wave7Math.tranceLevel(Wave7Math.DEEP_TRANCE_TICKS));
        assertEquals(10 * 60 * 20, Wave7Math.DEEP_TRANCE_TICKS, "the card's ten minutes");
        int still = 0;
        for (int i = 0; i < Wave7Math.DEEP_TRANCE_TICKS / Wave7Math.SAMPLE; i++) still = Wave7Math.stillNext(still, false);
        assertEquals(2, Wave7Math.tranceLevel(still), "ten minutes of samples reach the deep trance");
    }

    @Test
    void unseenAndTheSoulEnergyAreBounded() {
        assertEquals(Wave7Math.UNSEEN_TICKS, Wave7Math.unseenTicks(1.0));
        assertEquals(Wave7Math.UNSEEN_TICKS / 2, Wave7Math.unseenTicks(0.5));
        assertEquals(Wave7Math.UNSEEN_TICKS * 3 / 2, Wave7Math.unseenTicks(1.5));
        assertEquals(Wave7Math.UNSEEN_TICKS * 3 / 2, Wave7Math.unseenTicks(50.0));
        assertTrue(Wave7Math.soulEnergyShare(true) > Wave7Math.soulEnergyShare(false));
        assertTrue(Wave7Math.soulEnergyShare(true) <= 0.10f + 1e-6f, "never more than a tenth of the pool");
    }

    @Test
    void dispelMagicCostsTheHeadsSenses() {
        assertTrue(Wave7Math.sensesOn(true, 0, 100));
        assertFalse(Wave7Math.sensesOn(false, 0, 100), "no head off, no magical senses");
        assertFalse(Wave7Math.sensesOn(true, 100 + Wave7Math.DISPEL_SENSES_TICKS, 100), "dispelled: dark for a minute");
        assertTrue(Wave7Math.sensesOn(true, 100 + Wave7Math.DISPEL_SENSES_TICKS, 100 + Wave7Math.DISPEL_SENSES_TICKS), "and back after it");
        assertEquals(60 * 20, Wave7Math.DISPEL_SENSES_TICKS, "the card's 1 minute");
        assertTrue(Wave7Math.JAM_SECONDS_BOSS < Wave7Math.JAM_SECONDS);
    }
}
