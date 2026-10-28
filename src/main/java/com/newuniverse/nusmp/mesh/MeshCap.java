package com.newuniverse.nusmp.mesh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 3.32.0: the fixed detail limit of the big meshes, applied by the jar task (build.gradle, {@code -PmeshQuadCap}, default 3000 quads = 6000 triangles, 0 = off).
 * A mesh with more quads than the limit loses its smallest quads (by surface area) until it fits; the source files are never touched. The smallest half of a
 * heavy mesh holds only a few percent of its surface (engraving, scales, rivets), which is the detail the level-of-detail copies already leave out.
 *
 * <p>What is never dropped, so nothing the game measures changes: the quads that reach the lowest and highest x, y and z of each layer (the length of the weapon,
 * its grip and its scale come from those), every quad of the void and halo-card layers (1 and 4) and, in a mesh with {@code orbit} lines, the orbit layers (3 and up).
 * Everything else, including the line order and all header lines, is kept; a quad is a line {@code qn layer (x y z u v nx ny nz)*4} or {@code q layer (x y z u v)*4 nx ny nz}.
 * No Minecraft classes.
 */
public final class MeshCap {
    private MeshCap() {}

    /** What a cut did to one mesh. */
    public record Result(String text, int quadsBefore, int quadsAfter, double areaBefore, double areaAfter) {
        public boolean changed() { return quadsAfter != quadsBefore; }

        /** Share of the cappable surface that was removed. */
        public double areaLost() { return areaBefore <= 0 ? 0 : 1 - areaAfter / areaBefore; }
    }

    private static final class Q {
        int line, layer;
        double area;
        boolean keep;
    }

    public static Result cap(String text, int maxQuads) {
        if (maxQuads <= 0) return new Result(text, count(text), count(text), 0, 0);
        List<String> lines = new ArrayList<>(Arrays.asList(text.split("\n", -1)));
        boolean orbit = false;
        List<Q> quads = new ArrayList<>();
        double[][] lo = new double[8][], hi = new double[8][];
        int[][] loAt = new int[8][], hiAt = new int[8][];
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("orbit ")) orbit = true;
            if (!(line.startsWith("qn ") || line.startsWith("q "))) continue;
            String[] t = line.trim().split("\\s+");
            boolean qn = t[0].equals("qn");
            if (t.length != (qn ? 34 : 25)) continue;
            int layer;
            double[][] p = new double[4][3];
            try {
                layer = Integer.parseInt(t[1]);
                int stride = qn ? 8 : 5;
                for (int k = 0; k < 4; k++)
                    for (int a = 0; a < 3; a++) p[k][a] = Double.parseDouble(t[2 + k * stride + a]);
            } catch (NumberFormatException e) {
                continue;
            }
            Q q = new Q();
            q.line = i;
            q.layer = layer;
            q.area = tri(p[0], p[1], p[2]) + tri(p[0], p[2], p[3]);
            quads.add(q);
            int l = Math.min(layer, 7);
            if (lo[l] == null) {
                lo[l] = new double[]{1e30, 1e30, 1e30};
                hi[l] = new double[]{-1e30, -1e30, -1e30};
                loAt[l] = new int[3];
                hiAt[l] = new int[3];
            }
            int at = quads.size() - 1;
            for (int a = 0; a < 3; a++)
                for (int k = 0; k < 4; k++) {
                    if (p[k][a] < lo[l][a]) { lo[l][a] = p[k][a]; loAt[l][a] = at; }
                    if (p[k][a] > hi[l][a]) { hi[l][a] = p[k][a]; hiAt[l][a] = at; }
                }
        }
        int total = quads.size();
        double areaBefore = 0, areaAfter = 0;
        List<Q> droppable = new ArrayList<>();
        for (Q q : quads) {
            boolean cappable = q.layer == 0 || q.layer == 2 || (q.layer == 3 && !orbit);
            if (cappable) areaBefore += q.area;
            else q.keep = true;
        }
        for (int l = 0; l < 8; l++) {
            if (lo[l] == null) continue;
            for (int a = 0; a < 3; a++) {
                quads.get(loAt[l][a]).keep = true;
                quads.get(hiAt[l][a]).keep = true;
            }
        }
        for (Q q : quads) if (!q.keep) droppable.add(q);
        int drop = Math.min(Math.max(0, total - maxQuads), droppable.size());
        if (drop == 0) return new Result(text, total, total, areaBefore, areaBefore);
        droppable.sort(Comparator.<Q>comparingDouble(q -> q.area).thenComparing(q -> -q.line));
        boolean[] gone = new boolean[lines.size()];
        for (int i = 0; i < drop; i++) gone[droppable.get(i).line] = true;
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < lines.size(); i++) {
            if (gone[i]) continue;
            out.append(lines.get(i));
            if (i < lines.size() - 1) out.append('\n');
        }
        for (Q q : quads) if (!gone[q.line] && (q.layer == 0 || q.layer == 2 || (q.layer == 3 && !orbit))) areaAfter += q.area;
        return new Result(out.toString(), total, total - drop, areaBefore, areaAfter);
    }

    /** Number of quad lines of the two dialects. */
    public static int count(String text) {
        int n = 0;
        for (String line : text.split("\n")) {
            if (!(line.startsWith("qn ") || line.startsWith("q "))) continue;
            int tokens = line.trim().split("\\s+").length;
            if (tokens == (line.startsWith("qn ") ? 34 : 25)) n++;
        }
        return n;
    }

    /** The lowest and highest x, y and z of the quads of each layer (layers 0 to 7, six numbers each, NaN for an empty layer): what a cut must not change. */
    public static double[] bounds(String text) {
        double[] r = new double[48];
        for (int l = 0; l < 8; l++) for (int a = 0; a < 3; a++) { r[l * 6 + a] = Double.POSITIVE_INFINITY; r[l * 6 + 3 + a] = Double.NEGATIVE_INFINITY; }
        for (String line : text.split("\n")) {
            if (!(line.startsWith("qn ") || line.startsWith("q "))) continue;
            String[] t = line.trim().split("\\s+");
            boolean qn = t[0].equals("qn");
            if (t.length != (qn ? 34 : 25)) continue;
            int l = Math.min(Integer.parseInt(t[1]), 7), stride = qn ? 8 : 5;
            for (int k = 0; k < 4; k++)
                for (int a = 0; a < 3; a++) {
                    double v = Double.parseDouble(t[2 + k * stride + a]);
                    r[l * 6 + a] = Math.min(r[l * 6 + a], v);
                    r[l * 6 + 3 + a] = Math.max(r[l * 6 + 3 + a], v);
                }
        }
        return r;
    }

    private static double tri(double[] a, double[] b, double[] c) {
        double ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2], vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
        double x = uy * vz - uz * vy, y = uz * vx - ux * vz, z = ux * vy - uy * vx;
        return 0.5 * Math.sqrt(x * x + y * y + z * z);
    }
}
