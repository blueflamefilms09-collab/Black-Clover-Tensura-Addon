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
 * 1.93.0 Poison Plant Magic: the temporary armour of the magic (Aura.POISON_PLANT), the Venomflora armour. Eleven GeoLite pieces (tools/gen_poison_plant_rebuild_geo.py) that LUNGE in from the sides
 * like snapping traps (large sideways fly-in offsets) and forge and shatter through {@link EquipAnimation} like the Djinn Full Equip: a flytrap hood whose two toothed lobes open and snap, a ring of
 * glowing spore pods, three swollen gourd pods on the chest with glowing veins, spiked pod pauldrons, toxic leaf bracers with hooked venom claws, bulb greaves, a pitcher plant behind the back with an
 * opening lid and a venom pool, and four low spore pods, over a mottled violet skin with acid veins. Passes in order: the skin (cutout; translucent while fading), the pieces with the glow of veins
 * and venom (cutout + additive), the skin glow (additive), then the sprites of {@link TreeAuraFx} (rising spores, dripping venom, a toxic sigil, additive or translucent).
 */
public final class PoisonPlantAura {
    private PoisonPlantAura() {}

    private static final String K = "poison_plant";
    private static final Piece[] SET = {
            piece(K, "helm", Part.HEAD, 0, 0f, -10f, 10f, GLOWS | HIDE_1P),
            piece(K, "crown", Part.HEAD, 12, 0f, -16f, 0f, GLOWS | HIDE_1P | DETAIL),
            piece(K, "chest", Part.BODY, 2, 0f, 0f, -14f, GLOWS),
            piece(K, "back", Part.BODY, 4, 0f, 6f, 16f, GLOWS),
            piece(K, "pauld_r", Part.R_ARM, 3, -16f, -2f, 0f, 0),
            piece(K, "pauld_l", Part.L_ARM, 5, 16f, -2f, 0f, 0),
            piece(K, "bracer_r", Part.R_ARM, 6, -14f, 0f, -6f, 0),
            piece(K, "bracer_l", Part.L_ARM, 8, 14f, 0f, -6f, 0),
            piece(K, "greave_r", Part.R_LEG, 9, -10f, 8f, -4f, 0),
            piece(K, "greave_l", Part.L_LEG, 11, 10f, 8f, -4f, 0),
            piece(K, "aux", Part.BODY, 12, 0f, 8f, 0f, GLOWS | DETAIL),
    };
    private static final Style STYLE = new Style(0xB8FF30, 0.9f, 0.16f, false);
    private static ResourceLocation skin, skinGlow;

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        skin = AuraRender.tex("aura/poison_plant_skin");
        skinGlow = AuraRender.tex("aura/poison_plant_skin_glow");
        PlayerAuraClient.register(Aura.POISON_PLANT, PoisonPlantAura::paint);
    }

    private static void paint(AuraContext c) {
        if (c.fade() <= 0.002f) return;
        TreeArmour.skin(c, skin, skinGlow, 0xB8FF30, 0.5f, false, 2, 0.04f);
        if (!TreeArmour.paint(c, SET, STYLE)) return;
        TreeAuraFx.sprites(c, AuraRender.tex("particle/poison_plant_spore"), true, 0xB8FF30, 0xE8FF9A, 10, 0.65f, 40f, 0.08f, 0.12f);
        TreeAuraFx.sprites(c, AuraRender.tex("particle/poison_plant_drop"), false, 0x8AE020, 0xC8FF60, 6, 0.55f, -34f, 0.06f, 0.0f);
        TreeAuraFx.sigil(c, AuraRender.tex("particle/poison_plant_sigil"), 0xA6F020, 1.05f, 0.8f);
    }
}
