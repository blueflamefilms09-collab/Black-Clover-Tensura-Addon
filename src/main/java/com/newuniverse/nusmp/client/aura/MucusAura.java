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
 * 1.93.0 Mucus Magic: the temporary armour of the magic (Aura.MUCUS), the Gelplate armour. Eleven GeoLite pieces (tools/gen_mucus_rebuild_geo.py), ALL on the alpha-blended render layer (gel is
 * see-through; the translucent flag of the {@link Style} keeps even a fully formed piece off the alpha-tested layer) that SLIDE onto the body from the inside of each limb (small sideways fly-in
 * offsets, like a coat of slime spreading) and forge and shatter through {@link EquipAnimation} like the Djinn Full Equip: a gel dome with a glowing nucleus and dripping strings, eight bubbles
 * circling above the head, a swollen bladder on the chest with its nucleus visible through the layers, stacked wobbling blobs on the shoulders, gel gloves, puddle boots, four waving pseudopod
 * tendrils behind and six orbiting bubbles, over a gel skin. Passes in order: the skin (translucent), the pieces (translucent) with the glow of the nuclei and rims (additive), the skin glow
 * (additive), then the sprites of {@link TreeAuraFx} (rising bubbles, falling drips, a ring of gel on the ground, translucent / additive).
 */
public final class MucusAura {
    private MucusAura() {}

    private static final String K = "mucus";
    private static final Piece[] SET = {
            piece(K, "helm", Part.HEAD, 0, 0f, -8f, -6f, GLOWS | HIDE_1P),
            piece(K, "crown", Part.HEAD, 12, 0f, -12f, 0f, GLOWS | HIDE_1P | DETAIL),
            piece(K, "chest", Part.BODY, 2, 0f, 0f, -8f, GLOWS),
            piece(K, "back", Part.BODY, 4, 0f, 0f, 10f, GLOWS),
            piece(K, "pauld_r", Part.R_ARM, 3, 6f, -4f, 0f, GLOWS),
            piece(K, "pauld_l", Part.L_ARM, 5, -6f, -4f, 0f, GLOWS),
            piece(K, "bracer_r", Part.R_ARM, 6, 6f, 0f, 0f, 0),
            piece(K, "bracer_l", Part.L_ARM, 8, -6f, 0f, 0f, 0),
            piece(K, "greave_r", Part.R_LEG, 9, 4f, 4f, 0f, 0),
            piece(K, "greave_l", Part.L_LEG, 11, -4f, 4f, 0f, 0),
            piece(K, "aux", Part.BODY, 12, 0f, 4f, 0f, GLOWS | DETAIL),
    };
    private static final Style STYLE = new Style(0x8CFFE0, 0.7f, 0.15f, true);
    private static ResourceLocation skin, skinGlow;

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        skin = AuraRender.tex("aura/mucus_skin");
        skinGlow = AuraRender.tex("aura/mucus_skin_glow");
        PlayerAuraClient.register(Aura.MUCUS, MucusAura::paint);
    }

    private static void paint(AuraContext c) {
        if (c.fade() <= 0.002f) return;
        TreeArmour.skin(c, skin, skinGlow, 0x8CFFE0, 0.35f, true, 2, 0.045f);
        if (!TreeArmour.paint(c, SET, STYLE)) return;
        TreeAuraFx.sprites(c, AuraRender.tex("particle/mucus_bubble"), false, 0xA6F5E0, 0xE6FFF8, 9, 0.62f, 38f, 0.09f, 0.0f);
        TreeAuraFx.sprites(c, AuraRender.tex("particle/mucus_drip"), false, 0x72D6B4, 0xB0F0DC, 6, 0.55f, -32f, 0.06f, 0.0f);
        TreeAuraFx.sigil(c, AuraRender.tex("particle/mucus_sigil"), 0x72D6B4, 1.05f, 0.7f);
    }
}
