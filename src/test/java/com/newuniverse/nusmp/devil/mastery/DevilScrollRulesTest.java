package com.newuniverse.nusmp.devil.mastery;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why (5.7.0): the scroll text and the requirements list are pure, so what they promise is pinned here: every step of mastery is listed, the real kits give real pages, the book limits hold, the boost speeds the climb. */
class DevilScrollRulesTest {
    private static DevilKit.Kit kit(String id) { return DevilKit.table().of(id); }

    private static DevilScrollRules.Info info(DevilKit.Kit k, double rate) {
        return new DevilScrollRules.Info("Liebe", "the Anti-Magic devil", "Black Clover", "high", "anti magic", "It cancels magic.", "half your health", k, 10f, rate, 360);
    }

    @Test
    void everyStepOfMasteryIsListed() {
        String all = String.join("\n", DevilScrollRules.requirementLines(kit("liebe"), 10f));
        for (String step : new String[] {"Mastery 10%", "Mastery 20%", "Mastery 25%", "Mastery 50%", "Mastery 75%", "Mastery 100%"}) assertTrue(all.contains(step), step);
        assertTrue(all.contains("Black Slash") && all.contains("Black Meteorite"), "the arts of the kit are named at their step");
    }

    @Test
    void aDevilWithoutAKitStillGetsTheSteps() {
        List<String> lines = DevilScrollRules.requirementLines(null, 10f);
        assertTrue(lines.size() >= 7);
        assertTrue(String.join("\n", lines).contains("Devil Union"));
    }

    @Test
    void everyKitMakesAScrollInsideTheBookLimits() {
        assertFalse(DevilKit.table().kits().isEmpty());
        for (DevilKit.Kit k : DevilKit.table().kits().values()) {
            List<String> pages = DevilScrollRules.pages(info(k, 3.0));
            assertTrue(pages.size() >= 2 && pages.size() <= DevilScrollRules.MAX_PAGES, k.devil() + " pages " + pages.size());
            for (String p : pages) assertTrue(p.length() <= DevilScrollRules.PAGE_CHARS + 60, k.devil() + " page too long: " + p.length());
            String all = String.join("\n", pages);
            assertTrue(all.contains("Mastery 100%") && all.contains("HOW IT WORKS"), k.devil());
        }
    }

    @Test
    void theTitleFitsTheBook() {
        assertEquals("Scroll of Liebe", DevilScrollRules.titleFor("Liebe"));
        assertTrue(DevilScrollRules.titleFor("a daemon of Noir's line, the black Primordial's kin").length() <= DevilScrollRules.TITLE_MAX);
        assertEquals("Scroll of the Devil", DevilScrollRules.titleFor(null));
    }

    @Test
    void aHugeTextIsCutAtTheMaxPages() {
        List<String> lines = new java.util.ArrayList<>();
        for (int i = 0; i < 400; i++) lines.add("line number " + i + " of a very long text that would fill many pages");
        assertEquals(DevilScrollRules.MAX_PAGES, DevilScrollRules.paginate(lines, DevilScrollRules.PAGE_CHARS, DevilScrollRules.MAX_PAGES).size());
    }

    @Test
    void theBoostMakesTheClimbFaster() {
        double slow = DevilScrollRules.activeHours(1.0), fast = DevilScrollRules.activeHours(3.0);
        assertTrue(slow >= 25 && slow <= 40, "the old speed is still the pinned 25 to 40 hours: " + slow);
        assertTrue(fast < slow / 2.5 && fast > slow / 3.5, "x3 is about a third of the time: " + fast);
        assertTrue(MasteryRules.gain(10f, 100, 3.0) > MasteryRules.gain(10f, 100, 1.0));
    }
}
