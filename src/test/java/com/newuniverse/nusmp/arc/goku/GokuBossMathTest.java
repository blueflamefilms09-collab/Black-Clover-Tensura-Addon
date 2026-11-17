package com.newuniverse.nusmp.arc.goku;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The Goku boss's scaling arithmetic: EP is the first input, the fallback chain never gives 0, everything is monotone and capped, and the settings' defaults and bounds are sane. */
class GokuBossMathTest {
    private static GokuBossMath.Params params(GokuBossMath.Preset preset, double epWeight, double epCap, double bonus, int cap) {
        return new GokuBossMath.Params(preset, 1.0, 1.0, true, epWeight, epCap, bonus, cap);
    }

    private static final GokuBossMath.Params DEFAULTS = params(GokuBossMath.Preset.OVERPOWERED, GokuBossConfig.DEFAULT_EP_WEIGHT, GokuBossConfig.DEFAULT_EP_CAP,
            GokuBossConfig.DEFAULT_PER_PLAYER_BONUS, GokuBossMath.MAX_PLAYERS);

    // ------------------------------------------------------------------ the fallback chain: EP, then Battle Power, then the preset; never 0
    @Test
    void epIsReadFirstThenBattlePowerThenThePreset() {
        GokuBossMath.Preset p = GokuBossMath.Preset.OVERPOWERED;
        GokuBossMath.Reading ep = GokuBossMath.resolve(true, 2_000_000, 500, p);
        assertEquals(GokuBossMath.Source.EP, ep.source());
        assertEquals(2_000_000, ep.ep(), 1e-6);

        GokuBossMath.Reading bp = GokuBossMath.resolve(true, 0, 100_000, p);
        assertEquals(GokuBossMath.Source.BATTLE_POWER, bp.source(), "no EP: the Battle Power is used");
        assertEquals(100_000 * GokuBossMath.BP_TO_EP, bp.ep(), 1e-6);

        GokuBossMath.Reading none = GokuBossMath.resolve(true, 0, 0, p);
        assertEquals(GokuBossMath.Source.PRESET, none.source());
        assertEquals(p.floorEp(), none.ep(), 1e-9);

        GokuBossMath.Reading off = GokuBossMath.resolve(false, 9_000_000, 100_000, p);
        assertEquals(GokuBossMath.Source.BATTLE_POWER, off.source(), "epScaling off skips the EP");
    }

    @Test
    void theReadingIsNeverZeroNeverNegativeNeverBelowThePresetFloor() {
        double[] odd = {0, -1, -1e30, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 1e-12, 1, 1e300};
        for (GokuBossMath.Preset p : GokuBossMath.Preset.values()) {
            for (double a : odd) {
                for (double b : odd) {
                    for (boolean on : new boolean[]{true, false}) {
                        GokuBossMath.Reading r = GokuBossMath.resolve(on, a, b, p);
                        assertTrue(r.ep() > 0.0 && !Double.isNaN(r.ep()) && !Double.isInfinite(r.ep()), p + " " + a + " " + b + " gave " + r);
                        assertTrue(r.ep() >= p.floorEp(), "he is never below the preset's own floor: " + r);
                    }
                }
            }
        }
    }

    @Test
    void aWeakPartyStillMeetsABossAheadOfIt() {
        GokuBossMath.Reading weak = GokuBossMath.resolve(true, 800, 0, GokuBossMath.Preset.OVERPOWERED);
        assertEquals(GokuBossMath.Preset.OVERPOWERED.floorEp(), weak.ep(), 1e-9, "800 EP reads as the floor");
        assertTrue(GokuBossMath.epCurve(weak.ep(), 12) > GokuBossMath.epCurve(800, 12), "the boss's curve is ahead of the top player's");
    }

