package com.newuniverse.nusmp.vfx;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * Why (docs/vfx_square_bug.md): the textures the square-bug fix is checked against, and the reader that gets their pixels from the class path (the jar, or the resources folder in the unit tests). Pure
 * (no game classes), shared by the unit tests and the self-test {@code vfx.square_bug}.
 */
public final class SquareBugAudit {
    private SquareBugAudit() {}

    /** The sprites that were drawn as solid squares on the ground and round the body (spirit dive rings, the Erza equip ring, the air ring, the soft glow...), names in textures/particle. */
    public static final String[] SPRITES = {
            "air_ring", "flamefx_ring", "waterfx_ripple", "earthfam_rock_ring", "lightningfx_ring", "gravity_ring", "smoke_ring", "snow_ring", "permeation_ring", "glow"
    };

    /** A decoded texture: width, height and ARGB texels row by row. */
    public record Pixels(int w, int h, int[] argb) {}

    /** Reads {@code assets/nusmp/textures/particle/<name>.png} from the class path; null when it is not there. */
    public static Pixels particle(String name) throws IOException {
        try (InputStream in = SquareBugAudit.class.getResourceAsStream("/assets/nusmp/textures/particle/" + name + ".png")) {
            if (in == null) return null;
            BufferedImage img = ImageIO.read(in);
            if (img == null) return null;
            int w = img.getWidth(), h = img.getHeight();
            int[] argb = new int[w * h];
            img.getRGB(0, 0, w, h, argb, 0, w);
            return new Pixels(w, h, argb);
        }
    }
}
