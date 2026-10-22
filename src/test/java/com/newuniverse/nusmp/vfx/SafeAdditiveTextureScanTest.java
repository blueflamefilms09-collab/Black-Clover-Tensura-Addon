package com.newuniverse.nusmp.vfx;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 1.110.0 (docs/visual_upgrade/safe_additive.md): the textures an additive pass draws (the same scope as tools/safe_additive/scan_textures.py) carry no colour under a fully transparent pixel,
 * and the card sheet halo.png has transparent card borders and no solid white card (the white rectangles of the upgraded weapons). Time Magic textures (time_*) are never looked at.
 */
class SafeAdditiveTextureScanTest {
    private static final Path TEX = Path.of("src/main/resources/assets/nusmp/textures");
    private static final String[][] SCOPE = {
            {"entity/weapon_vu", ""}, {"visual_upgrade", "vu_fx|halo|glow|veins|nebula|star"}, {"entity/weapon", "_glow|veins|halo|nebula|star"}, {"entity/weapon/up", "_glow|veins|halo|nebula|star"},
            {"entity/djinn", "glow|rune|nebula|star|halo"}, {"relic_worn", "glow|nebula|star|halo|atlas|shimmer|fx"}, {"aura", "glow"}, {"entity/scalecast", "glow"},
            {"entity", "firecast_salamander_fx|firecast_dragon_fx"}, {"entity", "sylph_small_fx|sylph_powered_fx|sylph_halo|sylph_small_wings_glow|sylph_powered_wings_glow"},
            {"entity/spirit_space", ""}, {"entity", "spirit_space_orb|spirit_space_wisp_fx"},
            {"entity", "undine_fx|undine_ring"}, {"entity", "spiritlight_cub_fx|spiritlight_seraph_fx"},
            {"particle", "spiritlight_"}, {"entity", "gnome_companion_fx|gnome_full_fx|gnome_faces_fx"}};

    private static boolean wanted(String name, String keys) {
        if (!name.endsWith(".png") || name.startsWith("time_")) return false;
        if (keys.isEmpty()) return true;
        for (String k : keys.split("\\|")) if (name.contains(k)) return true;
        return false;
    }

    private static List<Path> scope() throws IOException {
        List<Path> out = new ArrayList<>();
        for (String[] d : SCOPE) {
            Path dir = TEX.resolve(d[0]);
            if (!Files.isDirectory(dir)) continue;
            try (Stream<Path> s = Files.list(dir)) {
                s.filter(p -> wanted(p.getFileName().toString(), d[1])).sorted().forEach(out::add);
            }
        }
        return out;
    }

    private static int[] argb(Path p, int[] size) throws IOException {
        BufferedImage img = ImageIO.read(p.toFile());
        int w = img.getWidth(), h = img.getHeight();
        size[0] = w;
        size[1] = h;
        int[] px = new int[w * h];
        img.getRGB(0, 0, w, h, px, 0, w);
        return px;
    }

    @Test
    void noAdditiveTextureCarriesColourUnderAFullyTransparentPixel() throws IOException {
        List<String> bad = new ArrayList<>();
        List<Path> all = scope();
        assertTrue(all.size() > 200, "the scope found " + all.size() + " textures");
        for (Path p : all) {
            int[] sz = new int[2];
            int[] px = argb(p, sz);
            for (int v : px) if ((v >>> 24) == 0 && (v & 0xFFFFFF) != 0) { bad.add(TEX.relativize(p).toString()); break; }
        }
        assertTrue(bad.isEmpty(), "run python3 -B tools/safe_additive/scan_textures.py --fix: " + bad);
    }

    @Test
    void theHaloCardSheetHasTransparentBordersAndNoSolidCard() throws IOException {
        int[] sz = new int[2];
        int[] px = argb(TEX.resolve("entity/weapon_vu/halo.png"), sz);
        int w = sz[0], cell = 128;
        for (int cy = 0; cy < sz[1]; cy += cell) {
            for (int cx = 0; cx < w; cx += cell) {
                int border = 0, solid = 0;
                for (int y = 0; y < cell; y++) {
                    for (int x = 0; x < cell; x++) {
                        int v = px[(cy + y) * w + cx + x], a = v >>> 24;
                        if (x == 0 || y == 0 || x == cell - 1 || y == cell - 1) border = Math.max(border, a);
                        if (a >= 200 && Math.min((v >> 16) & 255, Math.min((v >> 8) & 255, v & 255)) >= 235) solid++;
                    }
                }
                String at = "cell " + (cy / cell * 4 + cx / cell);
                assertTrue(border <= 6, at + " has a visible card border (alpha " + border + ")");
                assertTrue(solid < cell * cell * 0.35, at + " is a solid white card");
            }
        }
    }

    @Test
    void elsdociasAdditivePassTexturesAndTheSharedCardSheetAreClean() throws IOException {
        for (String f : new String[]{"elsdocia_glow.png", "elsdocia_veins.png", "elsdocia_lod_glow.png", "elsdocia_lod_veins.png", "halo.png"}) {
            int[] sz = new int[2];
            int[] px = argb(TEX.resolve("entity/weapon_vu/" + f), sz);
            int bad = 0;
            for (int v : px) if ((v >>> 24) == 0 && (v & 0xFFFFFF) != 0) bad++;
            assertEquals(0, bad, f);
        }
    }
}
