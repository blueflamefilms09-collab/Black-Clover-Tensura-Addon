package com.newuniverse.nusmp.dungeon.floor.goliath;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The Goliath's words: none of them names a devil, the framework's devil lines are rewritten, the bar and the server lines read right. */
class GoliathTextTest {
    private static List<String> everyLine() {
        List<String> all = new ArrayList<>();
        all.add(GoliathText.opening());
        for (int now = 0; now <= 100; now += 10) for (int old = 0; old <= now; old += 10) all.add(GoliathText.power(old, now));
        all.add(GoliathText.gates());
        all.add(GoliathText.death());
        all.add(GoliathText.cracking());
        all.add(GoliathText.risen());
        all.add(GoliathText.gatesNarration());
        all.add(GoliathText.hintWaves());
        all.add(GoliathText.hintExposed());
        for (String who : new String[]{null, "", "  ", "Steve"}) {
            all.add(GoliathText.woken(who));
            all.add(GoliathText.fallen(who));
            all.add(GoliathText.fallenDuringCollapse(who));
            all.add(GoliathText.slain(who));
        }
        for (int p : new int[]{0, 20, 60, 100}) for (boolean g : new boolean[]{false, true}) all.add(GoliathText.bar(p, g));
        for (GoliathTable.Spec s : GoliathTable.specs()) all.add(GoliathTable.warning(s.id()));
        return all;
    }

    @Test
    void noLineNamesADevil() {
        for (String line : everyLine()) {
            assertFalse(line.isBlank(), "a blank line");
            assertFalse(GoliathText.leaks(line), "a line names a devil: " + line);
            assertFalse(line.toLowerCase().contains("lucifero") || line.toLowerCase().contains("astaroth") || line.toLowerCase().contains("qliphoth"), line);
        }
    }

    @Test
    void theFrameworksDevilLinesAreRewritten() {
        assertEquals(GoliathText.gatesNarration(), GoliathText.rewrite("The Gates of the Tree of Qliphoth open!"));
        assertEquals("The Goliath's stone shakes with a new fury.", GoliathText.rewrite("Dante Zogratis has fallen. The Goliath rages: 20% -> 30% of Astaroth's power."));
        assertEquals("The Goliath's stone shakes with a new fury.", GoliathText.rewrite("A Dark Disciple answers."));
        assertEquals("The Goliath has risen.", GoliathText.rewrite("The Goliath has risen."), "its own lines are left as they are");
        assertEquals("", GoliathText.rewrite(null));
        for (String line : new String[]{"The Gates of the Tree of Qliphoth open!", "X has fallen. The Goliath rages: 50% -> 60% of Astaroth's power.", "plain text"})
            assertFalse(GoliathText.leaks(GoliathText.rewrite(line)), line);
    }

    @Test
    void leaksSeesEveryDevilWordAndNothingElse() {
        for (String w : new String[]{"Lucifero", "Megicula", "Beelzebub", "Astaroth", "Qliphoth", "a devil", "Devil magic", "Zogratis", "Dark Disciples"})
            assertTrue(GoliathText.leaks("some " + w + " text"), w);
        assertFalse(GoliathText.leaks("The Goliath's stone splits."));
        assertFalse(GoliathText.leaks(null));
    }

    @Test
    void theLinesGrowWithThePower() {
        assertNotEquals(GoliathText.power(0, 20), GoliathText.power(20, 40));
        assertNotEquals(GoliathText.power(20, 40), GoliathText.power(40, 60));
        assertNotEquals(GoliathText.power(40, 60), GoliathText.power(60, 80));
        assertEquals(GoliathText.power(60, 80), GoliathText.power(70, 90), "the line is chosen by the power it reaches");
    }

    @Test
    void theBarShowsTheFuryAndTheCrack() {
        assertEquals("The Goliath - Fury 20%", GoliathText.bar(20, false));
        assertEquals("The Goliath - Fury 100% - Cracked", GoliathText.bar(100, true));
        assertEquals("The Goliath - Fury 0%", GoliathText.bar(-5, false));
        assertTrue(GoliathText.bar(40, false).startsWith(GoliathText.NAME));
    }

    @Test
    void theServerLinesNameWhoDidIt() {
        assertTrue(GoliathText.woken("Steve").contains("Steve"));
        assertTrue(GoliathText.woken(null).contains("Something"));
        assertTrue(GoliathText.woken("  ").contains("Something"));
        assertTrue(GoliathText.fallen("Alex").contains("Alex") && GoliathText.fallen("Alex").contains("come down"));
        assertFalse(GoliathText.fallen(null).contains("null"));
        assertTrue(GoliathText.fallenDuringCollapse("Alex").contains("Alex"));
        assertFalse(GoliathText.fallenDuringCollapse("Alex").contains("come down"), "the pit is already coming down");
        assertFalse(GoliathText.fallenDuringCollapse(null).contains("null"));
    }

    @Test
    void theLineTheBossSaysPromisesNoPit() {
        assertTrue(GoliathText.slain("Alex").contains("Alex"));
        assertFalse(GoliathText.slain("Alex").contains("pit"), "since 1.58.0 no pit comes down");
        assertFalse(GoliathText.slain("Alex").contains("come down"));
        assertFalse(GoliathText.slain(null).contains("null"));
        assertFalse(GoliathText.slain("  ").isBlank());
        assertEquals("The Goliath has fallen.", GoliathText.slain("  "));
    }
}
