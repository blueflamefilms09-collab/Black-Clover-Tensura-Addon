package com.newuniverse.nusmp.valkyrielance;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the Valkyrie Drill Lance is an honest spear with a capped hit burst; its numbers and the bound of its attacker map are pure and pinned here. */
class ValkyrieLanceRulesTest {
    @Test
    void theNumbersAreATridentsAndTheBurstIsSmall() {
        assertEquals("valkyrie_drill_lance", ValkyrieLanceRules.ID);
        assertEquals(8.0f, ValkyrieLanceRules.BASE_DAMAGE + 2.0f, 1e-6f, "iron's 2 on top: a trident's 8 (9 with the wielder's own 1)");
        assertEquals(-2.9f, ValkyrieLanceRules.SPEED, 1e-6f);
        assertEquals(32, ValkyrieLanceRules.worstHitParticlesPerTick(), "at most 4 bursts of 8 drops a tick over the whole server");
        assertTrue(ValkyrieLanceRules.HIT_PARTICLES <= 8 && ValkyrieLanceRules.HIT_BURSTS_PER_TICK <= 4);
    }

    @Test
    void anAttackerThrowsOneBurstPerGapAndTheMapIsBounded() {
        Map<UUID, Long> map = new HashMap<>();
        UUID a = UUID.randomUUID();
        assertTrue(ValkyrieLanceRules.hitBurstAllowed(map, a, 100));
        assertFalse(ValkyrieLanceRules.hitBurstAllowed(map, a, 101));
        assertFalse(ValkyrieLanceRules.hitBurstAllowed(map, a, 103));
        assertTrue(ValkyrieLanceRules.hitBurstAllowed(map, a, 100 + ValkyrieLanceRules.HIT_GAP_TICKS));
        for (int i = 0; i < ValkyrieLanceRules.MAX_REMEMBERED * 4; i++) ValkyrieLanceRules.hitBurstAllowed(map, UUID.randomUUID(), 5000);
        assertTrue(map.size() <= ValkyrieLanceRules.MAX_REMEMBERED, "the map never grows past its bound: " + map.size());
        for (int i = 0; i < ValkyrieLanceRules.MAX_REMEMBERED * 2; i++) ValkyrieLanceRules.hitBurstAllowed(map, UUID.randomUUID(), 9000 + i);
        assertTrue(map.size() <= ValkyrieLanceRules.MAX_REMEMBERED);
    }
}
