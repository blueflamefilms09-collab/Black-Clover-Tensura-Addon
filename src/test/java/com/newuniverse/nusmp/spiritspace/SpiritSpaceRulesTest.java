package com.newuniverse.nusmp.spiritspace;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** The pure rules of the Spirit of Space: caps, ranges, the channel, the prop word, the projectile selection, the damage ceiling, the ledgers, the sound gate and the landing search on a fake world. */
class SpiritSpaceRulesTest {
    // ------------------------------------------------------------------ the prop word and the packet
    @Test
    void thePropWordKeepsTheVariantTheHiddenBitAndTheStamp() {
        int p = SpiritSpaceRules.param(SpiritSpaceRules.V_WISP, 12345);
        assertEquals(SpiritSpaceRules.V_WISP, SpiritSpaceRules.variant(p));
        assertEquals(12345, SpiritSpaceRules.extra(p));
        assertFalse(SpiritSpaceRules.hidden(p));
        assertTrue(SpiritSpaceRules.hidden(p | SpiritSpaceRules.HIDDEN));
        assertEquals(SpiritSpaceRules.V_WISP, SpiritSpaceRules.variant(p | SpiritSpaceRules.HIDDEN));
        assertEquals(1, SpiritSpaceRules.stampOf(0) , "a stamp is never 0 (0 means none)");
        assertEquals(Integer.MAX_VALUE, SpiritSpaceRules.sinceStamp(500, 0));
        assertEquals(30, SpiritSpaceRules.sinceStamp(130, SpiritSpaceRules.stampOf(100)));
        assertEquals(30, SpiritSpaceRules.sinceStamp((1 << 20) + 10, SpiritSpaceRules.stampOf((1 << 20) - 20)), "the 20 bit stamp wraps cleanly");
    }

    @Test
    void thePacketActionsAbilitiesAndSlotsAreValidated() {
        assertTrue(SpiritSpaceRules.validAction(0) && SpiritSpaceRules.validAction(1));
        assertFalse(SpiritSpaceRules.validAction(2) || SpiritSpaceRules.validAction(255) || SpiritSpaceRules.validAction(-1));
        assertTrue(SpiritSpaceRules.validAbility(0) && SpiritSpaceRules.validAbility(3));
        assertFalse(SpiritSpaceRules.validAbility(4) || SpiritSpaceRules.validAbility(-1));
        assertTrue(SpiritSpaceRules.validSlot(0, 3) && SpiritSpaceRules.validSlot(2, 3));
        assertFalse(SpiritSpaceRules.validSlot(3, 3) || SpiritSpaceRules.validSlot(-1, 3));
        assertTrue(SpiritSpaceRules.gapOk(18, 10, 8) && !SpiritSpaceRules.gapOk(17, 10, 8));
    }

    // ------------------------------------------------------------------ the portal rules
    @Test
    void theRangeIsInclusiveAndTheChannelBreaksOnlyPastTheLimit() {
        assertTrue(SpiritSpaceRules.inRange(0, 0, 0, 96, 0, 0, 96));
        assertFalse(SpiritSpaceRules.inRange(0, 0, 0, 96.1, 0, 0, 96));
        assertTrue(SpiritSpaceRules.inRange(0, 0, 0, 60, 60, 0, 96), "the range is a 3D distance");
        assertFalse(SpiritSpaceRules.channelBroken(0, 0, 0, 1.5, 0, 0));
        assertTrue(SpiritSpaceRules.channelBroken(0, 0, 0, 1.6, 0, 0));
        assertFalse(SpiritSpaceRules.channelDone(129, 100));
        assertTrue(SpiritSpaceRules.channelDone(130, 100));
        assertEquals(30, SpiritSpaceRules.CHANNEL_TICKS, "a 1.5 second channel");
        assertTrue(SpiritSpaceRules.PORTAL_COOLDOWN_DEFAULT >= 400 && SpiritSpaceRules.PORTAL_COOLDOWN_DEFAULT <= 600, "a 20 to 30 second cooldown");
        assertTrue(SpiritSpaceRules.RANGE_DEFAULT >= 64 && SpiritSpaceRules.RANGE_DEFAULT <= 128, "short or medium range only");
        assertTrue(SpiritSpaceRules.RANGE_HARD <= 160, "no config can make it long range");
    }

