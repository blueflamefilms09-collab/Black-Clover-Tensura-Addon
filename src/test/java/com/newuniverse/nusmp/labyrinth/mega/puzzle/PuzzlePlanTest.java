package com.newuniverse.nusmp.labyrinth.mega.puzzle;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the choice among the old and the new puzzles by theme and depth is a pure function of the weights file, so it can be pinned without a game: the shipped file parses without a single
 * problem and names every puzzle; the plan is the same for the same seed and caps and keeps its rules (a Spire sigil room never gets a new puzzle, a busy room never does, a marker room
 * always does, a puzzle never appears outside its tiers, the number of puzzles on a floor is capped); a theme that likes a puzzle draws it more often than a theme that dislikes it; the old
 * puzzles keep the share the file gives them; and every word the puzzles can say has an English text and no text carries a banned name.
 */
class PuzzlePlanTest {
    private static final Path FILE = Path.of("src/main/resources/data/nusmp/mega/puzzles/weights.json");

    private static PuzzleWeights shipped() throws IOException {
        List<String> problems = new ArrayList<>();
        PuzzleWeights w = PuzzleWeights.parse(Files.readString(FILE, StandardCharsets.UTF_8), problems);
        assertTrue(problems.isEmpty(), "problems in weights.json: " + problems);
        return w;
    }

    private static PuzzlePlanner.FloorInfo floor(int floor, long seed, String theme, boolean abyssal, List<PuzzlePlanner.RoomInfo> rooms) {
        return new PuzzlePlanner.FloorInfo(floor, abyssal, theme, seed, false, rooms);
    }

    private static List<PuzzlePlanner.RoomInfo> rooms(String kind, int n, String template) {
        List<PuzzlePlanner.RoomInfo> out = new ArrayList<>();
        for (int i = 0; i < n; i++) out.add(new PuzzlePlanner.RoomInfo(i, kind, template, 20, true, false, false, ""));
        return out;
    }

    @Test
    void theShippedWeightsParseCleanlyAndNameEveryPuzzleAndTheme() throws IOException {
        PuzzleWeights w = shipped();
        assertTrue(w.enabled);
        assertEquals(PuzzleCatalog.ids().size(), w.types.size());
        for (String id : PuzzleCatalog.ids()) {
            PuzzleWeights.TypeW t = w.types.get(id);
            assertNotNull(t, id);
            assertTrue(t.weight() > 0 && t.tierLo() >= 1 && t.tierHi() <= 5 && t.tierLo() <= t.tierHi(), id);
        }
        Set<String> themes = new HashSet<>();
        try (var dir = Files.list(Path.of("src/main/resources/data/nusmp/mega/themes"))) {
            dir.forEach(f -> themes.add(f.getFileName().toString().replace(".json", "")));
        }
        assertTrue(themes.size() >= 27);
        for (String th : themes) {
            assertTrue(w.themeTags.containsKey(th), "no flavour tags for theme " + th);
            assertNotNull(PuzzleText.en("theme." + th), "no flavour line for theme " + th);
        }
        // every tier has a decent menu
        for (int tier = 1; tier <= 5; tier++) {
            int n = 0;
            for (PuzzleWeights.TypeW t : w.types.values()) if (tier >= t.tierLo() && tier <= t.tierHi()) n++;
            assertTrue(n >= 18, "tier " + tier + " has only " + n + " puzzles");
        }
    }

    @Test
    void aBrokenFileNeverBreaksTheGame() {
        List<String> problems = new ArrayList<>();
        PuzzleWeights w = PuzzleWeights.parse("[1,2", problems);
        assertFalse(problems.isEmpty());
        assertEquals(PuzzleCatalog.ids().size(), w.types.size());
        problems.clear();
        w = PuzzleWeights.parse("{\"types\": {\"nonsense\": {\"weight\": 5}, \"lights_out\": {\"weight\": \"heavy\"}}, \"maxPerFloor\": \"many\"}", problems);
        assertFalse(problems.isEmpty());
        assertEquals(PuzzleCatalog.ids().size(), w.types.size());
        assertTrue(w.maxPerFloor >= 0);
    }

