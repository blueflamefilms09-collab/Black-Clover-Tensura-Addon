package com.newuniverse.nusmp.arc.npc;

import com.newuniverse.nusmp.arc.ArcCharacter;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: what the fourteen Black Bulls say is data, so a typo in it fails silently in the game (a condition that can never hold, a story id that does not exist,
 * a page that was renamed, a line that makes every hint the same). These tests read the real files and check them with the real {@link DialogueBook}: they all
 * parse, every line is keyed and reachable, a newcomer is always greeted, every conversation has guidance for every saga, the role lines name pages that exist
 * in the source, and a long conversation mixes lines without repeating one twice in a row. The rules they enforce are written in {@code docs/arc/npca.md}.
 */
class BlackBullsDialogueTest {
    private static final Path DIR = Path.of("src/main/resources/data/nusmp/arc/dialogue");
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final List<String> SOURCE_ROOTS = List.of("book", "seal", "shadow", "blackclover", "thread", "grimoire", "labyrinth");

    /** The characters the lines may mention in a condition: the early saga, Zagred (his own builder) and the Zogratis. A typo cannot hide among them. */
    private static final Set<String> STORY_IDS = Set.of("revchi_salik", "heath_grice", "lotus_whomalt", "mars", "vetto", "zagred", "dante", "vanica", "zenon", "lucius");
    private static final List<String> SAGAS = List.of(ArcSaga.ELF_REINCARNATION.id(), ArcSaga.SPADE_RAID.id(), ArcSaga.WIZARD_KING.id(), ArcSaga.SEVEN_DEADLY_SINS.id(), ArcSaga.DANMACHI.id());

    /**
     * Floors the lines may name. Each one is a claim about the dungeon and is listed, with its source, in docs/arc/npca.md section 5: "first" is the floor whose
     * clearing closes the other road (the Directional Lock), the rest are where the early bosses stand.
     */
    private static final Set<String> DOCUMENTED_FLOORS = Set.of("first", "tenth", "thirtieth", "fortieth");

    /** Pages and places the role lines talk about, per friend. Each must be said by that friend AND exist in the source (so a renamed page breaks this test). */
    private static final Map<String, List<String>> TERMS = new LinkedHashMap<>();
    static {
        TERMS.put("yami_sukehiro", List.of("Equinox"));
        TERMS.put("asta", List.of("Anti-Magic Power", "Demon-Slasher", "Demon-Dweller", "Demon-Destroyer", "Black Asta"));
        TERMS.put("noelle_silva", List.of("Sea Dragon's Roar", "Valkyrie Dress", "Sea Dragon's Nest"));
        TERMS.put("finral_roulacase", List.of("Fallen Angel Gate", "Sacred Mana Domination"));
        TERMS.put("magna_swing", List.of("Exploding Fireball", "Leo Rugiens", "Calidos Brachium"));
        TERMS.put("luck_voltia", List.of("Thunder Fiend", "Black Lightning Battle Fiend", "Chain Lightning"));
        TERMS.put("charmy_pappitson", List.of("Sheep Cook", "Sleeping Sheep Strike", "Sheep Bondage", "Cotton Cloud", "Glutton's Banquet"));
        TERMS.put("vanessa_enoteca", List.of("Red Thread", "Rouge", "Arresting Dark Weave", "Thread Trap", "Landing"));
        TERMS.put("gordon_agrippa", List.of("Aufwachen Dachs", "Violett Schirm", "Basilisk's Breath", "Curse-Worker's Neighbor"));
        TERMS.put("zora_ideale", List.of("Ash Bullets", "Ash Cloud", "Revelation of the Cowardly", "Ash Absorbing Formation"));
        TERMS.put("gauche_adlai", List.of("Reflect Refrain", "Real Double", "Full Reflection", "Mirror Array"));
        TERMS.put("grey", List.of("Magic Convert", "Iron Spikes", "Quagmire", "Grand Transmutation", "Landing"));
        TERMS.put("secre_swallowtail", List.of("Branching Array", "Eternal Prison", "Wound Sealing", "Orbital Bind", "Mana Core", "Crucible"));
        TERMS.put("nacht_faust", List.of("Unite Mode", "Shadow Travel", "Devil Union"));
    }

    private static String sourceText;

    // ================================================================ reading
    private static DialogueBook book(String id) throws IOException {
        DialogueBook.Parsed p = DialogueBook.parse(id, Files.readString(DIR.resolve(id + ".json"), StandardCharsets.UTF_8));
        assertTrue(p.ok(), id + ": " + p.problems());
        assertTrue(p.problems().isEmpty(), id + ": " + p.problems());
        return p.book();
    }

