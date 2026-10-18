package com.newuniverse.nusmp.client.skilltree.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.client.skilltree.layout.ButtonPlacement.Menu;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Why: where the "Skill Tree" button lands in the real menu at real GUI scales is the least testable part of the injection (nothing here runs the game), so it is attacked from three
 * sides. (1) For every window size the game can produce, the button must lie inside the window and touch none of the widgets the menu draws, which are modelled here independently
 * from the menus' source. (2) The fallback must work: when a widget sits where the button wants to be, it moves to a free corner and is still reachable. (3) A source-scan test reads
 * the three menu classes and fails, with an instruction, the day one of their layout constants changes, because the placement mirrors those constants and a stale mirror would
 * otherwise only show as a button in the wrong place on somebody's screen.
 */
class ButtonPlacementTest {
    private static final int LABEL = 45;                                  // "Skill Tree" in the vanilla font is about 45 px wide

    /** The widgets each menu really registers, written from the menus' own source (see {@link #menuSourcesStillMatchTheMirroredConstants}). */
    private static List<Rect> widgetsOf(Menu menu, int sw, int sh) {
        List<Rect> out = new ArrayList<>();
        switch (menu) {
            case CODEX -> {
                int bw = Math.min(sw - 56, 440), bh = Math.min(sh - 48, 260), bx = (sw - bw) / 2 - 10, by = (sh - bh - 20) / 2;
                out.add(new Rect(bx + bw - 94, by + bh + 4, 88, 16));                      // Grimoire Slot
            }
            case FOUR_KINGDOMS -> {
                int left = (sw - 340) / 2, top = (sh - 236) / 2, bookX = left + 170, bookY = top + 62;
                out.add(new Rect(bookX - 138 - 2, bookY + 160 + 4, 20, 14));                // <
                out.add(new Rect(bookX + 138 - 18, bookY + 160 + 4, 20, 14));               // >
                out.add(new Rect(left + 340 - 64, top + 8, 56, 16));                        // Status
            }
            case STATUS -> {
                out.add(new Rect(sw / 2 - 40, sh - 26, 80, 18));                            // Back
                out.add(new Rect(sw - 96, sh - 26, 88, 18));                                // Grimoire Slot
            }
        }
        return out;
    }

    /** Art the menus draw that is not a widget but must stay clear: the Codex's tab strip and book, the status title. */
    private static List<Rect> artOf(Menu menu, int sw, int sh) {
        List<Rect> out = new ArrayList<>();
        switch (menu) {
            case CODEX -> {
                int bw = Math.min(sw - 56, 440), bh = Math.min(sh - 48, 260), bx = (sw - bw) / 2 - 10, by = (sh - bh - 20) / 2;
                out.add(new Rect(bx, by, bw, bh));                                          // the book
                out.add(new Rect(bx + bw - 2, by + 14, 24, Math.max(0, (bh - 18) / 26) * 26)); // the tab strip
            }
            case FOUR_KINGDOMS -> {
                int left = (sw - 340) / 2, top = (sh - 236) / 2;
                out.add(new Rect(left + 8, top + 6, 48, 48));                               // the logo
                out.add(new Rect(left + 62, top + 12, 80, 20));                             // "Four Kingdoms" and "Tensura"
            }
            case STATUS -> out.add(new Rect(sw / 2 - 52, 8, 104, 16));                      // the centred title and its rule
        }
        return out;
    }

    @Test
    void everyGuiSizeKeepsTheButtonInsideTheWindowAndOffEveryWidget() {
        for (Menu menu : Menu.values()) {
            for (int[] s : GuiScales.allSizes()) {
                List<Rect> widgets = widgetsOf(menu, s[0], s[1]);
                Rect r = ButtonPlacement.place(menu, s[0], s[1], LABEL, widgets);
                String at = menu + " at " + s[0] + "x" + s[1] + ": " + r;
                assertEquals(ButtonPlacement.BUTTON_H, r.h(), at);
                assertTrue(r.w() >= ButtonPlacement.MIN_W && r.w() <= ButtonPlacement.MAX_W, at);
                assertTrue(r.isInside(s[0], s[1], ButtonPlacement.EDGE), "inside the window " + at);
                for (Rect w : widgets) assertFalse(r.intersects(w), "touches a widget " + w + " " + at);
            }
        }
    }

    @Test
    void theMirroredSpotIsUsedWhenItIsFreeAndKeepsClearOfTheMenusArt() {
        for (Menu menu : Menu.values()) {
            for (int[] s : GuiScales.allSizes()) {
                Rect r = ButtonPlacement.place(menu, s[0], s[1], LABEL, widgetsOf(menu, s[0], s[1]));
                for (Rect art : artOf(menu, s[0], s[1])) assertFalse(r.intersects(art), "overlaps the menu's art " + art + " in " + menu + " at " + s[0] + "x" + s[1] + ": " + r);
            }
        }
    }

