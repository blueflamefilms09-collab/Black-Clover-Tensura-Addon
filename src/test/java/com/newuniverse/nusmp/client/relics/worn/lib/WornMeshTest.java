package com.newuniverse.nusmp.client.relics.worn.lib;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** The worn model parser, the animation maths and the catalog, and every model file the generator wrote. */
class WornMeshTest {
    static final Path ROOT = Path.of("src/main/resources/assets/nusmp/relic_worn");

    static String quad(int g, int slot, int flags, float x) {
        StringBuilder b = new StringBuilder("q " + g + " " + slot + " " + flags);
        for (int k = 0; k < 4; k++) b.append(String.format(java.util.Locale.ROOT, " %.3f %.3f %.3f 0.25 0.5 0 -1 0", x + k, 2.0f, 3.0f));
        return b.toString();
    }

    @Test
    void parsesAModelAndSortsItsQuadsByGroup() {
        WornMesh m = WornMesh.parse("t", List.of("# a model", "model t", "anchor rarm", "grp 1 spin spin 0 -5 0 2.5 0 0", "grp 2 b bob 1 2 3 0.5 0.1 0",
                quad(2, 1, 1, 20), quad(0, 0, 0, 0), quad(1, 5, 2, 10), quad(0, 3, 4, 30), "", "future thing 1 2 3"));
        assertEquals(WornAnchor.RIGHT_ARM, m.anchor);
        assertEquals(4, m.quads);
        assertArrayEquals(new int[]{0, 2, 3, 4}, m.groupStart, "group 0 has two quads, groups 1 and 2 one each");
        assertEquals(3, m.groups.length);
        assertNull(m.groups[0]);
        assertEquals(WornMesh.Kind.SPIN, m.groups[1].kind());
        assertEquals(-5f, m.groups[1].py(), 0f);
        assertEquals(2.5f, m.groups[1].a(), 0f);
        assertEquals(0, m.slot[0]);
        assertEquals(3, m.slot[1], "stable order inside a group: the quad written second of group 0");
        assertEquals(5, m.slot[2]);
        assertEquals(1, m.slot[3]);
        assertEquals(1, m.emitQuads);
        assertEquals(1, m.shellQuads);
        assertEquals(16, m.solidVertices());
        assertEquals(4, m.emitVertices());
        assertEquals(0f, m.pos[0], 0f);
        assertEquals(30f, m.pos[12], 0f, "the second quad of group 0 starts at x 30");
        assertEquals(0.25f, m.uv[0], 0f);
        assertEquals(-1f, m.nrm[1], 0f);
    }

