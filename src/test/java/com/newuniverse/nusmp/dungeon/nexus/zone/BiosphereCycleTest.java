package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.nexus.zone.BiosphereCycle.Cover;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The slow cycle of the Golden City's garden: what belongs in a place at a time, and that it never touches a path, a pool, a tree or the city. */
class BiosphereCycleTest {
    private static final long SEED = 0x1234567L;
    private static final byte[] LIVING = {GoldenGrid.FLOWER, GoldenGrid.GRASS, GoldenGrid.FERN, GoldenGrid.MOSS};

    @Test
    void theCycleOnlyWritesOnTheGardensOwnGround() {
        for (byte k : LIVING) assertTrue(BiosphereCycle.mayWrite(k), "kind " + k);
        for (byte k : new byte[]{GoldenGrid.NONE, GoldenGrid.PATH, GoldenGrid.POOL, GoldenGrid.SHORE, GoldenGrid.FIXED, GoldenGrid.GROVE, GoldenGrid.CITY, GoldenGrid.LICHEN}) {
            assertFalse(BiosphereCycle.mayWrite(k), "never on kind " + k);
        }
    }

    @Test
    void whateverTheCyclePutsDownItMayAlsoTakeUp() {
        for (byte k : LIVING) {
            for (int i = 0; i < 400; i++) {
                int u = i * 7 % 101 - 50, v = i * 13 % 97 - 48;
                for (long t = 0; t < 9 * 3600 * 20L; t += 5000) {
                    Cover c = BiosphereCycle.cover(SEED, k, u, v, t);
                    assertTrue(BiosphereCycle.FLORA.contains(c.block()), "the glue could not replace " + c.block() + " later");
                }
            }
        }
        assertTrue(BiosphereCycle.cover(SEED, GoldenGrid.LICHEN, 1, 1, 0).block().equals("minecraft:glow_lichen"));
        assertTrue(BiosphereCycle.cover(SEED, GoldenGrid.PATH, 1, 1, 0).air());
        assertTrue(BiosphereCycle.cover(SEED, GoldenGrid.POOL, 1, 1, 0).air());
    }

    @Test
    void flowersBloomAndWiltAndTheCycleComesRound() {
        for (int i = 0; i < 40; i++) {
            int u = i * 5 - 90, v = i * 3 - 60;
            Set<String> seen = new HashSet<>();
            for (int s = 0; s < BiosphereCycle.STAGES; s++) seen.add(BiosphereCycle.cover(SEED, GoldenGrid.FLOWER, u, v, s * BiosphereCycle.STAGE_TICKS).block());
            assertTrue(seen.contains("minecraft:air"), "bare for a stage");
            assertTrue(seen.contains("minecraft:short_grass"), "sprouts and wilts into grass");
            assertTrue(seen.size() >= 3, "and blooms: " + seen);
            for (long t = 0; t < 4 * BiosphereCycle.STAGE_TICKS; t += 300) {
                assertEquals(BiosphereCycle.cover(SEED, GoldenGrid.FLOWER, u, v, t), BiosphereCycle.cover(SEED, GoldenGrid.FLOWER, u, v, t + BiosphereCycle.STAGES * BiosphereCycle.STAGE_TICKS), "a full cycle comes back");
            }
        }
    }

    @Test
    void grassIsGrazedBareAndFernsComeAndGo() {
        for (byte k : new byte[]{GoldenGrid.GRASS, GoldenGrid.FERN}) {
            int bare = 0, plants = 0;
            for (int s = 0; s < BiosphereCycle.STAGES; s++) {
                for (int i = 0; i < 60; i++) {
                    Cover c = BiosphereCycle.cover(SEED, k, i * 3, -i * 2, s * BiosphereCycle.STAGE_TICKS);
                    if (c.air()) bare++;
                    else plants++;
                }
            }
            assertTrue(bare > 0 && plants > bare * 3, "kind " + k + ": most of the time there are plants, but some places are bare: " + bare + " bare, " + plants + " green");
        }
    }

