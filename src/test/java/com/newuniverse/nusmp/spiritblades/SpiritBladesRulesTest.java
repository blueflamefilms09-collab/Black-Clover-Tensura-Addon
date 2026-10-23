package com.newuniverse.nusmp.spiritblades;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** The caps of the two Wind Spirit blades (pure): the arrow trail, the hit bursts, the leaf maths of the hand effect. */
class SpiritBladesRulesTest {
    @Test
    void theTrailIsCappedPerOwnerAndOverTheServer() {
        assertTrue(SpiritBladesRules.canTrack(0, 0));
        assertTrue(SpiritBladesRules.canTrack(SpiritBladesRules.TRAIL_PER_OWNER - 1, SpiritBladesRules.TRAIL_GLOBAL - 1));
        assertFalse(SpiritBladesRules.canTrack(SpiritBladesRules.TRAIL_PER_OWNER, 0), "the owner's cap");
        assertFalse(SpiritBladesRules.canTrack(0, SpiritBladesRules.TRAIL_GLOBAL), "the server's cap");
        assertTrue(SpiritBladesRules.TRAIL_GLOBAL <= 32 && SpiritBladesRules.TRAIL_PER_OWNER <= 4);
    }

    @Test
    void theTrailPuffsAreSparseAndShortLived() {
        int puffs = 0;
        for (int age = 0; age <= SpiritBladesRules.TRAIL_MAX_AGE + 5; age++) if (SpiritBladesRules.puffDue(age)) puffs++;
        assertEquals(SpiritBladesRules.TRAIL_MAX_AGE / SpiritBladesRules.TRAIL_EVERY + 1, puffs);
        assertTrue(puffs <= 14, "a trailed arrow sends a few puffs in its whole life: " + puffs);
        assertFalse(SpiritBladesRules.puffDue(-1));
        assertTrue(SpiritBladesRules.trailOver(SpiritBladesRules.TRAIL_MAX_AGE + 1, 9.0), "too old");
        assertTrue(SpiritBladesRules.trailOver(10, 0.0), "stopped");
        assertFalse(SpiritBladesRules.trailOver(1, 0.0), "just shot: still flying out of the bow");
        assertFalse(SpiritBladesRules.trailOver(10, 4.0), "flying");
    }

    @Test
    void theWorstTrafficIsSmall() {
        assertTrue(SpiritBladesRules.worstTrailParticlesPerTick() <= 16, "trail particles a tick: " + SpiritBladesRules.worstTrailParticlesPerTick());
        assertTrue(SpiritBladesRules.worstHitParticlesPerTick() <= 24, "hit particles a tick: " + SpiritBladesRules.worstHitParticlesPerTick());
        assertTrue(SpiritBladesRules.HIT_PARTICLES <= 6);
        // every trailed arrow of a full server: puffs are capped per tick whatever the number of arrows
        assertTrue(SpiritBladesRules.TRAIL_PUFFS_PER_TICK < SpiritBladesRules.TRAIL_GLOBAL);
    }

    @Test
    void aHitBurstIsThrottledPerAttackerAndTheMapIsBounded() {
        Map<UUID, Long> last = new HashMap<>();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        assertTrue(SpiritBladesRules.hitBurstAllowed(last, a, 100));
        assertFalse(SpiritBladesRules.hitBurstAllowed(last, a, 100), "the same tick");
        assertFalse(SpiritBladesRules.hitBurstAllowed(last, a, 100 + SpiritBladesRules.HIT_GAP_TICKS - 1), "inside the gap");
        assertTrue(SpiritBladesRules.hitBurstAllowed(last, a, 100 + SpiritBladesRules.HIT_GAP_TICKS), "after the gap");
        assertTrue(SpiritBladesRules.hitBurstAllowed(last, b, 100), "another attacker is not throttled by the first");
        assertTrue(SpiritBladesRules.hitBurstAllowed(last, a, 5), "a clock that went back (a new world) does not lock the attacker out");
        // many attackers in one tick: the map never grows past its bound
        Map<UUID, Long> big = new HashMap<>();
        int allowed = 0;
        for (int i = 0; i < SpiritBladesRules.MAX_REMEMBERED * 3; i++) if (SpiritBladesRules.hitBurstAllowed(big, UUID.randomUUID(), 1000)) allowed++;
        assertTrue(big.size() <= SpiritBladesRules.MAX_REMEMBERED);
        assertEquals(SpiritBladesRules.MAX_REMEMBERED, allowed);
        // a later tick: the old entries are dropped and new attackers get through
        assertTrue(SpiritBladesRules.hitBurstAllowed(big, UUID.randomUUID(), 1000 + SpiritBladesRules.HIT_GAP_TICKS));
        assertTrue(big.size() <= SpiritBladesRules.MAX_REMEMBERED);
    }

    @Test
    void theSwordIsNetheriteLevelAndTheBowIsVanillas() {
        assertEquals(3.0f, SpiritBladesRules.SWORD_BASE_DAMAGE, 0f, "netherite adds 4: 3 + 4 = 7, a netherite sword's");
        assertEquals(-2.4f, SpiritBladesRules.SWORD_SPEED, 0f, "1.6 swings a second, a netherite sword's");
        assertEquals(384, SpiritBladesRules.BOW_DURABILITY, "vanilla's bow");
    }

    @Test
    void theLeavesAreCappedBoundedAndFadeInAndOut() {
        float[] o = new float[SpiritBladesFxMath.FIELDS];
        assertEquals(6, SpiritBladesFxMath.MAX_LEAVES);
        assertEquals(24, SpiritBladesFxMath.worstQuads());
        int visible = 0, total = 0;
        for (float t = 0; t < 2000f; t += 0.37f) {
            for (int i = 0; i < SpiritBladesFxMath.MAX_LEAVES; i++) {
                SpiritBladesFxMath.leaf(i, t, o);
                total++;
                assertTrue(o[0] >= 0.1f && o[0] <= 1.0f, "along " + o[0]);
                double r = Math.hypot(o[1], o[2]);
                assertTrue(r <= SpiritBladesFxMath.MAX_RADIUS + 1e-4, "radius " + r);
                assertTrue(o[3] > 0.015f && o[3] < 0.06f, "size " + o[3]);
                assertTrue(o[4] >= 0f && o[4] <= 1f, "alpha " + o[4]);
                if (o[4] > 0.5f) visible++;
            }
        }
        assertTrue(visible > total * 2 / 5 && visible < total * 9 / 10, "a leaf is bright for most of its life but fades in and out: " + visible + " of " + total);
        // a leaf is invisible the instant it is reborn (no pop)
        for (int i = 0; i < SpiritBladesFxMath.MAX_LEAVES; i++) {
            float born = (float) ((5.0 - (double) i / SpiritBladesFxMath.MAX_LEAVES) * SpiritBladesFxMath.PERIOD_TICKS);
            SpiritBladesFxMath.leaf(i, born, o);
            assertTrue(o[4] < 0.1f, "a leaf appears from nothing: " + o[4]);
        }
    }
}
