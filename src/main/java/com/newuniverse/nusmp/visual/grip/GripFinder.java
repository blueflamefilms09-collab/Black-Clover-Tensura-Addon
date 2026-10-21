package com.newuniverse.nusmp.visual.grip;

import java.util.Set;

/**
 * 1.111.0 (docs/weapon_grip.md): where the hand holds a weapon mesh, found from the mesh itself. Pure: no Minecraft imports; tools/weapon_grip/grip_lib.py is the Python twin
 * (the golden file tools/weapon_grip/grip_golden.json is checked against this class in GripTableTest).
 * <p>
 * Mesh space: the butt of the weapon is at the low y end (WeaponNormaliser has already turned a reversed mesh over), mesh y runs along the blade. The result is a point
 * (x, y, z) on the mesh and the share of the mesh length (from the low y end) it lies at. Sources, strongest first:
 * <ol>
 *   <li>{@link Source#GRIPXYZ}: the mesh states the point itself (a {@code gripxyz x y z} line, for a generator that knows);</li>
 *   <li>{@link Source#OVERRIDE}: a per-weapon fraction in assets/nusmp/weapon_grips.json (checked by eye on the preview sheets);</li>
 *   <li>{@link Source#DIRECTIVE}: the mesh's {@code grip <fraction>} line (derived from its part names by tools/relics/mesh_lib.py, or verified by hand);</li>
 *   <li>{@link Source#HANDLE}: a sword-like class (dagger to greatsword, cleaver): the middle of the handle, i.e. of the narrow stretch between the pommel and the first
 *       section that is much wider (the guard), read from 64 cross-sections of the body;</li>
 *   <li>{@link Source#CLASS}: the grip fraction of the weapon's class (assets/nusmp/weapon_classes.json: 0.20 for a sword, 0.33 for a spear, 0.40 for a staff, 0.50 for a bow
 *       and a held item), which is also where the oversize rule of first person holds a long weapon.</li>
 * </ol>
 */
public final class GripFinder {
    private GripFinder() {}

    public enum Source { GRIPXYZ, OVERRIDE, DIRECTIVE, HANDLE, CLASS }

    /** The grip point in mesh space and its share of the length from the low y end. */
    public record Grip(float x, float y, float z, float fraction, Source source) {}

    /** Classes whose hilt is told from the blade by a guard (everything else is held on a shaft, a limb or as a whole). */
    public static final Set<String> HANDLE_MODE = Set.of("dagger", "shortsword", "longsword", "rapier", "katana", "sabre", "dadao", "greatsword", "cleaver");
    /** Classes that are held at their middle, whatever the class table says about the hand (a bow's riser, a trinket's body): the fraction is not clamped. */
    public static final Set<String> MIDDLE_HELD = Set.of("item", "handitem", "bow", "crossbow", "shield");

    public static final int SAMPLES = 64;
    /** A cross-section this many times wider than the handle is the guard. */
    public static final float GUARD_RATIO = 1.8f;
    /** A cross-section up to this many times the handle width still belongs to the handle (a pommel is wider). */
    public static final float POMMEL_RATIO = 1.3f;
    /** The handle lies in the lower half. */
    public static final float SEARCH_MAX = 0.5f;
    /** The handle is measured on the narrowest 20 percent of the cross-sections of the lower 40 percent. */
    public static final float REFERENCE_SPAN = 0.40f;
    /** A handle shorter than this share of the length is not trusted (the class fraction decides). */
    public static final float MIN_HANDLE = 0.03f;
    /** The hand never sits closer to the butt than this, nor further up than {@link #FRAC_MAX} (a sword-like class). */
    public static final float FRAC_MIN = 0.06f, FRAC_MAX = 0.5f;
    /** Half the height of the window whose vertices centre the point sideways, as a share of the length. */
    public static final float WINDOW = 0.04f;

