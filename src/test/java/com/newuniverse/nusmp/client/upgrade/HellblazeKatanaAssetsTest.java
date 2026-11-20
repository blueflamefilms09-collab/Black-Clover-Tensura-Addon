package com.newuniverse.nusmp.client.upgrade;

import com.newuniverse.nusmp.client.HellblazeSayaLayer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: Hellblaze, The Demon Sin as a katana (docs/hellblaze_katana.md) as data and source: the new meshes and skins exist and fit their budgets, the classic broadsword files are all still there, the spec carries the
 * katana block without touching the classic values, the client switch exists and defaults on, the sheath and the hit watcher are client-only with no packet, the Better Combat file is the katana's, and the engravings are
 * there. Reads the files (no game needed); nothing here says how it looks in a game.
 */
class HellblazeKatanaAssetsTest {
    static final Path ASSETS = Path.of("src/main/resources/assets/anime_multiverse");
    static final Path JAVA = Path.of("src/main/java/com/newuniverse/nusmp");
    static final String ID = "hellblaze_the_demon_sin", KATANA = ID + "__katana", SAYA = ID + "__saya";

    record Mesh(int quads, int[] layers, String place, boolean manual, String grip, double ymin, double ymax) {}

    static Mesh read(Path f) throws IOException {
        int quads = 0;
        int[] layers = new int[3];
        String place = null, grip = null;
        boolean manual = false;
        double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;
        for (String line : Files.readAllLines(f)) {
            String[] v = line.trim().split("\\s+");
            if (v[0].isEmpty() || v[0].startsWith("#")) continue;
            if (v[0].equals("place")) place = line;
            if (v[0].equals("grip")) grip = line;
            if (v[0].equals("orient") && v.length > 1 && v[1].equals("manual")) manual = true;
            if (v[0].equals("qn") || v[0].equals("q")) {
                quads++;
                layers[Integer.parseInt(v[1])]++;
                int stride = v[0].equals("q") ? 5 : 8;
                for (int k = 0; k < 4; k++) {
                    double y = Double.parseDouble(v[2 + k * stride + 1]);
                    lo = Math.min(lo, y);
                    hi = Math.max(hi, y);
                }
            }
        }
        return new Mesh(quads, layers, place, manual, grip, lo, hi);
    }

    static int[] pngSize(Path p) throws IOException {
        BufferedImage im = ImageIO.read(p.toFile());
        return new int[]{im.getWidth(), im.getHeight()};
    }

    @Test
    void theNewFilesExist() {
        List<String> missing = new ArrayList<>();
        for (String f : new String[]{"weapon_meshes/up/" + KATANA + ".txt", "weapon_meshes/up/" + KATANA + "_lod.txt", "weapon_meshes/up/" + SAYA + ".txt",
                "textures/entity/weapon/up/" + KATANA + ".png", "textures/entity/weapon/up/" + KATANA + "_glow.png", "textures/entity/weapon/up/" + KATANA + "_veins.png",
                "textures/entity/weapon/up/" + KATANA + "_lod.png", "textures/entity/weapon/up/" + KATANA + "_lod_glow.png",
                "textures/entity/weapon/up/" + SAYA + ".png", "textures/entity/weapon/up/" + SAYA + "_glow.png"})
            if (!Files.exists(ASSETS.resolve(f))) missing.add(f);
        assertEquals(List.of(), missing);
    }

    @Test
    void theClassicBroadswordStaysInTheJar() {
        List<String> missing = new ArrayList<>();
        for (String f : new String[]{"weapon_meshes/up/" + ID + ".txt", "weapon_meshes/up/" + ID + "_lod.txt", "weapon_meshes/" + ID + ".txt",
                "textures/entity/weapon/up/" + ID + ".png", "textures/entity/weapon/up/" + ID + "_glow.png", "textures/entity/weapon/up/" + ID + "_veins.png",
                "textures/entity/weapon/up/" + ID + "_lod.png", "textures/entity/weapon/up/" + ID + "_lod_glow.png", "textures/entity/weapon/" + ID + ".png", "textures/entity/weapon/" + ID + "_glow.png",
                "textures/item/" + ID + ".png", "models/item/" + ID + "_icon.json", "models/item/" + ID + ".json", "visual_upgrade/" + ID + ".json"})
            if (!Files.exists(ASSETS.resolve(f))) missing.add(f);
        assertEquals(List.of(), missing);
    }

