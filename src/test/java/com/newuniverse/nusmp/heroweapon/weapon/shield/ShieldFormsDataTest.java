package com.newuniverse.nusmp.heroweapon.weapon.shield;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Why: the Legendary Shield's forms file ({@code data/nusmp/hero/forms/legendary_shield.json}) is read by the forms system, the Weapon Book and the dispatcher, and a
 * wrong id in it fails there, in the game, long after the commit. This test holds the shipped file to the rules of design.md section 6 and to the Shield's own
 * logic: every skill id it lists is one the Shield has, every passive is one the Shield implements, every skill and passive is used by some form, the Wrath series is
 * there, and no form uses the use slot (the use key raises the guard). The checker itself is proved against deliberately wrong files first, so a green test means
 * something.
 */
class ShieldFormsDataTest {
    static final Path FILE = Path.of("src/main/resources/data/nusmp/hero/forms/legendary_shield.json");

    /** The Tensura items the file uses, each found in the owner's Tensura 2.0.1.3 id reference (items part) when the file was written. */
    static final Set<String> TENSURA_ITEMS = Set.of("tensura:low_magisteel_ingot", "tensura:daemon_essence");

    static FormsFileCheck.Rules rules() {
        Map<String, Boolean> skills = new LinkedHashMap<>();
        for (ShieldNumbers.Spec s : ShieldNumbers.all()) skills.put(s.id(), s.kind() == ShieldNumbers.Kind.ULTIMATE);
        return new FormsFileCheck.Rules(HeroKind.SHIELD, skills, ShieldNumbers.PASSIVES, TENSURA_ITEMS, 8, 10, 3, 4);
    }

    @Test
    void theShippedFileKeepsEveryRule() {
        List<String> bad = FormsFileCheck.check(FILE, rules());
        assertTrue(bad.isEmpty(), bad.size() + " problem(s): " + bad);
    }

    @Test
    void theShieldHasTenOrdinaryFormsAndFourWrathForms() throws IOException {
        String text = Files.readString(FILE, StandardCharsets.UTF_8);
        assertEquals(4, text.split("\"curse\": true", -1).length - 1, "four Wrath forms");
        assertEquals(10, text.split("\"curse\": false", -1).length - 1, "ten ordinary forms");
        assertFalse(text.contains("\"use\""), "a blocking weapon has no use slot");
        assertTrue(text.contains("\"sin\": \"wrath\""));
        assertFalse(text.contains("\"sin\": \"greed\""));
    }

    // ------------------------------------------------------------------ the checker is proved against wrong files
    private static List<String> checkText(String json) throws IOException {
        Path tmp = Files.createTempFile("forms", ".json");
        try {
            Files.writeString(tmp, json, StandardCharsets.UTF_8);
            return FormsFileCheck.check(tmp, rules());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void theCheckerRejectsAnUnknownSkillPassiveStatAndSlot() throws IOException {
        String text = Files.readString(FILE, StandardCharsets.UTF_8);
        assertTrue(checkText(text.replaceFirst("shield\\.air_strike", "shield.no_such_skill")).stream().anyMatch(s -> s.contains("unknown skill")));
        assertTrue(checkText(text.replaceFirst("\"herb_regen\"", "\"no_such_passive\"")).stream().anyMatch(s -> s.contains("unknown passive")));
        assertTrue(checkText(text.replaceFirst("\"max_health\": 2.0", "\"max_hp\": 2.0")).stream().anyMatch(s -> s.contains("unknown stat")));
        assertTrue(checkText(text.replaceFirst("\"key1\": \"shield.herb_mend\"", "\"use\": \"shield.herb_mend\"")).stream().anyMatch(s -> s.contains("no use slot")));
    }

    @Test
    void theCheckerRejectsABrokenTreeAndBrokenNumbers() throws IOException {
        String text = Files.readString(FILE, StandardCharsets.UTF_8);
        assertTrue(checkText(text.replaceFirst("\"parents\": \\[\"base\"\\]", "\"parents\": [\"no_such_form\"]")).stream().anyMatch(s -> s.contains("unknown parent")));
        assertTrue(checkText(text.replaceFirst("\"armor\": 2.5", "\"armor\": 9.5")).stream().anyMatch(s -> s.contains("out of")));
        assertTrue(checkText(text.replaceFirst("\"count\": 48", "\"count\": 3")).stream().anyMatch(s -> s.contains("count")));
        assertTrue(checkText(text.replaceFirst("tensura:daemon_essence", "tensura:no_such_item")).stream().anyMatch(s -> s.contains("verified")));
        assertTrue(checkText(text.replaceFirst("\"sin\": \"wrath\"", "\"sin\": \"pride\"")).stream().anyMatch(s -> s.contains("sin")));
        assertFalse(checkText("{ \"weapon\": \"legendary_shield\", \"forms\": [] }").isEmpty());
    }

    @Test
    void theCheckerRejectsTwoUltimatesInOneForm() throws IOException {
        String text = Files.readString(FILE, StandardCharsets.UTF_8);
        String two = text.replaceFirst("\"key2\": \"shield.cover\", \"key3\": \"shield.absolute_defence\"", "\"key2\": \"shield.megiddo\", \"key3\": \"shield.absolute_defence\"");
        assertTrue(checkText(two).stream().anyMatch(s -> s.contains("ultimates")));
    }
}
