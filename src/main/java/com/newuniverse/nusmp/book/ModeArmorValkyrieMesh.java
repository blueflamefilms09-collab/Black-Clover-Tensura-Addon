package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.Bone;
import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.Piece;
import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.Tex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Mode Armour, VALKYRIE DRESS v2: the static meshes, as plain arrays, parsed and baked ONCE per piece and level of detail from {@code assets/nusmp/valkyrie_meshes/*.txt} (the files of
 * {@code tools/gen_valkyrie_armor_v2.py} and {@code tools/gen_valkyrie_lance.py}; format {@code q <layer> (x y z u v) x4 nx ny nz r g b}, px, the left copy of a paired piece). Pure and server safe: no game
 * class, so the budgets, the self-test, the unit tests and the headless preview count and draw the real quads.
 *
 * <p>Baking turns the preview's player space (px, y up, feet at 0, front +z) into the space of the player-model part the piece is fitted to (px, y down, front -z, origin at the part's pivot; the
 * vanilla {@code ModelPart#translateAndRotate} then puts it on the body), builds the mirrored copy of a paired piece, remaps every uv into the 256 x 256 atlas, picks the toon tone of the flat layers
 * and the colour of the textured ones from the quad's normal against a fixed light, and sorts the quads into four passes: OPAQUE (plates and alpha-tested cards, the atlas), SCALES (the cards of the
 * mermaid tail, which repeat their sheet), TRANSLUCENT (veils and glass) and ADDITIVE (rims, veins, halo). A wing vertex keeps its weight (distance from the hinge over the wing length) so the painter can
 * flutter it ({@link Wings}).</p>
 */
public final class ModeArmorValkyrieMesh {
    public static final int OPAQUE = 0, SCALES = 1, TRANSLUCENT = 2, ADDITIVE = 3, PASSES = 4;

    public final int quads, verts;
    /** 3 floats a vertex: part-local px. */
    public final float[] pos;
    /** 2 floats a vertex: atlas uv (the repeating scale sheet keeps its own uv). */
    public final float[] uv;
    /** 3 floats a vertex: the normal in part space. */
    public final float[] nrm;
    /** A vertex's baked colour, ARGB (the painter scales the alpha by the wearer's fade). */
    public final int[] argb;
    /** The first vertex of each pass (PASSES + 1 entries): pass p is [passFrom[p], passFrom[p + 1]). */
    public final int[] passFrom;
    /** Wing meshes only (null otherwise): the original uv (2 a vertex), the weight 0..1 and weight^1.4 of each vertex. */
    public final float[] wuv, w, w14;

    private ModeArmorValkyrieMesh(int quads, float[] pos, float[] uv, float[] nrm, int[] argb, int[] passFrom, float[] wuv, float[] w, float[] w14) {
        this.quads = quads;
        this.verts = quads * 4;
        this.pos = pos;
        this.uv = uv;
        this.nrm = nrm;
        this.argb = argb;
        this.passFrom = passFrom;
        this.wuv = wuv;
        this.w = w;
        this.w14 = w14;
    }

    /** Quads of one pass. */
    public int passQuads(int pass) { return (passFrom[pass + 1] - passFrom[pass]) / 4; }

    // ================================================================ the table of sines (the painter's trig, no Math.sin per vertex)
    private static final int SIN_N = 4096;
    private static final float[] SIN = new float[SIN_N];
    private static final float TWO_PI = (float) (Math.PI * 2.0), DEG = (float) (Math.PI / 180.0), IDX = SIN_N / TWO_PI;

    static {
        for (int i = 0; i < SIN_N; i++) SIN[i] = (float) Math.sin(i * (Math.PI * 2.0) / SIN_N);
    }

    /** sin(x), x in radians, from a 4096 entry table (error below 0.002). */
    public static float sin(float x) { return SIN[(int) (x * IDX + 0.5f) & (SIN_N - 1)]; }

    public static float cos(float x) { return SIN[((int) (x * IDX + 0.5f) + SIN_N / 4) & (SIN_N - 1)]; }

    // ================================================================ the registry
    private static final ModeArmorValkyrieMesh[][][] ARMOUR = new ModeArmorValkyrieMesh[Piece.values().length][3][2];
    private static final ModeArmorValkyrieMesh[] LANCE = new ModeArmorValkyrieMesh[3];
    private static final ModeArmorValkyrieMesh EMPTY = bake(List.of(), null, false, false);
    private static final String FOLDER = "valkyrie_meshes";

    /**
     * The baked mesh of a piece at a level (0 NEAR, 1 MID, 2 FAR); {@code right} = the mirrored copy of a paired piece (the second copy of a piece with two). The empty mesh (nothing is drawn, the
     * self-test reports it) when the file is missing. Loaded on first use; the arrays are shared by every wearer and never copied.
     */
    public static ModeArmorValkyrieMesh armour(Piece p, int level, boolean right) {
        ModeArmorValkyrieMesh m = ARMOUR[p.ordinal()][level][right ? 1 : 0];
        return m != null ? m : loadArmour(p, level, right);
    }

    private static synchronized ModeArmorValkyrieMesh loadArmour(Piece p, int level, boolean right) {
        ModeArmorValkyrieMesh m = ARMOUR[p.ordinal()][level][right ? 1 : 0];
        if (m != null) return m;
        List<String> lines = read(p.file(level));
        m = lines == null ? EMPTY : bake(lines, p, right, false);
        ARMOUR[p.ordinal()][level][right ? 1 : 0] = m;
        return m;
    }

    /** The baked drill lance AB at a level, in the weapon frame (+y along the lance, the butt at 0, x across, z through; px). */
    public static ModeArmorValkyrieMesh lance(int level) {
        ModeArmorValkyrieMesh m = LANCE[level];
        return m != null ? m : loadLance(level);
    }

    private static synchronized ModeArmorValkyrieMesh loadLance(int level) {
        ModeArmorValkyrieMesh m = LANCE[level];
        if (m != null) return m;
        List<String> lines = read(ModeArmorValkyrieRules.lanceFile(level));
        m = lines == null ? EMPTY : bake(lines, null, false, true);
        LANCE[level] = m;
        return m;
    }

    /** True when the file of that name (without folder or extension) can be read. */
    public static boolean exists(String file) { return read(file) != null; }

    /** The raw lines of a mesh file (the tests count the file's own quads), or null. */
    public static List<String> lines(String file) { return read(file); }

    private static List<String> read(String file) {
        try (InputStream in = ModeArmorValkyrieMesh.class.getResourceAsStream("/assets/nusmp/" + FOLDER + "/" + file + ".txt")) {
            if (in != null) {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) { return r.lines().toList(); }
            }
        } catch (IOException ignored) { }
        try {
            Path p = Path.of("src", "main", "resources", "assets", "nusmp", FOLDER, file + ".txt");
            if (Files.isRegularFile(p)) return Files.readAllLines(p, StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException ignored) { }
        return null;
    }

    // ================================================================ the colours (the toon palettes of tools/vfx_preview/valkyrie_v2_preview.py)
    private static final int[][] PLATE = {{88, 160, 214}, {150, 214, 242}, {236, 252, 255}}, GOLD = {{176, 138, 56}, {226, 188, 92}, {255, 236, 160}};
    private static final int[][] FROST = {{150, 190, 225}, {222, 242, 255}, {255, 255, 255}}, DEEP = {{26, 74, 156}, {44, 116, 206}, {110, 178, 240}};
    private static final int[][] SCALE = {{40, 112, 180}, {76, 170, 226}, {176, 230, 252}}, GLASS = {{50, 128, 215}, {110, 190, 245}, {214, 244, 255}};
    private static final float[] LIGHT = {0.9f, 1.0f, 1.08f};
    /** The fixed light of the baked tones in the preview's space: from the wearer's right, above and a little in front (the preview camera's light). */
    private static final float LX = -0.5f, LY = 0.88f, LZ = 0.27f;
    private static final int GLOW_RGB = 0xCDF6FF;
    /** Alpha of the translucent veil, the glass and the additive glow layer (the preview's fresnel is view dependent, the game's is fixed). */
    private static final int VEIL_ALPHA = 235, GLASS_ALPHA = 209, GLOW_ALPHA = 204, EMISSIVE_ALPHA = 217;

    private static int tone(float nx, float ny, float nz) {
        float hl = 0.5f + 0.5f * (nx * LX + ny * LY + nz * LZ);
        return hl > 0.86f ? 2 : hl > 0.55f ? 1 : 0;
    }

    private static int pack(int a, int r, int g, int b) { return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | (b & 255); }

    private static int clamp255(float v) { return v <= 0f ? 0 : v >= 255f ? 255 : (int) (v + 0.5f); }

    // ================================================================ baking
    /**
     * Parses and bakes a mesh file. {@code piece} null = the lance (weapon frame: positions stay as written); {@code lance} marks it. A paired piece is built for the left copy ({@code right} false) or its
     * mirror ({@code right} true: x negated, quads walked backwards, normals mirrored, the part pivot on the other side).
     */
    public static ModeArmorValkyrieMesh bake(List<String> lines, Piece piece, boolean right, boolean lance) {
        int cap = 0;
        for (String l : lines) if (l.startsWith("q ")) cap++;
        float[][] pos = new float[PASSES][], uv = new float[PASSES][], nrm = new float[PASSES][], wuv = new float[PASSES][], w = new float[PASSES][];
        int[][] col = new int[PASSES][];
        int[] n = new int[PASSES];
        for (int p = 0; p < PASSES; p++) {
            pos[p] = new float[cap * 12];
            uv[p] = new float[cap * 8];
            nrm[p] = new float[cap * 12];
            wuv[p] = new float[cap * 8];
            w[p] = new float[cap * 4];
            col[p] = new int[cap * 4];
        }
        Bone bone = piece == null ? null : piece.bone;
        boolean wing = bone != null && bone.wing;
        float[] hinge = wing ? ModeArmorValkyrieRules.hinge(bone) : null;
        float side = right ? -1f : 1f;
        float[] x = new float[4], y = new float[4], z = new float[4], u = new float[4], v = new float[4];
        for (String line : lines) {
            if (!line.startsWith("q ")) continue;
            String[] t = line.trim().split("\\s+");
            if (t.length != 28) continue;
            int layer;
            float nx, ny, nz;
            float[] rgb = new float[3];
            try {
                layer = Integer.parseInt(t[1]);
                if (layer < 0 || layer >= ModeArmorValkyrieRules.LAYERS) continue;
                for (int k = 0; k < 4; k++) {
                    x[k] = Float.parseFloat(t[2 + k * 5]);
                    y[k] = Float.parseFloat(t[3 + k * 5]);
                    z[k] = Float.parseFloat(t[4 + k * 5]);
                    u[k] = Float.parseFloat(t[5 + k * 5]);
                    v[k] = Float.parseFloat(t[6 + k * 5]);
                }
                nx = Float.parseFloat(t[22]);
                ny = Float.parseFloat(t[23]);
                nz = Float.parseFloat(t[24]);
                for (int k = 0; k < 3; k++) rgb[k] = Float.parseFloat(t[25 + k]) / 255f;
            } catch (NumberFormatException e) {
                continue;
            }
            if (right) {                                                       // the mirror: x negated, the quad walked backwards (0, 3, 2, 1)
                for (int k = 0; k < 4; k++) x[k] = -x[k];
                nx = -nx;
                swap(x, 1, 3);
                swap(y, 1, 3);
                swap(z, 1, 3);
                swap(u, 1, 3);
                swap(v, 1, 3);
            }
            int tn = tone(nx, ny, nz);
            Tex tex = piece == null ? lanceTex(layer) : piece.texOf(layer);
            int pass;
            int[] c4 = new int[4];
            switch (layer) {
                case ModeArmorValkyrieRules.L_PLATE -> {
                    int[][] pal = rgb[2] < 0.55f ? GOLD : PLATE;
                    pass = OPAQUE;
                    int k0 = clamp255(pal[tn][0] * (0.7f + 0.3f * rgb[0])), k1 = clamp255(pal[tn][1] * (0.7f + 0.3f * rgb[1])), k2 = clamp255(pal[tn][2] * (0.7f + 0.3f * rgb[2]));
                    java.util.Arrays.fill(c4, pack(255, k0, k1, k2));
                }
                case ModeArmorValkyrieRules.L_EMISSIVE -> {
                    pass = ADDITIVE;
                    java.util.Arrays.fill(c4, EMISSIVE_ALPHA << 24 | GLOW_RGB);
                }
                case ModeArmorValkyrieRules.L_CARD -> {
                    pass = tex == Tex.SCALES ? SCALES : OPAQUE;
                    java.util.Arrays.fill(c4, pack(255, clamp255(255f * rgb[0] * LIGHT[tn]), clamp255(255f * rgb[1] * LIGHT[tn]), clamp255(255f * rgb[2] * LIGHT[tn])));
                }
                case ModeArmorValkyrieRules.L_VEIL -> {
                    pass = TRANSLUCENT;
                    int g = clamp255(255f * LIGHT[tn]);
                    java.util.Arrays.fill(c4, pack(VEIL_ALPHA, g, g, g));
                }
                case ModeArmorValkyrieRules.L_GLOW -> {
                    pass = ADDITIVE;
                    java.util.Arrays.fill(c4, pack(GLOW_ALPHA, clamp255(255f * rgb[0]), clamp255(255f * rgb[1]), clamp255(255f * rgb[2])));
                }
                case ModeArmorValkyrieRules.L_FROST -> { pass = OPAQUE; java.util.Arrays.fill(c4, pack(255, FROST[tn][0], FROST[tn][1], FROST[tn][2])); }
                case ModeArmorValkyrieRules.L_DEEP -> { pass = OPAQUE; java.util.Arrays.fill(c4, pack(255, DEEP[tn][0], DEEP[tn][1], DEEP[tn][2])); }
                case ModeArmorValkyrieRules.L_SCALE -> { pass = OPAQUE; java.util.Arrays.fill(c4, pack(255, SCALE[tn][0], SCALE[tn][1], SCALE[tn][2])); }
                default -> { pass = TRANSLUCENT; java.util.Arrays.fill(c4, pack(GLASS_ALPHA, GLASS[tn][0], GLASS[tn][1], GLASS[tn][2])); }      // L_GLASS
            }
            boolean flat = tex == Tex.WHITE;
            int q = n[pass]++;
            for (int k = 0; k < 4; k++) {
                int i = q * 4 + k;
                float lx, ly, lz;
                if (bone == null) { lx = x[k]; ly = y[k]; lz = z[k]; }                                  // the lance: the weapon frame as written
                else {
                    lx = x[k] - side * bone.pivotX;                                                      // part space: x - pivot, down is +y, front is -z
                    ly = bone.pivotY - y[k];
                    lz = -z[k];
                }
                pos[pass][i * 3] = lx;
                pos[pass][i * 3 + 1] = ly;
                pos[pass][i * 3 + 2] = lz;
                nrm[pass][i * 3] = nx;
                nrm[pass][i * 3 + 1] = bone == null ? ny : -ny;
                nrm[pass][i * 3 + 2] = bone == null ? nz : -nz;
                col[pass][i] = c4[k];
                wuv[pass][i * 2] = u[k];
                wuv[pass][i * 2 + 1] = v[k];
                if (flat) { uv[pass][i * 2] = cellU(Tex.WHITE, 0.5f); uv[pass][i * 2 + 1] = cellV(Tex.WHITE, 0.5f); }
                else if (tex == Tex.SCALES) { uv[pass][i * 2] = u[k]; uv[pass][i * 2 + 1] = v[k]; }
                else { uv[pass][i * 2] = cellU(tex, u[k]); uv[pass][i * 2 + 1] = cellV(tex, v[k]); }
                if (wing) {
                    float hx = side * hinge[0], dx = x[k] - hx, dy = y[k] - hinge[1], dz = z[k] - hinge[2];
                    w[pass][i] = Math.min(1f, (float) Math.sqrt(dx * dx + dy * dy + dz * dz) / hinge[3]);
                }
            }
        }
        // concatenate the passes in order
        int total = 0;
        int[] from = new int[PASSES + 1];
        for (int p = 0; p < PASSES; p++) { from[p] = total * 4; total += n[p]; }
        from[PASSES] = total * 4;
        float[] P = new float[total * 12], UV = new float[total * 8], N = new float[total * 12], WU = wing ? new float[total * 8] : null, W = wing ? new float[total * 4] : null, W14 = wing ? new float[total * 4] : null;
        int[] C = new int[total * 4];
        for (int p = 0; p < PASSES; p++) {
            int o = from[p];
            System.arraycopy(pos[p], 0, P, o * 3, n[p] * 12);
            System.arraycopy(uv[p], 0, UV, o * 2, n[p] * 8);
            System.arraycopy(nrm[p], 0, N, o * 3, n[p] * 12);
            System.arraycopy(col[p], 0, C, o, n[p] * 4);
            if (wing) {
                System.arraycopy(wuv[p], 0, WU, o * 2, n[p] * 8);
                System.arraycopy(w[p], 0, W, o, n[p] * 4);
            }
        }
        if (wing) for (int i = 0; i < W.length; i++) W14[i] = (float) Math.pow(W[i], 1.4);
        return new ModeArmorValkyrieMesh(total, P, UV, N, C, from, WU, W, W14);
    }

    /** The lance's sheets: the foam cards (layer 3), the veil of the ribbons (4) and the glow (5); its flat plates use the white cell. */
    private static Tex lanceTex(int layer) {
        return switch (layer) {
            case ModeArmorValkyrieRules.L_CARD -> Tex.FOAM;
            case ModeArmorValkyrieRules.L_VEIL -> Tex.VEIL;
            case ModeArmorValkyrieRules.L_GLOW -> Tex.GLOW;
            default -> Tex.WHITE;
        };
    }

    private static void swap(float[] a, int i, int j) { float t = a[i]; a[i] = a[j]; a[j] = t; }

    /** U of a point at {@code u} (0..1) across a cell of the atlas: inset by half a texel so that nearest sampling never reads the next cell. */
    public static float cellU(Tex t, float u) { return (t.column() * Tex.CELL + 0.5f + Math.max(0f, Math.min(1f, u)) * (Tex.CELL - 1f)) / Tex.ATLAS_SIZE; }

    public static float cellV(Tex t, float v) { return (t.row() * Tex.CELL + 0.5f + Math.max(0f, Math.min(1f, v)) * (Tex.CELL - 1f)) / Tex.ATLAS_SIZE; }

    // ================================================================ the wing flutter
    /**
     * The per-vertex motion of the wing pieces (the rule of docs/vfx_remake/valkyrie_wings.md section 5, written in part space): each vertex turns about the hinge by {@code flap + bend * w^1.4} (about z)
     * and {@code sweep * w} (about x), then moves along z by {@code amp * w * sin(2 pi 1.3 v - phase) * (0.25 + 0.75 u)}, a wave travelling from the root to the tip, stronger at the trailing edge, and
     * along x by 0.4 of that. All state is static scratch of the render thread: nothing is allocated.
     */
    public static final class Wings {
        private Wings() {}

        private static float hx, hy, hz, side, scale, flap, bend, sweep, amp, phase;

        /**
         * Sets the flutter of one wing copy. {@code piece} is a wing piece, {@code right} the mirrored copy; {@code scale} 1 (main and saint wings) or -0.7 (the small lower pair beats the other way);
         * {@code grow} 0..1 folds the wing in while the armour grows.
         */
        public static void set(Piece piece, boolean right, float flapDeg, float bendDeg, float sweepDeg, float amplitude, float wavePhase, float grow) {
            float[] h = ModeArmorValkyrieRules.hinge(piece.bone);
            side = right ? -1f : 1f;
            hx = side * h[0];
            hy = piece.bone.pivotY - h[1];
            hz = -h[2];
            scale = piece.bone == Bone.WING_LOW ? -0.7f : 1f;
            flap = flapDeg;
            bend = bendDeg;
            sweep = sweepDeg;
            amp = amplitude * grow;
            phase = wavePhase;
        }

        /** Writes the flown position of vertex {@code i} of a wing mesh into {@code out[0..2]}. */
        public static void apply(ModeArmorValkyrieMesh m, int i, float[] out) {
            float x = m.pos[i * 3], y = m.pos[i * 3 + 1], z = m.pos[i * 3 + 2];
            float wt = m.w[i], u = m.wuv[i * 2], v = m.wuv[i * 2 + 1];
            float dx = x - hx, dy = y - hy, dz = z - hz;
            float a = -side * scale * (flap + bend * m.w14[i]) * DEG, ca = cos(a), sa = sin(a);
            float dx2 = dx * ca + dy * sa, dy2 = -dx * sa + dy * ca;
            float s = sweep * wt * scale * DEG, cs = cos(s), ss = sin(s);
            float dy3 = dy2 * cs - dz * ss, dz3 = dy2 * ss + dz * cs;
            float zw = amp * wt * sin(TWO_PI * 1.3f * v - phase) * (0.25f + 0.75f * u);
            float xw = 0.4f * amp * wt * sin(TWO_PI * 1.1f * v - phase + 1.0f) * u * side;
            out[0] = hx + dx2 + xw;
            out[1] = hy + dy3;
            out[2] = hz + dz3 - zw;
        }
    }

    // ================================================================ counting helpers (tests, budgets, self-test)
    /** Counts the {@code q} lines of a mesh file. */
    public static int countQuads(List<String> lines) {
        int n = 0;
        for (String l : lines) if (l.startsWith("q ")) n++;
        return n;
    }

    /** Every mesh file the armour and the lance need (all pieces, all levels, the lance's three levels). */
    public static List<String> allFiles() {
        List<String> out = new ArrayList<>();
        for (Piece p : Piece.values()) for (int l = 0; l < 3; l++) out.add(p.file(l));
        for (int l = 0; l < 3; l++) out.add(ModeArmorValkyrieRules.lanceFile(l));
        return out;
    }
}
