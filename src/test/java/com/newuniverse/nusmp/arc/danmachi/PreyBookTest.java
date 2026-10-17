package com.newuniverse.nusmp.arc.danmachi;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the Hunt is a curse on the weakest fighter unless the others can steal the mark, so who is chosen, when the mark moves and how three quick hits or one big blow
 * take it are tested as a small state machine.
 */
class PreyBookTest {
    private static PreyBook.Candidate c(int id, int level, double dist) { return new PreyBook.Candidate(id, level, dist); }

    @Test
    void theWeakestIsThePreyThenTheNearestThenTheLowestId() {
        assertEquals(-1, PreyBook.choose(null));
        assertEquals(-1, PreyBook.choose(List.of()));
        assertEquals(7, PreyBook.choose(List.of(c(3, 4, 5), c(7, 1, 30), c(9, 2, 2))), "the lowest Level wins however far");
        assertEquals(9, PreyBook.choose(List.of(c(3, 2, 8), c(9, 2, 5), c(7, 2, 9))), "equal Levels: the nearest");
        assertEquals(3, PreyBook.choose(List.of(c(9, 2, 5), c(3, 2, 5))), "equal everything: the lowest id");
        assertEquals(4, PreyBook.choose(java.util.Arrays.asList(null, c(4, 3, 3))), "a null entry is skipped");
    }

    @Test
    void theMarkIsKeptThenChosenAgain() {
        PreyBook b = new PreyBook();
        List<PreyBook.Candidate> foes = List.of(c(1, 2, 5), c(2, 2, 9));
        assertEquals(1, b.update(0, foes));
        assertTrue(b.hasPrey());
        // a weaker fighter arrives, but the mark is fresh
        List<PreyBook.Candidate> more = List.of(c(1, 2, 5), c(2, 2, 9), c(3, 1, 12));
        assertEquals(1, b.update(PreyBook.REPICK_TICKS - 1, more));
        assertEquals(3, b.update(PreyBook.REPICK_TICKS, more), "after the repick time the weakest is hunted");
        // the prey leaves: a new one at once
        assertEquals(1, b.update(PreyBook.REPICK_TICKS + 20, List.of(c(1, 2, 5), c(2, 2, 9))));
        assertEquals(-1, b.update(PreyBook.REPICK_TICKS + 40, List.of()), "nobody to hunt");
        assertFalse(b.hasPrey());
        assertEquals(-1, b.update(PreyBook.REPICK_TICKS + 60, null));
    }

    @Test
    void thePreyTakesMoreFromTheBullAndNobodyElseDoes() {
        PreyBook b = new PreyBook();
        b.update(0, List.of(c(5, 1, 4), c(6, 3, 4)));
        assertEquals(5, b.prey());
        assertEquals(PreyBook.PREY_BONUS, b.bonusFor(5), 1e-9);
        assertEquals(1.0f, b.bonusFor(6), 1e-9);
        assertEquals(1.0f, b.bonusFor(-1), 1e-9);
        assertTrue(PreyBook.PREY_BONUS > 1.0f && PreyBook.PREY_BONUS < 1.5f, "a bite, not a death sentence");
        PreyBook none = new PreyBook();
        assertEquals(1.0f, none.bonusFor(0), 1e-9, "no prey, no bonus, not even for id 0 or -1");
        assertEquals(1.0f, none.bonusFor(-1), 1e-9);
    }

    @Test
    void threeQuickHitsStealTheMark() {
        PreyBook b = new PreyBook();
        b.update(0, List.of(c(1, 1, 4), c(2, 4, 4)));
        assertEquals(1, b.prey());
        assertFalse(b.onHit(10, 2, 5f, 1000f));
        assertFalse(b.onHit(30, 2, 5f, 1000f));
        assertEquals(1, b.prey(), "two hits are not enough");
        assertTrue(b.onHit(50, 2, 5f, 1000f), "the third within three seconds");
        assertEquals(2, b.prey());
        assertTrue(b.stolen(51));
        assertTrue(b.stolen(10 + PreyBook.STEAL_TICKS));
        assertFalse(b.stolen(50 + PreyBook.STEAL_TICKS));
        assertEquals(PreyBook.PREY_BONUS, b.bonusFor(2), 1e-9);
        assertEquals(1.0f, b.bonusFor(1), 1e-9);
    }

    @Test
    void slowHitsDoNotSteal() {
        PreyBook b = new PreyBook();
        b.update(0, List.of(c(1, 1, 4), c(2, 4, 4)));
        for (int i = 0; i < 10; i++) assertFalse(b.onHit(i * 40L, 2, 5f, 1000f), "one hit every two seconds never adds up");
        assertEquals(1, b.prey());
    }

    @Test
    void oneBigBlowStealsAtOnce() {
        PreyBook b = new PreyBook();
        b.update(0, List.of(c(1, 1, 4), c(2, 4, 4)));
        assertFalse(b.onHit(5, 2, 49f, 1000f), "just under 5% of the boss's health");
        assertTrue(b.onHit(300, 2, 50f, 1000f), "5% in one blow");
        assertEquals(2, b.prey());
    }

    @Test
    void aStolenMarkIsFreshForAWhileAndThenTheWeakestIsHuntedAgain() {
        PreyBook b = new PreyBook();
        List<PreyBook.Candidate> foes = List.of(c(1, 1, 4), c(2, 4, 4));
        b.update(0, foes);
        assertTrue(b.onHit(10, 2, 80f, 1000f));
        assertEquals(2, b.update(PreyBook.REPICK_TICKS + 5, foes), "the stolen mark is only 195 ticks old, still fresh");
        assertEquals(1, b.update(10 + PreyBook.STEAL_TICKS + PreyBook.REPICK_TICKS, foes), "when the steal and the freshness are both over the weakest is hunted again");
        // the prey's own hits never steal anything
        assertFalse(b.onHit(1000, 1, 500f, 1000f));
        assertEquals(1, b.prey());
        assertFalse(b.onHit(1001, -1, 500f, 1000f), "damage with no fighter behind it");
        assertFalse(b.onHit(1002, 2, 0f, 1000f), "a blow that does nothing");
    }

    @Test
    void clearForgetsEverything() {
        PreyBook b = new PreyBook();
        b.update(0, List.of(c(1, 1, 4)));
        b.onHit(1, 2, 90f, 1000f);
        b.clear();
        assertEquals(-1, b.prey());
        assertFalse(b.stolen(2));
        // an old hit record must not count towards a steal in the next fight
        b.update(100, List.of(c(1, 1, 4), c(2, 4, 4)));
        assertFalse(b.onHit(101, 2, 1f, 1000f));
        assertFalse(b.onHit(102, 2, 1f, 1000f));
        assertTrue(b.onHit(103, 2, 1f, 1000f), "the third hit of the new fight");
    }
}
