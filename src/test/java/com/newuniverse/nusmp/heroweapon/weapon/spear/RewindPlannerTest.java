package com.newuniverse.nusmp.heroweapon.weapon.spear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.weapon.pierce.SafeSpot;
import com.newuniverse.nusmp.heroweapon.weapon.spear.RewindBuffer.Snap;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Why: this is the owner's rule for the time reversal in executable form. The wielder goes to the oldest moment of the last eight seconds (but not one
 * younger than three) whose place is safe or has a safe place within three blocks, in their own dimension; if no moment qualifies the technique is
 * refused; the heal is what they had then minus what they have now, at most a quarter of their maximum, and never negative.
 */
class RewindPlannerTest {
    private static final String OVERWORLD = "minecraft:overworld", NETHER = "minecraft:the_nether";
    private static final int WINDOW = 160, MIN_AGE = 60;

    private static Snap snap(long tick, double x, double y, double z, String dim) { return new Snap(tick, x, y, z, 20f, dim, false, Set.of()); }

    private static SafeSpot.Probe world(int... xyz) {
        Set<String> ok = new HashSet<>();
        for (int i = 0; i + 2 < xyz.length; i += 3) ok.add(xyz[i] + "," + xyz[i + 1] + "," + xyz[i + 2]);
        return (x, y, z) -> ok.contains(x + "," + y + "," + z);
    }

    private static RewindBuffer of(Snap... snaps) {
        RewindBuffer b = new RewindBuffer(10);
        for (Snap s : snaps) b.push(s);
        return b;
    }

    @Test
    void itGoesToTheOldestSafeMomentInTheWindow() {
        RewindBuffer b = of(snap(1000, 10.5, 64, 10.5, OVERWORLD), snap(1040, 20.5, 64, 10.5, OVERWORLD), snap(1100, 30.5, 64, 10.5, OVERWORLD));
        RewindPlanner.Plan p = RewindPlanner.choose(b, 1150, WINDOW, MIN_AGE, OVERWORLD, world(10, 64, 10, 20, 64, 10, 30, 64, 10), 3.0);
        assertNotNull(p);
        assertEquals(1000L, p.snap().tick(), "the oldest, 150 ticks ago");
        assertEquals(new SafeSpot.Spot(10, 64, 10), p.spot());
        assertTrue(p.exact());
    }

    @Test
    void aMomentTooYoungOrTooOldIsIgnored() {
        RewindBuffer b = of(snap(800, 1.5, 64, 1.5, OVERWORLD), snap(1080, 2.5, 64, 2.5, OVERWORLD), snap(1130, 3.5, 64, 3.5, OVERWORLD));
        SafeSpot.Probe all = (x, y, z) -> true;
        RewindPlanner.Plan p = RewindPlanner.choose(b, 1150, WINDOW, MIN_AGE, OVERWORLD, all, 3.0);
        assertNotNull(p);
        assertEquals(1080L, p.snap().tick(), "800 is 350 ticks old (too old), 1130 is 20 ticks old (too young), 1080 is 70 ticks old");
        assertNull(RewindPlanner.choose(of(snap(1130, 3.5, 64, 3.5, OVERWORLD)), 1150, WINDOW, MIN_AGE, OVERWORLD, all, 3.0), "only a too-young moment: nothing");
        assertNull(RewindPlanner.choose(of(snap(100, 3.5, 64, 3.5, OVERWORLD)), 1150, WINDOW, MIN_AGE, OVERWORLD, all, 3.0), "only a too-old moment: nothing");
    }

    @Test
    void theEdgesOfTheWindowAreInclusive() {
        SafeSpot.Probe all = (x, y, z) -> true;
        assertNotNull(RewindPlanner.choose(of(snap(1000, 0.5, 64, 0.5, OVERWORLD)), 1160, WINDOW, MIN_AGE, OVERWORLD, all, 3.0), "exactly 160 ticks old");
        assertNull(RewindPlanner.choose(of(snap(1000, 0.5, 64, 0.5, OVERWORLD)), 1161, WINDOW, MIN_AGE, OVERWORLD, all, 3.0), "161 ticks old");
        assertNotNull(RewindPlanner.choose(of(snap(1000, 0.5, 64, 0.5, OVERWORLD)), 1060, WINDOW, MIN_AGE, OVERWORLD, all, 3.0), "exactly 60 ticks old");
        assertNull(RewindPlanner.choose(of(snap(1000, 0.5, 64, 0.5, OVERWORLD)), 1059, WINDOW, MIN_AGE, OVERWORLD, all, 3.0), "59 ticks old");
    }

