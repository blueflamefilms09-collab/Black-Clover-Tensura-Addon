package com.newuniverse.nusmp.beastwill;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why (docs/beastwill_canon_rework.md): the canon wills are 27 data files, and a file the validator rejects would silently leave a will out of the game. This reads the real files and pins what the owner
 * asked for: the same 27 ids as the remade wills (so old saves and cores load), every file passes the game's own validator, the rank progression, no two wills with one signature, every ability named, every
 * will has its source line, and the remade and classic folders were left alone. The tests read the repo's files; the working directory is the project root.
 */
class BeastWillCanonDataTest {
    private static final Path CANON = Path.of("src/main/resources/data/anime_multiverse/beast_will_canon");
    private static final Path BASE = Path.of("src/main/resources/data/anime_multiverse/beast_will");
    private static final Path LANG = Path.of("tools/lang/beastwill_canon.json");
    private static final Path EN = Path.of("src/main/resources/assets/anime_multiverse/lang/en_us.json");

    private static Set<String> ids(Path dir) throws IOException {
        Set<String> out = new TreeSet<>();
        try (var s = Files.list(dir)) { s.filter(p -> p.toString().endsWith(".json")).forEach(p -> out.add(p.getFileName().toString().replace(".json", ""))); }
        return out;
    }

    private static BeastWillDef load(String id) throws IOException {
        BeastWillSchema.Parsed p = BeastWillSchema.parse(id, Files.readString(CANON.resolve(id + ".json"), StandardCharsets.UTF_8));
        assertTrue(p.ok(), id + ": " + p.problems());
        return p.def();
    }

    private static List<BeastWillDef> all() throws IOException {
        List<BeastWillDef> out = new ArrayList<>();
        for (String id : ids(CANON)) out.add(load(id));
        return out;
    }

    private static JsonObject json(Path p) throws IOException { return JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject(); }

    @Test
    void theCanonFolderHoldsTheSame27IdsAsTheRemadeWills() throws Exception {
        assertEquals(27, ids(CANON).size());
        assertEquals(ids(BASE), ids(CANON), "a will keeps its id: it is a save key");
    }

    @Test
    void everyCanonWillPassesTheGamesOwnValidator() throws Exception {
        for (BeastWillDef d : all()) assertNull(d.problem(), d.id());
    }

    @Test
    void theIdentityOfAWillIsKeptAndOnlyItsPlayIsReworked() throws Exception {
        for (String id : ids(CANON)) {
            BeastWillDef c = load(id), b = BeastWillSchemaTest.load(id);
            assertEquals(b.name(), c.name(), id + ": the name is a lang key and a save-facing label");
            assertEquals(b.beast(), c.beast(), id);
            if (!id.equals("fenrir_will")) assertEquals(b.affinity(), c.affinity(), id + ": the attunement reads the affinity text");
            assertEquals(b.sources(), c.sources(), id);
            assertEquals(b.keywords(), c.keywords(), id);
            assertEquals(b.tint(), c.tint(), id + ": the core colour is the tint");
            if (!id.equals("fenrir_will")) { assertEquals(b.minRank(), c.minRank(), id); assertEquals(b.legendary(), c.legendary(), id); }
            assertEquals(b.dropChance(), c.dropChance(), 1e-12, id);
            assertEquals(b.assimilationSeconds(), c.assimilationSeconds(), 1e-9, id);
            assertEquals(b.masterySeconds(), c.masterySeconds(), 1e-9, id);
            if (!id.equals("shadow_panther_will")) assertEquals(b.requireEp(), c.requireEp(), 1e-9, id + ": only the AA-class panther asks for more EP");
        }
        assertTrue(load("shadow_panther_will").requireEp() >= BeastWillSchemaTest.load("shadow_panther_will").requireEp(), "the AA panther asks at least as much as before");
    }

