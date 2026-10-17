package com.newuniverse.nusmp.arc.hub;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.arc.arena.BlockSpec;
import com.newuniverse.nusmp.labyrinth.mega.HubLayout;
import com.newuniverse.nusmp.labyrinth.mega.hub.HubPlazaLayout;
import com.newuniverse.nusmp.labyrinth.mega.hub.HubProtect;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the Promenade and the Chronicle are drawn from a formula that three other things depend on (the builder that lays them, the guard that keeps them whole, the
 * pass that stands characters on the posts), so the formula is checked as hard as the arenas are: every block of it exists in the game, nothing is built on the
 * plaza's protected box or on the two roads, the whole site is on the flat disc of the Hub's terrain, every post has paving under it and air over it and faces the
 * plaza, the lectern can be walked up to (a walk from the plaza to it, found by a search over the columns) and the guard holds every block the builder lays.
 */
class ChronicleSiteTest {
    private static final Path VANILLA = Path.of("tools/labyrinth/vanilla_block_states_1_21_1.json");
    private static final int R = ChronicleSite.MAX_RADIUS;

    @Test
    void everyPartHasAVanillaBlockWithRealProperties() throws IOException {
        JsonObject vanilla = JsonParser.parseString(Files.readString(VANILLA)).getAsJsonObject();
        Map<SitePart, String> palette = ChronicleSite.palette();
        for (SitePart p : SitePart.values()) assertTrue(palette.containsKey(p), "no block for " + p);
        for (Map.Entry<SitePart, String> e : palette.entrySet()) {
            BlockSpec spec = BlockSpec.parse(e.getValue());
            assertEquals("minecraft", spec.namespace(), e.getKey() + ": only vanilla blocks");
            assertTrue(vanilla.has(spec.path()), e.getKey() + ": no such block " + spec.path());
            JsonObject props = vanilla.getAsJsonObject(spec.path());
            for (Map.Entry<String, String> prop : spec.properties().entrySet()) {
                assertTrue(props.has(prop.getKey()), spec.path() + " has no property " + prop.getKey());
                JsonArray values = props.getAsJsonArray(prop.getKey());
                boolean found = false;
                for (JsonElement v : values) found |= v.getAsString().equals(prop.getValue());
                assertTrue(found, spec.path() + "[" + prop.getKey() + "] has no value " + prop.getValue());
            }
        }
    }

    @Test
    void everyPartThatIsUsedIsUsedAtLeastOnceAndTheSolidnessMatchesTheBlock() {
        Set<SitePart> used = EnumSet.noneOf(SitePart.class);
        for (int x = -R; x <= R; x++) for (int z = -R; z <= R; z++) for (ChronicleSite.Cell c : ChronicleSite.column(x, z)) used.add(c.part());
        for (SitePart p : SitePart.values()) assertTrue(used.contains(p), p + " is never used");
        for (Map.Entry<SitePart, String> e : ChronicleSite.palette().entrySet()) {
            boolean light = e.getValue().startsWith("lantern");
            assertEquals(!light, e.getKey().solid(), e.getKey() + " solid flag vs " + e.getValue());
        }
    }

