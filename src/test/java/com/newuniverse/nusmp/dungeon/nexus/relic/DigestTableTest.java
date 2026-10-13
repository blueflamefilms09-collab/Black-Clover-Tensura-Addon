package com.newuniverse.nusmp.dungeon.nexus.relic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** What the Glutton's Chalice makes of raw monster meat (docs/living_dungeon.md section 7). */
class DigestTableTest {
    @Test
    void theMealIsAStewWithImmunity() {
        assertEquals(2, DigestTable.nutrition("raw_slime_jelly"));
        assertEquals(5, DigestTable.nutrition("raw_basilisk_fillet"));
        assertEquals(DigestTable.OTHER, DigestTable.nutrition("something_new"));
        assertEquals(10, DigestTable.foodGain(5));
        assertEquals(20, DigestTable.foodAfter(15, 5), "capped at a full stomach");
        assertEquals(14, DigestTable.foodAfter(10, 2));
        assertEquals(1800 + 300 * 5, DigestTable.immunityTicks(5));
    }

    @Test
    void immunityStacksButIsCapped() {
        long now = 1000;
        long until = DigestTable.extend(now, 0, 5);
        assertEquals(now + 3300, until);
        long twice = DigestTable.extend(now, until, 5);
        assertEquals(now + 6600, twice);
        long many = until;
        for (int i = 0; i < 20; i++) many = DigestTable.extend(now, many, 5);
        assertEquals(now + DigestTable.IMMUNITY_CAP, many);
        assertTrue(DigestTable.active(now, many));
        assertFalse(DigestTable.active(many, many));
        assertFalse(DigestTable.active(now, now + 10 * DigestTable.IMMUNITY_CAP), "an end stamp from a clock that ran back is not an immunity");
    }
}
