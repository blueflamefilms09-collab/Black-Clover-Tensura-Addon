package com.newuniverse.nusmp.devil.dual;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: Double Magic must stay server friendly and cheap (CLAUDE.md rules 9 and 11); the properties that keep it so are pinned by reading the source. Pure (file reads); the working directory is the project root. */
class DualSourceScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().resolve("src/main/java/com/newuniverse/nusmp");
    private static final Path DUAL = ROOT.resolve("devil/dual"), SKILL = ROOT.resolve("skill");

    private static String code(Path file) throws IOException {
        String s = Files.readString(file, StandardCharsets.UTF_8);
        return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)(?<!:)//.*$", "");
    }

    private static List<Path> files() throws IOException {
        List<Path> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(DUAL)) { s.filter(p -> p.toString().endsWith(".java")).sorted().forEach(out::add); }
        for (String f : List.of("DevilArts.java", "DevilMagicSkill.java", "DevilBondSkill.java")) out.add(SKILL.resolve(f));
        return out;
    }

    @Test
    void noClientClassNoMinecraftInstance() throws IOException {
        Pattern client = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(");
        for (Path f : files()) assertFalse(client.matcher(code(f)).find(), f.getFileName() + " names a client-only class");
    }

    @Test
    void noStaticPlayerNoMutableStaticCollectionNoWorldScanNoSpawn() throws IOException {
        for (Path f : files()) {
            String c = code(f), n = f.getFileName().toString();
            assertFalse(Pattern.compile("static\\s+(?:final\\s+|volatile\\s+)*(?:ServerPlayer|Player|FakePlayer)\\s+\\w+").matcher(c).find(), n + " has a static player");
            assertFalse(Pattern.compile("static\\s+[^;=(]*(?:HashMap|ArrayList|HashSet|ConcurrentHashMap|LinkedHashMap)<").matcher(c).find(), n + " keeps a mutable static collection");
            // the self-test file may spawn one stand-in foe at a time (discarded in a finally block); production code never spawns
            boolean selfTest = n.equals("DevilDualSelfTests.java");
            assertFalse(c.contains("getAllEntities") || c.contains("getChunk(") || c.contains("forceLoad") || (!selfTest && c.contains("addFreshEntity")), n + " scans, loads or spawns");
            assertFalse(c.contains("TickEvent") || c.contains("PlayerTickEvent") || c.contains("LevelTickEvent"), n + " has tick work of its own");
        }
    }

    @Test
    void thePureClassesNameNoMinecraftType() throws IOException {
        for (String f : List.of("DualRules.java", "HolderRules.java", "DevilMagicRules.java", "CorruptionRules.java", "DarkRitualRules.java", "TimeOrigin.java", "DevilRanks.java"))
            assertFalse(Pattern.compile("(?m)^import (net\\.minecraft|net\\.neoforged|com\\.mojang)").matcher(code(DUAL.resolve(f))).find(), f + " must stay pure (it is unit tested without a game)");
    }

    @Test
    void theArtIsBoundedAndCheap() throws IOException {
        String c = code(SKILL.resolve("DevilArts.java"));
        assertTrue(c.contains("DualRules.MAX_TARGETS"), "the number of foes an art hits must be capped");
        assertTrue(c.contains("foesInCone"), "the art must use the shared, capped target search");
        assertTrue(c.contains("DevilMagic.source"), "the art must hurt with the devil_magic damage type");
        assertTrue(c.contains("DevilMagic.markEffect"), "the effects of the art must be marked as devil magic");
    }

    @Test
    void thePricesAndCooldownsAreChecked() throws IOException {
        String c = code(SKILL.resolve("DevilMagicSkill.java"));
        assertTrue(c.contains("pay(") && c.contains("cooldown(") && c.contains("onCoolDown"), "an art must check its cooldown, pay and set its cooldown");
    }

    @Test
    void theDualConfigIsAServerConfigWithItsOwnFileName() throws IOException {
        String boot = code(ROOT.resolve("devil/DevilBoot.java"));
        assertTrue(Pattern.compile("registerConfig\\(ModConfig\\.Type\\.SERVER,\\s*com\\.newuniverse\\.nusmp\\.devil\\.dual\\.DualConfig\\.SPEC,\\s*\"nusmp-devil-dual-server\\.toml\"").matcher(boot).find());
        assertFalse(code(ROOT.resolve("client/devil/DevilClientConfig.java")).contains("oneOfOne"), "no rule setting may live in the client config");
    }

    @Test
    void theOwnershipEventsAreEventsNotTicks() throws IOException {
        String c = code(DUAL.resolve("DevilOwnership.java"));
        for (String must : new String[] {"PlayerLoggedInEvent", "PlayerLoggedOutEvent", "LivingDeathEvent"}) assertTrue(c.contains(must), "missing " + must);
        assertFalse(c.contains("TickEvent"), "One of One must not tick");
    }
}