    private static List<String> ids() {
        List<String> out = new ArrayList<>();
        for (ArcCharacter c : BlackBullsNpcs.all()) out.add(c.id());
        return out;
    }

    private static String allText(DialogueBook b) {
        StringBuilder sb = new StringBuilder();
        for (DialogueLine l : b.all()) sb.append(l.text()).append('\n');
        return sb.toString();
    }

    private static synchronized String source() throws IOException {
        if (sourceText == null) {
            StringBuilder sb = new StringBuilder();
            for (String root : SOURCE_ROOTS) {
                try (Stream<Path> s = Files.walk(SRC.resolve(root))) {
                    for (Path p : (Iterable<Path>) s.filter(x -> x.toString().endsWith(".java"))::iterator) sb.append(Files.readString(p, StandardCharsets.UTF_8)).append('\n');
                }
            }
            sourceText = sb.toString();
        }
        return sourceText;
    }

    private static List<DialogueLine> hints(DialogueBook b) { return b.lines(DialogueContext.HINT); }

    private static DialogueSituation at(int stage, String saga, List<String> defeated, boolean first) {
        return DialogueSituation.of(stage, saga, defeated, 0, first);
    }

    // ================================================================ the files
    @Test
    void everyFriendHasAFileThatParsesCleanly() throws IOException {
        for (String id : ids()) {
            DialogueBook b = book(id);
            assertEquals(id, b.characterId());
            assertTrue(b.size() >= 28, id + " has only " + b.size() + " lines");
        }
    }

    @Test
    void everyLineIsKeyedPlainTextOfASaneLength() throws IOException {
        Pattern bad = Pattern.compile("[^\\x20-\\x7E]");
        for (String id : ids()) {
            DialogueBook b = book(id);
            List<DialogueLine> all = b.all();
            for (int i = 0; i < all.size(); i++) {
                DialogueLine l = all.get(i);
                assertEquals("arc.dialogue." + id + "." + (i + 1), l.key(), id + " line " + (i + 1) + " is keyed by the generator's numbering");
                assertFalse(bad.matcher(l.text()).find(), id + ": plain ASCII only: " + l.text());
                assertTrue(l.text().length() >= 12 && l.text().length() <= 260, id + ": " + l.text().length() + " characters: " + l.text());
                assertFalse(l.text().contains("\"") || l.text().contains("%"), id + ": " + l.text());
                assertEquals(l.text().trim(), l.text(), id + ": stray blanks");
            }
        }
    }

    @Test
    void noQuestOrShopLinesAreShippedBecauseNothingCompletesAQuestOrOpensAShop() throws IOException {
        for (String id : ids()) {
            DialogueBook b = book(id);
            assertTrue(b.lines(DialogueContext.QUEST_OFFER).isEmpty(), id + ": an offered quest would stay offered for ever (TalkPlan marks it offered)");
            assertTrue(b.lines(DialogueContext.QUEST_DONE).isEmpty(), id);
            assertTrue(b.lines(DialogueContext.SHOP).isEmpty(), id);
        }
    }

    // ================================================================ greeting, hints, farewell
    @Test
    void aNewcomerIsGreetedWhateverTheirStageAndSaga() throws IOException {
        int[] stages = {0, 1, 3, 8, 15, 26};
        for (String id : ids()) {
            DialogueBook b = book(id);
            int firstOnly = 0;
            for (DialogueLine l : b.lines(DialogueContext.GREETING)) if (l.first() == DialogueLine.FIRST_ONLY) firstOnly++;
            assertTrue(firstOnly >= 4, id + " has " + firstOnly + " first-meeting greetings, the brief asks for 4 or more");
            for (String saga : SAGAS) {
                for (int stage : stages) {
                    DialogueSituation s = at(stage, saga, List.of(), true);
                    assertTrue(b.has(DialogueContext.GREETING, s), id + " cannot greet a newcomer at stage " + stage + " in " + saga);
                    assertEquals(DialogueContext.GREETING, TalkPlan.next(true, 0, c -> b.has(c, s)), id);
                }
            }
        }
    }

