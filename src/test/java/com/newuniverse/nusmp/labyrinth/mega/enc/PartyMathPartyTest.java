package com.newuniverse.nusmp.labyrinth.mega.enc;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: since 1.58.0 the dungeon is instanced per party and the Dungeon Master rolls for "the table", so two players of different parties must never be one table whatever
 * their coordinates say, while a world with a single party (everybody party 0, which is also what the 4-argument member means) groups exactly as it always did.
 */
public class PartyMathPartyTest {
    private static UUID id(int n) { return new UUID(7L, n); }

    private static PartyMath.Member at(int n, int party, double x, double z) { return new PartyMath.Member(id(n), x, 64, z, party); }

    @Test
    public void playersOfDifferentPartiesAreNeverOneTableEvenWhenTheyStandTogether() {
        List<PartyMath.Party> parties = PartyMath.group(List.of(at(1, 0, 0, 0), at(2, 1, 1, 0), at(3, 0, 5, 0)), 48);
        assertEquals(2, parties.size());
        boolean zeroTogether = false;
        for (PartyMath.Party p : parties) {
            if (p.has(id(1))) {
                assertTrue(p.has(id(3)), "the two members of party 0 are one table");
                assertTrue(!p.has(id(2)), "the player of party 1 is not at their table");
                zeroTogether = true;
            }
        }
        assertTrue(zeroTogether);
    }

    @Test
    public void aChainOfPlayersOfOnePartyIsStillOneTableAndAStrangerInTheMiddleDoesNotJoinThem() {
        // 1 - 2 - 3 are 40 apart in a row and all party 0: one table; player 4 of party 1 stands between them and is nobody's tablemate
        List<PartyMath.Party> parties = PartyMath.group(List.of(at(1, 0, 0, 0), at(2, 0, 40, 0), at(3, 0, 80, 0), at(4, 1, 20, 0)), 48);
        assertEquals(2, parties.size());
        for (PartyMath.Party p : parties) {
            if (p.has(id(4))) assertEquals(1, p.size());
            else assertEquals(3, p.size());
        }
    }

    @Test
    public void aMemberMadeTheOldWayIsAPlayerOfTheSharedChain() {
        PartyMath.Member old = new PartyMath.Member(id(9), 1, 2, 3);
        assertEquals(0, old.party());
        assertEquals(1, PartyMath.group(List.of(old, new PartyMath.Member(id(10), 2, 2, 3)), 48).size(), "the old way of making members groups as before");
        assertEquals(1, PartyMath.group(List.of(old, new PartyMath.Member(id(10), 2, 2, 3, 0)), 48).size(), "and a member of party 0 made the new way is the same as one made the old way");
    }
}
