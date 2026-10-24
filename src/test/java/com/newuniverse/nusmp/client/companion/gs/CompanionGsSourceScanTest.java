package com.newuniverse.nusmp.client.companion.gs;

import com.newuniverse.nusmp.client.ui.gs.GsNoticeAllow;
import com.newuniverse.nusmp.client.ui.gs.GsNoticeQueue;
import org.junit.jupiter.api.Test;

import java.io.IOException;
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
 * Why: the Great Spirit companion screen is client-only, decides nothing, and must stay cheap on a crowded server. Nothing here can be profiled without a game, so the rules are pinned by reading the source:
 * the pure classes name no Minecraft type, the screen sends only a pick, a close and the vanilla carried-slot packet, every pick is an id that is on the view, nothing is allocated per frame in the draw path,
 * the memories are bounded, the screen is registered with the GUI move registry, and the crest mirrors only the one companion line it should.
 */
class CompanionGsSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final Path DIR = SRC.resolve("client/companion/gs");

    private static String read(String rel) throws IOException { return Files.readString(SRC.resolve(rel)); }

    private static String code(String rel) throws IOException {
        return read(rel).replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("//[^\n]*", " ");
    }

    private static String body(String src, String name) {
        java.util.regex.Matcher decl = Pattern.compile("\\b(?:void|boolean|int|float|String)\\s+" + name + "\\(").matcher(src);
        assertTrue(decl.find(), "no method " + name);
        int i = decl.start();
        int open = src.indexOf('{', src.indexOf(')', i));
        int depth = 0;
        for (int k = open; k < src.length(); k++) {
            char c = src.charAt(k);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return src.substring(open, k + 1);
        }
        throw new AssertionError("unbalanced " + name);
    }

    private static final List<String> GAME_SIDE = List.of("GsTalkScreen.java", "GsCanvasImpl.java");

    @Test
    void everyPureClassOfThePackageNamesNoMinecraftType() throws IOException {
        List<String> bad = new ArrayList<>();
        try (Stream<Path> w = Files.list(DIR)) {
            for (Path p : (Iterable<Path>) w.filter(f -> f.toString().endsWith(".java"))::iterator) {
                if (GAME_SIDE.contains(p.getFileName().toString())) continue;
                String c = Files.readString(p);
                if (c.contains("net.minecraft") || c.contains("com.mojang") || c.contains("net.neoforged") || c.contains("Minecraft.getInstance")) bad.add(p.getFileName().toString());
            }
        }
        assertTrue(bad.isEmpty(), "these must stay pure: " + bad);
    }

    @Test
    void theScreenSendsOnlyAPickACloseAndTheCarriedSlot() throws IOException {
        String s = code("client/companion/gs/GsTalkScreen.java");
        assertEquals(2, Pattern.compile("PacketDistributor\\.sendToServer\\(").matcher(s).results().count(), "two asks go up: pick and close");
        assertTrue(s.contains("new CompanionNet.Pick(entityId, id)") && s.contains("new CompanionNet.Close(entityId)"));
        assertEquals(1, Pattern.compile("new ServerboundSetCarriedItemPacket\\(").matcher(s).results().count());
        assertEquals(1, Pattern.compile("handleInventoryMouseClick\\(").matcher(s).results().count(), "one vanilla inventory click, the swap that puts a gift in her hand");
        assertTrue(s.contains("ClickType.SWAP"));
        assertFalse(s.contains("ServerboundContainerClickPacket") || s.contains("PacketDistributor.sendToAllPlayers"), "no hand-built packets");
        String ask = body(s, "ask");
        assertTrue(ask.contains("ids.contains(o.id())") && ask.contains("o.blocked()"), "a pick is an id of the view and not a blocked one");
        String send = body(s, "send");
        assertTrue(send.contains("mc.getConnection() != null"));
        String removed = body(s, "removed");
        assertTrue(removed.contains("toldServer") && removed.contains("!closedByServer"), "closing tells the server once");
    }

    @Test
    void theScreenHoldsNoStaticPlayerNoKeybindAndNoServerClass() throws IOException {
        for (String f : new String[]{"client/companion/gs/GsTalkScreen.java", "client/companion/gs/GsCanvasImpl.java", "client/companion/CompanionScreens.java"}) {
            String c = code(f);
            assertFalse(Pattern.compile("static\\s+(?:final\\s+)?(?:LocalPlayer|Player|ServerPlayer)\\s+\\w+").matcher(c).find(), f + " keeps a static player");
            assertFalse(Pattern.compile("KeyMapping|InputConstants|GLFW_KEY|ServerLevel|MinecraftServer").matcher(c).find(), f);
        }
    }

    @Test
    void theDrawPathAllocatesNothingAtRest() throws IOException {
        String canvas = code("client/companion/gs/GsCanvasImpl.java");
        for (String m : new String[]{"fill", "text", "gradient", "sprite", "item"}) {
            String b = body(canvas, m);
            assertFalse(Pattern.compile("\\bnew\\s|String\\.format|fromNamespaceAndPath|\\+\\s*\"").matcher(b).find(), "GsCanvasImpl." + m + " allocates");
        }
        String painter = code("client/companion/gs/GsTalkPainter.java");
        assertFalse(painter.contains("String.format") || painter.contains("fromNamespaceAndPath"), "the painter builds no string with a format");
        String render = body(code("client/companion/gs/GsTalkScreen.java"), "render");
        assertFalse(Pattern.compile("new (?:ArrayList|HashMap|HashSet|int\\[|GsTalkFrame|GsTalkLayout|GsCanvasImpl)").matcher(render).find(), "render builds a container per frame");
        assertFalse(render.contains("Component.translatable") || render.contains("getString()"), "texts are resolved when the view arrives, never per frame");
    }

    @Test
    void theMemoriesAreBoundedAndClearedOnLogout() throws IOException {
        String g = code("client/companion/gs/GiftMemory.java"), b = code("client/companion/gs/BondStory.java");
        assertTrue(g.contains("MAX_COMPANIONS") && g.contains("MAX_ITEMS") && g.contains("MAX_KEY"));
        assertTrue(b.contains("MAX_COMPANIONS") && b.contains("MAX_TEXT") && b.contains("MAX = 6"));
        String client = code("client/companion/CompanionClient.java");
        assertTrue(client.contains("LoggingOut") && client.contains("CompanionScreens.clear()"), "cleared on logout");
        String screens = code("client/companion/CompanionScreens.java");
        assertFalse(screens.contains("setCanceled") || screens.contains("setMessage"), "the chat listener only reads");
        assertTrue(screens.contains("Cue.USE"), "only the pouch line is read");
        String s = code("client/companion/gs/GsTalkScreen.java");
        assertTrue(s.contains("IDS.size() > 512"), "the item-id cache is bounded");
    }

    @Test
    void theBubbleStaysWithinItsBudgetsAndUsesNoRenderType() throws IOException {
        String c = code("client/companion/BubbleClient.java");
        assertFalse(c.contains("RenderType"), "text only through Font.drawInBatch");
        for (String must : new String[]{"BubbleRules.MAX_SHOWN", "BubbleRules.inDrawRange", "MAX_LINES", "b.wrapped == null", "BubbleStyle.invisible", "BubbleStyle.lines"})
            assertTrue(c.contains(must), "BubbleClient lost: " + must);
        assertEquals(1, Pattern.compile("\\.split\\(").matcher(c).results().count(), "one split per bubble");
        String gs = body(c, "drawGs");
        assertEquals(3, Pattern.compile("drawInBatch\\(").matcher(gs).results().count(), "lines, name and pointer: at most MAX_LINES + 2 draws");
    }

    @Test
    void theGameWiresBothScreensThroughOneInterface() throws IOException {
        String client = code("client/companion/CompanionClient.java");
        assertTrue(client.contains("instanceof TalkHost open") && client.contains("CompanionScreens.create("));
        assertFalse(client.contains("new TalkScreen("), "the choice lives in CompanionScreens");
        assertTrue(code("client/companion/TalkScreen.java").contains("implements TalkHost"));
        assertTrue(code("client/companion/gs/GsTalkScreen.java").contains("implements TalkHost"));
        assertTrue(code("client/companion/CompanionScreens.java").contains("CompanionStyle.choose(CompanionClientConfig.screenChoice(), UiTheme.greatSpirit())"));
        String reg = read("client/ui/move/GuiMoveRegistry.java");
        assertTrue(reg.contains("fixed(\"client/companion/gs/GsTalkScreen.java\""), "a new Screen class needs its GUI move entry");
        String cfg = read("client/companion/CompanionClientConfig.java");
        assertTrue(cfg.contains("companions.screenStyle") || cfg.contains("\"screenStyle\""), "the option exists");
    }

    @Test
    void theCrestMirrorsOnlyTheCompanionsPouchLine() {
        assertTrue(GsNoticeAllow.allowed("nusmp.companion.charmy.use"));
        assertEquals(GsNoticeQueue.Kind.OK, GsNoticeAllow.kind("nusmp.companion.charmy.use"));
        for (String k : new String[]{"nusmp.companion.charmy.gift.love", "nusmp.companion.use", "nusmp.companion.", "nusmp.companion.a.b.use", "nusmp.companion.charmy.reuse.x", "nusmp.companion.charmy.sub", "other.charmy.use"})
            assertFalse(GsNoticeAllow.allowed(k), k);
    }
}
