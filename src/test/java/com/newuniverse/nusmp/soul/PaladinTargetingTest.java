package com.newuniverse.nusmp.soul;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Who a Paladin may attack, and what it does about its distance to the owner. */
class PaladinTargetingTest {
    private static PaladinTargeting.Candidate foe() {
        return new PaladinTargeting.Candidate(true, false, false, false, false, false, false, 6);
    }

    @Test
    void anOrdinaryFoeNearTheOwnerIsFair() {
        assertTrue(PaladinTargeting.fairTarget(foe()));
    }

    @Test
    void neverTheOwnerAnAllyAPaladinAPetOrASpectator() {
        assertFalse(PaladinTargeting.fairTarget(new PaladinTargeting.Candidate(true, true, false, false, false, false, false, 1)));
        assertFalse(PaladinTargeting.fairTarget(new PaladinTargeting.Candidate(true, false, true, false, false, false, false, 1)));
        assertFalse(PaladinTargeting.fairTarget(new PaladinTargeting.Candidate(true, false, false, true, false, false, false, 1)));
        assertFalse(PaladinTargeting.fairTarget(new PaladinTargeting.Candidate(true, false, false, false, true, false, false, 1)));
        assertFalse(PaladinTargeting.fairTarget(new PaladinTargeting.Candidate(true, false, false, false, false, true, false, 1)));
        assertFalse(PaladinTargeting.fairTarget(new PaladinTargeting.Candidate(true, false, false, false, false, false, true, 1)));
        assertFalse(PaladinTargeting.fairTarget(new PaladinTargeting.Candidate(false, false, false, false, false, false, false, 1)));
    }

    @Test
    void aFoeTooFarFromTheOwnerIsNotChased() {
        assertTrue(PaladinTargeting.fairTarget(new PaladinTargeting.Candidate(true, false, false, false, false, false, false, PaladinTargeting.MAX_TARGET_FROM_OWNER)));
        assertFalse(PaladinTargeting.fairTarget(new PaladinTargeting.Candidate(true, false, false, false, false, false, false, PaladinTargeting.MAX_TARGET_FROM_OWNER + 0.1)));
    }

    @Test
    void followByDistance() {
        assertEquals(PaladinTargeting.Follow.NONE, PaladinTargeting.follow(5, false, true));
        assertEquals(PaladinTargeting.Follow.WALK, PaladinTargeting.follow(12, false, true));
        assertEquals(PaladinTargeting.Follow.NONE, PaladinTargeting.follow(12, true, true), "busy with a foe: it fights");
        assertEquals(PaladinTargeting.Follow.RETURN, PaladinTargeting.follow(26, true, true), "past the leash it comes home even mid-fight");
        assertEquals(PaladinTargeting.Follow.TELEPORT, PaladinTargeting.follow(33, false, true));
        assertEquals(PaladinTargeting.Follow.TELEPORT, PaladinTargeting.follow(2, false, false), "another level: it is brought over");
    }

    @Test
    void theGrudgeFollowsWhoHurtTheOwnerThenWhomTheOwnerHit() {
        assertEquals(1, PaladinTargeting.pick(1000, 950, true, 990, true), "the attacker comes first");
        assertEquals(2, PaladinTargeting.pick(1000, 950, false, 990, true));
        assertEquals(0, PaladinTargeting.pick(1000, 950, false, 990, false));
        assertEquals(0, PaladinTargeting.pick(1000, 1000 - PaladinTargeting.GRUDGE_TICKS - 1, true, 0, true), "an old grudge is dropped");
        assertEquals(1, PaladinTargeting.pick(1000, 1000 - PaladinTargeting.GRUDGE_TICKS, true, 0, false));
        assertEquals(0, PaladinTargeting.pick(1000, 0, true, 0, true), "stamp 0 means never");
    }
}
