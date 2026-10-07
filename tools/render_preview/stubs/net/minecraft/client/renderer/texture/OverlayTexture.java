package net.minecraft.client.renderer.texture;

/** Preview stub of OverlayTexture: NO_OVERLAY and the packing helpers (the preview ignores overlays: they only tint hurt / flashing models). */
public class OverlayTexture {
    private static final int SIZE = 16;
    public static final int NO_WHITE_U = 0;
    public static final int RED_OVERLAY_V = 3;
    public static final int WHITE_OVERLAY_V = 10;
    public static final int NO_OVERLAY = pack(0, 10);

    private OverlayTexture() {}

    public static int pack(int u, int v) { return u | v << 16; }

    public static int pack(float whiteOverlayProgress, boolean hurtOrDead) {
        return pack(u(whiteOverlayProgress), v(hurtOrDead));
    }

    public static int u(float whiteOverlayProgress) { return (int) (whiteOverlayProgress * 15.0F); }

    public static int v(boolean hurtOrDead) { return hurtOrDead ? RED_OVERLAY_V : 10; }
}
