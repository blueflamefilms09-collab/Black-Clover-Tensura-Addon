package com.newuniverse.nusmp.arc.arena;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcSaga;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: an arena is built from a formula the author cannot see running, so every promise it makes is a property of the formula and is checked by walking it:
 * each column is a clean stack (rising, no gaps, standing on a foundation), every part has a block that really exists in the game's version with real property
 * values, a lantern stands on something that holds it, the boss, the arrival places and the ways home are on ground with air above, and above all the arena is
 * CLOSED: a breadth-first walk over the columns, with the step a player can take (one block up, any way down, two free rows of headroom), from the boss's
 * place must reach every arrival and every way home and never find an edge (a neighbour that is empty), because a hole in the wall is a way out of the fight.
 */
class ArenaShapesTest {
    private static final Path VANILLA = Path.of("tools/labyrinth/vanilla_block_states_1_21_1.json");

    private static List<ArenaShape> shapes() { return ArenaShapes.all(); }

    @Test
    void theRegistryHoldsSixArenasWithDistinctIdsAndTheRingLast() {
        assertEquals(6, shapes().size());
        Set<String> ids = new HashSet<>();
        for (ArenaShape s : shapes()) {
            assertTrue(ids.add(s.id()), "duplicate arena id " + s.id());
            assertTrue(s.id().matches("[a-z][a-z0-9_]*"), s.id());
            assertTrue(!s.title().isEmpty() && s.radius() > 10, s.id());
            assertSame(s, ArenaShapes.byId(s.id()));
            assertSame(s, ArenaShapes.byId(s.id().toUpperCase() + " "));
        }
        assertEquals("stone_ring", shapes().get(5).id());
        assertNull(ArenaShapes.byId("zogratis_arena"), "the Final Hour's arena belongs to the Hourglass, not to this registry");
        assertNull(ArenaShapes.byId(""));
        assertNull(ArenaShapes.byId(null));
    }

    @Test
    void everySagaHasItsOwnArenaAndAnUnknownOneFallsBackToTheRing() {
        Set<String> seen = new LinkedHashSet<>();
        for (ArcSaga saga : ArcSaga.values()) {
            ArcCharacter c = ArcCharacter.boss("probe_" + saga.id(), "Probe").saga(saga).build();
            ArenaShape s = ArenaShapes.forCharacter(c);
            assertNotNull(s, saga.id());
            seen.add(s.id());
        }
        assertEquals(6, seen.size(), "one distinct arena per saga: " + seen);
        assertEquals("midnight_sun_ruins", ArenaShapes.forCharacter(ArcCharacter.boss("a_b", "A").saga(ArcSaga.ELF_REINCARNATION).build()).id());
        assertEquals("spade_throne", ArenaShapes.forCharacter(ArcCharacter.boss("a_b", "A").saga(ArcSaga.SPADE_RAID).build()).id());
        assertEquals("wizard_court", ArenaShapes.forCharacter(ArcCharacter.boss("a_b", "A").saga(ArcSaga.WIZARD_KING).build()).id());
        assertEquals("sunlit_plain", ArenaShapes.forCharacter(ArcCharacter.boss("a_b", "A").saga(ArcSaga.SEVEN_DEADLY_SINS).build()).id());
        assertEquals("orario_pit", ArenaShapes.forCharacter(ArcCharacter.boss("a_b", "A").saga(ArcSaga.DANMACHI).build()).id());
        assertEquals("stone_ring", ArenaShapes.forCharacter(ArcCharacter.boss("a_b", "A").saga(ArcSaga.ALLIES).build()).id());
        assertEquals("stone_ring", ArenaShapes.forCharacter(null).id());
        ArcCharacter named = ArcCharacter.boss("a_b", "A").saga(ArcSaga.ALLIES).arena("orario_pit").build();
        assertEquals("orario_pit", ArenaShapes.forCharacter(named).id(), "a named arena wins over the saga's");
        ArcCharacter typo = ArcCharacter.boss("a_b", "A").saga(ArcSaga.DANMACHI).arena("no_such_arena").build();
        assertEquals("orario_pit", ArenaShapes.forCharacter(typo).id(), "an unknown arena id falls back to the saga's");
        assertTrue(ArenaShapes.usesFinalHour(ArcCharacter.boss("lu_cius", "L").arena("zogratis_arena").build()));
        assertFalse(ArenaShapes.usesFinalHour(named));
    }

