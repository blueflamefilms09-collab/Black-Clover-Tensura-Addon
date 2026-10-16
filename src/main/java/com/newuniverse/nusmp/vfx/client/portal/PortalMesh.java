package com.newuniverse.nusmp.vfx.client.portal;

/**
 * Why: the portal mesh is generated from its {@link PortalSpec} in code, once per tier, and cached, instead of shipping a model file.
 * A .geo.json (GeoLite) is a fixed set of cubes, but this gate needs several detail tiers, an outline that follows an oval of any
 * size, and rings that rotate every frame, so the cheap and honest representation is a table of angles per sector count; each frame
 * only scales and rotates those tables. Nothing here is rebuilt per frame and nothing holds a GL resource, so there is nothing to leak.
 */
public final class PortalMesh {
    private static final PortalMesh[] CACHE = new PortalMesh[PortalSpec.TIERS.length];

    public final PortalSpec spec;
    private final float[][] cos, sin;      // indexed by sector count (sparse), entries 0..n

    private PortalMesh(PortalSpec spec) {
        this.spec = spec;
        int max = 64;
        cos = new float[max + 1][];
        sin = new float[max + 1][];
        int[] counts = {spec.ringSegments(), spec.shellSectors(), spec.horizonSectors()};
        for (int n : counts) table(n);
    }

    private void table(int n) {
        if (cos[n] != null) return;
        cos[n] = new float[n + 1];
        sin[n] = new float[n + 1];
        for (int i = 0; i <= n; i++) {
            double a = Math.PI * 2 * i / n;
            cos[n][i] = (float) Math.cos(a);
            sin[n][i] = (float) Math.sin(a);
        }
    }

    /** cos of angle {@code i} of {@code n} (i may be n). */
    public float cos(int n, int i) { return cos[n][i]; }
    public float sin(int n, int i) { return sin[n][i]; }

    /** The cached mesh of a tier, built on first use. */
    public static synchronized PortalMesh of(PortalSpec spec) {
        PortalMesh m = CACHE[spec.tier()];
        if (m == null) CACHE[spec.tier()] = m = new PortalMesh(spec);
        return m;
    }
}
