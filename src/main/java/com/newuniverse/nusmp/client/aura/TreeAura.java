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
 * 1.93.0 Tree Magic: the temporary armour of the magic (Aura.TREE), the Heartwood armour. A set of eleven GeoLite pieces (tools/gen_tree_rebuild_geo.py) forged in and shattered out through
 * {@link EquipAnimation} like the Djinn Full Equip, plus a bark skin overlay over the body: a bark helm with antlers of living branches, a floating ring of leaves, layered bark plates with a
 * glowing heartwood tree-ring, burl pauldrons that sprout, root-fingered bracers, log greaves, a young tree growing from the back and four orbiting acorns. The other five nature magics have their own
 * painters ({@code VineAura}, {@code PlantAura}, {@code PoisonPlantAura}, {@code MudAura}, {@code MucusAura}) on the same kit ({@link TreeArmour}, {@link TreeAuraFx}).
 * Passes in order: the skin (cutout; translucent while fading), the pieces with their glow maps (cutout + additive), the heartwood skin glow (additive), then {@link TreeAuraFx}'s sprites (additive).
 */
public final class TreeAura {
    private TreeAura() {}

    private static final String K = "tree";
    private static final Piece[] SET = {
            piece(K, "helm", Part.HEAD, 0, 0f, -14f, 0f, HIDE_1P),
            piece(K, "crown", Part.HEAD, 12, 0f, -18f, 0f, GLOWS | HIDE_1P | DETAIL),
            piece(K, "chest", Part.BODY, 2, 0f, 0f, -12f, GLOWS),
            piece(K, "back", Part.BODY, 4, 0f, 0f, 14f, 0),
            piece(K, "pauld_r", Part.R_ARM, 3, -14f, -4f, 0f, 0),
            piece(K, "pauld_l", Part.L_ARM, 5, 14f, -4f, 0f, 0),
            piece(K, "bracer_r", Part.R_ARM, 6, -12f, 2f, -6f, 0),
            piece(K, "bracer_l", Part.L_ARM, 8, 12f, 2f, -6f, 0),
            piece(K, "greave_r", Part.R_LEG, 9, -3f, 14f, -6f, 0),
            piece(K, "greave_l", Part.L_LEG, 11, 3f, 14f, -6f, 0),
            piece(K, "aux", Part.BODY, 12, 0f, 10f, 0f, GLOWS | DETAIL),
    };
    private static final Style STYLE = new Style(0xFFC866, 0.85f, 0.12f, false);
    private static ResourceLocation skin, skinGlow;

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        skin = AuraRender.tex("aura/tree_skin");
        skinGlow = AuraRender.tex("aura/tree_skin_glow");
        PlayerAuraClient.register(Aura.TREE, TreeAura::paint);
    }

    private static void paint(AuraContext c) {
        if (c.fade() <= 0.002f) return;
        TreeArmour.skin(c, skin, skinGlow, 0xFFB050, 0.55f, false, 2, 0.04f);
        if (!TreeArmour.paint(c, SET, STYLE)) return;
        TreeAuraFx.sap(c, 0xFFD060, 0x9BE35A);
    }
}