    /**
     * @param xyz        the body's vertices, three floats each, four vertices per quad
     * @param count      number of vertices used
     * @param className  the grip class of the weapon (a key of weapon_classes.json)
     * @param classGrip  that class's grip fraction
     * @param override   a per-weapon fraction, NaN = none
     * @param directive  the mesh's {@code grip} fraction measured from the low y end of the mesh as it is drawn, NaN = none
     * @param explicit   {x, y, z} of a {@code gripxyz} line or null
     * @return the grip, or null for a mesh without vertices
     */
    public static Grip find(float[] xyz, int count, String className, float classGrip, float override, float directive, float[] explicit) {
        if (count < 4) return null;
        float y0 = Float.MAX_VALUE, y1 = -Float.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            y0 = Math.min(y0, xyz[i * 3 + 1]);
            y1 = Math.max(y1, xyz[i * 3 + 1]);
        }
        float length = y1 - y0;
        if (!(length > 0f)) return null;
        if (explicit != null && explicit.length >= 3) return new Grip(explicit[0], explicit[1], explicit[2], (explicit[1] - y0) / length, Source.GRIPXYZ);
        float frac;
        Source src;
        boolean middle = MIDDLE_HELD.contains(className);
        if (!Float.isNaN(override)) {
            frac = override;
            src = Source.OVERRIDE;
        } else if (!Float.isNaN(directive) && !middle) {
            frac = directive;
            src = Source.DIRECTIVE;
        } else {
            frac = Float.NaN;
            src = Source.CLASS;
            if (HANDLE_MODE.contains(className)) {
                float[] hr = handleRange(xyz, count);
                if (hr != null && hr[1] - hr[0] >= MIN_HANDLE * length) {
                    frac = ((hr[0] + hr[1]) / 2f - y0) / length;
                    src = Source.HANDLE;
                }
            }
            if (Float.isNaN(frac)) frac = classGrip;
        }
        if (!middle) frac = Math.min(FRAC_MAX, Math.max(FRAC_MIN, frac));
        frac = Math.min(1f, Math.max(0f, frac));
        float gy = y0 + frac * length;
        float xlo = Float.MAX_VALUE, xhi = -Float.MAX_VALUE, zlo = Float.MAX_VALUE, zhi = -Float.MAX_VALUE;
        float win = ("item".equals(className) || "handitem".equals(className)) && src == Source.CLASS ? Float.MAX_VALUE : WINDOW * length;      // a held item with no stated grip is held at the centre of its box
        for (int i = 0; i < count; i++) {
            if (Math.abs(xyz[i * 3 + 1] - gy) > win) continue;
            xlo = Math.min(xlo, xyz[i * 3]);
            xhi = Math.max(xhi, xyz[i * 3]);
            zlo = Math.min(zlo, xyz[i * 3 + 2]);
            zhi = Math.max(zhi, xyz[i * 3 + 2]);
        }
        float gx = xlo > xhi ? 0f : (xlo + xhi) / 2f, gz = zlo > zhi ? 0f : (zlo + zhi) / 2f;
        return new Grip(gx, gy, gz, frac, src);
    }

    /**
     * The widths (the larger of the x extent and the z extent) of the mesh at {@link #SAMPLES} evenly spaced heights, 0 where nothing is cut: the points where the quads' edges
     * cross the height. {@code out[0]} and {@code out[1]} receive y0 and y1 of the mesh.
     */
    public static float[] sliceWidths(float[] xyz, int count, float[] out) {
        float y0 = Float.MAX_VALUE, y1 = -Float.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            y0 = Math.min(y0, xyz[i * 3 + 1]);
            y1 = Math.max(y1, xyz[i * 3 + 1]);
        }
        int n = SAMPLES;
        float[] w = new float[n];
        if (out != null) {
            out[0] = y0;
            out[1] = y1;
        }
        float length = y1 - y0;
        if (!(length > 0f)) return w;
        float[] xlo = new float[n], xhi = new float[n], zlo = new float[n], zhi = new float[n];
        java.util.Arrays.fill(xlo, Float.MAX_VALUE);
        java.util.Arrays.fill(zlo, Float.MAX_VALUE);
        java.util.Arrays.fill(xhi, -Float.MAX_VALUE);
        java.util.Arrays.fill(zhi, -Float.MAX_VALUE);
        for (int q = 0; q + 3 < count; q += 4) {
            for (int e = 0; e < 4; e++) {
                int a = (q + e) * 3, b = (q + (e + 1) % 4) * 3;
                float ya = xyz[a + 1], yb = xyz[b + 1];
                int i0 = (int) Math.floor(((Math.min(ya, yb) - y0) / length) * n - 0.5) + 1;
                int i1 = (int) Math.floor(((Math.max(ya, yb) - y0) / length) * n - 0.5);
                for (int i = Math.max(0, i0); i <= Math.min(n - 1, i1); i++) {
                    float sy = y0 + (i + 0.5f) * length / n;
                    if (yb == ya) {
                        add(xlo, xhi, zlo, zhi, i, xyz[a], xyz[a + 2]);
                        add(xlo, xhi, zlo, zhi, i, xyz[b], xyz[b + 2]);
                    } else {
                        float t = (sy - ya) / (yb - ya);
                        add(xlo, xhi, zlo, zhi, i, xyz[a] + (xyz[b] - xyz[a]) * t, xyz[a + 2] + (xyz[b + 2] - xyz[a + 2]) * t);
                    }
                }
            }
        }
        for (int i = 0; i < n; i++) w[i] = xlo[i] > xhi[i] ? 0f : Math.max(xhi[i] - xlo[i], zhi[i] - zlo[i]);
        return w;
    }

    private static void add(float[] xlo, float[] xhi, float[] zlo, float[] zhi, int i, float x, float z) {
        xlo[i] = Math.min(xlo[i], x);
        xhi[i] = Math.max(xhi[i], x);
        zlo[i] = Math.min(zlo[i], z);
        zhi[i] = Math.max(zhi[i], z);
    }

    /** {yStart, yEnd} of the handle (mesh units) of a sword-like mesh, or null when no guard follows a handle. */
    public static float[] handleRange(float[] xyz, int count) {
        float[] ends = new float[2];
        float[] w = sliceWidths(xyz, count, ends);
        int n = w.length;
        float length = ends[1] - ends[0];
        if (!(length > 0f)) return null;
        float[] low = new float[n];
        int m = 0;
        for (int i = 0; i < n; i++) if ((i + 0.5f) / n <= REFERENCE_SPAN && w[i] > 1e-6f) low[m++] = w[i];
        if (m == 0) return null;
        java.util.Arrays.sort(low, 0, m);
        float hw = low[m / 5];
        int start = -1;
        for (int i = 0; i < n; i++) {
            if ((i + 0.5f) / n > SEARCH_MAX) break;
            if (start < 0) {
                if (w[i] > 1e-6f && w[i] <= POMMEL_RATIO * hw) start = i;
                continue;
            }
            if (w[i] >= GUARD_RATIO * hw) return new float[]{ends[0] + start * length / n, ends[0] + i * length / n};
        }
        return null;
    }
}
