package com.newuniverse.nusmp.visual.shields;

import java.util.List;
import java.util.Map;

/**
 * 1.110.0: the display transforms of vanilla's shield, the one preset every shield-class item of the mod shares (tools/gen_shield_models.py writes the model jsons
 * from the same numbers; ShieldModelsTest reads the shipped jsons back and compares them with this class). Written out from Minecraft 1.21.1's
 * {@code assets/minecraft/models/item/shield.json} and {@code shield_blocking.json} (rotation degrees, translation in sixteenths of a block, scale).
 * <p>
 * Two entries differ from vanilla on purpose: {@code gui} (vanilla tilts the entity model; the mod's inventory icon is the flat sprite, so it stays an identity)
 * and {@code head} (vanilla has none, so the identity). No Minecraft classes.
 */
public final class ShieldPreset {
    private ShieldPreset() {}

    /** The display contexts a shield model states, in the order the generator writes them. */
    public static final List<String> CONTEXTS = List.of("thirdperson_righthand", "thirdperson_lefthand", "firstperson_righthand", "firstperson_lefthand", "gui", "fixed", "ground", "head");

    /** The contexts whose numbers are exactly vanilla's (everything but the two noted above). */
    public static final List<String> VANILLA_CONTEXTS = List.of("thirdperson_righthand", "thirdperson_lefthand", "firstperson_righthand", "firstperson_lefthand", "fixed", "ground");

    /** The contexts that differ between the raised and the lowered shield. */
    public static final List<String> HAND_CONTEXTS = List.of("thirdperson_righthand", "thirdperson_lefthand", "firstperson_righthand", "firstperson_lefthand");

    /** How far a shipped number may be from the preset's (rotation degrees, translation sixteenths, scale). */
    public static final double TOLERANCE = 0.01;

    /** {rotation xyz, translation xyz, scale xyz} per context: minecraft:item/shield. */
    public static final Map<String, double[]> SHIELD = Map.of(
            "thirdperson_righthand", row(0, 90, 0, 10, 6, -4, 1.0),
            "thirdperson_lefthand", row(0, 90, 0, 10, 6, 12, 1.0),
            "firstperson_righthand", row(0, 180, 5, -10, 2, 0, 1.25),
            "firstperson_lefthand", row(0, 180, 5, 10, 0, 0, 1.25),
            "gui", row(0, 0, 0, 0, 0, 0, 1.0),
            "fixed", row(0, 180, 0, -2, 4, -5, 0.5),
            "ground", row(0, 0, 0, 4, 4, 2, 0.25),
            "head", row(0, 0, 0, 0, 0, 0, 1.0));

    /** minecraft:item/shield_blocking: the same, but the four hand contexts hold the shield up. */
    public static final Map<String, double[]> BLOCKING = Map.of(
            "thirdperson_righthand", row(45, 135, 0, 3.51, 11, -2, 1.0),
            "thirdperson_lefthand", row(45, 135, 0, 13.51, 3, 5, 1.0),
            "firstperson_righthand", row(0, 180, -5, -15, 5, -11, 1.25),
            "firstperson_lefthand", row(0, 180, -5, 5, 5, -11, 1.25),
            "gui", row(0, 0, 0, 0, 0, 0, 1.0),
            "fixed", row(0, 180, 0, -2, 4, -5, 0.5),
            "ground", row(0, 0, 0, 4, 4, 2, 0.25),
            "head", row(0, 0, 0, 0, 0, 0, 1.0));

    private static double[] row(double rx, double ry, double rz, double tx, double ty, double tz, double s) {
        return new double[]{rx, ry, rz, tx, ty, tz, s, s, s};
    }

    /** The preset row of a context for the lowered ({@code blocking == false}) or the raised shield; null for an unknown context. */
    public static double[] of(String context, boolean blocking) { return (blocking ? BLOCKING : SHIELD).get(context); }

    /** True when every number of {@code got} (nine values) is within {@link #TOLERANCE} of {@code want}. */
    public static boolean within(double[] want, double[] got) {
        if (want == null || got == null || want.length != 9 || got.length != 9) return false;
        for (int i = 0; i < 9; i++) if (!(Math.abs(want[i] - got[i]) <= TOLERANCE)) return false;
        return true;
    }
}
