package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Wave 13 of the Relics of Many Worlds: the pure rules of the nine blades. */
class Wave13RulesTest {
    private static Wave13Rules.Bag bag() {
        Map<String, Long> m = new HashMap<>();
        return new Wave13Rules.Bag() {
            @Override public long get(String key) { return m.getOrDefault(key, 0L); }
            @Override public void put(String key, long value) { m.put(key, value); }
        };
    }

    private static double after(double dmg, double armor, double tough) {
        double f = 2 + tough / 4, g = Math.min(20, Math.max(armor - dmg / f, armor * 0.2));
        return dmg * (1 - g / 25);
    }

    @Test
    void ethericBlowsLandAsIfOnlyNaturalHideWere() {
        double in = Wave13Rules.bypassArmour(8.0, 20.0, 0.0, 2.0);
        assertTrue(in > 8.0 && in <= 20.0);
        assertEquals(8.0, after(in, 20.0, 2.0), 0.25, "worn armour no longer reduces the blow");
        assertEquals(8.0, Wave13Rules.bypassArmour(8.0, 2.0, 2.0, 0.0), 1e-9, "natural armour only: nothing to bypass");
        assertEquals(8.0 * 2.5, Wave13Rules.bypassArmour(8.0, 100.0, 0.0, 0.0), 1e-9, "capped at two and a half times");
    }

    @Test
    void theSaintsPriceNeverLeavesLessThanThreeHearts() {
        assertEquals(0.0, Wave13Rules.sacrificeModifier(0, 20.0), 1e-9);
        assertEquals(-4.0, Wave13Rules.sacrificeModifier(2, 20.0), 1e-9);
        assertEquals(-12.0, Wave13Rules.sacrificeModifier(5, 20.0), 1e-9);
        double left = 20.0 - Wave13Rules.CURSE_HP + Wave13Rules.sacrificeModifier(5, 20.0);
        assertTrue(left >= Wave13Rules.MIN_MAX_HP);
        assertEquals(0.0, Wave13Rules.sacrificeModifier(5, 8.0), 1e-9, "a weak wielder gives nothing more");
        assertEquals(6.0, Wave13Rules.stageDamage(4), 1e-9);
        assertEquals(9.0, Wave13Rules.stageDamage(5), 1e-9);
        assertTrue(Wave13Rules.saintCrit(5, 0.1));
        assertFalse(Wave13Rules.saintCrit(5, 0.3));
        assertFalse(Wave13Rules.saintCrit(4, 0.0));
    }

    @Test
    void theAxeAsksAgainAtDawn() {
        Wave13Rules.Bag b = bag();
        assertFalse(Wave13Rules.askedToday(b, 100));
        assertTrue(Wave13Rules.askedToday(b, 5000));
        Wave13Rules.setStage(b, 100, 3);
        assertEquals(3, Wave13Rules.stage(b, 23000));
        assertEquals(0, Wave13Rules.stage(b, 24500), "forgotten at dawn");
        assertFalse(Wave13Rules.askedToday(b, 24500));
        assertEquals(1, Wave13Rules.nextOffer(0));
        assertEquals(0, Wave13Rules.nextOffer(4), "Nodol after the fourth heart");
    }

    @Test
    void otherworldlyFoes() {
        assertTrue(Wave13Rules.otherworldly("minecraft", "zombie", true));
        assertTrue(Wave13Rules.otherworldly("minecraft", "blaze", false));
        assertFalse(Wave13Rules.otherworldly("minecraft", "cow", false));
        assertTrue(Wave13Rules.otherworldly("tensura", "lesser_daemon", false));
        assertTrue(Wave13Rules.otherworldly("tensura", "water_spirit", false));
        assertFalse(Wave13Rules.otherworldly("tensura", "goblin", false));
    }

    @Test
    void humblingAndBetrayalThresholds() {
        assertTrue(Wave13Rules.surrenders(5, 20));
        assertFalse(Wave13Rules.surrenders(10, 20));
        assertFalse(Wave13Rules.surrenders(1, 300), "a boss-sized foe never yields");
        assertTrue(Wave13Rules.eyeContact(0.9));
        assertFalse(Wave13Rules.eyeContact(-0.4));
        assertTrue(Wave13Rules.saveHolds(0.2));
        assertFalse(Wave13Rules.saveHolds(0.8));
    }

