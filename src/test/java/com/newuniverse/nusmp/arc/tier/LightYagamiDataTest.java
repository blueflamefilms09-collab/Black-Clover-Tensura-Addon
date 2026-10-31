package com.newuniverse.nusmp.arc.tier;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** 3.36.0: Light Yagami (the gag boss): his row, his look, his lang keys, sounds, damage type and server setting are all in place, and the common code stays server-safe. */
class LightYagamiDataTest {
    private static final Path RES = Path.of("src/main/resources/assets/nusmp");
    private static final Path DATA = Path.of("src/main/resources/data");
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    private static TierRow row() { return TierRosterGag.rows().get(0); }

    private static JsonObject json(Path p) throws Exception { return JsonParser.parseString(Files.readString(p)).getAsJsonObject(); }

    @Test
    void theRowObeysTheHouseRulesAndIsALowHealthGagBoss() {
        TierRow r = row();
        assertEquals("light_yagami", r.id());
        assertEquals(List.of(), TierRules.problems(r));
        assertEquals(TierLetter.F, r.letter());
        long health = r.character().health() > 0 ? Math.round(r.character().health()) : 0;
        assertTrue(health > 0 && health <= 60, "a gag boss has very little health, this one has " + health);
        assertTrue(r.character().attack() <= 4.0, "his hits are jokes");
    }

    @Test
    void heIsAppendedLastAndTheSixteenBeforeHimAreUntouched() {
        List<String> ids = TierRoster.ids();
        assertEquals("light_yagami", ids.get(ids.size() - 1));
        List<String> before = new ArrayList<>();
        for (TierRow x : TierRosterF.rows()) before.add(x.id());
        for (TierRow x : TierRosterD.rows()) before.add(x.id());
        assertEquals(before, ids.subList(0, before.size()));
        assertEquals(Set.copyOf(ids).size(), ids.size(), "no repeated id");
    }

    @Test
    void hisPlayerModelSkinFallbackSkinAndLookExist() throws Exception {
        for (String f : new String[]{"textures/entity/playermodel/light_yagami.png", "textures/entity/playermodel/light_yagami_glow.png", "textures/entity/arc_light_yagami.png"})
            assertTrue(Files.isRegularFile(RES.resolve(f)), f);
        JsonObject look = json(RES.resolve("playermodel/looks_tier.json")).getAsJsonObject("looks").getAsJsonObject("light_yagami");
        assertNotNull(look);
        assertTrue(look.has("hair") && look.get("hair").getAsString().startsWith("anime_"), "3.37: anime skin style v2, his hair is the kit's volumetric anime hair");
        assertTrue(look.get("slim").getAsBoolean());
        assertTrue(look.get("glow").getAsBoolean(), "the eyes glow");
    }

    @Test
    void theSkinIsTheOriginalDarkSuitWithTheNotebookMotif() throws Exception {
        java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(RES.resolve("textures/entity/playermodel/light_yagami.png").toFile());
        int k = img.getWidth() / 64;                                    // 3.38.0 HD outfits: 64, 128 or 256 square, the same layout
        assertEquals(img.getWidth(), img.getHeight());
        assertTrue(k >= 1 && img.getWidth() == 64 * k && (k & (k - 1)) == 0, "a power-of-two multiple of 64: " + img.getWidth());
        // body front (u=20,v=20, 8x12): mostly a dark suit (every channel low), with no red tie
        int dark = 0, total = 0, red = 0;
        for (int y = 0; y < 12 * k; y++) for (int x = 0; x < 8 * k; x++) {
            int c = img.getRGB(20 * k + x, 20 * k + y);
            if ((c >>> 24) == 0) continue;
            int r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255;
            total++;
            if (Math.max(r, Math.max(g, b)) < 120) dark++;
            if (r > 120 && g < 50 && b < 60) red++;
        }
        assertTrue(total > 0 && dark * 100 / total >= 55, "the suit must be dark (" + dark + "/" + total + ")");
        assertEquals(0, red, "no red tie: the look is original");
        // the notebook motif: teal pixels in the pocket area (columns 5..6, rows 2..5)
        int teal = 0;
        for (int y = 2 * k; y <= 5 * k + k - 1; y++) for (int x = 5 * k; x <= 6 * k + k - 1; x++) {
            int c = img.getRGB(20 * k + x, 20 * k + y);
            int r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255;
            if (g > r + 40 && b > r + 40) teal++;
        }
        assertTrue(teal >= 3 * k * k, "the notebook motif is missing (" + teal + " teal texels)");
        // the fallback skin (the arc kit's own size) is a readable image too
        java.awt.image.BufferedImage fb = javax.imageio.ImageIO.read(RES.resolve("textures/entity/arc_light_yagami.png").toFile());
        assertTrue(fb.getWidth() >= 64 && fb.getHeight() >= 64);
    }

