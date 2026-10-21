package com.newuniverse.nusmp.visual.grip;

import java.util.List;

/**
 * 1.111.0 (docs/weapon_grip.md): the grip of one loaded mesh and the shifts that put it into the hand, per display context. Pure (no Minecraft imports).
 * <p>
 * {@code ctx} indices: 0 third person right, 1 third person left, 2 first person right, 3 first person left (anything else has no grip shift: the ground, a frame, the slot).
 * {@link #shift} gives {sx, sy, sz, tx, ty, tz}: translate the item's unit cube by s after placing the mesh and the grip point lies at t, where t is the point of the cube
 * the hand holds ({@link GripMath#handPoint}) in third person, and the place a sword's hilt lies ({@link GripMath#firstPersonTarget}) in first person.
 */
public final class GripState {
    public static final int TP_RIGHT = 0, TP_LEFT = 1, FP_RIGHT = 2, FP_LEFT = 3;

    /** The grip point in mesh space (x, y, z). */
    public final float[] grip;
    public final GripFinder.Source source;
    public final float fraction;
    private final GripMath.Display[] display;
    private final float[] box;                                                  // {x0, x1, y0, y1, z0, z1} of the body in mesh space; null = no first-person view limit
    private final float[][] cache = new float[4][];

    public GripState(GripFinder.Grip g, GripMath.Display[] display) {
        this(g, display, null);
    }

    public GripState(GripFinder.Grip g, GripMath.Display[] display, float[] box) {
        this.grip = new float[]{g.x(), g.y(), g.z()};
        this.source = g.source();
        this.fraction = g.fraction();
        this.display = display;
        this.box = box;
    }

    /** The four hand display entries of a model json as a Display array (a missing entry is vanilla's NO_TRANSFORM). */
    public GripMath.Display display(int ctx) { return display == null || display[ctx] == null ? GripMath.Display.NONE : display[ctx]; }

    /** {sx, sy, sz, tx, ty, tz} for a context index 0..3 and the mesh's placement {ox, oy, angle, scale, ymin}; cached (the placement of a loaded mesh does not change). */
    public float[] shift(int ctx, float ox, float oy, float angle, float scale, float ymin) {
        float[] c = cache[ctx];
        if (c != null) return c;
        float[] place = {ox, oy, angle, scale, ymin};
        boolean left = ctx == TP_LEFT || ctx == FP_LEFT, fp = ctx >= FP_RIGHT;
        GripMath.Display d = display(ctx);
        float[] t = fp ? GripMath.firstPersonTarget(d, left) : GripMath.handPoint(d, left);
        float[] s = GripMath.shift(place, grip, t);
        if (fp) {                                                              // first person: as far as the weapon stays in view (display_presets.py's camera bounds)
            float k = GripMath.firstPersonShare(place, box, s, d, left);
            s = new float[]{s[0] * k, s[1] * k, s[2] * k};
            float[] g = GripMath.cubePoint(place[0], place[1], place[2], place[3], place[4], grip[0], grip[1], grip[2]);
            t = new float[]{g[0] + s[0], g[1] + s[1], g[2] + s[2]};            // where the grip actually lies: the pivot of the oversize shrink
        }
        c = new float[]{s[0], s[1], s[2], t[0], t[1], t[2]};
        cache[ctx] = c;
        return c;
    }

    /** Packs the vertices of a layer ({x y z u v nx ny nz} each) into three floats per vertex for {@link GripFinder}. */
    public static float[] pack(List<float[]> verts) {
        float[] out = new float[verts.size() * 3];
        int i = 0;
        for (float[] v : verts) {
            out[i++] = v[0];
            out[i++] = v[1];
            out[i++] = v[2];
        }
        return out;
    }

    /** The body quads of an upgraded mesh as {x y z} vertices: only the static group "-" ({@code staticOnly}), or every group. */
    public static List<float[]> vuBody(com.newuniverse.nusmp.visual.genesis.VuMesh mesh, boolean staticOnly) {
        List<float[]> body = new java.util.ArrayList<>();
        for (com.newuniverse.nusmp.visual.genesis.VuMesh.Group g : mesh.groups) {
            if (staticOnly && !"-".equals(g.name)) continue;
            float[] a = g.verts[com.newuniverse.nusmp.visual.genesis.VuMesh.BODY];
            int n = g.count[com.newuniverse.nusmp.visual.genesis.VuMesh.BODY];
            for (int i = 0; i + 7 < n; i += com.newuniverse.nusmp.visual.genesis.VuMesh.FLOATS) body.add(new float[]{a[i], a[i + 1], a[i + 2]});
        }
        return body;
    }

    /** {x0, x1, y0, y1, z0, z1} of a layer's vertices (null for none): the box the first-person view limit looks at. */
    public static float[] box(List<float[]> verts) {
        if (verts == null || verts.isEmpty()) return null;
        float[] b = {Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE};
        for (float[] v : verts) {
            for (int a = 0; a < 3; a++) {
                b[a * 2] = Math.min(b[a * 2], v[a]);
                b[a * 2 + 1] = Math.max(b[a * 2 + 1], v[a]);
            }
        }
        return b;
    }

    /**
     * The grip of a mesh from its body vertices and the facts about it; null when it has no body. {@code gripAtFlipped}: the runtime normaliser turned the mesh over along its
     * length (the mesh's {@code grip} line then measures from the other end).
     */
    public static GripFinder.Grip find(List<float[]> body, String gripClass, float classGrip, float override, float gripLine, boolean gripAtFlipped, float[] explicit) {
        if (body == null || body.isEmpty()) return null;
        float directive = Float.isNaN(gripLine) ? Float.NaN : gripAtFlipped ? 1f - gripLine : gripLine;
        float[] xyz = pack(body);
        return GripFinder.find(xyz, body.size(), gripClass, classGrip, override, directive, explicit);
    }
}
