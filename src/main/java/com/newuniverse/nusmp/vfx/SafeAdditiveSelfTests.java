package com.newuniverse.nusmp.vfx;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.visual.genesis.VuCardLimits;
import com.newuniverse.nusmp.visual.genesis.VuMesh;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 1.110.0 self-test {@code visual.safe_additive} of the CI server smoke test (docs/visual_upgrade/safe_additive.md): inside the booted mod, the blend maths cannot draw a box, the shared halo card
 * sheet has transparent card borders, no solid card and no colour under alpha 0, and Elsdocia's cards and flares are inside the caps. Plain data and the JDK image reader only: runs on a dedicated server.
 */
public final class SafeAdditiveSelfTests {
    private SafeAdditiveSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("visual.safe_additive", server -> run());
    }

    private static SelfTests.Result run() throws Exception {
        if (!SafeAdditiveMath.addsNothing(0f, 1f) || SafeAdditiveMath.alphaAdd(0.25f, 1f, 0f, 1f) != 0.25f) return SelfTests.Result.fail("a transparent texel adds light");
        if (SafeAdditiveMath.alphaAdd(0f, 1f, 0.5f, 0.5f) != 0.25f) return SelfTests.Result.fail("the alpha-weighted add is wrong");
        if (AdditivePolicy.choose(TextureAlpha.Kind.COLOUR_UNDER_ALPHA, false) != AdditivePolicy.Blend.ALPHA_ADD || AdditivePolicy.choose(TextureAlpha.Kind.BLACK_UNDER_ALPHA, true) != AdditivePolicy.Blend.ALPHA_ADD) return SelfTests.Result.fail("the blend policy is wrong");
        BufferedImage img;
        try (InputStream in = SafeAdditiveSelfTests.class.getResourceAsStream("/assets/nusmp/textures/entity/weapon_vu/halo.png")) {
            if (in == null) return SelfTests.Result.fail("halo.png is missing");
            img = ImageIO.read(in);
        }
        int w = img.getWidth(), cell = 128, hiddenColour = 0, borderMax = 0, solidCells = 0;
        for (int cy = 0; cy < img.getHeight(); cy += cell) {
            for (int cx = 0; cx < w; cx += cell) {
                int solid = 0;
                for (int y = 0; y < cell; y++) {
                    for (int x = 0; x < cell; x++) {
                        int v = img.getRGB(cx + x, cy + y), a = v >>> 24;
                        if (a == 0 && (v & 0xFFFFFF) != 0) hiddenColour++;
                        if (x == 0 || y == 0 || x == cell - 1 || y == cell - 1) borderMax = Math.max(borderMax, a);
                        if (a >= 200 && Math.min((v >> 16) & 255, Math.min((v >> 8) & 255, v & 255)) >= 235) solid++;
                    }
                }
                if (solid >= cell * cell * 0.35) solidCells++;
            }
        }
        if (hiddenColour != 0) return SelfTests.Result.fail("halo.png carries colour under alpha 0 (" + hiddenColour + " px)");
        if (borderMax > 6) return SelfTests.Result.fail("halo.png has a visible card border (alpha " + borderMax + ")");
        if (solidCells != 0) return SelfTests.Result.fail("halo.png has a solid white card");
        VuMesh m;
        try (InputStream in = SafeAdditiveSelfTests.class.getResourceAsStream("/assets/nusmp/weapon_meshes_vu/elsdocia.txt")) {
            if (in == null) return SelfTests.Result.fail("the Elsdocia mesh is missing");
            m = VuMesh.parse(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)));
        }
        float h = VuCardLimits.bodyHeight(m);
        if (!SafeAdditiveMath.cardEdgeOk(VuCardLimits.longestCard(m), h)) return SelfTests.Result.fail("an Elsdocia card is taller than the cap");
        if (!SafeAdditiveMath.flareOk(VuCardLimits.largestFlare(m), h)) return SelfTests.Result.fail("an Elsdocia flare is over the cap");
        return SelfTests.Result.ok("halo sheet clean (borders <= " + borderMax + "), Elsdocia longest card " + Math.round(VuCardLimits.longestCard(m)) + " of body " + Math.round(h));
    }
}