    // ------------------------------------------------------------------ the curve, monotone and capped
    @Test
    void theEpCurveIsTheBalanceLawsAndMonotoneAndCapped() {
        assertEquals(1.0, GokuBossMath.epCurve(1, 12), 1e-9);
        assertEquals(1.0, GokuBossMath.epCurve(1000, 12), 1e-9);
        assertEquals(3.5, GokuBossMath.epCurve(10_000, 12), 1e-9);
        assertEquals(6.0, GokuBossMath.epCurve(100_000, 12), 1e-9);
        double prev = 0;
        for (double ep = 1; ep < 1e15; ep *= 1.7) {
            double c = GokuBossMath.epCurve(ep, 12);
            assertTrue(c >= prev - 1e-12, "monotone at " + ep);
            assertTrue(c <= 12.0 + 1e-9);
            prev = c;
        }
        assertEquals(8.0, GokuBossMath.epCurve(1e30, 8.0), 1e-9, "epCap stops the curve");
        assertEquals(12.0, GokuBossMath.epCurve(1e30, 99.0), 1e-9, "the cap itself is capped at 12");
        assertEquals(1.0, GokuBossMath.epCurve(1e30, -4.0), 1e-9, "and floored at 1");
        assertEquals(1.0, GokuBossMath.epCurve(Double.NaN, 12), 1e-9);
        assertEquals(12.0, GokuBossMath.epCurve(1e30, Double.NaN), 1e-9);
    }

    @Test
    void healthAndDamageRiseWithEpUntilTheCapAndThenStop() {
        double prevH = 0, prevD = 0;
        for (double ep = 1_000; ep < 1e12; ep *= 2) {
            GokuBossMath.Reading r = GokuBossMath.resolve(true, ep, 0, GokuBossMath.Preset.OVERPOWERED);
            double h = GokuBossMath.healthFactor(r, 1, DEFAULTS), d = GokuBossMath.damageFactor(r, 1, DEFAULTS);
            assertTrue(h >= prevH - 1e-12, "health monotone at " + ep);
            assertTrue(d >= prevD - 1e-12, "damage monotone at " + ep);
            prevH = h;
            prevD = d;
        }
        GokuBossMath.Reading a = GokuBossMath.resolve(true, 1e12, 0, GokuBossMath.Preset.OVERPOWERED);
        GokuBossMath.Reading b = GokuBossMath.resolve(true, 1e30, 0, GokuBossMath.Preset.OVERPOWERED);
        assertEquals(GokuBossMath.healthFactor(a, 1, DEFAULTS), GokuBossMath.healthFactor(b, 1, DEFAULTS), 1e-9, "past the cap more EP changes nothing");
        assertEquals(GokuBossMath.damageFactor(a, 1, DEFAULTS), GokuBossMath.damageFactor(b, 1, DEFAULTS), 1e-9);
    }

    @Test
    void damageGrowsSlowerThanHealthSoThePvpCapStillMeansSomething() {
        for (double ep : new double[]{5_000, 50_000, 500_000, 5_000_000, 5e8}) {
            GokuBossMath.Reading r = GokuBossMath.resolve(true, ep, 0, GokuBossMath.Preset.OVERPOWERED);
            assertTrue(GokuBossMath.damageFactor(r, 4, DEFAULTS) <= GokuBossMath.healthFactor(r, 4, DEFAULTS) + 1e-9, "at " + ep);
        }
    }

    @Test
    void epWeightZeroMeansEpChangesNothing() {
        GokuBossMath.Params flat = params(GokuBossMath.Preset.OVERPOWERED, 0.0, 8.0, 0.0, 10);
        GokuBossMath.Reading lo = GokuBossMath.resolve(true, 5_000, 0, GokuBossMath.Preset.OVERPOWERED);
        GokuBossMath.Reading hi = GokuBossMath.resolve(true, 5e9, 0, GokuBossMath.Preset.OVERPOWERED);
        assertEquals(GokuBossMath.healthFactor(lo, 1, flat), GokuBossMath.healthFactor(hi, 1, flat), 1e-9);
        assertEquals(1.0, GokuBossMath.healthFactor(hi, 1, flat), 1e-9);
    }

