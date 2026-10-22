package com.newuniverse.nusmp.engraving.codex;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Source scans of the Engravement Codex (rules 9 and 11 of CLAUDE.md): the common package names no client class and no Minecraft singleton, the screen is flat (never the blurred background),
 * builds nothing per frame, filters only when the text or the section changes, and the item sends no packet.
 */
class EngravingCodexSourceScanTest {
    private static final Path COMMON = Path.of("src/main/java/com/newuniverse/nusmp/engraving/codex");
    private static final Path CLIENT = Path.of("src/main/java/com/newuniverse/nusmp/client/engraving");
    private static final Pattern CLIENT_NAMES = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(\\)");

    private static String strip(String s) { return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", ""); }

    private static String read(Path p) throws Exception { return strip(Files.readString(p)); }

    /** The body of the first method whose signature starts with {@code head}. */
    private static String body(String src, String head) {
        int at = src.indexOf(head);
        assertTrue(at >= 0, "no method " + head);
        int open = src.indexOf('{', at), depth = 0, i = open;
        for (; i < src.length(); i++) {
            char c = src.charAt(i);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) break;
        }
        return src.substring(open, i);
    }

    @Test
    void theCommonPackageNamesNoClientClassExceptTheOneGuardedCall() throws Exception {
        List<String> bad = new ArrayList<>();
        try (var files = Files.list(COMMON)) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".java"))::iterator) {
                String src = read(f);
                if (f.getFileName().toString().equals("EngravingCodexItem.java")) {
                    Matcher m = CLIENT_NAMES.matcher(src);
                    int n = 0;
                    while (m.find()) n++;
                    assertEquals(1, n, "the item names the client class exactly once");
                    assertTrue(src.contains("if (level.isClientSide()) com.newuniverse.nusmp.client.engraving.EngravingCodexClient.open();"), "behind the side check");
                    continue;
                }
                if (CLIENT_NAMES.matcher(src).find()) bad.add(f.getFileName().toString());
            }
        }
        assertTrue(bad.isEmpty(), "common codex classes that name a client class: " + bad);
        assertTrue(Files.readString(Path.of("docs/server_safe_allow.txt")).contains("engraving/codex/EngravingCodexItem.java"));
    }

    @Test
    void thePureClassesNameNoMinecraftTypeAtAll() throws Exception {
        for (String name : new String[]{"CodexEntries", "CodexFacts", "CodexFilter", "CodexLayout", "TensuraNotes", "EngravingCodexLook"}) {
            String src = read(COMMON.resolve(name + ".java"));
            assertFalse(src.contains("net.minecraft"), name + " is pure");
            assertFalse(src.contains("net.neoforged"), name + " is pure");
        }
    }

    @Test
    void theItemSendsNoPacketAndKeepsNoStateOnTheServer() throws Exception {
        for (String name : new String[]{"EngravingCodexItem", "EngravingCodexItems", "EngravingCodexCommand", "EngravingCodexSelfTests", "CodexGather"}) {
            String src = read(COMMON.resolve(name + ".java"));
            assertFalse(src.contains("PacketDistributor") || src.contains("CustomPacketPayload") || src.contains("sendToServer"), name + " sends a packet");
            assertFalse(src.contains("UUID") || src.contains("ServerPlayer.class"), name + " keys state by player");
        }
        String item = read(COMMON.resolve("EngravingCodexItem.java"));
        assertTrue(body(item, "use(").contains("isClientSide()"));
        String cmd = read(COMMON.resolve("EngravingCodexCommand.java"));
        assertTrue(cmd.contains("getPlayerOrException()"), "the command acts on the player who ran it and has no target argument");
        assertFalse(cmd.contains("EntityArgument") || cmd.contains("Commands.argument"), "no target argument");
        assertFalse(cmd.contains("requires("), "a plain-information book: no permission level");
    }

    @Test
    void theScreenIsFlatAndDrawsTheBookInRenderBackground() throws Exception {
        String src = read(CLIENT.resolve("EngravingCodexScreen.java"));
        assertTrue(src.contains("extends Screen"));
        String rb = body(src, "void renderBackground(");
        assertFalse(rb.contains("super.renderBackground(") || rb.contains("renderBlurredBackground("), "the backdrop must not call the blurring vanilla background");
        assertTrue(rb.contains("drawFrame(") && rb.contains("half("), "the book is drawn in renderBackground, under the widgets");
        String render = body(src, "void render(");
        assertTrue(render.contains("super.render("));
        assertFalse(render.contains("renderBackground(") || render.contains("renderBlurredBackground("), "render must not call renderBackground by hand");
    }

    @Test
    void theFrameLoopAllocatesAndScansNothing() throws Exception {
        String src = read(CLIENT.resolve("EngravingCodexScreen.java"));
        String[] hot = {"void renderBackground(", "void render(", "private void drawFrame(", "private void drawStrip(", "private void drawFurniture(", "private void half(", "private void drawEntry("};
        String[] banned = {".split(", "new ArrayList", "new HashMap", "String.format", "CodexFilter.select(", "CodexFilter.paginate(", "CodexLayout.lay(", "CodexLayout.wrap(", "Component.translatable",
                "translatableWithFallback(", ".stream()", "getString()", "Minecraft.getInstance", "plainSubstrByWidth(", "font.width(", "substring(", "String.valueOf", "Integer.toString", "book.entries()"};
        for (String h : hot) {
            String b = body(src, h);
            for (String bad : banned) assertFalse(b.contains(bad), h + " must not use " + bad + " (it runs every frame)");
            assertFalse(Pattern.compile("\\btr\\(").matcher(b).find(), h + " must not translate text every frame");
            assertFalse(Pattern.compile("\"\\s*[+]|[+]\\s*\"").matcher(b).find(), h + " must not concatenate strings every frame");
        }
        assertFalse(body(src, "private void drawFurniture(").contains("for ("), "no loop over the entries in the footer");
        // the strings of the footer are built where the filter or the page changes
        assertTrue(body(src, "private void updateLines(").contains("tr(\"nusmp.engraving_codex.count\""));
        assertTrue(body(src, "private void go(").contains("updateLines()"));
        assertTrue(body(src, "private void refilter(").contains("updateLines()"));
    }

    @Test
    void theFilterRunsOnlyWhenTheTextOrTheSectionChanges() throws Exception {
        String src = read(CLIENT.resolve("EngravingCodexScreen.java"));
        Matcher m = Pattern.compile("refilter\\(").matcher(src);
        int calls = 0;
        while (m.find()) calls++;
        assertEquals(4, calls, "the definition and the three callers: the text responder, the row click and init");
        assertTrue(body(src, "private void onQuery(").contains("refilter(true)"));
        assertTrue(body(src, "private void selectRow(").contains("refilter(true)"));
        assertTrue(src.contains("search.setResponder(this::onQuery)"), "the filter is driven by the text change");
        for (String h : new String[]{"void tick(", "void render(", "void renderBackground(", "boolean mouseScrolled("}) assertFalse(body(src, h).contains("refilter("), h + " must not filter");
        assertTrue(body(src, "private void layout(").contains("CodexLayout.lay("), "the layout is done once per init");
        assertEquals(1, Pattern.compile("CodexLayout\\.lay\\(").matcher(src).results().count(), "one place lays out entries");
    }

    @Test
    void theClientHolderCachesTheBookOnceAndDropsItOnReload() throws Exception {
        String src = read(CLIENT.resolve("EngravingCodexClient.java"));
        assertTrue(src.contains("private static volatile CodexEntries.Book cached"), "one cached reference: bounded by the data");
        assertTrue(src.contains("ResourceManagerReloadListener") && src.contains("LoggingOut"), "dropped on a reload (language change) and on leaving the world");
        assertEquals(1, Pattern.compile("CodexGather\\.book\\(").matcher(src).results().count(), "the book is built in one place, on first use");
        assertTrue(body(src, "static CodexEntries.Book book(").contains("b == null"), "built only when there is no cached copy");
    }

    @Test
    void noGameplayOrConfigStateIsAdded() throws Exception {
        try (var files = Files.walk(Path.of("src/main/java/com/newuniverse/nusmp/engraving"))) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".java"))::iterator) {
                String src = read(f);
                assertFalse(src.contains("ModConfigSpec") || src.contains("registerConfig"), f + " adds a config");
                assertFalse(src.contains("ServerTickEvent") || src.contains("PlayerTickEvent") || src.contains("LevelTickEvent"), f + " has a tick handler");
                assertFalse(src.contains("EVENT_BUS"), f + " listens to events");
            }
        }
        assertTrue(new File("docs/items/engraving_codex.md").isFile(), "the doc exists");
    }
}
