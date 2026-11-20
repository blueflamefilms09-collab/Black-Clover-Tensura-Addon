package com.newuniverse.nusmp.heroweapon.tracker;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: rules 9 and 11 for the Weapon Tracker, pinned by reading the source: no client class in the common files, every request is checked and rate limited on the server, nothing runs per tick or
 * broadcasts, the reader of the reply is bounded, the setting is a server config in the core's own file, the screen is registered with the GUI frameworks, and every text has its language line.
 */
class WeaponTrackerSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final Pattern CLIENT = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(");

    private static String code(String s) { return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", ""); }

    private static String read(String rel) throws IOException { return Files.readString(SRC.resolve(rel)); }

    private static List<Path> common() throws IOException {
        try (Stream<Path> s = Files.list(SRC.resolve("heroweapon/tracker"))) { return s.filter(p -> p.toString().endsWith(".java")).sorted().toList(); }
    }

    @Test
    void commonCodeTouchesNoClientClassAndTheRulesArePure() throws IOException {
        List<Path> files = common();
        assertTrue(files.size() >= 6, "the tracker files are missing");
        for (Path p : files) assertFalse(CLIENT.matcher(code(Files.readString(p))).find(), p + " names a client class");
        for (String pure : new String[]{"TrackerRules.java", "TrackerLimiter.java"}) {
            String c = Files.readString(SRC.resolve("heroweapon/tracker/" + pure));
            assertFalse(c.contains("import net.") || c.contains("import com.mojang"), pure + " must stay free of Minecraft types");
        }
    }

    @Test
    void theClientFilesLiveUnderClientAndSendOnlyTheEmptyRequest() throws IOException {
        for (String f : new String[]{"client/heroweapon/WeaponTrackerClient.java", "client/heroweapon/WeaponTrackerScreen.java"}) assertTrue(Files.exists(SRC.resolve(f)), f);
        String client = code(read("client/heroweapon/WeaponTrackerClient.java"));
        assertTrue(client.contains("sendToServer(new WeaponTrackerNet.Request())"));
        assertTrue(client.indexOf("sendToServer(") == client.lastIndexOf("sendToServer("), "the client sends more than the one request");
        String screen = code(read("client/heroweapon/WeaponTrackerScreen.java"));
        assertFalse(screen.contains("PacketDistributor"), "the screen must go through WeaponTrackerClient.request()");
        assertTrue(screen.contains("TrackerLimiter.GAP_TICKS"), "the refresh button must wait as long as the server's gap");
        assertTrue(screen.contains("renderBackground(GuiGraphics") && !screen.contains("void render("), "the screen draws in renderBackground (ScreenBlurGuardTest)");
    }

    @Test
    void everyRequestIsCheckedAndRateLimitedOnTheServer() throws IOException {
        String net = code(read("heroweapon/tracker/WeaponTrackerNet.java"));
        assertTrue(net.contains("ctx.player() instanceof ServerPlayer sp"), "the request must come from a real server player");
        assertTrue(net.contains(".optional()"), "a client without the mod must still be able to join");
        assertTrue(net.contains("StreamCodec.unit(new Request())"), "the request carries no data");
        String svc = code(read("heroweapon/tracker/WeaponTrackerService.java"));
        for (String must : new String[]{"sp.connection == null", "LIMITER.allow(sp.getUUID(), server.getTickCount())", "WeaponTrackerNet.send(sp, "})
            assertTrue(svc.contains(must), "the request path lost " + must);
        assertTrue(svc.indexOf("LIMITER.allow(") < svc.indexOf("snapshot(sp)"), "the limiter must run before the register is read");
        String limiter = code(read("heroweapon/tracker/TrackerLimiter.java"));
        assertTrue(limiter.contains("GAP_TICKS = 40") && limiter.contains("SWEEP_AT"), "the gap or the sweep bound changed");
    }

    @Test
    void theReplyIsBoundedWhenItIsWrittenAndWhenItIsRead() throws IOException {
        String net = code(read("heroweapon/tracker/WeaponTrackerNet.java"));
        for (String must : new String[]{"n > kinds.length", "k >= kinds.length", "st >= statuses.length", "holders > MAX_HOLDERS", "readUtf(TrackerRules.MAX_NAME)", "writeUtf(TrackerRules.clean(r.holder()), TrackerRules.MAX_NAME)",
                "Math.min(TrackerRules.maxRows(), t.rows().size())"})
            assertTrue(net.contains(must), "the reply lost its bound " + must);
    }

    @Test
    void nothingRunsPerTickNothingIsBroadcastAndNothingScansTheWorld() throws IOException {
        for (Path p : common()) {
            String c = code(Files.readString(p));
            assertFalse(c.contains("TickEvent") || c.contains("ServerTickEvent") || c.contains("PlayerTickEvent"), p + " runs per tick");
            assertFalse(c.contains("sendToAllPlayers") || c.contains("sendToPlayersNear") || c.contains("sendToPlayersInDimension") || c.contains("broadcast"), p + " broadcasts");
            assertFalse(c.contains("getEntitiesOfClass") || c.contains("getAllEntities") || c.contains("getChunk(") || c.contains("setBlock"), p + " scans or touches the world");
            assertFalse(Pattern.compile("static\\s+(?:volatile\\s+)?(?:Server)?Player\\s+\\w+\\s*[;=]").matcher(c).find(), p + " keeps a static player");
        }
        String svc = code(read("heroweapon/tracker/WeaponTrackerService.java"));
        assertTrue(svc.contains("PlayerLoggedOutEvent") && svc.contains("LIMITER.forget(") && svc.contains("ServerStoppingEvent") && svc.contains("LIMITER.clear()"), "the limiter table is not dropped at logout and server stop");
    }

    @Test
    void theSettingIsAServerConfigInTheCoresOwnFileAndNoNewConfigFileIsRegistered() throws IOException {
        String cfg = read("heroweapon/HeroConfig.java");
        assertTrue(cfg.contains("define(\"trackerShowNames\", true)"), "trackerShowNames must default to true");
        assertTrue(cfg.contains("public static boolean trackerShowNames()"));
        assertTrue(read("heroweapon/HeroCore.java").contains("ModConfig.Type.SERVER, HeroConfig.SPEC, \"nusmp-heroweapon-server.toml\""));
        for (Path p : common()) assertFalse(Files.readString(p).contains("registerConfig("), p + " registers a config file");
        assertTrue(code(read("heroweapon/tracker/WeaponTrackerService.java")).contains("HeroConfig.trackerShowNames()"));
    }

    @Test
    void theFeatureIsWiredIn() throws IOException {
        assertTrue(read("heroweapon/HeroCore.java").contains("WeaponTrackerBoot.init(modBus)"));
        assertTrue(read("multiverse/MultiverseCommands.java").contains("WeaponTrackerService.node()"));
        assertTrue(read("client/heroweapon/HeroClient.java").contains("WeaponTrackerClient.init(modBus)"));
        assertTrue(read("client/ui/scale/GuiScaleRegistry.java").contains("screen(\"weapon_tracker\", \"client.heroweapon.WeaponTrackerScreen\""));
        String move = read("client/ui/move/GuiMoveRegistry.java");
        assertTrue(move.contains("\"client/heroweapon/WeaponTrackerScreen.java\"") && move.contains("\"gui_scale:weapon_tracker\""));
        String boot = read("heroweapon/tracker/WeaponTrackerBoot.java");
        assertTrue(boot.contains("WeaponTrackerNet::register") && boot.contains("WeaponTrackerSelfTests.register()"));
        String tests = code(read("heroweapon/tracker/WeaponTrackerSelfTests.java"));
        assertTrue(tests.contains("\"weapons.tracker.rules\"") && tests.contains("\"weapons.tracker.player\""));
        assertTrue(Files.exists(Path.of("docs/weapon_tracker.md")), "docs/weapon_tracker.md is missing");
    }

    @Test
    void theCommandNodeOnlyAsksTheServiceAndTheKeyIsUnboundByDefault() throws IOException {
        String svc = code(read("heroweapon/tracker/WeaponTrackerService.java"));
        assertTrue(svc.contains("Commands.literal(\"weapons\")"));
        assertFalse(svc.contains("requires("), "the tracker only reads the register: everyone may use it");
        assertTrue(code(read("client/heroweapon/WeaponTrackerClient.java")).contains("InputConstants.UNKNOWN.getValue()"));
    }

    @Test
    void everyTextHasItsLangLine() throws IOException {
        String lang = Files.readString(Path.of("src/main/resources/assets/anime_multiverse/lang/en_us.json"));
        String frag = Files.readString(Path.of("tools/lang/weapon_tracker.json"));
        Set<String> keys = new HashSet<>();
        Matcher t = Pattern.compile("tr\\(\"([a-z_]+(?:\\.[a-z_]+)*)\"").matcher(read("client/heroweapon/WeaponTrackerScreen.java"));
        while (t.find()) keys.add("anime_multiverse.weapon_tracker." + t.group(1));
        for (String chip : new String[]{"all", "legendary", "seven_star", "vassal", "free", "taken", "yours"}) keys.add("anime_multiverse.weapon_tracker.chip." + chip);
        Matcher lit = Pattern.compile("\"(anime_multiverse\\.weapon_tracker\\.[a-z_.]+|key\\.anime_multiverse\\.hero_tracker)\"").matcher(read("heroweapon/tracker/WeaponTrackerService.java") + read("client/heroweapon/WeaponTrackerClient.java"));
        while (lit.find()) keys.add(lit.group(1));
        keys.add("anime_multiverse.gui_scale.gui.weapon_tracker");
        assertTrue(keys.size() >= 30, "the scan found only " + keys.size() + " keys");
        for (String k : keys) {
            assertTrue(frag.contains("\"" + k + "\""), "tools/lang/weapon_tracker.json lacks " + k);
            assertTrue(lang.contains("\"" + k + "\""), "en_us.json lacks " + k);
        }
    }
}