    @Test
    void everyHardCapIsAtLeastItsDefaultAndTheClampHoldsIt() {
        assertTrue(SpiritSpaceRules.WAYPOINTS_DEFAULT <= SpiritSpaceRules.WAYPOINTS_HARD && SpiritSpaceRules.SPIRITS_DEFAULT <= SpiritSpaceRules.SPIRITS_HARD
                && SpiritSpaceRules.WISPS_DEFAULT <= SpiritSpaceRules.WISPS_HARD && SpiritSpaceRules.PAIRS_DEFAULT <= SpiritSpaceRules.PAIRS_HARD
                && SpiritSpaceRules.GRAVITY_TARGETS_DEFAULT <= SpiritSpaceRules.GRAVITY_TARGETS_HARD && SpiritSpaceRules.CUT_PROJECTILES_DEFAULT <= SpiritSpaceRules.CUT_PROJECTILES_HARD
                && SpiritSpaceRules.GRAVITY_RADIUS_DEFAULT <= SpiritSpaceRules.GRAVITY_RADIUS_HARD && SpiritSpaceRules.CUT_RADIUS_DEFAULT <= SpiritSpaceRules.CUT_RADIUS_HARD);
        assertEquals(48, SpiritSpaceRules.SPIRITS_DEFAULT);
        assertEquals(6, SpiritSpaceRules.WISPS_DEFAULT);
        assertEquals(1, SpiritSpaceRules.SPIRITS_PER_PLAYER);
        assertEquals(1, SpiritSpaceRules.WISPS_PER_PLAYER);
        assertEquals(1, SpiritSpaceRules.PAIRS_PER_PLAYER);
        assertEquals(10, SpiritSpaceRules.clamp(500, 1, 10));
        assertEquals(1, SpiritSpaceRules.clamp(-5, 1, 10));
        assertEquals(2.5, SpiritSpaceRules.clamp(2.5, 1.0, 10.0));
    }

    // ------------------------------------------------------------------ the projectile cut and the damage
    @Test
    void theNearestSelectionPicksTheNearestFirstAndNeverMoreThanTheCap() {
        double[] d = {25, 1, 9, 4, 16, 36};
        int[] out = new int[6];
        assertEquals(3, SpiritSpaceRules.nearest(d, 6, 3, out));
        assertEquals("[1, 3, 2]", java.util.Arrays.toString(new int[]{out[0], out[1], out[2]}));
        assertEquals(0, SpiritSpaceRules.nearest(d, 6, 0, out));
        assertEquals(6, SpiritSpaceRules.nearest(d, 6, 99, out), "the cap above the count gives all of them");
        assertEquals(2, SpiritSpaceRules.nearest(d, 6, 99, new int[2]), "and never more than the output holds");
        assertEquals(0, SpiritSpaceRules.nearest(new double[0], 0, 5, out));
        int[] ties = new int[3];
        assertEquals(3, SpiritSpaceRules.nearest(new double[]{4, 4, 4}, 3, 3, ties));
        assertEquals(3, java.util.Arrays.stream(ties).distinct().count(), "equal distances still give distinct entries");
        assertEquals(12, SpiritSpaceRules.deleteCount(500, 12));
        assertEquals(0, SpiritSpaceRules.deleteCount(-3, 12));
        assertEquals(5, SpiritSpaceRules.deleteCount(5, 12));
    }

    @Test
    void theCutDamageIsBoundedAndNeverNegative() {
        assertEquals(10.0, SpiritSpaceRules.cutDamage(10, 1, 1, 1), 1e-9);
        assertEquals(SpiritSpaceRules.CUT_HIT_CEILING, SpiritSpaceRules.cutDamage(60, 12, 5, 5), 1e-9, "the absolute ceiling holds after every multiplier");
        assertEquals(0.0, SpiritSpaceRules.cutDamage(-5, 1, 1, 1), 1e-9);
        assertEquals(0.0, SpiritSpaceRules.cutDamage(10, 1, 0, 1), 1e-9);
        assertTrue(SpiritSpaceRules.CUT_DAMAGE_DEFAULT <= SpiritSpaceRules.CUT_DAMAGE_HARD);
    }

