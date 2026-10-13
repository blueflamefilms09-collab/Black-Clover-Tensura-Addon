package com.newuniverse.nusmp.dungeon.nexus.collapse;

import com.newuniverse.nusmp.dungeon.nexus.Footprint;
import com.newuniverse.nusmp.dungeon.nexus.NexusState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** When two collapses are one, the ground of a pocket, who is inside, and how hard the expulsion hits. */
class CollapseRulesTest {
    @Test
    void twoCollapsesWithinNinetySixBlocksAreTheSameOne() {
        assertTrue(CollapseRules.sameArea(0, 0, 0, 0));
        assertTrue(CollapseRules.sameArea(0, 0, 96, 0), "96 blocks apart is still one area");
        assertTrue(CollapseRules.sameArea(0, 0, 0, -96));
        assertTrue(CollapseRules.sameArea(0, 0, 67, 67), "diagonals count by real distance (94.7)");
        assertFalse(CollapseRules.sameArea(0, 0, 97, 0));
        assertFalse(CollapseRules.sameArea(0, 0, 68, 68), "96.2 blocks");
        assertFalse(CollapseRules.sameArea(-100, 0, 100, 0));
        assertFalse(CollapseRules.sameArea(Integer.MIN_VALUE, 0, Integer.MAX_VALUE, 0), "no overflow");
        assertEquals(CollapseRules.sameArea(10, 20, -30, 5), CollapseRules.sameArea(-30, 5, 10, 20), "symmetric");
        assertFalse(CollapseRules.sameArea(0, 0, 128, 0), "the centres of two neighbouring core cells are two areas");
    }

    @Test
    void aPocketIsAnOvalRoundTheSpotWithItsFloorUnderTheFeet() {
        CollapseRules.Ground g = CollapseRules.pocket(100, -30, -200);
        assertEquals(Footprint.Shape.OVAL, g.fp().shape());
        assertEquals(100, g.fp().centreX());
        assertEquals(-200, g.fp().centreZ());
        assertEquals(CollapseRules.POCKET_RADIUS, g.fp().halfX());
        assertEquals(CollapseRules.POCKET_RADIUS, g.fp().halfZ());
        assertEquals(-31, g.floorY());
        assertEquals(-31 + CollapseRules.POCKET_HEIGHT, g.topY());
        assertTrue(g.topY() > g.floorY());
        assertTrue(CollapseRules.inside(g.fp(), g.floorY(), g.topY(), 100, -30, -200), "the spot itself is inside");
        assertTrue(2 * CollapseRules.POCKET_RADIUS < 64, "a pocket is smaller than a chunk grid cell of a vault");
    }

    @Test
    void insideMeansTheFootprintWithItsEdgeBetweenTheFloorAndAboveTheRoof() {
        Footprint fp = Footprint.oval(0, 0, 40, 40);
        int floor = -52, top = -13;
        assertTrue(CollapseRules.inside(fp, floor, top, 0, floor + 1, 0));
        assertTrue(CollapseRules.inside(fp, floor, top, 41, floor + 1, 0), "the two blocks of edge");
        assertTrue(CollapseRules.inside(fp, floor, top, 42, floor + 1, 0));
        assertFalse(CollapseRules.inside(fp, floor, top, 43, floor + 1, 0));
        assertFalse(CollapseRules.inside(fp, floor, top, 31, floor + 1, 31), "the oval is not a square");
        assertTrue(CollapseRules.inside(fp, floor, top, 0, floor - 4, 0), "four below the floor");
        assertFalse(CollapseRules.inside(fp, floor, top, 0, floor - 5, 0), "under the cell: no");
        assertTrue(CollapseRules.inside(fp, floor, top, 0, top + 2, 0));
        assertFalse(CollapseRules.inside(fp, floor, top, 0, top + 3, 0), "above the roof: no");
    }

