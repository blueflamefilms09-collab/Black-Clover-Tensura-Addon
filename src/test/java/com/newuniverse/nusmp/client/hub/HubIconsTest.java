package com.newuniverse.nusmp.client.hub;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: Hub v2 draws custom PNG icons; a missing or wrong-sized file would draw the purple checker in the game. Every node, Codex page and category needs a plain and a glow icon, 32 x 32, with the filter mcmeta. */
class HubIconsTest {
    private static final Path DIR = Path.of("src/main/resources/assets/anime_multiverse/textures/gui/hub");

    private static List<String> names() {
        List<String> n = new ArrayList<>();
        for (HubNodes.Tab t : HubNodes.Tab.values()) n.add("node_" + t.key);
        for (HubCodexNav.Item i : HubCodexNav.NAV) n.add("codex_" + i.id());
        for (HubCodexNav.Cat c : HubCodexNav.Cat.values()) n.add("cat_" + c.key);
        return n;
    }

    @Test
    void everyNodePageAndCategoryHasAPlainAndAGlowIconOf32By32() throws IOException {
        for (String n : names()) {
            for (String suffix : new String[] {"", "_glow"}) {
                Path p = DIR.resolve(n + suffix + ".png");
                assertTrue(Files.isRegularFile(p), "missing icon " + p);
                byte[] b = Files.readAllBytes(p);
                assertTrue(b.length > 24 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G', n + suffix + " is not a PNG");
                ByteBuffer bb = ByteBuffer.wrap(b);
                assertEquals(HubIcons.SRC, bb.getInt(16), n + suffix + " width");
                assertEquals(HubIcons.SRC, bb.getInt(20), n + suffix + " height");
                assertTrue(b.length < 20_000, n + suffix + " is oddly large");
                assertTrue(Files.isRegularFile(DIR.resolve(n + suffix + ".png.mcmeta")), "missing mcmeta of " + n + suffix);
            }
        }
    }

    @Test
    void mcmetaAsksForFilteringAndIconNamesMatchTheClass() throws IOException {
        for (String n : names()) assertTrue(Files.readString(DIR.resolve(n + ".png.mcmeta")).contains("\"blur\""), n + " mcmeta");
        for (HubNodes.Tab t : HubNodes.Tab.values()) assertEquals("node_" + t.key, HubIcons.node(t));
    }

    @Test
    void thereAreNoStrayFilesInTheIconFolder() throws IOException {
        List<String> ok = new ArrayList<>();
        for (String n : names()) for (String s : new String[] {".png", "_glow.png", ".png.mcmeta", "_glow.png.mcmeta"}) ok.add(n + s);
        try (var s = Files.list(DIR)) {
            for (Path p : s.toList()) assertTrue(ok.contains(p.getFileName().toString()), "unused file " + p.getFileName() + " (a new page needs a nav id)");
        }
    }
}
