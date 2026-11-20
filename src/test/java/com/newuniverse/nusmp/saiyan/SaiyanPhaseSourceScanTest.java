package com.newuniverse.nusmp.saiyan;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the source rules of the Dragon Ball rework (docs/dragon_ball/phase_report.md): the server rules are in the server classes, every owner decision is a server switch that is OFF by default, the config has a
 * file of its own, the older ask range was not widened, the new shape is appended, the smoke prefixes are on the list, and the new classes keep the repo's server and cost rules.
 */
class SaiyanPhaseSourceScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final String DIR = "src/main/java/com/newuniverse/nusmp/saiyan/";
    private static final List<String> NEW = List.of("SaiyanNet2.java", "SaiyanPhaseRules.java", "SaiyanRaceRules.java", "SaiyanRitualRules.java", "SaiyanPhaseConfig.java", "SaiyanTechniques4.java", "SaiyanDamage.java",
            "SaiyanRace.java", "SaiyanRitual.java");

    private static String read(String rel) throws IOException { return Files.readString(ROOT.resolve(rel), StandardCharsets.UTF_8); }

    @Test
    void theNewClassesNameNoClientClassAndNoStaticPlayerOrForeignMap() throws IOException {
        Pattern client = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|Minecraft\\.getInstance\\(");
        Pattern player = Pattern.compile("static\\s+(?:final\\s+|volatile\\s+)*(?:ServerPlayer|Player)\\s+\\w+");
        Pattern map = Pattern.compile("static\\s+[^;=(]*Map<(?!UUID)[^;=(]*>\\s*\\w+\\s*[=;]");
        for (String f : NEW) {
            String c = read(DIR + f);
            assertFalse(client.matcher(c).find(), f + " names a client-only class");
            assertFalse(player.matcher(c).find(), f + " has a static player");
            assertFalse(map.matcher(c).find(), f + " has a static map that is not keyed by UUID");
        }
    }

    @Test
    void thePureClassesNameNoGameType() throws IOException {
        for (String f : List.of("SaiyanNet2.java", "SaiyanPhaseRules.java", "SaiyanRaceRules.java", "SaiyanRitualRules.java"))
            assertFalse(Pattern.compile("(?m)^import (net\\.minecraft|net\\.neoforged|io\\.github|com\\.mojang)").matcher(read(DIR + f)).find(), f + " must stay pure");
    }

    @Test
    void everyOwnerDecisionIsAServerSwitchThatIsOffByDefault() throws IOException {
        String c = read(DIR + "SaiyanPhaseConfig.java");
        for (String k : List.of("kaiokenExtraStages", "apeForms", "ritualUsesExistencePoints", "branchKey", "blockAwakening", "realRace"))
            assertTrue(c.contains("define(\"" + k + "\", false)"), k + " must default to off (the safest answer of the plan)");
        assertTrue(read(DIR + "SaiyanBoot.java").contains("ModConfig.Type.SERVER, SaiyanPhaseConfig.SPEC, \"nusmp-saiyan-phase2-server.toml\""), "the settings are SERVER settings in a file of their own");
        assertFalse(c.contains("ModConfig.Type.CLIENT"));
    }

    @Test
    void theOlderAskRangeIsNotWidenedAndTheNewOneIsRoutedThroughItsOwnClass() throws IOException {
        String n = read(DIR + "SaiyanNet.java");
        assertTrue(n.contains("static boolean valid(int op) { return op >= OP_BLAST && op <= OP_ENERGY; }"), "the old range check is untouched");
        assertTrue(n.contains("SaiyanNet2.valid(op)") && n.contains("SaiyanManager.onActionPhase("), "an ask of the new range goes to its own handler");
        String m = read(DIR + "SaiyanManager.java");
        assertTrue(m.contains("static void onActionPhase(") && m.contains("SaiyanNet.MIN_GAP_TICKS") && m.contains("SaiyanNet2.OP_WAVE_RELEASE"), "the new asks are rate limited and the wave release is never dropped");
        assertTrue(m.contains("r.tech4.busy(now)") && m.contains("SaiyanTechniques4.cancel(p, r)") && m.contains("SaiyanRitual.end(p)"), "the new state ticks only while busy and is cleaned up");
    }

    @Test
    void theTechniquesAreBoundedAndSpawnNothing() throws IOException {
        String t = read(DIR + "SaiyanTechniques4.java");
        assertFalse(Pattern.compile("addFreshEntity|\\.setBlock\\(|setBlockAndUpdate|destroyBlock|getChunk\\(|\\.explode\\(|Explosion|LightningBolt|\\.spawn\\(|EntityType\\.[A-Z_]+\\.create").matcher(t).find(), "no entity, block or chunk work");
        assertTrue(t.contains("MAX_RUSH_HITS") && t.contains("MAX_SLAM_TARGETS") && t.contains("MAX_AVATAR_HITS") && t.contains("MAX_CANDIDATES") && t.contains("hasChunkAt("), "targets are capped and a Ki sphere never loads a chunk");
        assertTrue(t.contains("SaiyanAbilities.allowed(") && t.contains("SaiyanAbilities.pay(") && t.contains("SaiyanDamage.ki("), "foes only, the existing stamina and Ki path, the Tensura source");
        String d = read(DIR + "SaiyanDamage.java");
        assertTrue(d.contains("TensuraDamageTypes.MAGIC_GENERIC") && d.contains("BalanceLaw.damage("), "the Tensura magic damage type through the balance law");
        String r = read(DIR + "SaiyanRitual.java");
        assertTrue(r.contains("ConcurrentHashMap.newKeySet()") && r.contains("ritualMax()") && r.contains("SaiyanRitualRules.checkTick("), "the ritual is capped on the whole server and read every tenth tick");
    }

    @Test
    void theShapeIsAppendedAndTheSmokePrefixesAreListed() throws IOException {
        String shape = read("src/main/java/com/newuniverse/nusmp/vfx/VfxShape.java");
        assertTrue(shape.indexOf("GIANT_AVATAR") > shape.indexOf("SKILL_SIGIL,"), "GIANT_AVATAR is appended after SKILL_SIGIL");
        assertTrue(read("src/main/java/com/newuniverse/nusmp/vfx/client/VfxManager.java").contains("GiantAvatarLayer"), "the layer is registered");
        assertTrue(read(".github/workflows/smoke.yml").contains("saiyan.kivfx,db.phase2,saiyan.t3,"), "the self-test prefixes sit right after saiyan.kivfx");
        String boot = read(DIR + "SaiyanBoot.java");
        assertTrue(boot.contains("SaiyanPhaseSelfTests.register()"));
        String st = read(DIR + "SaiyanPhaseSelfTests.java");
        for (String k : List.of("db.phase2.rules", "db.phase2.race", "db.phase2.player", "saiyan.t3.rules", "saiyan.t3.player")) assertTrue(st.contains("\"" + k + "\""), k);
    }

    @Test
    void theLanguageFragmentIsFlatAndHasTheKeysOfEveryOpAndStage() throws IOException {
        String f = read("tools/lang/db_phase2.json").trim();
        assertTrue(f.startsWith("{") && f.endsWith("}"));
        for (int op = SaiyanNet2.FIRST; op <= SaiyanNet2.LAST; op++) assertTrue(f.contains("\"" + SaiyanNet2.nameKey(op) + "\""), "name of op " + op);
        for (int s = SaiyanRaceRules.SAIYAN; s <= SaiyanRaceRules.AUTONOMOUS; s++) assertTrue(f.contains("\"" + SaiyanRaceRules.nameKey(s) + "\""), "name of stage " + s);
        for (String key : List.of("key.anime_multiverse.saiyan_tech_next", "key.anime_multiverse.saiyan_tech_use", "key.anime_multiverse.saiyan_ki_wave", "key.anime_multiverse.saiyan_branch"))
            assertTrue(f.contains("\"" + key + "\""), key);
    }
}
