package com.newuniverse.nusmp.labyrinth.mega.enc;

import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the floor-banded threat is the one number the whole Dungeon Master reads, and a wrong sign, a missing absolute value or a lost fallback would make
 * the Abyssal road easier than the Holy one, or the first sector of an old save a sector of zero; these tests pin the rule: the floor decides when it is known
 * and wanted, the sector's threat answers otherwise, the two roads are symmetric, the number is never below one, and a party is as deep as its deepest member.
 */
class ThreatInputTest {
    @Test
    void aKnownFloorDecidesWhenTheRuleIsOn() {
        assertEquals(1, ThreatInput.of(9, 1, true));
        assertEquals(3, ThreatInput.of(9, 3, true));
        assertEquals(10, ThreatInput.of(1, 10, true));
        assertEquals(17, ThreatInput.of(2, 17, true), "the second sector of a road goes on counting from the Hub");
    }

    @Test
    void theTwoRoadsAreSymmetric() {
        for (int depth = 1; depth <= 40; depth++) {
            assertEquals(ThreatInput.of(3, depth, true), ThreatInput.of(3, -depth, true), "floor +" + depth + " and floor -" + depth);
            assertEquals(depth, ThreatInput.of(3, Polarity.ABYSSAL.sign() * depth, true));
            assertEquals(depth, ThreatInput.of(3, Polarity.HOLY.sign() * depth, true));
        }
    }

    @Test
    void anUnknownFloorFallsBackToTheSectorsThreat() {
        assertEquals(5, ThreatInput.of(5, 0, true), "floor 0 is outside every sector");
        assertEquals(1, ThreatInput.of(1, 0, true));
    }

    @Test
    void theSwitchOffGivesTheSectorsThreatWhateverTheFloor() {
        assertEquals(5, ThreatInput.of(5, 8, false));
        assertEquals(5, ThreatInput.of(5, -8, false));
        assertEquals(5, ThreatInput.of(5, 0, false));
    }

    @Test
    void theThreatIsNeverBelowOne() {
        assertEquals(1, ThreatInput.of(0, 0, true));
        assertEquals(1, ThreatInput.of(-4, 0, false));
        assertEquals(1, ThreatInput.of(-4, 0, true));
        assertEquals(1, ThreatInput.of(0, 1, true));
    }

    @Test
    void theRuleAgreesWithTheFloorMapForEveryFloorOfTheFirstThreeSectors() {
        for (Polarity p : Polarity.values()) {
            for (int chain = 0; chain < 3; chain++) {
                for (int band = 1; band <= FloorMap.DEFAULT_PER_REGION; band++) {
                    int floor = FloorMap.floorOf(p, chain, band);
                    assertEquals(FloorMap.threat(floor), ThreatInput.of(1 + chain, floor, true), p + " chain " + chain + " band " + band);
                    assertEquals(chain * FloorMap.DEFAULT_PER_REGION + band, ThreatInput.of(1 + chain, floor, true));
                }
            }
        }
    }

    @Test
    void aPartyIsAsDeepAsItsDeepestMember() {
        assertEquals(7, ThreatInput.deepest(2, 7, 4));
        assertEquals(-9, ThreatInput.deepest(2, -9, 4), "depth is the distance from the Hub, on either road");
        assertEquals(3, ThreatInput.deepest(3, -3), "on a tie the first one given decides");
        assertEquals(-3, ThreatInput.deepest(-3, 3));
        assertEquals(5, ThreatInput.deepest(5));
    }

    @Test
    void floorsOfZeroAreIgnoredAndNothingGivesZero() {
        assertEquals(4, ThreatInput.deepest(0, 4, 0));
        assertEquals(0, ThreatInput.deepest(0, 0));
        assertEquals(0, ThreatInput.deepest());
        assertEquals(0, ThreatInput.deepest((int[]) null));
        assertEquals(ThreatInput.of(6, 0, true), ThreatInput.of(6, ThreatInput.deepest(0, 0), true), "a party outside every sector falls back to the sector");
    }
}