    @Test
    void everyLangKeyOfTheFragmentIsInTheLanguageFile() throws Exception {
        JsonObject frag = json(Path.of("tools/lang/light_yagami.json"));
        JsonObject en = json(RES.resolve("lang/en_us.json"));
        for (String k : frag.keySet()) assertEquals(frag.get(k).getAsString(), en.has(k) ? en.get(k).getAsString() : null, k);
        for (String k : new String[]{"entity.nusmp.light_yagami", "death.attack.nusmp.death_note", "nusmp.light_yagami.caption.chip", "nusmp.light_yagami.card.note", "nusmp.light_yagami.joke",
                "item.nusmp.light_potato_chip", "subtitles.nusmp.light_yagami.chip_line", "subtitles.nusmp.light_yagami.death_just_as_planned", "subtitles.nusmp.light_yagami.death_i_am_justice"})
            assertTrue(en.has(k), k);
        assertEquals("%1$s died of a heart attack", en.get("death.attack.nusmp.death_note").getAsString());
    }

    @Test
    void theThreeSoundsAreNamedInSoundsJsonAndTheirFilesAreThere() throws Exception {
        JsonObject doc = json(RES.resolve("sounds.json"));
        String src = Files.readString(SRC.resolve("arc/tier/LightSounds.java"));
        String[][] pairs = {{"CHIP_LINE", "light_chip_line"}, {"JUST_AS_PLANNED", "light_death_just_as_planned"}, {"I_AM_JUSTICE", "light_death_i_am_justice"}};
        for (String[] p : pairs) {
            Matcher m = Pattern.compile(p[0] + " = \"([a-z_.]+)\"|" + p[0] + " = \"([a-z_.]+)\"").matcher(src);
            Matcher any = Pattern.compile("\\b" + p[0] + "\\s*=\\s*\"([a-z_.]+)\"").matcher(src);
            assertTrue(any.find(), p[0]);
            String event = any.group(1);
            assertTrue(doc.has(event), "sounds.json has no " + event);
            JsonObject e = doc.getAsJsonObject(event);
            assertTrue(e.has("subtitle"));
            JsonArray sounds = e.getAsJsonArray("sounds");
            assertEquals(1, sounds.size());
            JsonElement s = sounds.get(0);
            String name = s.isJsonObject() ? s.getAsJsonObject().get("name").getAsString() : s.getAsString();
            assertEquals("nusmp:" + p[1], name);
            Path f = RES.resolve("sounds/" + p[1] + ".ogg");
            assertTrue(Files.isRegularFile(f) && Files.size(f) > 1000, "missing or empty " + f);
            byte[] head = Files.readAllBytes(f);
            assertEquals("OggS", new String(head, 0, 4, java.nio.charset.StandardCharsets.US_ASCII), f + " is not an Ogg file");
        }
        assertEquals(2, Pattern.compile("DEATH_QUOTES = \\{JUST_AS_PLANNED, I_AM_JUSTICE\\}").matcher(src).results().count() * 2, "the two death quotes are picked at random");
    }

