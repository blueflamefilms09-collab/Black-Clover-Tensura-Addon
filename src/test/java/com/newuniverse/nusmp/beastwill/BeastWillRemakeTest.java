package com.newuniverse.nusmp.beastwill;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.skill.rules.BeastSkillRules;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why (docs/beastwill_remake.md): 3.60.0 remakes all 27 Beast Wills (Acquire = a human with borrowed strengths and ONE equipped piece, Integrate = a full armoured beast-humanoid with a signature weapon).
 * The old wills stay as data ({@code data/nusmp/beast_will_classic}, switched on by the server setting {@code beastwill.classicWills}), so this pins: the classic folder is whole and valid, the remade folder
 * has the same 27 ids (they are save keys), every remade will names its piece, form and weapon and has lore, every ability and every finisher incantation has a lang line, and the switches are wired.
 * Pure (file reads); the working directory is the project root.
 */
class BeastWillRemakeTest {
    private static final Path NEW = Path.of("src/main/resources/data/nusmp/beast_will");
    private static final Path OLD = Path.of("src/main/resources/data/nusmp/beast_will_classic");

    private static String[] ids(Path dir) {
        File[] f = dir.toFile().listFiles((d, n) -> n.endsWith(".json"));
        assertTrue(f != null && f.length > 0, dir + " is empty");
        String[] r = new String[f.length];
        for (int i = 0; i < f.length; i++) r[i] = f[i].getName().replace(".json", "");
        java.util.Arrays.sort(r);
        return r;
    }

    private static JsonObject json(Path p) throws Exception { return JsonParser.parseString(Files.readString(p)).getAsJsonObject(); }

    @Test
    void theClassicFolderHoldsTheSame27IdsAndEveryFileStillPassesTheValidator() throws Exception {
        String[] o = ids(OLD), n = ids(NEW);
        assertEquals(27, o.length);
        assertEquals(String.join(",", n), String.join(",", o), "the ids are save keys: the remade set and the classic set carry the same 27");
        for (String id : o) {
            BeastWillSchema.Parsed p = BeastWillSchema.parse(id, Files.readString(OLD.resolve(id + ".json")));
            assertTrue(p.ok(), id + " (classic): " + p.problems());
        }
    }

    @Test
    void everyRemadeWillNamesItsPieceFormAndWeaponAndHasLoreAndAPitch() throws Exception {
        Set<String> names = new HashSet<>();
        for (String id : ids(NEW)) {
            JsonObject d = json(NEW.resolve(id + ".json"));
            assertTrue(names.add(d.get("name").getAsString()), id + ": two wills share a name");
            assertFalse(d.getAsJsonObject("acquire").get("equipPiece").getAsString().isBlank(), id + ": the one piece of Acquire");
            JsonObject in = d.getAsJsonObject("integrate");
            assertFalse(in.get("transformation").getAsString().isBlank(), id + ": the Integrate form");
            assertFalse(in.getAsJsonObject("equip").get("weapon").getAsString().isBlank(), id + ": the signature weapon");
            for (String k : new String[]{"lore", "pitch", "untrained", "designTag"}) assertFalse(d.get(k).getAsString().isBlank(), id + ": " + k);
            assertEquals(3, d.getAsJsonObject("spirit").getAsJsonArray("colors").size(), id + ": three spirit colours");
            for (String ph : new String[]{"acquire", "integrate"})
                for (var a : d.getAsJsonObject(ph).getAsJsonArray("abilities")) assertFalse(a.getAsJsonObject().get("name").getAsString().isBlank(), id + ": an ability without a name");
        }
    }

    @Test
    void everyWillHasALoreLineAndEveryFinisherAnIncantationInTheLang() throws Exception {
        JsonObject en = json(Path.of("src/main/resources/assets/nusmp/lang/en_us.json"));
        for (String id : ids(NEW)) {
            assertTrue(en.has("nusmp.beastwill." + id + ".lore"), id + ": no lore line (shown on the core)");
            assertTrue(en.has("nusmp.beastwill." + id + ".name"), id + ": no name");
            for (String ph : new String[]{"acquire", "integrate"})
                for (var a : json(NEW.resolve(id + ".json")).getAsJsonObject(ph).getAsJsonArray("abilities"))
                    assertTrue(en.has("nusmp.beastwill.ability." + a.getAsJsonObject().get("id").getAsString()), id + ": ability without a lang name");
        }
        for (BeastSkillRules.Spec s : BeastSkillRules.SPECS) {
            assertTrue(en.has("nusmp.beastwill.incant." + s.key()), s.key() + ": the mastery finisher has no incantation");
            assertTrue(Files.exists(NEW.resolve(s.willId() + ".json")), s.willId());
        }
    }

    @Test
    void theSwitchesAreWiredAndTheFinisherSpeaks() throws Exception {
        String loader = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/beastwill/BeastWillLoader.java"));
        assertTrue(loader.contains("CLASSIC_ROOT = \"beast_will_classic\"") && loader.contains("BeastWillConfig.classicWills()"), "the server setting picks the classic folder");
        String cfg = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/beastwill/BeastWillConfig.java"));
        assertTrue(cfg.contains("\"classicWills\"") && cfg.contains("public static boolean classicWills()"));
        String client = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/client/aura/BeastWillSpiritClientConfig.java"));
        assertTrue(client.contains("\"classicSpirits\"") && client.contains("ModConfig.Type.CLIENT") && client.contains("nusmp-beastwill-spirit-client.toml"), "a look-only setting is a CLIENT config");
        assertTrue(Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/NUSMP.java")).contains("BeastWillSpiritClientConfig.init("), "registered on the client");
        String skill = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/skill/BeastSkill.java"));
        assertTrue(skill.contains("nusmp.beastwill.incant.") && skill.contains("m.mastery()"), "only the mastery finisher speaks, one action-bar line to the caster");
    }
}
