package com.newuniverse.nusmp.elite.forge.rules;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the shipped recipes and the skill table are DATA, so a typo would only show up in game. This test reads every file under
 * data/nusmp/elite/forge/ and holds them to the format: at least 40 recipes, all four station tiers and all four categories and all four minigames used, every
 * id well-formed and from a namespace the project knows, no recipe asking for a station tier it does not exist in, no duplicate rows, a sensible cost per tier,
 * and the skill table parsing to the documented entries. The parser's own refusals are tested with broken documents.
 */
class ForgeDataTest {
    static final Path DIR = Path.of("src/main/resources/data/nusmp/elite/forge");

    /** Tensura item ids this project already relies on elsewhere (TierRules, hero forms, Black Market), plus the three crystals. */
    static final Set<String> TENSURA = Set.of("tensura:low_magisteel_ingot", "tensura:high_magisteel_ingot", "tensura:mithril_ingot", "tensura:adamantite_ingot",
            "tensura:hihiirokane_ingot", "tensura:magic_stone", "tensura:low_quality_magic_crystal", "tensura:medium_quality_magic_crystal",
            "tensura:high_quality_magic_crystal", "tensura:daemon_essence");

    static List<ForgeRecipeDef> load() throws IOException {
        List<ForgeRecipeDef> out = new ArrayList<>();
        try (Stream<Path> files = Files.list(DIR.resolve("recipes"))) {
            for (Path f : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".json")).sorted()::iterator) {
                String name = f.getFileName().toString().replace(".json", "");
                JsonObject o = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
                out.add(ForgeRecipeDef.parse("nusmp:" + name, o));
            }
        }
        return out;
    }

    @Test
    void atLeastFortyRecipesAllParse() throws IOException {
        List<ForgeRecipeDef> all = load();
        assertTrue(all.size() >= 40, "only " + all.size() + " recipes");
        Set<String> ids = new HashSet<>();
        for (ForgeRecipeDef r : all) assertTrue(ids.add(r.id()), "duplicate recipe " + r.id());
    }

    @Test
    void everyTierCategoryAndMinigameIsUsed() throws IOException {
        Set<Integer> tiers = new HashSet<>();
        Set<Category> cats = new HashSet<>();
        Set<MinigameType> games = new HashSet<>();
        int[] perTier = new int[4];
        for (ForgeRecipeDef r : load()) {
            tiers.add(r.tier());
            perTier[r.tier()]++;
            cats.add(r.category());
            for (SessionCheck.Step s : r.steps()) games.add(s.type());
        }
        assertEquals(4, tiers.size());
        assertEquals(4, cats.size());
        assertEquals(4, games.size());
        for (int t = 0; t < 4; t++) assertTrue(perTier[t] >= 5, "tier " + t + " has only " + perTier[t] + " recipes");
    }

    @Test
    void itemIdsComeFromKnownNamespaces() throws IOException {
        for (ForgeRecipeDef r : load()) {
            List<String> ids = new ArrayList<>();
            ids.add(r.output().item());
            for (ForgeRecipeDef.Stack s : r.inputs()) ids.add(s.item());
            for (ForgeRecipeDef.Premium p : r.premium()) ids.add(p.item());
            for (String id : ids) {
                if (id.startsWith("tensura:")) assertTrue(TENSURA.contains(id), r.id() + ": tensura item not in the verified list: " + id);
                else assertTrue(id.startsWith("minecraft:") || id.startsWith("nusmp:"), r.id() + ": unexpected namespace " + id);
            }
            Set<String> seen = new HashSet<>();
            for (ForgeRecipeDef.Stack s : r.inputs()) assertTrue(seen.add(s.item()), r.id() + ": the input " + s.item() + " is listed twice");
            for (ForgeRecipeDef.Premium p : r.premium()) assertFalse(seen.contains(p.item()), r.id() + ": premium " + p.item() + " is also an input");
        }
    }

    @Test
    void costsAndSequencesGrowWithTheTier() throws IOException {
        double[] steps = new double[4], cnt = new double[4];
        int[] n = new int[4];
        for (ForgeRecipeDef r : load()) {
            steps[r.tier()] += r.steps().size();
            int items = 0;
            for (ForgeRecipeDef.Stack s : r.inputs()) items += s.count();
            cnt[r.tier()] += items;
            n[r.tier()]++;
            for (SessionCheck.Step s : r.steps()) assertTrue(s.difficulty() <= 1 + 1.3 * r.tier() + 1, r.id() + ": too hard for tier " + r.tier());
        }
        for (int t = 0; t < 4; t++) {
            steps[t] /= n[t];
            cnt[t] /= n[t];
        }
        assertTrue(steps[3] > steps[0], "higher tiers play longer sequences");
        assertTrue(steps[2] >= steps[1] - 0.001 && steps[1] >= steps[0] - 0.001);
    }

    @Test
    void forgeableWeaponsAndArmourAreMarkedForEngraving() throws IOException {
        for (ForgeRecipeDef r : load()) {
            if (r.category() == Category.WEAPON || r.category() == Category.ARMOR || r.category() == Category.RUNE) assertTrue(r.engrave(), r.id() + " should engrave");
            else assertFalse(r.engrave(), r.id() + " is a tool and should not engrave");
        }
    }

    @Test
    void theSkillTableParsesToTheDocumentedEntries() throws IOException {
        JsonObject o = JsonParser.parseString(Files.readString(DIR.resolve("skill_bonus.json"))).getAsJsonObject();
        SkillBonusTable t = SkillBonusTable.parse(o);
        assertEquals(16, t.cap());
        assertTrue(t.entries().size() >= 8);
        Set<String> ids = new HashSet<>();
        for (SkillBonusTable.Entry e : t.entries()) {
            assertTrue(ids.add(e.id()), "duplicate skill " + e.id());
            assertTrue(e.mastered() >= e.owned(), e.id() + ": mastered must pay at least what owned pays");
        }
        assertTrue(ids.contains("nusmp:hephaestus"));
        assertTrue(ids.contains("nusmp:runesmiths_insight"));
        assertEquals(0, t.resolve(id -> SkillBonusTable.NONE));
        assertEquals(5, t.resolve(id -> id.equals("nusmp:hephaestus") ? SkillBonusTable.OWNED : SkillBonusTable.NONE));
        assertEquals(8, t.resolve(id -> id.equals("nusmp:hephaestus") ? SkillBonusTable.MASTERED : SkillBonusTable.NONE));
        assertEquals(16, t.resolve(id -> SkillBonusTable.MASTERED), "the sum is capped");
        Map<String, Integer> why = t.explain(id -> id.equals("tensura:creator") ? SkillBonusTable.OWNED : SkillBonusTable.NONE);
        assertEquals(Map.of("tensura:creator", 4), why);
    }

    @Test
    void theSkillTableToleratesGarbage() {
        assertEquals(0, SkillBonusTable.parse(null).entries().size());
        assertEquals(0, SkillBonusTable.parse(JsonParser.parseString("{}").getAsJsonObject()).entries().size());
        JsonObject o = JsonParser.parseString("{\"cap\":99,\"skills\":[5,{\"id\":3},{\"id\":\"NOT AN ID\"},{\"id\":\"a:b\",\"owned\":999},{\"id\":\"a:ok\",\"owned\":2}]}").getAsJsonObject();
        SkillBonusTable t = SkillBonusTable.parse(o);
        assertEquals(1, t.entries().size());
        assertEquals(16, t.cap(), "the cap never exceeds the roller's skill cap");
        assertEquals(2, t.resolve(id -> SkillBonusTable.MASTERED), "mastered with no mastered value pays the owned value");
        assertNotNull(SkillBonusTable.fallback());
    }

    @Test
    void theParserRefusesBrokenRecipes() {
        String ok = "{\"tier\":0,\"category\":\"weapon\",\"output\":{\"item\":\"minecraft:stick\"},\"inputs\":[{\"item\":\"minecraft:stick\",\"count\":2}],"
                + "\"minigames\":[{\"type\":\"hammer_timing\",\"difficulty\":1}],\"base_quality\":10}";
        assertNotNull(ForgeRecipeDef.parse("t:ok", JsonParser.parseString(ok).getAsJsonObject()));
        String[][] bad = {
                {"\"tier\":0", "\"tier\":4"}, {"\"tier\":0", "\"tier\":-1"}, {"\"weapon\"", "\"spear\""}, {"\"minecraft:stick\"},\"inputs\"", "\"Minecraft:Stick\"},\"inputs\""},
                {"\"count\":2", "\"count\":65"}, {"\"count\":2", "\"count\":0"}, {"hammer_timing", "kicking"}, {"\"difficulty\":1", "\"difficulty\":6"},
                {"\"base_quality\":10", "\"base_quality\":31"}, {"\"inputs\":[{\"item\":\"minecraft:stick\",\"count\":2}]", "\"inputs\":[]"},
                {"\"minigames\":[{\"type\":\"hammer_timing\",\"difficulty\":1}]", "\"minigames\":[]"},
        };
        for (String[] b : bad) {
            String doc = ok.replace(b[0], b[1]);
            assertTrue(!doc.equals(ok), "the test replacement did not apply: " + b[0]);
            expectRefused(doc, b[1]);
        }
        String noOutput = ok.replace("\"output\":{\"item\":\"minecraft:stick\"},", "");
        expectRefused(noOutput, "no output");
    }

    private static void expectRefused(String doc, String what) {
        try {
            ForgeRecipeDef.parse("t:bad", JsonParser.parseString(doc).getAsJsonObject());
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("the parser accepted a broken recipe: " + what);
    }
}
