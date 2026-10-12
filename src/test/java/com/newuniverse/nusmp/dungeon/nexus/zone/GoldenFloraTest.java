package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.city.CityPlan;
import com.newuniverse.nusmp.dungeon.nexus.NexusOp;
import com.newuniverse.nusmp.dungeon.nexus.NexusOps;
import com.newuniverse.nusmp.dungeon.nexus.ZonePlan;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The trees, mushrooms and hanging things of the Golden City's garden. */
class GoldenFloraTest {
    private static final int[][] CELLS = {{0, 0}, {-3, 5}, {20, 31}, {-1, -1}, {3, 2}};

    @Test
    void aCanopyIsAFlattenedDomeThatShrinksToItsEdges() {
        for (int r = 2; r <= 11; r++) {
            for (int ry = 1; ry <= 4; ry++) {
                int mid = GoldenFlora.rho(r, ry, 0);
                assertEquals(r, mid, "full width in the middle");
                int last = mid;
                for (int dy = 1; dy <= ry; dy++) {
                    int up = GoldenFlora.rho(r, ry, dy), down = GoldenFlora.rho(r, ry, -dy);
                    assertEquals(up, down, "symmetric");
                    assertTrue(up <= last, "narrower toward the top");
                    assertTrue(up >= 1 || r < 3, "never closed to a point on a big tree: r=" + r + " ry=" + ry + " dy=" + dy);
                    last = up;
                }
            }
        }
    }

    @Test
    void everyVineHangsFromABlockThatIsThereAndCanHoldIt() {
        Set<String> holds = Set.of("nusmp:golden_leaves", "minecraft:flowering_azalea_leaves", "minecraft:azalea_leaves", "minecraft:ochre_froglight", "minecraft:shroomlight", "minecraft:stripped_oak_log");
        for (int[] c : CELLS) {
            GoldenRaster r = GoldenRaster.built(c[0], c[1]);
            GoldenGrid g = GoldenGrid.of(c[0], c[1]);
            assertFalse(g.vines.isEmpty());
            int golden = 0;
            for (GoldenGrid.Vine v : g.vines) {
                String anchor = r.at(g.cx + v.u(), g.floorY + v.anchor(), g.cz + v.v());
                assertTrue(holds.contains(anchor), "a vine hangs from " + anchor + " at " + v);
                if (anchor.equals("nusmp:golden_leaves")) golden++;
                assertTrue(g.isVineAnchor(v.u(), v.v(), v.anchor()));
            }
            assertTrue(golden >= g.vines.size() * 0.8, "most of them hang from the golden leaves: " + golden + " of " + g.vines.size());
        }
    }

    @Test
    void theTreesStartWithTheirVinesAtTheLengthTheCycleGivesThemAtTimeZero() {
        for (int[] c : CELLS) {
            GoldenRaster r = GoldenRaster.built(c[0], c[1]);
            GoldenGrid g = GoldenGrid.of(c[0], c[1]);
            int shown = 0;
            for (GoldenGrid.Vine v : g.vines) {
                int len = BiosphereCycle.vineLength(g.seed, v.u(), v.v(), v.max(), 0L);
                int top = g.floorY + v.anchor();
                String first = r.at(g.cx + v.u(), top - 1, g.cz + v.v());
                if (!first.startsWith("minecraft:cave_vines")) continue;               // another crown's leaves took the cell
                shown++;
                for (int i = 1; i <= len; i++) assertTrue(r.at(g.cx + v.u(), top - i, g.cz + v.v()).startsWith("minecraft:cave_vines"), "a vine of " + len + " cells at " + v);
                assertFalse(r.at(g.cx + v.u(), top - len - 1, g.cz + v.v()).startsWith("minecraft:cave_vines"), "and no more");
            }
            assertTrue(shown >= g.vines.size() * 0.9, "most of the vines are there: " + shown + " of " + g.vines.size());
        }
    }

    @Test
    void theCeilingVinesOverTheCityEndAboveItsRoof() {
        for (int[] c : CELLS) {
            ZonePlan p = GoldenCityPlan.of(c[0], c[1]);
            GoldenGrid g = GoldenGrid.of(c[0], c[1]);
            int over = 0;
            for (NexusOp o : p.ops) {
                if (!o.block().equals("minecraft:cave_vines") || o.y2() <= g.floorY + GoldenGrid.CEIL_MIN - 8) continue;
                if (!g.inCity(o.x1() - g.cx, o.z1() - g.cz, 0)) continue;
                over++;
                assertTrue(o.y1() >= CityPlan.CAP_Y + 2, "a vine over the city ends at " + o.y1() + ", the roof is at " + CityPlan.CAP_Y);
            }
            assertTrue(over > 20, "there are vines over the city: " + over);
        }
    }

