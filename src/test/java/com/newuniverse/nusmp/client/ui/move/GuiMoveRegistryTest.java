package com.newuniverse.nusmp.client.ui.move;

import com.newuniverse.nusmp.client.ui.move.GuiMoveRegistry.Element;
import com.newuniverse.nusmp.client.ui.scale.GuiScaleRegistry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: "every custom GUI of the mod is movable, or is deliberately fixed with a reason" is a promise that rots the moment someone adds a HUD and forgets to register it. So this
 * data and source test reads the real files: every element has an id, a name and a language key; every custom GUI class of the mod (a screen, or a file that registers a HUD
 * layer) is either claimed by a registered element or listed as fixed with its reason; every element is really wired through the framework (and nothing applies the GUI Size
 * on its own next to it, which would double the size); the language fragment has every text the edit mode shows.
 */
class GuiMoveRegistryTest {
    private static final Path MAIN = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final Pattern CUSTOM_GUI = Pattern.compile("registerAboveAll\\(|RenderGuiLayerEvent\\.Pre|\\bextends\\s+(Screen|AbstractContainerScreen)\\b");

    private static String code(Path f) throws IOException {
        return Files.readString(f).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
    }

    @Test
    void everyElementHasAnIdANameAndAPlace() {
        Set<String> ids = new HashSet<>();
        assertTrue(GuiMoveRegistry.all().size() >= 20, "the registry is nearly empty");
        for (Element e : GuiMoveRegistry.all()) {
            assertTrue(e.id.matches("[a-z][a-z0-9_]*"), "bad id " + e.id);
            assertTrue(ids.add(e.id), "duplicate id " + e.id);
            assertFalse(e.english.isBlank(), e.id + " has no name");
            assertEquals("gui_move.element." + e.id, e.nameKey());
            assertTrue(e.defW > 0 && e.defH > 0, e.id + " has no default size");
            assertTrue(e.margin >= 0 && e.margin < 200, e.id + " margin");
            assertNotNull(e.defPlace);
            assertFalse(e.sources.isEmpty(), e.id + " names no source file");
            assertEquals(e.kind == GuiMoveRegistry.Kind.PANEL, e.how == GuiMoveRegistry.How.SCREEN, e.id + ": panels are applied by the screen hooks, HUD elements are not");
            assertEquals(null, e.state, e.id + " must start untouched");
        }
    }

    @Test
    void everyPanelIsARealScreenOfTheModWithAGuiSizeEntry() throws IOException {
        for (Element e : GuiMoveRegistry.of(GuiMoveRegistry.Kind.PANEL)) {
            GuiScaleRegistry.Entry s = GuiScaleRegistry.byId(e.id);
            assertNotNull(s, e.id + " has no GUI Size entry (its screen class comes from there)");
            assertEquals(s.className(), e.className);
            assertEquals(e, GuiMoveRegistry.byClass(e.className));
            Path src = Path.of("src/main/java").resolve(e.className.replace('.', '/') + ".java");
            assertTrue(Files.exists(src), e.className + " has no source");
            String text = Files.readString(src);
            assertTrue(text.contains("extends Screen") || text.contains("extends AbstractContainerScreen"), e.className + " is not a screen");
            assertTrue(s.designW() != 0, e.id + " fills the whole window, so there is nothing to move");
        }
    }

    @Test
    void everyGuiSizeEntryIsMovableOrFixedWithAReason() {
        Set<String> fixedIds = new HashSet<>();
        for (GuiMoveRegistry.Fixed f : GuiMoveRegistry.fixed()) {
            assertFalse(f.reason().isBlank(), f.source() + " is fixed without a reason");
            if (f.source().startsWith("gui_scale:")) fixedIds.add(f.source().substring("gui_scale:".length()));
        }
        for (GuiScaleRegistry.Entry s : GuiScaleRegistry.all()) {
            assertTrue(GuiMoveRegistry.byId(s.id()) != null || fixedIds.contains(s.id()), "GUI Size entry " + s.id() + " is neither movable nor fixed with a reason");
            assertFalse(GuiMoveRegistry.byId(s.id()) != null && fixedIds.contains(s.id()), s.id() + " is both movable and fixed");
        }
        for (String id : fixedIds) assertNotNull(GuiScaleRegistry.byId(id), "fixed id " + id + " is not a GUI Size entry");
    }

