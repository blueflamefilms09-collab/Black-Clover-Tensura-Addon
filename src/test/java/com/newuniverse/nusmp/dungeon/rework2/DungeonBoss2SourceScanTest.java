package com.newuniverse.nusmp.dungeon.rework2;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.dungeon.rework.GuardianRules;
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
 * Dungeon-boss rework 2 (docs/dungeon_bosses_rework2.md): the source rules of rules 9 and 11 for the new code, and the wiring that keeps the old behaviour reachable. The Guardian's brain and the sound
 * classes load no client class, keep no static mutable state, spawn no entity; every hit goes through the helper (the boss damage law); the settings default to on; the Guardian gates on its setting; the
 * themes, the colourway skins and their rows exist; every translation key the brain builds is in the lang fragment; the self-test is registered and in the smoke list.
 */
class DungeonBoss2SourceScanTest {
    private static final String SRC = "src/main/java/com/newuniverse/nusmp/";
    private static final String P1 = SRC + "dungeon/rework/";
    private static final String P2 = SRC + "dungeon/rework2/";
    private static final Path RES = Path.of("src/main/resources/assets/anime_multiverse");

    private static String read(String path) throws IOException { return Files.readString(Path.of(path)); }

    private static int count(String s, String what) {
        int n = 0;
        for (int i = s.indexOf(what); i >= 0; i = s.indexOf(what, i + what.length())) n++;
        return n;
    }

    @Test
    void theNewClassesLoadNoClientClassKeepNoStaticStateAndSpawnNothing() throws IOException {
        Pattern mutableStatic = Pattern.compile("(?m)^\\s*(private |public |protected )?static (?!final)[A-Za-z<>\\[\\], ?]+ \\w+\\s*(=|;)");
        List<String> files = List.of(P1 + "GuardianRules.java", P1 + "GuardianBrain.java", P2 + "DungeonBossSoundRules.java", P2 + "DungeonBossSounds.java", P2 + "BossCues.java", P2 + "DungeonBoss2SelfTests.java");
        for (String f : files) {
            String src = read(f);
            assertFalse(src.contains("net.minecraft.client"), f + " touches the client");
            assertFalse(src.contains("com.mojang.blaze3d"), f + " touches blaze3d");
            assertFalse(src.contains("com.newuniverse.nusmp.client"), f + " touches the mod's client package");
            assertFalse(src.contains("Minecraft.getInstance"), f + " reads the client singleton");
            assertFalse(mutableStatic.matcher(src).find(), f + " has static mutable state");
            assertFalse(src.contains("addFreshEntity"), f + " spawns an entity");
        }
    }

    @Test
    void theGuardiansBrainHitsOnlyThroughTheHelpersAndAsksNoWorldQuery() throws IOException {
        String brain = read(P1 + "GuardianBrain.java");
        assertEquals(0, count(brain, "getEntitiesOfClass"), "the brain asks the helper for players only");
        assertFalse(Pattern.compile("\\.hurt\\(").matcher(brain).find(), "damage goes through DungeonStrikes.strike (the boss damage law)");
        assertFalse(brain.contains("setHealth("), "no touching health");
        assertTrue(brain.contains("DungeonStrikes.strike"), "it does strike");
        assertTrue(brain.contains("DungeonStrikes.slow"), "slows go through the control law");
        assertTrue(brain.contains("BossRulesConfig") || read(SRC + "labyrinth/mega/guard/LabyrinthGuardianEntity.java").contains("BossRulesConfig.dungeonGuardians()"), "gated on the setting");
        String strikes = read(P1 + "DungeonStrikes.java");
        assertTrue(strikes.contains("dungeonSounds()"), "the sound helper honours the sounds setting");
        assertTrue(strikes.contains("dungeonThemes()") || strikes.contains("theme("), "the theme helper exists");
    }

    @Test
    void theSettingsExistAndDefaultToOn() throws IOException {
        String cfg = read(SRC + "boss/rules/BossRulesConfig.java");
        for (String k : List.of("guardians", "sounds", "themes")) assertTrue(cfg.contains(".define(\"" + k + "\", true)"), k + " defaults to on");
        assertTrue(cfg.contains("dungeonGuardians()") && cfg.contains("dungeonSounds()") && cfg.contains("dungeonThemes()"));
        assertTrue(cfg.contains("get(DUNGEON_REWORK) && get(DUNGEON_GUARDIANS)") && cfg.contains("get(DUNGEON_REWORK) && get(DUNGEON_SOUNDS)"), "each toggle rides on the master switch");
    }

