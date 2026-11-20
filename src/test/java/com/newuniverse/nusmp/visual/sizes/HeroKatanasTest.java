package com.newuniverse.nusmp.visual.sizes;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why (hero katanas, docs/heroweapon/hero_katanas.md): 4.61.0 only thinned the blades of the white tiger, soul eater, siderite, sakura stone, wind king and Hellblaze katanas; wings, ribs, petals,
 * a crown and a star guard as wide as the blade is long still changed their outline, so they read as fantasy blades. They are true katanas now (a blade 1/30 as wide as the mesh is long, a thin
 * tsuba, the lore inlaid in the blade and set on the hilt). Data tests over the real mesh files: the width ratio of the blade, the width of the whole outline at the guard, the budgets, the kept
 * 4.61.0 copies (rule 2), and that the place line and glow tint did not move. Nothing here says how it looks in a game.
 */
class HeroKatanasTest {
    private static final Path RES = Path.of("src/main/resources/assets/anime_multiverse");
    private static final Path MESHES = RES.resolve("weapon_meshes");
    private static final Path SKINS = RES.resolve("textures/entity/weapon");
    private static final String HELL = "hellblaze_the_demon_sin__katana";
    private static final List<String> FORMS = List.of("vassal_katana__white_tiger_katana", "vassal_katana__soul_eater_katana", "vassal_katana__siderite_katana",
            "vassal_katana__sakura_stone_katana", "legendary_sword__wind_king_katana");
    /** The lore each one carries as a real part of the mesh (the painted inlay is in the skin, so it is not a part). */
    private static final java.util.Map<String, List<String>> LORE = java.util.Map.of(
            "vassal_katana__white_tiger_katana", List.of("menuki", "tassel", "kashira_gem"),
            "vassal_katana__soul_eater_katana", List.of("menuki", "tassel", "kashira_gem"),
            "vassal_katana__siderite_katana", List.of("menuki", "orbit", "tassel"),
            "vassal_katana__sakura_stone_katana", List.of("menuki", "kashira_gem", "tassel"),
            "legendary_sword__wind_king_katana", List.of("menuki", "tassel", "kashira_gem"),
            HELL, List.of("menuki", "kashira_gem"));

    private static List<String> all() {
        List<String> ids = new ArrayList<>(FORMS);
        ids.add(HELL);
        return ids;
    }

    private static Path mesh(String id) {
        return id.equals(HELL) ? MESHES.resolve("up/" + id + ".txt") : MESHES.resolve(id + ".txt");
    }

    private static Path thin(String id) {
        return id.equals(HELL) ? MESHES.resolve("up/classic_thin/" + id + ".txt") : MESHES.resolve("classic_thin/" + id + ".txt");
    }

    private static Path skin(String id, String suffix) {
        return id.equals(HELL) ? SKINS.resolve("up/" + id + suffix + ".png") : SKINS.resolve(id + suffix + ".png");
    }

    private static Path thinSkin(String id, String suffix) {
        return id.equals(HELL) ? SKINS.resolve("up/classic_thin/" + id + suffix + ".png") : SKINS.resolve("classic_thin/" + id + suffix + ".png");
    }

    /** {body quads, glow quads}. */
    private static int[] lines(Path mesh) throws IOException {
        int body = 0, glow = 0;
        for (String l : Files.readAllLines(mesh)) {
            if (l.startsWith("q ") || l.startsWith("qn ")) {
                if (l.split("\\s+")[1].equals("2")) glow++;
                else body++;
            }
        }
        return new int[]{body, glow};
    }

