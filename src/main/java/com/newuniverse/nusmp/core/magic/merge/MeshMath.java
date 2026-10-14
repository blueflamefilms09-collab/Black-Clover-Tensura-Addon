package com.newuniverse.nusmp.core.magic.merge;

/**
 * Pure maths of the baked emblem mesh (client.merge.MergedMesh). A mesh is a flat float array, eight floats per vertex (x y z u v nx ny nz),
 * four vertices per quad.
 */
public final class MeshMath {
    private MeshMath() {}

    public static final int STRIDE = 8;

    /**
     * Centres the vertices on the origin, scales the largest side to one block, and turns y-up geometry into the armour layer's y-down
     * space: y and the normals' y are negated and every quad's winding is reversed (0 1 2 3 becomes 0 3 2 1), so the faces keep facing
     * outward. Returns an empty array for no vertices, a vertex count that is not a multiple of four, or a degenerate (flat point) mesh.
     */
    public static float[] normalise(float[] v) {
        if (v == null || v.length == 0 || v.length % STRIDE != 0) return new float[0];
        int n = v.length / STRIDE;
        if (n % 4 != 0) return new float[0];
        float minX = Float.MAX_VALUE, minY = minX, minZ = minX, maxX = -Float.MAX_VALUE, maxY = maxX, maxZ = maxX;
        for (int i = 0; i < n; i++) {
            float x = v[i * STRIDE], y = v[i * STRIDE + 1], z = v[i * STRIDE + 2];
            minX = Math.min(minX, x); maxX = Math.max(maxX, x);
            minY = Math.min(minY, y); maxY = Math.max(maxY, y);
            minZ = Math.min(minZ, z); maxZ = Math.max(maxZ, z);
        }
        float size = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
        if (!(size > 1e-6f)) return new float[0];
        float cx = (minX + maxX) / 2f, cy = (minY + maxY) / 2f, cz = (minZ + maxZ) / 2f, k = 1f / size;
        float[] out = new float[v.length];
        for (int q = 0; q < n / 4; q++) {
            for (int j = 0; j < 4; j++) {
                int src = (q * 4 + j) * STRIDE;
                int dst = (q * 4 + (j == 0 ? 0 : 4 - j)) * STRIDE;
                out[dst] = (v[src] - cx) * k;
                out[dst + 1] = -(v[src + 1] - cy) * k;
                out[dst + 2] = (v[src + 2] - cz) * k;
                out[dst + 3] = v[src + 3];
                out[dst + 4] = v[src + 4];
                out[dst + 5] = v[src + 5];
                out[dst + 6] = -v[src + 6];
                out[dst + 7] = v[src + 7];
            }
        }
        return out;
    }
}