    @Test
    void fenrirIsTheOwnersSsClassHelHoundAndOnlyTheCanonLayerChanged() throws Exception {
        BeastWillDef c = load("fenrir_will"), b = BeastWillSchemaTest.load("fenrir_will");
        assertEquals(BeastRank.SS, c.minRank());
        assertTrue(c.minRank().atLeast(b.minRank()), "the remade will asked S; a core of rank SS still opens it with the rework off");
        assertEquals("Spatium / Soul / Ice", c.affinity());
        assertTrue(c.legendary(), "SS wills are legendary");
        assertEquals(0.20, c.acquire().unlockAssimilation(), 1e-9);
        assertEquals(0.70, c.integrate().unlockMastery(), 1e-9);
        assertEquals(List.of("dragon_sight", "ambush"), c.acquire().abilities().stream().map(BeastWillDef.Ability::kind).toList());
        assertEquals(List.of("shadow_dash", "devour"), c.integrate().abilities().stream().map(BeastWillDef.Ability::kind).toList());
        assertEquals(List.of("soul_tether_sight", "jotunn_lunge", "realm_tear", "fang_of_the_void"), List.of(c.ability(BeastWillRules.ACQUIRE, 1).id(), c.ability(BeastWillRules.ACQUIRE, 2).id(), c.ability(BeastWillRules.INTEGRATE, 1).id(), c.ability(BeastWillRules.INTEGRATE, 2).id()));
        assertTrue(c.integrate().drainPct() <= load("dragon_will").integrate().drainPct(), "Fenrir drains no more than the Dragon");
        assertEquals("soul_light_leap", b.ability(BeastWillRules.ACQUIRE, 1).id(), "the remade Fenrir keeps its own abilities");
        JsonObject o = json(CANON.resolve("fenrir_will.json"));
        assertFalse(o.getAsJsonObject("canon").get("source").getAsString().toLowerCase().contains("wikipedia"), "no wiki was fetched for Fenrir");
        assertEquals("DESIGN", o.get("designTag").getAsString());
        assertEquals(json(BASE.resolve("fenrir_will.json")).get("spirit"), o.get("spirit"), "the look is unchanged");
        assertTrue(o.getAsJsonObject("canon").getAsJsonArray("notes").toString().contains("Unbound Assimilation"), "the third phase is written down as not built");
    }

    @Test
    void theProgressionFollowsTheRank() throws Exception {
        for (BeastWillDef d : all()) assertNull(BeastWillCanon.progressionProblem(d), d.id());
        BeastWillDef dragon = load("dragon_will"), lion = load("lion_will"), ammit = load("ammit_will");
        assertEquals(0.20, dragon.acquire().unlockAssimilation(), 1e-9, "SS beast: the first form comes late");
        assertEquals(0.70, dragon.integrate().unlockMastery(), 1e-9);
        assertEquals(0.10, lion.acquire().unlockAssimilation(), 1e-9);
        assertEquals(0.50, lion.integrate().unlockMastery(), 1e-9);
        assertEquals(0.15, ammit.acquire().unlockAssimilation(), 1e-9);
        assertEquals(0.60, ammit.integrate().unlockMastery(), 1e-9);
    }

    @Test
    void noTwoWillsShareASignatureAndNoAbilityIdIsUsedTwice() throws Exception {
        assertEquals(List.of(), BeastWillCanon.signatureProblems(all()));
    }

    @Test
    void theThreeNewKindsAreUsedAndEveryWillHasTwoAbilitiesInEachForm() throws Exception {
        Map<String, Integer> use = new HashMap<>();
        for (BeastWillDef d : all()) {
            assertEquals(2, d.acquire().abilities().size(), d.id());
            assertEquals(2, d.integrate().abilities().size(), d.id());
            for (int phase = 1; phase <= 2; phase++) for (BeastWillDef.Ability a : d.phase(phase).abilities()) use.merge(a.kind(), 1, Integer::sum);
        }
        for (String k : List.of("tail_sweep", "thunder_chain", "devour")) assertTrue(use.getOrDefault(k, 0) >= 2, k + " is used by at least two abilities");
        assertTrue(use.size() >= 15, "the roster uses at least fifteen kinds: " + use.keySet());
    }

    @Test
    void theFourTbateWillsKeepWhatTheCanonSays() throws Exception {
        BeastWillDef dragon = load("dragon_will"), panther = load("shadow_panther_will"), elder = load("elderwood_guardian_will"), lion = load("lion_will");
        assertEquals("time_exclusion", dragon.ability(BeastWillRules.ACQUIRE, 1).kind(), "Static Void is the first form");
        assertEquals("dragon_sight", dragon.ability(BeastWillRules.INTEGRATE, 1).kind(), "Realmheart sees mana and aether");
        assertEquals("maelstrom", dragon.ability(BeastWillRules.INTEGRATE, 2).kind(), "Down Force is gravity");
        assertTrue(BeastWillStealth.quiet(panther), "the panther erases sound in its first form");
        assertEquals("ambush", panther.ability(BeastWillRules.INTEGRATE, 2).kind());
        assertTrue(elder.acquire().regen() > 0 && elder.integrate().regen() > elder.acquire().regen(), "a high speed regeneration that grows");
        assertEquals("verdant_mend", elder.ability(BeastWillRules.INTEGRATE, 2).kind());
        assertEquals("mane_surge", lion.ability(BeastWillRules.ACQUIRE, 1).kind());
        assertEquals("world_howl", lion.ability(BeastWillRules.INTEGRATE, 1).kind());
        assertTrue(lion.ability(BeastWillRules.INTEGRATE, 1).costPct() >= 16, "the World Howl costs a great deal");
        assertTrue(lion.acquire().strainPerSecond() > BeastWillSchemaTest.load("lion_will").acquire().strainPerSecond(), "Curtis barely controls the first form");
    }

