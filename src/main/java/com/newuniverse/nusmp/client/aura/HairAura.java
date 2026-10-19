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
 * 1.93.0 Hair Magic: the temporary armour (Aura.HAIR), rebuilt as a real set of pieces. A woven armour of hair: a mane with a fringe, side locks and a long back mass, a braided bodice with crossed lacing and a pearl, braid coils and locks on the shoulders, hair wound round the forearms, mitts with fringes, wrapped leggings with trailing locks, and on the back a three-panel cloak and four living tendrils that wave over the shoulders.
 * <p>The pieces are the root bones of {@code geo/entity/hair_armor.geo.json} (tools/gen_hair_geo.py), drawn by {@link ShakudoPieceKit} on the
 * model part each was built for and forged in / shattered out by {@link EquipAnimation} like the Djinn full equip (delays 0 to 12 ticks, the aura lasts at
 * least 40 ticks). Passes in order: skin pass of every piece (cutout, translucent while forging), glow pass of every piece (additive), then the accents,
 * all additive and full bright: a curling seal on the ground and long locks drifting in loops round the body, with floating tufts.
 * Layer space as in the other auras: y points down, the feet are at y = 1.5, the back is +z.
 */
public final class HairAura {
    private HairAura() {}

    private static final GeoSpec ARMOR = GeoSpec.of("hair", "armor");
    private static final int GLOW = 0xFFB0D0;
    private static final ResourceLocation SIGIL = AuraRender.tex("particle/hair_sigil");
    private static final ResourceLocation LOCK = AuraRender.tex("particle/hair_lock"), LOCK2 = AuraRender.tex("particle/hair_lock2"), TUFT = AuraRender.tex("particle/hair_tuft");

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
        PlayerAuraClient.register(Aura.HAIR, HairAura::paint);
    }

    private static void paint(AuraContext c) {
        float f = c.fade();
        if (f <= 0.002f) return;
        SealDraw.camera(c.pose());
        float age = c.age(), grow = Mth.clamp(c.t() / 14f, 0f, 1f);
        // 1 + 2. the pieces: skin passes, then glow passes
        ShakudoPieceKit.paint(c, SET, 1.0f);
        // 3. the curling seal under the feet
        ShakudoPieceKit.sigil(c, SIGIL, 0xFF8EB6, 1.1f, 0.7f, 0.75f * f, grow);
        // 4. long locks drifting in loops round the body (hanging sprites that swing)
        PoseStack pose = c.pose();
        VertexConsumer v0 = c.buffers().getBuffer(AuraRender.additive(LOCK)), v1 = c.buffers().getBuffer(AuraRender.additive(LOCK2));
        for (int i = 0; i < 7; i++) {
            float a = age * 0.035f * (i % 2 == 0 ? 1f : -1f) + i * Mth.TWO_PI / 7f, r = 0.72f + 0.12f * Mth.sin(age * 0.05f + i * 1.9f);
            float h = 0.35f + 0.8f * ShakudoPieceKit.frac(i * 0.43f + age * 0.004f);
            ShakudoPieceKit.upright(pose, i % 2 == 0 ? v0 : v1, Mth.cos(a) * r, 1.5f - h, Mth.sin(a) * r, a + Mth.HALF_PI + 0.4f * Mth.sin(age * 0.09f + i), 0.17f, 0.3f,
                    SealDraw.argb(0.75f * f * grow, 0xFFB4D2));
        }
        ShakudoPieceKit.motes(c, TUFT, 0xFFC8DE, 8, 0.5f, 0.75f, 0.06f, 0.85f * f, 5);
    }
}
