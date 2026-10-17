package com.newuniverse.nusmp.labyrinth.mega.macro;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: a tile is a promise, in two forms that must agree: its sockets (what the solver trusts) and its voxels (what the world gets). If two tiles whose sockets
 * match show different openings where they touch, the assembled chamber has a doorway that ends in a wall, or a cavern that is sealed by one stray block, and
 * nothing in the solver can notice, because the solver never looks at voxels. So this checks the voxels against the sockets for EVERY pair and every face of
 * both real tilesets, and every flavour of the dressing of the caves, and it checks the safety properties of the art: a doorway of a tile that is pre-collapsed
 * behind a landing is open and wide enough to walk through, no water sits where it could drain or flow, no pattern uses the placeholder kind, no pattern can drop
 * a player into lava (there is no lava kind in the closed vocabulary at all).
 */
public class TileSetsTest {

    private static void seams(Wfc3D.Tileset ts, TilePattern[][] patterns, String what) {
        int checked = 0;
        for (int t = 0; t < ts.size(); t++) {
            for (int d = 0; d < 6; d++) {
                for (int u = 0; u < ts.size(); u++) {
                    if (!ts.compatible(t, d, u)) continue;
                    for (int ft = 0; ft < patterns[t].length; ft++) {
                        for (int fu = 0; fu < patterns[u].length; fu++) {
                            boolean[] mine = patterns[t][ft].openFace(d), theirs = patterns[u][fu].openFace(d ^ 1);
                            for (int i = 0; i < mine.length; i++) assertEquals(mine[i], theirs[i], what + ": tile " + ts.tile(t).name + "/" + ft + " face " + d + " against " + ts.tile(u).name + "/" + fu + " differs at index " + i);
                            checked++;
                        }
                    }
                }
            }
        }
        assertTrue(checked > 100, "the seam test really looked at pairs: " + checked);
    }

    private static TilePattern[][] spirePatterns() {
        TilePattern[][] p = new TilePattern[SpireTiles.PATTERNS.length][];
        for (int i = 0; i < p.length; i++) p[i] = new TilePattern[] {SpireTiles.PATTERNS[i]};
        return p;
    }

    @Test
    public void matchingSocketsOfTheSpireShowTheSameOpeningWhereTheyTouch() {
        seams(SpireTiles.TILESET, spirePatterns(), "spire");
    }

    @Test
    public void matchingSocketsOfTheChasmShowTheSameOpeningInEveryFlavour() {
        seams(ChasmTiles.TILESET, ChasmTiles.PATTERNS, "chasm");
    }

    @Test
    public void facesToTheShaftAndTheOutsideAgreeOnTheirDoorsAndWindows() {
        // the doorway onto a landing and the front door are the same size on every tile that has them, and the windows are the same shape
        Wfc3D.Tileset ts = SpireTiles.TILESET;
        for (int d : new int[] {4, 5}) {
            for (int t = 0; t < ts.size(); t++)
                for (int u = 0; u < ts.size(); u++) {
                    if (ts.tile(t).sockets[d] != ts.tile(u).sockets[d]) continue;
                    boolean[] a = SpireTiles.PATTERNS[t].openFace(d), b = SpireTiles.PATTERNS[u].openFace(d);
                    for (int i = 0; i < a.length; i++) assertEquals(a[i], b[i], "face " + d + " of " + ts.tile(t).name + " and " + ts.tile(u).name + " at " + i);
                }
        }
    }

    @Test
    public void everyTileHasAPositiveWeightAUniqueNameAndSocketsInRange() {
        for (Wfc3D.Tileset ts : new Wfc3D.Tileset[] {SpireTiles.TILESET, ChasmTiles.TILESET}) {
            Set<String> names = new HashSet<>();
            for (int i = 0; i < ts.size(); i++) {
                assertTrue(ts.tile(i).weight > 0);
                assertTrue(names.add(ts.tile(i).name), "unique name " + ts.tile(i).name);
                for (int s : ts.tile(i).sockets) assertTrue(s >= 0 && s < 32);
            }
        }
        assertEquals(11, SpireTiles.TILESET.size(), "the Spire's trial chambers are 11 tiles");
        assertEquals(19, ChasmTiles.TILESET.size(), "the Chasm's caverns are 19 tiles");
    }

