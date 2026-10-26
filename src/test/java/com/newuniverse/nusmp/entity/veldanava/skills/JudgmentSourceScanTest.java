package com.newuniverse.nusmp.entity.veldanava.skills;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why (CLAUDE.md rules 9 and 11, docs/veldanava_skills.md): the Sandalphon skill cannot be played here, so its server-safety and its cost bounds are pinned by reading the real files. The skill code
 * names no client class (the one allowed hand-off is the card-screen packet), every effect goes through VfxSpawn, nothing scans chunks, changes blocks or spawns entities, the field manager is
 * the only per-tick work and returns on its first line when idle, the claim packet is validated, the config is a SERVER config, and every asset, lang key and smoke prefix exists.
 */
class JudgmentSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final Path RES = Path.of("src/main/resources");
    private static final Path DIR = SRC.resolve("entity/veldanava/skills");

    private static String code(String src) { return src.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", ""); }

    private static String read(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8); }

    private static String file(String name) throws IOException { return code(read(DIR.resolve(name + ".java"))); }

    private static List<Path> files() throws IOException {
        try (Stream<Path> s = Files.list(DIR)) { return s.filter(p -> p.toString().endsWith(".java")).sorted().toList(); }
    }

    @Test
    void theServerCodeNamesNoClientClass() throws IOException {
        Pattern client = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(");
        Pattern ours = Pattern.compile("com\\.newuniverse\\.nusmp\\.client\\.");
        List<String> bad = new ArrayList<>();
        for (Path p : files()) {
            String c = code(read(p));
            if (client.matcher(c).find()) bad.add(p.getFileName().toString());
            if (ours.matcher(c).find() && !p.getFileName().toString().equals("VeldanavaSkillNet.java")) bad.add(p.getFileName() + " (our client package)");
        }
        assertTrue(bad.isEmpty(), "the skill's server code names a client class: " + bad);
        assertTrue(file("VeldanavaSkillNet").contains("client.veldanava.VeldanavaSkillClient.onOpen"), "the one hand-off to the client is the card screen");
    }

    @Test
    void everyEffectGoesThroughTheEffectPacketBudget() throws IOException {
        for (Path p : files()) {
            String c = code(read(p));
            String n = p.getFileName().toString();
            assertFalse(c.contains("ParticleTypes."), n + " names a vanilla particle (go through VfxSpawn)");
            assertFalse(c.contains("sendParticles("), n + " sends particles itself");
            if (!n.equals("JudgmentFx.java") && !n.equals("TrialFx.java") && !n.equals("JibrilFx.java") && !n.equals("SolomonFx.java") && !n.equals("HerculesFx.java")) assertFalse(c.contains("VfxSpawn."), n + " sends an effect outside JudgmentFx, TrialFx, JibrilFx, SolomonFx and HerculesFx");
        }
        String fx = file("JudgmentFx");
        assertEquals(Pattern.compile("VfxSpawn\\.send(Following)?\\(").matcher(fx).results().count(), Pattern.compile("VfxShape\\.JUDGMENT_").matcher(fx).results().count(), "each packet draws exactly one Judgment shape");
        String trial = file("TrialFx");
        assertEquals(Pattern.compile("VfxSpawn\\.send(Following)?\\(").matcher(trial).results().count(), Pattern.compile("VfxShape\\.TRIAL_").matcher(trial).results().count(), "each packet draws exactly one Trial shape");
        String jibril = file("JibrilFx");
        assertEquals(Pattern.compile("VfxSpawn\\.send(Following)?\\(").matcher(jibril).results().count(), Pattern.compile("VfxShape\\.JIBRIL_").matcher(jibril).results().count(), "each packet draws exactly one Jibril shape");
        String solomon = file("SolomonFx");
        assertEquals(Pattern.compile("VfxSpawn\\.send(Following)?\\(").matcher(solomon).results().count(), Pattern.compile("VfxShape\\.SOLOMON_").matcher(solomon).results().count(), "each packet draws exactly one Solomon shape");
        String hercules = file("HerculesFx");
        assertEquals(Pattern.compile("VfxSpawn\\.send(Following)?\\(").matcher(hercules).results().count(), Pattern.compile("VfxShape\\.HERCULES_").matcher(hercules).results().count(), "each packet draws exactly one Hercules shape");
    }

    @Test
    void theSkillScansNoChunksChangesNoBlocksAndSpawnsNothing() throws IOException {
        for (Path p : files()) {
            String c = code(read(p));
            String n = p.getFileName().toString();
            if (n.equals("JudgmentSelfTests.java") || n.equals("IsrafilSelfTests.java") || n.equals("JibrilSelfTests.java") || n.equals("SolomonSelfTests.java") || n.equals("SolomonTerrain.java") || n.equals("HerculesSelfTests.java") || n.equals("HerculesTerrain.java")) continue;
            for (String banned : new String[]{"getChunk(", "getChunkAt(", "setBlock(", "destroyBlock(", "addFreshEntity", "new ItemEntity", "setBlockAndUpdate(", "explode("})
                assertFalse(c.contains(banned), n + " uses " + banned);
        }
    }

    @Test
    void everyEntityQueryIsABoundedBoxAndItsResultIsCapped() throws IOException {
        String skill = file("SandalphonSkill");
        String fields = file("JudgmentFields");
        for (String c : new String[]{skill, fields}) {
            assertFalse(c.contains("getEntities("), "an unbounded entity walk");
            assertFalse(c.contains("getAllEntities("), "a walk over every entity");
        }
        assertTrue(fields.contains("JudgmentRules.MAX_FIELD_TARGETS"), "a field pulse is capped");
        assertEquals(1, Pattern.compile("getEntitiesOfClass\\(").matcher(fields).results().count(), "one box query per pulse");
        assertTrue(skill.contains("MAX_EDICT_TARGETS") && skill.contains("MAX_CLEAVE_TARGETS") && skill.contains("MAX_PRESENCE_TARGETS") && skill.contains("MAX_GAZE_TARGETS"), "every area press is capped");
        assertTrue(file("JudgmentState").contains("max"), "the line-of-sight list is capped");
        String isra = file("IsrafilSkill");
        for (String cap : new String[]{"MAX_WEIGHT_TARGETS", "COLLAPSE_MAX_TARGETS", "TRUMPET_MAX_TARGETS", "SHIFT_MAX_AGGRO", "MAX_FIELD_TARGETS"})
            assertTrue(isra.contains(cap), "Israfil's area work is capped by " + cap);
        assertFalse(isra.contains("getEntities(") || isra.contains("getAllEntities("), "an unbounded entity walk in Israfil");
    }

    @Test
    void israfilsOnlyPerTickWorkIsTheRiftWalkMapAndItReturnsEarlyWhenIdle() throws IOException {
        String rift = file("RiftWalk");
        String tick = rift.substring(rift.indexOf("public static void onServerTick"));
        assertTrue(tick.contains("if (SESSIONS.isEmpty()) return;"), "an idle rift manager does nothing");
        assertTrue(rift.contains("Map<UUID, Session>"), "walks are keyed by the player's UUID");
        assertTrue(rift.contains("RIFT_SECONDS_MASTERED") || rift.contains("endsAt"), "a walk has a hard time limit");
        String ev = file("IsrafilEvents");
        assertEquals(1, Pattern.compile("ServerTickEvent").matcher(ev.replaceAll("import [^;]+;", "")).results().count(), "one tick hook in Israfil's events");
        assertFalse(ev.contains("PlayerTickEvent") || ev.contains("EntityTickEvent") || ev.contains("LevelTickEvent"), "a per-player or per-entity tick hook");
        for (String way : new String[]{"onDeath", "onLogout", "onLogin", "onServerStopping"}) assertTrue(ev.contains(way), "the rift is cleaned up on " + way);
        String skill = file("IsrafilSkill");
        assertTrue(skill.contains("RiftWalk.begin(") && skill.contains("RiftWalk.end("), "Rift Walk starts and ends through the one manager");
    }

    @Test
    void theOnlyPerTickWorkIsTheFieldManagerAndItReturnsEarlyWhenIdle() throws IOException {
        String ev = file("JudgmentEvents");
        assertEquals(1, Pattern.compile("ServerTickEvent").matcher(ev.replaceAll("import [^;]+;", "")).results().count(), "one tick hook in the events");
        assertFalse(ev.contains("PlayerTickEvent") || ev.contains("EntityTickEvent") || ev.contains("LevelTickEvent"), "a per-player or per-entity tick hook");
        String f = file("JudgmentFields");
        String tick = f.substring(f.indexOf("public static void onServerTick"));
        assertTrue(tick.substring(0, tick.indexOf('\n', tick.indexOf('\n') + 1)).contains("FIELDS.isEmpty()") || tick.contains("if (FIELDS.isEmpty()) return;"), "an idle field manager does nothing");
        assertTrue(f.contains("PerfProbe.begin()") && f.contains("PerfProbe.end("), "the field tick is on the perf probe");
        assertTrue(f.contains("JudgmentConfig.fieldsPerPlayer()") && f.contains("JudgmentConfig.fieldsTotal()") && f.contains("JudgmentRules.VERDICT_TICKS"), "fields are capped per player, in total and in time");
    }

    @Test
    void theClaimPacketIsValidatedOnTheServer() throws IOException {
        String net = file("VeldanavaSkillNet");
        assertTrue(net.contains("readUtf(ID_MAX)") && net.contains("writeUtf(p.id(), ID_MAX)"), "the id is read with a length limit");
        assertTrue(net.contains("ctx.player() instanceof ServerPlayer"), "the sender must be a server player");
        assertTrue(net.contains("WillOfTheCreator.waitingFor(p)"), "the player must have an orb waiting");
        assertTrue(net.contains("registrar(\"1\").optional()"), "a client without the mod can still join");
        String orb = code(read(SRC.resolve("entity/veldanava/WillOfTheCreator.java")));
        assertTrue(orb.contains("JudgmentRules.claimable("), "the orb judges the card by the rules table");
        assertTrue(orb.contains("VeldanavaSkills.grant("), "the skill is given through the one grant");
        String sk = file("VeldanavaSkills");
        assertTrue(sk.contains("!JudgmentRules.claimable(id) || received(p)"), "a skill is granted once");
    }

    @Test
    void theConfigIsAServerConfigWithItsOwnFileName() throws IOException {
        String main = code(read(SRC.resolve("NUSMP.java")));
        assertTrue(main.contains("ModConfig.Type.SERVER, com.newuniverse.nusmp.entity.veldanava.skills.JudgmentConfig.SPEC, \"nusmp-veldanava-skills-server.toml\""), "the skill's settings change gameplay, so they are a SERVER config");
        String cfg = file("JudgmentConfig");
        for (String key : new String[]{"enabled", "rewardScreen", "costScale", "cooldownScale", "debtDamage", "finalJudgmentCap", "activeDamageCap", "executePlayers", "executeBosses", "debtMax", "fieldsPerPlayer", "fieldsTotal", "pocketDimension", "strictGovernance", "wingsFlight", "solomonTerrainDamage", "solomonSpellReplay", "solomonMagicBypass", "herculesTerrainDamage", "herculesBlockBreak", "herculesShockwave"})
            assertTrue(cfg.contains("\"" + key + "\""), "config key missing: " + key);
        assertTrue(cfg.contains("executePlayers\", true") && cfg.contains("executeBosses\", false"), "players are executed by default (owner, 3.14.0), bosses are not");
    }

    @Test
    void thePlayerStateIsKeyedByPlayerNotByAStaticCurrentPlayer() throws IOException {
        for (Path p : files()) {
            String c = code(read(p));
            String n = p.getFileName().toString();
            assertFalse(Pattern.compile("static\\s+(?!final)[\\w<>\\[\\], ?]*\\b(ServerPlayer|Player|LivingEntity)\\s+\\w+\\s*[;=]").matcher(c).find(), n + " holds a static player");
            assertFalse(Pattern.compile("\\bMinecraft\\b").matcher(c).find(), n + " reaches for Minecraft");
        }
        String fields = file("JudgmentFields");
        assertTrue(fields.contains("final UUID owner"), "a field is keyed by the owner's UUID");
    }

    @Test
    void theHungerBanAndTheFoodBarAreRespected() throws IOException {
        for (Path p : files()) {
            String c = code(read(p));
            String n = p.getFileName().toString();
            assertFalse(c.contains("MobEffects.HUNGER"), n + " applies the Hunger effect (the owner's hunger ban)");
            assertFalse(c.contains("setFoodLevel(") || c.contains("setSaturation(") || c.contains("setExhaustion(") || c.contains("addExhaustion("), n + " writes the food bar");
        }
    }

    @Test
    void theTimeMagicVfxStaysUntouched() throws IOException {
        for (Path p : files()) assertFalse(code(read(p)).contains("TimeMagicLayer"), p.getFileName() + " names the Time Magic layer");
        Path layer = SRC.resolve("vfx/client/layer/JudgmentLayer.java");
        if (Files.exists(layer)) {
            String l = code(read(layer));
            assertFalse(l.contains("TimeMagicLayer") || l.contains("time_"), "the Judgment layer copies nothing of the Time layer");
        }
    }

    @Test
    void everyAssetLangKeyAndSmokePrefixExists() throws IOException {
        assertTrue(Files.exists(RES.resolve("assets/nusmp/textures/skill/veldanava/sandalphon.png")), "missing skill icon");
        JsonObject lang = JsonParser.parseString(read(RES.resolve("assets/nusmp/lang/en_us.json"))).getAsJsonObject();
        for (String k : new String[]{"nusmp.skill.sandalphon", "nusmp.skill.sandalphon.description", "nusmp.skill.sandalphon.mastered", "effect.nusmp.temporal_stutter", "effect.nusmp.temporal_debt", "effect.nusmp.event_silence",
                "effect.nusmp.future_rejection", "effect.nusmp.movement_interference", "effect.nusmp.action_lock", "nusmp.veldanava.skills.title", "nusmp.veldanava.skills.hint", "nusmp.veldanava.skills.take"})
            assertTrue(lang.has(k), "missing lang key " + k);
        for (JudgmentRules.Mode m : JudgmentRules.SANDALPHON.modes()) assertTrue(lang.has("nusmp.skill.mode.sandalphon." + m.id()), "missing lang key for mode " + m.id());
        String smoke = read(Path.of(".github/workflows/smoke.yml"));
        assertTrue(Pattern.compile("SMOKE_EXPECT:[^\\n]*[\", ]sandalphon[\",]").matcher(smoke).find(), "the smoke list must expect the sandalphon self-tests");
        String tests = file("JudgmentSelfTests");
        for (String key : new String[]{"sandalphon.rules", "sandalphon.skill", "sandalphon.conditions"}) assertTrue(tests.contains("\"" + key + "\""), "self-test missing: " + key);
        assertTrue(code(read(SRC.resolve("NUSMP.java"))).contains("JudgmentSelfTests.register()"), "the self-tests are registered");

        assertTrue(Files.exists(RES.resolve("assets/nusmp/textures/skill/veldanava/israfil.png")), "missing Israfil icon");
        for (String k : new String[]{"nusmp.skill.israfil", "nusmp.skill.israfil.description", "nusmp.skill.israfil.mastered", "effect.nusmp.gravity_crush", "effect.nusmp.nihility_collapse"})
            assertTrue(lang.has(k), "missing lang key " + k);
        for (JudgmentRules.Mode m : IsrafilRules.ISRAFIL.modes()) assertTrue(lang.has("nusmp.skill.mode.israfil." + m.id()), "missing lang key for mode " + m.id());
        assertTrue(Pattern.compile("SMOKE_EXPECT:[^\\n]*[\", ]israfil[\",]").matcher(smoke).find(), "the smoke list must expect the israfil self-tests");
        String israfilTests = file("IsrafilSelfTests");
        for (String key : new String[]{"israfil.rules", "israfil.skill", "israfil.conditions"}) assertTrue(israfilTests.contains("\"" + key + "\""), "self-test missing: " + key);
        assertTrue(code(read(SRC.resolve("NUSMP.java"))).contains("IsrafilSelfTests.register()") && code(read(SRC.resolve("NUSMP.java"))).contains("IsrafilEvents.register("), "Israfil's tests and events are wired");
        assertTrue(Files.exists(RES.resolve("assets/nusmp/textures/skill/veldanava/jibril.png")), "missing Jibril icon");
        for (String k : new String[]{"nusmp.skill.jibril", "nusmp.skill.jibril.description", "nusmp.skill.jibril.mastered"})
            assertTrue(lang.has(k), "missing lang key " + k);
        for (JudgmentRules.Mode m : JibrilRules.JIBRIL.modes()) assertTrue(lang.has("nusmp.skill.mode.jibril." + m.id()), "missing lang key for mode " + m.id());
        assertTrue(Pattern.compile("SMOKE_EXPECT:[^\\n]*[\", ]jibril[\",]").matcher(smoke).find(), "the smoke list must expect the jibril self-tests");
        String jibrilTests = file("JibrilSelfTests");
        for (String key : new String[]{"jibril.rules", "jibril.skill", "jibril.conditions"}) assertTrue(jibrilTests.contains("\"" + key + "\""), "self-test missing: " + key);
        assertTrue(code(read(SRC.resolve("NUSMP.java"))).contains("JibrilSelfTests.register()") && code(read(SRC.resolve("NUSMP.java"))).contains("JibrilEvents.register("), "Jibril's tests and events are wired");
        assertTrue(Files.exists(RES.resolve("assets/nusmp/textures/skill/veldanava/solomon.png")), "missing Solomon icon");
        for (String k : new String[]{"nusmp.skill.solomon", "nusmp.skill.solomon.description", "nusmp.skill.solomon.mastered"})
            assertTrue(lang.has(k), "missing lang key " + k);
        for (JudgmentRules.Mode m : SolomonRules.SOLOMON.modes()) assertTrue(lang.has("nusmp.skill.mode.solomon." + m.id()), "missing lang key for mode " + m.id());
        assertTrue(Pattern.compile("SMOKE_EXPECT:[^\\n]*[\", ]solomon[\",]").matcher(smoke).find(), "the smoke list must expect the solomon self-tests");
        String solomonTests = file("SolomonSelfTests");
        for (String key : new String[]{"solomon.rules", "solomon.skill", "solomon.conditions"}) assertTrue(solomonTests.contains("\"" + key + "\""), "self-test missing: " + key);
        assertTrue(code(read(SRC.resolve("NUSMP.java"))).contains("SolomonSelfTests.register()") && code(read(SRC.resolve("NUSMP.java"))).contains("SolomonEvents.register("), "Solomon's tests and events are wired");
        assertTrue(Files.exists(RES.resolve("assets/nusmp/textures/skill/veldanava/hercules.png")), "missing Hercules icon");
        for (String k : new String[]{"nusmp.skill.hercules", "nusmp.skill.hercules.description", "nusmp.skill.hercules.mastered", "effect.nusmp.armor_break"})
            assertTrue(lang.has(k), "missing lang key " + k);
        for (JudgmentRules.Mode m : HerculesRules.HERCULES.modes()) assertTrue(lang.has("nusmp.skill.mode.hercules." + m.id()), "missing lang key for mode " + m.id());
        assertTrue(Pattern.compile("SMOKE_EXPECT:[^\\n]*[\", ]hercules[\",]").matcher(smoke).find(), "the smoke list must expect the hercules self-tests");
        String herculesTests = file("HerculesSelfTests");
        for (String key : new String[]{"hercules.rules", "hercules.skill", "hercules.conditions"}) assertTrue(herculesTests.contains("\"" + key + "\""), "self-test missing: " + key);
        assertTrue(code(read(SRC.resolve("NUSMP.java"))).contains("HerculesSelfTests.register()") && code(read(SRC.resolve("NUSMP.java"))).contains("HerculesEvents.register("), "Hercules' tests and events are wired");
    }

    @Test
    void solomonsAreaWorkTerrainAndReplayAreCappedAndBehindTheirSettings() throws IOException {
        String skill = file("SolomonSkill");
        assertFalse(skill.contains("getEntities(") || skill.contains("getAllEntities("), "an unbounded entity walk in Solomon");
        assertTrue(skill.contains("JudgmentConfig.") || file("SolomonHooks").contains("JudgmentConfig."), "Solomon reads its settings");
        String terrain = file("SolomonTerrain");
        assertEquals(1, Pattern.compile("\\.setBlock\\(").matcher(terrain).results().count(), "one place removes blocks");
        for (String guard : new String[]{"JudgmentConfig.solomonTerrain()", "RULE_MOBGRIEFING", "DungeonGuard.on(", "isLoaded(", "TERRAIN_MAX_HARDNESS", "getBlockEntity("})
            assertTrue(terrain.contains(guard), "the terrain is guarded by " + guard);
        String pillars = file("SolomonPillars");
        assertTrue(pillars.contains("TERRAIN_MAX_BLOCKS") && pillars.contains("TERRAIN_PER_PILLAR") && pillars.contains("pillarDamage(") && pillars.contains("PILLAR_IMPACT_MAX_CREATURES"), "the pillars are capped in blocks, hits and creatures");
        assertFalse(pillars.contains("setBlock(") || pillars.contains("destroyBlock("), "the pillars change blocks only through SolomonTerrain");
        String casts = file("SolomonCasts");
        assertTrue(casts.contains("JudgmentConfig.solomonReplay()") && casts.contains("SafeCast.run(") && casts.contains("LOG_SIZE") && casts.contains("OWN_LOG"), "replays are behind their setting, guarded and the log is capped");
        String ev = file("SolomonEvents");
        assertEquals(0, Pattern.compile("PlayerTickEvent|EntityTickEvent|LevelTickEvent|ServerTickEvent").matcher(ev.replaceAll("import [^;]+;", "")).results().count(), "Solomon's events add no tick hook (its second runs from the skill)");
        for (String way : new String[]{"onDeath", "onLogout", "onServerStopping"}) assertTrue(ev.contains(way), "Solomon's state is cleaned up on " + way);
    }

    @Test
    void herculesAreaWorkTerrainAndThrowAreCappedAndBehindTheirSettings() throws IOException {
        String skill = file("HerculesSkill");
        for (String cap : new String[]{"TREMOR_MAX_CREATURES", "TREMOR_MAX_BLOCKS", "ROAR_MAX", "STRIKE_MAX", "ROAR_CLEANSE_MAX"})
            assertTrue(skill.contains(cap), "Hercules' area work is capped by " + cap);
        assertFalse(skill.contains("getEntities(") || skill.contains("getAllEntities("), "an unbounded entity walk in Hercules");
        assertEquals(1, Pattern.compile("getEntitiesOfClass\\(").matcher(skill).results().count(), "one shared box query helper");
        assertFalse(skill.contains("setBlock(") || skill.contains("destroyBlock(") || skill.contains("addFreshEntity"), "Hercules' skill changes blocks only through HerculesTerrain and spawns nothing");
        String terrain = file("HerculesTerrain");
        assertEquals(1, Pattern.compile("\\.setBlock\\(").matcher(terrain).results().count(), "one place removes blocks");
        for (String guard : new String[]{"JudgmentConfig.herculesTerrain()", "RULE_MOBGRIEFING", "DungeonGuard.on(", "isLoaded(", "SolomonTerrain.breakable("})
            assertTrue(terrain.contains(guard), "the terrain is guarded by " + guard);
        String thrown = file("HerculesThrow");
        assertTrue(thrown.contains("THROW_MAX_HITS") && thrown.contains("THROW_BLAST_MAX") && thrown.contains("THROW_CRATER_BLOCKS") && thrown.contains("THROW_MAX_TICKS"), "the throw is capped in hits, blast, crater and time");
        assertFalse(thrown.contains("setBlock(") || thrown.contains("destroyBlock(") || thrown.contains("addFreshEntity"), "the throw changes blocks only through HerculesTerrain and spawns nothing");
        String ev = file("HerculesEvents");
        assertEquals(0, Pattern.compile("PlayerTickEvent|EntityTickEvent|LevelTickEvent|ServerTickEvent").matcher(ev.replaceAll("import [^;]+;", "")).results().count(), "Hercules' events add no tick hook (its second runs from the skill)");
        for (String way : new String[]{"onDeath", "onLogout", "onServerStopping"}) assertTrue(ev.contains(way), "Hercules' state is cleaned up on " + way);
        assertTrue(ev.contains("JudgmentConfig.herculesBreak()") && ev.contains("JudgmentConfig.herculesShockwave()"), "block breaking and the shockwave sit behind their settings");
        assertTrue(ev.contains("HerculesState.take("), "the shockwave and the body picture are spaced");
    }

    @Test
    void jibrilsAreaWorkIsCappedAndItsOnlyPerTickWorkIsTheHotbarHold() throws IOException {
        String skill = file("JibrilSkill");
        for (String cap : new String[]{"PRESENCE_MAX_TARGETS", "GOVERNANCE_MAX_PLAYERS", "PURGE_MAX_CLOUDS", "PURGE_MAX_ALLIES", "KINGDOM_MAX_ENEMIES", "JUDGMENT_MAX_SPLASH", "SHACKLE_STEAL_MAX"})
            assertTrue(skill.contains(cap), "Jibril's area work is capped by " + cap);
        assertFalse(skill.contains("getEntities(") || skill.contains("getAllEntities("), "an unbounded entity walk in Jibril");
        String state = file("JibrilState");
        assertTrue(state.contains("GOVERNANCE_MAX_SKILLS") && state.contains("GOVERNANCE_MAX_MODES") && state.contains("MAX_HOTBAR_LOCKS"), "the governance walk and the hotbar locks are capped");
        assertFalse(state.contains("getEntities(") || state.contains("getAllEntities("), "an unbounded entity walk in Jibril's state");
        String ev = file("JibrilEvents");
        assertEquals(1, Pattern.compile("ServerTickEvent").matcher(ev.replaceAll("import [^;]+;", "")).results().count(), "one tick hook in Jibril's events");
        assertFalse(ev.contains("PlayerTickEvent") || ev.contains("EntityTickEvent") || ev.contains("LevelTickEvent"), "a per-player or per-entity tick hook");
        for (String way : new String[]{"onDeath", "onLogout", "onLogin", "onServerStopping"}) assertTrue(ev.contains(way), "Jibril's state is cleaned up on " + way);
        String tick = state.substring(state.indexOf("static void tickHotbar"));
        assertTrue(tick.substring(0, tick.indexOf('\n', tick.indexOf('\n') + 1)).contains("isEmpty()") || tick.contains("if (HOTBAR.isEmpty()) return;"), "an idle hotbar hold does nothing");
        assertTrue(skill.contains("JudgmentConfig.strictGovernance()") && skill.contains("JudgmentConfig.wingsFlight()"), "the two new toggles sit behind their settings");
    }

    @Test
    void theOldBoonsStayInTheCode() throws IOException {
        String boons = code(read(SRC.resolve("entity/veldanava/VeldanavaBoons.java")));
        assertTrue(boons.contains("TAG_RECEIVED") && boons.contains("TAG_CHOICE") && boons.contains("COUNT"), "the three attribute boons are kept (owner rule 2)");
        String orb = code(read(SRC.resolve("entity/veldanava/WillOfTheCreator.java")));
        assertTrue(orb.contains("VeldanavaBoons.grant("), "the boon grant is still wired for the rewardScreen=false setting");
    }
}
