package com.newuniverse.nusmp.heroweapon.forms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Why: the 21 forms files are written by six builders who cannot run the game, so the shipped data is read from the source tree and put through the same
 * {@link FormsSchema} the game uses: every file must belong to a weapon of the catalogue and be clean (no error, no warning). The folder may be empty (the files
 * arrive when the weapon builders merge): then only the negative control runs, which proves the validator rejects a file full of mistakes and accepts a good one,
 * so a green test is never just "no file was looked at". Whether an item or tag exists and whether a skill is registered is checked in the game by the
 * {@code hero.forms} self-test; this test cannot see the registries.
 */
class FormsDataTest {
    private static final Path DIR = Path.of("src/main/resources/data/nusmp/hero/forms");

    @Test
    void everyShippedFileBelongsToAWeaponAndIsClean() throws IOException {
        List<String> problems = new ArrayList<>();
        int files = 0;
        if (Files.isDirectory(DIR)) {
            List<Path> list;
            try (Stream<Path> s = Files.list(DIR)) {
                list = s.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList();
            }
            for (Path file : list) {
                files++;
                String name = file.getFileName().toString();
                HeroKind kind = HeroKind.byItemId(name.substring(0, name.length() - 5));
                if (kind == null) {
                    problems.add(name + ": no weapon has this item id (the file name must be <itemId>.json)");
                    continue;
                }
                if (!name.equals(kind.itemId() + ".json")) problems.add(name + ": write the plain item id " + kind.itemId() + ".json");
                FormsSchema.Parsed p = FormsSchema.parseText(kind, Files.readString(file, StandardCharsets.UTF_8), null);
                if (!p.clean()) problems.add(name + ":\n" + p.report());
            }
        }
        System.out.println("[forms data] " + files + " of " + HeroKind.values().length + " weapons ship a forms file");
        assertTrue(problems.isEmpty(), "forms data problems:\n" + String.join("\n", problems));
    }

    @Test
    void everyShippedFileIsPlainTextWithoutATabOrByteOrderMark() throws IOException {
        if (!Files.isDirectory(DIR)) return;
        List<Path> list;
        try (Stream<Path> s = Files.list(DIR)) {
            list = s.filter(p -> p.getFileName().toString().endsWith(".json")).toList();
        }
        for (Path file : list) {
            byte[] raw = Files.readAllBytes(file);
            assertFalse(raw.length >= 3 && (raw[0] & 0xFF) == 0xEF && (raw[1] & 0xFF) == 0xBB && (raw[2] & 0xFF) == 0xBF, file + " starts with a byte order mark; the game's reader would fail on it");
            String text = new String(raw, StandardCharsets.UTF_8);
            for (int i = 0; i < text.length(); i++) assertFalse(text.charAt(i) == '\ufffd', file + " has a character that is not valid UTF-8 (position " + i + ")");
        }
    }

    // ------------------------------------------------------------------------------------------------ negative control
    /** A file with a mistake of every kind the authors are most likely to make. */
    private static final String WRONG = """
            { "weapon": "legendary_shield",
              "forms": [
                { "id": "base", "name": "Small Shield", "desc": "The starting shape.", "tier": 0, "mastery": 0 },
                { "id": "Leaf_Shield", "name": "Leaf Shield", "desc": "x", "tier": 1, "parents": ["base"], "requires": [ { "item": "minecraft:leather", "count": 2 } ], "mastery": 120 },
                { "id": "iron_shield", "name": "Iron Shield", "desc": "x", "tier": 1, "parents": ["base"], "requires": [ { "item": "minecraft:iron_ingot", "count": 24 } ],
                  "mastery": 500, "stats": { "armor": 20, "strength": 1 }, "skills": { "use": "shield.air_strike", "sneak": "sword.slash" } },
                { "id": "deep_shield", "name": "Deep Shield", "desc": "x", "tier": 4, "parents": ["base"], "requires": [ { "item": "minecraft:diamond", "count": 8 } ], "mastery": 100 },
                { "id": "wrath_shield", "name": "Wrath Shield", "desc": "x", "tier": 1, "parents": ["base"], "mastery": 100, "curse": true, "sin": "pride", "look": "base" },
                { "id": "loop_a", "name": "A", "desc": "x", "tier": 2, "parents": ["loop_b"], "requires": [ { "item": "minecraft:bone", "count": 8 } ], "mastery": 100 },
                { "id": "loop_b", "name": "B", "desc": "x", "tier": 2, "parents": ["loop_a"], "requires": [ { "item": "minecraft:bone", "count": 8 } ], "mastery": 100 }
              ] }
            """;