    @Test
    void thePastIsThereOnlyWhenSomeMomentIsOldEnoughAndYoungEnough() {
        assertFalse(RewindPlanner.hasPast(new RewindBuffer(10), 1150, WINDOW, MIN_AGE), "an empty past");
        assertFalse(RewindPlanner.hasPast(of(snap(1100, 0.5, 64, 0.5, OVERWORLD)), 1150, WINDOW, MIN_AGE), "50 ticks old is too young");
        assertTrue(RewindPlanner.hasPast(of(snap(1090, 0.5, 64, 0.5, OVERWORLD)), 1150, WINDOW, MIN_AGE), "exactly 60 ticks old");
        assertTrue(RewindPlanner.hasPast(of(snap(990, 0.5, 64, 0.5, OVERWORLD)), 1150, WINDOW, MIN_AGE), "exactly 160 ticks old");
        assertFalse(RewindPlanner.hasPast(of(snap(989, 0.5, 64, 0.5, OVERWORLD)), 1150, WINDOW, MIN_AGE), "161 ticks old is too old");
    }

    @Test
    void anotherDimensionIsNeverUsed() {
        RewindBuffer b = of(snap(1000, 5.5, 64, 5.5, NETHER), snap(1040, 6.5, 64, 6.5, OVERWORLD));
        RewindPlanner.Plan p = RewindPlanner.choose(b, 1150, WINDOW, MIN_AGE, OVERWORLD, (x, y, z) -> true, 3.0);
        assertNotNull(p);
        assertEquals(1040L, p.snap().tick());
        assertNull(RewindPlanner.choose(of(snap(1000, 5.5, 64, 5.5, NETHER)), 1150, WINDOW, MIN_AGE, OVERWORLD, (x, y, z) -> true, 3.0));
    }

    @Test
    void aBlockedPlaceFallsBackToTheNearestSafeOneWithinThreeBlocks() {
        RewindBuffer b = of(snap(1000, 10.5, 64, 10.5, OVERWORLD));
        RewindPlanner.Plan p = RewindPlanner.choose(b, 1150, WINDOW, MIN_AGE, OVERWORLD, world(12, 64, 10), 3.0);
        assertNotNull(p);
        assertEquals(new SafeSpot.Spot(12, 64, 10), p.spot());
        assertFalse(p.exact());
        assertNull(RewindPlanner.choose(b, 1150, WINDOW, MIN_AGE, OVERWORLD, world(14, 64, 10), 3.0), "four blocks away is too far");
    }

    @Test
    void aMomentInMidAirIsSkippedForALaterOneOnTheGround() {
        RewindBuffer b = of(snap(1000, 10.5, 90, 10.5, OVERWORLD), snap(1060, 11.5, 64, 10.5, OVERWORLD));
        RewindPlanner.Plan p = RewindPlanner.choose(b, 1150, WINDOW, MIN_AGE, OVERWORLD, world(11, 64, 10), 3.0);
        assertNotNull(p);
        assertEquals(1060L, p.snap().tick(), "the high moment has no safe place near it, the next one does");
    }

    @Test
    void nothingSafeAnywhereRefuses() {
        RewindBuffer b = of(snap(1000, 10.5, 64, 10.5, OVERWORLD), snap(1060, 11.5, 64, 10.5, OVERWORLD));
        assertNull(RewindPlanner.choose(b, 1150, WINDOW, MIN_AGE, OVERWORLD, (x, y, z) -> false, 3.0));
        assertNull(RewindPlanner.choose(new RewindBuffer(10), 1150, WINDOW, MIN_AGE, OVERWORLD, (x, y, z) -> true, 3.0), "an empty past");
    }

    @Test
    void negativeCoordinatesRoundDownNotTowardsZero() {
        RewindBuffer b = of(snap(1000, -0.5, 64, -3.5, OVERWORLD));
        RewindPlanner.Plan p = RewindPlanner.choose(b, 1150, WINDOW, MIN_AGE, OVERWORLD, world(-1, 64, -4), 0.0);
        assertNotNull(p, "block (-1, 64, -4) is where (-0.5, 64, -3.5) stands");
        assertTrue(p.exact());
    }

    @Test
    void theHealIsWhatYouHadMinusWhatYouHaveCappedAtAQuarter() {
        assertEquals(5f, RewindPlanner.healBack(10f, 15f, 20f, 0.25), 1e-6f, "5 hearts' worth is exactly the quarter of 20");
        assertEquals(5f, RewindPlanner.healBack(2f, 20f, 20f, 0.25), 1e-6f, "wants 18, capped at a quarter of 20");
        assertEquals(0f, RewindPlanner.healBack(18f, 12f, 20f, 0.25), 1e-6f, "never takes health away");
        assertEquals(0f, RewindPlanner.healBack(10f, 10f, 20f, 0.25), 1e-6f);
        assertEquals(2f, RewindPlanner.healBack(10f, 12f, 20f, 0.25), 1e-6f);
        assertEquals(0f, RewindPlanner.healBack(1f, 20f, 0f, 0.25), 1e-6f, "no maximum, no heal");
        assertEquals(0f, RewindPlanner.healBack(Float.NaN, 20f, 20f, 0.25), 1e-6f);
        assertEquals(0f, RewindPlanner.healBack(1f, 20f, 20f, -1.0), 1e-6f, "a negative cap is zero");
    }
}
