package com.newuniverse.nusmp.labyrinth.mega.party;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the key is what every later visit of a party is recognised by, so the rule that picks it is pinned: a team's id when there is a real team, the player's own id for a
 * player alone and for the personal team FTB Teams gives everybody, and a form that survives being saved and read back (a damaged save must read as nothing, never throw).
 */
public class PartyKeyTest {
    private static UUID id(int n) { return new UUID(0x1234L, n); }

    @Test
    public void aPlayerWithoutATeamIsAPartyOfOne() {
        PartyKey k = PartyKey.of(null, id(1));
        assertEquals(id(1), k.id());
        assertFalse(k.team());
    }

    @Test
    public void thePersonalTeamOfAPlayerIsNoGroup() {
        // FTB Teams gives every player a personal team whose id is the player's own id: that is a party of one, not a team
        PartyKey k = PartyKey.of(id(1), id(1));
        assertEquals(PartyKey.solo(id(1)), k);
        assertFalse(k.team());
    }

    @Test
    public void aRealTeamIsTheKeyOfAllItsMembers() {
        PartyKey a = PartyKey.of(id(900), id(1));
        PartyKey b = PartyKey.of(id(900), id(2));
        assertTrue(a.team());
        assertEquals(a, b, "two teammates have the same key");
        assertEquals(id(900), a.id());
        assertFalse(a.equals(PartyKey.of(id(901), id(1))), "another team is another party");
        assertFalse(a.equals(PartyKey.of(null, id(1))), "a player who left the team is a party of one again");
    }

    @Test
    public void theKindIsNotPartOfTheIdentity() {
        assertEquals(PartyKey.solo(id(5)), PartyKey.team(id(5)));
        assertEquals(PartyKey.solo(id(5)).hashCode(), PartyKey.team(id(5)).hashCode());
        Set<PartyKey> set = new HashSet<>();
        set.add(PartyKey.solo(id(5)));
        set.add(PartyKey.team(id(5)));
        assertEquals(1, set.size());
    }

    @Test
    public void theSavedFormReadsBackAndABadOneReadsAsNothing() {
        PartyKey t = PartyKey.team(id(77)), p = PartyKey.solo(id(78));
        assertEquals("t:" + id(77), t.text());
        assertEquals("p:" + id(78), p.text());
        PartyKey t2 = PartyKey.parse(t.text()), p2 = PartyKey.parse(p.text());
        assertEquals(t, t2);
        assertTrue(t2.team());
        assertEquals(p, p2);
        assertFalse(p2.team());
        assertEquals(p, PartyKey.parse(id(78).toString()), "a bare uuid is a player");
        assertNull(PartyKey.parse(null));
        assertNull(PartyKey.parse(""));
        assertNull(PartyKey.parse("t:not-a-uuid"));
        assertNull(PartyKey.parse("hello"));
    }
}
