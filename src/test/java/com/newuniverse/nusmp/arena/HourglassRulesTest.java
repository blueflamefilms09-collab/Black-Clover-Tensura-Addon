package com.newuniverse.nusmp.arena;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Where the hourglass works, for how long, and who comes along. */
class HourglassRulesTest {
    @Test
    void itWorksNowhereInTheLabyrinthOrADungeon() {
        HourglassRules.Decision d = HourglassRules.decide(HourglassRules.Place.FORBIDDEN, false, 10000);
        assertFalse(d.allowed());
        assertEquals(HourglassRules.Denial.FORBIDDEN_DIMENSION, d.denial());
    }

    @Test
    void outsideItTakesYouInAfterThreeSeconds() {
        HourglassRules.Decision d = HourglassRules.decide(HourglassRules.Place.FREE, false, 10000);
        assertTrue(d.allowed());
        assertEquals(HourglassRules.Action.ENTER, d.action());
        assertEquals(60, d.channelTicks());
    }

    @Test
    void itIsNotAnEscapeFromAnotherFight() {
        assertEquals(HourglassRules.Denial.RECENTLY_HURT, HourglassRules.decide(HourglassRules.Place.FREE, false, 99).denial());
        assertTrue(HourglassRules.decide(HourglassRules.Place.FREE, false, 100).allowed());
    }

    @Test
    void theFightIsClosedToLatecomers() {
        assertEquals(HourglassRules.Denial.FIGHT_IN_PROGRESS, HourglassRules.decide(HourglassRules.Place.FREE, true, 10000).denial());
    }

    @Test
    void insideItTakesYouHomeThreeSecondsOrTenDuringTheFight() {
        HourglassRules.Decision calm = HourglassRules.decide(HourglassRules.Place.ARENA, false, 0);
        assertEquals(HourglassRules.Action.LEAVE, calm.action());
        assertEquals(60, calm.channelTicks());
        HourglassRules.Decision fight = HourglassRules.decide(HourglassRules.Place.ARENA, true, 0);
        assertTrue(fight.allowed(), "leaving mid-fight is allowed, even when hurt");
        assertEquals(200, fight.channelTicks());
    }

    @Test
    void theCooldownIsSixtySeconds() {
        assertEquals(1200, HourglassRules.COOLDOWN_TICKS);
    }

    private static HourglassRules.Candidate c(UUID id, double d, boolean party, boolean hostile) { return new HourglassRules.Candidate(id, d, party, hostile); }

    @Test
    void partyMembersComeFirstThenTheNearest() {
        UUID p1 = UUID.randomUUID(), p2 = UUID.randomUUID(), n1 = UUID.randomUUID(), n2 = UUID.randomUUID();
        List<UUID> out = HourglassRules.pickTravellers(List.of(c(n1, 2, false, false), c(p1, 7, true, false), c(n2, 1, false, false), c(p2, 3, true, false)), 8.0, 7);
        assertEquals(List.of(p2, p1, n2, n1), out);
    }

    @Test
    void nobodyBeyondEightBlocksNorAnyoneHostile() {
        UUID near = UUID.randomUUID(), far = UUID.randomUUID(), foe = UUID.randomUUID();
        List<UUID> out = HourglassRules.pickTravellers(List.of(c(near, 8.0, false, false), c(far, 8.01, true, false), c(foe, 1, true, true)), 8.0, 7);
        assertEquals(List.of(near), out);
    }

    @Test
    void theCompanionsAreBounded() {
        java.util.ArrayList<HourglassRules.Candidate> many = new java.util.ArrayList<>();
        for (int i = 0; i < 30; i++) many.add(c(UUID.randomUUID(), i * 0.2, false, false));
        assertEquals(7, HourglassRules.pickTravellers(many, 8.0, HourglassRules.MAX_COMPANIONS).size());
        assertEquals(0, HourglassRules.pickTravellers(many, 8.0, 0).size());
        assertEquals(0, HourglassRules.pickTravellers(null, 8.0, 7).size());
    }

    @Test
    void aDuplicateAndABrokenCandidateAreSkipped() {
        UUID a = UUID.randomUUID();
        List<UUID> out = HourglassRules.pickTravellers(java.util.Arrays.asList(c(a, 1, false, false), c(a, 2, false, false), null, c(null, 1, false, false), c(UUID.randomUUID(), Double.NaN, false, false)), 8.0, 7);
        assertEquals(List.of(a), out);
    }
}
