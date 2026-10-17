package com.newuniverse.nusmp.labyrinth.mega.macro;

import com.newuniverse.nusmp.labyrinth.mega.HubLayout;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: {@link HubModel} is the whole Hub as one function, so these tests state the Hub's promises as assertions on that function: the three things the rest of the
 * mod depends on (the gates stand exactly where {@link HubLayout} says, the doors are open, the plaza is flat at the height the plaza template was built for),
 * the things a player would notice if they broke (the roads are flush with the ground and three wide, the strata are in the right order, the bottom is bedrock),
 * the things that would make the world differ between two runs (determinism, the per-seed cache, the two code paths {@code kindAt} and {@code fillColumn} agreeing,
 * the placeholder kind never leaking out), and the switch that turns the landmarks off.
 */
public class HubModelTest {
    private static final long SEED = 2024L;

    @Test
    public void theSameSeedBuildsTheSameWorldAndAnotherSeedAnotherOne() {
        HubModel a = HubModel.fresh(SEED, MacroParams.DEFAULT), b = HubModel.fresh(SEED, MacroParams.DEFAULT), c = HubModel.fresh(SEED + 1, MacroParams.DEFAULT);
        Random rnd = new Random(7);
        int differing = 0;
        for (int i = 0; i < 4000; i++) {
            int x = rnd.nextInt(700) - 350, z = rnd.nextInt(260) - 130, y = rnd.nextInt(HubModel.HEIGHT) - 64;
            assertEquals(a.kindAt(x, y, z), b.kindAt(x, y, z), "the same seed gives the same block at " + x + " " + y + " " + z);
            if (a.surfaceHeight(x + 300, z + 200) != c.surfaceHeight(x + 300, z + 200)) differing++;
        }
        assertTrue(differing > 300, "another seed has other hills in open country: " + differing);
        assertSame(HubModel.of(SEED), HubModel.of(SEED), "the per-seed cache hands out one model");
        assertTrue(HubModel.of(SEED) != HubModel.of(SEED + 1));
    }

    @Test
    public void theGatesStandExactlyWhereTheLayoutSaysWithTheirFrames() {
        HubModel m = HubModel.of(SEED);
        int[][] gates = {{HubLayout.HOLY_GATE_X, HubLayout.HOLY_GATE_Y, HubLayout.HOLY_GATE_Z}, {HubLayout.ABYSS_GATE_X, HubLayout.ABYSS_GATE_Y, HubLayout.ABYSS_GATE_Z}};
        BlockKind[] frames = {BlockKind.GATE_FRAME, BlockKind.ABYSS_FRAME};
        for (int g = 0; g < 2; g++) {
            int x = gates[g][0], y = gates[g][1], z = gates[g][2];
            assertEquals(BlockKind.GATE, m.kindAt(x, y, z), "the centre of gate " + g + " is a gate block");
            for (int dx = -1; dx <= 1; dx++) for (int dy = -2; dy <= 1; dy++) assertEquals(BlockKind.GATE, m.kindAt(x + dx, y + dy, z), "gate " + g + " plane cell " + dx + "," + dy);
            for (int dy = -2; dy <= 2; dy++) {
                assertEquals(frames[g], m.kindAt(x - 2, y + dy, z), "left pillar of gate " + g);
                assertEquals(frames[g], m.kindAt(x + 2, y + dy, z), "right pillar of gate " + g);
            }
            for (int dx = -2; dx <= 2; dx++) assertEquals(frames[g], m.kindAt(x + dx, y + 2, z), "lintel of gate " + g);
            // in front of the plane there is room to stand, and a floor under it
            for (int dy = -2; dy <= 0; dy++) assertEquals(BlockKind.AIR, m.kindAt(x, y + dy, z + 1), "standing room in front of gate " + g);
            assertTrue(m.kindAt(x, y - 3, z + 1).solid, "a floor under the standing cell of gate " + g);
            assertEquals(HubLayout.gateNear(x, y, z), g == 0 ? HubLayout.GateKind.HOLY : HubLayout.GateKind.ABYSSAL);
        }
    }

