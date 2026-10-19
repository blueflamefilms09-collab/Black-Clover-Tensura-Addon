package com.newuniverse.nusmp.book;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the four pages Mercury Magic had keep their ids and order (saves and mastery bits), four new ones follow; the counts (spears, bullets, cage volley), the cage radius and the beam reach stay in
 * bounds, and the Silver Guardian's toxin ("this spell harms the user while in use") can never kill: it stops at its floor and never touches a creative player.
 */
class MercuryRulesTest {
    @Test
    void oldPagesKeepTheirIdsAndOrder() {
        assertEquals(List.of("silver_blade", "mercury_shield", "mercury_rain", "silver_eagle"), MercuryRules.OLD_IDS);
        assertEquals(List.of("silver_bullet", "silver_spear", "silver_shining_regal_spear", "silver_star_of_execution"), MercuryRules.NEW_IDS);
        var all = new HashSet<String>(MercuryRules.OLD_IDS);
        all.addAll(MercuryRules.NEW_IDS);
        assertEquals(8, all.size());
    }

    @Test
    void countsGrowWithPowerWithinBounds() {
        assertEquals(1, MercuryRules.spearCount(0.8f));
        assertEquals(3, MercuryRules.spearCount(2.4f));
        assertEquals(5, MercuryRules.bulletCount(0.8f));
        assertEquals(8, MercuryRules.bulletCount(2.4f));
        assertEquals(2, MercuryRules.starVolley(0.8f));
        assertEquals(5, MercuryRules.starVolley(2.4f));
        for (float p = 0.8f; p < 2.4f; p += 0.1f) {
            assertTrue(MercuryRules.spearCount(p + 0.1f) >= MercuryRules.spearCount(p));
            assertTrue(MercuryRules.bulletCount(p + 0.1f) >= MercuryRules.bulletCount(p));
            assertTrue(MercuryRules.starVolley(p + 0.1f) >= MercuryRules.starVolley(p));
        }
    }

    @Test
    void cageAndBeamStayInsideTheirBounds() {
        assertEquals(4.5f, MercuryRules.starRadius(0.8f), 1e-6);
        assertEquals(7f, MercuryRules.starRadius(10f), 1e-6);
        assertTrue(MercuryRules.starRadius(2.4f) > MercuryRules.starRadius(0.8f));
        assertTrue(MercuryRules.regalReach(0.8f) >= 20 && MercuryRules.regalReach(10f) <= 30);
    }

    @Test
    void theToxinNeverKills() {
        assertFalse(MercuryRules.selfHarmAllowed(MercuryRules.SELF_HARM_FLOOR, false));
        assertFalse(MercuryRules.selfHarmAllowed(2f, false));
        assertTrue(MercuryRules.selfHarmAllowed(MercuryRules.SELF_HARM_FLOOR + 0.1f, false));
        assertFalse(MercuryRules.selfHarmAllowed(20f, true));
        float hp = 20f;
        for (int s = 0; s < 100 && MercuryRules.selfHarmAllowed(hp, false); s++) hp -= MercuryRules.SELF_HARM_PER_SECOND;
        assertTrue(hp > 5f, "the toxin must stop above the floor, left " + hp);
    }

    @Test
    void shortArmourOutlastsTheShatterWindow() {
        assertTrue(MercuryRules.SHORT_ARMOUR_TICKS >= 40);
        assertTrue(MercuryRules.GUARDIAN_TICKS > MercuryRules.SHORT_ARMOUR_TICKS);
    }
}
