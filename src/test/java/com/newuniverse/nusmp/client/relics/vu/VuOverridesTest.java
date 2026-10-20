package com.newuniverse.nusmp.client.relics.vu;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The override table parser: hand and worn lines, options after the bar, comments, bad lines are reported and the fallback answers nothing. */
class VuOverridesTest {
    @Test
    void parsesHandAndWornLinesWithOptionsAndComments() {
        VuOverrides o = VuOverrides.parse(List.of("# a comment", "", "hand a vu/a | size=0.62 style=palm", "worn a vu/a_worn", "hand b vu/b@larm~ vu/c | size=1.0", "worn b vu/b@larm~ # trailing"));
        assertEquals(2, o.handCount());
        assertEquals(2, o.wornCount());
        assertEquals(List.of("vu/a"), o.hand("a").pieces());
        assertEquals(0.62f, o.hand("a").number("size", 0f), 1e-6f);
        assertEquals("palm", o.hand("a").options().get("style"));
        assertEquals(List.of("vu/b@larm~", "vu/c"), o.hand("b").pieces());
        assertEquals(List.of("vu/b@larm~"), o.worn("b").pieces());
        assertEquals(0.9f, o.worn("a").number("size", 0.9f), 1e-6f, "an option that is not there gives the fallback");
        assertTrue(o.problems().isEmpty(), o.problems().toString());
    }

    @Test
    void reportsMalformedLinesAndKeepsTheGoodOnes() {
        VuOverrides o = VuOverrides.parse(List.of("hand a vu/a | size", "nonsense", "hand b", "worn c vu/c"));
        assertEquals(3, o.problems().size());
        assertNotNull(o.hand("a"), "the line with a bad option keeps its pieces");
        assertNotNull(o.worn("c"));
        assertNull(o.hand("b"));
    }

    @Test
    void theFallbackAnswersNothingWhileTheUpgradeIsOff() {
        VuOverrides o = VuOverrides.parse(List.of("hand a vu/a", "worn a vu/a_worn"));
        assertNull(o.handIf(false, "a"));
        assertNull(o.wornIf(false, "a"));
        assertNotNull(o.handIf(true, "a"));
        assertNotNull(o.wornIf(true, "a"));
        assertNull(o.handIf(true, "nobody"));
    }
}