    @Test
    public void theDoorsAreOpenAtGroundLevelWithGroundUnderThem() {
        HubModel m = HubModel.of(SEED);
        for (int dy = 0; dy < 4; dy++) {
            assertEquals(BlockKind.AIR, m.kindAt(HubLayout.SPIRE_DOOR_X, HubLayout.SURFACE_Y + dy, HubLayout.SPIRE_DOOR_Z), "Spire doorway +" + dy);
            assertEquals(BlockKind.AIR, m.kindAt(HubLayout.CHASM_DOOR_X, HubLayout.SURFACE_Y + dy, HubLayout.CHASM_DOOR_Z), "Chasm door +" + dy);
        }
        assertTrue(m.kindAt(HubLayout.SPIRE_DOOR_X, HubLayout.SURFACE_Y - 1, HubLayout.SPIRE_DOOR_Z).solid, "the Spire's sill is solid");
        assertTrue(m.kindAt(HubLayout.CHASM_DOOR_X, HubLayout.SURFACE_Y - 1, HubLayout.CHASM_DOOR_Z).solid, "the Chasm's door has ground under it");
        // the doorway is as wide as the road is not: the cells beside the middle of the doorway are open too (4 wide)
        for (int dz = -1; dz <= 2; dz++) assertEquals(BlockKind.AIR, m.kindAt(HubLayout.SPIRE_DOOR_X, HubLayout.SURFACE_Y + 1, HubLayout.SPIRE_DOOR_Z + dz), "doorway width at dz " + dz);
    }

    @Test
    public void thePlazaIsFlatAtTheHeightTheTemplateWasBuiltForAndTheHillsAreBounded() {
        HubModel m = HubModel.of(SEED);
        for (int x = -HubLayout.PLAZA_RADIUS; x <= HubLayout.PLAZA_RADIUS; x += 2)
            for (int z = -HubLayout.PLAZA_RADIUS; z <= HubLayout.PLAZA_RADIUS; z += 2)
                if (x * x + z * z <= HubLayout.PLAZA_RADIUS * HubLayout.PLAZA_RADIUS) assertEquals(HubLayout.SURFACE_Y, m.surfaceHeight(x, z), "the plaza disc at " + x + "," + z);
        int lo = 999, hi = -999;
        for (int x = -420; x <= 420; x += 4)
            for (int z = -300; z <= 300; z += 4) {
                int h = m.surfaceHeight(x, z);
                lo = Math.min(lo, h);
                hi = Math.max(hi, h);
            }
        assertTrue(hi - HubLayout.SURFACE_Y <= 5 && HubLayout.SURFACE_Y - lo <= 5, "the hills stay within the amplitude: " + lo + " .. " + hi);
        assertTrue(hi - lo >= 4, "there are hills at all: " + lo + " .. " + hi);
        // flat round both landmarks (inside the radius the hills fade out over) and along the roads
        for (int dx = -50; dx <= 50; dx += 5)
            for (int dz = -50; dz <= 50; dz += 5) {
                if (dx * dx + dz * dz <= 50 * 50) assertEquals(HubLayout.SURFACE_Y, m.surfaceHeight(HubLayout.SPIRE_X + dx, HubLayout.SPIRE_Z + dz), "flat round the Spire at " + dx + "," + dz);
                if (dx * dx + dz * dz <= 62 * 62) assertEquals(HubLayout.SURFACE_Y, m.surfaceHeight(HubLayout.CHASM_X + dx, HubLayout.CHASM_Z + dz), "flat round the Chasm at " + dx + "," + dz);
            }
        for (int x = -150; x <= 160; x++) for (int z = -3; z <= 3; z++) assertEquals(HubLayout.SURFACE_Y, m.surfaceHeight(x, z), "flat along the road at " + x + "," + z);
    }