    // ------------------------------------------------------------------ the anchors
    @Test
    void theOrbCirclesTheOwnerAtAFixedRadiusAndHeight() {
        double[] o = new double[3];
        for (int age = 0; age < 400; age += 7) {
            SpiritSpaceRules.orbitTo(o, 10, 64, -5, 33, age);
            double r = Math.hypot(o[0] - 10, o[2] + 5);
            assertEquals(SpiritSpaceRules.ORBIT_RADIUS, r, 1e-9);
            assertTrue(Math.abs(o[1] - (64 + SpiritSpaceRules.ORBIT_HEIGHT)) <= SpiritSpaceRules.ORBIT_BOB + 1e-9);
        }
    }

    @Test
    void theWispHangsBehindAndAboveTheOwnerWhateverTheirYaw() {
        double[] o = new double[3], m = new double[3];
        for (int yaw = -180; yaw <= 180; yaw += 45) {
            SpiritSpaceRules.wispTo(o, 0, 0, 0, yaw);
            assertEquals(1.7, Math.hypot(o[0], o[2]), 1e-9);
            assertEquals(3.0, o[1], 1e-9);
            double fx = -Math.sin(Math.toRadians(yaw)), fz = Math.cos(Math.toRadians(yaw));
            assertTrue(o[0] * fx + o[2] * fz < 0, "behind the owner: the anchor lies against the facing");
            SpiritSpaceRules.mouthTo(m, 0, 0, 0, yaw);
            assertTrue(m[0] * fx + m[2] * fz > 0, "the mouth is out in front");
        }
    }

    // ------------------------------------------------------------------ the ledgers and the sound gate
    @Test
    void theLedgerHoldsThePerPlayerAndTheServerCapAndGivesSlotsBack() {
        SpaceLedger l = new SpaceLedger();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        assertTrue(l.acquire(a, 1, 2));
        assertFalse(l.acquire(a, 1, 2), "one per player");
        assertTrue(l.acquire(b, 1, 2));
        assertFalse(l.acquire(c, 1, 2), "two on the server");
        assertEquals(2, l.total());
        l.release(a);
        assertTrue(l.acquire(c, 1, 2));
        l.forget(b);
        l.forget(b);
        assertEquals(1, l.count(c));
        assertEquals(1, l.total());
        l.release(UUID.randomUUID());
        assertEquals(1, l.total(), "releasing a stranger changes nothing");
        assertFalse(l.acquire(null, 1, 2));
        assertFalse(l.acquire(a, 0, 2));
        l.clear();
        assertEquals(0, l.total());
    }

    @Test
    void theSoundGateAppliesAGapPerPlayerAndKindAndACapPerTick() {
        SoundGate g = new SoundGate();
        UUID a = UUID.randomUUID();
        assertTrue(g.allow(a, SoundGate.Kind.PING, 100));
        assertFalse(g.allow(a, SoundGate.Kind.PING, 105), "the same sound inside its gap is dropped");
        assertTrue(g.allow(a, SoundGate.Kind.TEAR, 105), "another kind is its own gap");
        assertTrue(g.allow(a, SoundGate.Kind.PING, 110));
        for (int i = 0; i < SoundGate.PER_TICK; i++) g.allow(UUID.randomUUID(), SoundGate.Kind.HUM, 500);
        assertFalse(g.allow(UUID.randomUUID(), SoundGate.Kind.HUM, 500), "at most PER_TICK sounds in one tick");
        assertTrue(g.allow(UUID.randomUUID(), SoundGate.Kind.HUM, 501), "the next tick refills");
        g.forget(a);
        assertTrue(g.allow(a, SoundGate.Kind.PING, 111), "a forgotten player starts clean");
        assertFalse(g.allow(null, SoundGate.Kind.PING, 111));
        assertTrue(SoundGate.Kind.VOICE.gap >= 60);
    }