    /** The smallest file that is entirely clean for a Seven Star weapon. */
    private static final String RIGHT = """
            { "weapon": "vassal_hammer",
              "forms": [
                { "id": "base", "name": "Hammer", "desc": "The starting shape.", "tier": 0, "mastery": 0 },
                { "id": "iron_hammer", "name": "Iron Hammer", "desc": "A heavier head.", "tier": 1, "parents": ["base"],
                  "requires": [ { "item": "minecraft:iron_ingot", "count": 24 } ], "mastery": 120, "stats": { "attack_damage": 2.0 }, "abilityBonus": { "armor": 0.5 } },
                { "id": "gold_hammer", "name": "Gold Hammer", "desc": "A flashier head.", "tier": 2, "parents": ["iron_hammer"],
                  "requires": [ { "tag": "c:ingots/gold", "count": 32 } ], "mastery": 200 }
              ] }
            """;

    @Test
    void theValidatorRejectsAFileFullOfMistakesAndNamesEachOne() {
        FormsSchema.Parsed p = FormsSchema.parseText(HeroKind.SHIELD, WRONG, null);
        assertFalse(p.clean(), "the negative control must not pass");
        String report = p.report();
        String[] expected = {
                "\"id\" must be lower-case snake_case",               // Leaf_Shield
                "\"count\" must be a whole number 4 to 64",           // 2 leather
                "\"mastery\" must be 40 to 400",                      // 500
                "outside the budget",                                  // armor 20
                "unknown stat 'strength'",
                "raises the guard",                                    // use on a Shield
                "is not a skill of the Legendary Shield",              // sword.slash
                "must be 1",                                           // deep_shield: tier 4 directly under the base form
                "carries [wrath] in canon, not pride",                 // wrath_shield: a sin the Shield does not carry
                "uses look \"curse\"",                                 // wrath_shield: a Holy curse form must show the cursed mesh
                "loop",                                                // loop_a / loop_b
        };
        List<String> missing = new ArrayList<>();
        for (String e : expected) if (!report.contains(e)) missing.add(e);
        assertTrue(missing.isEmpty(), "the validator did not report: " + missing + "\n" + report);
        assertTrue(p.warnings().stream().anyMatch(w -> w.contains("ordinary (non-curse) forms")), "and it warns that the form count is off: " + report);
        for (HeroForm f : p.forms()) {
            assertFalse(f.id().equals("iron_shield") || f.id().equals("deep_shield") || f.id().equals("wrath_shield") || f.id().startsWith("loop_"), "a bad form must be skipped: " + f.id());
        }
        assertEquals("base", p.forms().get(0).id());
    }

    @Test
    void theValidatorAcceptsAGoodFile() {
        FormsSchema.Parsed p = FormsSchema.parseText(HeroKind.HAMMER, RIGHT, null);
        assertTrue(p.clean(), p.report());
        assertEquals(3, p.forms().size());
        assertNotNull(p.forms().get(2).requires().get(0).tag());
        assertTrue(p.forms().get(2).requires().get(0).isTag());
    }

    @Test
    void aFileWhoseWeaponKeyNamesAnotherWeaponIsRejected() {
        FormsSchema.Parsed wrongKind = FormsSchema.parseText(HeroKind.AXE, RIGHT, null);
        assertTrue(wrongKind.fileRejected(), "vassal_hammer's content under vassal_axe's name: " + wrongKind.report());
    }
}
