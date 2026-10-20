package com.newuniverse.nusmp.vfx;

/**
 * Why (docs/vfx_square_bug.md): the stand-in for a ring or glow texture that is missing or came out opaque: a white disc whose alpha falls off smoothly to zero well inside the square, so the
 * corners and the edges are transparent whatever blend draws it. Pure: the renderer uploads the pixels into a dynamic texture.
 */
public final class SoftCircle {
    private SoftCircle() {}

    public static final int SIZE = 64;

    /** {@code size * size} ARGB texels: white, alpha 255 in the middle falling off with the square of the distance to 0 at 0.92 of the half side. */
    public static int[] argb(int size) {
        int[] out = new int[size * size];
        float c = (size - 1) / 2f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float d = (float) Math.sqrt((x - c) * (x - c) + (y - c) * (y - c)) / (c * 0.92f);
                float a = d >= 1f ? 0f : (1f - d) * (1f - d);
                out[y * size + x] = (Math.round(a * 255f) << 24) | 0xFFFFFF;
            }
        }
        return out;
    }
}