    @Test
    void neighboursAreOutOfStepSoTheGardenShiftsInWaves() {
        Set<Integer> stages = new HashSet<>();
        for (int u = 0; u < 12; u++) for (int v = 0; v < 12; v++) stages.add(BiosphereCycle.stage(SEED, u, v, 100_000L));
        assertEquals(BiosphereCycle.STAGES, stages.size(), "every stage is somewhere at once");
    }

    @Test
    void mossSpreadsThenRecedes() {
        double lo = 1, hi = 0;
        for (long t = 0; t < BiosphereCycle.MOSS_TICKS; t += BiosphereCycle.MOSS_TICKS / 16) {
            int on = 0, n = 0;
            for (int u = -40; u <= 40; u += 2) for (int v = -40; v <= 40; v += 2) {
                n++;
                if (BiosphereCycle.mossAt(SEED, u, v, t)) on++;
            }
            double cover = on / (double) n;
            lo = Math.min(lo, cover);
            hi = Math.max(hi, cover);
        }
        assertTrue(hi - lo > 0.2, "moss spreads and recedes: " + lo + " .. " + hi);
        assertTrue(lo > 0.05 && hi < 0.95, "but never takes everything or leaves nothing: " + lo + " .. " + hi);
    }

    @Test
    void vinesGrowDownAndAreDrawnUpWithinTheirLength() {
        for (int max = 2; max <= 11; max++) {
            int lo = Integer.MAX_VALUE, hi = 0;
            for (long t = 0; t < BiosphereCycle.VINE_TICKS * 8; t += 200) {
                int len = BiosphereCycle.vineLength(SEED, 3, -5, max, t);
                assertTrue(len >= Math.min(2, max) && len <= max, "length " + len + " of " + max);
                lo = Math.min(lo, len);
                hi = Math.max(hi, len);
            }
            if (max > 3) assertTrue(hi - lo >= (max - 2) - 1, "it grows and is drawn up: " + lo + " .. " + hi + " of " + max);
        }
        assertEquals(0, BiosphereCycle.vineLength(SEED, 0, 0, 0, 100));
        assertEquals(1, BiosphereCycle.vineLength(SEED, 0, 0, 1, 100));
    }

    @Test
    void berriesAreRipeMostOfTheTime() {
        int ripe = 0, n = 0;
        for (int i = 0; i < 100; i++) {
            for (long t = 0; t < BiosphereCycle.VINE_TICKS * 5; t += BiosphereCycle.VINE_TICKS) {
                n++;
                if (BiosphereCycle.ripe(SEED, i, -i, t)) ripe++;
            }
        }
        assertEquals(0.8, ripe / (double) n, 0.02);
    }

    @Test
    void theShoresRiseAndFall() {
        double lo = 1, hi = 0;
        for (long t = 0; t < BiosphereCycle.TIDE_TICKS; t += BiosphereCycle.TIDE_TICKS / 24) {
            int wet = 0, n = 0;
            for (int u = -40; u <= 40; u++) for (int v = -3; v <= 3; v++) {
                n++;
                if (BiosphereCycle.shoreWet(SEED, u, v, t)) wet++;
            }
            lo = Math.min(lo, wet / (double) n);
            hi = Math.max(hi, wet / (double) n);
        }
        assertTrue(hi - lo > 0.3, "the water rises and falls: " + lo + " .. " + hi);
    }

