package com.newuniverse.nusmp.heroweapon.weapon.shield;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.weapon.shield.PanelMath.Panel;
import org.junit.jupiter.api.Test;

/**
 * Why: the floating shield stops projectiles that cross a thin upright rectangle, and the two ways to get that wrong are a fast arrow that tunnels through between
 * two ticks and a shield that stops things beside or above it. The geometry is checked with a panel at the origin facing +z (so it spans x and y, and depth is z).
 */
class PanelMathTest {
    private static Panel panel() { return Panel.facing(0, 10, 0, 0, 1, 1.6, 1.6, 0.5); }

    @Test
    void theNormalIsUnitAndAZeroDirectionFacesSouth() {
        Panel p = Panel.facing(0, 0, 0, 3, 4, 1, 1, 0.5);
        assertEquals(0.6, p.nx(), 1e-9);
        assertEquals(0.8, p.nz(), 1e-9);
        Panel z = Panel.facing(0, 0, 0, 0, 0, 1, 1, 0.5);
        assertEquals(0.0, z.nx(), 1e-9);
        assertEquals(1.0, z.nz(), 1e-9);
    }

    @Test
    void aFastArrowThatCrossesThePlaneInsideTheRectangleIsStopped() {
        Panel p = panel();
        assertTrue(PanelMath.crosses(p, 0.5, 10.5, -3, 0.5, 10.5, 3), "an arrow at three blocks a tick, sixes across the whole slab in one step");
        assertTrue(PanelMath.crosses(p, -1.5, 9.0, 4, -1.5, 9.0, -2), "from the other side");
    }

    @Test
    void somethingThatPassesBesideOrAboveOrBelowIsNotStopped() {
        Panel p = panel();
        assertFalse(PanelMath.crosses(p, 2.0, 10, -3, 2.0, 10, 3), "beside: 2.0 from the centre, the half width is 1.6");
        assertFalse(PanelMath.crosses(p, 0, 12.0, -3, 0, 12.0, 3), "above");
        assertFalse(PanelMath.crosses(p, 0, 8.0, -3, 0, 8.0, 3), "below");
    }

    @Test
    void somethingThatStaysOnOneSideIsNotStopped() {
        Panel p = panel();
        assertFalse(PanelMath.crosses(p, 0, 10, -5, 0, 10, -2), "approaching but not there yet");
        assertFalse(PanelMath.crosses(p, 0, 10, 5, 0, 10, 2), "moving away on the far side");
    }

    @Test
    void somethingInsideTheThickSlabIsStoppedEvenIfItDoesNotCross() {
        Panel p = panel();
        assertTrue(PanelMath.crosses(p, 0, 10, 0.3, 0, 10, 0.4), "slow, inside the slab");
        assertTrue(p.contains(0, 10, 0.5));
        assertFalse(p.contains(0, 10, 0.51));
    }

    @Test
    void aRotatedPanelUsesItsOwnWidthAndDepth() {
        Panel p = Panel.facing(0, 10, 0, 1, 0, 1.6, 1.6, 0.5);           // faces +x: spans z and y
        assertTrue(PanelMath.crosses(p, -4, 10, 1.0, 4, 10, 1.0));
        assertFalse(PanelMath.crosses(p, -4, 10, 2.0, 4, 10, 2.0), "outside the width in z");
        assertEquals(3.0, p.depth(3, 7), 1e-9);
        assertEquals(7.0, p.across(3, 7), 1e-9);
    }

    @Test
    void anOffsetCentreIsRespected() {
        Panel p = Panel.facing(10, 64, -20, 0, 1, 1.0, 1.0, 0.25);
        assertTrue(PanelMath.crosses(p, 10, 64, -23, 10, 64, -17));
        assertFalse(PanelMath.crosses(p, 0, 64, -23, 0, 64, -17));
        assertFalse(PanelMath.crosses(p, 10, 66, -23, 10, 66, -17));
    }

    @Test
    void aMoveThatDoesNotMoveIsHandled() {
        Panel p = panel();
        assertFalse(PanelMath.crosses(p, 0, 10, 3, 0, 10, 3));
        assertTrue(PanelMath.crosses(p, 0, 10, 0, 0, 10, 0));
    }
}