    @Test
    void theExpulsionNeverKillsUnlessItIsMadeLethal() {
        for (float max : new float[]{1f, 6f, 20f, 40f, 2000f, 1e7f}) {
            for (float frac : new float[]{0.01f, 0.1f, 0.5f, 0.74f, 0.76f, 1f}) {
                float health = Math.max(0.5f, max * frac);
                float d = CollapseRules.expelDamage(health, max, false);
                assertTrue(d >= 0f, "never negative");
                assertTrue(health - d >= Math.min(health, CollapseRules.SPARED_HEALTH) - 1e-3f, "half a heart is left: " + health + " - " + d);
                assertTrue(d <= max * CollapseRules.EXPEL_SHARE + 1e-3f, "never more than heavy");
            }
        }
        assertEquals(15f, CollapseRules.expelDamage(20f, 20f, false), 1e-4f, "three quarters of a full bar");
        assertEquals(4f, CollapseRules.expelDamage(5f, 20f, false), 1e-4f, "a player at 5 is left at 1");
        assertEquals(0f, CollapseRules.expelDamage(1f, 20f, false), "half a heart takes nothing more");
        assertEquals(0f, CollapseRules.expelDamage(0.5f, 20f, false));
        assertEquals(15f, CollapseRules.expelDamage(3f, 20f, true), 1e-4f, "lethal: the full heavy blow, whatever is left");
        assertTrue(CollapseRules.expelDamage(3f, 20f, true) > 3f, "which is more than they have");
    }

    @Test
    void brokenValuesNeverHurt() {
        assertEquals(0f, CollapseRules.expelDamage(Float.NaN, 20f, false));
        assertEquals(0f, CollapseRules.expelDamage(10f, Float.NaN, false));
        assertEquals(0f, CollapseRules.expelDamage(10f, 0f, false));
        assertEquals(0f, CollapseRules.expelDamage(10f, -4f, true));
        assertEquals(0f, CollapseRules.expelDamage(0f, 20f, true), "an empty bar: nothing");
        assertEquals(0f, CollapseRules.expelDamage(-3f, 20f, false));
        assertTrue(CollapseRules.expelDamage(Float.POSITIVE_INFINITY, 20f, false) >= 0f);
        assertFalse(Float.isNaN(CollapseRules.expelDamage(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, false)));
    }

    @Test
    void thePoisonIsHeavyButFinite() {
        assertTrue(CollapseRules.POISON_TICKS >= 1200 && CollapseRules.POISON_TICKS <= 6000);
        assertTrue(CollapseRules.POISON_LEVEL >= 1 && CollapseRules.POISON_LEVEL <= 4, "the effect has five levels (0 to 4)");
        assertTrue(CollapseRules.EXPEL_SHARE > 0.5f && CollapseRules.EXPEL_SHARE < 1f);
    }

    @Test
    void theBlocksThatFallHurtALittleAndTheDripsAFewHearts() {
        assertEquals(CollapsePlan.DRIP_DAMAGE, CollapseRules.hit(true));
        assertEquals(CollapsePlan.FRONT_HIT, CollapseRules.hit(false));
        assertTrue(CollapseRules.hit(false) <= CollapseRules.hit(true));
        assertTrue(CollapseRules.hit(true) <= 4, "never more than two hearts");
    }

    @Test
    void aStandingNexusFallsAndEverywhereElseIsAPocket() {
        for (boolean boss : new boolean[]{false, true}) {
            assertSame(CollapseRules.Mode.POCKET, CollapseRules.modeFor(null, boss), "a spot in no Nexus");
            assertSame(CollapseRules.Mode.POCKET, CollapseRules.modeFor(NexusState.UNBUILT, boss));
            assertSame(CollapseRules.Mode.NEXUS, CollapseRules.modeFor(NexusState.DONE, boss), "the whole Nexus falls, whatever started it");
            assertSame(CollapseRules.Mode.NOT_READY, CollapseRules.modeFor(NexusState.PLAN, boss), "the builder is writing it");
            assertSame(CollapseRules.Mode.NOT_READY, CollapseRules.modeFor(NexusState.BUILDING, boss));
        }
        assertSame(CollapseRules.Mode.POCKET, CollapseRules.modeFor(NexusState.COLLAPSED, false), "a Wild Magic Surge in the ruin is a pocket");
        assertSame(CollapseRules.Mode.GONE, CollapseRules.modeFor(NexusState.COLLAPSED, true), "the boss of a collapsed Nexus cannot collapse it again");
        for (NexusState st : NexusState.values()) for (boolean boss : new boolean[]{false, true}) assertNotNull(CollapseRules.modeFor(st, boss));
    }