    @Test
    public void theRoadsAreThreeWideFlushWithTheGroundAndLitEveryTwelveBlocks() {
        HubModel m = HubModel.of(SEED);
        // the road runs up to the foot of the Spire's portico (the tower's collar at the door starts about 4 blocks before the nominal wall)
        for (int x = 12; x < HubLayout.SPIRE_DOOR_X - 6; x++) {
            for (int z = -1; z <= 1; z++) {
                assertEquals(BlockKind.PATH, m.kindAt(x, HubLayout.SURFACE_Y - 1, z), "east road at " + x + "," + z);
                assertEquals(BlockKind.AIR, m.kindAt(x, HubLayout.SURFACE_Y, z), "nothing on the east road at " + x + "," + z);
            }
            assertTrue(m.kindAt(x, HubLayout.SURFACE_Y - 1, 2) != BlockKind.PATH, "the road is three wide at " + x);
        }
        for (int x = HubLayout.CHASM_DOOR_X + 1; x <= -12; x++) assertEquals(BlockKind.PATH, m.kindAt(x, HubLayout.SURFACE_Y - 1, 0), "west road at " + x);
        int posts = 0;
        for (int x = 12; x < HubLayout.SPIRE_DOOR_X; x++) {
            for (int z : new int[] {-3, 3}) {
                if (m.kindAt(x, HubLayout.SURFACE_Y, z) == BlockKind.POST) {
                    posts++;
                    assertEquals(BlockKind.LANTERN, m.kindAt(x, HubLayout.SURFACE_Y + 1, z), "a lantern on top of the post at " + x + "," + z);
                    assertEquals(0, x % 12, "posts every twelve blocks");
                }
            }
        }
        assertTrue(posts == 18, "the east road has lamp posts on both sides: " + posts);
    }

    @Test
    public void theStrataAreInOrderFromBedrockToGrass() {
        HubModel m = HubModel.of(SEED);
        int x = 400, z = 150; // open country
        int h = m.surfaceHeight(x, z);
        assertEquals(BlockKind.BEDROCK, m.kindAt(x, HubModel.MIN_Y, z));
        assertEquals(BlockKind.DEEPSLATE, m.kindAt(x, -30, z));
        assertEquals(BlockKind.STONE, m.kindAt(x, 20, z));
        assertEquals(BlockKind.GRASS, m.kindAt(x, h - 1, z));
        assertEquals(BlockKind.DIRT, m.kindAt(x, h - 2, z));
        assertEquals(BlockKind.AIR, m.kindAt(x, h, z));
        assertEquals(BlockKind.AIR, m.kindAt(x, HubModel.MAX_Y + 5, z));
        assertEquals(BlockKind.BEDROCK, m.kindAt(x, HubModel.MIN_Y - 3, z));
        int bedrock2 = 0;
        for (int i = 0; i < 200; i++) if (m.kindAt(300 + i, HubModel.MIN_Y + 1, 200) == BlockKind.BEDROCK) bedrock2++;
        assertTrue(bedrock2 > 60 && bedrock2 < 140, "the second layer of bedrock is noisy: " + bedrock2 + " of 200");
    }

    @Test
    public void theTwoCodePathsAgreeAndThePlaceholderNeverEscapes() {
        HubModel m = HubModel.of(SEED);
        BlockKind[] col = new BlockKind[HubModel.HEIGHT];
        Random rnd = new Random(99);
        int[][] centres = {{HubLayout.SPIRE_X, HubLayout.SPIRE_Z, 50}, {HubLayout.CHASM_X, HubLayout.CHASM_Z, 78}, {0, 0, 60}};
        int landmarkVoxels = 0;
        for (int i = 0; i < 700; i++) {
            int[] c = centres[i % 3];
            int x = c[0] + rnd.nextInt(2 * c[2] + 1) - c[2], z = c[1] + rnd.nextInt(2 * c[2] + 1) - c[2];
            m.fillColumn(x, z, col);
            for (int y = HubModel.MIN_Y; y <= HubModel.MAX_Y; y += 1 + rnd.nextInt(3)) {
                BlockKind k = col[y - HubModel.MIN_Y];
                assertEquals(k, m.kindAt(x, y, z), "fillColumn and kindAt agree at " + x + " " + y + " " + z);
                assertTrue(k != BlockKind.NATIVE, "the placeholder kind never escapes: " + x + " " + y + " " + z);
                if (k != BlockKind.AIR && k != BlockKind.GRASS && k != BlockKind.DIRT && k != BlockKind.STONE && k != BlockKind.DEEPSLATE && k != BlockKind.BEDROCK) landmarkVoxels++;
            }
            // a ranged fill touches only its rows
            BlockKind[] part = new BlockKind[HubModel.HEIGHT];
            m.fillColumn(x, z, part, 60, 100);
            for (int y = 60; y <= 100; y++) assertEquals(col[y - HubModel.MIN_Y], part[y - HubModel.MIN_Y]);
            assertNull(part[0]);
        }
        assertTrue(landmarkVoxels > 800, "the sample really visited the landmarks: " + landmarkVoxels);
    }

