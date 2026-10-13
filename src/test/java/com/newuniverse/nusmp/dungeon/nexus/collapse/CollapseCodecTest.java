package com.newuniverse.nusmp.dungeon.nexus.collapse;

import com.newuniverse.nusmp.dungeon.nexus.Footprint;
import com.newuniverse.nusmp.dungeon.nexus.NexusZone;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** What a collapse writes into the world's data and reads back after a restart. */
class CollapseCodecTest {
    private static CollapseState sample() {
        CollapseState s = new CollapseState();
        s.id = 9_000_000_123L;
        s.centreX = -1234;
        s.centreY = -52;
        s.centreZ = 77;
        s.zone = NexusZone.UNDERWORLD_GATE;
        s.nexus = true;
        s.cellX = -10;
        s.cellZ = 0;
        s.startTick = 1_234_567_890_123L;
        s.fp = Footprint.rect(-1280, 64, 56, 40);
        s.floorY = -53;
        s.topY = -12;
        s.exits.add(new int[]{-1230, -52, 60});
        s.exits.add(new int[]{-1330, -52, 64});
        s.exits.add(new int[]{-1280, -52, 100});
        s.gates.add(new int[]{-1230, -52, 61, 1});
        s.gates.add(new int[0]);
        s.gates.add(new int[]{-1280, -51, 99, -1});
        s.cursor = 4321;
        s.wipeCursor = 17;
        s.nextMsg = 5;
        s.nextWave = 3;
        s.spawned = 11;
        s.started = true;
        s.expelled = true;
        s.inside.add(new UUID(1L, 2L));
        s.inside.add(new UUID(-5L, Long.MIN_VALUE));
        return s;
    }

    private static void same(CollapseState a, CollapseState b) {
        assertEquals(a.id, b.id);
        assertEquals(a.startTick, b.startTick);
        assertEquals(a.centreX, b.centreX);
        assertEquals(a.centreY, b.centreY);
        assertEquals(a.centreZ, b.centreZ);
        assertSame(a.zone, b.zone);
        assertEquals(a.nexus, b.nexus);
        assertEquals(a.cellX, b.cellX);
        assertEquals(a.cellZ, b.cellZ);
        assertEquals(a.fp, b.fp);
        assertEquals(a.floorY, b.floorY);
        assertEquals(a.topY, b.topY);
        assertEquals(a.exits.size(), b.exits.size());
        for (int i = 0; i < a.exits.size(); i++) assertArrayEquals(a.exits.get(i), b.exits.get(i));
        assertEquals(a.gates.size(), b.gates.size());
        for (int i = 0; i < a.gates.size(); i++) assertArrayEquals(a.gates.get(i), b.gates.get(i));
        assertEquals(a.cursor, b.cursor);
        assertEquals(a.wipeCursor, b.wipeCursor);
        assertEquals(a.nextMsg, b.nextMsg);
        assertEquals(a.nextWave, b.nextWave);
        assertEquals(a.spawned, b.spawned);
        assertEquals(a.started, b.started);
        assertEquals(a.expelled, b.expelled);
        assertEquals(a.inside, b.inside);
    }

    @Test
    void aStateComesBackExactlyAsItWasSaved() {
        CollapseState s = sample();
        CollapseState back = CollapseCodec.decode(CollapseCodec.ints(s), CollapseCodec.longs(s));
        assertNotNull(back);
        same(s, back);
        same(s, s.copy());
    }

    @Test
    void everyFlagAndShapeSurvivesOnItsOwn() {
        for (int mask = 0; mask < 8; mask++) {
            for (Footprint.Shape shape : Footprint.Shape.values()) {
                for (NexusZone zone : NexusZone.values()) {
                    CollapseState s = sample();
                    s.nexus = (mask & 1) != 0;
                    s.started = (mask & 2) != 0;
                    s.expelled = (mask & 4) != 0;
                    s.fp = new Footprint(shape, 3, 4, 5, 6);
                    s.zone = zone;
                    same(s, CollapseCodec.decode(CollapseCodec.ints(s), CollapseCodec.longs(s)));
                }
            }
        }
    }

    @Test
    void aBareStateWithNoExitsNoGatesNobodyInsideRoundTrips() {
        CollapseState s = new CollapseState();
        s.id = 1;
        s.floorY = 0;
        s.topY = 5;
        CollapseState back = CollapseCodec.decode(CollapseCodec.ints(s), CollapseCodec.longs(s));
        assertNotNull(back);
        same(s, back);
        assertTrue(back.exits.isEmpty() && back.gates.isEmpty() && back.inside.isEmpty());
    }

