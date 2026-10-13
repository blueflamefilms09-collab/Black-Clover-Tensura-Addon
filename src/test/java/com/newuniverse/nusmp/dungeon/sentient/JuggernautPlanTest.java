package com.newuniverse.nusmp.dungeon.sentient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** At most three Juggernauts at once, a cooldown per player, health that never passes the attribute's ceiling. */
class JuggernautPlanTest {
    @Test
    void atMostThreeAreAliveOrOnTheWayAtOnce() {
        assertTrue(JuggernautPlan.mayBirth(0, 0, 100, 0));
        assertTrue(JuggernautPlan.mayBirth(2, 0, 100, 0));
        assertFalse(JuggernautPlan.mayBirth(3, 0, 100, 0));
        assertFalse(JuggernautPlan.mayBirth(2, 1, 100, 0), "one on the way counts");
        assertFalse(JuggernautPlan.mayBirth(0, 3, 100, 0));
        assertTrue(JuggernautPlan.mayBirth(1, 1, 100, 0));
        assertEquals(3, JuggernautPlan.MAX_ALIVE);
    }

    @Test
    void aPlayerWhoDrewOneMustWaitOutTheCooldown() {
        long now = 50_000;
        long until = JuggernautPlan.cooldownUntil(now, 10);
        assertEquals(now + 12_000, until, "ten minutes");
        assertFalse(JuggernautPlan.mayBirth(0, 0, now, until));
        assertFalse(JuggernautPlan.mayBirth(0, 0, until - 1, until));
        assertTrue(JuggernautPlan.mayBirth(0, 0, until, until));
        assertEquals(now, JuggernautPlan.cooldownUntil(now, 0));
        assertEquals(now, JuggernautPlan.cooldownUntil(now, -4));
    }

    @Test
    void healthStaysUnderTheAttributeCeiling() {
        double wanted = JuggernautPlan.profile(FloorKind.CATACOMBS).healthExtra();
        assertEquals(3.0, wanted, 1e-9, "four times the health");
        for (double base : new double[]{1, 4, 20, 30, 80, 150, 255, 256, 300, 500, 1000, 1023, 1024, 1500, 5000}) {
            double extra = JuggernautPlan.healthExtra(base, wanted);
            assertTrue(extra >= 0.0, "base " + base);
            assertTrue(extra <= wanted + 1e-9, "never more than wanted");
            assertTrue(base * (1.0 + extra) <= Math.max(base, JuggernautPlan.HEALTH_CAP) + 1e-6, "base " + base + " total " + base * (1.0 + extra) + " (a base already over the ceiling gets nothing added)");
        }
        assertEquals(3.0, JuggernautPlan.healthExtra(100, 3.0), 1e-9, "room enough: all of it");
        assertEquals(1.0, JuggernautPlan.healthExtra(512, 3.0), 1e-9, "512 doubles to the ceiling");
        assertEquals(0.0, JuggernautPlan.healthExtra(1024, 3.0), 1e-9);
        assertEquals(0.0, JuggernautPlan.healthExtra(0, 3.0), 1e-9);
    }

    @Test
    void theProfileIsTheOwnersNumbers() {
        for (FloorKind kind : FloorKind.values()) {
            JuggernautPlan.Profile p = JuggernautPlan.profile(kind);
            assertEquals(3.0, p.healthExtra(), 1e-9);
            assertEquals(1.2, p.damageExtra(), 1e-9, "attack damage x2.2");
            assertEquals(0.12, p.speedExtra(), 1e-9);
            assertEquals(6.0, p.armor(), 1e-9);
            assertEquals(0.6, p.knockback(), 1e-9);
            assertEquals(64.0, p.followRange(), 1e-9);
        }
    }

    @Test
    void everyFloorHasATagAndAFallbackList() {
        assertEquals("dungeon/juggernaut_catacombs", JuggernautPlan.tagPath(FloorKind.CATACOMBS));
        assertEquals("dungeon/juggernaut_waterways", JuggernautPlan.tagPath(FloorKind.WATERWAYS));
        assertEquals("dungeon/juggernaut_underworld", JuggernautPlan.tagPath(FloorKind.UNDERWORLD));
        assertEquals("dungeon/juggernaut_catacombs", JuggernautPlan.tagPath(FloorKind.LEGACY), "anything else uses the Catacombs'");
        for (FloorKind kind : FloorKind.values()) assertFalse(JuggernautPlan.defaults(kind).isEmpty(), kind.name());
        assertTrue(JuggernautPlan.defaults(FloorKind.WATERWAYS).contains("tensura:megalodon"));
        assertTrue(JuggernautPlan.defaults(FloorKind.UNDERWORLD).contains("tensura:greater_daemon"));
        assertTrue(JuggernautPlan.defaults(FloorKind.CATACOMBS).contains("tensura:giant_bear"));
    }

    @Test
    void theCreaturesOfTheWaterMustBeBornInWater() {
        assertTrue(JuggernautPlan.needsWater("megalodon"));
        assertTrue(JuggernautPlan.needsWater("sissie"));
        assertFalse(JuggernautPlan.needsWater("giant_bear"));
        assertFalse(JuggernautPlan.needsWater("greater_daemon"));
    }
}