    @Test
    public void thePreCollapsedDoorwaysAreOpenAndWideEnoughToWalkThrough() {
        // the doorway onto a landing: layer c = 0, a = 1..4, b = 1..3 (and the middle of the top row) must be passable, the sill row b = 0 solid
        for (int tile : new int[] {SpireTiles.HALL_DOOR, SpireTiles.PASSAGE}) {
            TilePattern p = SpireTiles.PATTERNS[tile];
            for (int a = 1; a <= 4; a++) {
                assertTrue(p.get(a, 0, 0).supportsStanding(), "sill of tile " + tile + " at a=" + a);
                for (int b = 1; b <= 3; b++) assertTrue(p.get(a, b, 0).passable, "door of tile " + tile + " at a=" + a + " b=" + b);
            }
        }
        // the front door goes right through: the two cells of a walking body are free from the outside to the shaft (a chandelier hangs above head height)
        TilePattern pass = SpireTiles.PATTERNS[SpireTiles.PASSAGE];
        for (int a = 1; a <= 4; a++) for (int b = 1; b <= 2; b++) for (int c = 0; c < 6; c++) assertTrue(pass.get(a, b, c).passable, "passage at " + a + "," + b + "," + c);
        // the cave mouth behind a landing: a round hole of the face, open through the whole cell from the pit to the rock
        TilePattern mouth = ChasmTiles.PATTERNS[ChasmTiles.MOUTH][ChasmTiles.PLAIN];
        for (int c = 0; c < 6; c++) {
            assertTrue(mouth.get(2, 2, c).passable && mouth.get(3, 2, c).passable && mouth.get(2, 3, c).passable && mouth.get(3, 3, c).passable, "mouth tunnel at depth " + c);
            assertTrue(mouth.get(2, 1, c).supportsStanding() || mouth.get(2, 1, c).passable);
        }
    }

    @Test
    public void noPatternUsesThePlaceholderKindAfterItsOwnFlavourAndNoTileLeavesTheVocabulary() {
        for (TilePattern p : SpireTiles.PATTERNS) assertTrue(p.count(BlockKind.NATIVE) > 0, "every spire tile has some masonry");
        for (TilePattern[] ps : ChasmTiles.PATTERNS) for (TilePattern p : ps) assertTrue(p.count(BlockKind.NATIVE) > 0, "every cave tile has some rock");
    }

    @Test
    public void waterOnlySitsInASealedPoolOnAClosedFloor() {
        int pools = 0;
        for (int t = 0; t < ChasmTiles.PATTERNS.length; t++) {
            for (TilePattern p : ChasmTiles.PATTERNS[t]) {
                if (p.count(BlockKind.WATER) == 0) continue;
                pools++;
                assertEquals(ChasmTiles.S_ROCK, ChasmTiles.TILESET.tile(t).sockets[Wfc3D.NY], "a pool tile has a closed floor: " + ChasmTiles.TILESET.tile(t).name);
                for (int a = 0; a < 6; a++) for (int b = 0; b < 6; b++) for (int c = 0; c < 6; c++) {
                    if (p.get(a, b, c) != BlockKind.WATER) continue;
                    assertEquals(0, b, "water lies in the floor layer");
                    int[][] side = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                    for (int[] s : side) {
                        int na = a + s[0], nc = c + s[1];
                        if (na < 0 || na > 5 || nc < 0 || nc > 5) continue;
                        BlockKind k = p.get(na, b, nc);
                        assertFalse(k.passable, "the pool at " + a + "," + c + " has no open side at " + na + "," + nc + ": " + k);
                    }
                    assertTrue(p.get(a, 1, c) == BlockKind.AIR, "air above the water");
                }
            }
        }
        assertTrue(pools >= 3, "some caves do have pools: " + pools);
    }

