package com.newuniverse.nusmp.skilltree.hk;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the three Heavy Knight route files, the skill ids and passive ids of {@link HkRoute}, and the fallback numbers of the techniques are three copies of one truth, and a drift between them is the
 * kind of bug that only shows in a player's session (a node that grants a skill nobody registered, a passive nobody implements, a technique that falls back to a number the file no longer has). This test reads the SHIPPED files
 * and checks them against the constants: the franchise tag, the distinct class and book ids, the skill ids of the register, the passive ids, and that every fallback equals the file's level-1 value.
 */
class HkRouteFilesTest {
    private static final Path DIR = Path.of("src/main/resources/data/nusmp/skill_trees");
    private static final String[][] ROUTES = {{"oath_of_the_heavy_armor", HkRoute.OATH}, {"basic_swordsmanship", HkRoute.SWORD}, {"lady_luck", HkRoute.LUCK}, {"smoldering_fang_of_insanity", HkRoute.FANG}};
    private static final Set<String> SKILLS = Set.of(HkRoute.SKILL_PROVOKE, HkRoute.SKILL_SHIELD_WALL, HkRoute.SKILL_SHIELD_BASH, HkRoute.SKILL_POWER_STRIKE, HkRoute.SKILL_SWIFT_STEP,
            HkRoute.SKILL_CRESCENT_CUT, HkRoute.SKILL_GAMBLERS_COIN, HkRoute.SKILL_JACKPOT, HkRoute.SKILL_EMBER_BITE, HkRoute.SKILL_INSANITY, HkRoute.SKILL_HOWL_OF_RUIN, HkRoute.SKILL_FANG_OF_INSANITY);

    private static JsonObject read(String file) throws Exception {
        return JsonParser.parseString(Files.readString(DIR.resolve(file + ".json"))).getAsJsonObject();
    }

    @Test
    void theRoutesCarryTheFranchiseAndDistinctClassesAndBooks() throws Exception {
        Set<String> classes = new HashSet<>(), books = new HashSet<>();
        for (String[] r : ROUTES) {
            JsonObject o = read(r[0]);
            assertEquals(HkRoute.FRANCHISE, o.get("magic").getAsString(), r[0]);
            assertTrue(classes.add(o.get("class_id").getAsString()), "class used twice in " + r[0]);
            assertTrue(books.add(o.get("book").getAsString()), "book used twice in " + r[0]);
            assertTrue(r[1].endsWith(r[0]), "the route id constant names the file " + r[0]);
        }
        assertEquals(Set.of(HkRoute.OATH_CLASS, HkRoute.SWORD_CLASS, HkRoute.LUCK_CLASS, HkRoute.FANG_CLASS), classes);
    }

    @Test
    void theSmolderingFangIsItsOwnBookAndNoOtherBookCarriesItsName() throws Exception {
        JsonObject fang = read("smoldering_fang_of_insanity");
        assertEquals("Smoldering Fang of Insanity", fang.get("book_name").getAsString());
        for (String[] r : ROUTES) {
            if (r[0].equals("smoldering_fang_of_insanity")) continue;
            assertTrue(!read(r[0]).get("book_name").getAsString().toLowerCase().contains("smoldering"), r[0] + " must not carry the Fang's name");
        }
        for (JsonElement el : fang.getAsJsonArray("nodes")) {
            JsonObject n = el.getAsJsonObject();
            if (n.has("skill")) assertTrue(n.get("skill").getAsString().startsWith("nusmp:hk_"), "a Fang technique is a Heavy Knight technique: " + n.get("skill").getAsString());
        }
    }

    @Test
    void everySkillAndPassiveOfTheFilesIsImplemented() throws Exception {
        Set<String> skills = new HashSet<>();
        List<String> bad = new ArrayList<>();
        for (String[] r : ROUTES) {
            for (JsonElement el : read(r[0]).getAsJsonArray("nodes")) {
                JsonObject n = el.getAsJsonObject();
                if (n.has("skill")) {
                    String s = n.get("skill").getAsString();
                    if (!s.startsWith("nusmp:") || !SKILLS.contains(s.substring(6))) bad.add(r[0] + "/" + n.get("id").getAsString() + " names the unregistered skill " + s);
                    skills.add(s.substring(6));
                }
                if (n.has("passives")) for (JsonElement p : n.getAsJsonArray("passives")) if (!HkRoute.PASSIVES.contains(p.getAsString())) bad.add(r[0] + "/" + n.get("id").getAsString() + " lists the unimplemented passive " + p.getAsString());
            }
        }
        assertTrue(bad.isEmpty(), String.join("\n", bad));
        assertEquals(SKILLS, skills, "every registered skill is granted by exactly the files");
    }

    @Test
    void theFallbackNumbersAreTheLevelOneValuesOfTheFiles() throws Exception {
        List<String> bad = new ArrayList<>();
        for (String[] r : ROUTES) {
            for (JsonElement el : read(r[0]).getAsJsonArray("nodes")) {
                JsonObject n = el.getAsJsonObject();
                String id = n.get("id").getAsString();
                JsonObject values = n.getAsJsonObject("values");
                for (String key : values.keySet()) {
                    JsonArray t = values.getAsJsonArray(key);
                    if (t.size() != n.get("max_level").getAsInt()) bad.add(id + "." + key + " has " + t.size() + " entries for " + n.get("max_level").getAsInt() + " levels");
                    double fb = HkRoute.fallback(id, key);
                    if (Math.abs(fb - t.get(0).getAsDouble()) > 1e-9) bad.add(id + "." + key + ": fallback " + fb + " but the file says " + t.get(0).getAsDouble());
                }
                if (n.has("skill")) for (String need : new String[] {"cost", "cooldown"}) if (!values.has(need)) bad.add(id + " has no " + need + " table");
            }
        }
        assertTrue(bad.isEmpty(), String.join("\n", bad));
    }
}
