package com.newuniverse.nusmp.vfx.client.portal;

/**
 * Why: where the composed quads go differs (the shared 400-vertex effect buffer, or a direct draw with the portal shader), but the
 * quads are the same, so the composer talks to this one small interface.
 */
public interface PortalSink {
    /**
     * One quad. {@code p} holds four corners (12 floats, camera-relative): p0 and p1 are the inner edge, p2 and p3 the outer edge
     * (for a ring cell p0 = inner start angle, p1 = inner end angle, p2 = outer end angle, p3 = outer start angle). The texture's u runs
     * from {@code u0} (p0, p3) to {@code u1} (p1, p2); v is {@code vInner} on the inner edge and {@code vOuter} on the outer edge.
     * Colours are ARGB per edge. The arrays are reused by the composer: copy what you keep.
     */
    void quad(PortalPass pass, float[] p, float u0, float u1, float vInner, float vOuter, int cInner, int cOuter);
}
