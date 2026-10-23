package com.newuniverse.nusmp.book;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.book.ModeArmorWindV2Rules.Entry;
import com.newuniverse.nusmp.book.ModeArmorWindV2Rules.Pass;
import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the five textures of the overlay v2 are painted by tools/gen_mwv2_textures.py from tools/mwv2_layout.json and sampled by the meshes through the same numbers in {@link ModeArmorWindV2Rules}; this test
 * pins that the two layouts agree, the textures have the layout's size, the regions do not overlap, every quad of every mesh samples an opaque texel of its own texture (the cloth, the glow map and the wings:
 * every quad; the fur and the light atlas: nearly every quad, the tuft gaps and the soft glows are meant to be partly clear), the additive textures carry no colour under transparent texels (no boxes under
 * SafeAdditive), and the cloth regions are fully opaque.
 */
class ModeArmorWindV2AssetTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path TEX = ROOT.resolve("src/main/resources/assets/nusmp");

    private static BufferedImage read(String path) throws IOException { return ImageIO.read(TEX.resolve(path).toFile()); }

    private static int[] arr(com.google.gson.JsonArray a) {
        int[] r = new int[a.size()];
        for (int i = 0; i < r.length; i++) r[i] = a.get(i).getAsInt();
        return r;
    }

    @Test
    void theJsonLayoutAndTheJavaLayoutSayTheSame() throws IOException {
        JsonObject j = JsonParser.parseString(Files.readString(ROOT.resolve("tools/mwv2_layout.json"))).getAsJsonObject();
        assertEquals(ModeArmorWindV2Rules.COAT_W, arr(j.getAsJsonObject("coat").getAsJsonArray("size"))[0]);
        assertEquals(ModeArmorWindV2Rules.COAT_H, arr(j.getAsJsonObject("coat").getAsJsonArray("size"))[1]);
        assertEquals(ModeArmorWindV2Rules.FUR_W, arr(j.getAsJsonObject("fur").getAsJsonArray("size"))[0]);
        assertEquals(ModeArmorWindV2Rules.WING_H, arr(j.getAsJsonObject("wing").getAsJsonArray("size"))[1]);
        assertEquals(ModeArmorWindV2Rules.FX_W, arr(j.getAsJsonObject("fx").getAsJsonArray("size"))[0]);
        String[][] map = {
                {"coat", "TORSO", "R_TORSO"}, {"coat", "TAIL_BACK", "R_TAIL_BACK"}, {"coat", "TAIL_FRONT", "R_TAIL_FRONT"}, {"coat", "SLEEVE", "R_SLEEVE"}, {"coat", "MANTLE", "R_MANTLE"},
                {"coat", "TROUSER", "R_TROUSER"}, {"coat", "BOOT", "R_BOOT"}, {"coat", "SASH", "R_SASH"}, {"coat", "BUCKLE", "R_BUCKLE"}, {"coat", "CORD", "R_CORD"}, {"coat", "TASSEL", "R_TASSEL"},
                {"fur", "OUTER", "R_FUR_OUTER"}, {"fur", "LIP", "R_FUR_LIP"}, {"fur", "INNER", "R_FUR_INNER"}, {"wing", "WING", "R_WING"},
                {"fx", "BLADE", "R_BLADE"}, {"fx", "BLADE_FLAT", "R_BLADE_FLAT"}, {"fx", "ARROW", "R_ARROW"}, {"fx", "RING", "R_RING"}, {"fx", "RING_TOP", "R_RING_TOP"}, {"fx", "HALO", "R_HALO"},
                {"fx", "CROSS", "R_CROSS"}, {"fx", "RIBBON", "R_RIBBON"}, {"fx", "LEAF", "R_LEAF"}, {"fx", "EYE", "R_EYE"}, {"fx", "HAIR", "R_HAIR"}};
        for (String[] m : map) {
            int[] json = arr(j.getAsJsonObject(m[0]).getAsJsonObject("regions").getAsJsonArray(m[1]));
            try {
                int[] java = (int[]) ModeArmorWindV2Rules.class.getField(m[2]).get(null);
                assertEquals(java.length, json.length);
                for (int i = 0; i < json.length; i++) assertEquals(json[i], java[i], m[0] + "." + m[1] + " differs between tools/mwv2_layout.json and ModeArmorWindV2Rules." + m[2]);
            } catch (ReflectiveOperationException ex) {
                throw new AssertionError(ex);
            }
        }
    }

    @Test
    void theTexturesHaveTheLayoutSizeAndTheRegionsDoNotOverlap() throws IOException {
        int[][] sizes = {{ModeArmorWindV2Rules.COAT_W, ModeArmorWindV2Rules.COAT_H}, {ModeArmorWindV2Rules.FUR_W, ModeArmorWindV2Rules.FUR_H}, {ModeArmorWindV2Rules.COAT_W, ModeArmorWindV2Rules.COAT_H},
                {ModeArmorWindV2Rules.WING_W, ModeArmorWindV2Rules.WING_H}, {ModeArmorWindV2Rules.FX_W, ModeArmorWindV2Rules.FX_H}};
        List<String> all = ModeArmorWindV2Rules.allTextures();
        for (int i = 0; i < all.size(); i++) {
            BufferedImage im = read(all.get(i));
            assertEquals(sizes[i][0], im.getWidth(), all.get(i));
            assertEquals(sizes[i][1], im.getHeight(), all.get(i));
        }
        int[][] coat = {ModeArmorWindV2Rules.R_TORSO, ModeArmorWindV2Rules.R_TAIL_BACK, ModeArmorWindV2Rules.R_TAIL_FRONT, ModeArmorWindV2Rules.R_SLEEVE, ModeArmorWindV2Rules.R_MANTLE,
                ModeArmorWindV2Rules.R_TROUSER, ModeArmorWindV2Rules.R_BOOT, ModeArmorWindV2Rules.R_SASH, ModeArmorWindV2Rules.R_BUCKLE, ModeArmorWindV2Rules.R_CORD, ModeArmorWindV2Rules.R_TASSEL};
        int[][] fx = {ModeArmorWindV2Rules.R_BLADE, ModeArmorWindV2Rules.R_BLADE_FLAT, ModeArmorWindV2Rules.R_ARROW, ModeArmorWindV2Rules.R_RING, ModeArmorWindV2Rules.R_RING_TOP, ModeArmorWindV2Rules.R_HALO,
                ModeArmorWindV2Rules.R_CROSS, ModeArmorWindV2Rules.R_RIBBON, ModeArmorWindV2Rules.R_LEAF, ModeArmorWindV2Rules.R_EYE, ModeArmorWindV2Rules.R_HAIR};
        int[][] fur = {ModeArmorWindV2Rules.R_FUR_OUTER, ModeArmorWindV2Rules.R_FUR_LIP, ModeArmorWindV2Rules.R_FUR_INNER};
        for (int[][] set : new int[][][]{coat, fx, fur})
            for (int a = 0; a < set.length; a++)
                for (int b = a + 1; b < set.length; b++)
                    assertTrue(set[a][2] <= set[b][0] || set[b][2] <= set[a][0] || set[a][3] <= set[b][1] || set[b][3] <= set[a][1], "regions " + a + " and " + b + " overlap");
    }

    @Test
    void everyQuadSamplesAnOpaqueTexelOfItsOwnTexture() throws IOException {
        BufferedImage coat = read(ModeArmorWindV2Rules.COAT), fur = read(ModeArmorWindV2Rules.FUR), glow = read(ModeArmorWindV2Rules.GLOW), wing = read(ModeArmorWindV2Rules.WING), fx = read(ModeArmorWindV2Rules.FX);
        List<String> bad = new ArrayList<>();
        int furQuads = 0, furClear = 0, fxQuads = 0, fxClear = 0;
        for (Entry e : ModeArmorWindV2Rules.entries(Lod.FULL)) {
            ModeArmorWindV2Mesh m = ModeArmorWindV2Mesh.get(e.piece(), e.detail());
            BufferedImage im = switch (e.pass()) { case COAT -> coat; case FUR -> fur; case GLOW -> glow; case WING, WING_ADD -> wing; case FX, ADD -> fx; };
            for (int q = 0; q < m.verts; q += 4) {
                float u = 0, v = 0;
                for (int k = 0; k < 4; k++) { u += m.uv[(q + k) * 2] / 4f; v += m.uv[(q + k) * 2 + 1] / 4f; }
                int a = im.getRGB(Math.min(im.getWidth() - 1, (int) (u * im.getWidth())), Math.min(im.getHeight() - 1, (int) (v * im.getHeight()))) >>> 24;
                if (e.pass() == Pass.FUR) { furQuads++; if (a < 8) furClear++; }
                else if (e.pass() == Pass.FX || e.pass() == Pass.ADD) { fxQuads++; if (a < 8) fxClear++; }
                else if (e.pass() == Pass.GLOW) { if (a < 6) bad.add(e + " quad " + q / 4 + " samples no glow wash"); }
                else if (a < 40) bad.add(e + " quad " + q / 4 + " samples a transparent texel (alpha " + a + ")");
            }
        }
        assertTrue(bad.isEmpty(), String.join("; ", bad.subList(0, Math.min(6, bad.size()))));
        assertTrue(furClear <= furQuads / 10, "the fur quads are on fur: " + furClear + " of " + furQuads + " are clear");
        assertTrue(fxClear <= fxQuads / 8, "the light quads are on light: " + fxClear + " of " + fxQuads + " are clear");
    }

    @Test
    void theClothIsOpaqueAndTheAdditiveTexturesCarryNoColourUnderTransparentTexels() throws IOException {
        BufferedImage coat = read(ModeArmorWindV2Rules.COAT);
        int[][] cloth = {ModeArmorWindV2Rules.R_TORSO, ModeArmorWindV2Rules.R_TAIL_BACK, ModeArmorWindV2Rules.R_TAIL_FRONT, ModeArmorWindV2Rules.R_SLEEVE, ModeArmorWindV2Rules.R_MANTLE,
                ModeArmorWindV2Rules.R_TROUSER, ModeArmorWindV2Rules.R_BOOT, ModeArmorWindV2Rules.R_SASH, ModeArmorWindV2Rules.R_BUCKLE, ModeArmorWindV2Rules.R_CORD, ModeArmorWindV2Rules.R_TASSEL};
        for (int[] r : cloth)
            for (int y = r[1]; y < r[3]; y++)
                for (int x = r[0]; x < r[2]; x++) assertEquals(255, coat.getRGB(x, y) >>> 24, "a hole in the cloth at " + x + "," + y);
        for (String t : new String[]{ModeArmorWindV2Rules.GLOW, ModeArmorWindV2Rules.FX, ModeArmorWindV2Rules.FUR, ModeArmorWindV2Rules.WING, ModeArmorWindV2Rules.COAT}) {
            BufferedImage im = read(t);
            int stray = 0;
            for (int y = 0; y < im.getHeight(); y++)
                for (int x = 0; x < im.getWidth(); x++) {
                    int p = im.getRGB(x, y);
                    if ((p >>> 24) == 0 && (p & 0xFFFFFF) != 0) stray++;
                }
            assertEquals(0, stray, t + " has colour under transparent texels");
        }
        BufferedImage glow = read(ModeArmorWindV2Rules.GLOW);
        long lit = 0;
        for (int y = 0; y < glow.getHeight(); y++) for (int x = 0; x < glow.getWidth(); x++) if ((glow.getRGB(x, y) >>> 24) > 120) lit++;
        assertTrue(lit > 800, "the glow map has seams and trim lit: " + lit);
    }
}
