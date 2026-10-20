package com.newuniverse.nusmp.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: since 1.21 {@code Screen.render} calls {@code renderBackground} itself and that call BLURS everything drawn before it. A screen that calls
 * renderBackground by hand inside its own {@code render} (or draws its panel before {@code super.render}) blurs its own panel: the blurry menus of 1.15.0
 * (questionnaire), 1.51.0 (Avatar menu) and 1.105.0 (the Dungeon Map, which drew the whole map and then called {@code super.render} last). This pure source scan fails the build for any class under {@code client/} whose {@code render(...)} method body calls
 * {@code renderBackground(} or {@code renderBlurredBackground(}: panels belong in a {@code renderBackground} override (after super) or in {@code renderBg}.
 *
 * <p>1.105.0 adds a scan of EVERY screen class of the mod (anywhere under {@code src/main/java}): a screen whose {@code render} draws anything before {@code super.render} is only allowed when it
 * overrides {@code renderBackground} without calling the vanilla one (then there is no blur to put over what it drew), and the Dungeon Map is pinned down by name.
 */
class ScreenBlurGuardTest {
    private static final Path MAIN = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final Path CLIENT = MAIN.resolve("client");
    private static final Pattern SCREEN_CLASS = Pattern.compile("\\bclass\\s+\\w+(<[^{]*?>)?\\s+extends\\s+(Screen|AbstractContainerScreen<[^{]*?>|\\w*Screen)\\b");

    @Test
    void noScreenRendersItsBackgroundByHand() throws IOException {
        List<String> bad = new ArrayList<>();
        try (Stream<Path> files = Files.walk(CLIENT)) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".java"))::iterator) {
                String src = Files.readString(f);
                if (!src.contains("extends Screen") && !src.contains("Screen<") && !src.contains("extends AbstractContainerScreen") && !src.contains("Screen {")) continue;
                int at = 0;
                while ((at = src.indexOf("void render(", at)) >= 0) {
                    int open = src.indexOf('{', at);
                    if (open < 0) break;
                    int depth = 0, i = open;
                    for (; i < src.length(); i++) {
                        char c = src.charAt(i);
                        if (c == '{') depth++;
                        else if (c == '}' && --depth == 0) break;
                    }
                    String body = src.substring(open, Math.min(i, src.length()));
                    if (body.contains("renderBackground(") || body.contains("renderBlurredBackground(")) bad.add(CLIENT.relativize(f).toString());
                    at = i;
                }
            }
        }
        assertTrue(bad.isEmpty(), "these screens call renderBackground inside render() and blur their own panel (draw it in a renderBackground override after super): " + bad);
    }

    /** The body of the first method whose signature starts with {@code head} (text from the opening brace to the matching one), or null. */
    static String body(String src, String head) {
        int at = src.indexOf(head);
        if (at < 0) return null;
        int open = src.indexOf('{', at);
        if (open < 0) return null;
        int depth = 0, i = open;
        for (; i < src.length(); i++) {
            char c = src.charAt(i);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) break;
        }
        return src.substring(open, Math.min(i, src.length()));
    }

    private static String stripComments(String s) {
        return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
    }

    /** Null when the screen is fine, else why not. */
    static String blurProblem(String srcRaw) {
        String src = stripComments(srcRaw);
        Matcher sig = Pattern.compile("void\\s+render\\(\\s*GuiGraphics\\s+(\\w+)\\s*,").matcher(src);
        if (!sig.find()) return null;                                   // no render override: vanilla draws background, widgets (and renderBg) in the right order
        String g = sig.group(1);
        String render = body(src, sig.group());
        if (render == null) return null;
        int sup = render.indexOf("super.render(");
        if (sup < 0) return null;                                       // does not call vanilla's render: no vanilla blur at all
        String before = render.substring(0, sup);
        Matcher uses = Pattern.compile("\\b" + g + "\\s*[.,)]").matcher(before);
        if (!uses.find()) return null;                                  // nothing is drawn before the blur
        // something is drawn before super.render: only safe when renderBackground is overridden and does not call the blurring vanilla one
        String rb = body(src, "void renderBackground(");
        if (rb != null && !rb.contains("super.renderBackground(") && !rb.contains("renderBlurredBackground(")) return null;
        return "render() draws (" + before.substring(Math.max(0, uses.start() - 20), Math.min(before.length(), uses.end() + 30)).replace('\n', ' ').trim() + ") before super.render, whose renderBackground blurs it";
    }

    @Test
    void noScreenOfTheModDrawsBeforeTheBlur() throws IOException {
        List<String> bad = new ArrayList<>();
        int screens = 0;
        try (Stream<Path> files = Files.walk(MAIN)) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".java"))::iterator) {
                String src = Files.readString(f);
                if (!SCREEN_CLASS.matcher(src).find()) continue;
                screens++;
                String why = blurProblem(src);
                if (why != null) bad.add(MAIN.relativize(f) + ": " + why);
            }
        }
        assertTrue(screens >= 15, "the scan found only " + screens + " screens; the pattern is broken");
        assertTrue(bad.isEmpty(), "these screens draw before vanilla's blur: " + bad);
    }

    @Test
    void theScanCatchesTheDungeonMapMistake() {
        String broken = "class A extends Screen { public void render(GuiGraphics g, int mx, int my, float pt) {\n g.fill(0,0,10,10,0xFF000000);\n drawStuff(g);\n super.render(g, mx, my, pt);\n } }";
        assertTrue(blurProblem(broken) != null, "the 1.104.0 shape (draw the map, then super.render last) must be reported");
        String fixedOrder = "class A extends Screen { public void render(GuiGraphics g, int mx, int my, float pt) {\n super.render(g, mx, my, pt);\n g.fill(0,0,10,10,0xFF000000);\n } }";
        assertEquals(null, blurProblem(fixedOrder));
        String flat = "class A extends Screen { public void renderBackground(GuiGraphics g, int a, int b, float c) { g.fill(0,0,1,1,0); }\n public void render(GuiGraphics g, int mx, int my, float pt) {\n g.fill(0,0,10,10,0xFF000000);\n super.render(g, mx, my, pt);\n } }";
        assertEquals(null, blurProblem(flat), "a screen that replaces renderBackground with a flat fill has no blur to draw under");
        String withSuper = "class A extends Screen { public void renderBackground(GuiGraphics g, int a, int b, float c) { super.renderBackground(g, a, b, c); }\n public void render(GuiGraphics g, int mx, int my, float pt) {\n g.fill(0,0,10,10,0xFF000000);\n super.render(g, mx, my, pt);\n } }";
        assertTrue(blurProblem(withSuper) != null, "renderBackground that calls super still blurs");
        String noSuper = "class A extends Screen { public void render(GuiGraphics g, int mx, int my, float pt) {\n g.fill(0,0,10,10,0xFF000000);\n } }";
        assertEquals(null, blurProblem(noSuper), "a render that never calls super.render has no vanilla blur");
    }

    @Test
    void theDungeonMapScreenIsFlatAndDrawsAfterSuper() throws IOException {
        Path f = CLIENT.resolve("elite/hub/DungeonMapScreen.java");
        String src = stripComments(Files.readString(f));
        assertTrue(SCREEN_CLASS.matcher(src).find(), "the Dungeon Map must still be a Screen");
        String rb = body(src, "void renderBackground(");
        assertTrue(rb != null, "the Dungeon Map must override renderBackground (flat backdrop, no blur)");
        assertTrue(!rb.contains("super.renderBackground(") && !rb.contains("renderBlurredBackground("), "the backdrop must not call the blurring vanilla background");
        String render = body(src, "void render(");
        assertTrue(render != null && render.contains("super.render("), "render must call super.render (the Close button)");
        assertTrue(render.indexOf("drawMap(") > render.indexOf("super.render(") && render.indexOf("drawBars(") > render.indexOf("super.render("), "the map and the bars are drawn after super.render");
        assertEquals(null, blurProblem(Files.readString(f)));
    }
}
