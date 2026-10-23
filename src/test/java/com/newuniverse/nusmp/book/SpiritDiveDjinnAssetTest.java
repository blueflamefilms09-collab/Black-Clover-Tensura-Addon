package com.newuniverse.nusmp.book;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.book.SpiritDiveDjinnPieces.Def;
import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDivePieces.Stage;
import com.newuniverse.nusmp.book.SpiritDiveTier.Grade;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Djinn-style dive is made of about 250 generated model files; a missing file, a face outside its texture, a glow map with colour under a zero alpha (the additive blend would show it as a box) or a
 * set that costs more than the Djinn Full Equip it is modelled on would only show up in a running game. This test reads the real files: every piece of every set exists (model, texture, glow map, clip), the
 * models are valid GeoLite (identifiers, faces inside the texture), the textures match the size the model declares, the glow maps are clean (SafeAdditive: no colour where alpha is 0), no file is left over
 * that no list names, and the vertices of every set stay under the Djinn's own numbers, which are counted here from the Djinn's mesh files (assets/nusmp/djinn).
 */
class SpiritDiveDjinnAssetTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path ASSETS = ROOT.resolve("src/main/resources/assets/nusmp");

    private static Set<String> sets() {
        Set<String> out = new LinkedHashSet<>();
        for (Element e : Element.values()) {
            out.add(SpiritDiveDjinnPieces.key(e, false));
            if (SpiritDiveTier.gradeOfElement(e) != Grade.STANDARD) out.add(SpiritDiveDjinnPieces.key(e, true));
        }
        return out;
    }

    private static List<String[]> allPieces() {
        List<String[]> out = new ArrayList<>();                                                           // {set key, piece name, model id}
        for (Element e : Element.values())
            for (boolean ch : new boolean[]{false, true}) {
                if (ch && SpiritDiveTier.gradeOfElement(e) == Grade.STANDARD) continue;
                Set<String> names = new LinkedHashSet<>(SpiritDiveDjinnPieces.fullNames(e, ch));
                names.addAll(SpiritDiveDjinnPieces.halfNames(e, ch));
                for (String n : names) out.add(new String[]{SpiritDiveDjinnPieces.key(e, ch), n, SpiritDiveDjinnPieces.modelId(e, ch, n), e.name() + (ch ? "/c" : "/s")});
            }
        return out;
    }

    private static JsonObject geo(String id) throws IOException {
        return JsonParser.parseString(Files.readString(ASSETS.resolve("geo/entity/" + id + ".geo.json"), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static int faces(JsonObject g) {
        int n = 0;
        for (JsonElement geo : g.getAsJsonArray("minecraft:geometry"))
            for (JsonElement bone : geo.getAsJsonObject().getAsJsonArray("bones"))
                if (bone.getAsJsonObject().has("cubes"))
                    for (JsonElement cube : bone.getAsJsonObject().getAsJsonArray("cubes")) n += cube.getAsJsonObject().getAsJsonObject("uv").size();
        return n;
    }

    @Test
    void everyPieceOfEverySetHasItsModelTextureGlowMapAndClip() {
        List<String> missing = new ArrayList<>();
        for (String[] p : allPieces()) {
            for (String rel : new String[]{"geo/entity/" + p[2] + ".geo.json", "textures/entity/" + p[2] + ".png", "textures/entity/" + p[2] + "_glow.png"})
                if (!Files.isRegularFile(ASSETS.resolve(rel))) missing.add(rel);
            Def d = SpiritDiveDjinnPieces.def(p[1], p[0].endsWith("_c"));
            if (!d.clip().isEmpty() && !Files.isRegularFile(ASSETS.resolve("animations/entity/" + p[2] + ".animation.json"))) missing.add("animation of " + p[2]);
        }
        assertTrue(missing.isEmpty(), "missing: " + missing);
    }

    @Test
    void everyClipTheTableNamesIsInItsAnimationFile() throws IOException {
        for (String[] p : allPieces()) {
            Def d = SpiritDiveDjinnPieces.def(p[1], p[0].endsWith("_c"));
            if (d.clip().isEmpty()) continue;
            JsonObject a = JsonParser.parseString(Files.readString(ASSETS.resolve("animations/entity/" + p[2] + ".animation.json"), StandardCharsets.UTF_8)).getAsJsonObject();
            assertTrue(a.getAsJsonObject("animations").has(d.clip()), p[2] + " has no clip " + d.clip());
        }
    }

    @Test
    void theModelsAreValidAndEveryFaceIsInsideItsTexture() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String[] p : allPieces()) {
            JsonObject g = geo(p[2]);
            JsonObject desc = g.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonObject("description");
            if (!desc.get("identifier").getAsString().equals("geometry." + p[2])) bad.add(p[2] + ": identifier " + desc.get("identifier"));
            int tw = desc.get("texture_width").getAsInt(), th = desc.get("texture_height").getAsInt();
            BufferedImage img = ImageIO.read(ASSETS.resolve("textures/entity/" + p[2] + ".png").toFile());
            if (img.getWidth() != tw || img.getHeight() != th) bad.add(p[2] + ": texture " + img.getWidth() + "x" + img.getHeight() + " but the model says " + tw + "x" + th);
            if (tw > 1024 || th > 1024) bad.add(p[2] + ": texture " + tw + "x" + th + " is bigger than 1024");
            int cubes = 0;
            for (JsonElement bone : g.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones"))
                if (bone.getAsJsonObject().has("cubes"))
                    for (JsonElement ce : bone.getAsJsonObject().getAsJsonArray("cubes")) {
                        cubes++;
                        JsonObject cube = ce.getAsJsonObject();
                        for (var f : cube.getAsJsonObject("uv").entrySet()) {
                            JsonArray uv = f.getValue().getAsJsonObject().getAsJsonArray("uv"), sz = f.getValue().getAsJsonObject().getAsJsonArray("uv_size");
                            if (uv.get(0).getAsInt() < 0 || uv.get(1).getAsInt() < 0 || uv.get(0).getAsInt() + sz.get(0).getAsInt() > tw || uv.get(1).getAsInt() + sz.get(1).getAsInt() > th)
                                bad.add(p[2] + ": a face outside the texture " + uv + " " + sz);
                        }
                    }
            if (cubes == 0) bad.add(p[2] + " has no cubes");
        }
        assertTrue(bad.isEmpty(), "bad models: " + bad.subList(0, Math.min(10, bad.size())));
    }

    @Test
    void theGlowMapsAreCleanAndTheSameSizeAsTheirTexture() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String[] p : allPieces()) {
            BufferedImage body = ImageIO.read(ASSETS.resolve("textures/entity/" + p[2] + ".png").toFile());
            BufferedImage glow = ImageIO.read(ASSETS.resolve("textures/entity/" + p[2] + "_glow.png").toFile());
            if (glow.getWidth() != body.getWidth() || glow.getHeight() != body.getHeight()) bad.add(p[2] + ": the glow map is " + glow.getWidth() + "x" + glow.getHeight());
            int dirty = 0, lit = 0;
            for (int y = 0; y < glow.getHeight(); y++)
                for (int x = 0; x < glow.getWidth(); x++) {
                    int argb = glow.getRGB(x, y);
                    if ((argb >>> 24) == 0 && (argb & 0xFFFFFF) != 0) dirty++;
                    if ((argb >>> 24) != 0 && (argb & 0xFFFFFF) != 0) lit++;
                }
            if (dirty > 0) bad.add(p[2] + ": " + dirty + " glow pixels carry colour under alpha 0 (the additive blend would draw them)");
            Def d = SpiritDiveDjinnPieces.def(p[1], p[0].endsWith("_c"));
            if (d.glow() > 0f && lit == 0) bad.add(p[2] + ": the table gives it a glow pass but the glow map is empty");
        }
        assertTrue(bad.isEmpty(), "glow maps: " + bad.subList(0, Math.min(10, bad.size())));
    }

    @Test
    void noModelFileIsLeftOverThatNoListNames() throws IOException {
        Set<String> named = new LinkedHashSet<>();
        for (String[] p : allPieces()) named.add(p[2]);
        List<String> extra = new ArrayList<>();
        try (Stream<Path> s = Files.list(ASSETS.resolve("geo/entity"))) {
            s.map(x -> x.getFileName().toString()).filter(n -> n.startsWith("sdj_") && n.endsWith(".geo.json")).forEach(n -> {
                if (!named.contains(n.substring(0, n.length() - ".geo.json".length()))) extra.add(n);
            });
        }
        assertTrue(extra.isEmpty(), "model files no set lists (stale after a rename?): " + extra);
    }

    // ------------------------------------------------------------------ the budget, from the files
    private static int djinnQuads(Path dir) throws IOException {
        int n = 0;
        try (Stream<Path> s = Files.list(dir)) {
            for (Path f : (Iterable<Path>) s::iterator) for (String line : Files.readAllLines(f)) if (line.startsWith("qn ")) n++;
        }
        return n;
    }

    @Test
    void theDjinnNumbersTheCapsAreBuiltOnAreWhatTheDjinnFilesSay() throws IOException {
        int max = 0, min = Integer.MAX_VALUE;
        try (Stream<Path> dirs = Files.list(ASSETS.resolve("djinn"))) {
            for (Path d : (Iterable<Path>) dirs::iterator) {
                if (!d.getFileName().toString().startsWith("u_")) continue;                              // the unique look: what a Djinn is drawn with
                int q = djinnQuads(d);
                max = Math.max(max, q);
                min = Math.min(min, q);
            }
        }
        assertEquals(SpiritDiveDjinnPieces.DJINN_QUADS_MAX, max, "the largest unique set of the Djinn Full Equip");
        assertEquals(SpiritDiveDjinnPieces.DJINN_QUADS_MIN, min, "the smallest unique set of the Djinn Full Equip");
    }

    private static int setVertices(Element e, boolean ch) throws IOException {
        int v = 0;
        for (Def d : SpiritDiveDjinnPieces.pieces(e, Stage.FULL, Grade.STANDARD, ch)) v += faces(geo(SpiritDiveDjinnPieces.modelId(e, ch, d.name()))) * 4 * d.passes();
        return v;
    }

    @Test
    void everySetStaysUnderTheDjinnFullEquipsVertexBudget() throws IOException {
        List<String> over = new ArrayList<>();
        StringBuilder numbers = new StringBuilder();
        for (Element e : Element.values())
            for (boolean ch : new boolean[]{false, true}) {
                if (ch && SpiritDiveTier.gradeOfElement(e) == Grade.STANDARD) continue;
                int v = setVertices(e, ch), cap = SpiritDiveDjinnPieces.pieceVertexCap(ch);
                numbers.append(e).append(ch ? "/character " : "/standard ").append(v).append(" of ").append(cap).append("; ");
                if (v > cap) over.add(e + (ch ? "/character " : "/standard ") + v + " > " + cap);
                int total = SpiritDiveDjinnPieces.worstVertices(ch, v);
                int djinn = (ch ? SpiritDiveDjinnPieces.DJINN_QUADS_MAX : SpiritDiveDjinnPieces.DJINN_QUADS_MIN) * 4;
                if (total > djinn) over.add(e + (ch ? "/character" : "/standard") + " with the veins and the extras " + total + " > the Djinn's " + djinn);
            }
        assertTrue(over.isEmpty(), "over the Djinn's budget: " + over + " (" + numbers + ")");
    }

    @Test
    void theStandardSetIsTheLighterOneAndTheCharacterSetTheRicherOne() throws IOException {
        for (Element e : new Element[]{Element.WIND, Element.FIRE, Element.WATER}) {
            assertTrue(setVertices(e, false) < setVertices(e, true), e + ": the standard set must be cheaper than the character set");
            int fs = 0, fc = 0;
            for (Def d : SpiritDiveDjinnPieces.pieces(e, Stage.FULL, Grade.STANDARD, false)) fs += faces(geo(SpiritDiveDjinnPieces.modelId(e, false, d.name())));
            for (Def d : SpiritDiveDjinnPieces.pieces(e, Stage.FULL, Grade.STANDARD, true)) fc += faces(geo(SpiritDiveDjinnPieces.modelId(e, true, d.name())));
            assertTrue(fc > fs, e + ": more plates in the character set");
        }
    }

    @Test
    void theCharacterSetsPaintMoreTexelsThanTheDjinnsWholeAtlasAndTheStandardSetsAreNotBlank() throws IOException {
        // the Djinn atlas is one 1024 x 1024 sheet for a whole set (5.4 to 7.3 texels per model pixel); the character sets are painted at 10 texels a pixel, the standard sets at 7
        for (String set : sets()) {
            long painted = 0;
            for (String[] p : allPieces()) {
                if (!p[0].equals(set)) continue;
                BufferedImage img = ImageIO.read(ASSETS.resolve("textures/entity/" + p[2] + ".png").toFile());
                painted += (long) img.getWidth() * img.getHeight();
            }
            if (set.endsWith("_c")) assertTrue(painted >= 1024L * 1024L, set + " paints " + painted + " texels, fewer than the Djinn's 1024 x 1024 atlas");
            else assertTrue(painted >= 250_000L, set + " paints only " + painted + " texels");
        }
    }
}
