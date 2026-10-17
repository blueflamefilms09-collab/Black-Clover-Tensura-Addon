package com.newuniverse.nusmp.labyrinth.mega.macro;

import com.newuniverse.nusmp.labyrinth.mega.HubLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the whole point of pre-collapsing the stair and the ramp is that the two gates are ALWAYS reachable on foot, whatever the seed. This runs the
 * walkability search (a breadth-first search over standing states, see {@link Walkability}) from the road outside each door to the cell in front of each gate on
 * twenty different seeds, so every roll of the noise and of the Wave Function Collapse is covered, and then runs the same search on a world where one cross
 * section of the walkway has been cut away, which must FAIL: a search that cannot fail proves nothing. Walking is also checked cell by cell along the way
 * (every move is a legal step of at most one block) by the search itself.
 */
public class WalkabilityTest {
    static Walkability.Result toHolyGate(Walkability.ColumnSource src) {
        return Walkability.reach(src, WalkRoutes.spireBox(), WalkRoutes.SPIRE_START[0], WalkRoutes.SPIRE_START[1], WalkRoutes.SPIRE_START[2],
                WalkRoutes.HOLY_TARGET[0], WalkRoutes.HOLY_TARGET[1], WalkRoutes.HOLY_TARGET[2]);
    }

    static Walkability.Result toAbyssGate(Walkability.ColumnSource src) {
        return Walkability.reach(src, WalkRoutes.chasmBox(), WalkRoutes.CHASM_START[0], WalkRoutes.CHASM_START[1], WalkRoutes.CHASM_START[2],
                WalkRoutes.ABYSS_TARGET[0], WalkRoutes.ABYSS_TARGET[1], WalkRoutes.ABYSS_TARGET[2]);
    }

    @Test
    public void bothGatesAreReachableOnFootOnTwentySeeds() {
        for (long seed = 1; seed <= 20; seed++) {
            HubModel m = HubModel.of(seed * 7919L + 3);
            Walkability.Result a = toHolyGate(m::fillColumn);
            assertTrue(a.reached(), "the Holy gate must be reachable, seed " + seed + " (visited " + a.visited() + ")");
            Walkability.Result b = toAbyssGate(m::fillColumn);
            assertTrue(b.reached(), "the Abyssal gate must be reachable, seed " + seed + " (visited " + b.visited() + ")");
        }
    }

    @Test
    public void theRoadsSurviveTheRoughestSettingsOfTheConfig() {
        // spireNoise 4 and chasmNoise 6 are the config's maxima: ribs of rock up to seven blocks long jut into the pit, and the shell of the Spire moves by four
        // blocks; the carved walkway must still be clear because it is cut AFTER the noise, which is the whole point of the pre-collapsed path
        for (long seed = 1; seed <= 6; seed++) {
            HubModel m = HubModel.fresh(seed * 104729L, new MacroParams(6, 4.0, 6.0, 12.0, 12, true));
            assertTrue(toHolyGate(m::fillColumn).reached(), "Holy gate at maximum roughness, seed " + seed);
            assertTrue(toAbyssGate(m::fillColumn).reached(), "Abyssal gate at maximum roughness, seed " + seed);
        }
    }

    @Test
    public void theRoutesHaveTheLengthOfTwoSpirals() {
        HubModel m = HubModel.of(12345L);
        Walkability.Result a = toHolyGate(m::fillColumn), b = toAbyssGate(m::fillColumn);
        // the stair makes about two and a third turns of radius 17-30 (350 steps of arc plus the hall and the bridge); the ramp about a turn and a half
        assertTrue(a.steps() > 250 && a.steps() < 900, "Spire route length " + a.steps());
        assertTrue(b.steps() > 150 && b.steps() < 700, "Chasm route length " + b.steps());
    }

    /** Cuts one cross section (3 blocks of arc, the whole width, floor to headroom) out of a walkway and returns the source with the gap. */
    static Walkability.ColumnSource cut(Walkability.ColumnSource src, Spiral sp, int step, int cx, int cz) {
        double ang0 = sp.path.angleOf(step), r0 = sp.path.radius(step);
        int row = sp.surfaceRow(step);
        return (x, z, out, y0, y1) -> {
            src.fill(x, z, out, y0, y1);
            double dx = x - cx, dz = z - cz, r = Math.sqrt(dx * dx + dz * dz);
            double da = Math.abs(Geo.wrapPi(Geo.angle(dx, dz) - (ang0 + sp.path.dir * 1.5 / r0)));
            if (da * r0 < 2.2 && Math.abs(r - r0) < 6) for (int y = Math.max(y0, row - 3); y <= Math.min(y1, row + 7); y++) out[y - HubModel.MIN_Y] = BlockKind.AIR;
        };
    }

    private static int firstStepAtQ(Spiral sp, int q) {
        for (int i = 0; i < sp.path.steps(); i++) if (sp.path.q(i) == q && !sp.path.isLanding(i)) return i;
        throw new AssertionError("no step at q " + q);
    }

    @Test
    public void cuttingTheStairOrTheRampMakesTheSearchFail() {
        HubModel m = HubModel.of(4242L);
        Spiral stair = m.spire().stair();
        Walkability.Result broken = toHolyGate(cut(m::fillColumn, stair, firstStepAtQ(stair, 120), SpireModel.CX, SpireModel.CZ));
        assertFalse(broken.reached(), "a stair with a gap in it must not be passable");
        Spiral ramp = m.chasm().ramp();
        Walkability.Result broken2 = toAbyssGate(cut(m::fillColumn, ramp, firstStepAtQ(ramp, 120), ChasmModel.CX, ChasmModel.CZ));
        assertFalse(broken2.reached(), "a ramp with a gap in it must not be passable");
        // and the same worlds without the cut are passable, so the cut (not the box or the start) is what made it fail
        assertTrue(toHolyGate(m::fillColumn).reached());
        assertTrue(toAbyssGate(m::fillColumn).reached());
    }

    @Test
    public void aLandingIsNotABypassTheRailIsReal() {
        // the rail column really blocks: standing on the rail's curb is possible (it is a block) but the cell above it is a rail, which is not passable
        HubModel m = HubModel.of(99L);
        Spiral stair = m.spire().stair();
        int step = firstStepAtQ(stair, 100);
        double ang = stair.path.angleOf(step) + 0.5 / stair.path.radius(step);
        double r = stair.path.radius(step) - 2.0; // the rail column
        int x = (int) Math.round(SpireModel.CX + r * Math.cos(ang)), z = (int) Math.round(SpireModel.CZ + r * Math.sin(ang));
        BlockKind[] col = new BlockKind[HubModel.HEIGHT];
        m.fillColumn(x, z, col);
        int railY = stair.surfaceRow(step) + 1;
        boolean anyRail = false;
        for (int dy = -1; dy <= 3; dy++) anyRail |= col[railY + dy - HubModel.MIN_Y] == BlockKind.RAIL_LIGHT;
        assertTrue(anyRail, "the open side of the stair carries a railing near (" + x + "," + z + ")");
        assertEquals(false, BlockKind.RAIL_LIGHT.passable);
    }
}
