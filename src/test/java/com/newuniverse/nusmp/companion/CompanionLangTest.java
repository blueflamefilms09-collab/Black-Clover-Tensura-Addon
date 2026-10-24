package com.newuniverse.nusmp.companion;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: every text of the companion system is a lang key (a dedicated server has no lang file, each player reads their own language), so a key that the code names and the fragment does not have shows up in game as a raw
 * key. This reads the fragment {@code tools/lang/companions.json} and the source: every {@code nusmp.companion.*} key the code names exists; every companion record in {@code CompanionProfiles} has the lines the
 * {@code Companion} contract needs under its prefix (the high five, using the pouch, the four gift answers, the two "likes" lines, the three stance answers, the greeting and the line under her name), each with its
 * placeholder where the code passes one; the entity and egg names exist; nothing is empty; and the fragment does not disagree with the merged game file.
 */
class CompanionLangTest {
    private static final Path FRAGMENT = Path.of("tools/lang/companions.json"), EN_US = Path.of("src/main/resources/assets/nusmp/lang/en_us.json");
    private static final Path MAIN = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final Pattern LITERAL = Pattern.compile("\"([^\"\\\\]*)\"");

    private static JsonObject fragment() throws IOException { return JsonParser.parseString(Files.readString(FRAGMENT, StandardCharsets.UTF_8)).getAsJsonObject(); }

    private static List<Path> sources() throws IOException {
        List<Path> out = new ArrayList<>();
        for (String dir : new String[]{"companion", "client/companion"}) {
            try (Stream<Path> w = Files.walk(MAIN.resolve(dir))) {
                w.filter(p -> p.toString().endsWith(".java")).forEach(out::add);
            }
        }
        return out;
    }

    @Test
    void everyKeyTheCodeNamesIsInTheFragment() throws IOException {
        JsonObject lang = fragment();
        List<String> missing = new ArrayList<>();
        for (Path p : sources()) {
            for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                String t = line.strip();
                if (t.startsWith("*") || t.startsWith("//") || t.startsWith("/*")) continue;
                boolean bond = p.getFileName().toString().equals("Bond.java") && line.contains("K + ");
                Matcher m = LITERAL.matcher(line);
                while (m.find()) {
                    String lit = m.group(1);
                    String key = bond && !lit.startsWith("nusmp.") ? "nusmp.companion.bond." + lit : lit;
                    if (!key.startsWith("nusmp.companion.")) continue;
                    if (key.equals("nusmp.companion.bond.")) continue;
                    if (key.endsWith(".")) {                                              // a prefix the code completes at run time: some key must start with it
                        boolean any = false;
                        for (String k : lang.keySet()) if (k.startsWith(key)) any = true;
                        if (!any) missing.add(p.getFileName() + ": nothing starts with " + key);
                    } else if (!lang.has(key) && !key.startsWith("nusmp.companion.selftest.") && !isNotAKey(key)) {
                        missing.add(p.getFileName() + ": " + key);
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), "keys the code names that the fragment lacks: " + missing);
    }

    /** Strings that start like a key but are not lang keys (the config / toml names, the nbt root, the persistent-data root). */
    private static boolean isNotAKey(String s) { return s.startsWith("nusmp_companion") || s.endsWith(".toml"); }

    @Test
    void theTabsTheTiersAndTheStancesAreAllThere() throws IOException {
        JsonObject lang = fragment();
        for (String k : new String[]{"tab.talk", "tab.interact", "tab.bond", "tab.pouch", "tier.stranger", "tier.acquaintance", "tier.friend", "tier.close", "stance.guard", "stance.aggressive", "stance.passive",
                "bond.stance.guard", "bond.stance.aggressive", "bond.stance.passive", "close", "join.full"})
            assertTrue(lang.has("nusmp.companion." + k), k);
    }

    private static final String[] CONTRACT = {"sub", "greet", "act.five", "use", "gift.love", "gift.ok", "gift.hate", "gift.no", "likes.love", "likes.hate", "stance.guard", "stance.aggressive", "stance.passive"};
    private static final Set<String> WITH_ITEM = Set.of("use", "gift.love", "gift.ok", "gift.hate", "gift.no", "likes.love", "likes.hate");

    @Test
    void everyCompanionRecordHasTheLinesOfTheContract() throws IOException {
        JsonObject lang = fragment();
        String src = Files.readString(MAIN.resolve("companion/sample/CompanionProfiles.java"), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("new Profile\\(\"([a-z_]+)\", \"([a-z0-9_]+)\", \"([a-z0-9_.]+\\.)\"").matcher(src);
        int profiles = 0;
        while (m.find()) {
            profiles++;
            String prefix = m.group(3);
            for (String k : CONTRACT) {
                assertTrue(lang.has(prefix + k), "missing " + prefix + k);
                String v = lang.get(prefix + k).getAsString();
                assertFalse(v.isBlank(), prefix + k + " is empty");
                if (WITH_ITEM.contains(k)) assertTrue(v.contains("%s"), prefix + k + " has no %s for the item");
            }
            assertTrue(lang.get(prefix + "greet").getAsString().contains("%s"), "the greeting names the player");
            for (String k : new String[]{"q.self", "q.food", "q.magic", "q.join", "q.wait", "a.self", "a.food", "a.magic", "a.join", "a.wait", "join.need", "join.taken"})
                assertTrue(lang.has(prefix + k), "missing " + prefix + k);
        }
        assertTrue(profiles >= 1, "no companion record found in CompanionProfiles");
    }

    @Test
    void theEntityAndTheEggHaveNames() throws IOException {
        JsonObject lang = fragment();
        assertTrue(lang.has("entity.nusmp.arc_companion"));
        assertTrue(lang.has("item.nusmp.companion_charmy_spawn_egg"));
    }

    @Test
    void nothingIsEmptyAndNoKeyIsOutsideOurSpaces() throws IOException {
        JsonObject lang = fragment();
        for (String k : lang.keySet()) {
            assertTrue(k.startsWith("nusmp.companion.") || k.equals("entity.nusmp.arc_companion") || k.equals("item.nusmp.companion_charmy_spawn_egg"), "a key outside the companion spaces: " + k);
            assertFalse(lang.get(k).getAsString().isBlank(), k + " is empty");
        }
    }

    @Test
    void theFragmentDoesNotDisagreeWithTheMergedFile() throws IOException {
        JsonObject lang = fragment();
        JsonObject game = JsonParser.parseString(Files.readString(EN_US, StandardCharsets.UTF_8)).getAsJsonObject();
        Set<String> clash = new TreeSet<>();
        for (String k : lang.keySet()) if (game.has(k) && !game.get(k).getAsString().equals(lang.get(k).getAsString())) clash.add(k);
        assertEquals(Set.of(), clash, "keys that en_us.json already has with other text");
    }
}
