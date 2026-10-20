package com.newuniverse.nusmp.requip;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Why: the owner asked for Erza's armours to match the Djinn armours in quality, detail and resolution, so the quality is a NUMBER that a test can fail on instead of an impression. This class measures an
 * armour from the files in the jar (the same numbers {@code tools/erza_armour_audit.py} prints for the Djinn and for Erza) and compares them with the {@link #BAR}, the minimum measured over the 36 unique Djinn
 * sets. It has no Minecraft classes: the JUnit audit reads the files from disk, the in-game self-test {@code skill.erza_requip2} from the jar, through the same {@link Source}.
 * <p>
 * What is measured: pieces (model files of the set); faces (the polygons the model draws: a box face that has a uv; the Djinn's quads are the same thing); glow faces (the polygons of pieces whose
 * {@code _glow} map has any painted texel, since the glow pass draws the model again); the texture area (the sum of the skins of all pieces, against the Djinn's 1024 x 1024 atlas); painted texels
 * (alpha above 0 in the skin) and the same for the glow maps; the palette (distinct 5-bit colours holding at least 0.1 percent of the painted texels of the whole set) and the glow palette; and the texel
 * density: texels per model pixel of surface (sqrt of uv area over face area), area-weighted median over all faces of the set.
 */
public final class ArmourQuality {
    private ArmourQuality() {}

    /** The Djinn minimum (tools/erza_armour_audit.py --write-bar writes src/test/resources/erza_armour_bar.json; the JUnit audit keeps this constant equal to it). */
    public record Bar(int pieces, int faces, int glowFaces, long paintedTexels, long glowPaintedTexels, int palette, int glowPalette, double density, int textureSide) {}

    public static final Bar BAR = new Bar(19, 2856, 440, 664603, 20976, 69, 17, 5.45, 1024);

    public record Metrics(int pieces, int cubes, int faces, int glowFaces, long textureTexels, long paintedTexels, long glowPaintedTexels, int palette, int glowPalette, double density,
                          int animatedPieces) {}

    /** Where the files come from: a folder on disk (the JUnit test) or the class loader (the in-game self-test). */
    public interface Source {
        InputStream open(String path) throws IOException;     // path like "assets/nusmp/geo/entity/x.geo.json"; null when the file does not exist
    }

    private static final String[] FACES = {"west", "north", "east", "south", "up", "down"};

    public static Metrics measure(Armour a, Source src) throws IOException {
        int pieces = 0, cubes = 0, faces = 0, glowFaces = 0, animated = 0;
        long texels = 0, painted = 0, glowPainted = 0;
        long[] pal = new long[32768], gpal = new long[32768];
        List<double[]> dens = new ArrayList<>();                      // {density, weight}
        for (ArmourSets.Piece p : ArmourSets.of(a)) {
            String id = ArmourSets.modelId(a, p);
            InputStream gi = src.open("assets/nusmp/geo/entity/" + id + ".geo.json");
            if (gi == null) throw new IOException("missing geo " + id);
            JsonObject geo;
            try (InputStreamReader r = new InputStreamReader(gi, StandardCharsets.UTF_8)) { geo = JsonParser.parseReader(r).getAsJsonObject(); }
            JsonObject g = geo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            int tw = g.getAsJsonObject("description").get("texture_width").getAsInt(), th = g.getAsJsonObject("description").get("texture_height").getAsInt();
            pieces++;
            texels += (long) tw * th;
            int pf = 0;
            for (JsonElement be : g.getAsJsonArray("bones")) {
                for (JsonElement ce : be.getAsJsonObject().getAsJsonArray("cubes")) {
                    JsonObject c = ce.getAsJsonObject();
                    cubes++;
                    JsonArray size = c.getAsJsonArray("size");
                    double sx = size.get(0).getAsDouble(), sy = size.get(1).getAsDouble(), sz = size.get(2).getAsDouble();
                    JsonObject uv = c.get("uv").isJsonObject() ? c.getAsJsonObject("uv") : null;
                    if (uv == null) {
                        pf += 6;
                        continue;
                    }
                    for (String f : FACES) {
                        if (!uv.has(f)) continue;
                        JsonObject fd = uv.getAsJsonObject(f);
                        pf++;
                        JsonArray us = fd.getAsJsonArray("uv_size");
                        double ua = Math.abs(us.get(0).getAsDouble() * us.get(1).getAsDouble());
                        double wa = switch (f) { case "east", "west" -> sz * sy; case "north", "south" -> sx * sy; default -> sx * sz; };
                        if (wa > 1e-4) dens.add(new double[]{Math.sqrt(ua / wa), wa});
                    }
                }
            }
            faces += pf;
            long pp = paint(src, "assets/nusmp/textures/entity/" + id + ".png", pal);
            long gp = paint(src, "assets/nusmp/textures/entity/" + id + "_glow.png", gpal);
            painted += pp;
            glowPainted += gp;
            if (gp > 0) glowFaces += pf;
            if (p.clip() != null) animated++;
        }
        return new Metrics(pieces, cubes, faces, glowFaces, texels, painted, glowPainted, rich(pal, painted), rich(gpal, glowPainted), median(dens), animated);
    }

    private static long paint(Source src, String path, long[] pal) throws IOException {
        InputStream in = src.open(path);
        if (in == null) throw new IOException("missing texture " + path);
        BufferedImage im;
        try (in) { im = ImageIO.read(in); }
        if (im == null) throw new IOException("unreadable texture " + path);
        int w = im.getWidth(), h = im.getHeight();
        int[] px = im.getRGB(0, 0, w, h, null, 0, w);
        long n = 0;
        for (int argb : px) {
            if ((argb >>> 24) == 0) continue;
            n++;
            pal[(((argb >> 16) & 255) >> 3) << 10 | (((argb >> 8) & 255) >> 3) << 5 | ((argb & 255) >> 3)]++;
        }
        return n;
    }

    private static int rich(long[] pal, long painted) {
        long min = Math.max(1, (long) Math.ceil(painted * 0.001));
        int n = 0;
        for (long v : pal) if (v >= min) n++;
        return n;
    }

    private static double median(List<double[]> dens) {
        if (dens.isEmpty()) return 0;
        double[][] a = dens.toArray(new double[0][]);
        Arrays.sort(a, (x, y) -> Double.compare(x[0], y[0]));
        double total = 0;
        for (double[] d : a) total += d[1];
        double acc = 0;
        for (double[] d : a) {
            acc += d[1];
            if (acc >= total / 2.0) return d[0];
        }
        return a[a.length - 1][0];
    }

    /** What the armour falls short of the bar on, as readable lines (empty = it meets the Djinn standard). */
    public static List<String> violations(String id, Metrics m, Bar b) {
        List<String> v = new ArrayList<>();
        if (m.pieces < b.pieces()) v.add(id + ": " + m.pieces + " pieces, the Djinn minimum is " + b.pieces());
        if (m.faces < b.faces()) v.add(id + ": " + m.faces + " faces, the Djinn minimum is " + b.faces());
        if (m.glowFaces < b.glowFaces()) v.add(id + ": " + m.glowFaces + " glow faces, the Djinn minimum is " + b.glowFaces());
        if (m.paintedTexels < b.paintedTexels()) v.add(id + ": " + m.paintedTexels + " painted texels, the Djinn minimum is " + b.paintedTexels());
        if (m.glowPaintedTexels < b.glowPaintedTexels()) v.add(id + ": " + m.glowPaintedTexels + " glow texels, the Djinn minimum is " + b.glowPaintedTexels());
        if (m.palette < b.palette()) v.add(id + ": a palette of " + m.palette + " colours, the Djinn minimum is " + b.palette());
        if (m.glowPalette < b.glowPalette()) v.add(id + ": a glow palette of " + m.glowPalette + " colours, the Djinn minimum is " + b.glowPalette());
        if (m.density < b.density()) v.add(id + ": " + String.format("%.2f", m.density) + " texels per model pixel, the Djinn minimum is " + b.density());
        if (m.textureTexels < (long) b.textureSide() * b.textureSide())
            v.add(id + ": " + m.textureTexels + " texels of skin in all, the Djinn atlas has " + (long) b.textureSide() * b.textureSide());
        return v;
    }
}
