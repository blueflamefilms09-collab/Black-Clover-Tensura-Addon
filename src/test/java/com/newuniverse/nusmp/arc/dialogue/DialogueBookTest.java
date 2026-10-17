package com.newuniverse.nusmp.arc.dialogue;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The dialogue parser and selector: which line a player hears, and that a bad file never makes a character mute. */
class DialogueBookTest {
    private static DialogueBook book(String json) {
        DialogueBook.Parsed p = DialogueBook.parse("yami", json);
        assertTrue(p.ok(), String.valueOf(p.problems()));
        return p.book();
    }

    private static DialogueSituation sit(int stage, String saga, List<String> defeated, int quest, boolean first) {
        return DialogueSituation.of(stage, saga, defeated, quest, first);
    }

    private static boolean anyProblem(DialogueBook.Parsed p, String part) {
        for (String s : p.problems()) if (s.contains(part)) return true;
        return false;
    }

    @Test
    void stringsAndObjectsAreBothLines() {
        DialogueBook b = book("{\"lines\":{\"greeting\":[\"Yo.\",{\"text\":\"Hello, {player}.\",\"key\":\"arc.dialogue.yami.2\"}],\"hint\":[\"Eat.\"]}}");
        assertEquals(3, b.size());
        assertEquals(2, b.lines(DialogueContext.GREETING).size());
        assertEquals("", b.lines(DialogueContext.GREETING).get(0).key());
        assertEquals("arc.dialogue.yami.2", b.lines(DialogueContext.GREETING).get(1).key());
        assertEquals(0, b.lines(DialogueContext.SHOP).size());
        assertEquals("yami", b.characterId());
        assertEquals(-1, b.voice());
    }

    @Test
    void allLinesComeInTheOrderTheLanguageKeysAreNumberedIn() {
        DialogueBook b = book("{\"lines\":{\"farewell\":[\"f1\"],\"hint\":[\"h1\",\"h2\"],\"greeting\":[\"g1\"]}}");
        List<DialogueLine> all = b.all();
        assertEquals("g1", all.get(0).text());
        assertEquals("h1", all.get(1).text());
        assertEquals("h2", all.get(2).text());
        assertEquals("f1", all.get(3).text());
    }

    @Test
    void placeholdersBecomeLanguageArgumentsAndPercentIsEscaped() {
        DialogueLine l = book("{\"lines\":{\"greeting\":[\"Hello, {player}! I am {npc} of the {saga}. 100% sure.\"]}}").lines(DialogueContext.GREETING).get(0);
        assertEquals("Hello, %1$s! I am %2$s of the %3$s. 100%% sure.", l.template());
        assertEquals("Hello, Asta! I am Yami of the Black Bulls. 100% sure.", l.plain("Asta", "Yami", "Black Bulls"));
    }

    @Test
    void conditionsFilterLines() {
        DialogueBook b = book("{\"lines\":{\"hint\":["
                + "{\"text\":\"stage1to2\",\"minStage\":1,\"maxStage\":2},"
                + "{\"text\":\"saga\",\"saga\":[\"spade_raid\"]},"
                + "{\"text\":\"needs\",\"defeated\":[\"Heath_Grice\",\"revchi\"]},"
                + "{\"text\":\"forbids\",\"notDefeated\":\"zagred\"},"
                + "{\"text\":\"quest1\",\"minQuest\":1,\"maxQuest\":1},"
                + "{\"text\":\"newcomer\",\"first\":true},"
                + "{\"text\":\"regular\",\"first\":false}]}}");
        List<DialogueLine> l = b.lines(DialogueContext.HINT);
        DialogueSituation s0 = sit(0, "elf_reincarnation", List.of(), 0, false);
        assertFalse(l.get(0).matches(s0));
        assertTrue(l.get(0).matches(sit(2, "elf_reincarnation", List.of(), 0, false)));
        assertFalse(l.get(0).matches(sit(3, "elf_reincarnation", List.of(), 0, false)));
        assertFalse(l.get(1).matches(s0));
        assertTrue(l.get(1).matches(sit(0, "SPADE_RAID", List.of(), 0, false)));
        assertFalse(l.get(2).matches(sit(0, "", List.of("heath_grice"), 0, false)), "one of two is not enough");
        assertTrue(l.get(2).matches(sit(0, "", List.of("revchi", "heath_grice", "other"), 0, false)));
        assertTrue(l.get(3).matches(s0));
        assertFalse(l.get(3).matches(sit(0, "", List.of("zagred"), 0, false)));
        assertFalse(l.get(4).matches(s0));
        assertTrue(l.get(4).matches(sit(0, "", List.of(), 1, false)));
        assertFalse(l.get(4).matches(sit(0, "", List.of(), 2, false)));
        assertFalse(l.get(5).matches(s0));
        assertTrue(l.get(5).matches(sit(0, "", List.of(), 0, true)));
        assertTrue(l.get(6).matches(s0));
        assertFalse(l.get(6).matches(sit(0, "", List.of(), 0, true)));
    }

