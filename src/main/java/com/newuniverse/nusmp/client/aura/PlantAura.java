package com.newuniverse.nusmp.client.aura;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.client.aura.TreeArmour.Part;
import com.newuniverse.nusmp.client.aura.TreeArmour.Piece;
import com.newuniverse.nusmp.client.aura.TreeArmour.Style;
import net.minecraft.resources.ResourceLocation;

import static com.newuniverse.nusmp.client.aura.TreeArmour.DETAIL;
import static com.newuniverse.nusmp.client.aura.TreeArmour.GLOWS;
import static com.newuniverse.nusmp.client.aura.TreeArmour.HIDE_1P;
import static com.newuniverse.nusmp.client.aura.TreeArmour.piece;

/**
 * 1.93.0 Plant Magic: the temporary armour of the magic (Aura.PLANT), the Blossom armour. Eleven GeoLite pieces (tools/gen_plant_rebuild_geo.py) that DRIFT in from above and to the sides like
 * falling petals (the fly-in offsets point up and outwards) and forge and shatter through {@link EquipAnimation} like the Djinn Full Equip: a flower crown with leaf ribbons, a halo of eight turning
 * petals, a leaf bodice with a great golden-hearted blossom and a flaring petal skirt, a half-open bloom on each shoulder, petal-cuffed leaf sleeves, wrapped greaves, two pairs of petal wings that
 * beat, and six small blossoms drifting round the body, over a leaf skin. Passes in order: the skin (cutout; translucent while fading), the pieces with the glow of the golden hearts (cutout +
 * additive), the skin glow (additive), then the sprites of {@link TreeAuraFx} (falling petals, rising pollen, a flower sigil on the ground, additive or translucent).
 */
public final class PlantAura {
    private PlantAura() {}

    private static final String K = "plant";
    private static final Piece[] SET = {
            piece(K, "helm", Part.HEAD, 0, 2f, -16f, 0f, HIDE_1P),
            piece(K, "crown", Part.HEAD, 12, 0f, -22f, 0f, GLOWS | HIDE_1P | DETAIL),
            piece(K, "chest", Part.BODY, 2, 0f, -12f, -6f, GLOWS),
            piece(K, "back", Part.BODY, 4, 0f, -8f, 14f, 0),
            piece(K, "pauld_r", Part.R_ARM, 3, -8f, -14f, 0f, 0),
            piece(K, "pauld_l", Part.L_ARM, 5, 8f, -14f, 0f, 0),
            piece(K, "bracer_r", Part.R_ARM, 6, -6f, -10f, -4f, 0),
            piece(K, "bracer_l", Part.L_ARM, 8, 6f, -10f, -4f, 0),
            piece(K, "greave_r", Part.R_LEG, 9, -4f, -12f, -2f, 0),
            piece(K, "greave_l", Part.L_LEG, 11, 4f, -12f, -2f, 0),
            piece(K, "aux", Part.BODY, 12, 0f, -10f, 0f, DETAIL),
    };
    private static final Style STYLE = new Style(0xFFE690, 0.8f, 0.12f, false);
    private static ResourceLocation skin, skinGlow;

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        skin = AuraRender.tex("aura/plant_skin");
        skinGlow = AuraRender.tex("aura/plant_skin_glow");
        PlayerAuraClient.register(Aura.PLANT, PlantAura::paint);
    }

    private static void paint(AuraContext c) {
        if (c.fade() <= 0.002f) return;
        TreeArmour.skin(c, skin, skinGlow, 0xFFE690, 0.35f, false, 2, 0.04f);
        if (!TreeArmour.paint(c, SET, STYLE)) return;
        TreeAuraFx.sprites(c, AuraRender.tex("particle/plant_petal"), false, 0xF6A5C8, 0xFFF0F6, 9, 0.75f, -24f, 0.1f, 0.07f);
        TreeAuraFx.sprites(c, AuraRender.tex("particle/plant_mote"), true, 0xFFE070, 0xFFF6C0, 9, 0.6f, 45f, 0.06f, 0.1f);
        TreeAuraFx.sigil(c, AuraRender.tex("particle/plant_sigil"), 0xFFB6D8, 1.0f, 0.75f);
    }
}
