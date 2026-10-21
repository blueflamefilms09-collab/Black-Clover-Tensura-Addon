package com.newuniverse.nusmp.visual.genesis;

import com.newuniverse.nusmp.vfx.SafeAdditiveMath;

/**
 * 1.110.0 (docs/visual_upgrade/safe_additive.md): after a mesh is parsed, a halo card whose longest edge passes {@link SafeAdditiveMath#MAX_CARD_RATIO} of the weapon's body height is scaled down
 * about its own centre (UVs and winding untouched), and a flare is capped the same way. Every shipped mesh is inside the caps today, so this changes nothing for them; it keeps a future mesh from
 * drawing a light shaft far taller than the weapon. Pure data, no game classes.
 */
public final class VuCardLimits {
    private VuCardLimits() {}

    /** The body height the caps are measured against, or 0 when the mesh has no body. */
    public static float bodyHeight(VuMesh m) { return m.bodyMaxY > m.bodyMinY ? m.bodyMaxY - m.bodyMinY : 0f; }

    /** The longest edge of the quad starting at float {@code at} of a vertex array. */
    public static float longestEdge(float[] a, int at) {
        float best = 0f;
        for (int k = 0; k < 4; k++) {
            int p = at + k * VuMesh.FLOATS, q = at + ((k + 1) & 3) * VuMesh.FLOATS;
            float dx = a[p] - a[q], dy = a[p + 1] - a[q + 1], dz = a[p + 2] - a[q + 2];
            best = Math.max(best, (float) Math.sqrt(dx * dx + dy * dy + dz * dz));
        }
        return best;
    }

    /** The longest halo card edge of the mesh. */
    public static float longestCard(VuMesh m) {
        float best = 0f;
        for (VuMesh.Group g : m.groups) {
            float[] a = g.verts[VuMesh.HALO];
            for (int i = 0; i < g.count[VuMesh.HALO]; i += VuMesh.FLOATS * 4) best = Math.max(best, longestEdge(a, i));
        }
        return best;
    }

    /** The largest flare half size of the mesh. */
    public static float largestFlare(VuMesh m) {
        float best = 0f;
        for (VuMesh.Flare f : m.flares) best = Math.max(best, f.size());
        return best;
    }

    /** Scales the oversized halo cards of the mesh to the cap; returns how many were changed. Flares are not rewritten (a record): see {@link #flareScale}. */
    public static int clamp(VuMesh m) {
        float h = bodyHeight(m);
        if (h <= 0f) return 0;
        int changed = 0;
        for (VuMesh.Group g : m.groups) {
            float[] a = g.verts[VuMesh.HALO];
            for (int i = 0; i < g.count[VuMesh.HALO]; i += VuMesh.FLOATS * 4) {
                float s = SafeAdditiveMath.cardScale(longestEdge(a, i), h);
                if (s >= 1f) continue;
                float cx = 0, cy = 0, cz = 0;
                for (int k = 0; k < 4; k++) { cx += a[i + k * VuMesh.FLOATS]; cy += a[i + k * VuMesh.FLOATS + 1]; cz += a[i + k * VuMesh.FLOATS + 2]; }
                cx /= 4f; cy /= 4f; cz /= 4f;
                for (int k = 0; k < 4; k++) {
                    int p = i + k * VuMesh.FLOATS;
                    a[p] = cx + (a[p] - cx) * s;
                    a[p + 1] = cy + (a[p + 1] - cy) * s;
                    a[p + 2] = cz + (a[p + 2] - cz) * s;
                }
                changed++;
            }
        }
        return changed;
    }

    /** The factor a flare of this half size is drawn with (1 when inside the cap). */
    public static float flareScale(VuMesh m, float halfSize) {
        float h = bodyHeight(m);
        if (h <= 0f || halfSize <= 0f) return 1f;
        float cap = SafeAdditiveMath.MAX_FLARE_RATIO * h;
        return halfSize <= cap ? 1f : cap / halfSize;
    }
}
