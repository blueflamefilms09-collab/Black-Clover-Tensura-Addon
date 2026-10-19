package com.newuniverse.nusmp.labyrinth.mega.system;

import com.newuniverse.nusmp.labyrinth.mega.enc.DepthDropRules;
import com.newuniverse.nusmp.labyrinth.mega.enc.DepthRules;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: between "the game fired a floor event" and "a window appears" the System makes a few decisions, and each is a line here: which words describe the move (UP or DOWN,
 * arrived, going on, going back) on both roads, how strong the monsters of a depth are (the same numbers the Dungeon Master's depth rules give them), when a report may be
 * shown again (no spam), and what counts as news in the mechanics around a player.
 */
class FloorInfoRulesTest {
    private static final int[] MIND = DepthRules.parseThresholds(DepthRules.DEFAULT_MIND_FLOORS);

    // ---------------------------------------------------------------- the move
    @Test void aFirstLookIsAnArrivalAndAMoveToADeeperFloorIsGoingOn() {
        assertEquals(FloorInfoRules.Reason.ARRIVE, FloorInfoRules.reasonFor(0, 5));
        assertEquals(FloorInfoRules.Reason.ARRIVE, FloorInfoRules.reasonFor(0, -5));
        assertEquals(FloorInfoRules.Reason.FORWARD, FloorInfoRules.reasonFor(3, 4));
        assertEquals(FloorInfoRules.Reason.FORWARD, FloorInfoRules.reasonFor(-3, -4));
        assertEquals(FloorInfoRules.Reason.BACK, FloorInfoRules.reasonFor(4, 3));
        assertEquals(FloorInfoRules.Reason.BACK, FloorInfoRules.reasonFor(-4, -3));
        assertEquals(FloorInfoRules.Reason.ARRIVE, FloorInfoRules.reasonFor(3, -3), "crossing from one road to the other is not a move along a road");
    }

    @Test void directionIsReadFromTheSignedFloorOnBothRoads() {
        assertTrue(FloorInfoRules.up(3, 4), "+3 to +4 climbs");
        assertFalse(FloorInfoRules.up(4, 3), "+4 to +3 goes down");
        assertFalse(FloorInfoRules.up(-3, -4), "-3 to -4 goes down");
        assertTrue(FloorInfoRules.up(-4, -3), "-4 to -3 goes up");
        assertTrue(FloorInfoRules.up(0, 1), "from the Hub to +1 is up");
        assertFalse(FloorInfoRules.up(0, -1), "from the Hub to -1 is down");
        assertTrue(FloorInfoRules.up(0, 40), "no previous floor: the Holy road climbs");
        assertFalse(FloorInfoRules.up(0, -40), "no previous floor: the Abyssal road descends");
    }

    @Test void reasonCodesRoundTripAndAnUnknownCodeIsAnArrival() {
        for (FloorInfoRules.Reason r : FloorInfoRules.Reason.values()) assertEquals(r, FloorInfoRules.Reason.fromCode(r.code()));
        assertEquals(FloorInfoRules.Reason.ARRIVE, FloorInfoRules.Reason.fromCode(99));
    }

    // ---------------------------------------------------------------- the depth
    @Test void theFirstFloorIsFlatAndTheNumbersGrowWithDepth() {
        FloorInfoRules.Depth one = FloorInfoRules.depth(1, MIND, DepthDropRules.DEFAULTS, 1.0);
        assertTrue(one.flat(), one.toString());
        assertEquals(1, one.depth());
        FloorInfoRules.Depth ten = FloorInfoRules.depth(10, MIND, DepthDropRules.DEFAULTS, 1.0);
        assertEquals(12, ten.healthPct());
        assertEquals(7, ten.damagePct());
        assertEquals(1, ten.mind());
        FloorInfoRules.Depth hundred = FloorInfoRules.depth(100, MIND, DepthDropRules.DEFAULTS, 1.0);
        assertEquals(99, hundred.healthPct());
        assertEquals(62, hundred.damagePct());
        assertEquals(4, hundred.mind());
        assertEquals(174, hundred.xpTimes100());
        int last = -1;
        for (int d = 1; d <= 1000; d += 7) {
            FloorInfoRules.Depth x = FloorInfoRules.depth(d, MIND, DepthDropRules.DEFAULTS, 1.0);
            assertTrue(x.healthPct() >= last, "never weaker with depth");
            last = x.healthPct();
        }
    }

    @Test void plusAndMinusAreEquallyDeepAndTheScaleIsHonoured() {
        assertEquals(FloorInfoRules.depth(37, MIND, DepthDropRules.DEFAULTS, 1.0), FloorInfoRules.depth(-37, MIND, DepthDropRules.DEFAULTS, 1.0));
        FloorInfoRules.Depth off = FloorInfoRules.depth(100, MIND, DepthDropRules.DEFAULTS, 0.0);
        assertEquals(0, off.healthPct());
        assertEquals(0, off.damagePct());
        FloorInfoRules.Depth none = FloorInfoRules.depth(100, null, null, 1.0);
        assertEquals(99, none.healthPct(), "null thresholds and drops use the defaults");
    }

    @Test void multipliersAreWrittenWithoutPointlessZeros() {
        assertEquals("1.09", FloorInfoRules.times(109));
        assertEquals("1.2", FloorInfoRules.times(120));
        assertEquals("1", FloorInfoRules.times(100));
        assertEquals("0.9", FloorInfoRules.dangerText(0.9));
        assertEquals("1.5", FloorInfoRules.dangerText(1.5));
        assertEquals("2", FloorInfoRules.dangerText(2.0));
    }

    // ---------------------------------------------------------------- the words of a theme
    @Test void aThemeWithNoDescriptionGetsALineFromItsData() {
        assertTrue(FloorInfoRules.genericThemeDesc("flood").contains("flood"));
        assertFalse(FloorInfoRules.genericThemeDesc("").isBlank());
        assertFalse(FloorInfoRules.genericThemeDesc(null).isBlank());
        assertEquals("spirit", FloorInfoRules.favouredCatastrophe(Map.of("collapse", 2, "flood", 1, "spirit", 4)));
        assertEquals("collapse", FloorInfoRules.favouredCatastrophe(Map.of("collapse", 3, "flood", 3)), "a tie goes to the first by name");
        assertEquals("", FloorInfoRules.favouredCatastrophe(Map.of("flood", 0)));
        assertEquals("", FloorInfoRules.favouredCatastrophe(null));
    }

    @Test void idsBecomeReadableNames() {
        assertEquals("Feywild Grove", FloorInfoRules.humanize("feywild_grove"));
        assertEquals("Hazard Spore Cloud E Poison", FloorInfoRules.humanize("hazard.spore_cloud.e_poison"));
        assertEquals("", FloorInfoRules.humanize(""));
        assertEquals("", FloorInfoRules.humanize(null));
    }

    @Test void alignmentCodesNameTheRoadAndWhetherItIsExclusive() {
        assertEquals("holy", FloorInfoRules.alignCode("HOLY", false));
        assertEquals("holy_only", FloorInfoRules.alignCode("HOLY", true));
        assertEquals("abyssal_only", FloorInfoRules.alignCode("abyssal", true));
        assertEquals("any", FloorInfoRules.alignCode("ANY", true));
        assertEquals("any", FloorInfoRules.alignCode(null, false));
    }

    @Test void clippingKeepsTheWireLimit() {
        assertEquals("abc", FloorInfoRules.clip("abc", 10));
        assertEquals(10, FloorInfoRules.clip("x".repeat(100), 10).length());
        assertTrue(FloorInfoRules.clip("x".repeat(100), 10).endsWith("..."));
        assertEquals("", FloorInfoRules.clip(null, 10));
        assertEquals(2, FloorInfoRules.clip("abcdef", 2).length());
    }

    // ---------------------------------------------------------------- no spam
    @Test void anArrivalIsShownOncePerFloorChangeAndTheSameFloorOnlyAfterTheCooldown() {
        FloorInfoRules.Gap gap = FloorInfoRules.Gap.DEFAULT;
        FloorInfoRules.Throttle t = new FloorInfoRules.Throttle();
        assertTrue(t.offer(FloorInfoRules.Reason.ARRIVE, 3, 0, gap));
        assertFalse(t.offer(FloorInfoRules.Reason.ARRIVE, 3, 100, gap), "the same floor again, a staircase walked up and down");
        assertTrue(t.offer(FloorInfoRules.Reason.FORWARD, 4, 100, gap), "a different floor always shows");
        assertTrue(t.offer(FloorInfoRules.Reason.BACK, 3, 110, gap), "and so does going back, it is a different floor");
        assertFalse(t.offer(FloorInfoRules.Reason.FORWARD, 3, 120, gap));
        assertTrue(t.offer(FloorInfoRules.Reason.ARRIVE, 3, 110 + gap.arriveCooldown(), gap), "after the cooldown the same floor shows again");
    }

    @Test void aRequestIsAnsweredUnlessItFollowsAnotherAtOnce() {
        FloorInfoRules.Gap gap = FloorInfoRules.Gap.DEFAULT;
        FloorInfoRules.Throttle t = new FloorInfoRules.Throttle();
        assertTrue(t.offer(FloorInfoRules.Reason.REQUEST, 3, 1000, gap));
        assertFalse(t.offer(FloorInfoRules.Reason.REQUEST, 3, 1005, gap));
        assertTrue(t.offer(FloorInfoRules.Reason.REQUEST, 3, 1010, gap));
        assertEquals(1010, t.shownAt());
        assertEquals(3, t.shownFloor());
    }

    @Test void anUpdateWaitsForItsCooldownAndForTheArrivalReportToBeReadFirst() {
        FloorInfoRules.Gap gap = FloorInfoRules.Gap.DEFAULT;
        FloorInfoRules.Throttle t = new FloorInfoRules.Throttle();
        assertTrue(t.offer(FloorInfoRules.Reason.ARRIVE, 3, 0, gap));
        assertFalse(t.offer(FloorInfoRules.Reason.UPDATE, 3, 20, gap), "a moment after the arrival report");
        assertFalse(t.offer(FloorInfoRules.Reason.UPDATE, 3, 100, gap), "inside the update cooldown");
        assertTrue(t.offer(FloorInfoRules.Reason.UPDATE, 3, 200, gap));
        assertFalse(t.offer(FloorInfoRules.Reason.UPDATE, 3, 300, gap), "the next one waits its own cooldown");
        assertTrue(t.offer(FloorInfoRules.Reason.UPDATE, 3, 400, gap));
    }

    @Test void aZeroCooldownShowsEveryArrival() {
        FloorInfoRules.Gap none = new FloorInfoRules.Gap(0, 0, 0);
        FloorInfoRules.Throttle t = new FloorInfoRules.Throttle();
        assertTrue(t.offer(FloorInfoRules.Reason.ARRIVE, 3, 5, none));
        assertTrue(t.offer(FloorInfoRules.Reason.ARRIVE, 3, 5, none));
    }

    // ---------------------------------------------------------------- what changed
    @Test void aDiffListsWhatIsNewAndWhatIsGoneInAStableOrder() {
        FloorInfoRules.Diff d = FloorInfoRules.diff(Set.of("a|room|armed", "b|floor|waiting"), Set.of("b|floor|armed", "c|room|armed", "a|room|armed"));
        assertEquals(List.of("b|floor|armed", "c|room|armed"), d.added());
        assertEquals(List.of("b|floor|waiting"), d.removed());
        assertTrue(d.any());
        assertFalse(FloorInfoRules.diff(Set.of("x"), Set.of("x")).any());
        assertEquals(List.of("x"), FloorInfoRules.diff(null, Set.of("x")).added());
        assertEquals(List.of("x"), FloorInfoRules.diff(Set.of("x"), null).removed());
        assertEquals("hazard.y", FloorInfoRules.entryId("hazard.y|room|armed"));
        assertEquals("room|armed", FloorInfoRules.entryState("hazard.y|room|armed"));
        assertEquals("", FloorInfoRules.entryState("plain"));
    }
}
