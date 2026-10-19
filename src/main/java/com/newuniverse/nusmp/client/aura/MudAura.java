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
 * 1.93.0 Mud Magic: the temporary armour of the magic (Aura.MUD), the Bogplate armour. Eleven GeoLite pieces (tools/gen_mud_rebuild_geo.py) that DROP onto the body like thrown clods (the fly-in
 * offsets point straight up the model, large and heavy) and forge and shatter through {@link EquipAnimation} like the Djinn Full Equip: a thick cowl with a visor slit and amber eyes, orbiting
 * clods, three cracked slabs with amber seams and a pectoral boulder, big mounds on the shoulders, massive gauntlet fists, wading boots with a splash flare, a hunched hump with clod hands on the
 * back, and four orbiting globs, over a wet mud skin. Passes in order: the skin (cutout; translucent while fading), the pieces with the glow of the cracks (cutout + additive), the skin glow
 * (additive: the amber seams), then the sprites of {@link TreeAuraFx} (falling droplets, rising dust, a cracked-earth sigil under the feet).
 */
public final class MudAura {
    private MudAura() {}

    private static final String K = "mud";
    private static final Piece[] SET = {
            piece(K, "helm", Part.HEAD, 0, 0f, -18f, 0f, HIDE_1P),
            piece(K, "crown", Part.HEAD, 12, 0f, -24f, 0f, GLOWS | HIDE_1P | DETAIL),
            piece(K, "chest", Part.BODY, 2, 0f, -20f, -4f, GLOWS),
            piece(K, "back", Part.BODY, 4, 0f, -22f, 8f, GLOWS),
            piece(K, "pauld_r", Part.R_ARM, 3, -4f, -22f, 0f, 0),
            piece(K, "pauld_l", Part.L_ARM, 5, 4f, -22f, 0f, 0),
            piece(K, "bracer_r", Part.R_ARM, 6, -2f, -20f, -2f, 0),
            piece(K, "bracer_l", Part.L_ARM, 8, 2f, -20f, -2f, 0),
            piece(K, "greave_r", Part.R_LEG, 9, 0f, -18f, 0f, 0),
            piece(K, "greave_l", Part.L_LEG, 11, 0f, -18f, 0f, 0),
            piece(K, "aux", Part.BODY, 12, 0f, -18f, 0f, GLOWS | DETAIL),
    };
    private static final Style STYLE = new Style(0xFF9A3C, 0.75f, 0.1f, false);
    private static ResourceLocation skin, skinGlow;

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        skin = AuraRender.tex("aura/mud_skin");
        skinGlow = AuraRender.tex("aura/mud_skin_glow");
        PlayerAuraClient.register(Aura.MUD, MudAura::paint);
    }

    private static void paint(AuraContext c) {
        if (c.fade() <= 0.002f) return;
        TreeArmour.skin(c, skin, skinGlow, 0xFF9A3C, 0.6f, false, 2, 0.045f);
        if (!TreeArmour.paint(c, SET, STYLE)) return;
        TreeAuraFx.sprites(c, AuraRender.tex("particle/mud_drip"), false, 0x6B4429, 0x8A593D, 8, 0.6f, -36f, 0.07f, 0.0f);
        TreeAuraFx.sprites(c, AuraRender.tex("particle/mud_mist"), false, 0x8A6A4A, 0xB08C66, 6, 0.75f, 22f, 0.16f, 0.03f);
        TreeAuraFx.sigil(c, AuraRender.tex("particle/mud_sigil"), 0xFF9A3C, 1.05f, 0.7f);
    }
}
