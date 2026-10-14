package com.newuniverse.nusmp.relics.djinn.weapon;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.relics.djinn.Djinn;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The generated art of the 24 Djinn weapons (tools/relics/djinn_weapons.py) is all there, well formed, and in the size band of the size rule. */
class DjinnWeaponAssetsTest {
    private static final Path ASSETS = Path.of("src", "main", "resources", "assets", "nusmp");
    private static final Path TOOL = Path.of("tools", "relics", "djinn_weapons.py");

    private static List<String> ids() {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        for (Djinn d : Djinn.values()) {
            out.add(DjinnWeaponRules.itemPath(d, false));
            out.add(DjinnWeaponRules.itemPath(d, true));
        }
        return out;
    }

    private static BufferedImage png(Path p) throws IOException {
        assertTrue(Files.exists(p), "missing " + p);
        BufferedImage img = ImageIO.read(p.toFile());
        assertNotNull(img, "unreadable " + p);
        return img;
    }

    private static int opaquePixels(BufferedImage img) {
        int n = 0;
        for (int y = 0; y < img.getHeight(); y++) for (int x = 0; x < img.getWidth(); x++) if ((img.getRGB(x, y) >>> 24) > 0) n++;
        return n;
    }

    @Test
    void everyWeaponHasItsMeshWithSaneQuadsAndTheManualDirective() throws IOException {
        for (String id : ids()) {
            Path p = ASSETS.resolve("weapon_meshes").resolve(id + ".txt");
            assertTrue(Files.exists(p), "missing " + p);
            List<String> lines = Files.readAllLines(p);
            double scale = 0, ymin = 0, ymax = -1e9, ylo = 1e9;
            boolean place = false, manual = false, glow = false;
            int body = 0, glowQuads = 0;
            for (String line : lines) {
                String[] v = line.trim().split("\\s+");
                if (v[0].equals("place")) {
                    place = true;
                    assertEquals(45.0, Double.parseDouble(v[3]), 1e-6, id + ": the mesh lies on the sprite diagonal");
                    scale = Double.parseDouble(v[4]);
                    ymin = Double.parseDouble(v[5]);
                } else if (v[0].equals("orient") && v[1].equals("manual")) manual = true;
                else if (v[0].equals("glow")) glow = true;
                else if (v[0].equals("qn")) {
                    assertEquals(34, v.length, id + ": a qn line is two numbers, then four vertices of eight");
                    int layer = Integer.parseInt(v[1]);
                    if (layer == 0) body++;
                    else if (layer == 2) glowQuads++;
                    else fail(id + ": only the body and glow layers are used");
                    for (int k = 0; k < 4; k++) {
                        int o = 2 + k * 8;
                        double u = Double.parseDouble(v[o + 3]), w = Double.parseDouble(v[o + 4]);
                        assertTrue(u >= -0.01 && u <= 128.01 && w >= -0.01 && w <= 128.01, id + ": uv in 128 space");
                        double n = Math.sqrt(Math.pow(Double.parseDouble(v[o + 5]), 2) + Math.pow(Double.parseDouble(v[o + 6]), 2) + Math.pow(Double.parseDouble(v[o + 7]), 2));
                        assertEquals(1.0, n, 0.02, id + ": unit normals");
                        double y = Double.parseDouble(v[o + 1]);
                        ymax = Math.max(ymax, y);
                        ylo = Math.min(ylo, y);
                    }
                }
            }
            assertTrue(place && manual && glow, id + ": place, orient manual and glow lines");
            assertTrue(body >= 100 && glowQuads >= 10, id + ": the geometry came across");
            assertEquals(ylo, ymin, 0.01, id + ": the place line's ymin is the mesh's lowest point");
            double apparent = (ymax - ymin) * scale * 0.85;
            Djinn d = DjinnWeaponRules.djinnOfPath(id);
            double min = d == Djinn.MARCHOSIAS ? 1.5 : 2.2;                                    // the claws are a short weapon; every other one is a long one (the size rule: 2.2 to 3.6)
            assertTrue(apparent >= min && apparent <= 3.6, id + ": apparent length " + apparent);
        }
    }

    @Test
    void everyWeaponHasASkinAGlowSkinAndAnIcon() throws IOException {
        for (String id : ids()) {
            BufferedImage skin = png(ASSETS.resolve("textures/entity/weapon").resolve(id + ".png"));
            BufferedImage glow = png(ASSETS.resolve("textures/entity/weapon").resolve(id + "_glow.png"));
            assertEquals(skin.getWidth(), glow.getWidth(), id);
            assertTrue(opaquePixels(skin) > 2000, id + ": the skin has the weapon's paint");
            assertTrue(opaquePixels(glow) > 200, id + ": the glow skin has paint");
            BufferedImage icon = png(ASSETS.resolve("textures/item").resolve(id + ".png"));
            assertEquals(16, icon.getWidth(), id);
            assertEquals(16, icon.getHeight(), id);
            assertTrue(opaquePixels(icon) >= 20, id + ": the icon shows something");
        }
    }

    @Test
    void theDarkLookIsAnotherSkinNotACopy() throws IOException {
        for (Djinn d : Djinn.values()) {
            byte[] light = Files.readAllBytes(ASSETS.resolve("textures/entity/weapon").resolve(DjinnWeaponRules.itemPath(d, false) + ".png"));
            byte[] dark = Files.readAllBytes(ASSETS.resolve("textures/entity/weapon").resolve(DjinnWeaponRules.itemPath(d, true) + ".png"));
            assertFalse(java.util.Arrays.equals(light, dark), d + ": the two looks are different skins");
        }
    }

    @Test
    void everyModelJsonIsBuiltinEntityWithTheStandardThirdPersonAndAnIconModel() throws IOException {
        for (String id : ids()) {
            JsonObject m = JsonParser.parseString(Files.readString(ASSETS.resolve("models/item").resolve(id + ".json"))).getAsJsonObject();
            assertEquals("minecraft:builtin/entity", m.get("parent").getAsString(), id);
            assertEquals("manual", m.get("nusmp_display_preset").getAsString(), id + ": hand-tuned, display_presets.py leaves it alone");
            JsonObject disp = m.getAsJsonObject("display");
            assertEquals(0.85, disp.getAsJsonObject("thirdperson_righthand").getAsJsonArray("scale").get(0).getAsDouble(), 1e-9, id);
            double fp = disp.getAsJsonObject("firstperson_righthand").getAsJsonArray("scale").get(0).getAsDouble();
            assertTrue(fp > 0.2 && fp <= 0.68, id + ": first-person scale " + fp);
            JsonObject icon = JsonParser.parseString(Files.readString(ASSETS.resolve("models/item").resolve(id + "_icon.json"))).getAsJsonObject();
            assertEquals("nusmp:item/" + id, icon.getAsJsonObject("textures").get("layer0").getAsString(), id);
        }
    }

    @Test
    void theToolsActTableMatchesTheRules() throws IOException {
        String tool = Files.readString(TOOL);
        for (Djinn d : Djinn.values()) {
            DjinnWeaponRules.Spec s = DjinnWeaponRules.spec(d);
            for (int a = 0; a < 2; a++) {
                String key = s.act(a).key();
                assertTrue(tool.contains("\"" + key + "\": ("), "the lang table of tools/relics/djinn_weapons.py names the act " + key);
            }
            assertTrue(tool.contains("\"" + d.id() + "\""), d.id());
        }
    }
}
