package com.newuniverse.nusmp.labyrinth.mega.enc;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: one roll per group of players, not per player, is what keeps four friends from being hit by four ambushes; these tests pin who counts as a party
 * (transitively, inclusively, in three dimensions), the factor table, and the small modifier a bigger party adds.
 */
class PartyMathTest {
    private static UUID id(int n) { return new UUID(0L, n); }

    private static PartyMath.Member at(int n, double x, double z) { return new PartyMath.Member(id(n), x, 64, z); }

    @Test
    void aLonePlayerIsAPartyOfOneAtTheirOwnPosition() {
        List<PartyMath.Party> parties = PartyMath.group(List.of(at(1, 10, 20)), 48);
        assertEquals(1, parties.size());
        assertEquals(1, parties.get(0).size());
        assertEquals(id(1), parties.get(0).leader());
        assertEquals(10.0, parties.get(0).cx(), 1e-9);
        assertEquals(20.0, parties.get(0).cz(), 1e-9);
    }

    @Test
    void noPlayersGiveNoParties() {
        assertTrue(PartyMath.group(new ArrayList<>(), 48).isEmpty());
    }

    @Test
    void playersWithinTheRadiusShareAPartyAndFarOnesDoNot() {
        List<PartyMath.Party> parties = PartyMath.group(List.of(at(1, 0, 0), at(2, 40, 0), at(3, 500, 0)), 48);
        assertEquals(2, parties.size());
        assertEquals(2, parties.get(0).size());
        assertEquals(1, parties.get(1).size());
        assertEquals(id(3), parties.get(1).leader());
    }

    @Test
    void theRadiusIsInclusiveAndMeasuredInThreeDimensions() {
        assertEquals(1, PartyMath.group(List.of(at(1, 0, 0), at(2, 48, 0)), 48).size(), "exactly on the radius is together");
        assertEquals(2, PartyMath.group(List.of(at(1, 0, 0), at(2, 48.5, 0)), 48).size());
        PartyMath.Member high = new PartyMath.Member(id(2), 0, 64 + 60, 0);
        assertEquals(2, PartyMath.group(List.of(at(1, 0, 0), high), 48).size(), "a player 60 blocks above is not at the same table");
    }

    @Test
    void aChainOfNeighboursIsOnePartyEvenWhenTheEndsAreFarApart() {
        // 1 -- 2 -- 3 -- 4, each 40 apart: the ends are 120 apart but linked through the middle ones
        List<PartyMath.Party> parties = PartyMath.group(List.of(at(1, 0, 0), at(2, 40, 0), at(3, 80, 0), at(4, 120, 0)), 48);
        assertEquals(1, parties.size());
        assertEquals(4, parties.get(0).size());
        assertEquals(60.0, parties.get(0).cx(), 1e-9);
    }

    @Test
    void theLeaderIsTheLowestIdAndTheOrderDoesNotDependOnTheInputOrder() {
        List<PartyMath.Party> a = PartyMath.group(List.of(at(9, 0, 0), at(3, 5, 0), at(7, 900, 0), at(2, 905, 0)), 48);
        List<PartyMath.Party> b = PartyMath.group(List.of(at(2, 905, 0), at(7, 900, 0), at(3, 5, 0), at(9, 0, 0)), 48);
        assertEquals(2, a.size());
        assertEquals(id(2), a.get(0).leader());
        assertEquals(id(3), a.get(1).leader());
        assertEquals(a.get(0).leader(), b.get(0).leader());
        assertEquals(a.get(1).leader(), b.get(1).leader());
        assertEquals(id(2), a.get(0).members().get(0).id());
        assertEquals(id(7), a.get(0).members().get(1).id());
        assertTrue(a.get(1).has(id(9)));
        assertFalse(a.get(1).has(id(7)));
    }

    @Test
    void theDefaultFactorsAreOneTwoThreeAndFourPlayersThenAPartOfOneMoreEach() {
        double[] t = PartyMath.parseFactors(PartyMath.DEFAULT_FACTORS);
        assertEquals(1.0, PartyMath.factor(1, t, 0.3), 1e-9);
        assertEquals(1.6, PartyMath.factor(2, t, 0.3), 1e-9);
        assertEquals(2.1, PartyMath.factor(3, t, 0.3), 1e-9);
        assertEquals(2.5, PartyMath.factor(4, t, 0.3), 1e-9);
        assertEquals(2.8, PartyMath.factor(5, t, 0.3), 1e-9);
        assertEquals(3.4, PartyMath.factor(7, t, 0.3), 1e-9);
        assertEquals(1.0, PartyMath.factor(0, t, 0.3), 1e-9, "an empty party is read as one player, never zero");
    }

    @Test
    void aBadFactorStringFallsBackToTheDefaultTable() {
        double[] def = PartyMath.parseFactors(PartyMath.DEFAULT_FACTORS);
        assertEquals(4, def.length);
        for (String bad : new String[] {null, "", "  ", "1.0,x", "1.0,,2.0", "0.0,1.0", "-1", "1,2,NaN", "1,999"}) {
            double[] t = PartyMath.parseFactors(bad);
            assertEquals(4, t.length, "'" + bad + "'");
            assertEquals(1.6, t[1], 1e-9, "'" + bad + "'");
        }
        double[] custom = PartyMath.parseFactors(" 1 , 1.5 ");
        assertEquals(2, custom.length);
        assertEquals(1.5, PartyMath.factor(2, custom, 0.5), 1e-9);
        assertEquals(2.0, PartyMath.factor(3, custom, 0.5), 1e-9, "beyond a short table the extra applies from its last entry");
    }

    @Test
    void theSizeModifierRisesByOnePerExtraPlayerUpToTheCap() {
        assertEquals(0, PartyMath.mod(1, 2));
        assertEquals(1, PartyMath.mod(2, 2));
        assertEquals(2, PartyMath.mod(3, 2));
        assertEquals(2, PartyMath.mod(9, 2));
        assertEquals(0, PartyMath.mod(0, 2));
        assertEquals(0, PartyMath.mod(5, 0));
        assertEquals(0, PartyMath.mod(5, -3));
    }
}
