package com.newuniverse.nusmp.entity.avatar;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** What the Devil Avatar says: every move has its lines in both voices, the two voices differ, and nothing it says is empty, huge or a formatting code. */
class AvatarTextTest {
    private static final int[] VARIANTS = {AvatarTable.LUCIFERO, AvatarTable.MEGICULA};

    private static List<String> everyLine() {
        List<String> all = new ArrayList<>();
        for (int v : VARIANTS) {
            all.add(AvatarText.opening(v));
            all.add(AvatarText.gates(v));
            all.add(AvatarText.death(v));
            for (int[] step : new int[][]{{20, 40}, {40, 60}, {60, 80}, {80, 100}, {0, 20}, {20, 20}}) all.add(AvatarText.power(v, step[0], step[1]));
            for (AvatarTable.Spec s : AvatarTable.specs(v)) for (String q : AvatarText.quips(v, s.id())) all.add(q);
        }
        all.add(AvatarText.fell("Avatar of Lucifero", "Steve"));
        all.add(AvatarText.fell("Avatar of Megicula", null));
        return all;
    }

    @Test
    void everyMoveHasLinesInBothVoices() {
        for (int v : VARIANTS) {
            for (AvatarTable.Spec s : AvatarTable.specs(v)) {
                String[] q = AvatarText.quips(v, s.id());
                assertTrue(q.length >= 1, s.id() + " has a line (variant " + v + ")");
                for (String line : q) {
                    assertFalse(line.isBlank(), s.id());
                    assertNotEquals("...", line, s.id() + " must not fall back to the placeholder");
                }
            }
        }
        assertArrayEquals(new String[]{"..."}, AvatarText.quips(AvatarTable.LUCIFERO, "no_such_move"), "an unknown move is a silent beat, not an error");
    }

    @Test
    void theTwoVoicesDiffer() {
        assertNotEquals(AvatarText.opening(AvatarTable.LUCIFERO), AvatarText.opening(AvatarTable.MEGICULA));
        assertNotEquals(AvatarText.gates(AvatarTable.LUCIFERO), AvatarText.gates(AvatarTable.MEGICULA));
        assertNotEquals(AvatarText.death(AvatarTable.LUCIFERO), AvatarText.death(AvatarTable.MEGICULA));
        for (AvatarTable.Spec s : AvatarTable.specs())
            assertFalse(java.util.Arrays.equals(AvatarText.quips(AvatarTable.LUCIFERO, s.id()), AvatarText.quips(AvatarTable.MEGICULA, s.id())), s.id() + " sounds the same in both voices");
        assertEquals(AvatarText.opening(AvatarTable.LUCIFERO), AvatarText.opening(7), "an unknown variant speaks as Lucifero's Avatar");
    }

    @Test
    void thePowerStepsSayWhichStepTheyAre() {
        for (int v : VARIANTS) {
            assertTrue(AvatarText.power(v, 60, 80).contains("ighty"), AvatarText.power(v, 60, 80));
            assertTrue(AvatarText.power(v, 40, 60).contains("ixty"), AvatarText.power(v, 40, 60));
            assertTrue(AvatarText.power(v, 20, 40).contains("orty"), AvatarText.power(v, 20, 40));
            assertTrue(AvatarText.power(v, 0, 20).contains("20%"), "a first step names its percentage: " + AvatarText.power(v, 0, 20));
            assertNotEquals(AvatarText.power(v, 20, 40), AvatarText.power(v, 40, 60));
        }
    }

    @Test
    void theFallReportNamesTheAvatarAndTheKillerOnlyWhenThereIsOne() {
        String named = AvatarText.fell("Avatar of Lucifero", "Steve");
        assertTrue(named.contains("Avatar of Lucifero") && named.contains("Steve") && named.contains("Underworld Gate"), named);
        for (String none : new String[]{null, "", "   "}) {
            String line = AvatarText.fell("Avatar of Megicula", none);
            assertTrue(line.contains("Avatar of Megicula") && !line.contains("hands"), line);
        }
    }

    @Test
    void nothingItSaysIsEmptyHugeOrAFormattingCode() {
        for (String line : everyLine()) {
            assertNotNull(line);
            assertFalse(line.isBlank());
            assertTrue(line.length() <= 140, "short enough for the chat: " + line);
            assertTrue(line.chars().noneMatch(c -> c < 32 || c == 167), "no control or formatting characters (167 is the section sign): " + line);
        }
    }

}
