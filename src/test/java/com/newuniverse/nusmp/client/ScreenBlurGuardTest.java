package com.newuniverse.nusmp.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: since 1.21 {@code Screen.render} calls {@code renderBackground} itself and that call BLURS everything drawn before it. A screen that calls
 * renderBackground by hand inside its own {@code render} (or draws its panel before {@code super.render}) blurs its own panel: the blurry menus of 1.15.0
 * (questionnaire) and 1.51.0 (Avatar menu). This pure source scan fails the build for any class under {@code client/} whose {@code render(...)} method body calls
 * {@code renderBackground(} or {@code renderBlurredBackground(}: panels belong in a {@code renderBackground} override (after super) or in {@code renderBg}.
 */
class ScreenBlurGuardTest {
    private static final Path CLIENT = Path.of("src/main/java/com/newuniverse/nusmp/client");

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
}
