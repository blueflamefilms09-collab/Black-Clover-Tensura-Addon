package com.newuniverse.nusmp.labyrinth.mega;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: since 1.58.0 the saved data of the dungeon holds the floors of every party, and the one rule the whole instancing leans on is that a question about "the Holy road" is
 * a question about ONE party's road: the old one-argument lookups mean the shared chain (party 0, where every world saved before parties lives), the new ones name the party,
 * the party is saved with the sector but party 0 is left out of the save (so an old world's data does not change by a byte), a damaged party number cannot name a lane that
 * does not exist, and the position lookup that most systems call every tick still finds the right sector when several parties' floors are in the data.
 */
public class MegaDataPartyTest {
    private static Sector floor(MegaData d, int party, Polarity road, int chain, int x, int z) {
        Sector s = new Sector(d.sectors().size());
        s.party = party;
        s.polarity = road;
        s.chainIndex = chain;
        s.perRegion = 1;
        s.state = Sector.State.ACTIVE;
        s.region = new BoundingBox(x, 56, z - 85, x + 170, 103, z + 85);
        d.add(s);
        return s;
    }

    private static MegaData world() {
        MegaData d = new MegaData();
        floor(d, 0, Polarity.HOLY, 0, 0, 0);
        floor(d, 1, Polarity.HOLY, 0, 0, -4096);
        floor(d, 0, Polarity.HOLY, 1, 198, 0);
        floor(d, 1, Polarity.ABYSSAL, 0, 0, MegaGrid.laneZ(Polarity.ABYSSAL, 1));
        floor(d, 1, Polarity.HOLY, 1, 198, -4096);
        floor(d, 0, Polarity.ABYSSAL, 0, 0, MegaGrid.LANE_Z);
        return d;
    }

    @Test
    public void theChainsOfDifferentPartiesAreSeparateAndTheOldLookupsMeanPartyZero() {
        MegaData d = world();
        assertEquals(2, d.chain(Polarity.HOLY).size());
        assertEquals(2, d.chain(0, Polarity.HOLY).size());
        assertEquals(2, d.chain(1, Polarity.HOLY).size());
        assertEquals(1, d.chain(1, Polarity.ABYSSAL).size());
        assertEquals(0, d.chain(2, Polarity.HOLY).size());
        for (Sector s : d.chain(1, Polarity.HOLY)) assertEquals(1, s.party);
        for (Sector s : d.chain(Polarity.HOLY)) assertEquals(0, s.party, "the one-argument lookup is the shared chain");
        List<Sector> chain = d.chain(1, Polarity.HOLY);
        assertEquals(0, chain.get(0).chainIndex);
        assertEquals(1, chain.get(1).chainIndex);
    }

    @Test
    public void aSectorAndTheTailAreFoundInTheirOwnPartyOnly() {
        MegaData d = world();
        Sector p1c1 = d.chainSector(1, Polarity.HOLY, 1);
        assertNotNull(p1c1);
        assertEquals(1, p1c1.party);
        assertNull(d.chainSector(1, Polarity.ABYSSAL, 1));
        assertNull(d.chainSector(2, Polarity.HOLY, 0));
        assertEquals(0, d.chainSector(Polarity.HOLY, 1).party, "the old lookup answers the shared chain, not party 1's sector of the same place");
        assertSame(d.chainSector(0, Polarity.HOLY, 0), d.chainSector(Polarity.HOLY, 0));
        assertEquals(1, d.chainTail(1, Polarity.HOLY).chainIndex);
        assertEquals(1, d.chainTail(Polarity.HOLY).chainIndex);
        assertEquals(0, d.chainTail(1, Polarity.ABYSSAL).chainIndex);
        assertNull(d.chainTail(3, Polarity.HOLY));
    }

    @Test
    public void thePartiesAndTheirSectorCountsAreReported() {
        MegaData d = world();
        assertEquals(List.of(0, 1), d.parties());
        assertEquals(3, d.sectorCount(0));
        assertEquals(3, d.sectorCount(1));
        assertEquals(0, d.sectorCount(2));
        assertEquals(List.of(), new MegaData().parties());
    }

    @Test
    public void thePositionLookupFindsTheRightPartysFloorWhateverWasAskedBefore() {
        MegaData d = world();
        // ask in an order that fills the small memory of recent answers, then ask for everything again in another order: every answer must be the sector that holds the cell
        int[][] cells = {{50, 64, 0}, {50, 64, -4096}, {250, 64, 0}, {50, 64, 300000}, {250, 64, -4096}, {50, 64, 304096}, {50, 64, 0}, {250, 64, -4096}, {50, 64, 304096}, {50, 64, -4096}};
        for (int round = 0; round < 3; round++) {
            for (int i = 0; i < cells.length; i++) {
                int[] c = cells[(i * 7 + round) % cells.length];
                Sector s = d.sectorAt(new BlockPos(c[0], c[1], c[2]));
                assertNotNull(s, "a cell inside a floor");
                assertTrue(s.containsRegion(new BlockPos(c[0], c[1], c[2])));
                assertEquals(c[2] < -2000 ? 1 : c[2] > 304000 ? 1 : 0, s.party, "cell " + c[0] + "," + c[2]);
            }
        }
        assertNull(d.sectorAt(new BlockPos(50, 64, 150_000)), "the empty middle between the roads");
        assertNull(d.sectorAt(new BlockPos(50, 64, -2048)), "between the lanes of two parties");
    }

    @Test
    public void aFailedSectorIsNeverFoundByPosition() {
        MegaData d = world();
        BlockPos in = new BlockPos(50, 64, -4096);
        Sector s = d.sectorAt(in);
        assertNotNull(s);
        s.state = Sector.State.FAILED;
        assertNull(d.sectorAt(in), "the memory of recent answers must not keep a sector that has failed");
        s.state = Sector.State.ACTIVE;
        assertSame(s, d.sectorAt(in));
    }

    @Test
    public void thePartyIsSavedWithTheSectorAndPartyZeroIsLeftOut() {
        MegaData d = world();
        Sector shared = d.chainSector(0, Polarity.HOLY, 0), other = d.chainSector(1, Polarity.HOLY, 1);
        CompoundTag a = shared.save(), b = other.save();
        assertFalse(a.contains("party"), "party 0 is not written, so a world without parties saves byte for byte what it saved");
        assertTrue(b.contains("party"));
        assertEquals(1, b.getInt("party"));
        assertEquals(0, Sector.load(a).party);
        assertEquals(1, Sector.load(b).party);
        MegaData copy = MegaData.load(d.save(new CompoundTag(), null), null);
        assertEquals(6, copy.sectors().size());
        assertEquals(List.of(0, 1), copy.parties());
        assertEquals(2, copy.chain(1, Polarity.HOLY).size());
        assertEquals(1, copy.chainSector(1, Polarity.ABYSSAL, 0).party);
    }

    @Test
    public void aSectorSavedBeforePartiesReadsAsPartyZeroAndADamagedNumberCannotNameAMissingLane() {
        CompoundTag old = new CompoundTag();
        old.putInt("id", 4);
        old.putString("pol", "holy");
        old.putInt("chain", 4);
        assertEquals(0, Sector.load(old).party);
        CompoundTag wild = old.copy();
        wild.putInt("party", 9999);
        assertEquals(MegaGrid.MAX_PARTIES - 1, Sector.load(wild).party);
        wild.putInt("party", -7);
        assertEquals(0, Sector.load(wild).party);
    }
}