    @Test
    void theHardCeilingsHoldWhateverTheSettingsSay() {
        GokuBossMath.Params wild = new GokuBossMath.Params(GokuBossMath.Preset.ANIME, 50.0, 20.0, true, 5.0, 12.0, 2.0, 10);
        GokuBossMath.Reading r = GokuBossMath.resolve(true, 1e30, 0, GokuBossMath.Preset.ANIME);
        assertEquals(GokuBossMath.MAX_HEALTH_FACTOR, GokuBossMath.healthFactor(r, 50, wild), 1e-9);
        assertEquals(GokuBossMath.MAX_DAMAGE_FACTOR, GokuBossMath.damageFactor(r, 50, wild), 1e-9);
        GokuBossMath.Params tiny = new GokuBossMath.Params(GokuBossMath.Preset.NORMAL, 0.1, 0.1, true, 0.0, 1.0, 0.0, 1);
        GokuBossMath.Reading low = GokuBossMath.resolve(true, 0, 0, GokuBossMath.Preset.NORMAL);
        assertEquals(GokuBossMath.MIN_FACTOR, GokuBossMath.healthFactor(low, 1, tiny), 1e-9, "the floor of the factors");
        GokuBossMath.Params bad = new GokuBossMath.Params(GokuBossMath.Preset.OVERPOWERED, Double.NaN, -3.0, true, Double.NaN, Double.NaN, Double.NaN, 0);
        double h = GokuBossMath.healthFactor(r, 3, bad), d = GokuBossMath.damageFactor(r, 3, bad);
        assertTrue(h >= GokuBossMath.MIN_FACTOR && h <= GokuBossMath.MAX_HEALTH_FACTOR && !Double.isNaN(h), "NaN settings are survived: " + h);
        assertTrue(d >= GokuBossMath.MIN_FACTOR && d <= GokuBossMath.MAX_DAMAGE_FACTOR && !Double.isNaN(d), "NaN settings are survived: " + d);
    }

    // ------------------------------------------------------------------ players: counted at most ten, monotone
    @Test
    void atMostTenPlayersCountAndEachOneAddsTheBonus() {
        assertEquals(1, GokuBossMath.countedPlayers(0, 10));
        assertEquals(1, GokuBossMath.countedPlayers(-4, 10));
        assertEquals(7, GokuBossMath.countedPlayers(7, 10));
        assertEquals(10, GokuBossMath.countedPlayers(40, 10));
        assertEquals(10, GokuBossMath.countedPlayers(40, 99), "the setting cannot lift the 10-player cap");
        assertEquals(3, GokuBossMath.countedPlayers(40, 3));
        assertEquals(1, GokuBossMath.countedPlayers(40, 0));
        assertEquals(1.0, GokuBossMath.playerFactor(1, 0.35), 1e-9);
        assertEquals(1.35, GokuBossMath.playerFactor(2, 0.35), 1e-9);
        assertEquals(1.0 + 0.35 * 9, GokuBossMath.playerFactor(10, 0.35), 1e-9);
        GokuBossMath.Reading r = GokuBossMath.resolve(true, 100_000, 0, GokuBossMath.Preset.OVERPOWERED);
        double prev = 0;
        for (int n = 1; n <= 30; n++) {
            double h = GokuBossMath.healthFactor(r, n, DEFAULTS);
            assertTrue(h >= prev - 1e-12, "monotone at " + n + " players");
            prev = h;
        }
        assertEquals(GokuBossMath.healthFactor(r, 10, DEFAULTS), GokuBossMath.healthFactor(r, 30, DEFAULTS), 1e-9, "the 11th player adds nothing");
    }