    @Test
    void rejectsBrokenRecordsWithTheLineNumber() {
        for (String bad : new String[]{"anchor knee", "grp 2 x bob 0 0 0 0 0 0", "q 0 0 0 1 2 3", quad(3, 0, 0, 0), quad(0, 9, 0, 0), "grp 1 x wiggle 0 0 0 0 0 0", "q 0 0 0 " + "x ".repeat(32)}) {
            try {
                WornMesh.parse("bad", List.of("# c", bad));
                fail("accepted: " + bad);
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage().contains("bad line 2"), expected.getMessage());
            }
        }
        try {
            WornMesh.parse("nan", List.of(quad(0, 0, 0, 0).replace("2.000", "NaN")));
            fail("accepted NaN");
        } catch (IllegalArgumentException expected) { }
    }

    @Test
    void animationsMoveAsDocumented() {
        WornAnim.Xf xf = new WornAnim.Xf();
        WornAnim.eval(new WornMesh.Group("b", WornMesh.Kind.BOB, 0, 0, 0, 2f, 1f, 0f), (float) (Math.PI / 2), 0f, false, xf);
        assertEquals(2f, xf.ty, 1e-4);
        WornAnim.eval(new WornMesh.Group("s", WornMesh.Kind.SPIN, 0, 0, 0, 3f, 0f, 0f), 10f, 0f, false, xf);
        assertEquals(Math.toRadians(30), xf.ry, 1e-5);
        WornAnim.eval(new WornMesh.Group("o", WornMesh.Kind.ORBIT, 0, 0, 0, -2f, 0f, 0f), 5f, 0f, false, xf);
        assertEquals(Math.toRadians(-10), xf.ry, 1e-5);
        WornMesh.Group swing = new WornMesh.Group("w", WornMesh.Kind.SWING, 0, 0, 0, 14f, 0f, 0f);
        WornAnim.eval(swing, 0f, 1f, false, xf);
        assertEquals(Math.toRadians(14), xf.rx, 1e-5);
        WornAnim.eval(swing, 0f, 5f, false, xf);
        assertEquals(Math.toRadians(14), xf.rx, 1e-5, "the stride is clamped to 1");
        WornAnim.eval(swing, 0f, 0f, true, xf);
        assertTrue(xf.rx > 0f, "a crouch kicks the hem out");
        WornAnim.eval(new WornMesh.Group("p", WornMesh.Kind.PULSE, 0, 0, 0, 0.1f, 1f, 0f), (float) (Math.PI / 2), 0f, false, xf);
        assertEquals(1.1f, xf.s, 1e-5);
        WornAnim.eval(new WornMesh.Group("v", WornMesh.Kind.WAVE, 0, 0, 0, 1f, 1f, 1f), 3f, 0f, false, xf);
        assertEquals(0f, xf.rx + xf.ry + xf.rz + xf.tx + xf.ty + xf.tz, 0f);
        assertEquals(1f, xf.s, 0f);
        WornAnim.eval(null, 3f, 0f, false, xf);
        assertEquals(1f, xf.s, 0f);
        WornMesh.Group wave = new WornMesh.Group("v", WornMesh.Kind.WAVE, 0, 0, 0, 2f, 0f, 0f);
        assertEquals(0f, WornAnim.waveOffset(wave, 0f, 4f), 0f, "nothing at the pivot");
        WornMesh.Group w2 = new WornMesh.Group("v", WornMesh.Kind.WAVE, 0, 0, 0, 2f, (float) (Math.PI / 2), 0f);
        assertEquals(0f, WornAnim.waveOffset(w2, 0f, 0f), 0f);
        assertEquals(2f * (float) Math.sin(0.0), WornAnim.waveOffset(w2, 30f, 0f), 1e-5);
        assertEquals(2f, WornAnim.waveOffset(w2, 30f, 1f), 1e-4, "full strength 12 pixels below the pivot, at the crest of the sine");
    }

    @Test
    void paletteSlotsAreRightAndInRange() {
        int[] c = WornPalette.slots(0xFF8000, 0x0000FF, 0x00FF00);
        assertEquals(6, c.length);
        assertEquals(0xFF8000, c[WornMesh.MAIN]);
        assertEquals(0x0000FF, c[WornMesh.TRIM]);
        assertEquals(0x00FF00, c[WornMesh.GLOW]);
        assertEquals(0xFFFFFF, c[WornMesh.NONE]);
        assertTrue((c[WornMesh.DARK] >> 16 & 255) < 0xFF * 0.5, "dark is main darkened");
        assertEquals(0x007F7F, c[WornMesh.ACCENT], "accent is trim and glow mixed");
        assertEquals(0x7F7F7F, WornPalette.scale(0xFFFFFF, 0.5f), "scale halves");
        assertEquals(0xFFFFFF, WornPalette.scale(0xFFFFFF, 7f));
        assertEquals(0, WornPalette.scale(0xFFFFFF, -1f));
        assertEquals(0x00FF00, WornPalette.mix(0x00FF00, 0x0000FF, 0f));
    }

    @Test
    void theCatalogResolvesSignatureThenLookAndParsesPieces() {
        WornCatalog c = WornCatalog.parse(List.of("# x", "look HEAD CROWN crown_spired", "look hands gauntlets bracer@rarm bracer@larm~", "relic duck crown_duck deco@body", "fx duck Ember", "look BAD", "relic"));
        assertEquals("crown_duck", c.piecesFor("duck", "HEAD", "CROWN").get(0).model());
        assertEquals(2, c.piecesFor("duck", "HEAD", "CROWN").size());
        assertEquals(WornAnchor.BODY, c.piecesFor("duck", "HEAD", "CROWN").get(1).anchor());
        assertEquals("crown_spired", c.piecesFor("other", "head", "crown").get(0).model(), "slot and look match in any case");
        List<WornCatalog.Piece> h = c.piecesFor("x", "HANDS", "GAUNTLETS");
        assertEquals(2, h.size());
        assertFalse(h.get(0).mirror());
        assertTrue(h.get(1).mirror());
        assertEquals(WornAnchor.LEFT_ARM, h.get(1).anchor());
        assertEquals("bracer", h.get(1).model());
        assertTrue(c.piecesFor("x", "FEET", "BOOTS").isEmpty());
        assertEquals("ember", c.themeOf("duck"));
        assertNull(c.themeOf("x"));
        assertEquals(2, c.lookCount() + c.relicCount() - 1);
        assertNull(WornAnchor.byKey("knee"));
        assertEquals(WornAnchor.LEFT_LEG, WornAnchor.byKey("lleg"));
    }

    // ================================================================ the generated files
    static List<Path> modelFiles() throws IOException {
        try (Stream<Path> s = Files.list(ROOT)) {
            List<Path> out = new ArrayList<>();
            s.filter(p -> p.getFileName().toString().endsWith(".txt") && !p.getFileName().toString().equals("catalog.txt")).sorted().forEach(out::add);
            return out;
        }
    }

    @Test
    void everyGeneratedModelParsesAndIsSane() throws IOException {
        List<Path> files = modelFiles();
        assertTrue(files.size() >= 60, "the library ships its models: " + files.size());
        for (Path p : files) {
            String name = p.getFileName().toString().replace(".txt", "");
            WornMesh m = WornMesh.parse(name, Files.readAllLines(p));
            assertTrue(m.quads > 20, name + " has geometry");
            assertTrue(m.solidVertices() <= 5000, name + " has " + m.solidVertices() + " vertices");
            for (int i = 0; i < m.quads; i++) {
                for (int k = 0; k < 4; k++) {
                    float nx = m.nrm[i * 12 + k * 3], ny = m.nrm[i * 12 + k * 3 + 1], nz = m.nrm[i * 12 + k * 3 + 2];
                    assertEquals(1.0, Math.sqrt(nx * nx + ny * ny + nz * nz), 0.02, name + " quad " + i + " has a unit normal");
                    float u = m.uv[i * 8 + k * 2], v = m.uv[i * 8 + k * 2 + 1];
                    assertTrue(u >= -0.001f && u <= 1.001f && v >= -0.001f && v <= 1.001f, name + " quad " + i + " uv " + u + "," + v);
                    for (int c = 0; c < 3; c++) assertTrue(Math.abs(m.pos[i * 12 + k * 3 + c]) < 60f, name + " stays near the body");
                }
            }
            for (int g = 1; g < m.groups.length; g++) assertTrue(m.groupStart[g + 1] >= m.groupStart[g]);
            assertEquals(m.quads, m.groupStart[m.groups.length]);
        }
    }

    @Test
    void theCatalogOnlyNamesModelsThatExistAndCoversEverySlotAndLook() throws IOException {
        WornCatalog c = WornCatalog.parse(Files.readAllLines(ROOT.resolve("catalog.txt")));
        Set<String> files = new HashSet<>();
        for (Path p : modelFiles()) files.add(p.getFileName().toString().replace(".txt", ""));
        List<String> lines = Files.readAllLines(ROOT.resolve("catalog.txt"));
        int pieces = 0;
        for (String line : lines) {
            String[] v = line.trim().split("\\s+");
            int from = v[0].equals("look") ? 3 : v[0].equals("relic") ? 2 : -1;
            if (from < 0) continue;
            for (int i = from; i < v.length; i++) {
                WornCatalog.Piece p = WornCatalog.piece(v[i]);
                assertTrue(files.contains(p.model()), line + ": " + p.model() + " has no model file");
                pieces++;
            }
        }
        assertTrue(pieces > 100);
        String[][] looks = {{"HEAD", "CROWN"}, {"HEAD", "CIRCLET"}, {"HEAD", "HAT"}, {"HEAD", "HOOD"}, {"BACK", "CLOAK"}, {"BACK", "CAPE"}, {"BACK", "MANTLE"}, {"NECK", "AMULET"}, {"NECK", "PENDANT"},
                {"NECK", "LOCKET"}, {"BELT", "BELT"}, {"HANDS", "GAUNTLETS"}, {"HANDS", "GLOVES"}, {"HANDS", "CHARM"}, {"CHARM", "CHARM"}, {"CHARM", "AMULET"}, {"CHARM", "POTION"}, {"CHARM", "TOME"},
                {"CHEST", "PLATE"}, {"CHEST", "COAT"}, {"CHEST", "SUIT"}, {"CHEST", "LEATHER"}, {"CHEST", "ROBE"}, {"FEET", "BOOTS"}, {"FEET", "GREAVES"}, {"FEET", "SANDALS"}};
        for (String[] l : looks) assertFalse(c.piecesFor("none", l[0], l[1]).isEmpty(), l[0] + " " + l[1] + " has a default model");
        for (String id : new String[]{"tiny_duck_crown", "circlet_of_the_phoenix", "gold_circlet", "silver_circlet", "steel_circlet", "unicorn_crown", "dark_unicorn_crown", "crown_of_the_conjurer",
                "cloak_of_levitation", "cape_of_bees", "godking_crown"})
            assertFalse(c.piecesFor(id, "X", "Y").isEmpty(), id + " has a signature entry");
        assertEquals("crown_duck", c.piecesFor("tiny_duck_crown", "HEAD", "CROWN").get(0).model());
        assertEquals("circlet_dragon", c.piecesFor("steel_circlet", "HEAD", "CIRCLET").get(0).model());
    }

    @Test
    void everyAnimationGroupHasQuadsAndFinitePivots() throws IOException {
        for (Path p : modelFiles()) {
            WornMesh m = WornMesh.parse(p.getFileName().toString(), Files.readAllLines(p));
            for (int g = 1; g < m.groups.length; g++) {
                assertTrue(m.groupStart[g + 1] > m.groupStart[g], p.getFileName() + " group " + m.groups[g].name() + " is empty");
                WornMesh.Group gr = m.groups[g];
                assertTrue(Float.isFinite(gr.px() + gr.py() + gr.pz() + gr.a() + gr.b() + gr.c()));
            }
        }
    }
}
