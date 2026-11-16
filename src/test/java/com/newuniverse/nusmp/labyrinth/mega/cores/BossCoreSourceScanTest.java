package com.newuniverse.nusmp.labyrinth.mega.cores;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Rules 9 and 11 for the Boss Cores, pinned by reading the source: no client class in common code, no static player, bounded work, one server config, the hook beside the relic, the pure rules free of Minecraft. */
class BossCoreSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final Path DIR = SRC.resolve("labyrinth/mega/cores");
    private static final Pattern CLIENT = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(");

    private static String code(String s) { return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", ""); }

    private static List<Path> files() throws IOException {
        try (Stream<Path> s = Files.list(DIR)) { return s.filter(p -> p.toString().endsWith(".java")).sorted().toList(); }
    }

    @Test
    void commonCodeTouchesNoClientClass() throws IOException {
        assertFalse(files().isEmpty());
        for (Path p : files()) assertFalse(CLIENT.matcher(code(Files.readString(p))).find(), p + " uses a client class");
    }

    @Test
    void noStaticPlayerNoWorldScansNoPacketsNoEntitySpawnsInTicks() throws IOException {
        for (Path p : files()) {
            String c = code(Files.readString(p));
            assertFalse(Pattern.compile("static\\s+(?:volatile\\s+)?(?:Server)?Player\\s+\\w+\\s*[;=]").matcher(c).find(), p + " keeps a static player");
            assertFalse(c.contains("getAllEntities") || c.contains("getEntities().getAll") || c.contains("getEntitiesOfClass"), p + " scans entities");
            assertFalse(c.contains("getChunk(") || c.contains("loadChunk"), p + " touches chunks");
            assertFalse(c.contains("PacketDistributor") || c.contains("CustomPacketPayload"), p + " sends packets (the system has none)");
        }
        String events = code(Files.readString(DIR.resolve("BossCoreEvents.java")));
        assertFalse(events.contains("addFreshEntity"), "the handlers must not spawn entities");
    }

    @Test
    void thePureRulesNameNoMinecraftClass() throws IOException {
        String rules = code(Files.readString(DIR.resolve("BossCoreRules.java")));
        assertFalse(rules.contains("net.minecraft") || rules.contains("net.neoforged") || rules.contains("io.github"), "BossCoreRules must stay pure");
    }

    @Test
    void oneServerConfigWithItsOwnFileNameAndSafeReads() throws IOException {
        String boot = Files.readString(DIR.resolve("BossCoreBoot.java"));
        assertTrue(boot.contains("nusmp-boss-cores-server.toml"));
        assertTrue(boot.contains("ModConfig.Type.SERVER"));
        String cfg = code(Files.readString(DIR.resolve("BossCoreConfig.java")));
        assertFalse(cfg.contains("ModConfig.Type.CLIENT"));
        assertTrue(cfg.contains("catch (RuntimeException notLoaded)"), "a config read before the config is loaded must answer the default");
        assertEquals(1, count(Files.readString(SRC.resolve("NUSMP.java")), "BossCoreBoot.init("), "NUSMP calls the boot class once");
    }

    @Test
    void theBossHookSitsBesideTheRelicWithTheSameGuards() throws IOException {
        String layer = code(Files.readString(SRC.resolve("labyrinth/mega/decennial/DecennialLayer.java")));
        assertTrue(layer.contains("if (!noLoot && !standalone) DecennialRewards.award("), "the relic hook is untouched");
        assertTrue(layer.contains("if (!noLoot && !standalone) com.newuniverse.nusmp.labyrinth.mega.cores.BossCoreAward.award("), "the core hook has the same guards");
        String award = code(Files.readString(DIR.resolve("BossCoreAward.java")));
        assertTrue(award.contains("data.paid(memory)") && award.contains("data.claimLoot(memory)"), "paid once per hall");
        assertTrue(award.contains("if (killer == null) return;"), "a death without a killer pays nothing");
        assertTrue(award.contains("setUnlimitedLifetime") && award.contains("setInvulnerable(true)"), "the drop neither despawns nor burns");
    }

    @Test
    void theStrikerHasARecursionGuardAndAPerTickBound() throws IOException {
        String events = code(Files.readString(DIR.resolve("BossCoreEvents.java")));
        assertTrue(events.contains("inside = true") && events.contains("inside = false"), "the second damage type needs a recursion guard");
        assertTrue(events.contains("budget("), "the second damage type needs a per-player per-tick bound");
        assertTrue(events.contains("onLogout") && events.contains("forget("), "per-player state is dropped at logout");
    }

    @Test
    void theOneSecondHandlerIsSpreadByPlayerId() throws IOException {
        String events = code(Files.readString(DIR.resolve("BossCoreEvents.java")));
        assertTrue(events.contains("(p.tickCount + p.getId()) % 20 != 0"), "the Wellspring runs once a second per player, spread by id");
        assertTrue(events.contains("PerfProbe.begin()"), "the tick handler has a perf probe");
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int at = text.indexOf(needle); at >= 0; at = text.indexOf(needle, at + 1)) n++;
        return n;
    }
}
