package com.newuniverse.nusmp.grimoire;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.blackclover.MagicType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guide P1 #3: every registered grimoire_<magic> skill (skill.NUSkills, one per MagicType) has a lang name and description. */
class GrimoireSkillLangTest {
    private static String read(String p) throws Exception { return Files.readString(Path.of(p), StandardCharsets.UTF_8); }

    @Test
    void everyGrimoireSkillHasNameAndDescription() throws Exception {
        assertTrue(read("src/main/java/com/newuniverse/nusmp/skill/NUSkills.java")
                .contains("SKILLS.register(\"grimoire_\" + m.name().toLowerCase()"), "the grimoire skill ids are still grimoire_<MagicType>");
        JsonObject lang = JsonParser.parseString(read("src/main/resources/assets/anime_multiverse/lang/en_us.json")).getAsJsonObject();
        assertKeys(lang, "grimoire_magic");
        for (MagicType m : MagicType.values()) assertKeys(lang, "grimoire_" + m.name().toLowerCase());
    }

    @Test
    void fragmentIsMerged() throws Exception {
        JsonObject frag = JsonParser.parseString(read("tools/lang/grimoire_skills.json")).getAsJsonObject();
        JsonObject lang = JsonParser.parseString(read("src/main/resources/assets/anime_multiverse/lang/en_us.json")).getAsJsonObject();
        for (String k : frag.keySet()) assertTrue(lang.has(k) && lang.get(k).equals(frag.get(k)), k + " is merged into en_us.json");
    }

    private static void assertKeys(JsonObject lang, String id) {
        String k = "anime_multiverse.skill." + id;
        assertTrue(lang.has(k) && !lang.get(k).getAsString().isBlank(), k + " has a name");
        assertTrue(lang.has(k + ".description") && !lang.get(k + ".description").getAsString().isBlank(), k + " has a description");
    }
}
