package com.newuniverse.nusmp.entity.veldanava.pocket;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.compat.maps.DimensionAudit;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why (CLAUDE.md rules 9 and 11, docs/veldanava_skills.md "The pocket dimension"): the pocket maze cannot be walked here, so its server-safety and its cost bounds are pinned by reading the real
 * files. The pure maze names no game class; the world code names no client class, keys everything by UUID, forces chunks only in pairs with unforcing them, gates every handler on its first line,
 * undoes itself on server stop, and is wired once; the dimension data, the audit list, the setting and the smoke test exist.
 */
class PocketSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final Path RES = Path.of("src/main/resources");
    private static final Path DIR = SRC.resolve("entity/veldanava/pocket");

    private static String code(String src) { return src.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", ""); }

    private static String read(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8); }

    private static String file(String name) throws IOException { return code(read(DIR.resolve(name + ".java"))); }

    @Test
    void theMazeIsPureJava() throws IOException {
        String c = file("PocketMaze").replaceAll("(?m)^package .*$", "");
        assertFalse(c.contains("net.minecraft") || c.contains("net.neoforged") || c.contains("com.newuniverse"), "the maze names no game class");
        assertFalse(c.contains("System.currentTimeMillis") || c.contains("Math.random") || c.contains("new Random()"), "the maze is deterministic for a seed");
    }

    @Test
    void theWorldCodeNamesNoClientClassAndNoStaticPlayer() throws IOException {
        String c = file("PocketLabyrinth");
        assertFalse(Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|net\\.neoforged\\.neoforge\\.client\\.|com\\.newuniverse\\.nusmp\\.client\\.|Minecraft\\.getInstance\\(").matcher(c).find());
        assertFalse(c.contains("static ServerPlayer") || c.contains("static LivingEntity") || c.contains("static Player "), "no static current player: everything is keyed by UUID");
        assertTrue(c.contains("Map<UUID, Session> BY_CASTER") && c.contains("Map<UUID, Session> BY_MEMBER"));
    }

    @Test
    void chunksAreForcedAndAlwaysUnforcedAndTheCostIsBounded() throws IOException {
        String c = file("PocketLabyrinth");
        assertTrue(c.contains("setChunkForced(cx, cz, on)"), "forcing goes through one method");
        assertTrue(c.contains("setForced(pocket, s, true)") && c.contains("setForced(pocket, s, false)"), "forced and unforced");
        assertTrue(c.contains("USED[s.slot] = false") && c.contains("USED[slot] = false"), "a room is freed when it ends or the test ends");
        assertTrue(c.contains("freeSlot()") && c.contains("Too many labyrinths"), "the number of mazes is capped");
        assertTrue(c.contains("IsrafilRules.pocketMembers("), "the number of creatures is capped");
        assertFalse(c.contains("addFreshEntity") || c.contains("getChunk(") || c.contains("explode("), "no entity spawns, no chunk reads");
    }

    @Test
    void everyHandlerReturnsEarlyAndTheTickIsProbed() throws IOException {
        String c = file("PocketLabyrinth");
        assertTrue(c.contains("if (BY_CASTER.isEmpty()) return;"), "an idle server pays one map check");
        assertTrue(c.contains("PerfProbe.begin()") && c.contains("PerfProbe.end(SECTION, t0)"), "the tick is probed");
        assertTrue(c.contains("if (moving || BY_MEMBER.isEmpty()"), "the teleport guard returns first when nobody is held");
        assertTrue(c.contains("if (BY_MEMBER.isEmpty() && BY_CASTER.isEmpty()) return;"), "the death handler returns first");
    }

    @Test
    void theMazeUndoesItselfAndIsWiredOnce() throws IOException {
        String c = file("PocketLabyrinth");
        assertTrue(c.contains("onServerStopping") && c.contains("Ending.STOPPED"), "a server stop puts everyone back");
        assertTrue(c.contains("onLogin") && c.contains("sendBack"), "a player who logged out inside is sent back");
        assertTrue(c.contains("onLogout") && c.contains("onDeath"), "logout and death release a member");
        assertTrue(c.contains("e.setCanceled(true)") && c.contains("BlockEvent.BreakEvent") && c.contains("BlockEvent.EntityPlaceEvent") && c.contains("getAffectedBlocks().clear()"), "the maze cannot be broken, built in or blown up");
        String main = code(read(SRC.resolve("NUSMP.java")));
        assertEquals(1, main.split("PocketLabyrinth\\.register", -1).length - 1, "registered exactly once");
        String skill = code(read(SRC.resolve("entity/veldanava/skills/IsrafilSkill.java")));
        assertTrue(skill.contains("PocketLabyrinth.endNear") && skill.contains("PocketLabyrinth.endOf") && skill.contains("PocketLabyrinth.open"), "the skill opens it, the Trumpet and forgetting end it");
        assertTrue(skill.contains("!BalanceLaw.isBoss(t)") && skill.contains("JudgmentConfig.pocketDimension()") && skill.contains("class Cage"), "a boss, or the setting off, gets the old cage");
    }

    @Test
    void theDimensionTheAuditListTheSettingAndTheSmokeTestExist() throws IOException {
        JsonObject type = JsonParser.parseString(read(RES.resolve("data/nusmp/dimension_type/trial.json"))).getAsJsonObject();
        assertEquals(128, type.get("height").getAsInt());
        assertTrue(type.get("has_ceiling").getAsBoolean() && !type.get("has_skylight").getAsBoolean());
        assertEquals(0, type.get("min_y").getAsInt());
        JsonObject dim = JsonParser.parseString(read(RES.resolve("data/nusmp/dimension/trial.json"))).getAsJsonObject();
        assertEquals("nusmp:trial", dim.get("type").getAsString());
        assertEquals("minecraft:the_void", dim.getAsJsonObject("generator").getAsJsonObject("settings").get("biome").getAsString());
        assertTrue(DimensionAudit.NAMES.contains("trial"));
        String cfg = code(read(SRC.resolve("entity/veldanava/skills/JudgmentConfig.java")));
        assertTrue(cfg.contains("POCKET_DIMENSION") && cfg.contains("\"pocketDimension\"") && cfg.contains("pocketDimension()"), "the setting exists (a SERVER config: this class is the server config)");
        String self = code(read(SRC.resolve("entity/veldanava/skills/IsrafilSelfTests.java")));
        assertTrue(self.contains("\"israfil.pocket\""), "the smoke test exists");
        assertTrue(read(Path.of(".github/workflows/smoke.yml")).contains(",israfil,"), "the smoke list expects the israfil prefix");
    }
}