    @Test
    void thePlanIsAPureFunctionOfTheSeedAndKeepsItsCaps() throws IOException {
        PuzzleWeights w = shipped();
        for (int i = 0; i < 80; i++) {
            PuzzlePlanner.FloorInfo f = floor(1 + i * 12, 900L + i, "necropolis", i % 2 == 0, rooms("hall", 14, "nusmp:mega/hall/crypt_a"));
            List<PuzzlePlanner.Slot> a = PuzzlePlanner.plan(f, w), b = PuzzlePlanner.plan(f, w);
            assertEquals(a, b);
            assertTrue(a.size() <= w.maxPerFloor, "cap");
            Set<Integer> roomsUsed = new HashSet<>();
            for (PuzzlePlanner.Slot s : a) { assertTrue(roomsUsed.add(s.roomId()), "two puzzles in one room"); assertTrue(PuzzleCatalog.has(s.type())); }
        }
    }

    @Test
    void sigilRoomsAndBusyRoomsAndTinyRoomsAreLeftToTheOldPuzzles() throws IOException {
        PuzzleWeights w = shipped();
        for (int i = 0; i < 200; i++) {
            List<PuzzlePlanner.RoomInfo> rs = new ArrayList<>();
            rs.add(new PuzzlePlanner.RoomInfo(0, "puzzle", "nusmp:mega/puzzle/sigil_hall_a", 20, true, false, false, ""));
            rs.add(new PuzzlePlanner.RoomInfo(1, "hall", "nusmp:mega/hall/crypt_a", 20, true, true, false, ""));
            rs.add(new PuzzlePlanner.RoomInfo(2, "hall", "nusmp:mega/hall/crypt_b", 9, true, false, false, ""));
            rs.add(new PuzzlePlanner.RoomInfo(3, "safe", "nusmp:mega/safe/hearth_hall", 20, false, false, false, ""));
            rs.add(new PuzzlePlanner.RoomInfo(4, "puzzle", "nusmp:mega/puzzle/confessional", 20, true, false, false, ""));
            for (PuzzlePlanner.Slot s : PuzzlePlanner.plan(floor(10, i, "holy_church", false, rs), w)) fail("a protected room got a puzzle: " + s);
        }
    }

    private static void fail(String m) { assertTrue(false, m); }

    @Test
    void aMarkerRoomAlwaysGetsAPuzzleAndTheMarkerMayNameIt() throws IOException {
        PuzzleWeights w = shipped();
        for (int i = 0; i < 100; i++) {
            List<PuzzlePlanner.RoomInfo> rs = new ArrayList<>();
            rs.add(new PuzzlePlanner.RoomInfo(0, "trial", "nusmp:mega/trial/x", 20, true, false, true, ""));
            rs.add(new PuzzlePlanner.RoomInfo(1, "hall", "nusmp:mega/hall/y", 20, true, false, true, "lights_out"));
            List<PuzzlePlanner.Slot> plan = PuzzlePlanner.plan(floor(30, i, "frost", false, rs), w);
            assertEquals(2, plan.size());
            for (PuzzlePlanner.Slot s : plan) if (s.roomId() == 1) assertEquals("lights_out", s.type());
        }
    }

