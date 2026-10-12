package com.newuniverse.nusmp.dungeon.nexus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** What the Variable Core remembers: the zone, the progress, the forced zone, the seal. */
class NexusSaveTest {
    private static ZonePlan plan(NexusZone zone, int cellX, int cellZ) {
        return ZonePlan.builder(zone, cellX, cellZ, NexusZone.centre(cellX), NexusZone.centre(cellZ)).floorY(-52).topY(-34).footprint(Footprint.Shape.OVAL, 40, 30).build();
    }

    @Test
    void aCellNobodyTouchedIsUnbuiltAndTakesItsHashedZone() {
        NexusSave save = new NexusSave();
        long key = NexusSave.key(3, -4);
        assertEquals(NexusState.UNBUILT, save.state(key));
        assertSame(NexusZone.of(3, -4), save.zoneOf(3, -4));
        assertNull(save.lookup(key));
        assertFalse(save.sealBroken(key));
        assertFalse(save.centreRelicTaken(key));
        assertEquals(0, save.sealedCount());
    }

    @Test
    void aForcedZoneHoldsUntilABuildStartsAndThenTheBuildFixesIt() {
        NexusSave save = new NexusSave();
        long key = NexusSave.key(2, 2);
        NexusZone other = NexusZone.values()[(NexusZone.of(2, 2).ordinal() + 1) % 4];
        assertTrue(save.forceZone(key, other));
        assertSame(other, save.zoneOf(2, 2));
        assertEquals(NexusState.UNBUILT, save.state(key), "forcing is not building");
        NexusZone third = NexusZone.values()[(other.ordinal() + 1) % 4];
        assertTrue(save.forceZone(key, third), "it can be forced again");
        assertSame(third, save.zoneOf(2, 2));
        save.begin(key, third, plan(third, 2, 2));
        assertEquals(NexusState.PLAN, save.state(key));
        assertFalse(save.forceZone(key, other), "a started cell cannot be forced");
        assertSame(third, save.zoneOf(2, 2));
        save.lookup(key).override = null;
        assertSame(third, save.zoneOf(2, 2), "the build's zone beats the rest");
    }

    @Test
    void progressMovesPlanToBuildingToDoneAndKeepsTheGroundItWasBuiltOn() {
        NexusSave save = new NexusSave();
        long key = NexusSave.key(0, 0);
        ZonePlan p = plan(NexusZone.MONARCH_VAULT, 0, 0);
        NexusSave.Entry e = save.begin(key, NexusZone.MONARCH_VAULT, p);
        assertSame(p.footprint, e.fp);
        assertEquals(-52, e.floorY);
        assertEquals(-34, e.topY);
        save.progress(key, 1, 17, 4_000L);
        assertEquals(NexusState.BUILDING, save.state(key));
        assertEquals(1, e.stage);
        assertEquals(17, e.op);
        assertEquals(4_000L, e.cell);
        save.begin(key, NexusZone.MONARCH_VAULT, p);
        assertEquals(NexusState.BUILDING, save.state(key), "beginning again does not set a cell back");
        assertEquals(17, e.op);
        save.finish(key);
        assertEquals(NexusState.DONE, save.state(key));
        assertEquals(0, e.op);
        assertEquals(0, e.stage);
        save.progress(key, 0, 5, 5L);
        assertEquals(NexusState.DONE, save.state(key), "a late progress report does not unbuild it");
        save.markCollapsed(key);
        assertEquals(NexusState.COLLAPSED, save.state(key));
    }

    @Test
    void theSealBreaksOnceAndIsCounted() {
        NexusSave save = new NexusSave();
        long a = NexusSave.key(1, 1), b = NexusSave.key(5, 5);
        assertFalse(save.sealBroken(a));
        save.breakSeal(a);
        save.breakSeal(a);
        assertTrue(save.sealBroken(a));
        assertFalse(save.sealBroken(b));
        assertEquals(1, save.sealedCount());
        save.breakSeal(b);
        assertEquals(2, save.sealedCount());
    }

    @Test
    void theCentreRelicIsTakenOnce() {
        NexusSave save = new NexusSave();
        long key = NexusSave.key(9, 9);
        save.markCentreRelicTaken(key);
        save.markCentreRelicTaken(key);
        assertTrue(save.centreRelicTaken(key));
        assertFalse(save.centreRelicTaken(NexusSave.key(9, 8)));
    }

    @Test
    void forgettingACellMakesItNewButKeepsAForcedZone() {
        NexusSave save = new NexusSave();
        long key = NexusSave.key(4, 4);
        NexusZone forced = NexusZone.values()[(NexusZone.of(4, 4).ordinal() + 2) % 4];
        save.forceZone(key, forced);
        save.begin(key, forced, plan(forced, 4, 4));
        save.finish(key);
        save.breakSeal(key);
        save.markCentreRelicTaken(key);
        assertTrue(save.forget(key));
        assertEquals(NexusState.UNBUILT, save.state(key));
        assertFalse(save.sealBroken(key));
        assertFalse(save.centreRelicTaken(key));
        assertSame(forced, save.zoneOf(4, 4), "the forced zone stays, for the next build");
        assertTrue(save.forceZone(key, NexusZone.of(4, 4)), "and a reset cell can be forced again");
        assertFalse(save.forget(NexusSave.key(70, 70)), "nothing to forget");
        long plain = NexusSave.key(6, 6);
        NexusZone built = NexusZone.of(6, 6);
        save.begin(plain, built, plan(built, 6, 6));
        assertTrue(save.forget(plain));
        assertNull(save.lookup(plain), "an unforced cell leaves no record");
        assertSame(NexusZone.of(6, 6), save.zoneOf(6, 6));
    }

    @Test
    void theRecordSurvivesSavingAndLoading() {
        NexusSave save = new NexusSave();
        long a = NexusSave.key(-2, 3), b = NexusSave.key(7, 7), c = NexusSave.key(8, -1);
        ZonePlan p = plan(NexusZone.UNDERWORLD_GATE, -2, 3);
        save.begin(a, NexusZone.UNDERWORLD_GATE, p);
        save.progress(a, 2, 31, 123_456_789L);
        save.breakSeal(a);
        save.begin(b, NexusZone.TEMPEST_MIRAGE, plan(NexusZone.TEMPEST_MIRAGE, 7, 7));
        save.finish(b);
        save.markCentreRelicTaken(b);
        save.markCollapsed(b);
        save.forceZone(c, NexusZone.GOLDEN_CITY);
        NexusSave back = NexusSave.load(save.save(new net.minecraft.nbt.CompoundTag(), null), null);
        assertEquals(NexusState.BUILDING, back.state(a));
        assertSame(NexusZone.UNDERWORLD_GATE, back.zoneOf(-2, 3));
        NexusSave.Entry ea = back.lookup(a);
        assertEquals(2, ea.stage);
        assertEquals(31, ea.op);
        assertEquals(123_456_789L, ea.cell);
        assertTrue(ea.sealBroken);
        assertEquals(p.footprint, ea.fp);
        assertEquals(-52, ea.floorY);
        assertEquals(-34, ea.topY);
        assertEquals(NexusState.COLLAPSED, back.state(b));
        assertTrue(back.centreRelicTaken(b));
        assertSame(NexusZone.TEMPEST_MIRAGE, back.zoneOf(7, 7));
        assertEquals(NexusState.UNBUILT, back.state(c));
        assertSame(NexusZone.GOLDEN_CITY, back.zoneOf(8, -1), "a forced zone is saved");
        assertEquals(1, back.sealedCount());
        assertEquals(3, back.keys().size());
    }
}
