package com.newuniverse.nusmp.skilltree.shop;

import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.arc.dialogue.DialogueBook;
import com.newuniverse.nusmp.arc.dialogue.DialogueContext;
import com.newuniverse.nusmp.arc.dialogue.DialogueLine;
import com.newuniverse.nusmp.arc.dialogue.DialogueSituation;
import com.newuniverse.nusmp.arc.dialogue.TalkPlan;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: what the witch says is data, so a typo in it fails silently in the game (a hint that can never be heard, a shop line missing so the shop opens in silence, a number that
 * goes stale when the economy is retuned). These tests read the real file and check it with the real {@link DialogueBook}: it parses clean, every line is keyed and plain ASCII,
 * a newcomer is greeted at every stage and saga, there is always a hint (and the hint that tells a player to sneak is among the most likely), the shop and the farewell have lines
 * for everyone, the conditions only name a real boss, no line quotes a price or an odds number (those live in data and would go stale), and a long conversation mixes lines
 * without repeating one back to back. No other file of the arcs says any of her sentences.
 */
class WitchDialogueTest {
    private static final Path DIR = Path.of("src/main/resources/data/nusmp/arc/dialogue");
    private static final Path FILE = DIR.resolve("black_market_witch.json");
    private static final List<String> SAGAS = List.of(ArcSaga.ELF_REINCARNATION.id(), ArcSaga.SPADE_RAID.id(), ArcSaga.WIZARD_KING.id(), ArcSaga.SEVEN_DEADLY_SINS.id(), ArcSaga.DANMACHI.id());

    private static DialogueBook book() throws IOException {
        DialogueBook.Parsed p = DialogueBook.parse("black_market_witch", Files.readString(FILE, StandardCharsets.UTF_8));
        assertTrue(p.ok(), "the file parses");
        assertTrue(p.problems().isEmpty(), "problems: " + p.problems());
        return p.book();
    }

    private static List<DialogueSituation> situations(boolean first) {
        List<DialogueSituation> out = new ArrayList<>();
        for (int stage = 0; stage <= 22; stage++) {
            for (String saga : SAGAS) {
                out.add(DialogueSituation.of(stage, saga, stage >= 1 ? List.of("revchi_salik") : List.of(), 0, first));
                out.add(DialogueSituation.of(stage, saga, List.of(), 0, first));
            }
        }
        return out;
    }

    @Test
    void theFileParsesCleanAndHasThirtyLines() throws IOException {
        DialogueBook b = book();
        assertEquals(30, b.size(), "the owner asked for 20 to 30 lines");
        assertFalse(b.lines(DialogueContext.GREETING).isEmpty() || b.lines(DialogueContext.HINT).isEmpty() || b.lines(DialogueContext.SHOP).isEmpty() || b.lines(DialogueContext.FAREWELL).isEmpty());
        assertTrue(b.lines(DialogueContext.QUEST_OFFER).isEmpty() && b.lines(DialogueContext.QUEST_DONE).isEmpty(), "no quest system exists for her: a quest line would offer a quest nobody can finish");
    }

    @Test
    void everyLineIsKeyedAsciiAndAReadableLength() throws IOException {
        DialogueBook b = book();
        int n = 0;
        for (DialogueLine l : b.all()) {
            n++;
            assertEquals("arc.dialogue.black_market_witch." + n, l.key(), "line " + n);
            assertTrue(l.text().length() >= 12 && l.text().length() <= 260, "line " + n + " is " + l.text().length() + " characters");
            for (char ch : l.text().toCharArray()) assertTrue(ch >= 32 && ch < 127, "line " + n + " has a non-ASCII character: " + (int) ch);
        }
    }

    @Test
    void aNewcomerIsAlwaysGreetedAndAfterwardsAlwaysHasAHint() throws IOException {
        DialogueBook b = book();
        for (DialogueSituation s : situations(true)) assertTrue(b.has(DialogueContext.GREETING, s), "no greeting at stage " + s.stage() + " in " + s.saga());
        for (DialogueSituation s : situations(false)) {
            assertTrue(b.has(DialogueContext.HINT, s), "no hint at stage " + s.stage() + " in " + s.saga());
            assertNotNull(TalkPlan.next(false, 0, c -> b.has(c, s)), "the talk plan finds a topic");
        }
        int unconditionalFirst = 0;
        for (DialogueLine l : b.lines(DialogueContext.GREETING)) if (l.minStage() == DialogueLine.NO_MIN && l.defeated().isEmpty() && l.first() == DialogueLine.FIRST_ONLY) unconditionalFirst++;
        assertTrue(unconditionalFirst >= 4, "four plain greetings for a newcomer");
    }