    @Test
    void aPuzzleNeverAppearsOutsideItsTiersAndTheOpenGalleryHoldsOneOrLeavesTheRoomToTheOldOnes() throws IOException {
        PuzzleWeights w = shipped();
        int none = 0, some = 0;
        for (int i = 0; i < 600; i++) {
            int floor = 1 + (i % 5) * 200 + 5;
            int tier = (floor - 1) / 200 + 1;
            for (PuzzlePlanner.Slot s : PuzzlePlanner.plan(floor(floor, 4000L + i, "radiant", true, rooms("puzzle", 2, "nusmp:mega/puzzle/gallery_a")), w)) {
                PuzzleWeights.TypeW t = w.types.get(s.type());
                assertTrue(tier >= t.tierLo() && tier <= t.tierHi(), s.type() + " at tier " + tier);
                some++;
            }
            if (PuzzlePlanner.plan(floor(floor, 4000L + i, "radiant", true, rooms("puzzle", 2, "nusmp:mega/puzzle/gallery_a")), w).size() < 2) none++;
        }
        assertTrue(some > 100, "open galleries should usually hold a puzzle: " + some);
        assertTrue(none > 0, "the old puzzles should win some draws (legacy weight)");
    }

    @Test
    void theOldPuzzlesKeepTheShareTheFileGivesThem() throws IOException {
        PuzzleWeights w = shipped();
        double legacy = w.legacy.getOrDefault("puzzle", 0.0);
        int rooms = 0, got = 0;
        for (int i = 0; i < 2000; i++) {
            List<PuzzlePlanner.RoomInfo> rs = List.of(new PuzzlePlanner.RoomInfo(0, "puzzle", "nusmp:mega/puzzle/gallery_a", 20, true, false, false, ""));
            rooms++;
            got += PuzzlePlanner.plan(floor(40, 100L + i, "frost", false, rs), w).size();
        }
        double total = 0;
        for (String id : PuzzleCatalog.ids()) total += w.weightOf(w.types.get(id), 1, false, "frost", "puzzle", 0, 0);
        double expected = total / (total + legacy);
        assertTrue(Math.abs(got / (double) rooms - expected) < 0.05, "new share " + got / (double) rooms + " vs expected " + expected);
    }

    @Test
    void aThemeThatLikesAPuzzleDrawsItMoreOftenThanOneThatDislikesIt() throws IOException {
        PuzzleWeights w = shipped();
        // sundial likes sky/holy/mind and dislikes grave; torch_order likes fire/grave and dislikes water
        Map<String, Integer> sky = new HashMap<>(), grave = new HashMap<>();
        for (int i = 0; i < 3000; i++) {
            for (PuzzlePlanner.Slot s : PuzzlePlanner.plan(floor(50, 10L + i, "sky_citadel", false, rooms("hall", 1, "nusmp:mega/hall/a")), w)) sky.merge(s.type(), 1, Integer::sum);
            for (PuzzlePlanner.Slot s : PuzzlePlanner.plan(floor(50, 10L + i, "necropolis", false, rooms("hall", 1, "nusmp:mega/hall/a")), w)) grave.merge(s.type(), 1, Integer::sum);
        }
        assertTrue(sky.getOrDefault("sundial", 0) > grave.getOrDefault("sundial", 0) * 1.5, "sundial: sky " + sky.get("sundial") + " grave " + grave.get("sundial"));
        assertTrue(grave.getOrDefault("torch_order", 0) > sky.getOrDefault("torch_order", 0) * 1.3, "torch order: grave " + grave.get("torch_order") + " sky " + sky.get("torch_order"));
    }

    @Test
    void depthChangesTheMenu() throws IOException {
        PuzzleWeights w = shipped();
        Set<String> shallow = new HashSet<>(), deep = new HashSet<>();
        for (int i = 0; i < 4000; i++) {
            for (PuzzlePlanner.Slot s : PuzzlePlanner.plan(floor(3, i, "storm", false, rooms("hall", 2, "nusmp:mega/hall/a")), w)) shallow.add(s.type());
            for (PuzzlePlanner.Slot s : PuzzlePlanner.plan(floor(950, i, "storm", false, rooms("hall", 2, "nusmp:mega/hall/a")), w)) deep.add(s.type());
        }
        assertFalse(shallow.contains("chain_reaction"));
        assertFalse(shallow.contains("pendulum_crossing"));
        assertTrue(deep.contains("chain_reaction") && deep.contains("pendulum_crossing"));
        assertTrue(shallow.size() >= 15 && deep.size() >= 20, shallow.size() + " / " + deep.size());
    }