    @Test
    public void topYIsTheFirstFreeCellAboveTheHighestBlock() {
        HubModel m = HubModel.of(SEED);
        BlockKind[] col = new BlockKind[HubModel.HEIGHT];
        Random rnd = new Random(5);
        for (int i = 0; i < 300; i++) {
            int x = (i % 2 == 0 ? HubLayout.SPIRE_X : HubLayout.CHASM_X) + rnd.nextInt(100) - 50, z = rnd.nextInt(100) - 50;
            m.fillColumn(x, z, col);
            int top = HubModel.MAX_Y;
            while (top > HubModel.MIN_Y && col[top - HubModel.MIN_Y] == BlockKind.AIR) top--;
            assertEquals(top + 1, m.topY(x, z), "topY at " + x + "," + z);
        }
        assertEquals(HubLayout.SURFACE_Y, m.topY(400, 0) >= HubLayout.SURFACE_Y - 6 ? HubLayout.SURFACE_Y : -1);
        assertTrue(m.topY(HubLayout.SPIRE_X + 25, HubLayout.SPIRE_Z + 5) > HubLayout.SURFACE_Y + 60, "the tower is tall");
    }

    @Test
    public void withTheLandmarksOffTheHubIsPlainPlainsWithTwoGatesOnSmallPlatforms() {
        MacroParams off = new MacroParams(6, 2.0, 3.0, 5.0, 12, false);
        HubModel m = HubModel.fresh(SEED, off);
        assertNull(m.spire());
        assertNull(m.chasm());
        assertEquals(BlockKind.GATE, m.kindAt(HubLayout.HOLY_GATE_X, HubLayout.HOLY_GATE_Y, HubLayout.HOLY_GATE_Z));
        assertEquals(BlockKind.GATE, m.kindAt(HubLayout.ABYSS_GATE_X, HubLayout.ABYSS_GATE_Y, HubLayout.ABYSS_GATE_Z));
        assertTrue(m.kindAt(HubLayout.HOLY_GATE_X, HubLayout.HOLY_GATE_Y - 3, HubLayout.HOLY_GATE_Z + 1).solid, "the Holy gate has a platform");
        assertEquals(BlockKind.AIR, m.kindAt(HubLayout.SPIRE_X + 25, 100, HubLayout.SPIRE_Z), "no tower");
        assertEquals(BlockKind.GRASS, m.kindAt(HubLayout.CHASM_X, HubLayout.SURFACE_Y - 1, HubLayout.CHASM_Z + 3), "no pit");
        assertEquals(BlockKind.AIR, m.kindAt(HubLayout.ABYSS_GATE_X, HubLayout.ABYSS_GATE_Y, HubLayout.ABYSS_GATE_Z + 1), "standing room in the pocket of the Abyssal gate");
        assertEquals(0L, m.wfcStats()[2]);
        BlockKind[] col = new BlockKind[HubModel.HEIGHT];
        m.fillColumn(HubLayout.HOLY_GATE_X, HubLayout.HOLY_GATE_Z, col);
        assertEquals(BlockKind.GATE, col[HubLayout.HOLY_GATE_Y - HubModel.MIN_Y]);
        assertTrue(m.topY(HubLayout.HOLY_GATE_X, HubLayout.HOLY_GATE_Z) > HubLayout.HOLY_GATE_Y);
    }

