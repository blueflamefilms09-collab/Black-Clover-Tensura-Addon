package com.newuniverse.nusmp.client.relics.worn;

import com.newuniverse.nusmp.client.relics.worn.lib.WornMesh;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The files the worn layer draws, checked the way the game would meet them: every model parses, sits on a body part, has quads inside a sane box round the body, and its glowing
 * parts are wound front-facing (a back-face-culling render type draws nothing else). Reads the sources of the jar; no game.
 */
class WornAssetsTest {
    static final Path WORN = Path.of("src/main/resources/assets/nusmp/relic_worn");

    static List<Path> models() throws IOException {
        try (Stream<Path> s = Files.list(WORN)) {
            return s.filter(p -> p.toString().endsWith(".txt") && !p.getFileName().toString().equals("catalog.txt")).sorted().toList();
        }
    }

    @Test
    void theTexturesAndTheCatalogAreInTheJar() {
        assertTrue(WornAssets.exists(WornAssets.ATLAS), "atlas.png");
        assertTrue(WornAssets.exists(WornAssets.SHIMMER), "shimmer.png");
        assertTrue(WornAssets.exists(WornAssets.BASE + "catalog.txt"), "catalog.txt");
        assertNull(WornAssets.catalogProblem());
        assertTrue(WornAssets.catalog().lookCount() > 20, "looks");
        assertTrue(WornAssets.catalog().relicCount() > 50, "signature relics");
    }

    @Test
    void everyModelFileParsesAndHasQuadsOnABodyPart() throws IOException {
        List<String> bad = new ArrayList<>();
        int n = 0;
        for (Path p : models()) {
            String name = p.getFileName().toString().replace(".txt", "");
            n++;
            var m = WornAssets.mesh(name);
            if (m.isEmpty()) { bad.add(name + ": " + WornAssets.problemOf(name)); continue; }
            WornMesh w = m.get();
            if (w.quads == 0 || w.anchor == null) bad.add(name + ": no quads or no anchor");
        }
        assertTrue(n > 300, "the library has hundreds of models, found " + n);
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void everyModelStaysInsideAReasonableBoxRoundTheBody() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Path p : models()) {
            String name = p.getFileName().toString().replace(".txt", "");
            WornMesh w = WornAssets.mesh(name).orElse(null);
            if (w == null) continue;
            float lim = 40f;                                                     // pixels from the pivot of the anchor part: a model farther than this is not on the body
            for (int i = 0; i < w.quads * 4; i++) {
                float x = w.pos[3 * i], y = w.pos[3 * i + 1], z = w.pos[3 * i + 2];
                if (Math.abs(x) > lim || Math.abs(y) > lim || Math.abs(z) > lim || Float.isNaN(x + y + z)) { bad.add(name + " vertex " + i + " at " + x + "," + y + "," + z); break; }
            }
        }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    /** The normal of the quad's winding (Newell's sum over its four edges, so a quad with two equal corners still has one) against the normals stored with it: +1 front, -1 reversed, 0 flat. */
    static float facing(WornMesh w, int q) {
        int o = q * 12;
        double cx = 0, cy = 0, cz = 0;
        for (int c = 0; c < 4; c++) {
            int n = (c + 1) % 4;
            double x0 = w.pos[o + c * 3], y0 = w.pos[o + c * 3 + 1], z0 = w.pos[o + c * 3 + 2];
            double x1 = w.pos[o + n * 3], y1 = w.pos[o + n * 3 + 1], z1 = w.pos[o + n * 3 + 2];
            cx += (y0 - y1) * (z0 + z1);
            cy += (z0 - z1) * (x0 + x1);
            cz += (x0 - x1) * (y0 + y1);
        }
        float nx = 0, ny = 0, nz = 0;
        for (int c = 0; c < 4; c++) { nx += w.nrm[o + c * 3]; ny += w.nrm[o + c * 3 + 1]; nz += w.nrm[o + c * 3 + 2]; }
        double lc = Math.sqrt(cx * cx + cy * cy + cz * cz), ln = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (lc < 1e-9 || ln < 1e-9) return 0f;
        return (float) ((cx * nx + cy * ny + cz * nz) / (lc * ln));
    }

    @Test
    void theGlowingPartsOfEveryModelAreWoundFrontFacing() throws IOException {
        int emissive = 0, front = 0;
        List<String> worst = new ArrayList<>();
        for (Path p : models()) {
            String name = p.getFileName().toString().replace(".txt", "");
            WornMesh w = WornAssets.mesh(name).orElse(null);
            if (w == null) continue;
            int bad = 0, e = 0;
            for (int q = 0; q < w.quads; q++) {
                if ((w.flags[q] & WornMesh.EMIT) == 0) continue;
                e++;
                float f = facing(w, q);
                if (f > 0.2f) front++;
                else if (f < -0.2f) bad++;
            }
            emissive += e;
            if (bad > 0) worst.add(name + "=" + bad + "/" + e);
        }
        assertTrue(emissive > 5000, "emissive quads: " + emissive);
        assertEquals(emissive, front, front + " of " + emissive + " glowing quads face front; reversed: " + worst);
    }

    @Test
    void aMirroredPieceWritesItsCornersInTheOtherOrder() {
        assertEquals(0, WornMesh.corner(false, 0));
        assertEquals(3, WornMesh.corner(false, 3));
        assertEquals(3, WornMesh.corner(true, 0));
        assertEquals(0, WornMesh.corner(true, 3));
        boolean[] seen = new boolean[4];
        for (int j = 0; j < 4; j++) seen[WornMesh.corner(true, j)] = true;
        for (boolean s : seen) assertTrue(s, "every corner is written once");
    }

    @Test
    void everyWornRelicOfTheCatalogDressesFromTheJar() {
        var c = WornAssets.catalog();
        for (String slot : List.of("HEAD", "CHEST", "NECK", "BACK", "BELT", "HANDS", "CHARM", "FEET")) {
            String look = com.newuniverse.nusmp.client.relics.worn.lib.WornCatalog.defaultLook(slot);
            assertTrue(WornAssets.hasModel("any_relic", slot, look), slot + "/" + look + " has no model in the jar: " + WornAssets.dress("any_relic", slot, look).lines());
        }
        assertEquals(com.newuniverse.nusmp.client.relics.worn.lib.WornCatalog.Source.NONE, c.sourceOf("x", "MAIN_HAND", "SWORD"));
    }

    @Test
    void theOwnRendererListIsTheOneWornRenderersRegisters() throws IOException {
        String src = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/client/relics/worn/WornRenderers.java"));
        for (String id : WornAssets.CUSTOM_RENDERERS) assertTrue(src.contains("register(\"" + id + "\""), id + " is not registered by WornRenderers.init");
        int registers = src.split("register\\(\"", -1).length - 1;
        assertEquals(WornAssets.CUSTOM_RENDERERS.size(), registers, "WornRenderers registers a relic this list does not name");
    }
}
