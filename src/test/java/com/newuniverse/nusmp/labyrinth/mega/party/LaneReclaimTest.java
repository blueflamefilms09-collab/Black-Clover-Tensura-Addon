package com.newuniverse.nusmp.labyrinth.mega.party;

import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LaneReclaimTest {
    @Test
    public void modeReadsLeniently() {
        assertEquals(LaneReclaim.Mode.OFF, LaneReclaim.Mode.parse(null));
        assertEquals(LaneReclaim.Mode.OFF, LaneReclaim.Mode.parse("off"));
        assertEquals(LaneReclaim.Mode.OFF, LaneReclaim.Mode.parse("typo"));
        assertEquals(LaneReclaim.Mode.ARCHIVE, LaneReclaim.Mode.parse(" Archive "));
        assertEquals(LaneReclaim.Mode.DELETE, LaneReclaim.Mode.parse("DELETE"));
    }

    @Test
    public void onlyStampedIdleLanesBesidesLaneZeroAreDue() {
        long idle = LaneReclaim.idleTicks(168);
        assertEquals(168L * 72_000L, idle);
        Map<Integer, Long> used = Map.of(0, 0L, 1, 0L, 2, 4_000L, 3, 12_100_000L);
        long now = 12_100_000L;
        assertEquals(List.of(1, 2), LaneReclaim.idle(used, List.of(0, 1, 2, 3, 4), now, idle));
        assertEquals(List.of(4), LaneReclaim.unstamped(used, List.of(0, 1, 2, 3, 4)));
    }

    @Test
    public void everyRegionOfALaneBelongsToItsParty() {
        for (int party = 0; party < MegaGrid.MAX_PARTIES; party++) {
            for (Polarity road : Polarity.values()) {
                int centre = Math.floorDiv(MegaGrid.laneZ(road, party), 512);
                for (int d = -3; d <= 3; d++) {
                    assertEquals(party, LaneReclaim.partyOfFile("r.5." + (centre + d) + ".mca"), "party " + party + " " + road + " offset " + d);
                }
            }
        }
    }

    @Test
    public void foreignFilesAreNeverAttributed() {
        assertEquals(-1, LaneReclaim.partyOfFile("level.dat"));
        assertEquals(-1, LaneReclaim.partyOfFile("r.1.2.mcc"));
        assertEquals(-1, LaneReclaim.partyOfFile("r.x.2.mca"));
        assertEquals(-1, LaneReclaim.partyOfRegionZ(Math.floorDiv(MegaGrid.laneZ(Polarity.ABYSSAL, 255), 512) + 400));
    }

    @Test
    public void forgettingAPartyNeverReusesItsSectorIds() {
        MegaData d = new MegaData();
        for (int i = 0; i < 6; i++) {
            Sector s = new Sector(d.nextSectorId());
            s.party = i < 3 ? 0 : 7;
            d.add(s);
        }
        assertEquals(6, d.nextSectorId());
        assertEquals(3, d.removeParty(7));
        assertEquals(3, d.sectors().size());
        assertEquals(6, d.nextSectorId());
        Sector next = new Sector(d.nextSectorId());
        d.add(next);
        assertEquals(6, next.id);
        assertTrue(d.parties().equals(List.of(0)));
        assertEquals(0, d.removeParty(7));
    }

    @Test
    public void theBookKeepsARetiredLaneReservedUntilItsFilesAreGone() {
        MegaParties book = new MegaParties();
        PartyKey key = PartyKey.solo(java.util.UUID.randomUUID());
        book.index().allocate(key, "a", 8, Set.of(), List.of());
        int lane = book.index().indexOf(key.id());
        assertEquals(1, book.retire(lane));
        assertTrue(book.retired().contains(lane));
        assertEquals(-1, book.index().indexOf(key.id()));
        assertEquals(lane + 1, book.index().lowestFree(8, book.retired()));
        book.clearRetired(List.of(lane));
        assertEquals(lane, book.index().lowestFree(8, book.retired()));
    }
}
