package com.newuniverse.nusmp.compat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.boss.BossRegistry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** The compatibility data pack (the copy shipped in the jar): valid JSON, 1.21 folder names, optional ids, our own loot tables, our own bosses. */
class CompatPackTest {
    private static final Path PACK = Path.of("src/main/resources/resourcepacks/nusmp_tni_compat");
    private static final Path SOURCE = Path.of("tools/compat/multiverse_x_tensura_no_index");
    private static final Path OUR_LOOT = Path.of("src/main/resources/data/nusmp/loot_table");

    private static List<Path> jsons(Path root) throws IOException {
        try (Stream<Path> s = Files.walk(root)) {
            return s.filter(p -> p.toString().endsWith(".json") || p.toString().endsWith(".mcmeta")).sorted().toList();
        }
    }

    private static JsonObject read(Path p) throws IOException {
        return JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test
    void packMetaIsFormat48() throws IOException {
        assertEquals(48, read(PACK.resolve("pack.mcmeta")).getAsJsonObject("pack").get("pack_format").getAsInt());
    }

    @Test
    void jarCopyEqualsSourceAndEveryFileParses() throws IOException {
        for (Path p : jsons(SOURCE)) {
            Path copy = PACK.resolve(SOURCE.relativize(p));
            assertTrue(Files.exists(copy), "missing in the jar copy: " + copy);
            assertEquals(Files.readString(p), Files.readString(copy), "out of date, run tools/compat/build_compat_pack.py: " + copy);
            read(p);
        }
        try (Stream<Path> s = Files.walk(PACK)) {
            for (Path p : s.filter(Files::isRegularFile).toList()) assertTrue(Files.exists(SOURCE.resolve(PACK.relativize(p))), "stray file " + p);
        }
    }

    @Test
    void usesOnlyOneDotTwentyOneFolderNames() throws IOException {
        try (Stream<Path> s = Files.walk(PACK)) {
            for (Path p : s.toList()) {
                String n = p.getFileName().toString();
                assertFalse(Set.of("loot_tables", "entity_types", "recipes", "advancements", "tags_old").contains(n), "old plural folder: " + p);
            }
        }
        assertTrue(Files.exists(PACK.resolve("data/tensura/tags/entity_type/no_max_ep_plunder.json")));
        assertTrue(Files.exists(PACK.resolve("data/neoforge/loot_modifiers/global_loot_modifiers.json")));
    }

    @Test
    void everyModifierIsListedAndEveryListedModifierExists() throws IOException {
        JsonObject list = read(PACK.resolve("data/neoforge/loot_modifiers/global_loot_modifiers.json"));
        assertFalse(list.get("replace").getAsBoolean(), "must not wipe other packs' modifiers");
        Set<String> listed = new HashSet<>();
        for (JsonElement e : list.getAsJsonArray("entries")) listed.add(e.getAsString());
        Set<String> present = new HashSet<>();
        try (Stream<Path> s = Files.list(PACK.resolve("data/nusmp_tni_compat/loot_modifiers"))) {
            for (Path p : s.toList()) present.add("nusmp_tni_compat:" + p.getFileName().toString().replace(".json", ""));
        }
        assertEquals(present, listed);
        for (String id : listed) {
            JsonObject m = read(PACK.resolve("data/nusmp_tni_compat/loot_modifiers/" + id.substring(id.indexOf(':') + 1) + ".json"));
            assertEquals("neoforge:add_table", m.get("type").getAsString());
            String table = m.get("table").getAsString();
            assertTrue(table.startsWith("nusmp_tni_compat:"), "only its own tables: " + table);
            assertTrue(Files.exists(PACK.resolve("data/nusmp_tni_compat/loot_table/" + table.substring(table.indexOf(':') + 1) + ".json")), table);
            assertTrue(m.has("neoforge:conditions"), "gated on the other mod: " + id);
        }
    }

    @Test
    void targetedLootTablesExistInOurData() throws IOException {
        Set<String> targets = new HashSet<>();
        for (Path p : jsons(PACK.resolve("data/nusmp_tni_compat/loot_modifiers"))) collect(read(p), "loot_table_id", targets);
        assertFalse(targets.isEmpty());
        for (String t : targets) {
            assertTrue(t.startsWith("nusmp:"), t);
            assertTrue(Files.exists(OUR_LOOT.resolve(t.substring("nusmp:".length()) + ".json")), "no such table of ours: " + t);
        }
    }

    private static void collect(JsonElement e, String key, Set<String> out) {
        if (e.isJsonObject()) {
            for (var en : e.getAsJsonObject().entrySet()) {
                if (en.getKey().equals(key) && en.getValue().isJsonPrimitive()) out.add(en.getValue().getAsString());
                else collect(en.getValue(), key, out);
            }
        } else if (e.isJsonArray()) for (JsonElement x : e.getAsJsonArray()) collect(x, key, out);
    }

    @Test
    void otherModItemsAreOnlyInOwnLootTablesAndAreTheFourVerifiedOnes() throws IOException {
        Set<String> items = new HashSet<>();
        for (Path p : jsons(PACK.resolve("data/nusmp_tni_compat/loot_table"))) collect(read(p), "name", items);
        assertEquals(Set.of("tensuranoindex:phase_dust", "tensuranoindex:name_slip", "tensuranoindex:canalis_relic", "tensuranoindex:relic_philosophers_stone"), items);
    }

    @Test
    void lootChancesAreLow() throws IOException {
        List<Double> chances = new ArrayList<>();
        for (Path p : jsons(PACK.resolve("data/nusmp_tni_compat/loot_table"))) collectChance(read(p), chances);
        assertFalse(chances.isEmpty());
        for (double c : chances) assertTrue(c > 0 && c <= 0.25, "chance " + c);
    }

    private static void collectChance(JsonElement e, List<Double> out) {
        if (e.isJsonObject()) {
            JsonObject o = e.getAsJsonObject();
            if (o.has("condition") && o.get("condition").getAsString().equals("minecraft:random_chance")) out.add(o.get("chance").getAsDouble());
            for (var en : o.entrySet()) collectChance(en.getValue(), out);
        } else if (e.isJsonArray()) for (JsonElement x : e.getAsJsonArray()) collectChance(x, out);
    }

    @Test
    void ourBossTagIsTheBossRegistryPlusZagredAndAllIdsAreOptional() throws IOException {
        JsonArray values = read(PACK.resolve("data/nusmp_tni_compat/tags/entity_type/bosses.json")).getAsJsonArray("values");
        Set<String> ids = new HashSet<>();
        for (JsonElement v : values) {
            JsonObject o = v.getAsJsonObject();
            assertFalse(o.get("required").getAsBoolean());
            ids.add(o.get("id").getAsString());
        }
        Set<String> expected = new HashSet<>();
        for (String path : BossRegistry.paths()) expected.add("nusmp:" + path);
        expected.add("nusmp:zagred");
        assertEquals(expected, ids);
    }

    @Test
    void plunderTagHoldsOurBossesAndTheirsAndNothingRequired() throws IOException {
        JsonObject tag = read(PACK.resolve("data/tensura/tags/entity_type/no_max_ep_plunder.json"));
        assertFalse(tag.get("replace").getAsBoolean());
        Set<String> ids = new HashSet<>();
        for (JsonElement v : tag.getAsJsonArray("values")) {
            JsonObject o = v.getAsJsonObject();
            assertFalse(o.get("required").getAsBoolean(), "every id optional: " + o);
            ids.add(o.get("id").getAsString());
        }
        for (String path : BossRegistry.paths()) assertTrue(ids.contains("nusmp:" + path), path);
        assertTrue(ids.contains("tensuranoindex:coronzon") && ids.contains("tensuranoindex:innocentius"));
        assertEquals(15, ids.size());
    }
}
