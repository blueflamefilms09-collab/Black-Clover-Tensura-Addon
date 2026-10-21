package com.newuniverse.nusmp.client.labyrinth.rift;

/**
 * Why: the arithmetic of the masked parallax of the ordinary tear (see {@code RiftParallax}). The render pipeline of the game does not give a mod a stencil buffer it can count on (the main target
 * only has stencil bits when the loader asks the driver for them, and shader packs replace the buffers altogether), so the "window" is a geometric MASK instead: the layers that show another
 * sky are built as strips that follow the lens outline, each deeper layer a little narrower than the one in front (looking into a recess, you see less of it), so nothing a layer shows can
 * ever spill past the opening, which is what a stencil test guarantees. The parallax is the sliding of the texture: each layer is shifted by the viewing angle in proportion to its depth,
 * so as the viewer walks round the tear the deep layers slide faster than the near ones. The lens turns to face the camera, so the horizontal shift comes from the angle the viewer is at
 * and the vertical shift from how high above or below the tear's middle they are. Pure arithmetic: a unit test pins it.
 */
public final class ParallaxMath {
    private ParallaxMath() {}

    /** Depth of each layer behind the lens (blocks), texture tile size (blocks), and brightness. */
    public static final float[] DEPTH = {0.35f, 0.9f}, TILE = {2.6f, 4.2f}, LIGHT = {0.95f, 0.6f};
    /** Strips per layer along the lens, and vertices per strip (two quads: a left half and a right half, so the middle can be brighter than the edge). */
    public static final int SEGMENTS = 14, VERTS_PER_STRIP = 8;

    /** Layers drawn at a level of detail of {@link TearGeometry#lod}: two near, one in the middle, none far. */
    public static int layers(int lod) { return lod == TearGeometry.FULL ? 2 : lod == TearGeometry.MIDDLE ? 1 : 0; }

    public static int vertices(int lod) { return layers(lod) * SEGMENTS * VERTS_PER_STRIP; }

    /** How much narrower layer {@code l} is than the lens (1 = as wide): the mask of a deeper layer is smaller. */
    public static float inset(int l) { return 1f - 0.06f * (l + 1); }

    /** The horizontal texture coordinate of a point {@code x} across the lens on layer {@code l}, for a viewer at {@code angle} radians round the tear, at tick {@code t}. */
    public static float u(int l, float x, float angle, float t) { return x / TILE[l] + angle * DEPTH[l] * 0.6f + t * 0.003f * (l + 1); }

    /** The vertical texture coordinate of height {@code y} on layer {@code l}, for a viewer {@code camRel} blocks above the lens's middle. */
    public static float v(int l, float y, float camRel, float t) { return y / TILE[l] - camRel * DEPTH[l] * 0.05f - t * 0.002f * (l + 1); }
}
