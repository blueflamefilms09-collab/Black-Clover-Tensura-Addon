package com.newuniverse.nusmp.book;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spirit Dive WIND remake (the three selectable looks A, B, C): one real 3D piece (a sword, a bow, an arrow, a feather, a cube ...) as plain arrays, parsed from
 * {@code assets/nusmp/sdw3_meshes/<name>.txt} (written by {@code tools/gen_sdw3_models.py}, the mesh pipeline of the weapon models). Pure and server safe: it touches no Minecraft class, so the budgets
 * and the tests count the real quads. Shared instances: every diver draws the same arrays, nothing is copied per frame.
 *
 * <p>A mesh has three layers: 0 the solid body, 1 the soft glow shell (drawn additive), 2 rings and ornaments (solid, dropped at low detail and on the standard tier). Every quad carries its four
 * vertices, its normal and a tint; the body tint is already multiplied by a fixed face shade (so the solid piece reads as a 3D object although it is drawn full bright).</p>
 */
public final class SpiritDiveWind3Mesh {
    public static final int LAYERS = 3, SOLID = 0, GLOW = 1, ORNAMENT = 2;
    /** Every mesh of the look (the generator writes exactly these). */
    public static final List<String> NAMES = List.of("sword", "bow", "arrow", "spear", "blade", "feather", "cube", "panel", "plate", "crown");
    public static final String DIR = "/assets/nusmp/sdw3_meshes/";
    /** The mesh folder of the final Wind A (the 3D crown, {@code tools/gen_sdw4_models.py}); loaded by the same code through {@link #getIn}. */
    public static final String FOLDER3 = "sdw3_meshes", FOLDER4 = "sdw4_meshes";
    /** The shade of a face: the light comes from the front, a little from above and the side (mesh space: x across, y along the piece, z through it). */
    private static final float LX = 0.30f, LY = 0.35f, LZ = 0.89f;

    public final String name;
    /** Per layer: 12 floats a quad (x y z of four vertices), 8 floats a quad (u v of four vertices), 3 floats a quad (the normal), the packed tint (0xRRGGBB, shaded for layers 0 and 2) a quad. */
    public final float[][] pos = new float[LAYERS][], uv = new float[LAYERS][], nrm = new float[LAYERS][];
    public final int[][] tint = new int[LAYERS][];
    /** The furthest vertex from the mesh origin (px), for the bounds test. */
    public final float radius;

    private SpiritDiveWind3Mesh(String name, float[][] p, float[][] t, float[][] n, int[][] c) {
        this.name = name;
        float r = 0f;
        for (int l = 0; l < LAYERS; l++) {
            pos[l] = p[l]; uv[l] = t[l]; nrm[l] = n[l]; tint[l] = c[l];
            for (int i = 0; i + 2 < p[l].length; i += 3) r = Math.max(r, (float) Math.sqrt(p[l][i] * p[l][i] + p[l][i + 1] * p[l][i + 1] + p[l][i + 2] * p[l][i + 2]));
        }
        radius = r;
    }

    public int quads(int layer) { return tint[layer].length; }

    /** Vertices of one layer. */
    public int verts(int layer) { return tint[layer].length * 4; }

    /** Vertices of the layers whose bit is set in {@code mask}. */
    public int vertsOf(int mask) {
        int v = 0;
        for (int l = 0; l < LAYERS; l++) if ((mask >> l & 1) != 0) v += verts(l);
        return v;
    }

    public int totalVerts() { return verts(0) + verts(1) + verts(2); }

    // ------------------------------------------------------------------ loading
    private static final Map<String, SpiritDiveWind3Mesh> CACHE = new ConcurrentHashMap<>();
    private static final SpiritDiveWind3Mesh EMPTY = parse("empty", List.of());

    /** The mesh of that name (read once, shared); the empty mesh when the file is missing (nothing is drawn, the self-test reports it). */
    public static SpiritDiveWind3Mesh get(String name) { return getIn(FOLDER3, name); }

