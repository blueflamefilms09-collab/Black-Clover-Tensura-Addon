package com.newuniverse.nusmp.vfx;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * Self-test {@code visual.flat_props} of the CI server smoke test (docs/visual/flat_props.md): inside the booted mod, the gate decision of {@link FlatProps} is right (a flat cut-out is refused with the
 * switch off and allowed with it on, every other texture is always allowed, the default is off) and the material sheet of the solid replacements is there, fully opaque and not flat. Plain data and the
 * JDK image reader only: runs on a dedicated server, touches no client class and no player state.
 */
public final class FlatPropsSelfTests {
    private FlatPropsSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("visual.flat_props", server -> run());
    }

    private static SelfTests.Result run() throws Exception {
        if (FlatProps.FLAT.isEmpty()) return SelfTests.Result.fail("the flat list is empty");
        for (FlatProps.Entry e : FlatProps.FLAT) {
            String path = "textures/particle/" + e.texture() + ".png";
            if (!FlatProps.isFlat(path)) return SelfTests.Result.fail(e.texture() + " is not recognised as flat");
            if (FlatProps.shouldDraw(path, false)) return SelfTests.Result.fail(e.texture() + " would be drawn with visual.flatProps off");
            if (!FlatProps.shouldDraw(path, true)) return SelfTests.Result.fail(e.texture() + " would be hidden with visual.flatProps on");
        }
        if (!FlatProps.shouldDraw("textures/particle/glow.png", false) || !FlatProps.shouldDraw("textures/flatprops/solid.png", false)) return SelfTests.Result.fail("an ordinary texture is refused");
        if (FlatProps.isFlat("textures/entity/construct_sword.png")) return SelfTests.Result.fail("a texture outside textures/particle was taken for a cut-out");
        BufferedImage img;
        try (InputStream in = FlatPropsSelfTests.class.getResourceAsStream("/assets/nusmp/textures/flatprops/solid.png")) {
            if (in == null) return SelfTests.Result.fail("the solid material sheet is missing");
            img = ImageIO.read(in);
        }
        if (img.getWidth() != 128 || img.getHeight() != 64) return SelfTests.Result.fail("the solid material sheet has the wrong size");
        int lo = 255, hi = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int v = img.getRGB(x, y);
                if ((v >>> 24) != 255) return SelfTests.Result.fail("the solid material sheet has a see-through texel");
                int l = (((v >> 16) & 255) + ((v >> 8) & 255) + (v & 255)) / 3;
                lo = Math.min(lo, l);
                hi = Math.max(hi, l);
            }
        }
        if (hi - lo < 40) return SelfTests.Result.fail("the solid material sheet is flat");
        return SelfTests.Result.ok(FlatProps.FLAT.size() + " flat sprites gated, solid sheet ok");
    }
}
