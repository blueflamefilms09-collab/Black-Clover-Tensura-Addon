package com.newuniverse.nusmp.labyrinth.mega.system;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the floor report is only clear if the lines say what the owner asked for, in the order a player reads them: the floor and its direction, the theme with its line and its
 * road, the mechanics each with what it does and how to beat it, the depth. These tests compose reports for both roads, for a boss floor, for a theme and a mechanic that have no
 * text, with the mechanics layer off, and with a translator that knows only some keys, and read the lines back.
 */
class FloorCardLinesTest {
    private static FloorCard.Mech mech(String id, String name, String desc, String counter, String hint, String scope, String state) {
        return new FloorCard.Mech(id, name, desc, counter, hint, scope, state);
    }

    private static FloorCard holy() {
        return new FloorCard(FloorInfoRules.Reason.FORWARD.code(), 12, 11, true, 12, false, "holy_church", "Holy Church", "A great cathedral.", "holy_only", 90, 12, 8, 1, 109, true, 3,
                List.of(mech("floor.rim_curse", "Rim Curse", "The edges of the floor drain you.", "none", "", "floor", "armed"),
                        mech("floor.lantern_vow", "Lantern Vow", "Rooms stay dark unless a light is carried.", "hold_light", "Carry a light through it.", "floor", "waiting")),
                List.of(mech("puzzle.sigil", "Sigil Hall", "Four plates, one order.", "sequence", "The pedestals have an order.", "room", "armed")));
    }

    private static String text(List<FloorCardLines.Line> lines) {
        StringBuilder b = new StringBuilder();
        for (FloorCardLines.Line l : lines) b.append(l.kind()).append(": ").append(l.text()).append('\n');
        return b.toString();
    }

    @Test void theReportSaysFloorDirectionThemeMechanicsAndDepthInThatOrder() {
        List<FloorCardLines.Line> lines = FloorCardLines.compose(holy(), FloorCardLines.english(), "", false);
        String t = text(lines);
        assertEquals(FloorCardLines.Kind.TITLE, lines.get(0).kind());
        assertEquals("FLOOR +12  -  UP", lines.get(0).text());
        assertTrue(t.contains("HOLY ROAD: the way on climbs UP"), t);
        assertTrue(t.contains("You go UP, to floor +12."), t);
        assertTrue(t.contains("THEME: Holy Church"), t);
        assertTrue(t.contains("HOLY ROAD ONLY  -  DANGER x0.9"), t);
        assertTrue(t.contains("A great cathedral: nave and pews"), "a theme the mod describes uses its built-in line: " + t);
        assertTrue(t.contains("FLOOR MECHANICS (2)"), t);
        assertTrue(t.contains("Rim Curse") && t.contains("The edges of the floor drain you."), t);
        assertTrue(t.contains("Counter: none. Survive it, or leave its area."), t);
        assertTrue(t.contains("Lantern Vow") && t.contains("Counter: Carry a light through it."), t);
        assertTrue(t.contains("WAITING  -  whole floor") && t.contains("ACTIVE  -  whole floor"), t);
        assertTrue(t.contains("ACTIVE HERE (1)") && t.contains("Sigil Hall") && t.contains("ACTIVE  -  this room"), t);
        assertTrue(t.contains("3 more mechanic(s) wait in the rooms of this floor"), t);
        assertTrue(t.contains("DEPTH 12: monsters +12% health and +8% damage, mind level 1, kills give x1.09 experience."), t);
        assertTrue(t.indexOf("TITLE") < t.indexOf("THEME:") && t.indexOf("THEME:") < t.indexOf("FLOOR MECHANICS") && t.indexOf("FLOOR MECHANICS") < t.indexOf("DEPTH 12"), "order");
    }

    @Test void theAbyssalRoadGoesDown() {
        FloorCard c = new FloorCard(FloorInfoRules.Reason.ARRIVE.code(), -7, 0, false, 7, false, "necropolis", "Necropolis", "A city of the dead.", "abyssal_only", 110, 2, 1, 1, 102, true, 0,
                List.of(), List.of());
        String t = text(FloorCardLines.compose(c, FloorCardLines.english(), "", false));
        assertTrue(t.contains("FLOOR -7  -  DOWN"), t);
        assertTrue(t.contains("ABYSSAL ROAD: the way on descends DOWN"), t);
        assertTrue(t.contains("You arrive on floor -7."), t);
        assertTrue(t.contains("ABYSSAL ROAD ONLY"), t);
        assertTrue(t.contains("No floor-wide mechanic is in effect on this floor."), t);
    }

    @Test void aFavouredThemeAndAThemeOfBothRoadsAreWordedDifferently() {
        FloorCard favoured = new FloorCard(0, 3, 0, true, 3, false, "frost", "Frost", "Cold.", "holy", 100, 0, 0, 0, 100, true, 0, List.of(), List.of());
        assertTrue(text(FloorCardLines.compose(favoured, FloorCardLines.english(), "", false)).contains("HOLY ROAD FAVOURED"));
        FloorCard both = new FloorCard(0, 3, 0, true, 3, false, "gravity", "Gravity", "Heavy.", "any", 100, 0, 0, 0, 100, true, 0, List.of(), List.of());
        String t = text(FloorCardLines.compose(both, FloorCardLines.english(), "", false));
        assertTrue(t.contains("BOTH ROADS  -  DANGER x1"), t);
    }

