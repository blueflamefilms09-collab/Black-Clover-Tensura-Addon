package com.newuniverse.nusmp.vfx.deck;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why (owner order of release 1.110.0): the flat picture of a drawn card floating in the world is gone for good. This scans the sources: no client code may draw a camera-facing or flat
 * world-space quad textured with card art. The old SPELL_CARD effect (a billboard of the grimoire icon in a card frame) and its frame texture were removed; the enum constant stays (saved
 * ordinals) and nothing draws it, no act effect names it, and the new summon draws a box with thickness, never a billboard.
 */
class NoFlatCardPictureTest {
    private static final Path SRC = Path.of("src/main/java");
    private static final Path RES = Path.of("src/main/resources");

    private static List<Path> javaFiles() throws IOException {
        try (Stream<Path> s = Files.walk(SRC)) {
            return s.filter(p -> p.toString().endsWith(".java")).toList();
        }
    }

    private static boolean client(Path p) {
        String s = p.toString().replace('\\', '/');
        return s.contains("/client/") || s.contains("/vfx/");
    }

    @Test
    void noSourceNamesTheRemovedCardFrameOrDrawsTheSpellCard() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Path p : javaFiles()) {
            String s = Files.readString(p);
            if (s.contains("CARD_FRAME") || s.contains("\"card_frame\"")) bad.add(p + " names the card frame texture");
            if (s.contains("VfxShape.SPELL_CARD") || s.contains("case SPELL_CARD")) bad.add(p + " draws or lists the removed SPELL_CARD");
        }
        assertTrue(bad.isEmpty(), bad.toString());
        assertFalse(Files.exists(RES.resolve("assets/nusmp/textures/particle/card_frame.png")), "the card frame texture came back");
    }

    @Test
    void noClientMethodDrawsABillboardOrPlaneOfACard() throws IOException {
        Pattern method = Pattern.compile("(?m)^\\s*(?:public |private |protected |static |final )*[\\w<>\\[\\], ?]+\\s+(\\w*(?:[cC]ard|[dD]eck)\\w*)\\s*\\([^)]*\\)\\s*(?:throws [\\w., ]+)?\\{");
        List<String> bad = new ArrayList<>();
        for (Path p : javaFiles()) {
            if (!client(p)) continue;
            String s = Files.readString(p);
            Matcher m = method.matcher(s);
            while (m.find()) {
                int depth = 1, i = m.end();
                while (i < s.length() && depth > 0) {
                    char c = s.charAt(i++);
                    if (c == '{') depth++;
                    else if (c == '}') depth--;
                }
                String body = s.substring(m.end(), i);
                if (body.contains(".billboard(") || body.contains(".plane(")) bad.add(p + " " + m.group(1) + " draws a flat picture");
            }
        }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void theSummonLayerDrawsABoxAndNeverABillboard() throws IOException {
        String s = Files.readString(SRC.resolve("com/newuniverse/nusmp/vfx/client/layer/DeckSummonLayer.java"));
        assertFalse(s.contains(".billboard("), "the summon must not draw a camera-facing picture");
        assertFalse(s.contains(".plane("), "the summon must not draw a flat plane picture");
        assertTrue(s.contains("slab("), "the card is a thin box");
        assertTrue(s.contains("CARD_T"), "with a thickness");
    }

    @Test
    void noActEffectOrUpgradeFileNamesTheSpellCard() throws IOException {
        List<String> bad = new ArrayList<>();
        try (Stream<Path> s = Files.walk(RES.resolve("data/nusmp/visual_upgrade"))) {
            for (Path p : s.filter(Files::isRegularFile).toList()) if (Files.readString(p).contains("SPELL_CARD")) bad.add(p.toString());
        }
        try (Stream<Path> s = Files.walk(RES.resolve("assets/nusmp/visual_upgrade"))) {
            for (Path p : s.filter(q -> q.toString().endsWith(".json") || q.toString().endsWith(".txt")).toList()) if (Files.readString(p).contains("SPELL_CARD")) bad.add(p.toString());
        }
        assertTrue(bad.isEmpty(), "SPELL_CARD is back in " + bad);
    }
}