    /**
     * The widest the whole mesh is across x in any one stretch of its length (24 stretches; the points of the layers 0 and 2 that lie in it). The blade is thin, so this is the width of the widest
     * ornament: the tsuba (or what hangs from the hilt), which a true katana keeps small.
     */
    static double outlineOverLength(Path mesh) throws IOException {
        List<double[]> pts = new ArrayList<>();
        double y0 = Double.MAX_VALUE, y1 = -Double.MAX_VALUE;
        for (String l : Files.readAllLines(mesh)) {
            boolean qn = l.startsWith("qn "), q = l.startsWith("q ");
            if (!qn && !q) continue;
            String[] v = l.trim().split("\\s+");
            int layer = Integer.parseInt(v[1]);
            if (layer > 2) continue;
            int stride = qn ? 8 : 5;
            for (int k = 0; k < 4; k++) {
                double x = Double.parseDouble(v[2 + k * stride]), y = Double.parseDouble(v[3 + k * stride]);
                pts.add(new double[]{x, y});
                y0 = Math.min(y0, y);
                y1 = Math.max(y1, y);
            }
        }
        int bins = 24;
        double[] lo = new double[bins], hi = new double[bins];
        java.util.Arrays.fill(lo, 1e30);
        java.util.Arrays.fill(hi, -1e30);
        for (double[] p : pts) {
            int b = Math.min(bins - 1, (int) ((p[1] - y0) / (y1 - y0) * bins));
            lo[b] = Math.min(lo[b], p[0]);
            hi[b] = Math.max(hi[b], p[0]);
        }
        double w = 0;
        for (int b = 0; b < bins; b++) if (hi[b] >= lo[b]) w = Math.max(w, hi[b] - lo[b]);
        return w / (y1 - y0);
    }

    private static double bladeOverLength(Path mesh) throws IOException {
        WeaponSizeRule.Measure m = WeaponSizeRule.measure(Files.readAllLines(mesh));
        return m.bladeWidth / m.length;
    }