    @Test
    void afterTheGreetingTheNextConversationIsAHintWithPlentyToChooseFrom() throws IOException {
        int[] stages = {0, 1, 2, 3, 5, 8, 12, 20, 26};
        List<String> everyone = new ArrayList<>(STORY_IDS);
        for (String id : ids()) {
            DialogueBook b = book(id);
            for (String saga : SAGAS) {
                for (int stage : stages) {
                    for (List<String> defeated : List.of(List.<String>of(), everyone)) {
                        DialogueSituation s = at(stage, saga, defeated, false);
                        assertEquals(DialogueContext.HINT, TalkPlan.next(false, 0, c -> b.has(c, s)), id + " stage " + stage + " " + saga);
                        int fit = 0;
                        for (DialogueLine l : hints(b)) if (l.matches(s)) fit++;
                        assertTrue(fit >= 8, id + " has only " + fit + " hints at stage " + stage + " in " + saga + " (defeated " + defeated.size() + ")");
                    }
                }
            }
        }
    }

    @Test
    void everySagaHasGuidanceAndTheEarlySagaHasTwoSteps() throws IOException {
        for (String id : ids()) {
            DialogueBook b = book(id);
            for (String saga : SAGAS) {
                boolean found = false;
                for (DialogueLine l : hints(b)) if (l.sagas().contains(saga)) found = true;
                assertTrue(found, id + " says nothing about " + saga);
            }
            DialogueSituation early = at(0, ArcSaga.ELF_REINCARNATION.id(), List.of(), false);
            DialogueSituation deep = at(8, ArcSaga.ELF_REINCARNATION.id(), List.of(), false);
            assertTrue(guidance(b, early) >= 1, id + " has no pointer for a player at the start of the early saga");
            assertTrue(guidance(b, deep) >= 1, id + " has no pointer for a player deep in the early saga");
            boolean veteran = false;
            for (DialogueLine l : hints(b)) if (l.minStage() >= 20 && l.sagas().isEmpty()) veteran = true;
            assertTrue(veteran, id + " has no word for a veteran");
        }
    }

    private static int guidance(DialogueBook b, DialogueSituation s) {
        int n = 0;
        for (DialogueLine l : hints(b)) if (!l.sagas().isEmpty() && l.matches(s)) n++;
        return n;
    }

    @Test
    void everyFriendSaysFarewell() throws IOException {
        for (String id : ids()) {
            DialogueBook b = book(id);
            List<DialogueLine> f = b.lines(DialogueContext.FAREWELL);
            assertTrue(f.size() >= 3, id + " has " + f.size() + " farewells");
            for (DialogueLine l : f) assertTrue(l.matches(at(0, ArcSaga.ELF_REINCARNATION.id(), List.of(), false)), id + " farewell is conditional: " + l.text());
        }
    }

    @Test
    void theGateBossOfAFriendIsAnsweredWhenTheyMeetTheLatePlayer() throws IOException {
        for (ArcCharacter c : BlackBullsNpcs.all()) {
            if (c.prerequisites().isEmpty()) continue;
            DialogueBook b = book(c.id());
            String gate = c.prerequisites().get(0);
            boolean answered = false;
            for (DialogueLine l : b.lines(DialogueContext.GREETING)) if (l.first() == DialogueLine.FIRST_ONLY && l.defeated().contains(gate)) answered = true;
            assertTrue(answered, c.id() + " arrives after " + gate + " but never mentions it in a first-meeting greeting");
        }
    }

    // ================================================================ conditions and reach
    @Test
    void conditionsNameOnlyRealStoryIdsAndSagas() throws IOException {
        for (String id : ids()) {
            DialogueBook b = book(id);
            for (DialogueLine l : b.all()) {
                for (String d : l.defeated()) assertTrue(STORY_IDS.contains(d), id + " requires an unknown character '" + d + "' in: " + l.text());
                for (String d : l.notDefeated()) assertTrue(STORY_IDS.contains(d), id + " forbids an unknown character '" + d + "' in: " + l.text());
                for (String s : l.sagas()) assertTrue(SAGAS.contains(s), id + " names an unknown saga '" + s + "' in: " + l.text());
            }
        }
    }

    @Test
    void everyLineCanBeSaidInSomeSituation() throws IOException {
        for (String id : ids()) {
            DialogueBook b = book(id);
            for (DialogueContext ctx : DialogueContext.values()) {
                for (DialogueLine l : b.lines(ctx)) {
                    int stage = l.minStage() == DialogueLine.NO_MIN ? 0 : Math.max(0, l.minStage());
                    assertTrue(stage <= l.maxStage(), id + ": stage range is empty: " + l.text());
                    String saga = l.sagas().isEmpty() ? ArcSaga.ELF_REINCARNATION.id() : l.sagas().iterator().next();
                    boolean first = l.first() != DialogueLine.FIRST_NEVER;
                    DialogueSituation s = DialogueSituation.of(stage, saga, l.defeated(), 0, first);
                    assertTrue(l.matches(s), id + " has a line that can never be said: " + l.text());
                }
            }
        }
    }