    @Test
    void theShopAndTheFarewellHaveLinesForEveryone() throws IOException {
        DialogueBook b = book();
        for (DialogueSituation s : situations(false)) {
            assertTrue(b.has(DialogueContext.SHOP, s), "no shop line at stage " + s.stage());
            assertTrue(b.has(DialogueContext.FAREWELL, s), "no farewell at stage " + s.stage());
        }
        assertTrue(b.lines(DialogueContext.SHOP).size() >= 5 && b.lines(DialogueContext.FAREWELL).size() >= 3);
        for (DialogueLine l : b.lines(DialogueContext.FAREWELL)) {
            String t = l.text().toLowerCase(java.util.Locale.ROOT);
            assertFalse(t.contains("come again") || t.contains("goodbye") || t.contains("farewell"), "a farewell is also said a few seconds after the LAST deal while the screen may still be open, so it must not be a plain goodbye: " + l.text());
        }
    }

    @Test
    void theWitchTellsPlayersToSneakAndNamesTheFang() throws IOException {
        DialogueBook b = book();
        int sneakWeight = 0, total = 0;
        boolean fang = false;
        for (DialogueLine l : b.lines(DialogueContext.HINT)) {
            total += l.weight();
            if (l.text().toLowerCase(java.util.Locale.ROOT).contains("sneak") && l.minStage() == DialogueLine.NO_MIN && l.sagas().isEmpty()) sneakWeight += l.weight();
            if (l.text().contains("fang that smoulders")) fang = true;
        }
        assertTrue(sneakWeight >= 6, "hints that say how to open the shop carry weight " + sneakWeight + " of " + total);
        assertTrue(fang, "the owner's hint: a fang that smoulders ... for the right price");
        // and the first greetings tell a newcomer too
        int greetSneak = 0;
        for (DialogueLine l : b.lines(DialogueContext.GREETING)) if (l.text().toLowerCase(java.util.Locale.ROOT).contains("sneak")) greetSneak++;
        assertTrue(greetSneak >= 3, "most greetings point the way: " + greetSneak);
    }

    @Test
    void conditionsOnlyNameARealBossAndLineNeverQuoteNumbers() throws IOException {
        String early = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/arc/early/EarlyRoster.java"), StandardCharsets.UTF_8);
        DialogueBook b = book();
        for (DialogueLine l : b.all()) {
            for (String id : l.defeated()) assertTrue(early.contains("ArcCharacter.boss(\"" + id + "\""), "a condition names a boss that does not exist: " + id);
            for (String id : l.notDefeated()) assertTrue(early.contains("ArcCharacter.boss(\"" + id + "\""), id);
            for (String s : l.sagas()) assertTrue(SAGAS.contains(s), s);
            for (char ch : l.text().toCharArray()) assertFalse(ch >= '0' && ch <= '9', "a price or an odds number goes stale when the data is retuned: " + l.text());
        }
    }

    @Test
    void aLongConversationMixesItsHintsAndNeverRepeatsOneBackToBack() throws IOException {
        DialogueBook b = book();
        DialogueSituation s = DialogueSituation.of(4, ArcSaga.ELF_REINCARNATION.id(), List.of("revchi_salik"), 0, false);
        Random rnd = new Random(7);
        Set<Integer> used = new HashSet<>();
        int last = -1;
        for (int i = 0; i < 400; i++) {
            DialogueBook.Pick p = b.pick(DialogueContext.HINT, s, last, rnd::nextInt);
            assertNotNull(p);
            assertTrue(p.index() != last, "the same hint twice in a row at draw " + i);
            used.add(p.index());
            last = p.index();
        }
        assertTrue(used.size() >= 9, "only " + used.size() + " different hints in 400 draws");
        assertNull(b.pick(DialogueContext.QUEST_OFFER, s, -1, rnd::nextInt));
    }

    @Test
    void noOtherCharacterSaysAnyOfHerSentences() throws IOException {
        Set<String> mine = new HashSet<>();
        for (DialogueLine l : book().all()) mine.add(l.text());
        try (Stream<Path> files = Files.list(DIR)) {
            for (Path p : files.filter(f -> f.getFileName().toString().endsWith(".json") && !f.equals(FILE)).sorted().toList()) {
                DialogueBook other = DialogueBook.parse("x", Files.readString(p, StandardCharsets.UTF_8)).book();
                if (other == null) continue;
                for (DialogueLine l : other.all()) assertFalse(mine.contains(l.text()), p.getFileName() + " repeats one of her lines: " + l.text());
            }
        }
    }

    @Test
    void noForbiddenWordsAppearInHerLines() throws IOException {
        String all = Files.readString(FILE, StandardCharsets.UTF_8).toLowerCase(java.util.Locale.ROOT);
        for (String bad : new String[]{"language model", "as an ai", "assistant"}) assertFalse(all.contains(bad), bad);
    }
}