    @Test
    public void theProfilesOfTheTwoLandmarksTaperAsTheLayoutPromises() {
        assertEquals((double) HubLayout.SPIRE_BASE_RADIUS, SpireModel.outer(HubLayout.SURFACE_Y), 1e-9);
        assertEquals((double) HubLayout.SPIRE_TOP_RADIUS, SpireModel.outer(HubLayout.SURFACE_Y + HubLayout.SPIRE_HEIGHT), 1e-9);
        double prev = 1e9;
        for (int y = HubLayout.SURFACE_Y; y <= SpireModel.TOP; y++) {
            double r = SpireModel.outer(y);
            assertTrue(r <= prev + 1e-12, "the tower never widens going up, at y " + y);
            prev = r;
        }
        assertEquals((double) HubLayout.CHASM_RIM_RADIUS, ChasmModel.rwall(HubLayout.SURFACE_Y), 1e-9);
        assertEquals((double) HubLayout.CHASM_FLOOR_RADIUS, ChasmModel.rwall(ChasmModel.FLOOR_STAND), 1e-9);
        prev = -1e9;
        for (int y = ChasmModel.FLOOR_STAND; y <= HubLayout.SURFACE_Y; y++) {
            double r = ChasmModel.rwall(y);
            assertTrue(r >= prev - 1e-12, "the pit never narrows going up, at y " + y);
            prev = r;
        }
        assertEquals(HubLayout.ABYSS_GATE_Y - 2, ChasmModel.FLOOR_STAND);
        assertEquals(HubLayout.HOLY_GATE_Y - 3, SpireModel.PLATFORM_TOP);
        assertNotNull(HubModel.of(SEED).spire());
    }

    @Test
    public void theCrownOfTheSpireHasEightPinnaclesWithAGoldCapAndALantern() {
        HubModel m = HubModel.of(SEED);
        int pillars = 0;
        for (int x = HubLayout.SPIRE_X - 24; x <= HubLayout.SPIRE_X + 24; x++)
            for (int z = HubLayout.SPIRE_Z - 24; z <= HubLayout.SPIRE_Z + 24; z++) {
                if (m.kindAt(x, SpireModel.TOP, z) != BlockKind.QUARTZ_PILLAR) continue;
                pillars++;
                assertEquals(BlockKind.QUARTZ_PILLAR, m.kindAt(x, SpireModel.TOP + SpireModel.PINNACLE_HEIGHT - 2, z));
                assertEquals(BlockKind.GOLD_TRIM, m.kindAt(x, SpireModel.TOP + SpireModel.PINNACLE_HEIGHT - 1, z));
                assertEquals(BlockKind.LANTERN, m.kindAt(x, SpireModel.TOP + SpireModel.PINNACLE_HEIGHT, z));
                assertEquals(BlockKind.AIR, m.kindAt(x, SpireModel.TOP + SpireModel.PINNACLE_HEIGHT + 1, z));
                assertTrue(m.kindAt(x, SpireModel.TOP - 1, z) != BlockKind.AIR, "a pinnacle stands on the cornice");
            }
        assertEquals(8, pillars, "eight pinnacles");
        assertTrue(SpireModel.TOP + SpireModel.PINNACLE_HEIGHT + 1 <= HubModel.MAX_Y, "they fit under the build limit");
    }

    @Test
    public void theShaftOfTheSpireIsHollowAndTheCentreOfTheChasmIsOpenToTheRim() {
        HubModel m = HubModel.of(SEED);
        for (int y = HubLayout.SURFACE_Y; y < SpireModel.PLATFORM_TOP - 2; y++)
            for (int dx = -4; dx <= 4; dx += 4) for (int dz = -4; dz <= 4; dz += 4) {
                BlockKind k = m.kindAt(HubLayout.SPIRE_X + dx, y, HubLayout.SPIRE_Z + dz);
                if (dx == 0 && dz == 0 && y >= SpireModel.CHANDELIER_FOOT) assertTrue(k == BlockKind.CHAIN_Y || k == BlockKind.LANTERN_HANGING, "the great chandelier hangs on the axis at " + y + ": " + k);
                else assertEquals(BlockKind.AIR, k, "the Spire's shaft is empty at " + dx + "," + y + "," + dz);
            }
        for (int y = ChasmModel.FLOOR_STAND; y <= HubLayout.SURFACE_Y + 1; y++) assertEquals(BlockKind.AIR, m.kindAt(HubLayout.CHASM_X + 1, y, HubLayout.CHASM_Z + 5), "the Chasm's centre is open at y " + y);
        assertFalse(m.kindAt(HubLayout.CHASM_X + 1, ChasmModel.FLOOR_TOP, HubLayout.CHASM_Z + 5) == BlockKind.AIR, "and has a floor");
    }
}