    @Test
    public void everyCaveShapeIsReachableThroughItsOwnOpenFaces() {
        // a tile that says a face is open really has a hole there, and a tile that says it is closed has none
        for (int t = 0; t < ChasmTiles.TILESET.size(); t++) {
            for (int d = 0; d < 6; d++) {
                boolean open = ChasmTiles.TILESET.tile(t).sockets[d] == ChasmTiles.S_OPEN;
                boolean[] face = ChasmTiles.PATTERNS[t][0].openFace(d);
                int holes = 0;
                for (boolean b : face) if (b) holes++;
                if (open) assertEquals(12, holes, "an open face of " + ChasmTiles.TILESET.tile(t).name + " has the round 12 voxel hole");
                else assertEquals(0, holes, "a closed face of " + ChasmTiles.TILESET.tile(t).name + " is solid");
            }
        }
    }

    @Test
    public void theSpireProblemNeverOffersADoorwayOnACellThatWasNotPreCollapsed() {
        for (boolean ground : new boolean[] {true, false}) {
            Wfc3D.Problem p = SpireTiles.problem(24, ground);
            assertEquals(Wfc3D.FIXED, p.stateAt(0, 0, 0));
            assertEquals(ground ? SpireTiles.PASSAGE : SpireTiles.HALL_DOOR, p.fixedTileAt(0, 0, 0));
            for (int x = 0; x < 24; x++) for (int y = 0; y < 2; y++) {
                if (x == 0 && y == 0) continue;
                assertEquals(Wfc3D.FREE, p.stateAt(x, y, 0));
                assertEquals(0L, p.allowedAt(x, y, 0) & SpireTiles.FIXED_ONLY, "no doorway tile may be chosen freely at " + x + "," + y);
            }
        }
    }

    @Test
    public void resamplingKeepsTheFirstAndLastLayerOneVoxelThickAtEveryCellSize() {
        for (int s = 3; s <= 12; s++) {
            assertEquals(0, TilePattern.map(0, s));
            assertEquals(TilePattern.N - 1, TilePattern.map(s - 1, s));
            int prev = 0;
            for (int i = 1; i < s - 1; i++) {
                int m = TilePattern.map(i, s);
                assertTrue(m >= 1 && m <= TilePattern.N - 2, "the middle of a cell of " + s + " maps to the middle layers: " + i + " -> " + m);
                assertTrue(m >= prev, "monotone");
                prev = m;
            }
        }
        for (int i = 0; i < 6; i++) assertEquals(i, TilePattern.map(i, 6), "at the authored size nothing moves");
        // the floor row of the doorway tile is a single row at every cell size, so the sill is never raised
        for (int cell = 4; cell <= 8; cell++) {
            int solidRows = 0;
            for (int b = 0; b < cell; b++) if (!SpireTiles.PATTERNS[SpireTiles.PASSAGE].sample(cell / 2, b, 0, cell, cell, SpireModel.DEPTH).passable) solidRows++;
            assertEquals(2, solidRows, "the doorway layer of the passage has the floor and the lintel, one row each, at cell size " + cell);
        }
    }

    @Test
    public void resamplingStretchesAPatternWithoutLosingItsOpenings() {
        // a 4 x 4 doorway authored at 6 must survive every legal cell size (4..8): the middle voxels of the layer stay passable
        TilePattern door = SpireTiles.PATTERNS[SpireTiles.PASSAGE];
        for (int cell = 4; cell <= 8; cell++) {
            int open = 0;
            for (int a = 0; a < cell; a++) for (int b = 0; b < cell; b++) if (door.sample(a, b, 0, cell, cell, SpireModel.DEPTH).passable) open++;
            assertTrue(open >= 4, "a doorway still exists at cell size " + cell + ": " + open + " open voxels");
        }
    }
}
