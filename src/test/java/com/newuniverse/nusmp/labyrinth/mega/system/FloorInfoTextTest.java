package com.newuniverse.nusmp.labyrinth.mega.system;

import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.lock.LockText;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the words of the floor report and of the escape rules live in {@link FloorInfoText} and {@link LockText} (the English the code falls back on) and in
 * {@code tools/lang/system_floor_info.json} (the flat fragment the owner merges into the game's lang file; {@code en_us.json} itself is not edited). If the two ever differ a
 * player sees one text in a fresh checkout and another after the merge, so this pins them identical, key for key and word for word. It also pins that every key has the
 * module's prefix and a text, that the System's lookup ({@link SystemText#english}) finds them, that the numbered placeholders are well formed, that every theme that exists
 * has a one-line description today, and that a theme which does not (one added later) is not an error: it falls back to a line made from its data.
 */
class FloorInfoTextTest {
    private static final Path LANG = Paths.get("tools", "lang", "system_floor_info.json");
    private static final Pattern PLACEHOLDER = Pattern.compile("%(\\d+)\\$s");

    private static Map<String, String> all() {
        Map<String, String> m = new LinkedHashMap<>(FloorInfoText.all());
        m.putAll(LockText.all());
        return m;
    }

    @Test void theFragmentFileSaysExactlyWhatTheCodeSays() throws IOException {
        Map<String, Object> file = MiniJson.object(Files.readString(LANG, StandardCharsets.UTF_8));
        Map<String, String> code = all();
        assertEquals(code.size(), file.size(), "same number of keys");
        for (Map.Entry<String, String> e : code.entrySet()) {
            assertTrue(file.containsKey(e.getKey()), "missing in the fragment: " + e.getKey());
            assertEquals(e.getValue(), file.get(e.getKey()), e.getKey());
        }
        assertEquals(List.copyOf(code.keySet()), List.copyOf(file.keySet()), "same order, so a diff is readable");
    }

    @Test void everyKeyHasThePrefixOrIsAKeyBindingAndAnEnglishTextTheSystemFinds() {
        for (Map.Entry<String, String> e : all().entrySet()) {
            assertTrue(e.getKey().startsWith(SystemText.PREFIX) || e.getKey().startsWith("key.nusmp."), e.getKey());
            assertFalse(e.getValue().isBlank(), e.getKey());
            assertEquals(e.getValue(), SystemText.english(e.getKey()), "SystemText.english finds " + e.getKey());
        }
    }

    @Test void noKeyClashesWithAnOlderOne() {
        for (String k : all().keySet()) {
            assertFalse(SystemText.all().containsKey(k), "already in SystemText: " + k);
            assertNull(PointsText.englishOrNull(k), "already in PointsText: " + k);
        }
        for (String k : FloorInfoText.all().keySet()) assertNull(LockText.englishOrNull(k), "in both maps: " + k);
    }

    @Test void placeholdersAreNumberedFromOneWithoutGaps() {
        for (Map.Entry<String, String> e : all().entrySet()) {
            String v = e.getValue();
            assertFalse(v.contains("%s") || v.contains("%d"), "an unnumbered placeholder in " + e.getKey() + ": " + v);
            Matcher m = PLACEHOLDER.matcher(v);
            int max = 0;
            java.util.Set<Integer> seen = new java.util.TreeSet<>();
            while (m.find()) {
                int n = Integer.parseInt(m.group(1));
                max = Math.max(max, n);
                seen.add(n);
            }
            for (int i = 1; i <= max; i++) assertTrue(seen.contains(i), "placeholder " + i + " is missing in " + e.getKey() + ": " + v);
        }
    }

    @Test void everyThemeThatExistsHasAOneLineDescription() {
        List<String> known = List.of("gravity", "blood", "spatial", "flame", "frost", "verdant", "storm", "shadow", "astral", "radiant", "abyss", "feywild_grove", "dwarven_forgehold",
                "crystal_geode", "sunken_temple", "holy_church", "celestial_court", "sky_citadel", "mountain_monastery", "cursed_archive", "necropolis", "vampire_castle", "haunted_manor",
                "fungal_underdark", "infernal_foundry", "drowned_crypt", "spider_warrens");
        for (String id : known) {
            boolean exists = false;
            for (RegionTheme t : RegionTheme.values()) if (t.id().equals(id)) exists = true;
            assertTrue(exists, id + " is not a theme");
            String d = FloorInfoText.themeDesc(id);
            assertNotNull(d, id);
            assertTrue(d.length() >= 20 && d.length() <= 160, id + " should be one line, got " + d.length() + " characters");
            assertEquals(d, FloorInfoText.englishOrNull(FloorInfoText.themeDescKey(id)), id);
        }
        assertEquals(known.size(), FloorInfoText.describedThemes().size());
    }

    @Test void aThemeAddedLaterIsNotAnErrorItFallsBackToItsData() {
        for (RegionTheme t : RegionTheme.values()) {
            String d = FloorInfoText.themeDesc(t.id());
            if (d == null) assertFalse(FloorInfoRules.genericThemeDesc("collapse").isBlank(), "a theme with no description needs a generic line: " + t.id());
        }
        assertNull(FloorInfoText.themeDesc("not_a_theme"));
        assertNull(FloorInfoText.themeDesc(null));
        assertEquals("Not A Theme", FloorInfoRules.humanize("not_a_theme"));
    }
}
