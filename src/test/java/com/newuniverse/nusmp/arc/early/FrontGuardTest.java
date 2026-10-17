package com.newuniverse.nusmp.arc.early;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the armour only teaches "walk round him" if it has a front and nothing behind, and if anti-magic cuts straight through. */
class FrontGuardTest {
    @Test
    void theFrontIsCutAndTheRestIsNot() {
        assertEquals(0.4, FrontGuard.factor(true, 0.0, false), 1e-9, "straight ahead");
        assertEquals(0.4, FrontGuard.factor(true, Math.toRadians(64), false), 1e-9, "inside the arc");
        assertEquals(1.0, FrontGuard.factor(true, Math.toRadians(66), false), 1e-9, "just outside the arc");
        assertEquals(1.0, FrontGuard.factor(true, Math.PI / 2, false), 1e-9, "from the side");
        assertEquals(1.0, FrontGuard.factor(true, Math.PI, false), 1e-9, "from behind");
    }

    @Test
    void noArmorAndAntiMagicAreUntouched() {
        assertEquals(1.0, FrontGuard.factor(false, 0.0, false), 1e-9, "the armor is down");
        assertEquals(1.0, FrontGuard.factor(true, 0.0, true), 1e-9, "anti-magic cuts straight through");
    }
}
