package com.newuniverse.nusmp.labyrinth.mega.party;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the mode string of the config, the rule for when parties are separate chains at all, the verdict of the guard that keeps a party's floors to that party and the
 * name shown for a team are small decisions with corners (a typo, an operator, a world saved before parties, a team name that contains a hash), so each is pinned by a
 * table here rather than found by a player.
 */
public class PartyPolicyTest {
    private static final Integer ONE = 1, TWO = 2;

    @Test
    public void theModeStringIsReadLeniently() {
        assertEquals(PartyPolicy.Mode.PARTY, PartyPolicy.Mode.parse("party"));
        assertEquals(PartyPolicy.Mode.PARTY, PartyPolicy.Mode.parse("PARTY"));
        assertEquals(PartyPolicy.Mode.SHARED, PartyPolicy.Mode.parse("shared"));
        assertEquals(PartyPolicy.Mode.SHARED, PartyPolicy.Mode.parse("  Shared "));
        assertEquals(PartyPolicy.Mode.PARTY, PartyPolicy.Mode.parse("sharde"), "a typo keeps the default, it neither throws nor turns the instancing off");
        assertEquals(PartyPolicy.Mode.PARTY, PartyPolicy.Mode.parse(""));
        assertEquals(PartyPolicy.Mode.PARTY, PartyPolicy.Mode.parse(null));
        assertEquals("shared", PartyPolicy.Mode.SHARED.id());
        assertEquals("party", PartyPolicy.Mode.PARTY.id());
    }

    @Test
    public void partiesAreInstancedOnlyInPartyModeAndOnlyInTheFloorModel() {
        assertTrue(PartyPolicy.instanced(PartyPolicy.Mode.PARTY, true));
        assertFalse(PartyPolicy.instanced(PartyPolicy.Mode.PARTY, false), "the legacy ten-floor sectors wind through the dimension and keep one shared chain");
        assertFalse(PartyPolicy.instanced(PartyPolicy.Mode.SHARED, true));
        assertFalse(PartyPolicy.instanced(PartyPolicy.Mode.SHARED, false));
    }

    @Test
    public void theEntryIndexIsTheBooksOnlyWhenInstanced() {
        assertEquals(5, PartyPolicy.entryIndex(true, 5));
        assertEquals(0, PartyPolicy.entryIndex(true, 0));
        assertEquals(0, PartyPolicy.entryIndex(true, -1), "a party the book refused is not an index");
        assertEquals(0, PartyPolicy.entryIndex(false, 5));
    }

    @Test
    public void theGuardLeavesEverythingAloneUnlessPartiesAreInstancedAndItIsOn() {
        assertEquals(PartyPolicy.Verdict.OK, PartyPolicy.verdict(false, true, false, ONE, TWO));
        assertEquals(PartyPolicy.Verdict.OK, PartyPolicy.verdict(true, false, false, ONE, TWO));
        assertEquals(PartyPolicy.Verdict.OK, PartyPolicy.verdict(true, true, false, ONE, null), "a gap between sectors decides nothing");
        assertEquals(PartyPolicy.Verdict.OK, PartyPolicy.verdict(true, true, false, null, null));
    }

    @Test
    public void aPlayerIsSentOutOfAFloorThatBelongsToAnotherParty() {
        assertEquals(PartyPolicy.Verdict.EVICT, PartyPolicy.verdict(true, true, false, ONE, TWO));
        assertEquals(PartyPolicy.Verdict.EVICT, PartyPolicy.verdict(true, true, false, 0, 3), "the shared lane is a party too");
        assertEquals(PartyPolicy.Verdict.OK, PartyPolicy.verdict(true, true, false, TWO, TWO));
        assertEquals(PartyPolicy.Verdict.OK, PartyPolicy.verdict(true, true, false, 0, 0));
    }

    @Test
    public void aPlayerWithNoRecordAdoptsTheFloorTheyStandIn() {
        assertEquals(PartyPolicy.Verdict.ADOPT, PartyPolicy.verdict(true, true, false, null, TWO), "a world saved before parties, a teleport by command, a login after an update");
        assertEquals(PartyPolicy.Verdict.ADOPT, PartyPolicy.verdict(true, true, false, null, 0));
    }

    @Test
    public void operatorsAndSpectatorsAreNeverMoved() {
        assertEquals(PartyPolicy.Verdict.OK, PartyPolicy.verdict(true, true, true, ONE, TWO));
        assertEquals(PartyPolicy.Verdict.OK, PartyPolicy.verdict(true, true, true, null, TWO), "an exempt player does not even adopt");
    }

    @Test
    public void theTeamNameLosesFtbsIdSuffixAndNothingElse() {
        assertEquals("Notch_Party", PartyPolicy.displayName("Notch_Party#5E6F8A9B"));
        assertEquals("Herobrine", PartyPolicy.displayName("Herobrine#1a2b3c4d"));
        assertEquals("A#1 Team", PartyPolicy.displayName("A#1 Team#00ff00ff"), "only the last hash is the suffix");
        assertEquals("Team#Alpha123", PartyPolicy.displayName("Team#Alpha123"), "eight characters after a hash that are not hex are part of the name");
        assertEquals("Plain", PartyPolicy.displayName("Plain"));
        assertEquals("#5E6F8A9B", PartyPolicy.displayName("#5E6F8A9B"), "a name that is only the suffix is left whole");
        assertEquals("?", PartyPolicy.displayName(null));
        assertEquals("?", PartyPolicy.displayName("   "));
    }
}