    @Test
    void theFireWillsStillIgniteAndTheOthersNoLonger() throws Exception {
        for (String id : List.of("flame_serpent_will", "typhon_will")) {
            BeastWillDef d = load(id);
            BeastWillDef.Ability pulse = null;
            for (int phase = 1; phase <= 2; phase++) for (BeastWillDef.Ability a : d.phase(phase).abilities()) if (a.kind().equals("draconic_pulse")) pulse = a;
            assertNotNull(pulse, id + " keeps a pulse");
            assertTrue(BeastWillCanon.ignites(pulse.power(), d.affinity(), true), id + " is a fire will: its pulse ignites");
        }
        BeastWillDef lion = load("lion_will");
        assertFalse(BeastWillCanon.ignites(lion.ability(BeastWillRules.INTEGRATE, 2).power(), lion.affinity(), true), "the earth lion's paw does not burn");
    }

    @Test
    void everyWillAndEveryNewAbilityHasItsText() throws Exception {
        JsonObject frag = json(LANG), en = json(EN);
        for (String k : frag.keySet()) {
            assertTrue(k.startsWith("anime_multiverse.beastwill."), k);
            if (en.has(k)) assertEquals(en.get(k).getAsString(), frag.get(k).getAsString(), k + " is in en_us with another text: a fragment cannot override it");
            assertFalse(frag.get(k).getAsString().isBlank(), k);
        }
        for (BeastWillDef d : all()) {
            for (String part : List.of("canon_lore", "canon_source")) {
                assertTrue(frag.has(d.langKey(part)), d.id() + " has no " + part);
                assertTrue(frag.get(d.langKey(part)).getAsString().length() <= 400, d.id() + " " + part + " is too long for a tooltip");
            }
            assertTrue(en.has(d.langKey("name")) && en.has(d.langKey("lore")), d.id() + " keeps its name and old lore line for the setting that turns the rework off");
            for (int phase = 1; phase <= 2; phase++) for (BeastWillDef.Ability a : d.phase(phase).abilities()) {
                String key = "anime_multiverse.beastwill.ability." + a.id();
                assertTrue(frag.has(key) || en.has(key), a.id() + " has no name");
            }
        }
        for (int n = 1; n <= 4; n++) assertTrue(frag.has("anime_multiverse.beastwill.milestone." + n), "milestone " + n);
        assertTrue(frag.get("anime_multiverse.beastwill.msg.milestone").getAsString().chars().filter(c -> c == '%').count() == 2, "the milestone line takes the will and the text");
    }

    @Test
    void everyFileCarriesItsSourceTagAndNoDesignClaimsToBeCanon() throws Exception {
        Set<String> tags = Set.of("CANON", "TBATE-KIN", "MYTH", "DESIGN");
        int canon = 0;
        for (String id : ids(CANON)) {
            JsonObject o = json(CANON.resolve(id + ".json"));
            assertTrue(tags.contains(o.get("designTag").getAsString()), id + ": designTag");
            JsonObject c = o.getAsJsonObject("canon");
            assertNotNull(c, id + " has no canon block");
            assertFalse(c.get("source").getAsString().isBlank(), id + ": source");
            assertEquals("en", c.get("language").getAsString(), id + ": the English wiki was the one read");
            JsonArray facts = c.getAsJsonArray("facts");
            assertTrue(facts.size() >= 2, id + ": at least two facts are written down");
            if (o.get("designTag").getAsString().equals("CANON")) canon++;
        }
        assertEquals(4, canon, "Dragon, Shadow Panther, Elderwood Guardian and Lion are the four TBATE wills");
        for (String id : List.of("dragon_will", "shadow_panther_will", "elderwood_guardian_will", "lion_will")) assertEquals("CANON", json(CANON.resolve(id + ".json")).get("designTag").getAsString(), id);
    }

    @Test
    void theRemadeAndClassicFoldersWereLeftAlone() throws Exception {
        // the setting canonRework = false must give back the remade wills exactly: none of them may carry a canon block or a canon-only ability
        for (String id : ids(BASE)) {
            JsonObject o = json(BASE.resolve(id + ".json"));
            assertFalse(o.has("canon"), id + ": the remade will was changed");
            for (String ph : List.of("acquire", "integrate"))
                for (var e : o.getAsJsonObject(ph).getAsJsonArray("abilities"))
                    assertFalse(List.of("tail_sweep", "thunder_chain", "devour").contains(e.getAsJsonObject().get("kind").getAsString()), id + ": a canon kind in the remade will");
        }
        assertEquals(27, ids(Path.of("src/main/resources/data/anime_multiverse/beast_will_classic")).size());
    }
}
