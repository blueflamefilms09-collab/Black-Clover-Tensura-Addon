package com.newuniverse.nusmp.beastwill;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Beast Will system must stay server friendly and cheap (CLAUDE.md rules 9 and 11), and nothing can be profiled here, so the properties that keep it so are pinned by reading the source: no client class
 * and no {@code Minecraft.getInstance()}, no static player, every per-player map keyed by UUID, no block placed and no entity spawned by an ability (so there is nothing to clean up and no protection to bypass),
 * hard control only through the balance law, targets only through the foe rule, particles only through the budget, every packet validated, the only config a SERVER config. If one of these is replaced by a
 * better one on purpose, change the pin in the same commit and say why. Pure (file reads); the working directory is the project root.
 */
class BeastWillSourceScanTest {
    private static final Path DIR = Path.of("").toAbsolutePath().resolve("src/main/java/com/newuniverse/nusmp/beastwill");

    private static String code(String file) throws IOException {
        String s = Files.readString(DIR.resolve(file), StandardCharsets.UTF_8);
        return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)(?<!:)//.*$", "");
    }

    private static List<String> files() throws IOException {
        List<String> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(DIR)) { s.filter(p -> p.toString().endsWith(".java")).sorted().forEach(p -> out.add(p.getFileName().toString())); }
        return out;
    }

    @Test
    void noClientClassAndNoMinecraftInstanceAnywhere() throws IOException {
        Pattern client = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(");
        for (String f : files()) assertFalse(client.matcher(code(f)).find(), f + " names a client-only class");
    }

    @Test
    void thePureClassesNameNoMinecraftType() throws IOException {
        for (String f : List.of("BeastRank.java", "BeastWillRules.java", "BeastWillDef.java", "BeastWillSchema.java", "BeastWillState.java")) {
            assertFalse(Pattern.compile("(?m)^import (net\\.minecraft|net\\.neoforged|io\\.github|com\\.mojang\\.serialization)").matcher(code(f)).find(), f + " must stay pure (it is unit tested without a game)");
        }
    }

    @Test
    void noStaticPlayerAndEveryPerPlayerMapIsKeyedByUuid() throws IOException {
        for (String f : files()) {
            String c = code(f);
            assertFalse(Pattern.compile("static\\s+(?:final\\s+|volatile\\s+)*(?:ServerPlayer|Player|FakePlayer)\\s+\\w+").matcher(c).find(), f + " has a static player");
            assertFalse(Pattern.compile("static\\s+[^;=(]*Map<(?!UUID|String)[^>]*,").matcher(c).find(), f + " has a static map that is not keyed by UUID (or a plain id)");
        }
    }

    @Test
    void anAbilityNeverPlacesABlockSpawnsAnEntityOrLoadsAChunk() throws IOException {
        Pattern bad = Pattern.compile("addFreshEntity|\\.setBlock\\(|setBlockAndUpdate|destroyBlock|getChunk\\(|forceChunk|setChunkForced|loadChunk|\\.explode\\(|Explosion|LightningBolt|createEntity|\\.spawn\\(|EntityType\\.[A-Z_]+\\.create");
        for (String f : files()) if (!f.equals("BeastWillSelfTests.java")) assertFalse(bad.matcher(code(f)).find(), f + " places a block, spawns an entity, loads a chunk or explodes: a Beast Will is effects and pictures only");
    }

    @Test
    void hardControlGoesThroughTheBalanceLawAndTargetsThroughTheFoeRule() throws IOException {
        String a = code("BeastWillAbilities.java");
        assertTrue(a.contains("BalanceLaw.beginControl") && a.contains("BalanceLaw.controlTicks"), "hard control through the shared cooldown and the caps");
        assertTrue(a.contains("SongTargets.isFoe"), "targets through the foe rule");
        assertTrue(a.contains("FtbTeamHelper.sameTeam") && a.contains("mayInteract"), "teams and protections are respected");
        assertTrue(a.contains("PerfBudgets.particles"), "particles through the budget");
        assertEquals(0, count(a, "Math.random"), "no unseeded randomness");
    }

    @Test
    void everyPacketIsValidatedAndRateLimitedAndTheSyncIsNotPerTick() throws IOException {
        String n = code("BeastWillNet.java");
        assertTrue(n.contains("static boolean valid(int op, int arg)") && n.contains("if (!valid(op, arg)) return;"), "an ask is range-checked before the server looks at it");
        assertTrue(n.contains("ctx.player() instanceof ServerPlayer"), "the sender must be a real server player");
        String m = code("BeastWillManager.java");
        assertTrue(m.contains("BeastWillNet.MIN_GAP_TICKS"), "asks are rate limited");
        assertTrue(m.contains("now - r.lastSent >= 5"), "at most four sync packets a second");
        assertTrue(m.contains("(p.tickCount % 20) == 0"), "the upkeep runs once a second");
    }

    @Test
    void theOnlyConfigIsAServerConfigWithItsOwnFile() throws IOException {
        String b = code("BeastWillBoot.java");
        assertTrue(b.contains("ModConfig.Type.SERVER, BeastWillConfig.SPEC, \"nusmp-beastwill-server.toml\""));
        assertFalse(b.contains("ModConfig.Type.CLIENT"), "gameplay settings are server settings");
    }

    @Test
    void nothingRunsAtLoginWithoutAWillAndTheTickReturnsAtOnce() throws IOException {
        String m = code("BeastWillManager.java");
        assertTrue(m.contains("BeastWillState s = BeastWillStore.peek(p);\n        if (s == null) return;"), "a player with no will costs one lookup per tick");
        assertTrue(code("BeastWillStore.java").contains("p.hasData(STATE)"), "reading never creates the attachment");
    }

    private static int count(String s, String needle) {
        int n = 0;
        for (int i = s.indexOf(needle); i >= 0; i = s.indexOf(needle, i + needle.length())) n++;
        return n;
    }
}