    @Test
    void extremeNumbersSurvive() {
        CollapseState s = sample();
        s.id = Long.MIN_VALUE;
        s.startTick = Long.MAX_VALUE;
        s.centreX = Integer.MIN_VALUE;
        s.centreZ = Integer.MAX_VALUE;
        s.cursor = Integer.MAX_VALUE;
        same(s, CollapseCodec.decode(CollapseCodec.ints(s), CollapseCodec.longs(s)));
    }

    @Test
    void aDamagedRecordIsRefusedNotTrusted() {
        CollapseState s = sample();
        int[] ints = CollapseCodec.ints(s);
        long[] longs = CollapseCodec.longs(s);
        assertNull(CollapseCodec.decode(null, longs));
        assertNull(CollapseCodec.decode(ints, null));
        assertNull(CollapseCodec.decode(new int[0], longs));
        assertNull(CollapseCodec.decode(ints, new long[1]));
        for (int cut = 0; cut < ints.length; cut++) {
            int[] shorter = java.util.Arrays.copyOf(ints, cut);
            CollapseState back = CollapseCodec.decode(shorter, longs);
            if (back != null) fail("a record cut to " + cut + " of " + ints.length + " ints must not load (or must load a valid prefix)");
        }
        int[] wrongVersion = ints.clone();
        wrongVersion[0] = 99;
        assertNull(CollapseCodec.decode(wrongVersion, longs));
        int[] badShape = ints.clone();
        badShape[8] = 7;
        assertNull(CollapseCodec.decode(badShape, longs));
        int[] noWidth = ints.clone();
        noWidth[11] = 0;
        assertNull(CollapseCodec.decode(noWidth, longs));
        int[] upsideDown = ints.clone();
        upsideDown[13] = 40;
        upsideDown[14] = 30;
        assertNull(CollapseCodec.decode(upsideDown, longs));
        int[] huge = ints.clone();
        huge[11] = 1_000_000;
        assertNull(CollapseCodec.decode(huge, longs), "a footprint a million blocks wide");
        int[] tall = ints.clone();
        tall[14] = 2_000_000_000;
        assertNull(CollapseCodec.decode(tall, longs), "a roof two billion blocks up");
        int[] hugeExits = ints.clone();
        hugeExits[20] = 100_000;
        assertNull(CollapseCodec.decode(hugeExits, longs));
        int[] negativeExits = ints.clone();
        negativeExits[20] = -3;
        assertNull(CollapseCodec.decode(negativeExits, longs));
    }

    @Test
    void anUnknownZoneNumberFallsBackAndNegativeProgressIsRaisedToZero() {
        CollapseState s = sample();
        int[] ints = CollapseCodec.ints(s);
        ints[1] = 77;
        ints[15] = -4;
        ints[16] = -4;
        ints[17] = -1;
        ints[18] = -1;
        ints[19] = -1;
        CollapseState back = CollapseCodec.decode(ints, CollapseCodec.longs(s));
        assertNotNull(back);
        assertSame(NexusZone.GOLDEN_CITY, back.zone);
        assertEquals(0, back.cursor);
        assertEquals(0, back.wipeCursor);
        assertEquals(0, back.nextMsg);
        assertEquals(0, back.nextWave);
        assertEquals(0, back.spawned);
    }

    @Test
    void aGateWithABadLengthIsRefusedAndTheGateSlotsAlwaysMatchTheExits() {
        CollapseState s = sample();
        int[] ints = CollapseCodec.ints(s);
        int gateAt = 21 + 3 * s.exits.size();
        int[] fewer = ints.clone();
        fewer[gateAt] = 1;                                         // claims one gate: the data then carries a stray second
        CollapseState back = CollapseCodec.decode(fewer, CollapseCodec.longs(s));
        if (back != null) assertEquals(back.exits.size(), back.gates.size());
        int[] badLen = ints.clone();
        badLen[gateAt + 1] = 3;
        assertNull(CollapseCodec.decode(badLen, CollapseCodec.longs(s)));
        CollapseState noGates = sample();
        noGates.gates.clear();
        CollapseState b2 = CollapseCodec.decode(CollapseCodec.ints(noGates), CollapseCodec.longs(noGates));
        assertNotNull(b2);
        assertEquals(3, b2.gates.size(), "every exit has a slot");
        assertEquals(0, b2.gates.get(0).length);
    }

    @Test
    void anOddLongIsIgnored() {
        CollapseState s = sample();
        long[] l = java.util.Arrays.copyOf(CollapseCodec.longs(s), 2 + 2 * s.inside.size() + 1);
        l[l.length - 1] = 12345L;
        CollapseState back = CollapseCodec.decode(CollapseCodec.ints(s), l);
        assertNotNull(back);
        assertEquals(s.inside, back.inside);
    }
}
