package com.mojang.blaze3d.platform;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/** Why: harness shim: a PNG read through ImageIO. */
public class NativeImage implements AutoCloseable {
    private final BufferedImage img;
    private NativeImage(BufferedImage i) { img = i; }
    public static NativeImage read(InputStream in) throws IOException {
        BufferedImage i = ImageIO.read(in);
        if (i == null) throw new IOException("not an image");
        return new NativeImage(i);
    }
    public int getWidth() { return img.getWidth(); }
    public int getHeight() { return img.getHeight(); }
    @Override public void close() {}
}