    @Test
    void theGuardianKeepsItsOldFightBehindTheSetting() throws IOException {
        String g = read(SRC + "labyrinth/mega/guard/LabyrinthGuardianEntity.java");
        assertTrue(g.contains("BossRulesConfig.dungeonGuardians()"));
        assertTrue(g.contains("customMoves()") && g.contains("customMovesTick") && g.contains("customCasting()"));
        assertTrue(g.contains("themedMoves.reset()"), "the brain is cleaned up on death");
        String w = read(SRC + "labyrinth/seal/FloorWardenEntity.java");
        assertTrue(w.contains("} else if (target != null && target.isAlive()) {"), "the old chooser is still there");
        assertTrue(w.contains("private void groundSlam") && w.contains("private boolean shadowStep"), "the old moves are kept");
        assertTrue(w.contains("protected boolean customMoves()") && w.contains("protected void customMovesTick") && w.contains("protected boolean customCasting()"), "the hooks are there");
    }

    @Test
    void theNewSoundRegisterIsHandedToTheModBusAndTheWaveOneBrainsUseTheCues() throws IOException {
        assertTrue(read(SRC + "NUSMP.java").contains("DungeonBossSounds.SOUNDS.register(modEventBus)"));
        for (String f : List.of("WardenBrain.java", "BeastBrain.java")) {
            String src = read(P1 + f);
            assertTrue(src.contains("DungeonStrikes.cue("), f + " plays the new cues");
            assertFalse(src.contains("DungeonStrikes.sound("), f + " no longer plays a bare vanilla sound (the fallback lives in DungeonStrikes.cue)");
        }
        assertTrue(read(SRC + "labyrinth/mega/foundry/TelegraphedAttackGoal.java").contains("BossCues.play"), "the Foundry telegraph has a voice");
    }

    @Test
    void everyKitThemeIsInTheThemeFileAndEveryColourwayHasItsFilesAndRow() throws IOException {
        JsonObject themes = JsonParser.parseString(read("src/main/resources/data/anime_multiverse/particle_themes.json")).getAsJsonObject();
        JsonObject all = themes.has("themes") ? themes.getAsJsonObject("themes") : themes;
        for (String t : List.of("dungeon.boss.warden", "dungeon.boss.direwolf", "dungeon.boss.basilisk", "dungeon.boss.armor")) assertTrue(all.has(t), t + " is in particle_themes.json");
        JsonObject looks = JsonParser.parseString(Files.readString(RES.resolve("playermodel/looks_dungeon.json"))).getAsJsonObject().getAsJsonObject("looks");
        assertTrue(looks.has("floor_warden"), "the original Warden look is still there");
        for (GuardianRules.Kit k : GuardianRules.KITS) {
            assertTrue(all.has(k.theme()), k.theme() + " is in particle_themes.json");
            String id = "guardian_" + k.id();
            assertTrue(looks.has(id), "looks_dungeon.json has the row " + id);
            assertEquals(id, looks.getAsJsonObject(id).get("skin").getAsString());
            for (String f : List.of("textures/entity/playermodel/" + id + ".png", "textures/entity/playermodel/" + id + "_glow.png", "textures/entity/pmx_" + id + ".png", "textures/entity/pmx_" + id + "_glow.png"))
                assertTrue(Files.exists(RES.resolve(f)), f + " is missing");
        }
        String looksJava = read(SRC + "client/playermodel/DungeonLooks.java");
        for (GuardianRules.Kit k : GuardianRules.KITS) assertTrue(looksJava.contains("\"guardian_" + k.id() + "\""), "DungeonLooks knows guardian_" + k.id());
    }

    @Test
    void everyTranslationKeyTheGuardianBrainBuildsIsInTheLangFragment() throws IOException {
        JsonObject frag = JsonParser.parseString(read("tools/lang/dungeon_bosses2.json")).getAsJsonObject();
        String brain = read(P1 + "GuardianBrain.java");
        assertTrue(brain.contains("\"message.anime_multiverse.dboss.guardian.\" + key"));
        assertTrue(frag.has("message.anime_multiverse.dboss.guardian.p2") && frag.has("message.anime_multiverse.dboss.guardian.p3"));
        for (GuardianRules.Kit k : GuardianRules.KITS) assertTrue(frag.has("message.anime_multiverse.dboss.guardian.verdict." + k.id()), "verdict line of " + k.id());
        for (DungeonBossSoundRules.Cue c : DungeonBossSoundRules.CUES) assertTrue(frag.has(c.subtitleKey()), c.subtitleKey());
    }

    @Test
    void theSelfTestIsRegisteredAndInTheSmokeListBeforeScaling() throws IOException {
        assertTrue(read(SRC + "boss/BossHooks.java").contains("DungeonBoss2SelfTests.register()"));
        assertTrue(read(P2 + "DungeonBoss2SelfTests.java").contains("SelfTests.register(\"dboss2\""));
        String smoke = read(".github/workflows/smoke.yml");
        assertEquals(1, count(smoke, ",dboss2,"), "dboss2 is in the smoke list once");
        assertTrue(smoke.indexOf(",dboss2,") < smoke.indexOf(",scaling,boss.rework1"), "it sits before the scaling prefix");
        assertTrue(smoke.contains(",scaling,boss.rework1,boss.rework3,boss.stats,tier\""), "the tail is unchanged");
    }
}
