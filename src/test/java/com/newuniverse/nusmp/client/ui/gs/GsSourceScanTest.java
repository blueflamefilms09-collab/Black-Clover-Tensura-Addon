package com.newuniverse.nusmp.client.ui.gs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Great Spirit look must stay client-only, pure where it is meant to be pure, allocation-free in its draw calls and complete (every texture it
 * names exists). The test reads the real files; if a pattern comes back, it fails with the file and the method.
 */
class GsSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final Path RES = Path.of("src/main/resources/assets/nusmp");

    private static String read(String rel) throws IOException { return Files.readString(SRC.resolve(rel)); }

    /** The body of the first method called 'name' (brace matched; good enough for these files). */
    private static String body(String src, String name) {
        int i = src.indexOf(" " + name + "(");
        assertTrue(i >= 0, "no method " + name);
        int open = src.indexOf('{', src.indexOf(')', i));
        int depth = 0;
        for (int k = open; k < src.length(); k++) {
            char c = src.charAt(k);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return src.substring(open, k + 1);
        }
        throw new AssertionError("unbalanced " + name);
    }

    @Test
    void thePureClassesImportNoMinecraftType() throws IOException {
        for (String f : new String[]{"GsSkin", "GsCrestPos", "GsNoticeQueue", "GsNoticeAllow", "GsCompile", "GsClover", "GsRipple"}) {
            String s = read("client/ui/gs/" + f + ".java");
            assertFalse(s.contains("net.minecraft") || s.contains("com.mojang") || s.contains("net.neoforged"), f + " must stay pure");
        }
        String d = read("multiverse/DevilUser.java");
        assertFalse(d.contains("import net.minecraft") || d.contains("nusmp.client"), "DevilUser is server-side logic");
    }

    @Test
    void noMinecraftClientHandleLeavesTheClientPackages() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String f : new String[]{"multiverse/DevilUser.java", "multiverse/MultiverseSync.java"})
            if (read(f).contains("Minecraft.getInstance") || read(f).contains("com.newuniverse.nusmp.client.ui.gs")) bad.add(f);
        assertTrue(bad.isEmpty(), "common code must not touch the Great Spirit client classes: " + bad);
    }

    @Test
    void theDrawCallsBuildNoLocationNoStringAndNoObject() throws IOException {
        String draw = read("client/ui/gs/GsDraw.java");
        assertFalse(draw.contains("fromNamespaceAndPath") || draw.contains("String.format") || draw.contains(" new "), "GsDraw draws from prebuilt textures and allocates nothing");
        String notices = read("client/ui/gs/GsNotices.java");
        for (String m : new String[]{"render", "draw"}) {
            String b = body(notices, m);
            assertFalse(b.contains(" new ") || b.contains("Component.") || b.contains("String.format"), "GsNotices." + m + " allocates per frame");
        }
        String tex = read("client/ui/gs/GsTex.java");
        assertTrue(tex.contains("static final"), "locations are built once");
        String ripple = body(read("client/ui/theme/ThemedButton.java"), "renderWidget");
        assertFalse(ripple.contains(" new "), "the button's draw allocates nothing");
    }

    @Test
    void theBudgetsAreTheOnesTheDocStates() {
        assertEquals(3, GsNoticeQueue.VISIBLE);
        assertEquals(16, GsNoticeQueue.QUEUED);
        assertEquals(4, GsNoticeQueue.PER_SECOND);
        assertEquals(900, GsCompile.GLYPH_BUDGET);
        assertEquals(3, GsRipple.MAX_RINGS);
        assertEquals(25, GsClover.MAX_ANIMATED);
    }

    @Test
    void theOptionsAreClientOnlyAndTheConfigFileKeepsItsLiteralName() throws IOException {
        String o = read("client/ui/theme/UiThemeOptions.java");
        for (String k : new String[]{"\"greatSpirit\"", "\"skin\"", "\"notices\"", "\"noticePos\"", "\"quality\"", "\"cloverIcons\"", "great_spirit"})
            assertTrue(o.contains(k), "missing option " + k);
        assertTrue(read("NUSMP.java").contains("ModConfig.Type.CLIENT, com.newuniverse.nusmp.client.ui.theme.UiThemeOptions.SPEC, \"nusmp-ui-client.toml\""));
        assertTrue(o.contains("\"AUTO\""), "the skin defaults to AUTO");
        assertTrue(o.contains("define(\"theme\", \"anime\""), "the theme default stays anime until the owner flips it");
    }

    @Test
    void theStatusCarriesTheDevilFlagAndTheCoverTierAndNoNewPacketExists() throws IOException {
        String s = read("multiverse/MultiverseSync.java");
        assertTrue(s.contains("t.putBoolean(\"Devil\"") && s.contains("DevilUser.is("));
        assertTrue(s.contains("g.putInt(\"Tier\""));
        assertEquals(1, s.split("implements CustomPacketPayload", -1).length - 1, "the status payload is still the only payload of MultiverseSync");
        for (String f : new String[]{"GsNotices", "GsCrestScreen", "GsDraw", "GsUser"})
            assertFalse(read("client/ui/gs/" + f + ".java").contains("PacketDistributor"), f + " must send nothing");
    }

    @Test
    void everyTextureTheLookNamesIsShipped() {
        String[] common = {"corner", "crest", "ripple", "clover3_frame", "clover4_frame", "clover4_glow", "clover5_frame", "clover5_glow", "clover5_drips"};
        for (String skin : new String[]{"b", "c"}) {
            for (String n : common) {
                assertTrue(Files.exists(RES.resolve("textures/gui/gs/" + skin + "/" + n + ".png")), skin + "/" + n);
                assertTrue(Files.exists(RES.resolve("textures/gui/gs/" + skin + "/" + n + ".png.mcmeta")), skin + "/" + n + " mcmeta");
            }
        }
        assertTrue(Files.exists(RES.resolve("textures/gui/gs/b/page.png")));
    }
}
