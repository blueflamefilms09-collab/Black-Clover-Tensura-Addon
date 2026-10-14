package com.newuniverse.nusmp.client.relics.worn.lib;

/** The six colours a worn model's quads pick from, made from the relic's palette (main, trim, glow as 0xRRGGBB). Pure. */
public final class WornPalette {
    private WornPalette() {}

    /** Index = {@link WornMesh#MAIN} .. {@link WornMesh#NONE}: main, trim, glow, dark (main darkened), accent (trim and glow mixed), none (white: the tile's own colours). */
    public static int[] slots(int main, int trim, int glow) {
        return new int[]{main & 0xFFFFFF, trim & 0xFFFFFF, glow & 0xFFFFFF, mix(main, 0x000000, 0.62f), mix(trim, glow, 0.5f), 0xFFFFFF};
    }

    /** A blend of two 0xRRGGBB colours, t = 0 is {@code a}, 1 is {@code b}. */
    public static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1f - t) + ((b >> 16) & 255) * t), g = (int) (((a >> 8) & 255) * (1f - t) + ((b >> 8) & 255) * t),
                bl = (int) ((a & 255) * (1f - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /** A colour with each channel multiplied by k (clamped to 0..255). */
    public static int scale(int rgb, float k) {
        int r = Math.min(255, Math.max(0, (int) (((rgb >> 16) & 255) * k))), g = Math.min(255, Math.max(0, (int) (((rgb >> 8) & 255) * k))),
                b = Math.min(255, Math.max(0, (int) ((rgb & 255) * k)));
        return (r << 16) | (g << 8) | b;
    }
}
