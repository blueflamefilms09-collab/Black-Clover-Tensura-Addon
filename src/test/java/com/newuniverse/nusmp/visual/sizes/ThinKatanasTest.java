package com.newuniverse.nusmp.visual.sizes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why (katanas v2, docs/heroweapon/katanas.md): the owner said the katanas looked thick, like broadswords. They were: the 1.110.0 size rule thickened every katana blade to about 1/6 of its length. The
 * katanas are real katanas now (a blade about 1/18 as wide as it is long, thickness a third of the width, a flat diamond section), so they are excluded from that rule and drawn as they were built.
 * Data tests over the REAL mesh files listed under {@code thin_katanas} in weapon_sizes.json: each one is thin, is not thickened again, keeps its old copies (rule 2) and stays inside the budgets.
 */
class ThinKatanasTest {
    private static final Path RES = Path.of("src/main/resources/assets/anime_multiverse");
    private static final Path MESHES = RES.resolve("weapon_meshes");
    private static final Path SKINS = RES.resolve("textures/entity/weapon");

    /** The wooden bokken never had a glow skin, so it has no old glow skin to keep. */
    private static final List<String> NEVER_GLOWED = List.of("selection_hopefuls_bokken");

    private static JsonObject sizes() throws IOException {
        return JsonParser.parseString(Files.readString(RES.resolve("weapon_sizes.json"))).getAsJsonObject();
    }

    private static List<String> thin() throws IOException {
        List<String> ids = new ArrayList<>();
        sizes().getAsJsonArray("thin_katanas").forEach(e -> ids.add(e.getAsString()));
        return ids;
    }

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

    @Test
    void fifteenKatanasAreListedAndTheyAreExcludedFromTheThickening() throws IOException {
        List<String> ids = thin();
        assertEquals(15, ids.size(), ids.toString());
        List<String> excluded = new ArrayList<>();
        sizes().getAsJsonArray("excluded").forEach(e -> excluded.add(e.getAsString()));
        weaponClassKatanasAreExcluded(ids, excluded);
    }

    private static void weaponClassKatanasAreExcluded(List<String> ids, List<String> excluded) throws IOException {
        JsonObject classes = JsonParser.parseString(Files.readString(RES.resolve("weapon_classes.json"))).getAsJsonObject().getAsJsonObject("weapons");
        for (String id : ids) {
            if (classes.has(id) && classes.get(id).getAsString().equals("katana")) assertTrue(excluded.contains(id), id + " is a katana-class weapon that the size rule would thicken again");
        }
    }

    @Test
    void noThinKatanaIsThickenedAgain() throws IOException {
        for (String id : thin()) {
            for (String dir : new String[]{"weapon_meshes_sz", "weapon_meshes_sz/up", "weapon_meshes_vu_sz"}) {
                assertTrue(!Files.exists(RES.resolve(dir + "/" + id + ".txt")), id + ": a thickened copy in " + dir + " would make it a broadsword again");
            }
        }
    }

    @Test
    void theBladeIsAboutOneEighteenthAsWideAsLongAndNotADeepSlab() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String id : thin()) {
            WeaponSizeRule.Measure m = WeaponSizeRule.measure(Files.readAllLines(MESHES.resolve(id + ".txt")));
            double wl = m.bladeWidth / m.length, dw = m.bladeDepth / m.bladeWidth;
            if (wl > 1.0 / 17.0 || wl < 1.0 / 60.0) bad.add(id + ": blade width / length 1/" + Math.round(1.0 / wl) + " (a katana is 1/60 to 1/17 of the whole mesh length)");
            if (dw > 0.66) bad.add(id + ": thickness / width " + dw + " (a katana's flat diamond is at most a third; a wooden bokken's oval 0.66)");
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void theThickenedOldBladesAreTheBroadOnesThatWereTheProblem() throws IOException {
        // the negative control for the check above: the thickened copies the game drew until now (kept in classic_vassal) fail it
        int broad = 0, total = 0;
        for (String id : thin()) {
            Path old = RES.resolve("weapon_meshes_sz/classic_vassal/" + id + ".txt");
            if (!Files.exists(old)) continue;
            WeaponSizeRule.Measure m = WeaponSizeRule.measure(Files.readAllLines(old));
            total++;
            if (m.bladeWidth / m.length > 1.0 / 17.0) broad++;
        }
        assertTrue(total >= 10 && broad * 10 >= total * 8, broad + " of " + total + " old thickened blades are broad");
    }

    @Test
    void everyThinKatanaKeepsItsOldMeshAndSkins() throws IOException {
        List<String> missing = new ArrayList<>();
        for (String id : thin()) {
            List<Path> keep = new ArrayList<>(List.of(MESHES.resolve("classic_vassal/" + id + ".txt"), SKINS.resolve("classic_vassal/" + id + ".png")));
            if (!NEVER_GLOWED.contains(id)) keep.add(SKINS.resolve("classic_vassal/" + id + "_glow.png"));
            for (Path p : keep) {
                if (!Files.isRegularFile(p) || Files.size(p) == 0) missing.add(p.toString());
            }
            assertTrue(!Files.readString(MESHES.resolve(id + ".txt")).equals(Files.readString(MESHES.resolve("classic_vassal/" + id + ".txt"))), id + ": the mesh is identical to the classic copy");
        }
        assertEquals(List.of(), missing, "the old copy of a rebuilt katana is gone");
    }

    @Test
    void theNewKatanasAreInsideTheBudgets() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String id : thin()) {
            int[] c = lines(MESHES.resolve(id + ".txt"));
            if (c[0] + c[1] > 5000) bad.add(id + ": " + (c[0] + c[1]) + " quad lines");
            if (c[0] < 500) bad.add(id + ": only " + c[0] + " body quads, not a model");
            Path glow = SKINS.resolve(id + "_glow.png");
            long kb = (Files.size(SKINS.resolve(id + ".png")) + (Files.exists(glow) ? Files.size(glow) : 0)) / 1024;
            if (kb > 350) bad.add(id + ": skins " + kb + " KB");
        }
        assertEquals(List.of(), bad);
    }
}
