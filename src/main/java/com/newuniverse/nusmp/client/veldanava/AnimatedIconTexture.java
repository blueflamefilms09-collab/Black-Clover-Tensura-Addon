package com.newuniverse.nusmp.client.veldanava;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.Tickable;

/**
 * Why: vanilla's {@code .mcmeta} animation only works for textures stitched into an atlas, not for a skill icon blitted by its path (ManasCore's skill lists, our card screen), so the animated
 * icons are a small dynamic texture that shows one square frame of a vertical strip and swaps it as the game ticks. The texture manager ticks it ({@link Tickable}); a tick that does not
 * change the frame costs one integer division, and a frame change copies one square (32 x 32 or 128 x 128 pixels) and uploads it. Nothing is allocated per tick. Client only.
 */
public final class AnimatedIconTexture extends DynamicTexture implements Tickable {
    private final NativeImage strip;
    private final int size, frames, frameTime;
    private long ticks;
    private int shown;

    private AnimatedIconTexture(NativeImage first, NativeImage strip, int size, int frames, int frameTime) {
        super(first);
        this.strip = strip;
        this.size = size;
        this.frames = frames;
        this.frameTime = frameTime;
    }

    /** Builds the texture from a decoded strip, or returns null (and closes the strip) when the strip is not a whole number of square frames. */
    public static AnimatedIconTexture of(NativeImage strip, int frameTime, boolean linear) {
        int n = IconStrip.frames(strip.getWidth(), strip.getHeight());
        if (n < 1) {
            strip.close();
            return null;
        }
        int w = strip.getWidth();
        NativeImage first = new NativeImage(w, w, false);
        strip.copyRect(first, 0, 0, 0, 0, w, w, false, false);
        AnimatedIconTexture t = new AnimatedIconTexture(first, strip, w, n, frameTime);
        if (linear) t.setFilter(true, false);
        return t;
    }

    public int frames() { return frames; }

    public int frameTime() { return frameTime; }

    public int size() { return size; }

    @Override
    public void tick() {
        ticks++;
        int f = IconStrip.frameAt(ticks, frames, frameTime);
        if (f == shown) return;
        shown = f;
        NativeImage px = getPixels();
        if (px == null) return;
        strip.copyRect(px, 0, f * size, 0, 0, size, size, false, false);
        upload();
    }

    @Override
    public void close() {
        super.close();
        strip.close();
    }
}