    @Test
    void referencePositionsAtTheCommonSizes() {
        // 1280 x 720 at GUI scale 3 = 427 x 240
        assertEquals(new Rect(230, 210, 61, 16), ButtonPlacement.place(Menu.CODEX, 427, 240, LABEL, widgetsOf(Menu.CODEX, 427, 240)));
        assertEquals(new Rect(254, 10, 61, 16), ButtonPlacement.place(Menu.FOUR_KINGDOMS, 427, 240, LABEL, widgetsOf(Menu.FOUR_KINGDOMS, 427, 240)));
        assertEquals(new Rect(358, 4, 61, 16), ButtonPlacement.place(Menu.STATUS, 427, 240, LABEL, widgetsOf(Menu.STATUS, 427, 240)));
    }

    @Test
    void theCodexButtonSitsInTheStripUnderTheBookBesideGrimoireSlot() {
        int[] s = {480, 270};                                             // 1920 x 1080 at GUI scale 4
        Rect slot = widgetsOf(Menu.CODEX, s[0], s[1]).get(0);
        Rect r = ButtonPlacement.place(Menu.CODEX, s[0], s[1], LABEL, List.of(slot));
        assertEquals(slot.y(), r.y(), "same row as Grimoire Slot");
        assertEquals(slot.x() - ButtonPlacement.GAP, r.right(), "immediately to its left");
    }

    @Test
    void aWidgetOnTheMirroredSpotMovesTheButtonToAFreeCorner() {
        int sw = 427, sh = 240;
        Rect first = ButtonPlacement.place(Menu.CODEX, sw, sh, LABEL, List.of());
        List<Rect> blocked = List.of(first.inset(-6));                    // somebody put a widget exactly there
        Rect moved = ButtonPlacement.place(Menu.CODEX, sw, sh, LABEL, blocked);
        assertNotEquals(first, moved);
        assertFalse(moved.intersects(blocked.get(0)));
        assertTrue(moved.isInside(sw, sh, ButtonPlacement.EDGE));
    }

    @Test
    void ifEverythingIsBlockedTheButtonIsStillInsideTheWindow() {
        int sw = 427, sh = 240;
        List<Rect> wall = List.of(new Rect(0, 0, sw, sh));
        Rect r = ButtonPlacement.place(Menu.STATUS, sw, sh, LABEL, wall);
        assertTrue(r.isInside(sw, sh, 0), "a button over something is better than no button, and it is never off screen");
    }

    @Test
    void buttonWidthFollowsTheLabelWithinItsBounds() {
        assertEquals(ButtonPlacement.MIN_W, ButtonPlacement.buttonWidth(10));
        assertEquals(61, ButtonPlacement.buttonWidth(45));
        assertEquals(ButtonPlacement.MAX_W, ButtonPlacement.buttonWidth(400));
    }

    @Test
    void aTinyWindowStillGivesARectangleInsideIt() {
        Rect r = ButtonPlacement.place(Menu.CODEX, 120, 60, LABEL, List.of());
        assertTrue(r.x() >= 0 && r.y() >= 0 && r.w() > 0, "no negative coordinates: " + r);
    }

    // ---------------------------------------------------------------- the source guard
    private static String source(String relative) {
        Path p = Path.of("src/main/java/com/newuniverse/nusmp").resolve(relative);
        try {
            return Files.readString(p, StandardCharsets.UTF_8).replaceAll("[ \\t]+", " ");
        } catch (IOException e) {
            throw new AssertionError("cannot read " + p.toAbsolutePath() + " (run the tests from the project root): " + e);
        }
    }

    private static void mustContain(String file, String text, String fragment) {
        assertTrue(text.contains(fragment), file + " no longer contains `" + fragment + "`. ButtonPlacement mirrors this line to put the Skill Tree button beside it: update ButtonPlacement"
                + " (and widgetsOf/artOf in this test) to the menu's new layout, then change this fragment.");
    }

    @Test
    void menuSourcesStillMatchTheMirroredConstants() {
        String codex = source("client/ui/CodexScreen.java");
        mustContain("CodexScreen", codex, "bw = Math.min(width - 56, 440);");
        mustContain("CodexScreen", codex, "bh = Math.min(height - 48, 260);");
        mustContain("CodexScreen", codex, "bx = (width - bw) / 2 - 10;");
        mustContain("CodexScreen", codex, "by = (height - bh - 20) / 2;");
        mustContain("CodexScreen", codex, "new ThemedButton(bx + bw - 94, by + bh + 4, 88, 16,");
        String fk = source("client/multiverse/FourKingdomsScreen.java");
        mustContain("FourKingdomsScreen", fk, "PANEL_W = 340, PANEL_H = 236");
        mustContain("FourKingdomsScreen", fk, "left = (width - PANEL_W) / 2;");
        mustContain("FourKingdomsScreen", fk, "top = (height - PANEL_H) / 2;");
        mustContain("FourKingdomsScreen", fk, "new ThemedButton(left + PANEL_W - 64, top + 8, 56, 16,");
        String status = source("client/multiverse/MultiverseStatusScreen.java");
        mustContain("MultiverseStatusScreen", status, "new ThemedButton(width / 2 - 40, height - 26, 80, 18,");
        mustContain("MultiverseStatusScreen", status, "new ThemedButton(width - 96, height - 26, 88, 18,");
        mustContain("MultiverseStatusScreen", status, "\"MULTIVERSE STATUS\", width / 2, 10,");
    }
}
