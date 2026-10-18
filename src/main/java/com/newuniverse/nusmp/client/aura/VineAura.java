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
 * 1.93.0 Vine Magic: the temporary armour of the magic (Aura.VINE), the Thornbine armour. Eleven GeoLite pieces (tools/gen_vine_rebuild_geo.py), every one of which GROWS in from below (the
 * fly-in offsets point down the model: the vines climb the body from the ground) and forges and shatters through {@link EquipAnimation} like the Djinn Full Equip: a braided circlet with tendril
 * horns, a ring of thorns and buds above the head, criss-cross bands with a glowing bud, a fan of leaves on each shoulder, spiral bracers with groping tendril fingers, wrapped greaves, four
 * jointed vine whips arching over the shoulders and five thorn seeds spiralling round the body, over a vine-strand skin. Passes in order: the skin (cutout; translucent while fading), the pieces
 * with the glow maps of the buds (cutout + additive), the skin glow (additive), then the sprites of {@link TreeAuraFx} (leaves, lime motes, a thorn-ring sigil, additive).
 */
public final class VineAura {
    private VineAura() {}

    private static final String K = "vine";
    private static final Piece[] SET = {
            piece(K, "helm", Part.HEAD, 0, 0f, 16f, 0f, GLOWS | HIDE_1P),
            piece(K, "crown", Part.HEAD, 12, 0f, 22f, 0f, GLOWS | HIDE_1P | DETAIL),
            piece(K, "chest", Part.BODY, 2, 0f, 14f, -4f, GLOWS),
            piece(K, "back", Part.BODY, 4, 0f, 18f, 8f, 0),
            piece(K, "pauld_r", Part.R_ARM, 3, -4f, 14f, 0f, 0),
            piece(K, "pauld_l", Part.L_ARM, 5, 4f, 14f, 0f, 0),
            piece(K, "bracer_r", Part.R_ARM, 6, -2f, 16f, 0f, 0),
            piece(K, "bracer_l", Part.L_ARM, 8, 2f, 16f, 0f, 0),
            piece(K, "greave_r", Part.R_LEG, 9, 0f, 14f, 0f, 0),
            piece(K, "greave_l", Part.L_LEG, 11, 0f, 14f, 0f, 0),
            piece(K, "aux", Part.BODY, 12, 0f, 14f, 0f, GLOWS | DETAIL),
    };
    private static final Style STYLE = new Style(0xB6FF5A, 0.9f, 0.14f, false);
    private static ResourceLocation skin, skinGlow;

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        skin = AuraRender.tex("aura/vine_skin");
        skinGlow = AuraRender.tex("aura/vine_skin_glow");
        PlayerAuraClient.register(Aura.VINE, VineAura::paint);
    }

    private static void paint(AuraContext c) {
        if (c.fade() <= 0.002f) return;
        TreeArmour.skin(c, skin, skinGlow, 0xB6FF5A, 0.4f, false, 2, 0.04f);
        if (!TreeArmour.paint(c, SET, STYLE)) return;
        TreeAuraFx.sprites(c, AuraRender.tex("particle/vine_leaf"), false, 0x6DB743, 0xA8E05A, 8, 0.7f, -26f, 0.09f, 0.08f);
        TreeAuraFx.sprites(c, AuraRender.tex("particle/vine_mote"), true, 0xB6FF5A, 0xEFFFB0, 9, 0.6f, 50f, 0.06f, 0.1f);
        TreeAuraFx.sigil(c, AuraRender.tex("particle/vine_sigil"), 0x9CE84A, 1.0f, 0.75f);
    }
}
