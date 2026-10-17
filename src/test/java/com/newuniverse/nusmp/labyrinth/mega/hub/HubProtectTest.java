package com.newuniverse.nusmp.labyrinth.mega.hub;

import com.newuniverse.nusmp.labyrinth.mega.HubLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the protected volumes of Floor 0 decide what a player can and cannot change; a box that is one block too small shows up as a missing step of the
 * way home, a sphere that is too big as a base the owner cannot build. The question is a pure function of three integers, so every boundary is pinned here:
 * inside and outside each face of the plaza box, the centres and the radius of the two gate spheres, the switch matrix of {@link HubProtect#refuses} and the
 * message rate rule. The creative-mode bypass is the event handler's, not this class's, so it is not tested here.
 */
class HubProtectTest {

    @Test
    void theHomeGateTheArrivalCellAndTheSignpostsAreInsideThePlazaBox() {
        assertTrue(HubProtect.inPlazaBox(HubLayout.HOME_GATE_X, HubLayout.HOME_GATE_Y, HubLayout.HOME_GATE_Z), "the way home");
        assertTrue(HubProtect.inPlazaBox(HubLayout.SPAWN_X, HubLayout.SPAWN_Y, HubLayout.SPAWN_Z), "the arrival cell");
        assertTrue(HubProtect.inPlazaBox(HubLayout.PLAZA_X + 12, HubLayout.SURFACE_Y + 1, HubLayout.PLAZA_Z - 3), "the east signpost");
        assertTrue(HubProtect.inPlazaBox(HubLayout.PLAZA_X - 12, HubLayout.SURFACE_Y + 1, HubLayout.PLAZA_Z - 3), "the west signpost");
        assertTrue(HubProtect.inPlazaBox(HubLayout.PLAZA_X, HubLayout.SURFACE_Y - 1, HubLayout.PLAZA_Z), "the paving under the fountain");
    }

    @Test
    void everyFaceOfThePlazaBoxIsExact() {
        int x0 = HubPlazaLayout.originX(), x1 = HubPlazaLayout.maxX(), y0 = HubPlazaLayout.originY(), y1 = HubPlazaLayout.maxY(), z0 = HubPlazaLayout.originZ(), z1 = HubPlazaLayout.maxZ();
        int mx = (x0 + x1) / 2, my = (y0 + y1) / 2, mz = (z0 + z1) / 2;
        assertTrue(HubProtect.inPlazaBox(x0, my, mz) && HubProtect.inPlazaBox(x1, my, mz), "west and east faces are inside");
        assertFalse(HubProtect.inPlazaBox(x0 - 1, my, mz) || HubProtect.inPlazaBox(x1 + 1, my, mz), "one block beyond west and east is outside");
        assertTrue(HubProtect.inPlazaBox(mx, y0, mz) && HubProtect.inPlazaBox(mx, y1, mz), "bottom and top faces are inside");
        assertFalse(HubProtect.inPlazaBox(mx, y0 - 1, mz) || HubProtect.inPlazaBox(mx, y1 + 1, mz), "one block under and over is outside");
        assertTrue(HubProtect.inPlazaBox(mx, my, z0) && HubProtect.inPlazaBox(mx, my, z1), "north and south faces are inside");
        assertFalse(HubProtect.inPlazaBox(mx, my, z0 - 1) || HubProtect.inPlazaBox(mx, my, z1 + 1), "one block beyond north and south is outside");
        assertEquals(33, x1 - x0 + 1, "the box is as wide as the template");
        assertEquals(33, z1 - z0 + 1, "and as deep");
    }

    @Test
    void theGateSpheresHaveRadiusEightAndAreSpherical() {
        int[][] gates = {
                {HubLayout.HOLY_GATE_X, HubLayout.HOLY_GATE_Y, HubLayout.HOLY_GATE_Z},
                {HubLayout.ABYSS_GATE_X, HubLayout.ABYSS_GATE_Y, HubLayout.ABYSS_GATE_Z}};
        for (int[] g : gates) {
            assertTrue(HubProtect.nearPolarityGate(g[0], g[1], g[2]), "the centre");
            assertTrue(HubProtect.nearPolarityGate(g[0] + 8, g[1], g[2]) && HubProtect.nearPolarityGate(g[0] - 8, g[1], g[2]), "eight along x is inside");
            assertTrue(HubProtect.nearPolarityGate(g[0], g[1] + 8, g[2]) && HubProtect.nearPolarityGate(g[0], g[1] - 8, g[2]), "eight along y is inside");
            assertTrue(HubProtect.nearPolarityGate(g[0], g[1], g[2] + 8) && HubProtect.nearPolarityGate(g[0], g[1], g[2] - 8), "eight along z is inside");
            assertFalse(HubProtect.nearPolarityGate(g[0] + 9, g[1], g[2]) || HubProtect.nearPolarityGate(g[0], g[1] + 9, g[2]) || HubProtect.nearPolarityGate(g[0], g[1], g[2] + 9), "nine is outside");
            assertTrue(HubProtect.nearPolarityGate(g[0] + 5, g[1] + 5, g[2]), "(5,5,0) is 7.07 away: inside");
            assertFalse(HubProtect.nearPolarityGate(g[0] + 5, g[1] + 5, g[2] + 5), "(5,5,5) is 8.66 away: outside, so the volume is a sphere and not a cube");
        }
    }

    @Test
    void theSpheresAndTheBoxAreSeparateVolumesThatDoNotCoverTheLandmarks() {
        assertFalse(HubProtect.inPlazaBox(HubLayout.HOLY_GATE_X, HubLayout.HOLY_GATE_Y, HubLayout.HOLY_GATE_Z), "the Holy gate is not in the plaza box");
        assertFalse(HubProtect.inPlazaBox(HubLayout.ABYSS_GATE_X, HubLayout.ABYSS_GATE_Y, HubLayout.ABYSS_GATE_Z), "the Abyssal gate is not in the plaza box");
        assertFalse(HubProtect.nearPolarityGate(HubLayout.HOME_GATE_X, HubLayout.HOME_GATE_Y, HubLayout.HOME_GATE_Z), "the home gate is guarded by the box, not by a sphere");
        assertTrue(HubProtect.isProtected(HubLayout.HOME_GATE_X, HubLayout.HOME_GATE_Y, HubLayout.HOME_GATE_Z));
        assertTrue(HubProtect.isProtected(HubLayout.HOLY_GATE_X, HubLayout.HOLY_GATE_Y, HubLayout.HOLY_GATE_Z));
        assertTrue(HubProtect.isProtected(HubLayout.ABYSS_GATE_X, HubLayout.ABYSS_GATE_Y, HubLayout.ABYSS_GATE_Z));
        assertFalse(HubProtect.isProtected(HubLayout.PLAZA_X + 30, HubLayout.SURFACE_Y, HubLayout.PLAZA_Z + 30), "open plains are buildable");
        assertFalse(HubProtect.isProtected(HubLayout.SPIRE_X, HubLayout.SURFACE_Y, HubLayout.SPIRE_Z), "the foot of the Spire is not protected: only the gate platform is");
        assertFalse(HubProtect.isProtected(HubLayout.CHASM_X, HubLayout.SURFACE_Y, HubLayout.CHASM_Z), "the rim of the Chasm is not protected either");
        assertEquals(8, HubProtect.GATE_SPHERE_RADIUS);
    }

    @Test
    void theSwitchesDecideWhoIsRefusedWhere() {
        int px = HubLayout.HOME_GATE_X, py = HubLayout.HOME_GATE_Y, pz = HubLayout.HOME_GATE_Z;          // protected
        int ox = HubLayout.PLAZA_X + 30, oy = HubLayout.SURFACE_Y, oz = HubLayout.PLAZA_Z + 30;          // open
        assertTrue(HubProtect.refuses(true, true, px, py, pz), "build on, protect on: the plaza is refused");
        assertFalse(HubProtect.refuses(true, true, ox, oy, oz), "build on, protect on: open ground is not");
        assertFalse(HubProtect.refuses(true, false, px, py, pz), "protect off: even the plaza is open");
        assertFalse(HubProtect.refuses(true, false, ox, oy, oz));
        assertTrue(HubProtect.refuses(false, true, ox, oy, oz), "build off: the whole dimension is read-only");
        assertTrue(HubProtect.refuses(false, false, ox, oy, oz), "build off wins over protect off");
        assertTrue(HubProtect.refuses(false, false, px, py, pz));
    }

    @Test
    void aRefusalIsToldAtMostOnceAWhileAndATimeThatWentBackCountsAsDue() {
        assertFalse(HubProtect.messageDue(100, 100), "the same tick");
        assertFalse(HubProtect.messageDue(100 + HubProtect.MESSAGE_INTERVAL - 1, 100), "just before the interval");
        assertTrue(HubProtect.messageDue(100 + HubProtect.MESSAGE_INTERVAL, 100), "at the interval");
        assertTrue(HubProtect.messageDue(50, 100), "a stamp from the future (a restored backup)");
        assertTrue(HubProtect.messageDue(HubProtect.MESSAGE_INTERVAL, 0), "a player who was never told has stamp 0, which is long ago by the time the world is two seconds old");
    }

    @Test
    void theGateSphereRadiusCoversThePlatformButNotTheWholeLandmark() {
        // the gate plane is 3 x 4 and its frame and one platform step sit within eight blocks; the Spire's crown radius is far bigger than the sphere
        assertTrue(HubProtect.GATE_SPHERE_RADIUS < HubLayout.SPIRE_TOP_RADIUS, "the sphere is smaller than the crown, so players can still build round the Spire's top");
        assertNull(HubLayout.gateNear(HubLayout.HOLY_GATE_X + HubLayout.GATE_REACH + 1, HubLayout.HOLY_GATE_Y, HubLayout.HOLY_GATE_Z), "past the gate reach nothing is a gate");
    }
}