    @Test
    void theDamageTypeExistsAndBypassesDefencesButNotTheTotem() throws Exception {
        JsonObject t = json(DATA.resolve("nusmp/damage_type/death_note.json"));
        assertEquals("nusmp.death_note", t.get("message_id").getAsString());
        for (String tag : new String[]{"bypasses_armor", "bypasses_effects", "bypasses_enchantments", "bypasses_shield", "bypasses_cooldown", "bypasses_resistance"}) {
            JsonObject j = json(DATA.resolve("minecraft/tags/damage_type/" + tag + ".json"));
            assertTrue(j.getAsJsonArray("values").contains(JsonParser.parseString("\"nusmp:death_note\"")), tag);
        }
        Path inv = DATA.resolve("minecraft/tags/damage_type/bypasses_invulnerability.json");
        if (Files.exists(inv)) assertFalse(Files.readString(inv).contains("death_note"), "the Totem of Undying must keep working");
        // the older severance entries are kept
        assertTrue(Files.readString(DATA.resolve("minecraft/tags/damage_type/bypasses_armor.json")).contains("conceptual_severance"));
    }

    @Test
    void theLethalSettingIsAServerSettingInTheTierBossFileAndDefaultsOn() throws Exception {
        String cfg = Files.readString(SRC.resolve("arc/ArcConfig.java"));
        assertTrue(cfg.contains("\"lightDeathNoteLethal\", true"), "default on");
        assertTrue(cfg.contains("tierBossMax"), "the older keys stay");
        String sys = Files.readString(SRC.resolve("arc/ArcSystem.java"));
        assertTrue(sys.contains("ModConfig.Type.SERVER, ArcConfig.SPEC, \"nusmp-arc-server.toml\""), "gameplay settings are SERVER configs in the arc file");
        assertFalse(cfg.contains("Type.CLIENT"));
    }

    @Test
    void theCommonCodeNamesNoClientClass() throws Exception {
        for (String f : new String[]{"LightScene", "LightScenes", "LightFrame", "LightNet", "LightSounds", "LightEvents", "LightDeathNote", "LightYagamiBoss", "TierRosterGag"}) {
            String code = Files.readString(SRC.resolve("arc/tier/" + f + ".java"));
            assertFalse(code.contains("net.minecraft.client") || code.contains("com.mojang.blaze3d") || code.contains("nusmp.client") || code.contains("Minecraft.getInstance"), f + " touches a client class");
        }
    }

    @Test
    void thePureClassesNameNoGameClassAtAll() throws Exception {
        for (String f : new String[]{"LightScene", "LightScenes", "LightFrame"}) {
            String code = Files.readString(SRC.resolve("arc/tier/" + f + ".java"));
            assertFalse(code.contains("import net.minecraft") || code.contains("import net.neoforged") || code.contains("import com.mojang"), f + " is meant to be pure");
        }
    }

    @Test
    void thePacketRejectsWhatIsOutOfRange() {
        assertTrue(new LightNet.Scene(LightNet.OP_START, 5, 7, LightNet.ROLE_TARGET).valid());
        assertTrue(new LightNet.Scene(LightNet.OP_CANCEL, 5, 7, LightNet.ROLE_BYSTANDER).valid());
        assertFalse(new LightNet.Scene((byte) 0, 5, 7, LightNet.ROLE_TARGET).valid());
        assertFalse(new LightNet.Scene((byte) 9, 5, 7, LightNet.ROLE_TARGET).valid());
        assertFalse(new LightNet.Scene(LightNet.OP_START, 0, 7, LightNet.ROLE_TARGET).valid());
        assertFalse(new LightNet.Scene(LightNet.OP_START, 5, -1, LightNet.ROLE_TARGET).valid());
        assertFalse(new LightNet.Scene(LightNet.OP_START, 5, 7, (byte) 2).valid());
    }

    @Test
    void theSelfTestsAreRegisteredUnderThePrefixTheSmokeTestAlreadyExpects() throws Exception {
        String t = Files.readString(SRC.resolve("arc/tier/TierSelfTests.java"));
        for (String k : new String[]{"tier.light.data", "tier.light.scene", "tier.light.cleanup"}) assertTrue(t.contains("\"" + k + "\""), k);
        assertTrue(Files.readString(Path.of(".github/workflows/smoke.yml")).contains(",tier\""), "the smoke test expects the tier prefix");
    }
}