    // ---------------------------------------------------------------- the columns
    @Test
    void everyColumnIsACleanStackStandingOnAFoundation() {
        for (ArenaShape s : shapes()) {
            int R = s.radius();
            for (int x = -R; x <= R; x++) {
                for (int z = -R; z <= R; z++) {
                    List<ArenaCell> cells = s.column(x, z);
                    if (cells.isEmpty()) continue;
                    String where = s.id() + " (" + x + "," + z + ")";
                    assertEquals(-2, cells.get(0).dy(), where + " starts at the foundation");
                    assertEquals(ArenaPart.FOUNDATION, cells.get(0).part(), where);
                    assertEquals(-1, cells.get(1).dy(), where);
                    assertEquals(ArenaPart.FOUNDATION, cells.get(1).part(), where);
                    int expect = -2;
                    for (ArenaCell c : cells) {
                        if (c.part() == ArenaPart.CEILING || c.part() == ArenaPart.LEAVES) {                   // a roof and a crown of leaves may float above the stack
                            assertTrue(c.dy() >= expect, where + " the roof or the crown is inside the stack");
                            continue;
                        }
                        assertEquals(expect, c.dy(), where + " has a gap or a repeat in its stack");
                        expect++;
                    }
                    assertTrue(expect - 1 <= 30, where + " is too tall: " + (expect - 1));
                    boolean solid = false;
                    for (ArenaCell c : cells) solid |= c.part().solid() && c.dy() >= 0;
                    assertTrue(solid, where + " has a stack with no ground");
                }
            }
        }
    }

    @Test
    void theBuildStaysUnderSixtyThousandBlocksAndEveryArenaGivesLight() {
        for (ArenaShape s : shapes()) {
            int R = s.radius(), blocks = 0;
            boolean light = false;
            for (int x = -R; x <= R; x++) {
                for (int z = -R; z <= R; z++) {
                    for (ArenaCell c : s.column(x, z)) {
                        blocks++;
                        ArenaPart p = c.part();
                        light |= p == ArenaPart.LAMP_FLOOR || p == ArenaPart.LANTERN || p == ArenaPart.CEILING_LAMP;
                    }
                }
            }
            assertTrue(blocks < 60000, s.id() + " builds " + blocks + " blocks in one go");
            assertTrue(blocks > 3000, s.id() + " is suspiciously small: " + blocks);
            assertTrue(light, s.id() + " has no light at all: it is black at night");
        }
    }

