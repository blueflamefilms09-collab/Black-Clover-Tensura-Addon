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
 * 1.93.0 Spike Magic: the temporary armour (Aura.SPIKE), rebuilt as a real set of pieces. Brushed gun-metal plate bristling with spikes: a crown of five long spikes, forward spikes on the chest, three long swept spikes on each shoulder, knuckle spikes, a knee spike and a fan of seven flexing spines on the back; every tip glows ice white.
 * <p>The pieces are the root bones of {@code geo/entity/spike_armor.geo.json} (tools/gen_spike_geo.py), drawn by {@link ShakudoPieceKit} on the
 * model part each was built for and forged in / shattered out by {@link EquipAnimation} like the Djinn full equip (delays 0 to 12 ticks, the aura lasts at
 * least 40 ticks). Passes in order: skin pass of every piece (cutout, translucent while forging), glow pass of every piece (additive), then the accents,
 * all additive and full bright: a ring of spikes thrusting out of the ground round the feet, a seal under them, and glinting shards.
 * Layer space as in the other auras: y points down, the feet are at y = 1.5, the back is +z.
 */
public final class SpikeAura {
    private SpikeAura() {}

    private static final GeoSpec ARMOR = GeoSpec.of("spike", "armor");
    private static final int GLOW = 0xCFEFFF;
    private static final ResourceLocation SIGIL = AuraRender.tex("particle/spike_sigil");
    private static final ResourceLocation SPIKE = AuraRender.tex("particle/spike_spike"), SPIKE2 = AuraRender.tex("particle/spike_spike2"), SHARD = AuraRender.tex("particle/spike_shard");

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
        PlayerAuraClient.register(Aura.SPIKE, SpikeAura::paint);
    }

    private static void paint(AuraContext c) {
        float f = c.fade();
        if (f <= 0.002f) return;
        SealDraw.camera(c.pose());
        float age = c.age(), grow = Mth.clamp(c.t() / 14f, 0f, 1f);
        // 1 + 2. the pieces: skin passes, then glow passes
        ShakudoPieceKit.paint(c, SET, 1.0f);
        // 3. the seal, and a ring of spikes thrusting out of the ground (upright sprites that grow and sink in turn)
        ShakudoPieceKit.sigil(c, SIGIL, 0xBFE4FF, 1.15f, 0.9f, 0.8f * f, grow);
        PoseStack pose = c.pose();
        VertexConsumer a0 = c.buffers().getBuffer(AuraRender.additive(SPIKE)), a1 = c.buffers().getBuffer(AuraRender.additive(SPIKE2));
        for (int i = 0; i < 9; i++) {
            float yaw = i * Mth.TWO_PI / 9f + 0.3f, ph = Mth.sin(age * 0.06f + i * 1.7f) * 0.5f + 0.5f;
            float h = (0.28f + 0.3f * ph) * (0.4f + 0.6f * grow), r = 0.95f + 0.1f * Mth.sin(i * 2.3f);
            ShakudoPieceKit.upright(pose, i % 2 == 0 ? a0 : a1, Mth.cos(yaw) * r, 1.5f - h * 0.5f, Mth.sin(yaw) * r, yaw + Mth.HALF_PI, 0.1f, h * 0.5f,
                    SealDraw.argb(0.85f * f, 0xDCEEFF));
        }
        ShakudoPieceKit.motes(c, SHARD, 0xEAF6FF, 6, 0.9f, 0.8f, 0.05f, 0.9f * f, 7);
    }
}
