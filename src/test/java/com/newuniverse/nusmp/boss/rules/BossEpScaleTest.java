package com.newuniverse.nusmp.boss.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.arc.goku.GokuEp;
import org.junit.jupiter.api.Test;

/** 4.102.0: a boss's base EP follows the players: Goku heaviest, Sinbad in the middle, Kakashi lightest; only ever raised. */
class BossEpScaleTest {
    @Test void orderOfTheShares() {
        assertTrue(BossEpScale.strictness("anime_multiverse:goku_boss") > BossEpScale.strictness("sinbad"));
        assertTrue(BossEpScale.strictness("sinbad") > BossEpScale.strictness("someone_else"));
        assertTrue(BossEpScale.strictness("someone_else") > BossEpScale.strictness("kakashi_hatake"));
        assertEquals(BossEpScale.OTHER, BossEpScale.strictness(null));
    }

    @Test void raisesOnlyAndScalesWithShareAndGroup() {
        assertEquals(1e12, BossEpScale.base(1e12, 1e9, 1, 1.0, 1.0, 0.25), 0.0);          // weak player: his own EP stays
        assertEquals(5e12, BossEpScale.base(1e12, 5e12, 1, 1.0, 1.0, 0.25), 1.0);         // Goku matches the player
        assertEquals(3e12, BossEpScale.base(1e12, 5e12, 1, 0.6, 1.0, 0.25), 1.0);         // Sinbad 60%
        assertEquals(1.5e12, BossEpScale.base(1e12, 5e12, 1, 0.3, 1.0, 0.25), 1.0);       // Kakashi 30%
        assertEquals(5e12 * 1.5, BossEpScale.base(1e12, 5e12, 3, 1.0, 1.0, 0.25), 1.0);   // two more players: +50%
        assertEquals(5e12 * 3.25, BossEpScale.base(1e12, 5e12, 99, 1.0, 1.0, 0.25), 1.0); // at most 10 count
    }

    @Test void badInputChangesNothing() {
        assertEquals(1e12, BossEpScale.base(1e12, Double.NaN, 2, 1.0, 1.0, 0.25), 0.0);
        assertEquals(1e12, BossEpScale.base(1e12, 5e12, 2, 0.0, 1.0, 0.25), 0.0);
        assertEquals(1e12, BossEpScale.base(1e12, 5e12, 2, 1.0, 0.0, 0.25), 0.0);
        assertEquals(0.0, BossEpScale.base(-5, 0, 1, 1, 1, 0), 0.0);
        assertEquals(BossEpScale.CAP, BossEpScale.base(1, 1e30, 1, 5, 5, 0), 0.0);
    }

    @Test void gokuLadderLiftsWithTheBaseAndKeepsTheClimb() {
        assertEquals(1e12, GokuEp.lifted(0, 9, 1e12, 5e14, 1e12), 1.0);
        assertEquals(5e14, GokuEp.lifted(9, 9, 1e12, 5e14, 1e12), 1.0);
        assertEquals(4e12, GokuEp.lifted(0, 9, 1e12, 5e14, 4e12), 1.0);                   // players at 4 T: Base is 4 T
        assertEquals(2e15, GokuEp.lifted(9, 9, 1e12, 5e14, 4e12), 1e3);                   // and the last form 500x that
        assertEquals(1e12, GokuEp.lifted(0, 9, 1e12, 5e14, 1e6), 1.0);                   // a lower base never lowers it
    }
}
