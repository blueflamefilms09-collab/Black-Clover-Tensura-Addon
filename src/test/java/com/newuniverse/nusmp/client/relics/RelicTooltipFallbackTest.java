package com.newuniverse.nusmp.client.relics;

import com.newuniverse.nusmp.client.relics.RelicTooltipLayout.Para;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** A relic tooltip is never empty and never throws, whatever state the stack's data is in. */
class RelicTooltipFallbackTest {
    @Test
    void fullLinesWinWhenTheyBuild() {
        List<Para> full = List.of(new Para("a", 0), new Para("b", 0));
        assertSame(full, RelicTooltipFallback.safe(() -> full, () -> List.of(new Para("x", 1))));
    }

    @Test
    void aThrowingBuildFallsBackToThePlainLines() {
        List<Para> out = RelicTooltipFallback.safe(() -> { throw new IllegalStateException("registry not bound"); }, () -> RelicTooltipFallback.lines("★★★", "Rare", "Neck", true));
        assertEquals(2, out.size());
        assertEquals("★★★  Rare", out.get(0).text());
        assertEquals("Slot: Neck (Curios)", out.get(1).text());
        assertEquals(RelicTooltipFallback.TAG_GRADE, out.get(0).tag());
    }

    @Test
    void anEmptyBuildAlsoFallsBack() {
        assertEquals(2, RelicTooltipFallback.safe(List::of, () -> RelicTooltipFallback.lines("a", "b", "c", false)).size());
        assertEquals(2, RelicTooltipFallback.safe(() -> null, () -> RelicTooltipFallback.lines("a", "b", "c", false)).size());
    }

    @Test
    void evenAFailingFallbackLeavesOneLine() {
        List<Para> out = RelicTooltipFallback.safe(() -> { throw new NullPointerException(); }, () -> { throw new NullPointerException(); });
        assertEquals(1, out.size());
        assertFalse(out.get(0).text().isEmpty());
    }

    @Test
    void nullPartsDoNotThrow() {
        List<Para> out = RelicTooltipFallback.lines(null, null, null, false);
        assertEquals(2, out.size());
        assertEquals("Slot: ?", out.get(1).text());
    }
}