    @Test
    void theBladeIsAboutOneThirtiethAsWideAsTheMeshIsLong() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String id : all()) {
            WeaponSizeRule.Measure m = WeaponSizeRule.measure(Files.readAllLines(mesh(id)));
            double wl = m.bladeWidth / m.length, dw = m.bladeDepth / m.bladeWidth;
            if (wl > 1.0 / 25.0 || wl < 1.0 / 60.0) bad.add(id + ": blade width / length 1/" + Math.round(1.0 / wl) + " (a hero katana is 1/60 to 1/25)");
            if (dw > 0.45) bad.add(id + ": thickness / width " + dw + " (a flat diamond section is at most a third, a ridge may add a little)");
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void noOrnamentWidensTheOutlineBeyondASmallTsuba() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String id : all()) {
            double w = outlineOverLength(mesh(id));
            if (w > 1.0 / 13.0) bad.add(id + ": the whole outline is 1/" + Math.round(1.0 / w) + " of the length across (wings, ribs, a crown or a ring; a true katana's tsuba is about 1/14)");
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void the461ThinnedBladesAreTheControl() throws IOException {
        // the negative control for the two checks above: the meshes of 4.61.0 (kept in classic_thin) fail at least one of them in 5 of the 6 cases (Hellblaze was a katana already, only its guard was wide)
        int fail = 0;
        for (String id : all()) {
            if (bladeOverLength(thin(id)) > 1.0 / 25.0 || outlineOverLength(thin(id)) > 1.0 / 13.0) fail++;
        }
        assertTrue(fail >= 5, fail + " of 6 old meshes are wide");
    }

    @Test
    void everyHeroKatanaKeepsItsOldMeshesAndSkins() throws IOException {
        List<String> missing = new ArrayList<>();
        for (String id : all()) {
            List<Path> keep = new ArrayList<>(List.of(thin(id), thinSkin(id, ""), thinSkin(id, "_glow")));
            if (id.equals(HELL)) keep.add(thinSkin(id, "_veins"));
            for (Path p : keep) if (!Files.isRegularFile(p) || Files.size(p) == 0) missing.add(p.toString());
            assertTrue(!Files.readString(mesh(id)).equals(Files.readString(thin(id))), id + ": the mesh is identical to the 4.61.0 copy");
            if (!id.equals(HELL)) {                                                                       // the file before 4.61.0 stays too (the classicVassalForms setting draws it)
                for (Path p : new Path[]{MESHES.resolve("classic_vassal/" + id + ".txt"), SKINS.resolve("classic_vassal/" + id + ".png"), SKINS.resolve("classic_vassal/" + id + "_glow.png")})
                    if (!Files.isRegularFile(p) || Files.size(p) == 0) missing.add(p.toString());
            }
        }
        for (String id : List.of(HELL + "_lod", "hellblaze_the_demon_sin__saya")) {
            for (Path p : new Path[]{MESHES.resolve("up/classic_thin/" + id + ".txt"), SKINS.resolve("up/classic_thin/" + id + ".png"), SKINS.resolve("up/classic_thin/" + id + "_glow.png")})
                if (!Files.isRegularFile(p) || Files.size(p) == 0) missing.add(p.toString());
        }
        assertEquals(List.of(), missing, "the 4.61.0 copy of a remodelled hero katana is gone");
    }

    @Test
    void thePlaceLineTheGlowTintAndTheHandStayTheSame() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String id : all()) {
            String[] now = header(mesh(id)), old = header(thin(id));
            // place: the origin and the angle never move (the display transforms hold the katana in the same hand spot); the scale is the old one except Hellblaze, whose in-hand length is pinned at 1.72
            String[] pn = now[0].split("\\s+"), po = old[0].split("\\s+");
            for (int i = 1; i <= 3; i++) if (!pn[i].equals(po[i])) bad.add(id + ": place field " + i + " moved: " + pn[i] + " (was " + po[i] + ")");
            if (!id.equals(HELL) && !pn[4].equals(po[4])) bad.add(id + ": the place scale moved: " + pn[4] + " (was " + po[4] + ")");
            if (!now[1].equals(old[1])) bad.add(id + ": the glow header moved: " + now[1] + " (was " + old[1] + ")");
            if (!now[2].equals("orient manual") || !old[2].equals("orient manual")) bad.add(id + ": orient manual is gone");
            if (now[3] == null) bad.add(id + ": no grip line");
        }
        assertEquals(List.of(), bad);
    }

    /** {place line, glow line, "orient manual" or "", grip line} of a mesh. */
    private static String[] header(Path mesh) throws IOException {
        String place = "", glow = "", orient = "", grip = null;
        for (String l : Files.readAllLines(mesh)) {
            if (l.startsWith("place ")) place = l.trim();
            else if (l.startsWith("glow ")) glow = l.trim();
            else if (l.startsWith("orient ")) orient = l.trim();
            else if (l.startsWith("grip ")) grip = l.trim();
            else if (l.startsWith("q ") || l.startsWith("qn ")) break;
        }
        return new String[]{place, glow, orient, grip};
    }

    @Test
    void theLoreIsStillOnTheWeapon() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String id : all()) {
            String text = Files.readString(mesh(id));
            for (String part : LORE.get(id)) if (!text.contains("# part " + part + " ")) bad.add(id + ": the part '" + part + "' is gone");
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void theNewMeshesAreInsideTheBudgets() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String id : all()) {
            int[] c = lines(mesh(id));
            int cap = id.equals(HELL) ? 1800 : 5000;                                                      // Hellblaze's visual-upgrade budget (HellblazeKatanaAssetsTest) is 1800
            if (c[0] + c[1] > cap) bad.add(id + ": " + (c[0] + c[1]) + " quad lines (cap " + cap + ")");
            if (c[0] < 400) bad.add(id + ": only " + c[0] + " body quads, not a model");
            long kb = (Files.size(skin(id, "")) + Files.size(skin(id, "_glow"))) / 1024;
            long cap350 = id.equals(HELL) ? (Files.size(thinSkin(id, "")) + Files.size(thinSkin(id, "_glow"))) / 1024 : 350;   // Hellblaze's 1024 px skin was never in the 350 KB class: it may not grow past the 4.61.0 one
            if (kb > cap350) bad.add(id + ": skins " + kb + " KB (cap " + cap350 + ")");
        }
        assertEquals(List.of(), bad);
    }
}
