package com.newuniverse.nusmp.dungeon.floor.charybdis;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/** The leviathan's own words: none of them is a devil's. */
class CharybdisTextTest {
    private static final String[] FORBIDDEN = {"devil", "beelzebub", "lucifero", "megicula", "astaroth", "qliphoth", "disciple", "contract", "zogratis"};

    private static List<String> everyLine() {
        List<String> all = new ArrayList<>();
        all.add(CharybdisText.opening());
        for (int[] step : new int[][]{{20, 40}, {40, 60}, {60, 80}, {50, 70}, {80, 100}}) all.add(CharybdisText.power(step[0], step[1]));
        all.add(CharybdisText.gates());
        all.add(CharybdisText.death());
        all.add(CharybdisText.hint());
        all.add(CharybdisText.surfacing());
        all.add(CharybdisText.megalodons());
        all.add(CharybdisText.maelstrom());
        all.add(CharybdisText.fallen(null));
        all.add(CharybdisText.fallen("Aster"));
        all.add(CharybdisText.fallenDuringCollapse(null));
        all.add(CharybdisText.fallenDuringCollapse("Aster"));
        return all;
    }

    @Test
    void noLineMentionsADevilOrItsGates() {
        for (String line : everyLine()) {
            assertFalse(line.isBlank());
            String lower = line.toLowerCase(Locale.ROOT);
            for (String bad : FORBIDDEN) assertFalse(lower.contains(bad), "'" + bad + "' in: " + line);
        }
    }

    @Test
    void theFrameworksGatesLineIsDroppedAndOthersAreLeftAlone() {
        assertEquals("", CharybdisText.translate("The Gates of the Tree of Qliphoth open!"), "the leviathan's own line follows it, so this one is not said");
        assertEquals("Anything else.", CharybdisText.translate("Anything else."));
        assertEquals("", CharybdisText.translate(null));
        assertTrue(CharybdisText.FRAMEWORK_GATES.contains("Qliphoth"), "this is the line that has to go");
    }

    @Test
    void thePowerLinesNameTheStepAndTheMaelstromComesAtSixty() {
        assertTrue(CharybdisText.power(40, 60).contains("Maelstrom"));
        assertTrue(CharybdisText.power(60, 80).contains("80%"));
        assertTrue(CharybdisText.power(20, 40).contains("40%"));
        assertTrue(CharybdisText.power(30, 50).contains("50%"), "a power between the steps still says what it is");
    }

    @Test
    void theHintTeachesTheThreeAnswers() {
        String h = CharybdisText.hint().toLowerCase(Locale.ROOT);
        assertTrue(h.contains("shield") && h.contains("parry") && h.contains("strike"), h);
    }

    @Test
    void theFallLinesNameTheKillerWhenThereIsOne() {
        assertTrue(CharybdisText.fallen("Aster").startsWith("Aster has slain"));
        assertTrue(CharybdisText.fallen(null).startsWith("The Charybdis Spawn has been slain"));
        assertTrue(CharybdisText.fallenDuringCollapse("Aster").contains("Aster"));
        assertTrue(CharybdisText.fallenDuringCollapse(null).contains("already coming down"));
    }
}
