package com.newuniverse.nusmp.client.aura;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.client.aura.WaterAuraRig.Anchor;
import com.newuniverse.nusmp.client.aura.WaterAuraRig.Piece;
import com.newuniverse.nusmp.fairytail.FtForms;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Why: the timed forms of the Fairy Tail skills (Dragon Force, Lightning Flame Dragon Mode, Fire Dragon King's Mode, Ice Devil's Rage, the Sky
 * Dragon's Armor, Lightning Body, Iron Dragon Scales, the Shadow Iron Dragon, Water Body, Rain Woman) are shown as a temporary armour on the
 * wearer's body, built to the standard the Djinn full equip set: every form is a SET of seven geo pieces (helm, chest, two pauldrons, two
 * vambraces and an accent: wings, a tail, a mantle, a plume or a veil) with a glow map, each anchored on the model part it was made for and
 * forged in and shattered out by {@link EquipAnimation} through {@link WaterAuraRig}. Under the plates sits a body shell in the form's material
 * (and an additive glow shell on top), so the whole body reads as the form. The style number is the form's index in {@code fairytail.FtForms};
 * the server sets it with {@code PlayerAuras.set(player, Aura.FAIRY_TAIL, ticks, index)} when the form is cast and clears it when the form ends.
 * Models and textures: tools/gen_fairy_tail_geo.py (geo/entity/ft_&lt;form&gt;_arm_&lt;piece&gt;, textures/aura/ft_&lt;form&gt;_shell).
 */
public final class FairyTailAura {
    private FairyTailAura() {}

    private static final WaterAuraRig.Set[] SETS = new WaterAuraRig.Set[FtForms.ALL.size()];
    private static final ResourceLocation[] SHELL = new ResourceLocation[FtForms.ALL.size()], SHELL_GLOW = new ResourceLocation[FtForms.ALL.size()];

    static {
        for (int i = 0; i < FtForms.ALL.size(); i++) {
            FtForms f = FtForms.ALL.get(i);
            SETS[i] = new WaterAuraRig.Set(f.geoKey(),
                    Piece.of("chest", Anchor.BODY, 1, 0, 0, -14),
                    Piece.of("pauldron_r", Anchor.ARM_R, 2, -14, -4, 0),
                    Piece.of("pauldron_l", Anchor.ARM_L, 2, 14, -4, 0),
                    Piece.of("vambrace_r", Anchor.ARM_R, 4, -12, 2, -6).glow(0.6f),
                    Piece.of("vambrace_l", Anchor.ARM_L, 4, 12, 2, -6).glow(0.6f),
                    Piece.of("helm", Anchor.HEAD, 0, 0, -16, 0).head(),
                    Piece.of("accent", Anchor.BODY, 8, 0, -4, 12).clip("idle", 1f).glass().detail());
            SHELL[i] = AuraRender.tex("aura/ft_" + f.key() + "_shell");
            SHELL_GLOW[i] = AuraRender.tex("aura/ft_" + f.key() + "_shell_glow");
        }
    }

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        PlayerAuraClient.register(Aura.FAIRY_TAIL, FairyTailAura::paint);
    }

    private static void paint(AuraContext c) {
        int i = c.style();
        if (i < 0 || i >= SETS.length || c.fade() <= 0.002f) return;
        FtForms f = FtForms.ALL.get(i);
        float age = c.age(), pulse = 0.78f + 0.22f * Mth.sin(age * 0.23f) + 0.1f * Mth.sin(age * 0.71f);
        float forge = WaterAuraRig.forge(c, 0f);
        boolean veil = f.key().equals("water_body") || f.key().equals("rain_woman");
        int rgb = f.element().argb & 0xFFFFFF;
        // 1. the body shell in the form's material (translucent for the water forms), then its glow on top of it
        WaterAuraRig.shell(c, AuraRender.translucent(SHELL[i]), 1.04f, 1.025f, 1.04f, AuraRender.alpha(0xFFFFFFFF, (veil ? 0.55f : 0.9f) * forge), false, true);
        WaterAuraRig.shell(c, AuraRender.additive(SHELL_GLOW[i]), 1.045f, 1.03f, 1.045f, AuraRender.alpha(0xFF000000 | rgb, 0.65f * pulse * forge), true, true);
        // 2. the armour pieces and their glow maps
        WaterAuraRig.draw(c, SETS[i], rgb, 0.8f * pulse + 0.35f);
        // 3. a faint halo of the element round everything, last
        WaterAuraRig.shell(c, AuraRender.additive(SHELL_GLOW[i]), 1.09f + 0.01f * Mth.sin(age * 0.2f), 1.055f, 1.09f + 0.01f * Mth.sin(age * 0.2f),
                AuraRender.alpha(0xFF000000 | rgb, 0.16f * pulse * forge), true, true);
    }
}
