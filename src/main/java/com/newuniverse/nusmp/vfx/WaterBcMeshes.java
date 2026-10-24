package com.newuniverse.nusmp.vfx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * The 3D cores of the water hybrid as plain arrays, parsed once from {@code assets/nusmp/wbc_meshes/bc_<piece>[_lo].txt} (written by {@code tools/gen_water_bc_meshes.py}, the mesh pipeline of the weapon
 * models; the file format is the one of {@code SpiritDiveWind3Mesh}: {@code q <layer> (x y z u v) x4 nx ny nz r g b}, units 1/16 block, y along the piece). Pure and server safe: no Minecraft class, so
 * the budgets, the self-test and the tests count the real quads. Shared instances: every effect reads the same arrays, nothing is copied per frame.
 *
 * <p>Layers: 0 the solid (or hull) body, 1 the emissive core (drawn additive), 2 foam. The tint of a quad is kept raw (0..1 per channel): the toon shading of {@link WaterBcToon} multiplies it into its palette.
 */
public final class WaterBcMeshes {
    private WaterBcMeshes() {}

    public static final int LAYERS = 3;

    /** One piece (one file). */
    public static final class Piece {
        public final String name;
        public final int quads;
        /** 12 floats a quad: x y z of the four vertices, in model pixels. */
        public final float[] pos;
        /** 3 floats a quad: the tint (r, g, b, 0..1). */
        public final float[] tint;
        /** the layer of each quad (0 body, 1 glow, 2 foam). */
        public final byte[] layer;
        public final int[] perLayer = new int[LAYERS];

        Piece(String name, int quads, float[] pos, float[] tint, byte[] layer) {
            this.name = name;
            this.quads = quads;
            this.pos = pos;
            this.tint = tint;
            this.layer = layer;
            for (byte l : layer) perLayer[l]++;
        }
    }

    private static final Piece EMPTY = parse("empty", List.of());
    /** [piece][lo]; read once on first use (the render thread, or the self-test thread), published through the volatile flag. */
    private static final Piece[][] CACHE = new Piece[WaterBc.PIECES.length][2];
    private static volatile boolean loaded;

    /** The piece with that id, in its light variant when {@code lo}; the empty piece (nothing is drawn, the self-test reports it) when the file is missing. */
    public static Piece get(int piece, boolean lo) {
        if (!loaded) load();
        return CACHE[piece][lo ? 1 : 0];
    }

    private static synchronized void load() {
        if (loaded) return;
        for (int p = 0; p < WaterBc.PIECES.length; p++) {
            for (int lo = 0; lo < 2; lo++) {
                String name = WaterBc.meshFile(p, lo == 1);
                List<String> lines = read(name);
                CACHE[p][lo] = lines == null ? EMPTY : parse(name, lines);
            }
        }
        loaded = true;
    }

    /** True when the file of that name (without folder or extension) can be read. */
    public static boolean exists(String file) { return read(file) != null; }

    private static List<String> read(String file) {
        try (InputStream in = WaterBcMeshes.class.getResourceAsStream("/assets/nusmp/" + WaterBc.MESH_FOLDER + "/" + file + ".txt")) {
            if (in != null) {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) { return r.lines().toList(); }
            }
        } catch (IOException ignored) { }
        try {
            Path p = Path.of("src", "main", "resources", "assets", "nusmp", WaterBc.MESH_FOLDER, file + ".txt");
            if (Files.isRegularFile(p)) return Files.readAllLines(p, StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException ignored) { }
        return null;
    }

    /** Parses the lines of a mesh file; a line that is not a well formed quad is skipped. */
    public static Piece parse(String name, List<String> lines) {
        float[] pos = new float[lines.size() * 12], tint = new float[lines.size() * 3];
        byte[] layer = new byte[lines.size()];
        int n = 0;
        for (String line : lines) {
            if (!line.startsWith("q ")) continue;
            String[] v = line.trim().split("\\s+");
            if (v.length != 2 + 20 + 3 + 3) continue;
            try {
                int l = Integer.parseInt(v[1]);
                if (l < 0 || l >= LAYERS) continue;
                float[] row = new float[26];
                for (int i = 0; i < 26; i++) row[i] = Float.parseFloat(v[2 + i]);
                for (int k = 0; k < 4; k++) {
                    pos[n * 12 + k * 3] = row[k * 5];
                    pos[n * 12 + k * 3 + 1] = row[k * 5 + 1];
                    pos[n * 12 + k * 3 + 2] = row[k * 5 + 2];
                }
                tint[n * 3] = row[23] / 255f;
                tint[n * 3 + 1] = row[24] / 255f;
                tint[n * 3 + 2] = row[25] / 255f;
                layer[n++] = (byte) l;
            } catch (NumberFormatException ignored) { }
        }
        return new Piece(name, n, java.util.Arrays.copyOf(pos, n * 12), java.util.Arrays.copyOf(tint, n * 3), java.util.Arrays.copyOf(layer, n));
    }
}
