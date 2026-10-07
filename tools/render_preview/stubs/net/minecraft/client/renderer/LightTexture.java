package net.minecraft.client.renderer;

/** Preview stub of LightTexture: the packed-light helpers (block light in bits 4..7, sky light in bits 20..23). */
public class LightTexture {
    public static final int FULL_BRIGHT = 15728880;
    public static final int FULL_SKY = 15728640;
    public static final int FULL_BLOCK = 240;

    private LightTexture() {}

    public static int pack(int blockLight, int skyLight) { return blockLight << 4 | skyLight << 20; }

    public static int block(int packedLight) { return packedLight >>> 4 & 15; }

    public static int sky(int packedLight) { return packedLight >>> 20 & 15; }

    public static int lightCoordsWithEmission(int packedLight, int emission) {
        if (emission == 0) return packedLight;
        int i = Math.max(sky(packedLight), emission);
        int j = Math.max(block(packedLight), emission);
        return pack(j, i);
    }
}