    @Test
    void noColumnIsBuiltOnThePlazaBoxOrTheRoadsOrOutsideTheFlatDisc() {
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                if (ChronicleSite.column(x, z).isEmpty()) continue;
                double r = Math.sqrt((double) x * x + (double) z * z);
                assertFalse(HubProtect.inPlazaBox(x, ChronicleSite.PAVE_Y, z), "column " + x + "," + z + " is on the plaza's protected box");
                assertFalse(ChronicleSite.onRoad(x, z) && !ChronicleSite.inRotunda(x, z), "column " + x + "," + z + " is on a road");
                assertTrue(r <= HubLayout.PLAZA_RADIUS - 6, "column " + x + "," + z + " is " + r + " from the centre: too near the edge of the flat disc");
            }
        }
        assertTrue(ChronicleSite.ROT_Z - ChronicleSite.ROT_R > HubPlazaLayout.maxZ(), "the rotunda clears the plaza box");
        assertTrue(ChronicleSite.RING_INNER > Math.sqrt(2.0) * HubPlazaLayout.SIZE_X / 2.0, "the ring clears even the corner of the plaza box");
    }

    @Test
    void theRoadsAreLeftUntouchedEvenWherePostsAreNearThem() {
        for (int x = -R; x <= R; x++) {
            for (int z = -ChronicleSite.ROAD_HALF_WIDTH; z <= ChronicleSite.ROAD_HALF_WIDTH; z++) {
                if (Math.abs(x) < ChronicleSite.ROAD_FROM_X) continue;
                assertTrue(ChronicleSite.column(x, z).isEmpty(), "the road at " + x + "," + z + " is built on");
                assertFalse(ChronicleSite.protectedCell(x, ChronicleSite.PAVE_Y + 1, z), "the guard holds the road at " + x + "," + z);
            }
        }
    }

    // ---------------------------------------------------------------- the posts
    @Test
    void thereAreEnoughPostsAndTheyAreNumberedInOrderAndNeverMove() {
        List<ChronicleSite.Post> posts = ChronicleSite.posts();
        assertTrue(posts.size() >= 40, "only " + posts.size() + " posts");
        assertTrue(posts.size() <= 80, "too many posts: " + posts.size());
        for (int i = 0; i < posts.size(); i++) assertEquals(i, posts.get(i).index());
        assertSame(posts, ChronicleSite.posts());
        boolean refused = false;
        try {
            ChronicleSite.posts().add(null);
        } catch (UnsupportedOperationException expected) {
            refused = true;
        }
        assertTrue(refused, "the list of posts must not be changeable");
        assertNull(ChronicleSite.post(-1));
        assertNull(ChronicleSite.post(posts.size()));
        assertEquals(posts.get(3), ChronicleSite.post(3));
    }

    @Test
    void everyPostHasPavingUnderItAirOverItAndRoomAroundIt() {
        Set<Long> seen = new HashSet<>();
        List<ChronicleSite.Post> posts = ChronicleSite.posts();
        for (ChronicleSite.Post p : posts) {
            assertTrue(seen.add(((long) p.x() << 32) ^ (p.z() & 0xFFFFFFFFL)), "two posts on " + p.x() + "," + p.z());
            assertEquals(ChronicleSite.PAVE_Y + 1, p.y());
            List<ChronicleSite.Cell> cells = ChronicleSite.column(p.x(), p.z());
            assertEquals(1, cells.size(), "post " + p.index() + " has something over the paving");
            assertEquals(0, cells.get(0).dy());
            assertEquals(SitePart.POST_TILE, cells.get(0).part());
            assertTrue(ChronicleSite.inRing(p.x(), p.z()), "post " + p.index() + " is off the band");
            assertFalse(ChronicleSite.protectedCell(p.x(), p.y() - 5, p.z()), "the guard reaches too deep under the post");
            assertTrue(ChronicleSite.protectedCell(p.x(), p.y() - 2, p.z()), "the guard leaves the paving undermined");
            assertTrue(ChronicleSite.protectedCell(p.x(), p.y(), p.z()), "the guard misses the post " + p.index());
            // a character who stands here must be able to turn round: at least two of the four side neighbours are free at head height
            int free = 0;
            for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) if (!blocked(p.x() + d[0], p.z() + d[1])) free++;
            assertTrue(free >= 2, "post " + p.index() + " is walled in");
        }
        for (int i = 0; i < posts.size(); i++) {
            for (int j = i + 1; j < posts.size(); j++) {
                ChronicleSite.Post a = posts.get(i), b = posts.get(j);
                double d = Math.sqrt((double) (a.x() - b.x()) * (a.x() - b.x()) + (double) (a.z() - b.z()) * (a.z() - b.z()));
                assertTrue(d >= 2.9, "posts " + i + " and " + j + " are only " + d + " apart");
            }
        }
    }

    @Test
    void thePostsAreOrderedFromTheChronicleOutwardsAroundTheRing() {
        List<ChronicleSite.Post> posts = ChronicleSite.posts();
        double prev = -1;
        for (ChronicleSite.Post p : posts) {
            double deg = Math.toDegrees(Math.atan2(p.z(), p.x()));
            double d = Math.abs(deg - 90.0);
            if (d > 180.0) d = 360.0 - d;
            assertTrue(d >= prev - 1e-9, "post " + p.index() + " is nearer the Chronicle than the one before it");
            prev = d;
        }
        // the first posts stand close to the rotunda, the last ones on the far side of the ring
        ChronicleSite.Post first = posts.get(0), last = posts.get(posts.size() - 1);
        assertTrue(Math.hypot(first.x() - ChronicleSite.ROT_X, first.z() - ChronicleSite.ROT_Z) < Math.hypot(last.x() - ChronicleSite.ROT_X, last.z() - ChronicleSite.ROT_Z));
        assertTrue(Math.hypot(first.x() - ChronicleSite.ROT_X, first.z() - ChronicleSite.ROT_Z) >= 9.0, "the first post is at least nine blocks from the rotunda's centre");
    }

    @Test
    void everyPostFacesThePlazaWithinAHandfulOfDegrees() {
        for (ChronicleSite.Post p : ChronicleSite.posts()) {
            // yaw 0 faces south (+z), 90 west (-x), 180 north, 270 east: the facing vector is (-sin yaw, cos yaw)
            double fx = -Math.sin(Math.toRadians(p.yaw())), fz = Math.cos(Math.toRadians(p.yaw()));
            double tx = -p.x(), tz = -p.z(), len = Math.hypot(tx, tz);
            double dot = (fx * tx + fz * tz) / len;
            assertTrue(dot > 0.999, "post " + p.index() + " does not face the plaza (dot " + dot + ")");
        }
    }

    // ---------------------------------------------------------------- the columns
    @Test
    void everyColumnIsOrderedFromThePavingUpAndHasAPavingCell() {
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                List<ChronicleSite.Cell> cells = ChronicleSite.column(x, z);
                if (cells.isEmpty()) continue;
                assertEquals(0, cells.get(0).dy(), "column " + x + "," + z + " does not start on the paving");
                int prev = -1;
                for (ChronicleSite.Cell c : cells) {
                    assertTrue(c.dy() > prev, "column " + x + "," + z + " is out of order or doubled at " + c.dy());
                    prev = c.dy();
                    assertTrue(c.dy() <= ChronicleSite.OBELISK_TOP_DY, "column " + x + "," + z + " is too tall");
                }
                // a light stands on something solid, and a lantern is never the lowest block
                for (int i = 1; i < cells.size(); i++) {
                    if (cells.get(i).part() == SitePart.LAMP || cells.get(i).part() == SitePart.COLUMN_LAMP) {
                        ChronicleSite.Cell below = cells.get(i - 1);
                        assertEquals(cells.get(i).dy() - 1, below.dy(), "a lantern floats at " + x + "," + z);
                        assertTrue(below.part().solid(), "a lantern has no support at " + x + "," + z);
                    }
                }
            }
        }
    }

    @Test
    void theLecternStandsAtTheCentreOfTheRotundaFacingThePlaza() {
        List<ChronicleSite.Cell> cells = ChronicleSite.column(ChronicleSite.LECTERN_X, ChronicleSite.LECTERN_Z);
        assertEquals(2, cells.size());
        assertEquals(SitePart.LECTERN, cells.get(1).part());
        assertEquals(1, cells.get(1).dy());
        assertTrue(ChronicleSite.palette().get(SitePart.LECTERN).contains("facing=north"), "the plaza lies to the north of the rotunda");
        assertTrue(ChronicleSite.LECTERN_Z > 0, "the rotunda is south of the plaza");
        assertTrue(ChronicleSite.protectedCell(ChronicleSite.LECTERN_X, ChronicleSite.PAVE_Y + 1, ChronicleSite.LECTERN_Z));
    }

    @Test
    void theObeliskIsTallEnoughToBeSeenOverThePavilionAndStandsBehindTheLectern() {
        List<ChronicleSite.Cell> shaft = ChronicleSite.column(0, ChronicleSite.ROT_Z + 3);
        assertEquals(ChronicleSite.OBELISK_TOP_DY, shaft.get(shaft.size() - 1).dy());
        assertEquals(SitePart.OBELISK_TOP, shaft.get(shaft.size() - 1).part());
        assertEquals(SitePart.OBELISK_LAMP, shaft.get(shaft.size() - 2).part());
        assertTrue(ChronicleSite.OBELISK_TOP_DY >= 12);
        for (int i = 1; i < shaft.size(); i++) assertEquals(shaft.get(i - 1).dy() + 1, shaft.get(i).dy(), "the shaft has a gap at " + shaft.get(i).dy());
    }

    /**
     * A walk by a search over the columns: the walker steps from one column to a side neighbour when the two cells it needs (feet and head) are not solid, and
     * the ground is level everywhere (the Hub is flat), so a column is blocked exactly when it has a solid cell at dy 1 or dy 2.
     */
    private static boolean blocked(int x, int z) {
        for (ChronicleSite.Cell c : ChronicleSite.column(x, z)) if (c.dy() >= 1 && c.dy() <= 2 && c.part().solid()) return true;
        return false;
    }

    @Test
    void theLecternCanBeWalkedUpToFromThePlazaAndEveryFloorCellOfTheRotundaCanBeReached() {
        Set<Long> seen = new HashSet<>();
        ArrayDeque<int[]> todo = new ArrayDeque<>();
        int sx = 0, sz = 17;                                   // just outside the plaza's box, on the way to the Chronicle
        todo.add(new int[] {sx, sz});
        seen.add(((long) sx << 32) ^ (sz & 0xFFFFFFFFL));
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!todo.isEmpty()) {
            int[] c = todo.poll();
            for (int[] d : dirs) {
                int nx = c[0] + d[0], nz = c[1] + d[1];
                if (Math.abs(nx) > R || Math.abs(nz) > R || blocked(nx, nz)) continue;
                if (seen.add(((long) nx << 32) ^ (nz & 0xFFFFFFFFL))) todo.add(new int[] {nx, nz});
            }
        }
        // the cell in front of the lectern (north of it) is reached, as is every free cell of the rotunda floor
        assertTrue(seen.contains(((long) 0 << 32) ^ ((ChronicleSite.LECTERN_Z - 1) & 0xFFFFFFFFL)), "nobody can stand in front of the lectern");
        for (int x = ChronicleSite.ROT_X - 6; x <= ChronicleSite.ROT_X + 6; x++) {
            for (int z = ChronicleSite.ROT_Z - 6; z <= ChronicleSite.ROT_Z + 6; z++) {
                if (!ChronicleSite.inRotunda(x, z) || blocked(x, z)) continue;
                assertTrue(seen.contains(((long) x << 32) ^ (z & 0xFFFFFFFFL)), "the rotunda floor at " + x + "," + z + " cannot be reached");
            }
        }
        // every post can be reached too
        for (ChronicleSite.Post p : ChronicleSite.posts()) assertTrue(seen.contains(((long) p.x() << 32) ^ (p.z() & 0xFFFFFFFFL)), "post " + p.index() + " cannot be reached");
    }

    @Test
    void theEntranceToTheRotundaIsAtLeastThreeWideAndHeadHeightIsFree() {
        int open = 0;
        for (int x = -2; x <= 2; x++) if (!blocked(x, ChronicleSite.ROT_Z - 5)) open++;
        assertTrue(open >= 3, "the entrance on the plaza side is only " + open + " wide");
        // and the way from the entrance to the lectern has no beam at head height: dy 7 is the lowest roof cell and the walker needs dy 1 and 2 only
        for (int z = ChronicleSite.ROT_Z - 5; z < ChronicleSite.ROT_Z; z++) assertFalse(blocked(0, z), "the way in is blocked at " + z);
    }

    // ---------------------------------------------------------------- the guard
    @Test
    void theGuardHoldsEveryBlockTheBuilderLays() {
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                for (ChronicleSite.Cell c : ChronicleSite.column(x, z)) {
                    int y = ChronicleSite.PAVE_Y + c.dy();
                    assertTrue(ChronicleSite.protectedCell(x, y, z), "the guard does not hold " + c.part() + " at " + x + "," + y + "," + z);
                }
            }
        }
    }

    @Test
    void theGuardDoesNotReachFarBelowOrAboveOrOutAcrossTheTerrain() {
        assertFalse(ChronicleSite.protectedCell(0, ChronicleSite.PAVE_Y - 10, ChronicleSite.ROT_Z));
        assertFalse(ChronicleSite.protectedCell(0, ChronicleSite.PAVE_Y + 60, ChronicleSite.ROT_Z));
        assertFalse(ChronicleSite.protectedCell(0, ChronicleSite.PAVE_Y + 1, 36));
        assertFalse(ChronicleSite.protectedCell(0, ChronicleSite.PAVE_Y + 1, 10), "the plaza is the plaza guard's, not ours");
        assertFalse(ChronicleSite.protectedCell(35, ChronicleSite.PAVE_Y + 1, 35));
    }

    @Test
    void theBuildAreaContainsTheWholeSite() {
        for (int x = -R - 5; x <= R + 5; x++) {
            for (int z = -R - 5; z <= R + 5; z++) {
                if (!ChronicleSite.column(x, z).isEmpty()) assertTrue(ChronicleSite.inBuildArea(x, z), "column " + x + "," + z + " is outside the build area");
            }
        }
    }

    @Test
    void lampPostsStandOnTheBandBetweenThePostsAndNeverOnOne() {
        List<int[]> lamps = ChronicleSite.lampPosts();
        assertTrue(lamps.size() >= 12, "only " + lamps.size() + " lamp posts");
        List<String> problems = new ArrayList<>();
        for (int[] l : lamps) {
            List<ChronicleSite.Cell> cells = ChronicleSite.column(l[0], l[1]);
            if (cells.size() == 4 && cells.get(3).part() == SitePart.LAMP) continue;
            problems.add(l[0] + "," + l[1] + " -> " + cells);
        }
        // a lamp post that falls on a post tile or off the band is simply not built (the column function decides); it must not be the usual case
        assertTrue(problems.size() <= lamps.size() / 4, "too many lamp posts are lost: " + problems);
    }

    @Test
    void theSiteIsDeterministic() {
        StringBuilder a = new StringBuilder(), b = new StringBuilder();
        for (int x = -R; x <= R; x++) for (int z = -R; z <= R; z++) a.append(ChronicleSite.column(x, z));
        for (int x = -R; x <= R; x++) for (int z = -R; z <= R; z++) b.append(ChronicleSite.column(x, z));
        assertEquals(a.toString(), b.toString());
    }

    @Test
    void theWiderGuardContainsTheGuardedVolumeAndGrowsWithTheMargin() {
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                for (int dy = -3; dy <= ChronicleSite.OBELISK_TOP_DY + 4; dy++) {
                    int y = ChronicleSite.PAVE_Y + dy;
                    boolean g = ChronicleSite.protectedCell(x, y, z), n0 = ChronicleSite.near(x, y, z, 0), n3 = ChronicleSite.near(x, y, z, 3), n8 = ChronicleSite.near(x, y, z, 8);
                    if (g) assertTrue(n0, "near(0) misses a guarded cell at " + x + "," + y + "," + z);
                    if (n0) assertTrue(n3, "a larger margin lost a cell at " + x + "," + y + "," + z);
                    if (n3) assertTrue(n8, "a larger margin lost a cell at " + x + "," + y + "," + z);
                }
            }
        }
        assertFalse(ChronicleSite.near(0, ChronicleSite.PAVE_Y + 1, 0, 8), "the middle of the plaza is not near the site");
        assertFalse(ChronicleSite.near(0, ChronicleSite.PAVE_Y + 80, ChronicleSite.ROT_Z, 8), "high in the sky is not near it");
        assertTrue(ChronicleSite.near(0, ChronicleSite.PAVE_Y + 1, ChronicleSite.ROT_Z + 7 + 5, 8), "a bucket emptied within eight blocks of the rotunda is refused");
    }

    @Test
    void everyColumnTheSiteLaysIsNearTheSiteAndTheCircleTestsAgreeWithTheColumns() {
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                if (ChronicleSite.column(x, z).isEmpty()) continue;
                assertTrue(ChronicleSite.nearSite(x, z, 0), "column " + x + "," + z + " is laid but not near the site");
                assertTrue(ChronicleSite.inRotunda(x, z) ? ChronicleSite.nearSiteRotunda(x, z, 0) : ChronicleSite.nearRing(x, z, 0), "the circle tests disagree at " + x + "," + z);
            }
        }
    }
}
