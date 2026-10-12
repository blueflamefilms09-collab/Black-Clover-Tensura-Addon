package com.newuniverse.nusmp.dungeon.nexus;

import com.newuniverse.nusmp.dungeon.nexus.ZonePlan.Spot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** What a zone describes: ground, volume, phases of the build, relic and gate spots, spawn roles. */
class ZonePlanTest {
    private static ZonePlan.Builder base() {
        return ZonePlan.builder(NexusZone.MONARCH_VAULT, 3, -2, 3 * 128 + 64, -2 * 128 + 64).floorY(-52).topY(-34).footprint(Footprint.Shape.OVAL, 40, 30);
    }

    @Test
    void thePlanKeepsWhatItWasGiven() {
        ZonePlan.Builder b = base();
        b.ops().put("minecraft:obsidian", 0, -52, 0);
        b.centralRelic(448, -51, -192).minorRelic(450, -51, -190).minorRelic(446, -51, -190).gate(448, -51, -170).entrance(440, -51, -160)
                .spawn(ZonePlan.ROLE_BOSS, 448, -51, -192).spawn(ZonePlan.ROLE_GUARD, 440, -51, -190).spawn(ZonePlan.ROLE_GUARD, 456, -51, -190).spawn("custom", 1, 2, 3);
        ZonePlan p = b.build();
        assertSame(NexusZone.MONARCH_VAULT, p.zone);
        assertEquals(3, p.cellX);
        assertEquals(-2, p.cellZ);
        assertEquals(448, p.centreX());
        assertEquals(-192, p.centreZ());
        assertEquals(new Spot(448, -52, -192), p.centre(), "the floor block in the middle");
        assertEquals(new Spot(448, -51, -192), p.centralRelic);
        assertEquals(2, p.minorRelics.size());
        assertEquals(1, p.gates.size());
        assertEquals(1, p.entrances.size());
        assertEquals(1, p.spawns(ZonePlan.ROLE_BOSS).size());
        assertEquals(2, p.spawns(ZonePlan.ROLE_GUARD).size());
        assertEquals(List.of(new Spot(1, 2, 3)), p.spawns("custom"));
        assertTrue(p.spawns(ZonePlan.ROLE_SWARM).isEmpty(), "a role nobody used is empty, not null");
        assertEquals(1, p.ops.size());
        assertThrows(UnsupportedOperationException.class, () -> p.ops.clear());
        assertThrows(UnsupportedOperationException.class, () -> p.gates.add(new Spot(0, 0, 0)));
        assertThrows(UnsupportedOperationException.class, () -> p.spawns(ZonePlan.ROLE_GUARD).add(new Spot(0, 0, 0)));
    }

    @Test
    void aPlanNeedsItsGroundAndAHeight() {
        assertThrows(IllegalStateException.class, () -> ZonePlan.builder(NexusZone.GOLDEN_CITY, 0, 0, 64, 64).floorY(-50).topY(-30).build(), "no footprint");
        assertThrows(IllegalStateException.class, () -> ZonePlan.builder(NexusZone.GOLDEN_CITY, 0, 0, 64, 64).footprint(Footprint.Shape.RECT, 5, 5).build(), "no heights");
        assertThrows(IllegalStateException.class, () -> base().topY(-60).build(), "roof under the floor");
        assertThrows(IllegalArgumentException.class, () -> ZonePlan.builder(null, 0, 0, 0, 0));
    }

    @Test
    void phasesCountOpsFromTheirMarks() {
        ZonePlan.Builder b = base();
        NexusOps o = b.ops();
        o.put("a:b", 0, 0, 0);
        o.put("a:b", 1, 0, 0);
        o.phase();
        o.put("a:b", 2, 0, 0);
        o.put("a:b", 3, 0, 0);
        o.put("a:b", 4, 0, 0);
        o.phase();
        o.put("a:b", 5, 0, 0);
        ZonePlan p = b.build();
        assertEquals(3, p.phaseCount());
        assertEquals(0, p.phaseStart(0));
        assertEquals(2, p.phaseStart(1));
        assertEquals(5, p.phaseStart(2));
        assertEquals(0, p.phaseOf(0));
        assertEquals(0, p.phaseOf(1));
        assertEquals(1, p.phaseOf(2));
        assertEquals(1, p.phaseOf(4));
        assertEquals(2, p.phaseOf(5));
        assertEquals(2, p.phaseOf(99), "past the end is the last phase");
        assertEquals(5, p.phaseStart(99), "a phase past the last clamps to the last phase's start");
    }