    @Test
    void theMostSpecificLineWinsAndPriorityZeroMixesWithTheGenericOnes() {
        DialogueBook b = book("{\"lines\":{\"hint\":[\"generic\",{\"text\":\"specific\",\"defeated\":[\"heath_grice\"],\"minStage\":1}]}}");
        DialogueSituation after = sit(1, "", List.of("heath_grice"), 0, false);
        assertEquals("specific", b.pick(DialogueContext.HINT, after, -1, n -> 0).line().text());
        assertEquals("generic", b.pick(DialogueContext.HINT, sit(0, "", List.of(), 0, false), -1, n -> 0).line().text());

        DialogueBook mixed = book("{\"lines\":{\"hint\":[\"generic\",{\"text\":\"specific\",\"defeated\":[\"heath_grice\"],\"priority\":0}]}}");
        boolean sawGeneric = false, sawSpecific = false;
        for (int roll = 0; roll < 2; roll++) {
            final int r = roll;
            String t = mixed.pick(DialogueContext.HINT, after, -1, n -> r).line().text();
            sawGeneric |= t.equals("generic");
            sawSpecific |= t.equals("specific");
        }
        assertTrue(sawGeneric && sawSpecific, "priority 0 lets a conditional line mix with the generic ones");
    }

    @Test
    void weightsDecideAmongEqualsAndTheLastLineIsAvoided() {
        DialogueBook b = book("{\"lines\":{\"hint\":[{\"text\":\"a\",\"weight\":3},\"b\",\"c\"]}}");
        DialogueSituation s = sit(0, "", List.of(), 0, false);
        // total weight 5: rolls 0..2 give a, 3 gives b, 4 gives c
        assertEquals("a", b.pick(DialogueContext.HINT, s, -1, n -> 0).line().text());
        assertEquals("a", b.pick(DialogueContext.HINT, s, -1, n -> 2).line().text());
        assertEquals("b", b.pick(DialogueContext.HINT, s, -1, n -> 3).line().text());
        assertEquals("c", b.pick(DialogueContext.HINT, s, -1, n -> 4).line().text());
        // avoiding index 0 (a): total weight 2: rolls give b then c, never a
        for (int roll = 0; roll < 2; roll++) {
            final int r = roll;
            assertNotEquals("a", b.pick(DialogueContext.HINT, s, 0, n -> r).line().text());
        }
        assertEquals(1, b.pick(DialogueContext.HINT, s, 0, n -> 0).index());
        // a lone line is repeated rather than silence
        DialogueBook one = book("{\"lines\":{\"hint\":[\"only\"]}}");
        assertEquals("only", one.pick(DialogueContext.HINT, s, 0, n -> 0).line().text());
        // an out-of-range roll is clamped, never an exception
        assertNotNull(b.pick(DialogueContext.HINT, s, -1, n -> 99));
    }

