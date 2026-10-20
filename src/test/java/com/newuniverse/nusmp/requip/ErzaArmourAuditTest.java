package com.newuniverse.nusmp.requip;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the owner called Erza's armours "the worst" and set the Djinn armours as the strict standard. This audit fails the build if ANY Erza armour has fewer pieces, fewer drawn faces, a smaller or
 * thinner skin, a poorer palette or a weaker glow map than the Djinn minimum measured by tools/erza_armour_audit.py (the numbers are in {@code src/test/resources/erza_armour_bar.json}, written by
 * {@code python3 -B tools/erza_armour_audit.py --write-bar}, and kept equal to {@link ArmourQuality#BAR}, which the in-game self-test {@code skill.erza_requip2} uses). It also checks that the bar is not
 * above what the Djinn files really contain, so the standard cannot be quietly raised past the Djinn.
 */
class ErzaArmourAuditTest {
    private static final File RES = new File("src/main/resources");
    private static final File DJINN = new File(RES, "assets/nusmp/djinn");

    private static final ArmourQuality.Source SRC = p -> {
        File f = new File(RES, p);
        return f.isFile() ? new FileInputStream(f) : null;
    };

    @Test
    void theBarFileIsTheConstantTheGameChecks() throws Exception {
        File f = new File("src/test/resources/erza_armour_bar.json");
        assertTrue(f.isFile(), "run python3 -B tools/erza_armour_audit.py --write-bar");
        JsonObject o;
        try (Reader r = new FileReader(f)) { o = JsonParser.parseReader(r).getAsJsonObject(); }
        ArmourQuality.Bar b = ArmourQuality.BAR;
        assertEquals(b.pieces(), o.get("pieces_min").getAsInt(), "pieces");
        assertEquals(b.faces(), o.get("faces_min").getAsInt(), "faces");
        assertEquals(b.glowFaces(), o.get("glow_faces_min").getAsInt(), "glow faces");
        assertEquals(b.paintedTexels(), o.get("painted_texels_min").getAsLong(), "painted texels");
        assertEquals(b.glowPaintedTexels(), o.get("glow_painted_texels_min").getAsLong(), "glow painted texels");
        assertEquals(b.palette(), o.get("palette_min").getAsInt(), "palette");
        assertEquals(b.glowPalette(), o.get("glow_palette_min").getAsInt(), "glow palette");
        assertEquals(b.density(), o.get("density_min").getAsDouble(), 1e-9, "density");
        assertEquals(b.textureSide(), o.get("texture_side_min").getAsInt(), "texture side");
    }

    @Test
    void theBarIsNotAboveWhatTheDjinnFilesContain() throws Exception {
        int sets = 0, minPieces = Integer.MAX_VALUE, minFaces = Integer.MAX_VALUE;
        String[] names = DJINN.list();
        assertNotNull(names, "djinn folder");
        for (String n : names) {
            if (!n.startsWith("u_")) continue;
            File[] files = new File(DJINN, n).listFiles((d, fn) -> fn.endsWith(".txt"));
            if (files == null) continue;
            int quads = 0;
            for (File f : files) {
                try (BufferedReader r = new BufferedReader(new FileReader(f))) {
                    String line;
                    while ((line = r.readLine()) != null) if (line.startsWith("q")) quads++;
                }
            }
            sets++;
            minPieces = Math.min(minPieces, files.length);
            minFaces = Math.min(minFaces, quads);
        }
        assertTrue(sets >= 30, "the unique Djinn sets are there: " + sets);
        assertTrue(ArmourQuality.BAR.pieces() <= minPieces, "the bar asks " + ArmourQuality.BAR.pieces() + " pieces, the smallest unique Djinn set has " + minPieces);
        assertTrue(ArmourQuality.BAR.faces() <= minFaces, "the bar asks " + ArmourQuality.BAR.faces() + " faces, the smallest unique Djinn set has " + minFaces);
    }

    @Test
    void everyErzaArmourMeetsTheDjinnMinimum() throws Exception {
        List<String> bad = new ArrayList<>();
        for (Armour a : Armour.values()) {
            ArmourQuality.Metrics m = ArmourQuality.measure(a, SRC);
            bad.addAll(ArmourQuality.violations(a.id(), m, ArmourQuality.BAR));
        }
        assertTrue(bad.isEmpty(), "below the Djinn standard:\n" + String.join("\n", bad));
    }

    @Test
    void everyArmourIsAFullSet() {
        String[] standard = {"helm", "gorget", "chest", "backplate", "belt", "skirt", "pauldron_r", "pauldron_l", "vambrace_r", "vambrace_l", "gauntlet_r", "gauntlet_l", "cuisse_r", "cuisse_l",
                "greave_r", "greave_l", "sabaton_r", "sabaton_l"};
        for (Armour a : Armour.values()) {
            List<String> names = new ArrayList<>();
            for (ArmourSets.Piece p : ArmourSets.of(a)) names.add(p.name());
            for (String s : standard) assertTrue(names.contains(s), a.id() + " lacks " + s);
            assertTrue(names.size() > standard.length, a.id() + " has no signature piece of its own");
        }
    }
}
