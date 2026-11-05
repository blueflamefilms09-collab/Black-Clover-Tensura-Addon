package com.newuniverse.nusmp.manacore;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why (docs/solid_cores.md): the 24 rank Mana Cores and the Beast Will Core must be SOLID orbs. The cause of the see-through look was geometric (every sphere quad wound clockwise seen from outside, so the culled
 * near half showed the inside), so the test reads the shipped mesh files and checks the winding of every body and glow quad, that the glow is a shell outside the body, that the skins are opaque, and that the
 * 16x16 icons have no semi-transparent pixel and no hole inside the circle. Pure file reads; the working directory is the project root.
 */
class SolidCoresTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");

    private static List<String> ids() {
        List<String> out = new ArrayList<>();
        for (ManaCoreRules.Core c : ManaCoreRules.all()) out.add(c.id());
        out.add("beast_will_core");
        return out;
    }

    private static BufferedImage png(String rel) throws IOException {
        File f = ASSETS.resolve(rel).toFile();
        assertTrue(f.isFile(), rel + " is missing");
        return ImageIO.read(f);
    }

    @Test
    void everyBodyAndGlowQuadFacesOutwardAndTheGlowIsAShellOutsideTheBody() throws IOException {
        for (String id : ids()) {
            Path mesh = ASSETS.resolve("weapon_meshes/" + id + ".txt");
            assertTrue(Files.isRegularFile(mesh), id + ": mesh missing");
            List<double[]> body = new ArrayList<>(), glow = new ArrayList<>();
            boolean hasGlowLine = false;
            for (String line : Files.readAllLines(mesh)) {
                String[] p = line.trim().split("\\s+");
                if (p[0].equals("glow")) hasGlowLine = true;
                if (!p[0].equals("qn")) continue;
                double[][] v = new double[4][3];
                double[] n = new double[3];
                for (int k = 0; k < 4; k++) for (int c = 0; c < 3; c++) {
                    v[k][c] = Double.parseDouble(p[2 + 8 * k + c]);
                    n[c] += Double.parseDouble(p[2 + 8 * k + 5 + c]);
                }
                double[] g = new double[3];                                   // the counter-clockwise (front face) normal of the quad, as two triangles
                add(g, cross(sub(v[1], v[0]), sub(v[2], v[0])));
                add(g, cross(sub(v[2], v[0]), sub(v[3], v[0])));
                assertTrue(dot(g, n) > 0, id + ": a quad is wound clockwise seen from outside (culled front = see-through orb)");
                (p[1].equals("0") ? body : glow).add(new double[]{Math.sqrt(dot(v[0], v[0])), Math.sqrt(dot(v[2], v[2]))});
            }
            assertEquals(288, body.size(), id + ": body quads");
            assertEquals(hasGlowLine ? 288 : 0, glow.size(), id + ": glow quads");
            for (int i = 0; i < glow.size(); i++) assertTrue(glow.get(i)[0] > body.get(i)[0] * 1.01 && glow.get(i)[1] > body.get(i)[1] * 1.01, id + ": the glow must be a shell outside the body");
        }
    }

    @Test
    void everySkinIsOpaqueAndHasItsGlowSkinWhenTheMeshGlows() throws IOException {
        for (String id : ids()) {
            BufferedImage skin = png("textures/entity/weapon/" + id + ".png");
            for (int y = 0; y < skin.getHeight(); y++) for (int x = 0; x < skin.getWidth(); x++)
                assertEquals(255, skin.getRGB(x, y) >>> 24, id + ": skin pixel " + x + "," + y + " is not opaque");
            if (Files.readString(ASSETS.resolve("weapon_meshes/" + id + ".txt")).contains("\nglow ")) png("textures/entity/weapon/" + id + "_glow.png");
        }
    }

    @Test
    void everyIconIsSolidNoSoftPixelAndNoHoleInsideTheCircle() throws IOException {
        for (String id : ids()) {
            BufferedImage ic = png("textures/item/" + id + ".png");
            assertEquals(16, ic.getWidth());
            assertEquals(16, ic.getHeight());
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
                int a = ic.getRGB(x, y) >>> 24;
                assertTrue(a == 0 || a == 255, id + ": semi-transparent icon pixel " + x + "," + y + " (alpha " + a + ")");
                if (!id.equals("beast_will_core") && Math.hypot(x + 0.5 - 8, y + 0.5 - 8) <= 7.0) assertEquals(255, a, id + ": hole inside the orb at " + x + "," + y);
            }
            if (id.equals("beast_will_core")) assertEquals(255, ic.getRGB(8, 8) >>> 24);
        }
    }

    @Test
    void theBeastWillCoreIsA3dOrbWithItsFlatSpriteKeptAsTheIcon() throws IOException {
        JsonObject model = JsonParser.parseString(Files.readString(ASSETS.resolve("models/item/beast_will_core.json"))).getAsJsonObject();
        assertEquals("minecraft:builtin/entity", model.get("parent").getAsString());
        JsonObject icon = JsonParser.parseString(Files.readString(ASSETS.resolve("models/item/beast_will_core_icon.json"))).getAsJsonObject();
        assertEquals("nusmp:item/beast_will_core", icon.getAsJsonObject("textures").get("layer0").getAsString());
        assertTrue(Files.readString(ASSETS.resolve("weapon_classes.json")).contains("\"beast_will_core\": \"handitem\""));
        String renderer = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/client/WeaponRenderer.java"));
        assertTrue(renderer.contains("BeastWillItems.renderables()") && renderer.contains("CoreItems.renderables()"), "WeaponRenderer draws both core families");
    }

    private static double[] sub(double[] a, double[] b) { return new double[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]}; }
    private static double[] cross(double[] a, double[] b) { return new double[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]}; }
    private static double dot(double[] a, double[] b) { return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]; }
    private static void add(double[] a, double[] b) { for (int i = 0; i < 3; i++) a[i] += b[i]; }
}
