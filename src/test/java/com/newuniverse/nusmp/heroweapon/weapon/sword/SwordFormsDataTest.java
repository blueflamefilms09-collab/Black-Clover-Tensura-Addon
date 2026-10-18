package com.newuniverse.nusmp.heroweapon.weapon.sword;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.weapon.shield.FormsFileCheck;
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
 * Why: the Legendary Sword's forms file ({@code data/nusmp/hero/forms/legendary_sword.json}) is read by the forms system, the Weapon Book and the dispatcher, and a
 * wrong id in it fails there, in the game, long after the commit. This test holds the shipped file to the rules of design.md section 6 (through the same checker the
 * Shield's test uses) and to the Sword's own logic: every skill id it lists is one the Sword has, every passive is one the Sword implements, every skill and passive is
 * used by some form, and both curse series are there with two forms each. The checker is proved against deliberately wrong copies of the file first.
 */
class SwordFormsDataTest {
    static final Path FILE = Path.of("src/main/resources/data/nusmp/hero/forms/legendary_sword.json");

    /** The Tensura items the file uses, each found in the owner's Tensura 2.0.1.3 id reference (items part) when the file was written. */
    static final Set<String> TENSURA_ITEMS = Set.of("tensura:armorsaurus_scale", "tensura:armorsaurus_shell", "tensura:giant_ant_carapace", "tensura:mithril_ingot",
            "tensura:high_quality_magic_crystal", "tensura:gold_coin");

    static FormsFileCheck.Rules rules() {
        Map<String, Boolean> skills = new LinkedHashMap<>();
        for (SwordNumbers.Spec s : SwordNumbers.all()) skills.put(s.id(), s.kind() == SwordNumbers.Kind.ULTIMATE);
        return new FormsFileCheck.Rules(HeroKind.SWORD, skills, SwordNumbers.PASSIVES, TENSURA_ITEMS, 8, 10, 3, 4);
    }

    @Test
    void theShippedFileKeepsEveryRule() {
        List<String> bad = FormsFileCheck.check(FILE, rules());
        assertTrue(bad.isEmpty(), bad.size() + " problem(s): " + bad);
    }

    @Test
    void theSwordHasTenOrdinaryFormsAndTwoFormsForEachOfItsSins() throws IOException {
        String text = Files.readString(FILE, StandardCharsets.UTF_8);
        assertEquals(10, text.split("\"curse\": false", -1).length - 1, "ten ordinary forms");
        assertEquals(4, text.split("\"curse\": true", -1).length - 1, "four curse forms");
        assertEquals(2, text.split("\"sin\": \"greed\"", -1).length - 1, "two Greed forms");
        assertEquals(2, text.split("\"sin\": \"gluttony\"", -1).length - 1, "two Gluttony forms");
        assertFalse(text.contains("\"sin\": \"wrath\""));
    }

    @Test
    void theBaseFormUsesTheUseSlotForBlitzStrike() throws IOException {
        String text = Files.readString(FILE, StandardCharsets.UTF_8);
        assertTrue(text.contains("\"use\": \"sword.blitz\""), "a swing weapon's use key fires its first technique");
    }

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
    void theCheckerRejectsWrongCopiesOfTheFile() throws IOException {
        String text = Files.readString(FILE, StandardCharsets.UTF_8);
        assertTrue(checkText(text.replaceFirst("sword\\.tempest", "sword.no_such_skill")).stream().anyMatch(s -> s.contains("unknown skill")));
        assertTrue(checkText(text.replaceFirst("\"quick_draw\"", "\"no_such_passive\"")).stream().anyMatch(s -> s.contains("unknown passive")));
        assertTrue(checkText(text.replaceFirst("\"attack_speed\": 0.3", "\"attack_speed\": 3.3")).stream().anyMatch(s -> s.contains("attack_speed")));
        assertTrue(checkText(text.replaceFirst("tensura:mithril_ingot", "tensura:no_such_item")).stream().anyMatch(s -> s.contains("verified")));
        assertTrue(checkText(text.replaceFirst("\"sin\": \"gluttony\"", "\"sin\": \"lust\"")).stream().anyMatch(s -> s.contains("sin")));
        assertTrue(checkText(text.replaceFirst("\"parents\": \\[\"hundred_sword\", \"phoenix_sword\"\\]", "\"parents\": [\"hundred_sword\", \"no_form\"]")).stream().anyMatch(s -> s.contains("unknown parent")));
        assertTrue(checkText(text.replaceFirst("\"key3\": \"sword.insatiable_maw\"", "\"key3\": \"sword.lightning_hundred\"")).stream().anyMatch(s -> s.contains("ultimates") || s.contains("no form lists")));
        assertFalse(checkText("{ \"weapon\": \"legendary_sword\", \"forms\": [] }").isEmpty());
    }
}