    // ------------------------------------------------------------------ presets
    @Test
    void thePresetsAreOrderedAndUnknownWordsMeanOverpowered() {
        assertEquals(GokuBossMath.Preset.NORMAL, GokuBossMath.Preset.parse(" Normal "));
        assertEquals(GokuBossMath.Preset.ANIME, GokuBossMath.Preset.parse("ANIME"));
        assertEquals(GokuBossMath.Preset.OVERPOWERED, GokuBossMath.Preset.parse("overpowered"));
        assertEquals(GokuBossMath.Preset.OVERPOWERED, GokuBossMath.Preset.parse("easy"));
        assertEquals(GokuBossMath.Preset.OVERPOWERED, GokuBossMath.Preset.parse(null));
        assertTrue(GokuBossMath.Preset.NORMAL.health() < GokuBossMath.Preset.OVERPOWERED.health());
        assertTrue(GokuBossMath.Preset.OVERPOWERED.health() < GokuBossMath.Preset.ANIME.health());
        assertTrue(GokuBossMath.Preset.NORMAL.damage() < GokuBossMath.Preset.OVERPOWERED.damage());
        assertTrue(GokuBossMath.Preset.OVERPOWERED.damage() < GokuBossMath.Preset.ANIME.damage());
        assertTrue(GokuBossMath.Preset.NORMAL.floorEp() < GokuBossMath.Preset.OVERPOWERED.floorEp());
        assertTrue(GokuBossMath.Preset.OVERPOWERED.floorEp() < GokuBossMath.Preset.ANIME.floorEp());
        for (GokuBossMath.Preset p : GokuBossMath.Preset.values()) assertTrue(p.floorEp() > 0 && p.formStrength() > 0, p.name());
    }

    @Test
    void theFormPaceQuickensWithEpButNeverMoreThanTheCap() {
        double prev = 2.0;
        for (double ep = 1_000; ep < 1e13; ep *= 3) {
            double pace = GokuBossMath.paceFactor(GokuBossMath.resolve(true, ep, 0, GokuBossMath.Preset.OVERPOWERED), DEFAULTS);
            assertTrue(pace <= prev + 1e-12, "monotone at " + ep);
            assertTrue(pace >= 1.0 - GokuBossMath.MAX_PACE_GAIN - 1e-9 && pace <= 1.0 + 1e-9);
            prev = pace;
        }
        assertEquals(1.0, GokuBossMath.paceFactor(new GokuBossMath.Reading(5e6, GokuBossMath.Source.EP), params(GokuBossMath.Preset.OVERPOWERED, 0.6, 1.0, 0.0, 10)), 1e-9);
        assertEquals(14_400L, GokuBossMath.enrageTicks(12, 1.0));
        assertTrue(GokuBossMath.enrageTicks(12, 0.6) < 14_400L);
        assertTrue(GokuBossMath.enrageTicks(0, 0.0) >= 600L, "never a zero timer");
        assertTrue(GokuBossMath.enrageTicks(60, 5.0) <= 60L * 1200L, "a pace above 1 is ignored");
    }

    // ------------------------------------------------------------------ the safety caps
    @Test
    void oneHitNeverTakesMoreThanTheShareOfAPlayersHealth() {
        assertEquals(6f, GokuBossMath.capHit(1e9f, 20f, 0.3), 1e-4);
        assertEquals(3f, GokuBossMath.capHit(3f, 20f, 0.3), 1e-4, "a small hit is untouched");
        assertEquals(1f, GokuBossMath.capHit(1e9f, 2f, 0.05), 1e-4, "at least one point is always dealt");
        assertEquals(20f, GokuBossMath.capHit(1e9f, 20f, 5.0), 1e-4, "the share is itself capped at 1");
        assertEquals(6f, GokuBossMath.capHit(1e9f, 20f, Double.NaN), 1e-4, "a broken setting reads as 30%");
    }

