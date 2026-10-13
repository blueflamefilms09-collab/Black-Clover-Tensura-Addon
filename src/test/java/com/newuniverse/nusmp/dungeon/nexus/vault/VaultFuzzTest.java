package com.newuniverse.nusmp.dungeon.nexus.vault;

import com.newuniverse.nusmp.dungeon.nexus.vault.VaultLayout.Role;
import com.newuniverse.nusmp.dungeon.nexus.vault.VaultLayout.Spot;
import com.newuniverse.nusmp.dungeon.nexus.vault.VaultOp.Pal;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The planner and the layouts over blobby made-up caves (open pockets, flooded below sea level, deepslate low down): what is found is sound. */
class VaultFuzzTest {
    /** Caves: about one block in five is open, in blobs of 3 blocks; open space below y=32 is water. */
    private static SimWorld caves(long seed) {
        return new SimWorld((x, y, z) -> {
            long h = VaultPlan.mix(Math.floorDiv(x, 3) + seed, Math.floorDiv(y, 3), Math.floorDiv(z, 3), 9);
            boolean open = Math.floorMod(h, 100L) < 20;
            if (!open) return y < 0 ? VaultTerrain.DEEPSLATE : VaultTerrain.STONE;
            return y < 32 ? VaultTerrain.WATER : VaultTerrain.AIR;
        });
    }

    /** Caves with a few long open galleries, so doorways with a dry floor exist. */
    private static SimWorld galleries(long seed) {
        return new SimWorld((x, y, z) -> {
            boolean gallery = Math.floorMod(Math.floorDiv(y, 8) + Math.floorDiv(x, 24) * 3 + seed, 3L) == 0 && Math.floorMod(y, 8) >= 1 && Math.floorMod(y, 8) <= 4 && Math.floorMod(z, 20) < 7;
            if (gallery) return y < 32 ? VaultTerrain.WATER : VaultTerrain.AIR;
            return y < 0 ? VaultTerrain.DEEPSLATE : VaultTerrain.STONE;
        });
    }

    private static void checkBuilt(VaultSite s, VaultLayout l, SimWorld w) {
        // the hollow
        for (int u = 0; u < s.width(); u++) {
            for (int y = 0; y < s.height(); y++) {
                for (int v = 0; v < s.depth(); v++) {
                    int x = s.worldX(u, v), z = s.worldZ(u, v);
                    if (w.palAt(x, s.iy() + y, z) == null) assertEquals(VaultTerrain.AIR, w.at(x, s.iy() + y, z), "hollow at " + u + "," + y + "," + v);
                }
            }
        }
        // sealed: nothing open (air or water) touches the hollow from outside except through the front wall
        for (int u = -1; u <= s.width(); u++) {
            for (int y = -1; y <= s.height(); y++) {
                for (int v = -1; v <= s.depth(); v++) {
                    boolean interior = u >= 0 && u < s.width() && y >= 0 && y < s.height() && v >= 0 && v < s.depth();
                    boolean front = v == -1 && u >= 0 && u < s.width() && y >= 0 && y < s.height();
                    if (interior || front) continue;
                    int c = w.at(s.worldX(u, v), s.iy() + y, s.worldZ(u, v));
                    assertTrue(VaultTerrain.solid(c), "shell closed at " + u + "," + y + "," + v + " (" + c + ")");
                }
            }
        }
        // the way in: the wall cells in front of the doorway, and the doorway itself
        for (int y = 0; y < 2; y++) {
            int x = s.worldX(s.width() / 2, -1), z = s.worldZ(s.width() / 2, -1);
            if (s.hidden()) assertEquals(Pal.ILLUSION, w.palAt(x, s.iy() + y, z), "an illusion in front of the doorway");
            else assertEquals(VaultTerrain.AIR, w.at(x, s.iy() + y, z), "an open doorway");
        }
        Spot door = l.first(Role.DOOR);
        assertTrue(VaultTerrain.open(w.at(door.x(), door.y(), door.z())), "the cave side of the door is open");
    }