    @Test
    void switchingThePuzzlesOffPlansNothing() {
        List<String> problems = new ArrayList<>();
        PuzzleWeights off = PuzzleWeights.parse("{\"enabled\": false}", problems);
        assertTrue(PuzzlePlanner.plan(floor(10, 1, "frost", false, rooms("hall", 10, "nusmp:mega/hall/a")), off).isEmpty());
    }

    // ---------------------------------------------------------------- the words
    @Test
    void everyLineAPuzzleCanSayHasAnEnglishText() {
        for (String id : PuzzleCatalog.ids()) {
            assertNotNull(PuzzleText.en(id + ".name"), id);
            assertNotNull(PuzzleText.en(id + ".hint"), id);
            for (int party : new int[] {1, 4}) for (int tier = 1; tier <= 5; tier++) {
                PuzzleRule r = PuzzleCatalog.create(id, new Params(5L * tier + party, party, tier));
                PuzzleSim sim = new PuzzleSim(r);
                sim.garbage(new SplittableRandom(tier * 31L + party), 120);
                for (int rep = 0; rep < 3 && !r.solved(); rep++) { sim.solve(30 + 40 * rep); }
                List<Fx> all = new ArrayList<>(sim.log);
                all.addAll(r.question());
                if (r.layout().total() > 2) for (int c = 0; c < r.layout().total(); c++) { PuzzleRule q = PuzzleCatalog.create(id, r.params()); all.addAll(q.act(Act.click(0, c, 1))); all.addAll(q.act(Act.sneakClick(0, c, 200))); }
                for (Fx f : all) {
                    if (f.kind() == Fx.Kind.SAY) {
                        assertNotNull(PuzzleText.resolve(id, f.key()), id + " says '" + f.key() + "' with no text");
                        for (String m : f.more()) if (m.startsWith("$")) assertNotNull(PuzzleText.en(m.substring(1)), id + " names '" + m + "' with no text");
                    }
                    if (f.kind() == Fx.Kind.ASK) {
                        assertNotNull(PuzzleText.en(f.key()), "question " + f.key());
                        for (String a : f.more()) assertNotNull(PuzzleText.en(a), "answer " + a);
                    }
                }
            }
        }
    }

    @Test
    void everyRiddleHasItsFourLinesAndEveryTokenItsRiddle() {
        for (String id : Riddles.CHOICE) {
            assertNotNull(PuzzleText.en(Riddles.question(id)), id);
            for (int i = 0; i < 3; i++) assertNotNull(PuzzleText.en(Riddles.answer(id, i)), id + " a" + i);
        }
        for (String t : Tokens.ITEMS) assertNotNull(PuzzleText.en(Riddles.itemQuestion(t)), t);
        assertEquals(Palettes.size("token"), Tokens.ITEMS.size());
        assertEquals(Palettes.size("elem") >= 4 + 6 + 1, true);
    }

    @Test
    void noTextNamesAModelOrAnAssistant() {
        for (Map.Entry<String, String> e : PuzzleText.all().entrySet()) {
            String t = (e.getKey() + " " + e.getValue()).toLowerCase();
            for (String banned : new String[] {"claude", "sonnet", "anthropic", "opus", "haiku", "assistant", "language model", "chatgpt", "gpt-"}) assertFalse(t.contains(banned), e.getKey());
        }
    }

    @Test
    void theLangFragmentOnDiskIsExactlyWhatTheCodeSays() throws IOException {
        Path f = Path.of("tools/lang/puzzles_v2.json");
        assertTrue(Files.exists(f), "tools/lang/puzzles_v2.json is missing; run PuzzleTextDump");
        assertEquals(PuzzleText.json(), Files.readString(f, StandardCharsets.UTF_8));
    }
}
