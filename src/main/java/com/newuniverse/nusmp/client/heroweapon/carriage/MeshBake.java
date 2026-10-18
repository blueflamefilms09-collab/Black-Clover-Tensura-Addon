package com.newuniverse.nusmp.client.heroweapon.carriage;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the carriage you ride is drawn from the SAME mesh text file as the small carriage in the hand, and reading that text and putting it into the entity's frame
 * is arithmetic on numbers, so it is kept here without any game class: a test can feed it a small mesh and also the real shipped mesh, and check that the carriage
 * comes out the right way round, the right size and with benches under its seats. The text format is the weapon renderer's (its parser is private to its
 * package): a {@code glow RRGGBB speed min max} line, {@code q layer 4x(x y z u v) nx ny nz} quads with one normal and {@code qn layer 4x(x y z u v nx ny nz)} quads
 * with a normal per vertex; layer 0 is the body, layer 2 the glow, layer 1 (the demon swords' void) and every other line (the placement of the hand-held model)
 * are ignored. The quads are mapped with {@link MeshFrame} and scaled so that the carriage is the wanted length. {@link #fallback} is a plain wooden wagon built of
 * boxes for when no mesh can be read.
 */
public final class MeshBake {
    private MeshBake() {}

    /** The carriage baked in the entity's frame: per vertex {x, y, z, u, v, nx, ny, nz} (u and v in 0..128), the body and the glow, and the glow's pulse. */
    public static final class Baked {
        public final List<float[]> body = new ArrayList<>(), glow = new ArrayList<>();
        public int glowRgb = 0xFFC247;
        public float glowSpeed = 0.1f, glowMin = 0.6f, glowMax = 1.0f;
        public boolean fallback;
    }

    /**
     * Reads the mesh lines and bakes the quads into the entity's frame, {@code length} blocks long. Returns null when the text holds no body quad or its body has
     * no volume. A broken number or a short line throws a {@code RuntimeException}; the caller treats that like a missing mesh.
     */
    public static Baked bake(Iterable<String> lines, double length) {
        List<float[]> body = new ArrayList<>(), glow = new ArrayList<>();
        Baked out = new Baked();
        for (String line : lines) parse(line.trim().split("\\s+"), body, glow, out);
        if (body.isEmpty()) return null;
        MeshFrame.Box box = boxOf(body);
        if (!box.valid()) return null;
        double scale = MeshFrame.scaleFor(box, length);
        for (float[] v : body) out.body.add(convert(v, box, scale));
        for (float[] v : glow) out.glow.add(convert(v, box, scale));
        return out;
    }

    /** The bounding box of the quads' positions, in mesh space. */
    public static MeshFrame.Box boxOf(List<float[]> quads) {
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (float[] v : quads) {
            minX = Math.min(minX, v[0]);
            maxX = Math.max(maxX, v[0]);
            minY = Math.min(minY, v[1]);
            maxY = Math.max(maxY, v[1]);
            minZ = Math.min(minZ, v[2]);
            maxZ = Math.max(maxZ, v[2]);
        }
        return new MeshFrame.Box(minX, maxX, minY, maxY, minZ, maxZ);
    }

    private static void parse(String[] v, List<float[]> body, List<float[]> glow, Baked out) {
        switch (v[0]) {
            case "glow" -> {
                out.glowRgb = Integer.parseInt(v[1], 16);
                out.glowSpeed = Float.parseFloat(v[2]);
                out.glowMin = Float.parseFloat(v[3]);
                out.glowMax = Float.parseFloat(v[4]);
            }
            case "q" -> {
                List<float[]> into = layer(Integer.parseInt(v[1]), body, glow);
                float nx = Float.parseFloat(v[22]), ny = Float.parseFloat(v[23]), nz = Float.parseFloat(v[24]);
                for (int k = 0; k < 4; k++) {
                    int o = 2 + k * 5;
                    float[] vertex = {Float.parseFloat(v[o]), Float.parseFloat(v[o + 1]), Float.parseFloat(v[o + 2]), Float.parseFloat(v[o + 3]), Float.parseFloat(v[o + 4]), nx, ny, nz};
                    if (into != null) into.add(vertex);
                }
            }
            case "qn" -> {
                List<float[]> into = layer(Integer.parseInt(v[1]), body, glow);
                for (int k = 0; k < 4; k++) {
                    int o = 2 + k * 8;
                    float[] vertex = {Float.parseFloat(v[o]), Float.parseFloat(v[o + 1]), Float.parseFloat(v[o + 2]), Float.parseFloat(v[o + 3]), Float.parseFloat(v[o + 4]),
                            Float.parseFloat(v[o + 5]), Float.parseFloat(v[o + 6]), Float.parseFloat(v[o + 7])};
                    if (into != null) into.add(vertex);
                }
            }
            default -> { }
        }
    }

    /** Layer 0 is the body, layer 2 the glow; layer 1 is the demon swords' void and has no place on a carriage. */
    private static List<float[]> layer(int index, List<float[]> body, List<float[]> glow) { return index == 0 ? body : index == 2 ? glow : null; }

    private static float[] convert(float[] v, MeshFrame.Box box, double scale) {
        double[] p = MeshFrame.point(v[0], v[1], v[2], box, scale);
        double[] n = MeshFrame.normal(v[5], v[6], v[7]);
        return new float[]{(float) p[0], (float) p[1], (float) p[2], v[3], v[4], (float) n[0], (float) n[1], (float) n[2]};
    }

    // ================================================================ the plain wagon
    /** A wooden wagon of boxes in the entity's frame (x to the left, y up, z forward), about the size of the real one, used only when the mesh cannot be read. */
    public static Baked fallback() {
        Baked out = new Baked();
        out.fallback = true;
        box(out.body, -1.0f, 0.25f, -1.5f, 1.0f, 0.55f, 1.5f);                  // the floor
        box(out.body, -0.8f, 0.55f, -1.45f, 0.8f, 1.3f, -1.3f);                 // the back of the rear bench
        box(out.body, -0.7f, 0.55f, 0.35f, 0.7f, 0.85f, 0.75f);                 // the driver's bench
        box(out.body, -1.05f, 2.1f, -1.6f, 1.05f, 2.2f, 0.3f);                  // the roof
        box(out.body, -1.0f, 0.55f, -1.5f, -0.9f, 2.1f, -1.4f);                 // the four posts
        box(out.body, 0.9f, 0.55f, -1.5f, 1.0f, 2.1f, -1.4f);
        box(out.body, -1.0f, 0.55f, 0.2f, -0.9f, 2.1f, 0.3f);
        box(out.body, 0.9f, 0.55f, 0.2f, 1.0f, 2.1f, 0.3f);
        box(out.body, 0.98f, 0.0f, -1.3f, 1.1f, 0.8f, -0.5f);                   // the four wheels (inside the 2.2 block hitbox)
        box(out.body, -1.1f, 0.0f, -1.3f, -0.98f, 0.8f, -0.5f);
        box(out.body, 0.98f, 0.0f, 0.5f, 1.1f, 0.8f, 1.3f);
        box(out.body, -1.1f, 0.0f, 0.5f, -0.98f, 0.8f, 1.3f);
        return out;
    }

    private static void box(List<float[]> out, float x0, float y0, float z0, float x1, float y1, float z1) {
        quad(out, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, 0, 0, -1);
        quad(out, x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1, 0, 0, 1);
        quad(out, x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, -1, 0, 0);
        quad(out, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, 1, 0, 0);
        quad(out, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, 0, -1, 0);
        quad(out, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0, 1, 0);
    }

    private static void quad(List<float[]> out, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz,
                             float nx, float ny, float nz) {
        out.add(new float[]{ax, ay, az, 0f, 128f, nx, ny, nz});
        out.add(new float[]{bx, by, bz, 128f, 128f, nx, ny, nz});
        out.add(new float[]{cx, cy, cz, 128f, 0f, nx, ny, nz});
        out.add(new float[]{dx, dy, dz, 0f, 0f, nx, ny, nz});
    }
}