    @Test
    void everyHintMixesAtOnePriorityAndCarriesAWeightOfOneToThree() throws IOException {
        for (String id : ids()) {
            for (DialogueLine l : hints(book(id))) {
                assertEquals(1, l.priority(), id + ": a hint with another priority would silence the others: " + l.text());
                assertTrue(l.weight() >= 1 && l.weight() <= 3, id + ": weight " + l.weight() + ": " + l.text());
            }
            for (DialogueLine l : book(id).lines(DialogueContext.FAREWELL)) assertEquals(0, l.priority(), id);
        }
    }

    @Test
    void aLongConversationMixesLinesAndNeverRepeatsOneBackToBack() throws IOException {
        for (String id : ids()) {
            DialogueBook b = book(id);
            DialogueSituation s = at(3, ArcSaga.ELF_REINCARNATION.id(), List.of(BlackBullsNpcs.GATE_1, BlackBullsNpcs.GATE_2), false);
            Random rnd = new Random(5);
            Set<Integer> seen = new HashSet<>();
            int last = -1;
            for (int i = 0; i < 400; i++) {
                DialogueBook.Pick p = b.pick(DialogueContext.HINT, s, last, rnd::nextInt);
                assertNotNull(p, id);
                assertNotEquals(last, p.index());
                seen.add(p.index());
                last = p.index();
            }
            assertTrue(seen.size() >= 10, id + " only ever says " + seen.size() + " different hints at this point of the story");
        }
    }

    @Test
    void aReactionToABossIsHeardOnlyOnceThatBossHasFallen() throws IOException {
        for (String id : ids()) {
            DialogueBook b = book(id);
            DialogueSituation none = at(10, ArcSaga.ELF_REINCARNATION.id(), List.of(), false);
            DialogueSituation all = at(10, ArcSaga.ELF_REINCARNATION.id(), new ArrayList<>(STORY_IDS), false);
            int reactions = 0;
            for (DialogueLine l : hints(b)) {
                if (l.defeated().isEmpty()) continue;
                reactions++;
                assertFalse(l.matches(none), id + ": a reaction that fires before the boss has fallen: " + l.text());
                assertTrue(l.matches(all), id + ": a reaction that does not fire after every boss has fallen: " + l.text());
            }
            assertTrue(reactions >= 3, id + " has only " + reactions + " reactions to the player's victories");
        }
    }

    // ================================================================ what the lines claim about the game
    @Test
    void theRoleLinesNameRealPagesAndPlaces() throws IOException {
        String src = source();
        for (Map.Entry<String, List<String>> e : TERMS.entrySet()) {
            String said = allText(book(e.getKey()));
            for (String term : e.getValue()) {
                assertTrue(said.contains(term), e.getKey() + " no longer mentions " + term);
                assertTrue(src.contains(term), e.getKey() + " talks about '" + term + "' but the source has no such page or place any more");
            }
        }
        assertEquals(ids().size(), TERMS.size(), "every friend has a list of terms");
    }

    @Test
    void floorsNamedInTheLinesAreTheDocumentedOnes() throws IOException {
        Pattern ordinal = Pattern.compile("(?i)\\b(first|second|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth|twentieth|thirtieth|fortieth|fiftieth|sixtieth|seventieth|eightieth|ninetieth|hundredth)\\s+floor");
        Pattern digits = Pattern.compile("(?i)\\bfloors?\\s+\\d|\\d+(st|nd|rd|th)\\s+floor");
        Set<String> named = new HashSet<>();
        for (String id : ids()) {
            for (DialogueLine l : book(id).all()) {
                Matcher m = ordinal.matcher(l.text());
                while (m.find()) named.add(m.group(1).toLowerCase(java.util.Locale.ROOT));
                assertFalse(digits.matcher(l.text()).find(), id + ": write floors as words so this test can list them: " + l.text());
            }
        }
        assertTrue(DOCUMENTED_FLOORS.containsAll(named), "undocumented floors named: " + named);
        assertTrue(named.containsAll(DOCUMENTED_FLOORS), "a documented floor is no longer used: " + named);
    }

    @Test
    void noTextRepeatsAcrossFriendsAndNothingForbiddenIsWritten() throws IOException {
        Set<String> seen = new HashSet<>();
        Pattern forbidden = Pattern.compile("(?i)todo|fixme|lorem|placeholder|\\btbd\\b");
        for (String id : ids()) {
            for (DialogueLine l : book(id).all()) {
                assertTrue(seen.add(l.text()), "the same words twice: " + l.text());
                assertFalse(forbidden.matcher(l.text()).find(), id + ": " + l.text());
            }
        }
    }
}