    @Test
    void everyCustomGuiClassOfTheModIsClaimedOrFixed() throws IOException {
        Set<String> claimed = new HashSet<>();
        for (Element e : GuiMoveRegistry.all()) claimed.addAll(e.sources);
        for (GuiMoveRegistry.Fixed f : GuiMoveRegistry.fixed()) if (!f.source().startsWith("gui_scale:")) claimed.add(f.source());
        for (String c : claimed) assertTrue(Files.exists(MAIN.resolve(c)), "claimed source does not exist: " + c);
        List<String> forgotten = new ArrayList<>();
        int found = 0;
        try (Stream<Path> files = Files.walk(MAIN)) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".java"))::iterator) {
                if (!CUSTOM_GUI.matcher(code(f)).find()) continue;
                found++;
                String rel = MAIN.relativize(f).toString().replace('\\', '/');
                if (!claimed.contains(rel)) forgotten.add(rel);
            }
        }
        assertTrue(found >= 25, "the scan found only " + found + " custom GUI files; the pattern is broken");
        assertTrue(forgotten.isEmpty(), "custom GUI files that are neither movable (GuiMoveRegistry element) nor fixed with a reason: " + forgotten);
    }

    @Test
    void everyElementIsReallyWiredThroughTheFramework() throws IOException {
        for (Element e : GuiMoveRegistry.all()) {
            StringBuilder all = new StringBuilder();
            for (String s : e.sources) all.append(code(MAIN.resolve(s))).append('\n');
            String text = all.toString();
            switch (e.how) {
                case BEGIN -> assertTrue(text.contains("GuiMove.begin(") || text.contains("GuiMove.apply("), e.id + " never calls GuiMove.begin/apply");
                case LAYER -> {
                    assertTrue(text.contains("GuiMove.report(\"" + e.id + "\""), e.id + " never reports its rectangle");
                    assertNotNull(GuiScaleRegistry.byId(e.id), e.id + " is a layer element but has no GUI Size layer entry for the hook to find it by");
                    assertEquals(GuiScaleRegistry.Mode.LAYER, GuiScaleRegistry.byId(e.id).mode());
                }
                case SELF -> assertTrue(text.contains("GuiMove.reportDrawn(") && text.contains("GUI_MOVE_ID"), e.id + " does not report where it draws");
                case SCREEN -> assertTrue(Files.readString(MAIN.resolve("client/ui/GuiScale.java")).contains("GuiMove.screenXf("), "the screen hooks do not ask GuiMove");
            }
            if (e.how == GuiMoveRegistry.How.BEGIN || e.how == GuiMoveRegistry.How.LAYER) assertTrue(text.contains("GuiMove.hidden(") || e.how == GuiMoveRegistry.How.LAYER, e.id + " never asks whether it is hidden");
        }
    }

    @Test
    void nothingAppliesTheGuiSizeNextToTheFrameworkForTheSameElement() throws IOException {
        List<String> bad = new ArrayList<>();
        try (Stream<Path> files = Files.walk(MAIN)) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".java"))::iterator) {
                String rel = MAIN.relativize(f).toString().replace('\\', '/');
                if (rel.equals("client/ui/GuiScale.java") || rel.equals("client/ui/move/GuiMove.java") || rel.equals("client/ui/GuiSizeScreen.java")) continue;
                String c = code(f);
                if (c.contains("GuiScale.begin(") || c.contains("GuiScale.hud(")) {
                    // the one allowed user is the System window, which sizes its own layout (and reports the GUI Size part to GuiMove)
                    if (!rel.equals("client/mega/system/SystemOverlay.java")) bad.add(rel);
                }
            }
        }
        assertTrue(bad.isEmpty(), "these files apply the GUI Size themselves and would double it next to GuiMove.begin: " + bad);
    }

    @Test
    void theLanguageFragmentHasEveryTextOfEditMode() throws IOException {
        Path p = Path.of("tools/lang/gui_move.json");
        assertTrue(Files.exists(p), "tools/lang/gui_move.json is missing");
        String json = Files.readString(p).trim();
        assertTrue(json.startsWith("{") && json.endsWith("}"));
        assertEquals(-1, json.indexOf('{', 1), "the fragment must be flat");
        Set<String> keys = new HashSet<>();
        Matcher m = Pattern.compile("\"([^\"]+)\"\\s*:").matcher(json);
        while (m.find()) keys.add(m.group(1));
        assertTrue(keys.contains("key.nusmp.gui_move"), "the key's name is not translated");
        for (Element e : GuiMoveRegistry.all()) assertTrue(keys.contains(e.nameKey()), "no text for " + e.nameKey());
        for (String file : new String[]{"client/ui/move/GuiMoveScreen.java", "client/ui/move/GuiMove.java", "client/ui/GuiSizeScreen.java"}) {
            Matcher u = Pattern.compile("(?:tr|translatableWithFallback)\\(\\s*\"(gui_move\\.[a-z_.]+)\"").matcher(code(MAIN.resolve(file)));
            while (u.find()) assertTrue(keys.contains(u.group(1)), file + " uses " + u.group(1) + " which the fragment lacks");
        }
        assertTrue(Files.readString(MAIN.resolve("client/ui/move/GuiMove.java")).contains("\"key.nusmp.gui_move\""));
    }

    @Test
    void theLayoutFileHasAUniqueNameAndOnlyHoldsLooks() throws IOException {
        String move = Files.readString(MAIN.resolve("client/ui/move/GuiMove.java"));
        assertTrue(move.contains("ModConfig.Type.CLIENT, GuiMoveOptions.SPEC, \"nusmp-gui-layout-client.toml\""), "the layout is a CLIENT config with its own file name");
        assertFalse(move.contains("ModConfig.Type.SERVER") || move.contains("ModConfig.Type.COMMON"), "where a GUI sits is never a server setting");
    }
}