    @Test
    void whateverIsFoundInBlobbyCavesIsSoundWhenBuilt() {
        int found = 0, tried = 0;
        Map<VaultKind, Integer> kinds = new EnumMap<>(VaultKind.class);
        for (long seed = 0; seed < 12; seed++) {
            for (int cx = 0; cx < 4; cx++) {
                for (int cz = 0; cz < 4; cz++) {
                    for (VaultKind kind : VaultKind.values()) {
                        int grid = kind == VaultKind.GATE ? VaultPlan.GRID_GATE : VaultPlan.GRID_VAULT;
                        for (int a = 0; a < VaultPlan.ATTEMPTS; a++) {
                            SimWorld w = seed % 2 == 0 ? caves(seed) : galleries(seed);
                            VaultPlan.Result r = VaultPlan.search(grid, kind, cx, cz, a, -44, 108, (x, y, z) -> true, w);
                            tried++;
                            assertNotEquals(VaultPlan.Status.UNKNOWN, r.status(), "a fully loaded world is never unknown");
                            if (r.status() != VaultPlan.Status.FOUND) continue;
                            found++;
                            kinds.merge(kind, 1, Integer::sum);
                            VaultSite s = r.site();
                            assertTrue(s.iy() >= -44 && s.iy() <= 108);
                            VaultLayout l = VaultPlan.layout(s);
                            w.apply(l);
                            checkBuilt(s, l, w);
                        }
                    }
                }
            }
        }
        assertTrue(found > 20, "caves like these do hold rooms: " + found + " of " + tried);
        for (VaultKind k : VaultKind.values()) assertTrue(kinds.getOrDefault(k, 0) > 0, k + " was found at least once");
    }

    @Test
    void theChamberIsSealedInOpenWaterAndInBlobbyCaves() {
        for (long seed = 0; seed < 20; seed++) {
            SimWorld w = seed % 2 == 0 ? new SimWorld((x, y, z) -> VaultTerrain.WATER) : caves(seed);
            VaultPlan.Result r = VaultPlan.chamber(7, 11, -34, 14, (int) seed, (x, y, z) -> true, w);
            assertEquals(VaultPlan.Status.FOUND, r.status());
            VaultLayout l = VaultPlan.chamberLayout(r.site());
            w.apply(l);
            checkBuilt(l.site, l, w);
            Spot g = l.first(Role.GATE), st = l.first(Role.STAND);
            assertEquals(Pal.GATE_UP, w.palAt(g.x(), g.y(), g.z()));
            assertEquals(VaultTerrain.AIR, w.at(st.x(), st.y(), st.z()));
            assertTrue(VaultTerrain.solid(w.at(st.x(), st.y() - 1, st.z())), "the arrival cell has a floor");
        }
    }

    @Test
    void everyFoundSiteKeepsAnAirtightHollowAgainstWater() {
        // flood the whole outside, build, and check that no water cell touches the hollow
        for (int n = 0; n < 6; n++) {
            final long seed = n;
            SimWorld w = new SimWorld((x, y, z) -> Math.floorMod(Math.floorDiv(x, 5) + Math.floorDiv(z, 7) + seed, 3L) == 0 ? VaultTerrain.WATER : VaultTerrain.STONE);
            VaultSite s = new VaultSite(VaultKind.MINIBOSS, 40, 10, 40, 9, 6, 9, (int) seed, true, 3);
            VaultLayout l = VaultPlan.layout(s);
            w.apply(l);
            for (int u = 0; u < 9; u++) for (int y = 0; y < 6; y++) for (int v = 0; v < 9; v++) {
                int x = s.worldX(u, v), z = s.worldZ(u, v);
                for (int[] d : new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}}) {
                    int c = w.at(x + d[0], s.iy() + y + d[1], z + d[2]);
                    assertNotEquals(VaultTerrain.WATER, c, "water next to the hollow");
                }
            }
        }
    }
}