    @Test
    void aPlanWithoutOpsHasOnePhaseAndNothingToDo() {
        ZonePlan p = base().build();
        assertEquals(1, p.phaseCount());
        assertEquals(0, p.phaseOf(0));
        assertTrue(p.ops.isEmpty());
    }

    @Test
    void theVolumeIsTheGroundFromJustBelowTheFloorToJustAboveTheRoof() {
        ZonePlan p = base().build();
        assertTrue(p.contains(448, -52, -192, 0));
        assertTrue(p.contains(448, -52 - ZonePlan.BELOW, -192, 0));
        assertFalse(p.contains(448, -52 - ZonePlan.BELOW - 1, -192, 0));
        assertTrue(p.contains(448, -34 + ZonePlan.ABOVE, -192, 0));
        assertFalse(p.contains(448, -34 + ZonePlan.ABOVE + 1, -192, 0));
        assertTrue(p.contains(448 + 40, -50, -192, 0));
        assertFalse(p.contains(448 + 41, -50, -192, 0));
        assertTrue(p.contains(448 + 41, -50, -192, 1), "a margin grows the ground");
        assertEquals(p.footprint.chunks().size(), p.chunks().size());
    }

    @Test
    void chunksAlsoCoverEveryChunkAnOpWritesInto() {
        ZonePlan plain = base().build();
        assertFalse(plain.opsStrayFromFootprint());
        ZonePlan.Builder inside = base();
        inside.ops().box("a:b", -10, -50, -10, 10, -48, 10);
        assertFalse(inside.build().opsStrayFromFootprint(), "ops inside the footprint add nothing");
        ZonePlan.Builder b = base();                          // centre (448, -192), oval of half-widths 40 x 30: x 408..488, chunks 25..30
        b.ops().box("a:b", 60, -50, 0, 70, -48, 3);           // x 508..518: chunks 31 and 32, past the oval
        b.ops().put("a:b", 61, -50, 1);                       // the same chunks again
        ZonePlan stray = b.build();
        assertTrue(stray.opsStrayFromFootprint());
        List<int[]> chunks = stray.chunks();
        assertEquals(stray.footprint.chunks().size() + 2, chunks.size(), "the two extra chunks once each");
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int[] c : chunks) assertTrue(seen.add(c[0] + "," + c[1]), "no chunk twice");
        assertTrue(seen.contains("31,-12") && seen.contains("32,-12"));
        assertSame(chunks, stray.chunks(), "worked out once");
        assertThrows(UnsupportedOperationException.class, () -> chunks.add(new int[]{0, 0}));
    }

    @Test
    void aPlanSaysWhetherItStaysInsideItsCoreCell() {
        ZonePlan inside = base().build();                      // cell (3, -2): x 384..511, z -256..-129; centre (448, -192), half 40 x 30
        assertTrue(inside.insideCell());
        ZonePlan wide = ZonePlan.builder(NexusZone.MONARCH_VAULT, 3, -2, 448, -192).floorY(-52).topY(-34).footprint(Footprint.Shape.RECT, 61, 30).build();
        assertTrue(wide.insideCell(), "61 + 2 is the last block of the cell on the high side (the middle of a cell is its 65th column)");
        ZonePlan tooWide = ZonePlan.builder(NexusZone.MONARCH_VAULT, 3, -2, 448, -192).floorY(-52).topY(-34).footprint(Footprint.Shape.RECT, 62, 30).build();
        assertFalse(tooWide.insideCell());
        ZonePlan off = ZonePlan.builder(NexusZone.MONARCH_VAULT, 3, -2, 448 + 30, -192).floorY(-52).topY(-34).footprint(Footprint.Shape.OVAL, 40, 30).build();
        assertFalse(off.insideCell(), "pushed to the cell's edge");
        ZonePlan golden = ZonePlan.builder(NexusZone.GOLDEN_CITY, -1, -1, -64 + 10, -64 - 10).floorY(-53).topY(-31).footprint(Footprint.Shape.OVAL, 48, 48).build();
        assertTrue(golden.insideCell(), "the oldest Golden City: centre jitter 10, half-width 48");
    }

    @Test
    void aRescuedPlayerIsSetDownAtAnArrivalThenAnEntranceThenTheMiddle() {
        assertEquals(new Spot(448, -51, -192), base().build().rescuePoint());
        assertEquals(new Spot(1, 2, 3), base().entrance(1, 2, 3).entrance(4, 5, 6).build().rescuePoint());
        assertEquals(new Spot(7, 8, 9), base().entrance(1, 2, 3).spawn(ZonePlan.ROLE_ARRIVAL, 7, 8, 9).build().rescuePoint());
    }
}
