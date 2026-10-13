package com.newuniverse.nusmp.dungeon.nexus.collapse;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The words of the Treasury Collapse: the owner's broadcast, the Dungeon that speaks, the clock, the compass. */
class CollapseTextTest {
    @Test
    void theBroadcastIsTheOwnersWording() {
        assertEquals("[Server] The Dungeon wails in agony! The floor is collapsing!", CollapseText.BROADCAST);
        assertTrue(CollapseText.BROADCAST.startsWith("[Server] "));
    }

    /** Every line a player can read in the course of a collapse. */
    private static List<String> everyLine() {
        List<String> out = new ArrayList<>(List.of(CollapseText.BROADCAST, CollapseText.TITLE, CollapseText.TITLE_SUB, CollapseText.GASP, CollapseText.EXPEL_TITLE,
                CollapseText.EXPEL_SUB, CollapseText.EXPELLED, CollapseText.OPENED, CollapseText.NO_EXIT, CollapseText.GUARDS, CollapseText.WARN, CollapseText.SURGE,
                CollapseText.SURGE_LAND, CollapseText.SURGE_HELD, CollapseText.SURGE_CLOSED, CollapseText.SURGE_FIZZLED));
        for (int i = 0; i < CollapsePlan.MARKS.length; i++) out.add(CollapseText.mark(i));
        for (long left : new long[]{3600, 2400, 1200, 600, 200, 20}) {
            out.add(CollapseText.bar(left, Double.NaN, ""));
            out.add(CollapseText.bar(left, 31.4, "NE"));
        }
        return out;
    }

    @Test
    void itIsTheDungeonThatCollapsesAndNoLineSaysNexus() {
        for (String line : everyLine()) {
            assertFalse(line.isEmpty(), "no empty line");
            assertFalse(line.toLowerCase(java.util.Locale.ROOT).contains("nexus"), "the Dungeon speaks of itself, not of a Nexus: " + line);
            assertFalse(line.toLowerCase(java.util.Locale.ROOT).contains("mana zone"), "the old broadcast is gone: " + line);
            assertFalse(line.toLowerCase(java.util.Locale.ROOT).contains("mutated"), "the guards are frenzied now: " + line);
        }
    }

    @Test
    void theOtherLinesAreTheNewWordsOfADungeonThatIsComingDown() {
        assertEquals("THE DUNGEON IS COLLAPSING", CollapseText.TITLE);
        assertEquals("3:00 to reach a Root Gate", CollapseText.TITLE_SUB);
        assertTrue(CollapseText.EXPELLED.startsWith("The Dungeon is done with you"));
        assertEquals("THE DUNGEON IS DONE WITH YOU", CollapseText.EXPEL_TITLE);
        assertEquals("Something frenzied stirs in the walls.", CollapseText.GUARDS);
        assertTrue(CollapseText.OPENED.startsWith("The Dungeon has opened a Root Gate"));
        assertTrue(CollapseText.OPENED.contains("Reach one"));
        assertTrue(CollapseText.WARN.contains("ceiling") && CollapseText.WARN.endsWith("move!"));
        assertEquals("Space tears open under you!", CollapseText.SURGE, "the design's line for the spatial tear");
        assertTrue(CollapseText.mark(0).startsWith("The Dungeon is coming down."));
        assertTrue(CollapseText.GASP.contains("Chalice"), "the line about the air is kept");
        assertTrue(CollapseText.NO_EXIT.contains("Root Gate"));
    }

    @Test
    void theClockRoundsUpSoTheLastSecondShowsUntilTheEnd() {
        assertEquals("3:00", CollapseText.clock(3600));
        assertEquals("3:00", CollapseText.clock(3599), "the first second is still 3:00");
        assertEquals("3:00", CollapseText.clock(3581));
        assertEquals("2:59", CollapseText.clock(3580));
        assertEquals("2:00", CollapseText.clock(2400));
        assertEquals("1:05", CollapseText.clock(1300));
        assertEquals("0:30", CollapseText.clock(600));
        assertEquals("0:10", CollapseText.clock(200));
        assertEquals("0:01", CollapseText.clock(20));
        assertEquals("0:01", CollapseText.clock(1), "a single tick left still reads 0:01");
        assertEquals("0:00", CollapseText.clock(0));
        assertEquals("0:00", CollapseText.clock(-40), "never negative");
    }

    @Test
    void everyMarkHasItsLineAndTheLastFiveAreJustNumbers() {
        for (int i = 0; i < CollapsePlan.MARKS.length; i++) assertFalse(CollapseText.mark(i).isEmpty(), "mark " + i);
        assertEquals("", CollapseText.mark(-1));
        assertEquals("", CollapseText.mark(CollapsePlan.MARKS.length));
        assertTrue(CollapseText.mark(0).contains("3:00"));
        assertTrue(CollapseText.mark(1).contains("2:00"));
        assertTrue(CollapseText.mark(2).contains("1:00"));
        assertTrue(CollapseText.mark(3).contains("0:30"));
        assertTrue(CollapseText.mark(4).contains("0:10"));
        assertEquals("5", CollapseText.mark(5));
        assertEquals("4", CollapseText.mark(6));
        assertEquals("3", CollapseText.mark(7));
        assertEquals("2", CollapseText.mark(8));
        assertEquals("1", CollapseText.mark(9));
    }

    @Test
    void theCompassWordsAreRightForTheWorldsAxes() {
        assertEquals("N", CollapseText.direction(0, -10));
        assertEquals("NE", CollapseText.direction(10, -10));
        assertEquals("E", CollapseText.direction(10, 0));
        assertEquals("SE", CollapseText.direction(10, 10));
        assertEquals("S", CollapseText.direction(0, 10));
        assertEquals("SW", CollapseText.direction(-10, 10));
        assertEquals("W", CollapseText.direction(-10, 0));
        assertEquals("NW", CollapseText.direction(-10, -10));
        assertEquals("here", CollapseText.direction(0.2, -0.3));
        assertEquals("N", CollapseText.direction(1, -30), "a little east of north is still north");
        assertEquals("E", CollapseText.direction(30, 5));
        assertEquals("N", CollapseText.direction(-1, -30));
        for (double a = 0; a < 360; a += 3) {
            String w = CollapseText.direction(Math.sin(Math.toRadians(a)) * 20, -Math.cos(Math.toRadians(a)) * 20);
            assertTrue(java.util.List.of("N", "NE", "E", "SE", "S", "SW", "W", "NW").contains(w), w + " at " + a);
        }
    }

    @Test
    void theBarShowsTheClockAndTheWayToTheNearestGate() {
        assertEquals("Collapse 2:30", CollapseText.bar(3000, Double.NaN, "N"));
        assertEquals("Collapse 0:20 | Root Gate 37 m NE", CollapseText.bar(400, 37.2, "NE"));
        assertEquals("Collapse 0:20 | Root Gate 0 m here", CollapseText.bar(400, 0.0, "here"));
        assertEquals("Collapse 2:41 | Root Gate 31 m NE", CollapseText.bar(3220, 31.0, "NE"), "the example line of the brief");
    }

    @Test
    void theStatusLineIsClampedAndReadable() {
        assertEquals("rolling, 1:30 left, 40% of the swept ground down", CollapseText.status(CollapsePlan.Stage.ROLLING, 1800, 40));
        assertEquals("cracks, 3:00 left, 0% of the swept ground down", CollapseText.status(CollapsePlan.Stage.CRACKS, 3600, -5));
        assertEquals("wipe, 0:00 left, 100% of the swept ground down", CollapseText.status(CollapsePlan.Stage.WIPE, 0, 400));
    }
}
