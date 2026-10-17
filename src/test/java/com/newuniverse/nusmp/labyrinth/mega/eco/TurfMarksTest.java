package com.newuniverse.nusmp.labyrinth.mega.eco;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the marker strings are copied from the systems that set them, so a wrong letter would let a boss or a Paladin into a goblin quarrel without
 * any error. The values are pinned here (the data test also checks that each string still appears in the source of the system that sets it), and
 * the matcher is checked for the cases the game hits: no tags at all, a tag that matches, and tags that do not.
 */
class TurfMarksTest {
    @Test void bossMarkersAreTheOnesTheModSets() {
        for (String t : new String[] {"nusmp_peak_boss", "nusmp_wanderer", "nusmp_floor_boss", "nusmp_juggernaut", "nusmp_charybdis"})
            assertTrue(TurfMarks.BOSS_TAGS.contains(t), t);
        assertEquals(5, TurfMarks.BOSS_TAGS.size());
        assertEquals("nusmp:floor_warden", TurfMarks.WARDEN_ID);
    }

    @Test void servantMarkersAreTheOnesTheModSets() {
        for (String t : new String[] {"nusmp_underworld", "nusmp_servant"}) assertTrue(TurfMarks.SERVANT_TAGS.contains(t), t);
        for (String k : new String[] {"nusmp_paladin", "nusmp_paladin_of", "nusmp_paladin_turn", "nusmp_shadow", "nusmp_relic_owner", "nusmp_bull_owner"})
            assertTrue(TurfMarks.SERVANT_KEYS.contains(k), k);
    }

    @Test void theMatcherHandlesEmptyAndNull() {
        assertFalse(TurfMarks.anyOf(null, TurfMarks.BOSS_TAGS));
        assertFalse(TurfMarks.anyOf(Set.of(), TurfMarks.BOSS_TAGS));
        assertFalse(TurfMarks.anyOf(List.of("nusmp_dm", "pet"), TurfMarks.BOSS_TAGS));
        assertTrue(TurfMarks.anyOf(List.of("nusmp_dm", "nusmp_wanderer"), TurfMarks.BOSS_TAGS));
        assertTrue(TurfMarks.anyOf(Set.of("nusmp_underworld"), TurfMarks.SERVANT_TAGS));
    }

    @Test void aWanderingBossIsNotAServantAndAServantIsNotABoss() {
        for (String b : TurfMarks.BOSS_TAGS) assertFalse(TurfMarks.SERVANT_TAGS.contains(b) || TurfMarks.SERVANT_KEYS.contains(b), b);
    }
}
