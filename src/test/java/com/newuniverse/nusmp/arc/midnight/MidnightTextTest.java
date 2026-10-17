package com.newuniverse.nusmp.arc.midnight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The rewriting of the framework's devil-voiced lines, the bar title and the gauge. */
class MidnightTextTest {

    @Test
    void theGatesLineIsReplacedWordForWord() {
        assertEquals("The Third Eye opens!", MidnightText.rewrite(MidnightText.QLIPHOTH_GATES, "Lilith", "The Third Eye opens!"));
        assertEquals("The Gates of the Tree of Qliphoth open!", MidnightText.QLIPHOTH_GATES, "the key must match the framework's text exactly");
        assertEquals(MidnightText.QLIPHOTH_GATES, MidnightText.rewrite(MidnightText.QLIPHOTH_GATES, "Lilith", null), "no replacement given: leave it");
    }

    @Test
    void theRageLineLosesItsDevil() {
        String framework = "Dante has fallen. Fana rages: 40% -> 50% of Lilith's power.";
        assertEquals("Dante has fallen. Fana rages: 40% -> 50% of their strength.", MidnightText.rewrite(framework, "Lilith", "x"));
        assertEquals("Vetto has fallen. Fana is already at full fury.", MidnightText.rewrite("Vetto has fallen. Fana is already at full fury.", "Lilith", "x"));
        assertEquals("anything else", MidnightText.rewrite("anything else", "Lilith", "x"));
        assertEquals("", MidnightText.rewrite(null, "Lilith", "x"));
        assertEquals(framework, MidnightText.rewrite(framework, "", "x"), "no devil name: nothing to remove");
    }

    @Test
    void theBarTitleNamesTheMagicNotADevil() {
        assertEquals("Patolli - Light 60%", MidnightText.barTitle("Patolli", "Light", 60, false, "Dark Elf"));
        assertEquals("Patolli - Light 100% - Dark Elf", MidnightText.barTitle("Patolli", "Light", 100, true, "Dark Elf"));
        assertEquals("Fana - Salamander 100%", MidnightText.barTitle("Fana", "Salamander", 100, true, ""), "no gates word: no suffix");
        assertEquals("Fana - Salamander 100%", MidnightText.barTitle("Fana", "Salamander", 100, true, null));
    }

    @Test
    void theGaugeFillsLeftToRight() {
        assertEquals("[....]", MidnightText.gauge(0f, 4));
        assertEquals("[||..]", MidnightText.gauge(0.5f, 4));
        assertEquals("[||||]", MidnightText.gauge(1f, 4));
        assertEquals("[||||]", MidnightText.gauge(7f, 4), "clamped above");
        assertEquals("[....]", MidnightText.gauge(-3f, 4), "clamped below");
        assertEquals("[|]", MidnightText.gauge(1f, 0), "a gauge is never empty of cells");
    }

    @Test
    void secondsAreReadable() {
        assertEquals("7.5", MidnightText.seconds(150));
        assertEquals("0.0", MidnightText.seconds(0));
        assertEquals("0.0", MidnightText.seconds(-4));
        assertEquals("3.0", MidnightText.seconds(60));
    }
}