    @Test
    void aMoreSpecificLineIsNotDroppedToAvoidARepeat() {
        DialogueBook b = book("{\"lines\":{\"hint\":[\"generic\",{\"text\":\"specific\",\"minStage\":1}]}}");
        DialogueBook.Pick p = b.pick(DialogueContext.HINT, sit(1, "", List.of(), 0, false), 1, n -> 0);
        assertEquals("specific", p.line().text());
    }

    @Test
    void nothingFitsGivesNull() {
        DialogueBook b = book("{\"lines\":{\"hint\":[{\"text\":\"late\",\"minStage\":5}]}}");
        assertNull(b.pick(DialogueContext.HINT, sit(0, "", List.of(), 0, false), -1, n -> 0));
        assertNull(b.pick(DialogueContext.SHOP, sit(9, "", List.of(), 0, false), -1, n -> 0));
        assertFalse(b.has(DialogueContext.HINT, sit(0, "", List.of(), 0, false)));
        assertTrue(b.has(DialogueContext.HINT, sit(5, "", List.of(), 0, false)));
    }

    @Test
    void problemsAreReportedAndTheRestOfTheFileIsKept() {
        DialogueBook.Parsed p = DialogueBook.parse("yami", "{\"character\":\"asta\",\"voice\":\"red\",\"lines\":{"
                + "\"greeting\":[\"fine\",{\"key\":\"k\"},\"  \",5,{\"text\":\"x {nobody}\"},{\"text\":\"y\",\"minStage\":\"two\"},{\"text\":\"z\",\"minStage\":4,\"maxStage\":2},"
                + "{\"text\":\"w\",\"defeated\":[\"a\"],\"notDefeated\":[\"a\"]},{\"text\":\"v\",\"first\":1}],"
                + "\"dancing\":[\"?\"],\"hint\":\"not an array\"}}");
        assertTrue(p.ok());
        assertEquals(6, p.book().size(), "fine, x, y, z, w and v survive");
        assertTrue(anyProblem(p, "'asta'"), "the character field does not match the file");
        assertTrue(anyProblem(p, "voice"));
        assertTrue(anyProblem(p, "needs a non-blank"));
        assertTrue(anyProblem(p, "blank text"));
        assertTrue(anyProblem(p, "a string or an object"));
        assertTrue(anyProblem(p, "{nobody}"));
        assertTrue(anyProblem(p, "must be a number"));
        assertTrue(anyProblem(p, "can never be said"));
        assertTrue(anyProblem(p, "both required and forbidden"));
        assertTrue(anyProblem(p, "\"first\" must be true or false"));
        assertTrue(anyProblem(p, "unknown situation 'dancing'"));
        assertTrue(anyProblem(p, "hint: must be an array"));
    }

    @Test
    void aBrokenFileGivesNoBookButNeverThrows() {
        for (String bad : new String[]{"", "not json", "[]", "{}", "{\"lines\":[]}", "{\"lines\":{}}", "{\"lines\":{\"hint\":[]}}", "{\"lines\":{\"hint\":[{\"x\":1}]}}"}) {
            DialogueBook.Parsed p = DialogueBook.parse("yami", bad);
            assertFalse(p.ok(), bad);
            assertFalse(p.problems().isEmpty(), bad);
        }
        assertFalse(DialogueBook.parse("yami", null).ok());
    }

    @Test
    void theVoiceColourIsRead() {
        assertEquals(0xC8C8D8, book("{\"voice\":\"#C8C8D8\",\"lines\":{\"hint\":[\"x\"]}}").voice());
        assertEquals(0x00FF10, book("{\"voice\":\"00ff10\",\"lines\":{\"hint\":[\"x\"]}}").voice());
    }

    @Test
    void contextsAreNamedByTheirIds() {
        assertEquals(DialogueContext.QUEST_OFFER, DialogueContext.fromId("Quest_Offer"));
        assertNull(DialogueContext.fromId("nope"));
        assertNull(DialogueContext.fromId(null));
        assertEquals(6, DialogueContext.values().length);
    }
}
