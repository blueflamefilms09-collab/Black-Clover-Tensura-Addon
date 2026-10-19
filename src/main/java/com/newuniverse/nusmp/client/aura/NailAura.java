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
 * 1.93.0 Nail Magic: the temporary armour (Aura.NAIL), rebuilt as a real set of pieces. Dark studded iron with rusted patches: a cap with a crown of eight long nails, a brigandine with nails driven into the sternum, studded domes with four nails on each shoulder, rows of nails through the bracers, five long nail-claws on each hand, and on the back the nail wheel, ten long nails that turn.
 * <p>The pieces are the root bones of {@code geo/entity/nail_armor.geo.json} (tools/gen_nail_geo.py), drawn by {@link ShakudoPieceKit} on the
 * model part each was built for and forged in / shattered out by {@link EquipAnimation} like the Djinn full equip (delays 0 to 12 ticks, the aura lasts at
 * least 40 ticks). Passes in order: skin pass of every piece (cutout, translucent while forging), glow pass of every piece (additive), then the accents,
 * all additive and full bright: a seal of nails on the ground, small nails orbiting the waist like a ring of thrown iron, and ember sparks.
 * Layer space as in the other auras: y points down, the feet are at y = 1.5, the back is +z.
 */
public final class NailAura {
    private NailAura() {}

    private static final GeoSpec ARMOR = GeoSpec.of("nail", "armor");
    private static final int GLOW = 0xFFA050;
    private static final ResourceLocation SIGIL = AuraRender.tex("particle/nail_sigil");
    private static final ResourceLocation NAIL = AuraRender.tex("particle/nail_nail_small"), SPARK = AuraRender.tex("particle/nail_spark");

    private static final PieceSet SET = new PieceSet(ARMOR, List.of(
            ShakudoPieceKit.head("helm", Anchor.HEAD, 0, 0f, -14f, -4f),
            ShakudoPieceKit.piece("chest", Anchor.BODY, 2, 0f, 0f, -12f),
            ShakudoPieceKit.piece("waist", Anchor.BODY, 4, 0f, 8f, -6f),
            ShakudoPieceKit.piece("pauldron_r", Anchor.RIGHT_ARM, 3, -14f, -4f, 0f),
            ShakudoPieceKit.piece("pauldron_l", Anchor.LEFT_ARM, 3, 14f, -4f, 0f),
            ShakudoPieceKit.detail("vambrace_r", Anchor.RIGHT_ARM, 5, -12f, 2f, -6f),
            ShakudoPieceKit.detail("vambrace_l", Anchor.LEFT_ARM, 5, 12f, 2f, -6f),
            ShakudoPieceKit.detail("gauntlet_r", Anchor.RIGHT_ARM, 6, -10f, 8f, -6f),
            ShakudoPieceKit.detail("gauntlet_l", Anchor.LEFT_ARM, 6, 10f, 8f, -6f),
            ShakudoPieceKit.piece("greave_r", Anchor.RIGHT_LEG, 7, -3f, 14f, -6f),
            ShakudoPieceKit.piece("greave_l", Anchor.LEFT_LEG, 7, 3f, 14f, -6f),
            ShakudoPieceKit.piece("back", Anchor.BODY, 10, 0f, -2f, 12f)), GLOW);

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        PlayerAuraClient.register(Aura.NAIL, NailAura::paint);
    }

    private static void paint(AuraContext c) {
        float f = c.fade();
        if (f <= 0.002f) return;
        SealDraw.camera(c.pose());
        float age = c.age(), grow = Mth.clamp(c.t() / 14f, 0f, 1f);
        // 1 + 2. the pieces: skin passes, then glow passes
        ShakudoPieceKit.paint(c, SET, 1.0f);
        // 3. the seal of nails under the feet
        ShakudoPieceKit.sigil(c, SIGIL, 0xFFB070, 1.1f, -1.2f, 0.8f * f, grow);
        // 4. a ring of small nails orbiting the waist, each tilted
        PoseStack pose = c.pose();
        VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(NAIL));
        for (int i = 0; i < 10; i++) {
            float a = -age * 0.09f + i * Mth.TWO_PI / 10f, r = 0.8f + 0.05f * Mth.sin(age * 0.2f + i);
            ShakudoPieceKit.upright(pose, vc, Mth.cos(a) * r, 0.55f + 0.18f * Mth.sin(age * 0.08f + i * 1.3f), Mth.sin(a) * r, a, 0.14f, 0.14f,
                    SealDraw.argb(0.85f * f * grow, 0xFFE0B8));
        }
        ShakudoPieceKit.motes(c, SPARK, 0xFFA050, 7, 1.0f, 0.7f, 0.05f, 0.95f * f, 33);
    }
}