    @Test void aBossFloorAndTheFirstFloorHaveTheirOwnLines() {
        FloorCard boss = new FloorCard(0, 10, 0, true, 10, true, "gravity", "Gravity", "Heavy.", "any", 100, 12, 7, 1, 109, true, 0, List.of(), List.of());
        assertTrue(text(FloorCardLines.compose(boss, FloorCardLines.english(), "", false)).contains("BOSS FLOOR: the Guardian waits at the end of this floor."));
        FloorCard first = new FloorCard(0, 1, 0, true, 1, false, "gravity", "Gravity", "Heavy.", "any", 100, 0, 0, 0, 100, true, 0, List.of(), List.of());
        String t = text(FloorCardLines.compose(first, FloorCardLines.english(), "", false));
        assertTrue(t.contains("DEPTH 1: the first floor, monsters have their base strength."), t);
        assertFalse(t.contains("BOSS FLOOR"));
    }

    @Test void aThemeAndAMechanicWithNoTextStillRead() {
        FloorCard c = new FloorCard(3, 5, 0, true, 5, false, "brand_new_theme", "", "", "any", 100, 0, 0, 0, 100, true, 0,
                List.of(mech("hazard.brand_new.e_poison", "", "", "kill_all", "", "floor", "armed"), mech("hazard.odd", "Odd", "", "mystery_counter", "", "floor", "armed")), List.of());
        String t = text(FloorCardLines.compose(c, FloorCardLines.english(), "", false));
        assertTrue(t.contains("Hazard Brand New E Poison"), "a mechanic with no name reads as its readable id: " + t);
        assertTrue(t.contains("Counter: Kill All"), "an unknown hint reads as its readable counter kind: " + t);
        assertTrue(t.contains("Counter: Mystery Counter"), t);
        assertTrue(t.contains("THEME: "), t);
    }

    @Test void aThemeWithNoIdIsSaidToBeUnshaped() {
        FloorCard c = new FloorCard(0, 5, 0, true, 5, false, "", "", "", "any", 100, 0, 0, 0, 100, true, 0, List.of(), List.of());
        assertTrue(text(FloorCardLines.compose(c, FloorCardLines.english(), "", false)).contains("THEME: not shaped yet"));
    }

    @Test void mechanicsSwitchedOffAreSaidSoAndTheDepthStillShows() {
        FloorCard c = new FloorCard(0, 5, 0, true, 5, false, "frost", "Frost", "Cold.", "holy", 100, 5, 3, 0, 101, false, 0, List.of(), List.of());
        String t = text(FloorCardLines.compose(c, FloorCardLines.english(), "", false));
        assertTrue(t.contains("Mechanics are switched off on this server."), t);
        assertFalse(t.contains("FLOOR MECHANICS"), t);
        assertTrue(t.contains("DEPTH 5"), t);
    }

    @Test void moreThanTheLimitIsCutToAMoreLine() {
        List<FloorCard.Mech> many = new ArrayList<>();
        for (int i = 0; i < FloorCard.MAX_MECHS + 1; i++) many.add(mech("floor.m" + i, "M" + i, "d", "none", "", "floor", "armed"));
        FloorCard c = new FloorCard(0, 5, 0, true, 5, false, "frost", "Frost", "Cold.", "holy", 100, 0, 0, 0, 100, true, 0, many, List.of());
        List<FloorCardLines.Line> lines = FloorCardLines.compose(c, FloorCardLines.english(), "", false);
        long names = lines.stream().filter(l -> l.kind() == FloorCardLines.Kind.MECH).count();
        assertEquals(FloorCard.MAX_MECHS, names);
        assertTrue(text(lines).contains("+1 more"));
    }

    @Test void theFooterNamesTheKeyAndTheEscapeCommand() {
        String t = text(FloorCardLines.compose(holy(), FloorCardLines.english(), "[.]", true));
        assertTrue(t.contains("Press [.], or type /dungeon floor, to read this again."), t);
        assertTrue(t.contains("Stuck? Type /dungeon unstuck."), t);
        String bare = text(FloorCardLines.compose(holy(), FloorCardLines.english(), "", false));
        assertFalse(bare.contains("Stuck?"));
    }

    @Test void aTranslatorWithTheLangFileWinsOverTheFallback() {
        Map<String, String> lang = Map.of("mechanic.nusmp.floor.rim_curse.name", "Fluch des Randes", "message.nusmp.mega.mgd.theme.holy_church", "Heilige Kirche");
        FloorCardLines.Tr de = (key, fallback, args) -> FloorCardLines.format(lang.getOrDefault(key, fallback), args);
        String t = text(FloorCardLines.compose(holy(), de, "", false));
        assertTrue(t.contains("Fluch des Randes"), t);
        assertTrue(t.contains("THEME: Heilige Kirche") || t.contains("Heilige Kirche"), t);
        assertTrue(t.contains("Lantern Vow"), "a key with no text falls back to the English the card carries");
    }

    @Test void formatNeverThrows() {
        assertEquals("a x", FloorCardLines.format("a %1$s", "x", "y"));
        assertEquals("100%", FloorCardLines.format("100%%"));
        assertEquals("bad %q", FloorCardLines.format("bad %q", "x"));
        assertEquals("", FloorCardLines.format(null, "x"));
    }
}