    // ------------------------------------------------------------------ the landing search on a fake world
    /** A flat world: stone up to y = 63 everywhere, air above, with holes the test can dig. */
    private static class Flat implements SpaceLanding.Probe {
        final java.util.Set<String> solid = new java.util.HashSet<>(), lava = new java.util.HashSet<>(), air = new java.util.HashSet<>();
        int calls;
        String k(int x, int y, int z) { return x + "," + y + "," + z; }
        @Override public boolean solid(int x, int y, int z) { calls++; return !air.contains(k(x, y, z)) && !lava.contains(k(x, y, z)) && (y <= 63 || solid.contains(k(x, y, z))); }
        @Override public boolean passable(int x, int y, int z) { calls++; return !solid(x, y, z) && !lava.contains(k(x, y, z)); }
        @Override public boolean hazard(int x, int y, int z) { calls++; return lava.contains(k(x, y, z)); }
        @Override public int minY() { return -64; }
        @Override public int maxY() { return 320; }
    }

    @Test
    void aFlatPlaceIsItsOwnLanding() {
        assertEquals("[5, 64, 5]", java.util.Arrays.toString(SpaceLanding.find(new Flat(), 5, 64, 5)));
        assertEquals("[5, 64, 5]", java.util.Arrays.toString(SpaceLanding.find(new Flat(), 5, 66, 5)), "a waypoint a little above the ground finds the ground");
        assertEquals("[5, 64, 5]", java.util.Arrays.toString(SpaceLanding.find(new Flat(), 5, 62, 5)), "and one sunk in the ground finds the surface");
    }

    @Test
    void aWaypointInsideARockMovesUpAndOverLavaOrVoidIsRefused() {
        Flat f = new Flat();
        for (int y = 64; y <= 66; y++) f.solid.add(f.k(0, y, 0));                          // a three block pillar at the waypoint
        int[] at = SpaceLanding.find(f, 0, 64, 0);
        assertNotNull(at);
        assertTrue(SpaceLanding.safe(f, at[0], at[1], at[2]));
        Flat lava = new Flat();
        lava.lava.add(lava.k(0, 63, 0));                                                   // the ground under the waypoint is lava: it stands on the next safe cell
        int[] beside = SpaceLanding.find(lava, 0, 64, 0);
        assertNotNull(beside);
        assertFalse(beside[0] == 0 && beside[2] == 0, "never on the lava");
        SpaceLanding.Probe void_ = new SpaceLanding.Probe() {
            @Override public boolean solid(int x, int y, int z) { return false; }
            @Override public boolean passable(int x, int y, int z) { return true; }
            @Override public boolean hazard(int x, int y, int z) { return false; }
            @Override public int minY() { return -64; }
            @Override public int maxY() { return 320; }
        };
        assertNull(SpaceLanding.find(void_, 0, 64, 0), "over the void there is no landing");
    }

    @Test
    void anUnloadedPlaceAnswersNotSafeAndTheSearchStaysBounded() {
        SpaceLanding.Probe unloaded = new SpaceLanding.Probe() {
            int calls;
            @Override public boolean solid(int x, int y, int z) { calls++; return false; }
            @Override public boolean passable(int x, int y, int z) { calls++; return false; }
            @Override public boolean hazard(int x, int y, int z) { calls++; return true; }
            @Override public int minY() { return -64; }
            @Override public int maxY() { return 320; }
        };
        assertNull(SpaceLanding.find(unloaded, 0, 64, 0));
        Flat worst = new Flat();
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) for (int y = 56; y <= 72; y++) worst.lava.add(worst.k(x, y, z));
        assertNull(SpaceLanding.find(worst, 0, 64, 0));
        assertTrue(worst.calls <= SpaceLanding.maxProbeCalls(), "the search asked " + worst.calls + " times, the bound is " + SpaceLanding.maxProbeCalls());
        assertEquals(25, SpaceLanding.MAX_COLUMNS);
    }

    @Test
    void theHeightLimitsAreRespected() {
        Flat f = new Flat();
        assertFalse(SpaceLanding.safe(f, 0, -64, 0), "feet at the very bottom are not safe");
        assertFalse(SpaceLanding.safe(f, 0, 319, 0), "and no head room at the very top");
        assertTrue(SpaceLanding.safe(f, 0, 64, 0));
    }
}