    @Test
    void groveStagesRunBareSaplingBushTreeAndAreAllDistinguishable() {
        Set<String> markers = new HashSet<>();
        for (int s = 0; s < BiosphereCycle.GROVE_STAGES; s++) markers.add(GoldenFlora.groveMarker(s));
        assertEquals(BiosphereCycle.GROVE_STAGES, markers.size(), "the marker cell tells the stages apart");
        int[] count = new int[4];
        for (long t = 0; t < BiosphereCycle.GROVE_TICKS * 6; t += BiosphereCycle.GROVE_TICKS) count[BiosphereCycle.groveStage(2, t)]++;
        assertEquals(1, count[0]);
        assertEquals(1, count[1]);
        assertEquals(1, count[2]);
        assertEquals(3, count[3], "a small tree stands for half the cycle, then it is pruned");
        for (int s = 0; s < 4; s++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dy = GoldenFlora.GROVE_LOW; dy <= GoldenFlora.GROVE_HIGH; dy++) {
                        String[] c = GoldenFlora.groveCell(s, dx, dy, dz);
                        if (c != null) assertTrue(BiosphereCycle.GROVE_BLOCKS.contains(c[0]), "the glue may replace " + c[0]);
                    }
                }
            }
            assertNull(GoldenFlora.groveCell(s, 2, 1, 0), "nothing outside the 3 x 3");
            assertNull(GoldenFlora.groveCell(s, 0, 6, 0), "nor above the top");
        }
        assertNull(GoldenFlora.groveCell(0, 0, 1, 0), "a bare grove has nothing");
        assertEquals(GoldenFlora.GROVE_HIGH, 5);
    }

    @Test
    void aPassIsBoundedAndAGroveCountsInFull() {
        assertEquals(GoldenFlora.GROVE_HIGH - GoldenFlora.GROVE_LOW + 1, 5);
        assertEquals(3 * 3 * (GoldenFlora.GROVE_HIGH - GoldenFlora.GROVE_LOW + 1), BiosphereCycle.GROVE_CELLS, "the whole of a grove");
        assertTrue(BiosphereCycle.MAX_WRITES >= BiosphereCycle.GROVE_CELLS, "a grove always fits in a pass");
        assertTrue(BiosphereCycle.mayRebuildGrove(0));
        assertTrue(BiosphereCycle.mayRebuildGrove(BiosphereCycle.MAX_WRITES - BiosphereCycle.GROVE_CELLS));
        assertFalse(BiosphereCycle.mayRebuildGrove(BiosphereCycle.MAX_WRITES - BiosphereCycle.GROVE_CELLS + 1), "never over the budget");
        assertFalse(BiosphereCycle.mayRebuildGrove(BiosphereCycle.MAX_WRITES));
        long perMinute = 60L * 20 / BiosphereCycle.PASS_TICKS * BiosphereCycle.MAX_WRITES * BiosphereCycle.MAX_PLAYERS;
        assertTrue(perMinute <= 20_000, "the worst case is a trickle of blocks, not a flood: " + perMinute + " a minute for the busiest garden");
        assertTrue(BiosphereCycle.SAMPLES * BiosphereCycle.MAX_PLAYERS <= 200, "and a handful of reads");
    }

    @Test
    void theLookIsBoundedAndSpreadAndTheSameEveryTime() {
        Set<String> seen = new HashSet<>();
        for (int k = 0; k < 40; k++) {
            int[] a = BiosphereCycle.sample(99L, 12L, k, 26), b = BiosphereCycle.sample(99L, 12L, k, 26);
            assertArrayEquals(a, b, "replayable");
            assertTrue(a[0] * a[0] + a[1] * a[1] <= 26 * 26, "within the radius");
            seen.add(a[0] + "," + a[1]);
        }
        assertTrue(seen.size() >= 30, "spread: " + seen.size());
        Set<String> next = new HashSet<>();
        for (int k = 0; k < 40; k++) {
            int[] a = BiosphereCycle.sample(99L, 13L, k, 26);
            next.add(a[0] + "," + a[1]);
        }
        assertNotEquals(seen, next, "the next pass looks elsewhere");
    }

    // ---------------------------------------------------------------- the cycle on a built garden
    /** What the glue may replace in a cover cell, as the raster names it. */
    private static boolean replaceable(String block) { return BiosphereCycle.FLORA.contains(block); }

    @Test
    void anEverRunningCycleOnABuiltGardenOnlyTouchesItsOwnPlantsAndNeverClosesAPath() {
        for (int[] c : new int[][]{{0, 0}, {-3, 5}}) {
            GoldenRaster r = GoldenRaster.built(c[0], c[1]);
            GoldenGrid g = GoldenGrid.of(c[0], c[1]);
            int f = g.floorY;
            for (int u = -GoldenGrid.REACH; u <= GoldenGrid.REACH; u++) {
                for (int v = -GoldenGrid.REACH; v <= GoldenGrid.REACH; v++) {
                    if (!g.g(u, v) || !BiosphereCycle.mayWrite(g.coverAt(u, v))) continue;
                    assertEquals(BiosphereCycle.cover(g.seed, g.coverAt(u, v), u, v, 0L).block(), r.at(g.cx + u, f + g.groundAt(u, v) + 1, g.cz + v), "the plan builds the garden as it is at time zero");
                }
            }
            String[][][] before = new String[GoldenGrid.N][GoldenGrid.N][];
            for (long t = 500; t < 6 * 3600 * 20L; t += 2500) {
                for (int u = -GoldenGrid.REACH; u <= GoldenGrid.REACH; u++) {
                    for (int v = -GoldenGrid.REACH; v <= GoldenGrid.REACH; v++) {
                        if (!g.g(u, v)) continue;
                        byte k = g.coverAt(u, v);
                        if (!BiosphereCycle.mayWrite(k)) continue;
                        int y = f + g.groundAt(u, v) + 1;
                        String cur = r.at(g.cx + u, y, g.cz + v);
                        assertTrue(replaceable(cur), "only plants stand in a cover cell: " + cur);
                        r.set(g.cx + u, y, g.cz + v, BiosphereCycle.cover(g.seed, k, u, v, t).block());
                    }
                }
            }
            for (int u = -GoldenGrid.REACH; u <= GoldenGrid.REACH; u++) {
                for (int v = -GoldenGrid.REACH; v <= GoldenGrid.REACH; v++) {
                    if (!g.g(u, v) || g.coverAt(u, v) != GoldenGrid.PATH) continue;
                    for (int y = f + 1; y <= f + 3; y++) {
                        String cell = r.at(g.cx + u, y, g.cz + v);
                        assertTrue(cell.equals("minecraft:air") || cell.equals("minecraft:vine"), "a path is still open (ivy is passable): " + cell);
                    }
                }
            }
        }
    }

    @Test
    void theGrovesGrowAndArePrunedWithoutLeavingTheirNineBlocksOrBlockingAPath() {
        for (int[] c : new int[][]{{20, 31}, {-12, -9}, {3, 2}}) {
            GoldenRaster r = GoldenRaster.built(c[0], c[1]);
            GoldenGrid g = GoldenGrid.of(c[0], c[1]);
            for (GoldenGrid.Grove gr : g.groves) {
                int base = g.floorY + g.groundAt(gr.u(), gr.v());
                for (long t = 0; t < BiosphereCycle.GROVE_TICKS * 12; t += BiosphereCycle.GROVE_TICKS) {
                    int stage = BiosphereCycle.groveStage(gr.phase(), t);
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            for (int dy = GoldenFlora.GROVE_LOW; dy <= GoldenFlora.GROVE_HIGH; dy++) {
                                String cur = r.at(g.cx + gr.u() + dx, base + dy, g.cz + gr.v() + dz);
                                assertTrue(BiosphereCycle.GROVE_BLOCKS.contains(cur), "only the grove's own blocks stand in it: " + cur);
                                String[] want = GoldenFlora.groveCell(stage, dx, dy, dz);
                                r.set(g.cx + gr.u() + dx, base + dy, g.cz + gr.v() + dz, want == null ? "minecraft:air" : want[0]);
                            }
                        }
                    }
                    assertEquals(GoldenFlora.groveMarker(stage), r.at(g.cx + gr.u(), base + 1, g.cz + gr.v()));
                }
                for (int du = -2; du <= 2; du++) {
                    for (int dv = -2; dv <= 2; dv++) {
                        assertNotEquals(GoldenGrid.PATH, g.coverAt(gr.u() + du, gr.v() + dv), "a grove is never next to a path");
                    }
                }
            }
        }
    }
}