    @Test
    void eschatonChargesAndTiers() {
        assertEquals(0, Wave13Rules.tier(499));
        assertEquals(1, Wave13Rules.tier(500));
        assertEquals(2, Wave13Rules.tier(1500));
        assertEquals(3, Wave13Rules.tier(3000));
        assertEquals(4, Wave13Rules.tier(5000));
        assertEquals(5, Wave13Rules.tier(7500));
        assertEquals(0, Wave13Rules.gain(0.0));
        assertEquals(333, Wave13Rules.gain(0.10), 1);
        assertEquals(800L, Wave13Rules.gain(0.9), "one look gains at most eight charges");
        Wave13Rules.Bag b = bag();
        Wave13Rules.addCharge(b, 100, 9000);
        Wave13Rules.addCharge(b, 100, 9000);
        assertEquals(Wave13Rules.MAX_CHARGE_HUNDREDTHS, Wave13Rules.charge(b, 200));
        assertEquals(0L, Wave13Rules.charge(b, 25000), "charges are lost at dawn");
        Wave13Rules.clearCharge(b, 300);
        assertEquals(0L, Wave13Rules.charge(b, 300));
    }

    @Test
    void contingencyCircumstances() {
        var calm = new Wave13Rules.Snapshot(20, 20, false, 300, 0, 0, false, false);
        for (Wave13Rules.Trigger t : Wave13Rules.Trigger.values()) assertFalse(Wave13Rules.fires(t, calm), t.name());
        assertTrue(Wave13Rules.fires(Wave13Rules.Trigger.LOW_HEALTH, new Wave13Rules.Snapshot(5, 20, false, 300, 0, 0, false, false)));
        assertTrue(Wave13Rules.fires(Wave13Rules.Trigger.BURNING, new Wave13Rules.Snapshot(20, 20, true, 300, 0, 0, false, false)));
        assertTrue(Wave13Rules.fires(Wave13Rules.Trigger.DROWNING, new Wave13Rules.Snapshot(20, 20, false, 30, 0, 0, false, false)));
        assertTrue(Wave13Rules.fires(Wave13Rules.Trigger.FALLING, new Wave13Rules.Snapshot(20, 20, false, 300, 15, 0, false, false)));
        assertTrue(Wave13Rules.fires(Wave13Rules.Trigger.SURROUNDED, new Wave13Rules.Snapshot(20, 20, false, 300, 0, 3, false, false)));
        assertFalse(Wave13Rules.fires(Wave13Rules.Trigger.BURNING, new Wave13Rules.Snapshot(20, 20, true, 300, 0, 0, false, true)), "never for creative");
        assertEquals(Wave13Rules.Trigger.LOW_HEALTH, Wave13Rules.nextTrigger(Wave13Rules.Trigger.values().length - 1));
    }

    @Test
    void storedSpellRoundTrips() {
        List<Wave13Rules.Stored> in = List.of(new Wave13Rules.Stored("minecraft:water_breathing", 3600, 0), new Wave13Rules.Stored("minecraft:regeneration", 99999, 9));
        String text = Wave13Rules.encode(in);
        List<Wave13Rules.Stored> out = Wave13Rules.decode(text);
        assertEquals(2, out.size());
        assertEquals("minecraft:water_breathing", out.get(0).id());
        assertEquals(Wave13Rules.MAX_TICKS, out.get(1).ticks());
        assertEquals(Wave13Rules.MAX_AMP, out.get(1).amp());
        assertTrue(Wave13Rules.decode("garbage;;a@b@c").isEmpty());
        assertTrue(Wave13Rules.decode(null).isEmpty());
        Wave13Rules.Bag b = bag();
        assertFalse(Wave13Rules.spentToday(b, 50));
        Wave13Rules.markSpent(b, 50);
        assertTrue(Wave13Rules.spentToday(b, 23999));
        assertFalse(Wave13Rules.spentToday(b, 24001));
    }

    @Test
    void dadaoTiersOpenWithTime() {
        assertEquals(1, Wave13Rules.timeTier(0));
        assertEquals(2, Wave13Rules.timeTier(Wave13Rules.TIER2_TICKS));
        assertEquals(3, Wave13Rules.timeTier(Wave13Rules.TIER3_TICKS));
        assertEquals(Wave13Rules.TIER2_TICKS - 100, Wave13Rules.untilNextTier(100));
        assertEquals(0, Wave13Rules.untilNextTier(Wave13Rules.TIER3_TICKS + 5));
        Wave13Rules.Bag b = bag();
        assertEquals(1, Wave13Rules.usesLeft(b, "slow", 10, 1));
        assertTrue(Wave13Rules.useUp(b, "slow", 10, 1));
        assertFalse(Wave13Rules.useUp(b, "slow", 20, 1));
        assertTrue(Wave13Rules.useUp(b, "slow", 24100, 1), "a new day");
    }
}
