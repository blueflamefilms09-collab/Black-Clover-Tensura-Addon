package com.newuniverse.nusmp.beastwill;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why (docs/beastwill_canon_rework.md): the canon rework is wired into the game in a handful of places (the data folder choice, the server setting, three ability kinds, the milestone cooldowns, the core tooltip,
 * the self-test and the CI list) and nothing can be run here, so each hook is pinned by reading the source. Every assertion looks for a fact, not for a position, so adding code next to it does not break the test.
 * Pure (file reads); the working directory is the project root.
 */
class BeastWillCanonWiringTest {
    private static final Path DIR = Path.of("src/main/java/com/newuniverse/nusmp/beastwill");

    private static String code(String file) throws IOException {
        String s = Files.readString(DIR.resolve(file), StandardCharsets.UTF_8);
        return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)(?<!:)//.*$", "");
    }

    @Test
    void theLoaderPicksItsFolderThroughTheCanonRule() throws IOException {
        String l = code("BeastWillLoader.java");
        assertTrue(l.contains("BeastWillCanon.rootFor(BeastWillConfig.classicWills(), BeastWillConfig.canonRework())"), "the folder comes from the pure rule");
        assertTrue(l.contains("\"beast_will_classic\"") && l.contains("\"beast_will\""), "the old folders keep their names");
    }

    @Test
    void theSettingIsAServerSettingThatDefaultsToOn() throws IOException {
        String c = code("BeastWillConfig.java");
        assertTrue(Pattern.compile("define\\(\\s*\"canonRework\"\\s*,\\s*true\\s*\\)").matcher(c).find(), "canonRework defaults to true");
        assertTrue(c.contains("public static boolean canonRework()"), "a getter that survives a config not loaded yet");
        assertTrue(c.contains("CANON_REWORK"), "the value is declared");
        assertFalse(code("BeastWillBoot.java").contains("ModConfig.Type.CLIENT"), "gameplay settings stay server settings");
    }

    @Test
    void theThreeNewKindsAreRunByTheAbilitiesAndBoundedByTheRules() throws IOException {
        String a = code("BeastWillAbilities.java"), r = code("BeastWillRules.java");
        for (String k : List.of("tail_sweep", "thunder_chain", "devour")) {
            assertTrue(a.contains("case \"" + k + "\" ->"), k + " has a case in the dispatcher");
            assertTrue(r.contains("Map.entry(\"" + k + "\""), k + " has a limit");
        }
        assertTrue(a.contains("BeastWillCanon.ignites("), "the pulse ignition goes through the canon gate");
        assertTrue(a.contains("BeastWillCanon.chainDamage(") && a.contains("BeastWillCanon.devourHeal("), "the chain and the bite use the pure numbers");
        assertTrue(a.contains("BalanceLaw.heal("), "healing goes through the balance law");
        assertTrue(a.contains("SongTargets.isFoe"), "targets are still picked by the foe rule");
    }

    @Test
    void theNewKindsSpawnNothingAndDrawOnlyThroughTheFacade() throws IOException {
        String a = code("BeastWillAbilities.java");
        int from = a.indexOf("private static boolean sweep(");
        assertTrue(from > 0, "the sweep exists");
        String mine = a.substring(from);
        assertFalse(Pattern.compile("addFreshEntity|\\.setBlock\\(|LightningBolt|\\.spawn\\(|createEntity|Explosion|\\.explode\\(|ParticleTypes\\.|sendParticles\\(|Math\\.random").matcher(mine).find(), "no spawn, no block, no raw particle, no unseeded random");
        assertTrue(mine.contains("cap(a)"), "the targets are cut to the kind's limit");
    }

    @Test
    void theManagerAppliesTheMilestonesOnlyWithTheSettingOn() throws IOException {
        String m = code("BeastWillManager.java");
        assertTrue(m.contains("BeastWillCanon.cooldownFactor(BeastWillConfig.canonRework(), prog.mastery)"), "the cooldown milestones read the setting");
        assertTrue(m.contains("BeastWillCanon.milestone(prog.mastery)") && m.contains("\"milestone\""), "the milestone line");
        assertTrue(m.contains("BeastWillConfig.canonRework()"), "the milestone line is canon only");
        assertTrue(Pattern.compile("final\\s+Map<String,\\s*Integer>\\s+milestones").matcher(m).find(), "the announced milestones are one small map per run state");
    }

    @Test
    void theCoreTooltipReadsTheCanonLinesAndFallsBackToTheOldLore() throws IOException {
        String t = code("BeastWillCoreItem.java");
        assertTrue(t.contains(".canon_lore") && t.contains(".canon_source"), "the two new lines");
        assertTrue(t.contains("BeastWillConfig.canonRework()"), "only with the setting on");
        assertTrue(t.contains("lore"), "the old lore line is still there for the setting off");
    }

    @Test
    void theSelfTestIsRegisteredAndListedForTheSmokeRun() throws IOException {
        assertTrue(code("BeastWillBoot.java").contains("BeastCanonSelfTests.register()"), "registered at boot");
        String s = code("BeastCanonSelfTests.java");
        for (String k : List.of("beastcanon.catalog.load", "beastcanon.rules.bounds", "beastcanon.abilities.nospawn")) assertTrue(s.contains("\"" + k + "\""), k);
        String yml = Files.readString(Path.of(".github/workflows/smoke.yml"), StandardCharsets.UTF_8);
        assertTrue(Pattern.compile("SMOKE_EXPECT:[^\\n]*[\",]beastcanon[,\"]").matcher(yml).find(), "the prefix is in the smoke list");
    }

    @Test
    void thePureClassStaysFreeOfMinecraftTypes() throws IOException {
        assertFalse(Pattern.compile("(?m)^import (net\\.minecraft|net\\.neoforged|com\\.mojang|io\\.github)").matcher(code("BeastWillCanon.java")).find(), "BeastWillCanon must stay pure (it is unit tested without a game)");
        assertFalse(Pattern.compile("net\\.minecraft\\.client\\.|Minecraft\\.getInstance\\(").matcher(code("BeastCanonSelfTests.java")).find(), "the self-test names no client class");
    }
}