    @Test
    void theDodgeChanceIsCappedAndNeverNegative() {
        assertEquals(0.35, GokuBossMath.dodgeChance(0.9, 0.35), 1e-9);
        assertEquals(0.2, GokuBossMath.dodgeChance(0.2, 0.35), 1e-9);
        assertEquals(0.6, GokuBossMath.dodgeChance(1.0, 5.0), 1e-9, "no setting lifts it above 0.6");
        assertEquals(0.0, GokuBossMath.dodgeChance(-1.0, 0.35), 1e-9);
        assertEquals(0.0, GokuBossMath.dodgeChance(0.9, 0.0), 1e-9, "a cap of 0 switches the dodge off");
    }

    @Test
    void aChangeOfMaximumHealthKeepsTheSameShareOfTheBar() {
        assertEquals(500f, GokuBossMath.keepShare(250f, 1000f, 2000f), 1e-3);
        assertEquals(125f, GokuBossMath.keepShare(250f, 1000f, 500f), 1e-3);
        assertEquals(1000f, GokuBossMath.keepShare(1000f, 1000f, 1000f), 1e-3);
        assertEquals(1f, GokuBossMath.keepShare(0.01f, 1000f, 1000f), 1e-3, "he is never left at 0 by a rescale");
        assertTrue(GokuBossMath.keepShare(5f, 0f, 100f) >= 1f, "a broken 'before' is survived");
    }

    @Test
    void theBeamDistanceIsTheDistanceToTheSegment() {
        assertEquals(1.0, GokuBossMath.distToSegment(5, 1, 0, 0, 0, 0, 10, 0, 0), 1e-9);
        assertEquals(5.0, GokuBossMath.distToSegment(-5, 0, 0, 0, 0, 0, 10, 0, 0), 1e-9, "before the start counts from the start");
        assertEquals(3.0, GokuBossMath.distToSegment(10, 3, 0, 0, 0, 0, 10, 0, 0), 1e-9);
        assertEquals(5.0, GokuBossMath.distToSegment(3, 4, 0, 0, 0, 0, 0, 0, 0), 1e-9, "a zero-length beam is a point");
    }

    // ------------------------------------------------------------------ the settings
    @Test
    void theSettingsDefaultsAndBoundsAreSane() {
        assertTrue(GokuBossConfig.DEFAULT_EP_WEIGHT >= 0.0 && GokuBossConfig.DEFAULT_EP_WEIGHT <= 5.0);
        assertTrue(GokuBossConfig.DEFAULT_EP_CAP >= 1.0 && GokuBossConfig.DEFAULT_EP_CAP <= 12.0);
        assertTrue(GokuBossConfig.DEFAULT_PER_PLAYER_BONUS >= 0.0 && GokuBossConfig.DEFAULT_PER_PLAYER_BONUS <= 2.0);
        assertTrue(GokuBossConfig.DEFAULT_MAX_HIT > 0.05 && GokuBossConfig.DEFAULT_MAX_HIT <= 0.9);
        assertTrue(GokuBossConfig.DEFAULT_DODGE_CAP >= 0.0 && GokuBossConfig.DEFAULT_DODGE_CAP <= 0.6);
        assertEquals(2, GokuBossConfig.DEFAULT_GLOBAL_CAP, "the global cap is 2");
        assertTrue(GokuBossConfig.DEFAULT_ENRAGE_MINUTES >= 3 && GokuBossConfig.DEFAULT_ENRAGE_MINUTES <= 60);
        assertEquals(10, GokuBossMath.MAX_PLAYERS);
        assertEquals(100, GokuBossMath.RESCAN_TICKS);
        assertEquals(GokuBossMath.Preset.OVERPOWERED, GokuBossConfig.preset(), "the default difficulty is overpowered");
        assertTrue(GokuBossConfig.enabled() && GokuBossConfig.epScaling() && !GokuBossConfig.blockDamage(), "defaults: on, EP scaling on, block damage off");
        assertEquals(GokuBossConfig.DEFAULT_EP_WEIGHT, GokuBossConfig.params().epWeight(), 1e-9);
        assertEquals(GokuBossConfig.DEFAULT_GLOBAL_CAP, GokuBossConfig.globalCap());
    }
}
