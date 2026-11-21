package com.newuniverse.nusmp.doublesui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wiring guard for the Real Double stack (docs/doubles_ui.md): client code stays in client/, the common package names no client class (except the allow-listed packet registration),
 * the server never scans the world for doubles, every packet is validated and rate limited on the server, the old rule is behind a setting, and the new keys, strings and
 * smoke self-tests exist.
 */
class DoublesUiSourceScanTest {
    private static final Path MAIN = Path.of("src/main/java/com/newuniverse/nusmp");

    private static String code(Path f) throws IOException {
        return Files.readString(f).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
    }

    private static String code(String rel) throws IOException { return code(MAIN.resolve(rel)); }

    @Test
    void commonPackageNamesNoClientClassExceptThePacketRegistration() throws IOException {
        try (Stream<Path> files = Files.list(MAIN.resolve("doublesui"))) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".java"))::iterator) {
                String c = code(f);
                String name = f.getFileName().toString();
                assertFalse(c.contains("net.minecraft.client") || c.contains("com.mojang.blaze3d"), name + " names a vanilla client class");
                assertFalse(c.contains("Minecraft.getInstance"), name + " uses Minecraft.getInstance");
                if (!name.equals("DoublesUiNet.java")) assertFalse(c.contains("nusmp.client"), name + " names a mod client class");
            }
        }
        String net = code("doublesui/DoublesUiNet.java");
        assertTrue(net.contains("-> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.doublesui.DoublesClient.onRoster(p))"), "the client handler must sit inside the lambda");
        assertTrue(Files.readString(Path.of("docs/server_safe_allow.txt")).contains("doublesui/DoublesUiNet.java"), "the packet registration is not in the server-safe allow-list");
    }

    @Test
    void theClientSideLivesInClientAndIsOnlyReachedFromTheClientSection() throws IOException {
        for (String f : List.of("DoublesClient", "DoublesOverlay", "DoublesView", "DoublesClientConfig"))
            assertTrue(Files.exists(MAIN.resolve("client/doublesui/" + f + ".java")), f + " is not under client/doublesui");
        String nusmp = code("NUSMP.java");
        int clientBlock = nusmp.indexOf("FMLEnvironment.dist == Dist.CLIENT");
        int init = nusmp.indexOf("client.doublesui.DoublesClient.init(");
        assertTrue(clientBlock >= 0 && init > clientBlock, "DoublesClient.init must be inside the Dist.CLIENT block of NUSMP");
        assertTrue(nusmp.contains("DoublesUiNet::registerPayloads") && nusmp.contains("DoublesRoster.registerEvents(") && nusmp.contains("DoublesUiConfig.SPEC")
                && nusmp.contains("DoublesUiSelfTests.register()"), "NUSMP does not wire the server half");
        String client = code("client/doublesui/DoublesClient.java");
        assertTrue(client.contains("ModConfig.Type.CLIENT") && client.contains("nusmp-doubles-ui-client.toml"), "the look settings must be a CLIENT config");
        assertTrue(code("client/doublesui/DoublesClientConfig.java").contains("\"doublesStackedUi\", true"), "doublesStackedUi must default to true");
        assertTrue(code("client/doublesui/DoublesOverlay.java").contains("GuiMove.report(\"doubles_stack\""), "the stack does not report to GUI Move");
        assertTrue(code("client/doublesui/DoublesOverlay.java").contains("!DoublesClientConfig.stackedUi()"), "doublesStackedUi = false must draw nothing (the old screen)");
    }

    @Test
    void gameplaySettingsAreServerConfigs() throws IOException {
        String cfg = code("doublesui/DoublesUiConfig.java");
        assertTrue(cfg.contains("\"selectedDoubleTrades\"") && cfg.contains("\"enabled\""), "the server switches are missing");
        assertTrue(code("NUSMP.java").contains("ModConfig.Type.SERVER, com.newuniverse.nusmp.doublesui.DoublesUiConfig.SPEC"), "the gameplay settings must be a SERVER config");
    }

    @Test
    void theServerNeverScansTheWorldAndValidatesEveryRequest() throws IOException {
        String roster = code("doublesui/DoublesRoster.java");
        for (String bad : List.of("getEntitiesOfClass", ".getEntities(", "getChunk", "getAllEntities", "getEntitiesOfClass"))
            assertFalse(roster.contains(bad), "DoublesRoster scans the world with " + bad);
        assertTrue(roster.contains("LIMIT.allow("), "the selection is not rate limited");
        assertTrue(roster.contains("DoublesUiConfig.enabled()"), "the selection ignores the enabled switch");
        assertTrue(roster.contains("d.level() != level") && roster.contains("REACH * REACH") && roster.contains("o.list.get(id)"), "SET does not check owner, level and range");
        assertTrue(roster.contains("LivingDeathEvent") && roster.contains("PlayerLoggedOutEvent") && roster.contains("PlayerChangedDimensionEvent"), "state is not cleared on death / logout / dimension change");
        assertTrue(roster.contains("MAX_ROWS"), "the roster is not capped");
        String net = code("doublesui/DoublesUiNet.java");
        assertTrue(net.contains("ctx.player() instanceof ServerPlayer sp") && net.contains("DoublesRoster.select(sp, p.op(), p.id())"), "the handler must act for the sender");
        String pay = code("doublesui/S2C_DoublesPayload.java");
        assertTrue(pay.contains("Math.min(Math.max(0, b.readVarInt()), DoublesUiMath.MAX_ROWS)"), "the roster packet does not cap what it reads");
    }

    @Test
    void theOldTradeRuleIsKeptAsTheFallback() throws IOException {
        String works = code("book/MirrorWorks.java");
        assertTrue(works.contains("DoublesRoster.chosen(p.getUUID(), p.level())"), "MirrorWorks does not use the selected double");
        assertTrue(works.contains("out.stream().min((x, y) -> Double.compare(x.distanceToSqr(p), y.distanceToSqr(p)))"), "the nearest-double rule was removed");
        String ent = code("entity/MirrorDoubleEntity.java");
        assertTrue(ent.contains("DoublesRoster.register(this)") && ent.contains("DoublesRoster.unregister(this)"), "the double does not join and leave the roster");
        assertTrue(ent.contains("\"DoubleLife\""), "the old save key is gone");
    }

    @Test
    void keysStringsAndSelfTestsExist() throws IOException {
        String lang = Files.readString(Path.of("src/main/resources/assets/anime_multiverse/lang/en_us.json"));
        for (String k : List.of("anime_multiverse.doubles.header", "anime_multiverse.doubles.label", "anime_multiverse.doubles.more",
                "key.anime_multiverse.doubles_next", "key.anime_multiverse.doubles_prev", "key.anime_multiverse.doubles_pick"))
            assertTrue(lang.contains("\"" + k + "\""), "missing string " + k);
        String client = code("client/doublesui/DoublesClient.java");
        for (String k : List.of("doubles_next", "doubles_prev", "doubles_pick")) assertTrue(client.contains("key.anime_multiverse." + k), "key " + k + " is not registered");
        assertTrue(client.contains("CATEGORY = \"key.categories.anime_multiverse\""), "the keys must use the mod's Controls category");
        String tests = code("doublesui/DoublesUiSelfTests.java");
        for (String k : List.of("doublesui.math", "doublesui.roster", "doublesui.codec")) assertTrue(tests.contains("\"" + k + "\""), "self-test " + k + " is not registered");
    }

    @Test
    void theHudIsRegisteredWithGuiMoveAndGuiSize() throws IOException {
        assertTrue(code("client/ui/move/GuiMoveRegistry.java").contains("hud(\"doubles_stack\""), "GUI Move does not know the stack");
        assertTrue(code("client/ui/scale/GuiScaleRegistry.java").contains("layer(\"doubles_stack\", \"doubles_stack\""), "GUI Size does not know the stack");
        assertTrue(code("client/doublesui/DoublesClient.java").contains("\"doubles_stack\""), "the layer is not registered under the id GUI Move expects");
    }
}
