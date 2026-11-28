package com.newuniverse.nusmp.arc.kakashi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why (5.8.0): the eye-rip mini-game must be fair and bounded: three windows, a marker that stays on the bar, a zone that can be hit, a bad pull that resets, and a cap on who may play. */
class EyeRipRulesTest {
    @Test
    void threeWindowsOneForEachNewPhase() {
        int n = 0;
        for (int phase = 0; phase < 6; phase++) if (EyeRipRules.windowFor(phase)) n++;
        assertEquals(EyeRipRules.WINDOWS, n);
        assertFalse(EyeRipRules.windowFor(0), "the start of the fight is no window");
    }

    @Test
    void theMarkerSweepsTheWholeBarAndBack() {
        boolean left = false, right = false;
        for (long t = -5; t < EyeRipRules.PERIOD * 3L; t++) {
            int m = EyeRipRules.marker(t);
            assertTrue(m >= 0 && m < EyeRipRules.WIDTH);
            left |= m == 0;
            right |= m == EyeRipRules.WIDTH - 1;
        }
        assertTrue(left && right);
        assertEquals(EyeRipRules.marker(3), EyeRipRules.marker(3 + EyeRipRules.PERIOD), "the sweep repeats");
    }

    @Test
    void theZoneIsAlwaysOnTheBarAndMovesBetweenPulls() {
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (long seed = 0; seed < 50; seed++) for (int pull = 0; pull < 6; pull++) {
            int z = EyeRipRules.zoneStart(seed * 977L, pull);
            assertTrue(z >= 0 && z + EyeRipRules.ZONE <= EyeRipRules.WIDTH, "zone " + z);
            seen.add(z);
        }
        assertTrue(seen.size() > 5, "the zone is not always in the same place");
    }

    @Test
    void cleanPullsRipTheEyeAndABadPullStartsAgain() {
        int z = 4;
        EyeRipRules.Pull p = EyeRipRules.pull(5, z, 0, 3);
        assertTrue(p.clean() && !p.ripped() && p.progress() == 1);
        p = EyeRipRules.pull(5, z, p.progress(), 3);
        p = EyeRipRules.pull(5, z, p.progress(), 3);
        assertTrue(p.ripped() && p.progress() == 3);
        EyeRipRules.Pull bad = EyeRipRules.pull(0, z, 2, 3);
        assertTrue(!bad.clean() && !bad.ripped() && bad.progress() == 0);
        assertTrue(EyeRipRules.pull(z + EyeRipRules.ZONE, z, 0, 1).progress() == 0, "the cell after the zone is a miss");
        assertTrue(EyeRipRules.pull(z, z, 0, 0).ripped(), "a broken need still means at least one pull");
    }

    @Test
    void whoMayBeginAndHowFastTheyMayClick() {
        assertTrue(EyeRipRules.canBegin(true, true, false, 0, 4, false));
        assertFalse(EyeRipRules.canBegin(false, true, false, 0, 4, false), "setting off");
        assertFalse(EyeRipRules.canBegin(true, false, false, 0, 4, false), "no window");
        assertFalse(EyeRipRules.canBegin(true, true, true, 0, 4, false), "already has the skill");
        assertFalse(EyeRipRules.canBegin(true, true, false, 4, 4, false), "table full");
        assertTrue(EyeRipRules.canBegin(true, true, false, 4, 4, true), "a player already at the table keeps their place");
        assertFalse(EyeRipRules.clickAllowed(103, 100));
        assertTrue(EyeRipRules.clickAllowed(100 + EyeRipRules.CLICK_GAP, 100));
    }

    @Test
    void theBarShowsTheZoneAndTheMarker() {
        String bar = EyeRipRules.bar(2, 10);
        assertEquals(EyeRipRules.WIDTH, bar.length());
        assertEquals('|', bar.charAt(2));
        assertEquals(EyeRipRules.ZONE, bar.chars().filter(ch -> ch == '=').count());
    }
}
