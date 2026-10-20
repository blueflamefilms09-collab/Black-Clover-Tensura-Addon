package com.newuniverse.nusmp.vfx.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.newuniverse.nusmp.vfx.SoftCircle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Why (docs/vfx_square_bug.md): the generated stand-in for a ring or glow texture that is missing or came out opaque, uploaded into a dynamic texture the first time it is needed
 * ({@link SoftCircle} makes the pixels). Render thread only. The texture is registered once; a resource reload does not touch it (it is not a resource).
 */
public final class SoftCircleTexture {
    private SoftCircleTexture() {}

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("nusmp", "dynamic/soft_circle");
    private static boolean registered;

    /** The location of the soft circle, registering it on the first call. */
    public static ResourceLocation get() {
        if (!registered) {
            registered = true;
            int n = SoftCircle.SIZE;
            int[] argb = SoftCircle.argb(n);
            NativeImage img = new NativeImage(NativeImage.Format.RGBA, n, n, false);
            for (int y = 0; y < n; y++) {
                for (int x = 0; x < n; x++) {
                    int p = argb[y * n + x];
                    img.setPixelRGBA(x, y, (p & 0xFF00FF00) | ((p >> 16) & 0xFF) | ((p & 0xFF) << 16));     // ARGB -> ABGR
                }
            }
            Minecraft.getInstance().getTextureManager().register(ID, new DynamicTexture(img));
        }
        return ID;
    }
}
