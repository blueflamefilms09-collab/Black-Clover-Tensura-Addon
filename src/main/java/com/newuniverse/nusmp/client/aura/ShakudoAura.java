package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.client.aura.ShakudoPieceKit.Anchor;
import com.newuniverse.nusmp.client.aura.ShakudoPieceKit.PieceSet;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.prop.SealDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * 1.93.0 Shakudo Magic: the temporary armour (Aura.SHAKUDO), rebuilt as a real set of pieces. A black-purple patinated alloy full armour with fine gold inlay: a kabuto with a gilt crescent crest and neck lames, a lamellar do with a crest disc, layered sode on the shoulders, kote, gauntlets, haidate and greaves, and on the back a gilt ring with a glowing rune on a pole with two sash tails (it turns).
 * <p>The pieces are the root bones of {@code geo/entity/shakudo_armor.geo.json} (tools/gen_shakudo_geo.py), drawn by {@link ShakudoPieceKit} on the
 * model part each was built for and forged in / shattered out by {@link EquipAnimation} like the Djinn full equip (delays 0 to 12 ticks, the aura lasts at
 * least 40 ticks). Passes in order: skin pass of every piece (cutout, translucent while forging), glow pass of every piece (additive), then the accents,
 * all additive and full bright: a spinning gold seal on the ground and drifting gold flakes.
 * Layer space as in the other auras: y points down, the feet are at y = 1.5, the back is +z.
 */
public final class ShakudoAura {
    private ShakudoAura() {}

    private static final GeoSpec ARMOR = GeoSpec.of("shakudo", "armor");
    private static final int GLOW = 0xFFD070;
    private static final ResourceLocation SIGIL = AuraRender.tex("particle/shakudo_sigil");
    private static final ResourceLocation FLAKE = AuraRender.tex("particle/shakudo_flake");

    private static final PieceSet SET = new PieceSet(ARMOR, List.of(
            ShakudoPieceKit.head("helm", Anchor.HEAD, 0, 0f, -14f, -4f),
            ShakudoPieceKit.piece("chest", Anchor.BODY, 2, 0f, 0f, -12f),
            ShakudoPieceKit.piece("waist", Anchor.BODY, 4, 0f, 8f, -6f),
            ShakudoPieceKit.piece("pauldron_r", Anchor.RIGHT_ARM, 3, -14f, -4f, 0f),
            ShakudoPieceKit.piece("pauldron_l", Anchor.LEFT_ARM, 3, 14f, -4f, 0f),
            ShakudoPieceKit.piece("vambrace_r", Anchor.RIGHT_ARM, 5, -12f, 2f, -6f),
            ShakudoPieceKit.piece("vambrace_l", Anchor.LEFT_ARM, 5, 12f, 2f, -6f),
            ShakudoPieceKit.piece("gauntlet_r", Anchor.RIGHT_ARM, 6, -10f, 8f, -6f),
            ShakudoPieceKit.piece("gauntlet_l", Anchor.LEFT_ARM, 6, 10f, 8f, -6f),
            ShakudoPieceKit.piece("greave_r", Anchor.RIGHT_LEG, 7, -3f, 14f, -6f),
            ShakudoPieceKit.piece("greave_l", Anchor.LEFT_LEG, 7, 3f, 14f, -6f),
            ShakudoPieceKit.piece("back", Anchor.BODY, 10, 0f, -2f, 12f)), GLOW);

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        PlayerAuraClient.register(Aura.SHAKUDO, ShakudoAura::paint);
    }

    private static void paint(AuraContext c) {
        float f = c.fade();
        if (f <= 0.002f) return;
        SealDraw.camera(c.pose());
        float age = c.age(), grow = Mth.clamp(c.t() / 14f, 0f, 1f);
        // 1 + 2. the pieces: skin passes, then glow passes
        ShakudoPieceKit.paint(c, SET, 1.0f);
        // 3. the gold seal under the feet (additive)
        ShakudoPieceKit.sigil(c, SIGIL, 0xE6B450, 1.15f, 1.1f, 0.85f * f, grow);
        ShakudoPieceKit.sigil(c, SIGIL, 0xFFE9A8, 0.8f, -1.6f, 0.45f * f * (0.6f + 0.4f * Mth.sin(age * 0.1f)), grow);
        // 4. gold flakes lifting off the plates
        ShakudoPieceKit.motes(c, FLAKE, 0xFFD878, 9, 0.8f, 0.7f, 0.07f, 0.9f * f, 14);
    }
}