    /** The mesh of that name in a mesh folder ({@link #FOLDER3} or {@link #FOLDER4}), read once and shared. */
    public static SpiritDiveWind3Mesh getIn(String folder, String name) {
        String key = folder + "/" + name;
        SpiritDiveWind3Mesh m = CACHE.get(key);
        if (m != null) return m;
        List<String> lines = read(folder, name);
        m = lines == null ? EMPTY : parse(name, lines);
        CACHE.put(key, m);
        return m;
    }

    /** True when the file of that name could be read. */
    public static boolean exists(String name) { return read(FOLDER3, name) != null; }

    public static boolean existsIn(String folder, String name) { return read(folder, name) != null; }

    private static List<String> read(String folder, String name) {
        try (InputStream in = SpiritDiveWind3Mesh.class.getResourceAsStream("/assets/nusmp/" + folder + "/" + name + ".txt")) {
            if (in != null) {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) { return r.lines().toList(); }
            }
        } catch (IOException ignored) { }
        try {
            Path p = Path.of("src", "main", "resources", "assets", "nusmp", folder, name + ".txt");
            if (Files.isRegularFile(p)) return Files.readAllLines(p, StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException ignored) { }
        return null;
    }

    /** Parses the lines of a mesh file; a line that is not a well formed quad is skipped. */
    public static SpiritDiveWind3Mesh parse(String name, List<String> lines) {
        float[][] p = new float[LAYERS][], t = new float[LAYERS][], n = new float[LAYERS][];
        int[][] c = new int[LAYERS][];
        int[] count = new int[LAYERS];
        float[][] rows = new float[lines.size()][];
        int[] layerOf = new int[lines.size()];
        int rn = 0;
        for (String line : lines) {
            if (!line.startsWith("q ")) continue;
            String[] v = line.trim().split("\\s+");
            if (v.length != 2 + 20 + 3 + 3) continue;
            try {
                int layer = Integer.parseInt(v[1]);
                if (layer < 0 || layer >= LAYERS) continue;
                float[] row = new float[26];
                for (int i = 0; i < 26; i++) row[i] = Float.parseFloat(v[2 + i]);
                rows[rn] = row;
                layerOf[rn++] = layer;
                count[layer]++;
            } catch (NumberFormatException ignored) { }
        }
        for (int l = 0; l < LAYERS; l++) {
            p[l] = new float[count[l] * 12]; t[l] = new float[count[l] * 8]; n[l] = new float[count[l] * 3]; c[l] = new int[count[l]];
        }
        int[] at = new int[LAYERS];
        for (int r = 0; r < rn; r++) {
            int l = layerOf[r], q = at[l]++;
            float[] row = rows[r];
            for (int k = 0; k < 4; k++) {
                p[l][q * 12 + k * 3] = row[k * 5]; p[l][q * 12 + k * 3 + 1] = row[k * 5 + 1]; p[l][q * 12 + k * 3 + 2] = row[k * 5 + 2];
                t[l][q * 8 + k * 2] = row[k * 5 + 3]; t[l][q * 8 + k * 2 + 1] = row[k * 5 + 4];
            }
            float nx = row[20], ny = row[21], nz = row[22];
            n[l][q * 3] = nx; n[l][q * 3 + 1] = ny; n[l][q * 3 + 2] = nz;
            int rr = Math.round(row[23]), gg = Math.round(row[24]), bb = Math.round(row[25]);
            if (l != GLOW) {
                float d = nx * LX + ny * LY + nz * LZ;
                float shade = 0.84f + 0.16f * Math.max(0f, d) + 0.06f * Math.max(0f, -d);
                rr = Math.min(255, Math.round(rr * shade)); gg = Math.min(255, Math.round(gg * shade)); bb = Math.min(255, Math.round(bb * shade));
            }
            c[l][q] = (rr & 255) << 16 | (gg & 255) << 8 | (bb & 255);
        }
        return new SpiritDiveWind3Mesh(name, p, t, n, c);
    }
}