    @Test
    void theGreatTreesStandOnTheGroundAndTheirCrownsRiseOverTheCityOnlyWellAboveItsRoof() {
        boolean sawGreat = false;
        for (int[] c : CELLS) {
            GoldenGrid g = GoldenGrid.of(c[0], c[1]);
            for (GoldenGrid.Site s : g.sites) {
                if (s.kind() != GoldenGrid.GREAT_TREE) continue;
                sawGreat = true;
                assertTrue(s.ht() >= 18 && s.ry() == 4 && s.r() >= 8 && s.r() <= 11);
                int yc = GoldenFlora.canopyMiddle(g, s), low = yc - s.ry();
                assertTrue(low > g.groundAt(s.u(), s.v()) + 8, "a crown far above the ground");
                assertTrue(yc + s.ry() + 1 <= g.ceil(s.u(), s.v()) - 2, "and below the ceiling");
                for (int du = -s.r(); du <= s.r(); du++) {
                    for (int dv = -s.r(); dv <= s.r(); dv++) {
                        if (du * du + dv * dv > s.r() * s.r() || !g.inCity(s.u() + du, s.v() + dv, 0)) continue;
                        assertTrue(g.floorY + low >= CityPlan.CAP_Y + 2, "over the city only above its roof: " + (g.floorY + low) + " vs " + CityPlan.CAP_Y);
                    }
                }
            }
        }
        assertTrue(sawGreat, "there are giant golden trees in the gardens");
    }

    @Test
    void theMushroomsHaveStemsUnderTheirCapsAndGlowUnderneath() {
        for (int[] c : CELLS) {
            ZonePlan p = GoldenCityPlan.of(c[0], c[1]);
            GoldenGrid g = GoldenGrid.of(c[0], c[1]);
            boolean glow = false;
            for (GoldenGrid.Site s : g.sites) {
                if (s.kind() != GoldenGrid.GIANT_MUSHROOM) continue;
                int x = g.cx + s.u(), z = g.cz + s.v();
                boolean stem = false, cap = false;
                for (NexusOp o : p.ops) {
                    if (o.x1() > x || o.x2() < x || o.z1() > z || o.z2() < z) continue;
                    if (o.block().equals(GoldenFlora.STEM) && o.y2() - o.y1() >= 3) stem = true;
                    if (o.block().equals(GoldenFlora.RED_CAP) || o.block().equals(GoldenFlora.BROWN_CAP)) cap = true;
                    if (o.block().equals(GoldenFlora.SHROOMLIGHT)) glow = true;
                }
                assertTrue(stem && cap, "a stem and a cap at " + s);
            }
            assertTrue(glow, "gills that glow");
        }
    }

    @Test
    void theCeilingCarriesVinesLichenRootsAndSporeBlossomsAndLights() {
        for (int[] c : CELLS) {
            ZonePlan p = GoldenCityPlan.of(c[0], c[1]);
            int vines = 0, lichen = 0, roots = 0, blossom = 0, froglight = 0;
            for (NexusOp o : p.ops) {
                switch (o.block()) {
                    case "minecraft:cave_vines" -> vines++;
                    case "minecraft:glow_lichen" -> lichen++;
                    case "minecraft:hanging_roots" -> roots++;
                    case "minecraft:spore_blossom" -> blossom++;
                    case "minecraft:ochre_froglight" -> froglight++;
                    default -> { }
                }
            }
            assertTrue(vines > 200 && lichen > 200 && roots > 50 && blossom > 20 && froglight > 100, vines + " " + lichen + " " + roots + " " + blossom + " " + froglight);
        }
    }

    @Test
    void theOpsFollowTheFrameAndTheirBlocksAreNamedAndPropertiesAreWellFormed() {
        for (int[] c : CELLS) {
            ZonePlan p = GoldenCityPlan.of(c[0], c[1]);
            for (NexusOp o : p.ops) {
                assertTrue(o.block().contains(":"), "a block named by its id: " + o);
                for (String[] kv : com.newuniverse.nusmp.dungeon.nexus.BlockProps.parse(o.props())) assertFalse(kv[1].isEmpty(), "property " + kv[0] + " of " + o);
                if (o.kind() == NexusOp.Kind.CARVE) assertEquals(NexusOp.AIR, o.block());
            }
        }
    }

    @Test
    void theTreasuryOpsAreInTheFrameOfTheCityAndStayUnderTheAvenueAndThePlaza() {
        for (int[] c : CELLS) {
            CityPlan city = GoldenGrid.of(c[0], c[1]).city;
            ZonePlan t = GoldenCityPlan.treasury(c[0], c[1]);
            assertEquals(2, t.phaseCount(), "carve, then the stonework");
            assertSame(t, GoldenCityPlan.treasury(c[0], c[1]));
            assertEquals(t.ops, GoldenTreasury.ops(city, c[0], c[1]).ops, "deterministic");
            for (NexusOp o : t.ops) {
                int u1 = o.x1() - city.centreX, u2 = o.x2() - city.centreX, v1 = o.z1() - city.centreZ, v2 = o.z2() - city.centreZ;
                assertTrue(u1 >= -7 && u2 <= 7, "x of " + o);
                assertTrue(v1 >= -18 && v2 <= 7, "z of " + o);
                assertTrue(o.y1() >= city.floorY - 5 && o.y2() <= city.floorY + 6, "y of " + o);
            }
            assertEquals(city.centreX, t.centreX());
            assertEquals(city.centreZ, t.centreZ());
        }
    }

    @Test
    void opsCollectorKeepsTheFrameAtTheOrigin() {
        NexusOps o = new NexusOps(100, 200);
        o.put("a:b", 3, 4, 5);
        assertEquals(103, o.list().get(0).x1());
        assertEquals(205, o.list().get(0).z1());
    }
}
