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
 * 1.93.0 Scale Magic: the temporary armour (Aura.SCALE), rebuilt as a real set of pieces. A scaled hood with swept fins, a scale cuirass over a pale belly plate, scale cowls with fins on the shoulders, clawed gauntlets, scaled greaves with calf fins, a dorsal ridge on the back and a tail of seven segments that waves.
 * <p>The pieces are the root bones of {@code geo/entity/scale_armor.geo.json} (tools/gen_scale_geo.py), drawn by {@link ShakudoPieceKit} on the
 * model part each was built for and forged in / shattered out by {@link EquipAnimation} like the Djinn full equip (delays 0 to 12 ticks, the aura lasts at
 * least 40 ticks). Passes in order: skin pass of every piece (cutout, translucent while forging), glow pass of every piece (additive), then the accents,
 * all additive and full bright: a rippling water-like ring on the ground and single scales orbiting the body in a slow spiral, with gleams.
 * Layer space as in the other auras: y points down, the feet are at y = 1.5, the back is +z.
 */
public final class ScaleAura {
    private ScaleAura() {}

    private static final GeoSpec ARMOR = GeoSpec.of("scale", "armor");
    private static final int GLOW = 0xA8FFE8;
    private static final ResourceLocation SIGIL = AuraRender.tex("particle/scale_sigil");
    private static final ResourceLocation RIPPLE = AuraRender.tex("particle/scale_ripple"), SCALE = AuraRender.tex("particle/scale_scale"), GLEAM = AuraRender.tex("particle/scale_gleam");

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
            ShakudoPieceKit.piece("tail", Anchor.BODY, 12, 0f, 6f, 14f),
            ShakudoPieceKit.piece("back", Anchor.BODY, 10, 0f, -2f, 12f)), GLOW);

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        PlayerAuraClient.register(Aura.SCALE, ScaleAura::paint);
    }

    private static void paint(AuraContext c) {
        float f = c.fade();
        if (f <= 0.002f) return;
        SealDraw.camera(c.pose());
        float age = c.age(), grow = Mth.clamp(c.t() / 14f, 0f, 1f);
        // 1 + 2. the pieces: skin passes, then glow passes
        ShakudoPieceKit.paint(c, SET, 1.0f);
        // 3. ripples running out from the feet (additive)
        for (int k = 0; k < 2; k++) {
            float ph = ShakudoPieceKit.frac(age * 0.02f + k * 0.5f);
            ShakudoPieceKit.sigil(c, RIPPLE, 0x8AF0E0, 0.5f + 0.9f * ph, 0f, 0.8f * f * (1f - ph) * grow, 1f);
        }
        ShakudoPieceKit.sigil(c, SIGIL, 0x6FE0D0, 1.0f, 0.8f, 0.55f * f, grow);
        // 4. scales orbiting the body in a spiral, upright, each turned to face outward
        PoseStack pose = c.pose();
        VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(SCALE));
        for (int i = 0; i < 8; i++) {
            float a = age * 0.045f + i * Mth.TWO_PI / 8f, h = 0.2f + 1.2f * ShakudoPieceKit.frac(i * 0.381f + age * 0.006f);
            float r = 0.78f + 0.08f * Mth.sin(age * 0.07f + i);
            ShakudoPieceKit.upright(pose, vc, Mth.cos(a) * r, 1.5f - h, Mth.sin(a) * r, a + Mth.HALF_PI, 0.09f, 0.1f, SealDraw.argb(0.8f * f * grow, 0x9AF5E4));
        }
        ShakudoPieceKit.motes(c, GLEAM, 0xE8FFF8, 6, 0.6f, 0.65f, 0.06f, 0.9f * f, 21);
    }
}
