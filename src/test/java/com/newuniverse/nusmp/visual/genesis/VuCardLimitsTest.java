package com.newuniverse.nusmp.visual.genesis;

import com.newuniverse.nusmp.vfx.SafeAdditiveMath;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 1.110.0 (docs/visual_upgrade/safe_additive.md): every Genesis weapon mesh keeps its halo cards (light shafts, rings) and flares inside the caps relative to the weapon's body height, Elsdocia
 * explicitly (the owner's screenshot showed a giant white column and diamond around it), and the clamp scales an oversized card down about its centre.
 */
class VuCardLimitsTest {
    private static final Path DIR = Path.of("src/main/resources/assets/nusmp/weapon_meshes_vu");

    private static VuMesh read(Path p) throws IOException {
        try (BufferedReader in = Files.newBufferedReader(p)) { return VuMesh.parse(in); }
    }

    @Test
    void everyGenesisMeshKeepsItsCardsAndFlaresInsideTheCaps() throws IOException {
        List<String> bad = new ArrayList<>();
        int n = 0;
        try (Stream<Path> s = Files.list(DIR)) {
            for (Path p : s.filter(f -> f.toString().endsWith(".txt")).sorted().toList()) {
                VuMesh m = read(p);
                float h = VuCardLimits.bodyHeight(m);
                if (h <= 0f) continue;
                n++;
                if (!SafeAdditiveMath.cardEdgeOk(VuCardLimits.longestCard(m), h)) bad.add(p.getFileName() + " card " + VuCardLimits.longestCard(m) + " of " + h);
                if (!SafeAdditiveMath.flareOk(VuCardLimits.largestFlare(m), h)) bad.add(p.getFileName() + " flare " + VuCardLimits.largestFlare(m) + " of " + h);
            }
        }
        assertTrue(n >= 15, "meshes found: " + n);
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void elsdociaIsInsideTheCaps() throws IOException {
        VuMesh m = read(DIR.resolve("elsdocia.txt"));
        float h = VuCardLimits.bodyHeight(m);
        assertTrue(h > 100f);
        assertTrue(VuCardLimits.longestCard(m) <= SafeAdditiveMath.MAX_CARD_RATIO * h, "the light shaft is shorter than the weapon");
        assertTrue(VuCardLimits.largestFlare(m) <= SafeAdditiveMath.MAX_FLARE_RATIO * h);
        assertEquals(0, VuCardLimits.clamp(m), "nothing to clamp: the shipped mesh is inside the caps");
    }

    @Test
    void anOversizedCardIsScaledToTheCapAboutItsCentre() throws IOException {
        String src = "vu 1\ng -\n"
                + "qn 0 0 0 0 0 0 0 0 1 10 0 0 0 0 0 0 1 10 100 0 0 0 0 0 1 0 100 0 0 0 0 0 1\n"
                + "qn 4 -150 0 0 0 0 0 0 1 150 0 0 0 0 0 0 1 150 100 0 0 0 0 0 1 -150 100 0 0 0 0 0 1\n";
        VuMesh m = VuMesh.parse(new BufferedReader(new java.io.StringReader(src)));
        assertEquals(0.9f * 100f / 300f, SafeAdditiveMath.cardScale(300f, 100f), 1e-6f);
        assertTrue(VuCardLimits.longestCard(m) <= 90.01f, "the parse clamped the 300 wide card to 90: " + VuCardLimits.longestCard(m));
        float[] a = m.groups.get(0).verts[VuMesh.HALO];
        assertEquals(0f, (a[0] + a[VuMesh.FLOATS] + a[2 * VuMesh.FLOATS] + a[3 * VuMesh.FLOATS]) / 4f, 1e-3f, "still centred on x = 0");
    }
}