    @Test
    void theSkinsHaveTheirSizesAndTheGlowSkinsAreGreyMasks() throws IOException {
        for (String f : new String[]{KATANA, KATANA + "_glow", KATANA + "_veins", SAYA, SAYA + "_glow"}) {
            int[] s = pngSize(ASSETS.resolve("textures/entity/weapon/up/" + f + ".png"));
            assertEquals(1024, s[0], f);
            assertEquals(1024, s[1], f);
        }
        for (String f : new String[]{KATANA + "_lod", KATANA + "_lod_glow"})
            assertEquals(256, pngSize(ASSETS.resolve("textures/entity/weapon/up/" + f + ".png"))[0], f);
        for (String f : new String[]{KATANA + "_glow", KATANA + "_veins", KATANA + "_lod_glow", SAYA + "_glow"}) {        // grey, so the tint of the glow line (dark red, purple on a hit) is the colour
            BufferedImage im = ImageIO.read(ASSETS.resolve("textures/entity/weapon/up/" + f + ".png").toFile());
            int lit = 0;
            for (int y = 0; y < im.getHeight(); y++)
                for (int x = 0; x < im.getWidth(); x++) {
                    int p = im.getRGB(x, y);
                    if ((p >>> 24) < 16) continue;
                    lit++;
                    assertEquals(p >> 16 & 255, p >> 8 & 255, f + " is not grey at " + x + "," + y);
                    assertEquals(p >> 8 & 255, p & 255, f + " is not grey at " + x + "," + y);
                }
            assertTrue(lit > 200, f + " has almost nothing lit");
        }
    }

    @Test
    void theMeshesStayInsideTheirBudgets() throws IOException {
        Mesh full = read(ASSETS.resolve("weapon_meshes/up/" + KATANA + ".txt")), lod = read(ASSETS.resolve("weapon_meshes/up/" + KATANA + "_lod.txt")), saya = read(ASSETS.resolve("weapon_meshes/up/" + SAYA + ".txt"));
        assertTrue(full.quads() <= 1800 && full.layers()[0] >= 400 && full.layers()[2] >= 20, "full katana: " + full.quads());
        assertTrue(lod.quads() <= 0.25 * full.quads() && lod.quads() <= 320, "low-detail katana: " + lod.quads() + " of " + full.quads());
        assertTrue(saya.quads() <= HellblazeSayaLayer.QUADS, "sheath: " + saya.quads());
        for (Mesh m : new Mesh[]{full, lod}) {
            assertNotNull(m.place());
            assertTrue(m.place().startsWith("place -0.10872 -0.13203 44.750 "), "the katana keeps the old Hellblaze's hand spot: " + m.place());
            assertNotNull(m.grip());
            assertTrue(m.manual(), "orient manual: the size and the way up are exactly as modelled");
            double inHand = (m.ymax() - m.ymin()) * Double.parseDouble(m.place().split("\\s+")[4]);
            assertEquals(1.72, inHand, 0.02, "in-hand length in blocks (the class band of a longsword is 1.4 to 1.8)");
        }
    }

    @Test
    void theSpecCarriesTheKatanaBlockAndTheClassicValuesAreUntouched() throws IOException {
        UpgradeSpec classic = UpgradeSpec.parse(Files.newBufferedReader(ASSETS.resolve("visual_upgrade/" + ID + ".json")));
        assertEquals(0xFF6428, classic.primary, "the classic orange palette is as it was");
        assertEquals("", classic.mesh);
        assertEquals(0, classic.hitTicks);
        assertFalse(classic.veinsTint);
        assertEquals("up/" + ID + ".png", classic.skin);
        UpgradeSpec k = classic.alt;
        assertNotNull(k, "the katana block");
        assertNull(k.alt);
        assertEquals("up/" + KATANA, k.mesh);
        assertEquals("up/" + KATANA + ".png", k.skin);
        assertEquals("up/" + KATANA + "_lod.png", k.lodSkin);
        assertEquals(10, k.hitTicks);
        assertEquals(0x963CC8, k.hitRgb);
        assertTrue(k.veinsTint);
        int r = k.primary >> 16 & 255, g = k.primary >> 8 & 255, b = k.primary & 255;
        assertTrue(r > 100 && r < 220 && g < 40 && b < 70, "the katana palette is dark red: " + Integer.toHexString(k.primary));
        assertEquals((k.secondary >> 16 & 255) < 60, true, "and black");
        assertEquals(classic.acts.size(), k.acts.size());
        for (int i = 0; i < classic.acts.size(); i++) {
            assertEquals(classic.acts.get(i).act(), k.acts.get(i).act());
            assertEquals(classic.acts.get(i).id(), k.acts.get(i).id(), "an act keeps its signature id (the server sends it)");
            assertEquals(classic.acts.get(i).recipe().size(), k.acts.get(i).recipe().size());
            for (UpgradeSpec.Comp c : k.acts.get(i).recipe()) assertTrue((c.rgb() >> 16 & 255) < 215 || (c.rgb() & 255) > 100, "recoloured: " + Integer.toHexString(c.rgb()));
        }
    }

