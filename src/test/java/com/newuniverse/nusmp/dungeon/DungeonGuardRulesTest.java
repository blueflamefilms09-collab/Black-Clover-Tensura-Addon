package com.newuniverse.nusmp.dungeon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The unbreakable-Dungeon rule: what is held, what is let go, and which changes are refused. */
class DungeonGuardRulesTest {
    // old state flags: air, fluid, fire, fixture; new state flags: air, fluid, fire
    private static boolean refuses(boolean oldAir, boolean oldFluid, boolean oldFire, boolean oldFixture, boolean newAir, boolean newFluid, boolean newFire) {
        return DungeonGuardRules.refusesChange(0, oldAir, oldFluid, oldFire, oldFixture, newAir, newFluid, newFire);
    }

    @Test
    void anOrdinaryBlockIsHeld() {
        assertTrue(DungeonGuardRules.isProtected(0, false, false, false, false));
        assertFalse(DungeonGuardRules.exempt(0, false, false, false, false));
    }

    @Test
    void airFluidsFireAndTheLivingFixturesAreNotHeld() {
        assertFalse(DungeonGuardRules.isProtected(0, true, false, false, false), "air");
        assertFalse(DungeonGuardRules.isProtected(0, false, true, false, false), "a fluid");
        assertFalse(DungeonGuardRules.isProtected(0, false, false, true, false), "fire must be able to burn out");
        assertFalse(DungeonGuardRules.isProtected(0, false, false, false, true), "a carcass or a veil of mycelium");
    }

    @Test
    void theModsOwnBuildersAreLetThroughWhateverIsThere() {
        assertTrue(DungeonGuardRules.exempt(1, false, false, false, false));
        assertTrue(DungeonGuardRules.exempt(3, false, false, false, false));
        assertFalse(DungeonGuardRules.refusesChange(1, false, false, false, false, true, false, false), "carving rock inside a builder");
        assertTrue(DungeonGuardRules.refusesChange(0, false, false, false, false, true, false, false), "and outside it");
    }

    @Test
    void onlyAirFluidAndFireTakeABlockAway() {
        assertTrue(DungeonGuardRules.takesAway(true, false, false));
        assertTrue(DungeonGuardRules.takesAway(false, true, false));
        assertTrue(DungeonGuardRules.takesAway(false, false, true));
        assertFalse(DungeonGuardRules.takesAway(false, false, false), "another block does not");
    }

    @Test
    void aHeldBlockCannotBecomeAirAFluidOrFire() {
        assertTrue(refuses(false, false, false, false, true, false, false), "stone -> air");
        assertTrue(refuses(false, false, false, false, false, true, false), "stone -> water or lava");
        assertTrue(refuses(false, false, false, false, false, false, true), "stone -> fire");
        assertTrue(refuses(false, false, false, false, true, true, true), "all at once");
    }

    @Test
    void placingAndChangingAreNotRefused() {
        assertFalse(refuses(false, false, false, false, false, false, false), "a block for a block (a door opening, grass to dirt)");
        assertFalse(refuses(true, false, false, false, false, false, false), "a block placed in the air");
        assertFalse(refuses(true, false, false, false, true, false, false), "air to air");
        assertFalse(refuses(false, true, false, false, false, false, false), "a block placed in water");
    }

    @Test
    void whatIsNotHeldCanBeTakenAway() {
        assertFalse(refuses(true, false, false, false, false, true, false), "water poured into air");
        assertFalse(refuses(false, true, false, false, true, false, false), "a fluid drained");
        assertFalse(refuses(false, false, true, false, true, false, false), "a fire going out");
        assertFalse(refuses(false, false, false, true, true, false, false), "a carcass eaten");
        assertFalse(refuses(false, false, false, true, false, true, false), "a veil of mycelium flooded");
    }

    @Test
    void everyCombinationFollowsTheRule() {
        for (int bits = 0; bits < 128; bits++) {
            boolean oAir = (bits & 1) != 0, oFluid = (bits & 2) != 0, oFire = (bits & 4) != 0, oFixture = (bits & 8) != 0;
            boolean nAir = (bits & 16) != 0, nFluid = (bits & 32) != 0, nFire = (bits & 64) != 0;
            boolean expected = !oAir && !oFluid && !oFire && !oFixture && (nAir || nFluid || nFire);
            assertEquals(expected, refuses(oAir, oFluid, oFire, oFixture, nAir, nFluid, nFire), "combination " + bits);
            assertFalse(DungeonGuardRules.refusesChange(2, oAir, oFluid, oFire, oFixture, nAir, nFluid, nFire), "never inside a builder: " + bits);
        }
    }

    @Test
    void aPlayerIsToldOnceEveryFewSeconds() {
        long gap = DungeonGuardRules.MESSAGE_INTERVAL;
        assertTrue(gap >= 40 && gap <= 200, "a few seconds");
        assertTrue(DungeonGuardRules.messageDue(1000, 0, gap), "the first time");
        assertFalse(DungeonGuardRules.messageDue(1010, 1000, gap), "straight after");
        assertFalse(DungeonGuardRules.messageDue(1000 + gap - 1, 1000, gap));
        assertTrue(DungeonGuardRules.messageDue(1000 + gap, 1000, gap), "after the gap");
        assertTrue(DungeonGuardRules.messageDue(50, 1000, gap), "a clock that went backwards (another world) counts as due");
    }

    @Test
    void builderDepthNestsAndNeverGoesBelowZero() {
        DungeonGuardRules.Depth d = new DungeonGuardRules.Depth();
        assertEquals(0, d.get());
        d.begin();
        d.begin();
        assertEquals(2, d.get());
        d.end();
        assertEquals(1, d.get());
        d.end();
        d.end();
        d.end();
        assertEquals(0, d.get(), "a stray end() cannot push it negative (the next builder would then be refused)");
        d.begin();
        assertEquals(1, d.get());
    }
}
