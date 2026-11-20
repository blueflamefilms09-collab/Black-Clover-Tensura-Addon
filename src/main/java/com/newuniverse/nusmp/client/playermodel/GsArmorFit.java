package com.newuniverse.nusmp.client.playermodel;

import java.util.List;
import java.util.Set;

/**
 * Why: the armour fit of the Goku and Sinbad looks (docs/boss_redesign_goku_sinbad.md, "Armour overlap"), as a pure table and a few pure box checks (no game class, so the unit test and the preview tool read
 * it). Two questions, answered with numbers:
 * <ul>
 *   <li><b>Does an outer-layer cube of a look sit inside the body it is drawn on?</b> {@link #base} gives the vanilla cube of each bone in the geo's own coordinates (y up, the pivot rows of the kit's
 *       bones), {@link #inflated} the skin's second layer (+0.25 body, arms, legs, +0.5 head) and the player armour layers (+0.5 inner, +1.0 outer); {@link #sitsInside} and {@link #zFights} say whether a
 *       look cube is buried in such a box or shares a face plane with it. The test runs every cube of every {@code pmx_gs_*} geo file through them.</li>
 *   <li><b>Which pieces of the player's Djinn Equip does the look already wear?</b> The 7 Sinbad bodies carry their own crown, collar and chains, belt, cuffs, shoulders and sandals
 *       ({@code gsbosses.LOOKS_S}), so the player's pieces of the same names are drawn over them and clip. {@link #hiddenPieces} lists, per synced equip number, the piece file names
 *       ({@code DjinnMeshes.Piece#file}) that are forged in and then dissolve into the look; wings, tail, halo, gauntlets, ears, mantle, the piece behind the back and the weapon are not in the list
 *       because the looks have nothing there.</li>
 * </ul>
 * Client option {@code bossLooks.bossArmorFit} (default on) switches the hiding off. Add-only: a new djinn appends a row.
 */
public final class GsArmorFit {
    private GsArmorFit() {}

    /** An axis-aligned box in the geo's coordinates: {minX, minY, minZ, maxX, maxY, maxZ}. */
    public record Box(double x0, double y0, double z0, double x1, double y1, double z1) {
        public double volume() { return Math.max(0, x1 - x0) * Math.max(0, y1 - y0) * Math.max(0, z1 - z0); }
    }

    public static final double SKIN_LAYER = 0.25, HAT_LAYER = 0.5, ARMOR_INNER = 0.5, ARMOR_OUTER = 1.0;

    /** The vanilla cube of a kit bone ({@code x_head, x_body, x_arm_r, x_arm_l, x_leg_r, x_leg_l}), or null for a bone with no body cube. */
    public static Box base(String bone, boolean slim) {
        double aw = slim ? 3 : 4;
        return switch (bone) {
            case "x_head" -> new Box(-4, 24, -4, 4, 32, 4);
            case "x_body" -> new Box(-4, 12, -2, 4, 24, 2);
            case "x_arm_r" -> new Box(-4 - aw, 12, -2, -4, 24, 2);
            case "x_arm_l" -> new Box(4, 12, -2, 4 + aw, 24, 2);
            case "x_leg_r" -> new Box(-4, 0, -2, 0, 12, 2);
            case "x_leg_l" -> new Box(0, 0, -2, 4, 12, 2);
            default -> null;
        };
    }

    public static Box inflated(Box b, double g) { return new Box(b.x0 - g, b.y0 - g, b.z0 - g, b.x1 + g, b.y1 + g, b.z1 + g); }

    /** True when the whole cube lies inside the container (hidden under it, or sharing its faces). */
    public static boolean sitsInside(Box cube, Box container) {
        double e = 1e-6;
        return cube.x0 >= container.x0 - e && cube.y0 >= container.y0 - e && cube.z0 >= container.z0 - e
                && cube.x1 <= container.x1 + e && cube.y1 <= container.y1 + e && cube.z1 <= container.z1 + e;
    }

    /**
     * True when a face of the cube lies within {@code tol} of a face plane of the container and the two overlap in area on that plane while the cube crosses into the container: two surfaces at one
     * depth, which z-fight. A cube that only touches the container from outside (its face on the plane, its body outside) is not a fight and is not reported.
     */
    public static boolean zFights(Box cube, Box container, double tol) {
        double[] c0 = {cube.x0, cube.y0, cube.z0}, c1 = {cube.x1, cube.y1, cube.z1};
        double[] b0 = {container.x0, container.y0, container.z0}, b1 = {container.x1, container.y1, container.z1};
        for (int ax = 0; ax < 3; ax++) {
            boolean area = true, crosses = c1[ax] > b0[ax] + tol && c0[ax] < b1[ax] - tol;
            for (int o = 0; o < 3; o++) if (o != ax && Math.min(c1[o], b1[o]) - Math.max(c0[o], b0[o]) <= 0.05) area = false;
            if (!area || !crosses) continue;
            for (double plane : new double[]{b0[ax], b1[ax]})
                if (Math.abs(c0[ax] - plane) <= tol || Math.abs(c1[ax] - plane) <= tol) return true;
        }
        return false;
    }

    /** The synced equip number the table rows are indexed by (0 none, then Baal, Valefor, Zepar, Focalor, Vepar, Crocell, Furfur: {@link GsLooks#SINBAD}). */
    private static final Set<String> COMMON = Set.of("helm", "crest", "chest", "belt", "vambrace_l", "vambrace_r", "greave_l", "greave_r");
    private static final Set<String> SHOULDERS = Set.of("pauldron_l", "pauldron_r"), ANTLERS = Set.of("horn_l", "horn_r");

    /**
     * The piece file names the look of this equip number already wears (so the player's piece of that name is not left standing over it): crown (helm, brow crest), collar and chains (chest), belt,
     * cuffs (vambraces) and sandals (greaves) on every body; the shoulder plates on every body but Zepar and Furfur (their look has none); the gold antlers of Baal's look stand in for the horns.
     */
    public static Set<String> hiddenPieces(int equipIdx) {
        if (equipIdx <= 0 || equipIdx >= GsLooks.SINBAD.size()) return Set.of();
        java.util.HashSet<String> s = new java.util.HashSet<>(COMMON);
        boolean noShoulders = equipIdx == 3 || equipIdx == 7;                             // Zepar, Furfur
        if (!noShoulders) s.addAll(SHOULDERS);
        if (equipIdx == 1) s.addAll(ANTLERS);                                              // Baal
        return Set.copyOf(s);
    }

    /** The bone each hideable piece is drawn on (the anchor of {@code DjinnArmorLayer}), so a test can ask whether the look has geometry there. */
    public static String boneOf(String piece) {
        return switch (piece) {
            case "helm", "crest", "horn_l", "horn_r", "ears" -> "x_head";
            case "chest", "belt", "mantle" -> "x_body";
            case "pauldron_r", "vambrace_r", "gauntlet_r" -> "x_arm_r";
            case "pauldron_l", "vambrace_l", "gauntlet_l" -> "x_arm_l";
            case "greave_r" -> "x_leg_r";
            case "greave_l" -> "x_leg_l";
            default -> null;
        };
    }

    /** All piece names the table can ever hide (for the test). */
    public static List<String> hideable() {
        return List.of("helm", "crest", "horn_l", "horn_r", "chest", "belt", "pauldron_l", "pauldron_r", "vambrace_l", "vambrace_r", "greave_l", "greave_r");
    }
}