    @Test
    void theNexusIsWatchedForAResetOnlyAfterItWasSeenCollapsed() {
        assertSame(CollapseRules.Watch.KEEP, CollapseRules.watch(false, NexusState.COLLAPSED));
        assertSame(CollapseRules.Watch.KEEP, CollapseRules.watch(true, NexusState.COLLAPSED));
        for (NexusState st : new NexusState[]{NexusState.DONE, NexusState.UNBUILT, NexusState.PLAN, NexusState.BUILDING}) {
            assertSame(CollapseRules.Watch.MARK, CollapseRules.watch(false, st), "not marked yet (or the mark was lost): mark it, never end");
            assertSame(CollapseRules.Watch.END, CollapseRules.watch(true, st), "it was marked and now is not: an admin reset or rebuilt it, so the collapse ends");
        }
    }

    @Test
    void theFrontWarnsOnlyWhereItIsAboutToComeDown() {
        CollapsePlan.Stage roll = CollapsePlan.Stage.ROLLING;
        assertTrue(CollapseRules.frontAhead(roll, 100, 100, 400), "at the cursor");
        assertTrue(CollapseRules.frontAhead(roll, 399, 100, 400));
        assertFalse(CollapseRules.frontAhead(roll, 400, 100, 400), "beyond what it reaches in four seconds");
        assertFalse(CollapseRules.frontAhead(roll, 99, 100, 400), "already down");
        assertFalse(CollapseRules.frontAhead(roll, -1, 100, 400), "not a column of the footprint");
        assertFalse(CollapseRules.frontAhead(CollapsePlan.Stage.CRACKS, 100, 0, 400), "no warning while it only cracks");
        assertFalse(CollapseRules.frontAhead(CollapsePlan.Stage.WIPE, 100, 0, 400), "the wipe warns by its clock, not by a column");
        assertTrue(CollapseRules.frontAhead(CollapsePlan.Stage.FINAL, 10, 0, 20));
        assertFalse(CollapseRules.frontAhead(roll, 5, 5, 5), "an empty window");
    }

    @Test
    void groundThatCouldFreezeTheServerIsNotWorkedOn() {
        assertTrue(CollapseRules.sane(Footprint.oval(0, 0, 47, 47), -52, -13));
        assertTrue(CollapseRules.sane(Footprint.rect(-1280, 64, 56, 56), -53, -12), "the biggest Nexus (a Golden City, 113 wide)");
        assertTrue(CollapseRules.sane(CollapseRules.pocket(5, 5, 5).fp(), 4, 19));
        assertFalse(CollapseRules.sane(Footprint.oval(0, 0, 101, 40), -52, -13), "wider than a core cell by far");
        assertFalse(CollapseRules.sane(Footprint.oval(0, 0, 40, 5000), -52, -13));
        assertFalse(CollapseRules.sane(Footprint.oval(0, 0, 40, 40), -52, -52), "no height");
        assertFalse(CollapseRules.sane(Footprint.oval(0, 0, 40, 40), -52, -60), "upside down");
        assertFalse(CollapseRules.sane(Footprint.oval(0, 0, 40, 40), -52, 1_000_000), "a roof a million blocks up");
        assertFalse(CollapseRules.sane(Footprint.oval(0, 0, 40, 40), Integer.MIN_VALUE, 0));
        assertFalse(CollapseRules.sane(Footprint.oval(2_000_000_000, 0, 40, 40), -52, -13), "beyond the world border");
        assertFalse(CollapseRules.sane(null, -52, -13));
    }
}
