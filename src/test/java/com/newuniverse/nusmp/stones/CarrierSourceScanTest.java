package com.newuniverse.nusmp.stones;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Infinity Stones Phase B, pinned by reading the source and the data: the new classes touch no client class and no vanilla particle, the dangerous settings default OFF, the per-player tables are dropped on logout,
 * death and dimension change, nothing scans the world per tick, no act takes a mind, every asset and lang line of the new items and the altar exists, the scepter has its Better Combat file and its mesh.
 */
class CarrierSourceScanTest {
    private static final Path ROOT = Path.of("src/main");
    private static final Path SRC = ROOT.resolve("java/com/newuniverse/nusmp/stones");
    private static final Path ASSETS = ROOT.resolve("resources/assets/anime_multiverse");
    private static final String[] NEW = {"CarrierRules", "CarrierData", "CarrierState", "CarrierKit", "CarrierActs", "SnapActs", "MindActs", "ScepterActs", "CarrierEvents", "VormirAltarBlock", "CarrierSelfTests"};
    private static final Pattern CLIENT = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(");

    private static String code(String s) { return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", ""); }

    private static String read(String cls) throws IOException { return Files.readString(SRC.resolve(cls + ".java")); }

    @Test
    void newCodeHasNoClientClassNoVanillaParticleAndAPureRulesFile() throws IOException {
        for (String c : NEW) {
            String s = read(c);
            assertFalse(CLIENT.matcher(s).find(), c + " names a client class");
            assertFalse(code(s).contains("ParticleTypes."), c + " uses a vanilla particle (use the Fx facade)");
        }
        String rules = read("CarrierRules");
        assertFalse(rules.contains("import net.") || rules.contains("import com.mojang") || rules.contains("import com.newuniverse"), "CarrierRules must stay free of game types");
    }

    @Test
    void theDangerousSettingsAreServerSettingsAndDefaultOff() throws IOException {
        String cfg = code(Files.readString(SRC.resolve("StoneConfig.java")));
        assertTrue(cfg.contains("define(\"snapErase\", false)"), "Erase must default OFF");
        assertTrue(cfg.contains("define(\"altarRequired\", false)"), "the altar requirement must default OFF");
        for (String gentle : new String[]{"carriers", "synergyCircuit", "combos", "snap", "snapToll", "scepter", "telekinesis", "mindLink", "masteryBonus", "altar"})
            assertTrue(cfg.contains("define(\"" + gentle + "\", true)"), gentle + " should default ON");
        assertTrue(read("StoneBoot").contains("ModConfig.Type.SERVER, StoneConfig.SPEC"), "stones settings are SERVER settings");
        assertTrue(cfg.contains("snapCooldownDays") && cfg.contains("maxLifts") && cfg.contains("maxLinks"), "the caps are settings");
    }

    @Test
    void everyPerPlayerTableIsDroppedAndNothingScansTheWorldPerTick() throws IOException {
        String ev = code(read("CarrierEvents"));
        for (String hook : new String[]{"PlayerLoggedOutEvent", "PlayerChangedDimensionEvent", "ServerStoppingEvent"})
            assertTrue(ev.contains(hook), "no cleanup hook " + hook);
        assertTrue(ev.contains("CarrierState.forgetAll(") && ev.contains("CarrierState.forgetMotion(") && ev.contains("CarrierState.clear()"));
        String st = code(read("CarrierState"));
        assertTrue(st.contains("UUID"), "per-player state is keyed by UUID");
        assertFalse(Pattern.compile("static\\s+(?:\\w+\\.)*(ServerPlayer|Player)\\s+\\w+\\s*[;=]").matcher(st + ev).find(), "no static current player");
        for (String c : new String[]{"CarrierEvents", "MindActs", "CarrierState"}) {
            String s = code(read(c));
            assertFalse(s.contains("getAllEntities") || s.contains("loadedChunks"), c + " must not scan the world per tick");
        }
    }

    @Test
    void noActTakesAMind() throws IOException {
        for (String c : new String[]{"MindActs", "ScepterActs", "SnapActs", "CarrierActs"}) {
            String s = code(read(c));
            assertFalse(s.contains("setTarget(") || s.contains("setOwnerUUID") || s.contains("tame("), c + " must not take control of a mind");
        }
    }

    @Test
    void theSnapNeverTouchesAPlayerAndErasesAtMostTheCap() throws IOException {
        String s = code(read("SnapActs"));
        assertTrue(s.contains("CarrierRules.SNAP_TARGET_CAP"), "the erase loop is capped");
        assertTrue(s.contains("CarrierRules.fate("), "every erase goes through the pure fate rule");
        assertTrue(code(read("CarrierKit")).contains("snapErase()"), "the Erase mode is refused while the setting is off");
    }

    @Test
    void theNewItemsAndTheAltarHaveTheirAssetsAndLang() throws IOException {
        JsonObject lang = JsonParser.parseString(Files.readString(ASSETS.resolve("lang/en_us.json"))).getAsJsonObject();
        for (String id : List.of("the_nano_gauntlet", "lokis_scepter")) {
            for (String suffix : new String[]{"", ".desc", ".card"}) assertTrue(lang.has("item.anime_multiverse." + id + suffix), "lang item " + id + suffix);
            for (String f : new String[]{"textures/item/" + id + ".png", "models/item/" + id + ".json", "models/item/" + id + "_icon.json"})
                assertTrue(Files.exists(ASSETS.resolve(f)), "missing " + f);
        }
        assertTrue(lang.has("block.anime_multiverse.vormir_altar"));
        for (String f : new String[]{"blockstates/vormir_altar.json", "models/block/vormir_altar.json", "models/item/vormir_altar.json", "textures/block/vormir_altar_top.png", "textures/block/vormir_altar_side.png"})
            assertTrue(Files.exists(ASSETS.resolve(f)), "missing " + f);
        assertTrue(Files.exists(ROOT.resolve("resources/data/anime_multiverse/loot_table/blocks/vormir_altar.json")));
        assertTrue(Files.readString(ROOT.resolve("resources/data/curios/tags/item/hands.json")).contains("anime_multiverse:the_nano_gauntlet"));
        String fr = Files.readString(ROOT.resolve("resources/data/anime_multiverse/relics/franchise_map.json"));
        assertTrue(fr.contains("\"the_nano_gauntlet\"") && fr.contains("\"lokis_scepter\""));
    }

    @Test
    void theScepterHasItsBetterCombatFileItsMeshAndItsEngravings() throws IOException {
        assertTrue(Files.exists(ROOT.resolve("resources/data/anime_multiverse/weapon_attributes/lokis_scepter.json")));
        assertTrue(Files.exists(ASSETS.resolve("weapon_meshes/lokis_scepter.txt")));
        assertTrue(Files.exists(ASSETS.resolve("textures/entity/weapon/lokis_scepter.png")));
        assertTrue(Files.exists(ASSETS.resolve("textures/entity/weapon/lokis_scepter_glow.png")));
        assertTrue(code(read("CarrierKit")).contains(".engrave("), "Loki's Scepter carries Tensura engravings");
        assertEquals(1, Files.readString(ASSETS.resolve("weapon_classes.json")).split("\"lokis_scepter\"", -1).length - 1);
    }
}
