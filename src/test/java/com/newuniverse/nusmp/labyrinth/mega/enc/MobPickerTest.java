package com.newuniverse.nusmp.labyrinth.mega.enc;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: "deeper is harder" and "sector one is survivable" are promises made by one small function, the toughness window over a faction tag. These tests pin the
 * cap, the floor, the relaxation when nothing fits, the reading of unreadable health, and the mild preference for the tougher members.
 */
class MobPickerTest {
    private static MobPicker.Candidate c(String id, double health) { return new MobPicker.Candidate(id, health); }

    private static final List<MobPicker.Candidate> UNDEAD = List.of(c("bat", 6), c("zombie", 20), c("skeleton", 20), c("husk", 20), c("brute", 50), c("ogre", 100), c("lord", 400));

    private static boolean has(List<MobPicker.Candidate> l, String id) {
        for (MobPicker.Candidate x : l) if (x.id().equals(id)) return true;
        return false;
    }

    @Test
    void theCapGrowsFourteenHealthPerThreatLevel() {
        assertEquals(26.0, MobPicker.cap(1), 1e-9);
        assertEquals(40.0, MobPicker.cap(2), 1e-9);
        assertEquals(82.0, MobPicker.cap(5), 1e-9);
        assertEquals(152.0, MobPicker.cap(10), 1e-9);
        assertEquals(26.0, MobPicker.cap(0), 1e-9, "threat below one is read as one");
        assertEquals(26.0 * 0.12, MobPicker.floor(26.0), 1e-9);
    }

    @Test
    void sectorOneDrawsOnlyTheWeakMembers() {
        List<MobPicker.Candidate> w = MobPicker.window(UNDEAD, MobPicker.cap(1));
        assertTrue(has(w, "zombie") && has(w, "skeleton") && has(w, "husk") && has(w, "bat"));
        assertFalse(has(w, "brute") || has(w, "ogre") || has(w, "lord"));
    }

    @Test
    void deeperSectorsDropTheTrivialMembersAndAddTheTough() {
        List<MobPicker.Candidate> mid = MobPicker.window(UNDEAD, MobPicker.cap(5));
        assertTrue(has(mid, "brute") && has(mid, "zombie"), "cap 82 floor 9.8");
        assertFalse(has(mid, "bat"), "6 health is under the floor of 9.8");
        assertFalse(has(mid, "ogre") || has(mid, "lord"));
        List<MobPicker.Candidate> deep = MobPicker.window(UNDEAD, MobPicker.cap(10));
        assertTrue(has(deep, "ogre") && has(deep, "brute") && has(deep, "zombie"));
        assertFalse(has(deep, "bat"), "6 health is under the floor of 18.2");
        assertFalse(has(deep, "lord"));
        List<MobPicker.Candidate> deeper = MobPicker.window(UNDEAD, MobPicker.cap(30));
        assertTrue(has(deeper, "lord"));
        assertFalse(has(deeper, "zombie"), "20 health is under the floor of 52.8");
        assertFalse(has(deeper, "bat"));
    }

    @Test
    void whenNothingIsInTheWindowTheRuleRelaxesInsteadOfSkipping() {
        // only weak members in a deep sector: they are all under the floor, but they are all there is
        List<MobPicker.Candidate> weak = List.of(c("a", 6), c("b", 8));
        assertEquals(2, MobPicker.window(weak, MobPicker.cap(20)).size());
        // only strong members in sector one: 30 is within the soft 60 percent over the cap of 26 (41.6), 400 is not
        List<MobPicker.Candidate> strong = List.of(c("x", 30), c("y", 400));
        List<MobPicker.Candidate> w = MobPicker.window(strong, MobPicker.cap(1));
        assertEquals(1, w.size());
        assertEquals("x", w.get(0).id());
        assertTrue(MobPicker.window(List.of(c("y", 400)), MobPicker.cap(1)).isEmpty(), "a 400-health member is no business of sector one");
        assertTrue(MobPicker.window(List.of(), MobPicker.cap(1)).isEmpty());
    }

    @Test
    void anUnreadableToughnessCountsAsTheDefault() {
        List<MobPicker.Candidate> odd = List.of(c("nan", Double.NaN), c("zero", 0), c("neg", -5), c("inf", Double.POSITIVE_INFINITY));
        List<MobPicker.Candidate> w = MobPicker.window(odd, MobPicker.cap(1));
        assertEquals(4, w.size(), "each is read as 20 health, which fits sector one");
    }

    @Test
    void pickingNeverLeavesTheWindowAndPrefersTheTougherAMilderWay() {
        Random r = new Random(5);
        Map<String, Integer> seen = new HashMap<>();
        for (int i = 0; i < 20000; i++) {
            MobPicker.Candidate p = MobPicker.pick(UNDEAD, 10, r);
            assertNotNull(p);
            seen.merge(p.id(), 1, Integer::sum);
        }
        assertNull(seen.get("bat"));
        assertNull(seen.get("lord"));
        int zombie = seen.get("zombie"), ogre = seen.get("ogre");
        assertTrue(ogre > zombie, "the 100-health member is drawn more often than a 20-health one: " + ogre + " vs " + zombie);
        assertTrue(zombie > 1500, "but the weak ones still come: " + zombie);
    }

    @Test
    void aMemberWeakerThanEightWeighsAsEight() {
        List<MobPicker.Candidate> two = List.of(c("tiny", 6), c("small", 8));
        Random r = new Random(9);
        int tiny = 0;
        for (int i = 0; i < 10000; i++) if (MobPicker.pick(two, 1, r).id().equals("tiny")) tiny++;
        assertTrue(tiny > 4600 && tiny < 5400, "equal weights: " + tiny);
    }

    @Test
    void pickingFromAnEmptyOrImpossibleSetGivesNull() {
        assertNull(MobPicker.pick(List.of(), 5, new Random(1)));
        assertNull(MobPicker.pick(List.of(c("god", 5000)), 1, new Random(1)));
    }

    @Test
    void tougherMembersAreDrawnInProportionToTheirHealth() {
        // both fit the window of sector five (cap 82, floor 9.8): weights 20 and 60 make the second three times as likely
        List<MobPicker.Candidate> two = List.of(c("light", 20), c("heavy", 60));
        Random r = new Random(11);
        int light = 0, heavy = 0;
        for (int i = 0; i < 20000; i++) {
            if (MobPicker.pick(two, 5, r).id().equals("light")) light++;
            else heavy++;
        }
        assertTrue(heavy > light * 2.5 && heavy < light * 3.6, "heavy " + heavy + " light " + light);
    }
}