    static void assertNull(Object o) { assertTrue(o == null); }

    @Test
    void theClientSwitchExistsAndIsOnByDefault() throws IOException {
        String cfg = Files.readString(JAVA.resolve("client/upgrade/UpgradeConfig.java"));
        assertTrue(cfg.contains(".define(\"hellblazeKatana\", true)"), "visual.hellblazeKatana, default on");
        assertTrue(cfg.contains("public static boolean hellblazeKatana()"));
        String specs = Files.readString(JAVA.resolve("client/upgrade/UpgradeSpecs.java"));
        assertTrue(specs.contains("UpgradeConfig.hellblazeKatana()"), "the spec lookup follows the switch");
    }

    @Test
    void theSheathAndTheHitWatcherAreClientOnlyAndSendNoPacket() throws IOException {
        for (String f : new String[]{"client/HellblazeSayaLayer.java", "client/upgrade/HellblazeHits.java", "client/upgrade/HellblazeTone.java"}) {
            String t = Files.readString(JAVA.resolve(f));
            for (String bad : new String[]{"net.minecraft.server", "ServerPlayer", "ServerLevel", "PacketDistributor", "CustomPacketPayload", "IPayloadContext", "SelfTest", "static Player "})
                assertFalse(t.contains(bad), f + " must not use " + bad);
        }
        List<String> users = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(JAVA)) {
            for (Path p : (Iterable<Path>) walk.filter(x -> x.toString().endsWith(".java"))::iterator) {
                String rel = JAVA.relativize(p).toString().replace('\\', '/');
                String t = Files.readString(p);
                if ((t.contains("HellblazeSayaLayer") || t.contains("HellblazeHits")) && !rel.startsWith("client/")) users.add(rel);
            }
        }
        assertEquals(List.of(), users, "common code never touches the sheath layer or the hit watcher");
        String layer = Files.readString(JAVA.resolve("client/HellblazeSayaLayer.java"));
        assertTrue(layer.contains("getOffhandItem().isEmpty()") && layer.contains("getItemStack().isEmpty()"), "it never replaces a real off-hand item");
        assertTrue(layer.contains("RANGE"), "far players are skipped");
    }

    @Test
    void betterCombatSwingsItAsAOneHandedKatanaWithTheSameReach() throws IOException {
        String bc = Files.readString(Path.of("src/main/resources/data/anime_multiverse/weapon_attributes/" + ID + ".json"));
        assertTrue(bc.contains("\"bettercombat:katana\""), bc);
        assertTrue(bc.contains("\"range_bonus\": 0.25"), bc);
        assertFalse(bc.contains("two_handed"), bc);
        String manifest = Files.readString(Path.of("tools/bettercombat/manifest.json"));
        int at = manifest.indexOf("\"" + ID + "\"");
        assertTrue(at > 0 && manifest.substring(at, at + 120).contains("\"class\": \"longsword\"") && manifest.substring(at, at + 120).contains("hellblaze_katana"), "class stays longsword (damage row), the swing is the katana's");
    }

    @Test
    void theEngravingsAndTheDamageRowAreKept() throws IOException {
        String w = Files.readString(JAVA.resolve("relics/wave/Wave71.java"));
        int at = w.indexOf("RelicDef.of(\"" + ID + "\"");
        String def = w.substring(at, w.indexOf("RelicDef.of(", at + 10));
        assertTrue(def.contains(".engrave(\"elemental_boost@3\", \"severance\""), "severance (a katana's cut) and the sin's engravings");
        assertTrue(def.contains("GradeDamage") || def.contains("DMG_HELLBLAZE_THE_DEMON_SIN"), "the God longsword damage row");
        assertTrue(w.contains("GradeDamage.damage(\"god\", \"longsword\")"));
    }

    @Test
    void thePreviewsAndTheLangFragmentAreThere() {
        assertTrue(Files.exists(Path.of("docs/hellblaze/previews/katana_blender.png")));
        assertTrue(Files.exists(Path.of("docs/hellblaze/previews/saya_blender.png")));
        assertTrue(Files.exists(Path.of("tools/lang/hellblaze_katana.json")));
        assertTrue(Files.exists(Path.of("docs/hellblaze_katana.md")));
    }
}
