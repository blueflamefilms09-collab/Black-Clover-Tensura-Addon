package com.newuniverse.nusmp.client.skilltree.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Why: the small pure rules around the screen each prevent one specific bad experience, and each has an edge that a human would not think to try: the countdown that must never read
 * "0s" on a disabled button, the gate that must let exactly one purchase through, the double click that must not pair two different nodes, the number that must not print
 * ".0", the family heading that must keep first-seen order. One test per rule, edges included.
 */
class SmallRulesTest {

    // ---------------------------------------------------------------- the countdown
    @Test
    void secondsRoundUpSoTheButtonNeverShowsZeroWhileDisabled() {
        assertEquals(0, TreeText.secondsUp(0));
        assertEquals(1, TreeText.secondsUp(1));
        assertEquals(1, TreeText.secondsUp(20));
        assertEquals(2, TreeText.secondsUp(21));
        assertEquals(30, TreeText.secondsUp(600));
        assertEquals(0, TreeText.secondsUp(-40));
    }

    // ---------------------------------------------------------------- PurchaseGate
    @Test
    void gateLetsExactlyOneRequestThroughUntilTheLevelRises() {
        PurchaseGate g = new PurchaseGate();
        assertTrue(g.tryBegin(1000, 2));
        assertFalse(g.tryBegin(1100, 2), "a second click while waiting is refused");
        assertTrue(g.isPending(1100));
        assertFalse(g.observe(2), "still the old level");
        assertTrue(g.observe(3), "the level rose: the purchase went through, reported once");
        assertFalse(g.observe(3), "reported only once");
        assertFalse(g.isPending(1200));
        assertTrue(g.tryBegin(1200, 3), "free again");
    }

    @Test
    void gateReportsARefusalAfterTheTimeoutExactlyOnce() {
        PurchaseGate g = new PurchaseGate();
        assertTrue(g.tryBegin(0, 0));
        assertFalse(g.refused(PurchaseGate.TIMEOUT_MS - 1));
        assertTrue(g.refused(PurchaseGate.TIMEOUT_MS));
        assertFalse(g.refused(PurchaseGate.TIMEOUT_MS + 5), "once");
        assertTrue(g.tryBegin(PurchaseGate.TIMEOUT_MS + 6, 0));
    }

    @Test
    void gateIsFreeAgainAfterTheTimeoutEvenIfNobodyAskedAboutRefusal() {
        PurchaseGate g = new PurchaseGate();
        assertTrue(g.tryBegin(0, 0));
        assertTrue(g.tryBegin(PurchaseGate.TIMEOUT_MS + 1, 0));
    }

    @Test
    void gateResetForgetsTheRequest() {
        PurchaseGate g = new PurchaseGate();
        g.tryBegin(0, 0);
        g.reset();
        assertFalse(g.observe(5));
        assertFalse(g.refused(10_000));
        assertTrue(g.tryBegin(1, 0));
    }

    // ---------------------------------------------------------------- ClickTracker
    @Test
    void twoQuickClicksOnTheSameTargetAreADoubleClick() {
        ClickTracker c = new ClickTracker();
        assertFalse(c.click(1000, 4));
        assertTrue(c.click(1000 + ClickTracker.WINDOW_MS, 4), "at the edge of the window still counts");
    }

    @Test
    void slowClicksOrDifferentTargetsAreNot() {
        ClickTracker c = new ClickTracker();
        assertFalse(c.click(1000, 4));
        assertFalse(c.click(1000 + ClickTracker.WINDOW_MS + 1, 4), "too slow");
        assertFalse(c.click(2000, 5));
        assertFalse(c.click(2100, 4), "another node in between");
        assertFalse(c.click(3000, -1));
        assertFalse(c.click(3010, -1), "clicks on nothing never pair up");
    }

    @Test
    void aDoubleClickIsConsumedSoATripleIsOneDoublePlusOne() {
        ClickTracker c = new ClickTracker();
        assertFalse(c.click(100, 2));
        assertTrue(c.click(150, 2));
        assertFalse(c.click(200, 2), "the third click starts a new pair");
        assertTrue(c.click(250, 2));
    }

    // ---------------------------------------------------------------- TreeText
    @Test
    void prettifyMakesTitleCaseFromIds() {
        assertEquals("Fire Dragon Slayer", TreeText.prettify("fire_dragon_slayer"));
        assertEquals("Fairy Tail", TreeText.prettify("fairy_tail"));
        assertEquals("Fire Dragon Slayer", TreeText.prettify("nusmp:fire_dragon_slayer"), "a namespace is dropped");
        assertEquals("Ice", TreeText.prettify("__ice__"));
        assertEquals("", TreeText.prettify(""));
        assertEquals("", TreeText.prettify(null));
    }

    @Test
    void numbersDropMeaninglessDecimals() {
        assertEquals("6", TreeText.number(6.0));
        assertEquals("0", TreeText.number(0.0));
        assertEquals("-3", TreeText.number(-3.0));
        assertEquals("7.5", TreeText.number(7.5));
        assertEquals("0.33", TreeText.number(1.0 / 3.0));
        assertEquals("2", TreeText.number(2.0000000001));
        assertEquals("0.1", TreeText.number(0.10));
        assertEquals("1.25", TreeText.number(1.25));
        assertEquals("?", TreeText.number(Double.NaN));
        assertEquals("?", TreeText.number(Double.POSITIVE_INFINITY));
        assertEquals("12000", TreeText.number(12000));
    }

    // ---------------------------------------------------------------- RouteGrouping
    @Test
    void familiesKeepFirstSeenOrderAndRoutesKeepRegistryOrder() {
        SkillRoute a = route("nusmp:a", "fairy_tail"), b = route("nusmp:b", "other_anime"), c = route("nusmp:c", "fairy_tail"), d = route("nusmp:d", null);
        List<RouteGrouping.Group> groups = RouteGrouping.group(List.of(a, b, c, d));
        assertEquals(3, groups.size());
        assertEquals("fairy_tail", groups.get(0).magic());
        assertEquals(List.of(a, c), groups.get(0).routes());
        assertEquals("other_anime", groups.get(1).magic());
        assertEquals("", groups.get(2).magic(), "a route with no family goes under the empty heading");
        assertEquals(List.of(d), groups.get(2).routes());
    }

    @Test
    void noRoutesNoGroups() { assertTrue(RouteGrouping.group(List.of()).isEmpty()); }

    private static SkillRoute route(String id, String magic) {
        return new SkillRoute(id, magic, id, "", id + "_book", "Book", "", 0, "minecraft:book", id.substring(id.indexOf(':') + 1), 3, 1, List.of());
    }
}
