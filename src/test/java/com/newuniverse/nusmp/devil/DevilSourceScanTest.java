package com.newuniverse.nusmp.devil;

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

/**
 * Why: the Devil Contract system must stay server friendly and cheap (CLAUDE.md rules 9 and 11) and nothing can be profiled here, so the properties that keep it so are pinned by reading the source: no client
 * class in the common package, no static player and no static map, no world scan and no spawned entity, the once-a-second step, every packet checked and rate limited, the gameplay config a SERVER config and the
 * look config a CLIENT config with distinct file names, and the client layer bounded by a distance and a per-frame cap. If one of these is replaced by a better one on purpose, change the pin in the same commit and
 * say why. Pure (file reads); the working directory is the project root.
 */
class DevilSourceScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().resolve("src/main/java/com/newuniverse/nusmp");
    private static final Path DIR = ROOT.resolve("devil"), CLIENT = ROOT.resolve("client/devil");

    private static String code(Path file) throws IOException {
        String s = Files.readString(file, StandardCharsets.UTF_8);
        return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)(?<!:)//.*$", "");
    }

    private static String common(String f) throws IOException { return code(DIR.resolve(f)); }

    private static String client(String f) throws IOException { return code(CLIENT.resolve(f)); }

    private static List<Path> files(Path dir) throws IOException {
        List<Path> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(dir)) { s.filter(p -> p.toString().endsWith(".java")).sorted().forEach(out::add); }
        return out;
    }

    @Test
    void theCommonPackageNamesNoClientClassAndNoMinecraftInstance() throws IOException {
        Pattern client = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(");
        for (Path f : files(DIR)) assertFalse(client.matcher(code(f)).find(), f.getFileName() + " names a client-only class");
    }

    @Test
    void thePureClassesNameNoMinecraftType() throws IOException {
        for (String f : List.of("DevilRules.java", "DevilTier.java", "DevilAttribute.java", "Sacrifice.java"))
            assertFalse(Pattern.compile("(?m)^import (net\\.minecraft|net\\.neoforged|com\\.mojang)").matcher(common(f)).find(), f + " must stay pure (it is unit tested without a game)");
    }

    @Test
    void noStaticPlayerNoStaticMapAndNothingScansTheWorld() throws IOException {
        for (Path f : files(DIR)) {
            String c = code(f);
            String n = f.getFileName().toString();
            assertFalse(Pattern.compile("static\\s+(?:final\\s+|volatile\\s+)*(?:ServerPlayer|Player|FakePlayer)\\s+\\w+").matcher(c).find(), n + " has a static player");
            assertFalse(Pattern.compile("static\\s+[^;=(]*(?:Map|List|Set)<").matcher(c).find(), n + " keeps a static collection: the state of a player lives in its attachment");
            assertFalse(Pattern.compile("getEntitiesOfClass|getEntities\\(|getNearbyEntities|getChunk\\(|forceChunk|setChunkForced|addFreshEntity|\\.setBlock\\(|setBlockAndUpdate|\\.explode\\(|Math\\.random").matcher(c).find(),
                    n + " scans the world, spawns something or uses unseeded randomness: the devil system is numbers and pictures only");
        }
    }

    @Test
    void particlesOnlyThroughTheBudgetsIfAtAll() throws IOException {
        for (Path f : files(DIR)) assertFalse(code(f).contains("ParticleTypes.") || code(f).contains("sendParticles("), f.getFileName() + " sends a vanilla particle");
    }

    @Test
    void theStepRunsOnceASecondAndOnlyForAPlayerWithAContract() throws IOException {
        String c = common("DevilContracts.java");
        assertTrue(c.contains("p.tickCount % 20 != 0"), "once a second");
        assertTrue(c.contains("if (c == null || !c.hasContract()) return;"), "no contract costs one lookup");
        assertTrue(common("DevilStore.java").contains("player.hasData(CONTRACT)") || common("DevilStore.java").contains("p.hasData(CONTRACT)"), "reading never creates the attachment");
        assertTrue(common("DevilStore.java").contains("copyOnDeath()"), "the contract and its sacrifices go to the new body");
    }

    @Test
    void everyPacketIsCheckedAndTheAskIsRateLimited() throws IOException {
        String n = common("DevilNet.java");
        assertTrue(n.contains("ctx.player() instanceof ServerPlayer"), "the sender must be a real server player");
        assertTrue(n.contains("DevilRules.clampInt(buf.readByte()"), "numbers read from the network are clamped");
        assertTrue(n.contains(".optional()"), "a client without the mod can still join");
        assertTrue(common("DevilContracts.java").contains("DevilRules.askAllowed(c.lastAsk, now)"), "asks are rate limited");
        assertTrue(common("DevilContracts.java").contains("c.sentSelf") && common("DevilContracts.java").contains("c.sentVisual"), "packets go out only when something changed");
        assertTrue(n.contains("sendToPlayersTrackingEntityAndSelf"), "the picture goes to the players who can see the devil, not to everybody");
    }

    @Test
    void gameplayIsAServerConfigAndLooksAreAClientConfigWithOwnFiles() throws IOException {
        assertTrue(common("DevilBoot.java").contains("ModConfig.Type.SERVER, DevilConfig.SPEC, \"nusmp-devil-server.toml\""));
        assertFalse(common("DevilBoot.java").contains("ModConfig.Type.CLIENT"), "gameplay settings are server settings");
        assertTrue(client("DevilClient.java").contains("ModConfig.Type.CLIENT, DevilClientConfig.SPEC, \"nusmp-devil-client.toml\""));
    }

    @Test
    void theClientOnlyAsksAndTheLayerIsBounded() throws IOException {
        String c = client("DevilClient.java");
        assertTrue(c.contains("while (ASSIMILATE.consumeClick())"), "every queued click is drained");
        assertTrue(c.contains("DevilRules.askAllowed(lastAsk, now)"), "the client rate limits its asks too");
        assertTrue(c.contains("MAX_TRACKED") && c.contains("VIS.clear()") && c.contains("removeIf"), "the picture map is capped, emptied on logout and swept");
        assertTrue(c.contains("registerBelowAll") && c.contains("registerAboveAll"));
        String w = client("WegLayer.java");
        assertTrue(w.contains("FAR2") && w.contains("DevilClient.takeSlot()"), "players far away are skipped and the draw count of a frame is capped");
        assertTrue(w.contains("PerfProbe.section(\"c.devil.layer\")"), "the layer is timed");
        assertTrue(w.contains("SafeAdditive.additive("), "glow passes use the repo's safe additive type");
        assertFalse(w.contains("new float[") || w.contains("new Vector") || w.contains("new Quaternionf"), "no allocation in the draw path");
    }

    @Test
    void theTexturesTheLayerNamesExist() throws IOException {
        Path dir = Path.of("").toAbsolutePath().resolve("src/main/resources/assets/nusmp/textures/entity/devil");
        for (String t : List.of("low", "mid", "high", "supreme"))
            for (String s : List.of("", "_slim")) {
                assertTrue(Files.exists(dir.resolve("weg_" + t + s + ".png")), "weg_" + t + s + ".png");
                assertTrue(Files.exists(dir.resolve("weg_" + t + s + "_glow.png")), "weg_" + t + s + "_glow.png");
            }
        for (String f : List.of("horns.png", "wings.png", "wings_glow.png")) assertTrue(Files.exists(dir.resolve(f)), f);
    }
}