    @Test
    void whatStandsOnWhatIsSupported() {
        for (ArenaShape s : shapes()) {
            int R = s.radius();
            for (int x = -R; x <= R; x++) {
                for (int z = -R; z <= R; z++) {
                    List<ArenaCell> cells = s.column(x, z);
                    for (int i = 1; i < cells.size(); i++) {
                        ArenaPart p = cells.get(i).part(), below = cells.get(i - 1).part();
                        String where = s.id() + " (" + x + "," + z + ") " + p + " on " + below;
                        switch (p) {
                            case LANTERN -> assertFalse(below == ArenaPart.WALL_CAP || below == ArenaPart.CARPET || below.name().startsWith("STEP_") || below == ArenaPart.THRONE
                                    || below == ArenaPart.LANTERN || !below.solid(), where + ": a lantern needs a full block or a post under it");
                            case FLOWER_A, FLOWER_B, TALL_GRASS -> assertEquals(ArenaPart.GRASS, below, where);
                            case CARPET -> assertTrue(below.solid(), where);
                            case LEAVES -> assertTrue(below == ArenaPart.LEAVES || below == ArenaPart.LOG || below == ArenaPart.GRASS || below == ArenaPart.FLOWER_A
                                    || below == ArenaPart.FLOWER_B || below == ArenaPart.TALL_GRASS || cells.get(i).dy() >= 4, where);
                            case SIGIL -> assertEquals(ArenaPart.SIGIL_PAD, below, where);
                            default -> { }
                        }
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- the standing places
    @Test
    void theBossTheArrivalsAndTheWaysHomeStandOnGroundWithAirAbove() {
        for (ArenaShape s : shapes()) {
            int[] b = s.bossSpot();
            assertTrue(s.standable(b[0], b[1]), s.id() + ": the boss has no ground at " + b[0] + "," + b[1]);
            for (int n = 1; n <= ArenaShape.MAX_ARRIVALS; n++) {
                int[][] spots = s.arrivalSpots(n);
                assertEquals(n, spots.length, s.id() + " n=" + n);
                Set<String> seen = new HashSet<>();
                for (int[] a : spots) {
                    assertTrue(s.standable(a[0], a[1]), s.id() + ": nobody can stand at arrival " + a[0] + "," + a[1]);
                    assertTrue(seen.add(a[0] + "," + a[1]), s.id() + ": two travellers on " + a[0] + "," + a[1]);
                }
                for (int i = 0; i < n; i++) assertTrue(spotsPrefix(s, i + 1, spots[i]), s.id() + ": the first travellers move when more come");
            }
            for (int[] g : s.sigils()) {
                List<ArenaCell> cells = s.column(g[0], g[1]);
                ArenaCell top = cells.get(cells.size() - 1);
                if (top.part() == ArenaPart.CEILING) top = cells.get(cells.size() - 2);                      // a cavern's roof is above the answering block
                assertEquals(ArenaPart.SIGIL, top.part(), s.id() + ": the answering block of a way home");
                assertEquals(1, top.dy(), s.id());
                for (int ox = -1; ox <= 1; ox++) for (int oz = -1; oz <= 1; oz++) {
                    if (ox == 0 && oz == 0) continue;
                    List<ArenaCell> pad = s.column(g[0] + ox, g[1] + oz);
                    boolean padPart = false;
                    for (ArenaCell c : pad) if (c.dy() == 0 && c.part() == ArenaPart.SIGIL_PAD) padPart = true;
                    assertTrue(padPart, s.id() + ": the pad round a way home is missing at " + (g[0] + ox) + "," + (g[1] + oz));
                    assertTrue(s.standable(g[0] + ox, g[1] + oz), s.id() + ": the pad is blocked at " + (g[0] + ox) + "," + (g[1] + oz));
                }
            }
        }
    }

    /** The arrival places are a stable prefix: asking for more travellers never moves the ones already placed. */
    private static boolean spotsPrefix(ArenaShape s, int count, int[] expected) {
        int[][] again = s.arrivalSpots(Math.max(count, ArenaShape.MAX_ARRIVALS));
        return again[count - 1][0] == expected[0] && again[count - 1][1] == expected[1];
    }

    @Test
    void theArenaIsClosedAndEveryStandingPlaceCanBeWalkedTo() {
        for (ArenaShape s : shapes()) {
            int R = s.radius();
            int[] start = s.bossSpot();
            Set<Long> seen = new HashSet<>();
            ArrayDeque<int[]> queue = new ArrayDeque<>();
            queue.add(start);
            seen.add(key(start[0], start[1]));
            int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            while (!queue.isEmpty()) {
                int[] a = queue.poll();
                int sa = s.surface(a[0], a[1]);
                for (int[] d : steps) {
                    int bx = a[0] + d[0], bz = a[1] + d[1];
                    List<ArenaCell> cells = s.column(bx, bz);
                    assertFalse(cells.isEmpty(), s.id() + ": a walker can leave the arena at " + bx + "," + bz + " (from " + a[0] + "," + a[1] + ")");
                    int sb = s.surface(bx, bz);
                    if (sb - sa > 1) continue;                                                                    // a wall: cannot be climbed
                    boolean headroom = true;
                    for (ArenaCell c : cells) if (c.dy() > sb && c.dy() <= sb + 2 && c.part().solid()) headroom = false;
                    if (!headroom) continue;
                    if (seen.add(key(bx, bz))) queue.add(new int[] {bx, bz});
                }
            }
            for (int[] a : s.arrivalSpots(ArenaShape.MAX_ARRIVALS)) assertTrue(seen.contains(key(a[0], a[1])), s.id() + ": the boss cannot be reached from arrival " + a[0] + "," + a[1]);
            for (int[] g : s.sigils()) assertTrue(seen.contains(key(g[0] + 1, g[1])), s.id() + ": the way home at " + g[0] + "," + g[1] + " cannot be reached");
            assertTrue(seen.size() > 1000, s.id() + " reaches only " + seen.size() + " columns of " + (2 * R + 1) * (2 * R + 1));
        }
    }

    private static long key(int x, int z) { return ((long) x << 32) ^ (z & 0xFFFFFFFFL); }

    // ---------------------------------------------------------------- the blocks
    @Test
    void everyPartAnArenaUsesHasABlockThatReallyExistsWithRealProperties() throws IOException {
        JsonObject vanilla = JsonParser.parseString(Files.readString(VANILLA)).getAsJsonObject();
        for (ArenaShape s : shapes()) {
            Set<ArenaPart> used = new LinkedHashSet<>();
            int R = s.radius();
            for (int x = -R; x <= R; x++) for (int z = -R; z <= R; z++) for (ArenaCell c : s.column(x, z)) used.add(c.part());
            Map<ArenaPart, String> palette = s.palette();
            for (ArenaPart p : used) assertTrue(palette.containsKey(p), s.id() + " has no block for the part " + p);
            for (Map.Entry<ArenaPart, String> e : palette.entrySet()) {
                BlockSpec spec = BlockSpec.parse(e.getValue());
                assertEquals("minecraft", spec.namespace(), s.id() + " " + e.getKey() + ": only vanilla blocks");
                assertTrue(vanilla.has(spec.path()), s.id() + " " + e.getKey() + ": no such block " + spec.path());
                JsonObject props = vanilla.getAsJsonObject(spec.path());
                for (Map.Entry<String, String> prop : spec.properties().entrySet()) {
                    assertTrue(props.has(prop.getKey()), s.id() + " " + spec.path() + " has no property " + prop.getKey());
                    JsonArray values = props.getAsJsonArray(prop.getKey());
                    boolean found = false;
                    for (JsonElement v : values) found |= v.getAsString().equals(prop.getValue());
                    assertTrue(found, s.id() + " " + spec.path() + "[" + prop.getKey() + "] has no value " + prop.getValue());
                }
            }
        }
    }

    @Test
    void theStairPartsAscendTowardsTheirNamedSide() {
        for (ArenaShape s : shapes()) {
            Map<ArenaPart, String> p = s.palette();
            if (!p.containsKey(ArenaPart.STEP_N)) continue;
            assertTrue(p.get(ArenaPart.STEP_N).contains("facing=north"), s.id());
            assertTrue(p.get(ArenaPart.STEP_E).contains("facing=east"), s.id());
            assertTrue(p.get(ArenaPart.STEP_S).contains("facing=south"), s.id());
            assertTrue(p.get(ArenaPart.STEP_W).contains("facing=west"), s.id());
        }
    }

    @Test
    void aPlaceThatIsLeavesNeverDecaysAndNothingHarmfulIsUsed() {
        List<String> harmful = List.of("lava", "fire", "magma", "cactus", "campfire", "tnt", "cobweb", "powder_snow", "sweet_berry", "wither_rose", "water");
        for (ArenaShape s : shapes()) {
            for (Map.Entry<ArenaPart, String> e : s.palette().entrySet()) {
                if (e.getValue().contains("leaves")) assertTrue(e.getValue().contains("persistent=true"), s.id() + " " + e.getKey() + " would rot away");
                for (String h : harmful) assertFalse(e.getValue().contains(h), s.id() + " " + e.getKey() + " uses " + e.getValue());
            }
        }
    }

    @Test
    void theShapesAreDeterministic() {
        List<String> a = describe(ArenaShapes.SUNLIT_PLAIN), b = describe(new SunlitPlainShape());
        assertEquals(a, b);
        assertEquals(describe(ArenaShapes.MIDNIGHT_SUN_RUINS), describe(new MidnightSunRuinsShape()));
        assertEquals(describe(ArenaShapes.ORARIO_PIT), describe(new OrarioPitShape()));
    }

    private static List<String> describe(ArenaShape s) {
        List<String> out = new ArrayList<>();
        int R = s.radius();
        for (int x = -R; x <= R; x += 3) for (int z = -R; z <= R; z += 3) out.add(x + "," + z + ":" + s.column(x, z));
        return out;
    }

    @Test
    void theOrarioWallIsNeverACleanCircle() {
        double lo = 99, hi = 0;
        for (int a = 0; a < 360; a += 5) {
            int[] p = ArenaKit.at(30.0, a);
            double w = OrarioPitShape.wallFrom(p[0], p[1]);
            lo = Math.min(lo, w);
            hi = Math.max(hi, w);
        }
        assertTrue(hi - lo > 2.0, "the wall's radius varies by " + (hi - lo));
        assertTrue(lo >= 27.0 && hi <= 33.2, "the wall stays between the floor and the build: " + lo + " to " + hi);
    }
}
