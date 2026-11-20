package com.newuniverse.nusmp.client.devil;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.client.SafeAdditive;
import com.newuniverse.nusmp.devil.DevilRules;
import com.newuniverse.nusmp.devil.WorldDevil;
import com.newuniverse.nusmp.perf.PerfProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Why: what a devil looks like (3.48.0), drawn on top of the player the game already draws: the Weg markings (a copy of the player's body, a hair larger, carrying a translucent marking texture of the
 * contract's tier, so they sit on the skin and under any armour), the horns from 20 %, the wings from 50 % (spread and beating while the player is in the air or in the union), and a glow pass in the union.
 * Every number it reads is the {@link DevilClient.Vis} the server sent (tier, level in steps of five, union); nothing here decides anything.
 *
 * <p>Cost: nothing at all for a player with no entry. For a devil: one body copy (about 6 boxes) for the markings, 4 horn segments from 20 %, about 15 boxes of wings from 50 %, and the same again for the
 * glow in the union. Players further than 32 blocks are skipped (the markings are a few pixels there), and at most {@link DevilClient#MAX_PER_FRAME} other players are drawn a frame.
 * Section {@code c.devil.layer} of the performance probe times it. No allocation per frame (render types are memoised by the game, colours are ints). Not seen in a running game by its author.
 */
public final class WegLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final PerfProbe.Section SECTION = PerfProbe.section("c.devil.layer");
    private static final double FAR2 = 32.0 * 32.0;
    private static final String BASE = "textures/entity/devil/";
    private static final ResourceLocation HORNS_TEX = tex("horns"), WINGS_TEX = tex("wings"), WINGS_GLOW = tex("wings_glow");
    /** 4.40.0: the new look (tools/gen_devil_textures_v2.py): flowing Weg lines, swept horns, veined wings. {@code devil.classicLook = true} draws the old files above and below instead. */
    private static final ResourceLocation HORNS2_TEX = tex("horns2"), WINGS2_TEX = tex("wings2"), WINGS2_GLOW = tex("wings2_glow");
    /** [tier 0..3][slim][glow]. */
    private static final ResourceLocation[][][] WEG = new ResourceLocation[4][2][2], WEG2 = new ResourceLocation[4][2][2];

    static {
        String[] tiers = {"low", "mid", "high", "supreme"};
        for (int t = 0; t < 4; t++)
            for (int s = 0; s < 2; s++) {
                WEG[t][s][0] = tex("weg_" + tiers[t] + (s == 1 ? "_slim" : ""));
                WEG[t][s][1] = tex("weg_" + tiers[t] + (s == 1 ? "_slim" : "") + "_glow");
                WEG2[t][s][0] = tex("weg2_" + tiers[t] + (s == 1 ? "_slim" : ""));
                WEG2[t][s][1] = tex("weg2_" + tiers[t] + (s == 1 ? "_slim" : "") + "_glow");
            }
    }

    private static ResourceLocation tex(String name) { return ResourceLocation.fromNamespaceAndPath("anime_multiverse", BASE + name + ".png"); }

    private final PlayerModel<AbstractClientPlayer> overlay;
    private final DevilModels.Horns horns, hornsSwept;
    private final DevilModels.Wings wings;
    private final boolean slim;

    private WegLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, PlayerModel<AbstractClientPlayer> overlay, DevilModels.Horns horns, DevilModels.Horns hornsSwept, DevilModels.Wings wings, boolean slim) {
        super(parent);
        this.overlay = overlay;
        this.horns = horns;
        this.hornsSwept = hornsSwept;
        this.wings = wings;
        this.slim = slim;
    }

    /** Mod bus, client: puts the layer on both player renderers (wide and slim arms). */
    static void add(EntityRenderersEvent.AddLayers e) {
        DevilModels.Horns horns = new DevilModels.Horns(e.getEntityModels().bakeLayer(DevilModels.HORNS));
        DevilModels.Horns swept = new DevilModels.Horns(e.getEntityModels().bakeLayer(DevilModels.HORNS_SWEPT));
        DevilModels.Wings wings = new DevilModels.Wings(e.getEntityModels().bakeLayer(DevilModels.WINGS));
        for (PlayerSkin.Model skin : e.getSkins()) {
            boolean slim = skin == PlayerSkin.Model.SLIM;
            if (e.getSkin(skin) instanceof PlayerRenderer r) {
                PlayerModel<AbstractClientPlayer> body = new PlayerModel<>(e.getEntityModels().bakeLayer(slim ? DevilModels.WEG_SLIM : DevilModels.WEG_WIDE), slim);
                r.addLayer(new WegLayer(r, body, horns, swept, wings, slim));
            }
        }
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer p, float limbSwing, float limbSwingAmount,
                       float partial, float age, float netHeadYaw, float headPitch) {
        DevilClient.Vis v = DevilClient.vis(p.getId());
        if (v == null || p.isInvisible() || p.isSpectator() || !DevilClientConfig.weg()) return;
        Minecraft mc = Minecraft.getInstance();
        boolean self = p == mc.player;
        if (!self && (mc.getEntityRenderDispatcher().distanceToSqr(p) > FAR2 || !DevilClient.takeSlot())) return;
        int tier = DevilRules.clampInt(v.tier, 1, 4) - 1;
        long t0 = PerfProbe.begin();
        try {
            float now = p.level().getGameTime() + partial;
            boolean air = !p.onGround() && !p.isInWater() && !p.onClimbable();
            v.step(now, air);
            float level = v.shown;
            boolean union = v.union;
            int s = slim ? 1 : 0;
            boolean classic = DevilClientConfig.classicLook();                                          // 4.40.0: true = the look of before

            // ---- the markings: the body, a hair larger, in the tier's texture, fainter when the level is low
            float markA = Mth.clamp(0.18f + 0.82f * level / 100f, 0f, 1f);
            boolean custom = v.devil >= 0 && DevilClientConfig.customForms();                          // 3.97.0: a devil of another world: the markings in its colour, and its mesh instead of the generic horns and wings
            int tint = custom ? tintOf(WorldDevil.at(v.devil).color()) : 0xFFFFFF;
            boolean ownForm = custom && DevilFormLayer.usable(v.devil);
            drawMarks(pose, buffers, light, p, tier, s, markA, glowAmount(level, union, age), tint, classic);

            // ---- horns from 20 %
            if (level >= DevilRules.HORNS_AT && !ownForm) {
                float grow = 0.75f + 0.5f * Mth.clamp((level - DevilRules.HORNS_AT) / 40f, 0f, 1f);
                if (union) grow *= 1.2f;
                pose.pushPose();
                getParentModel().head.translateAndRotate(pose);
                pose.translate(0f, -0.5f, 0f);
                pose.scale(grow, grow, grow);
                pose.translate(0f, 0.5f, 0f);
                (classic ? horns : hornsSwept).render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(classic ? HORNS_TEX : HORNS2_TEX)), light, OverlayTexture.NO_OVERLAY, -1);
                pose.popPose();
            }

            // ---- wings from 50 %
            if (level >= DevilRules.WINGS_AT && !ownForm) {
                float size = (0.55f + 0.2f * Mth.clamp((level - DevilRules.WINGS_AT) / 50f, 0f, 1f)) * (union ? 1.2f : 1f);
                float flap = air ? Mth.sin(age * 0.38f) * 0.85f : Mth.sin(age * 0.07f) * 0.12f;
                wings.pose(v.spread, flap, size);
                pose.pushPose();
                getParentModel().body.translateAndRotate(pose);
                wings.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(classic ? WINGS_TEX : WINGS2_TEX)), light, OverlayTexture.NO_OVERLAY, -1);
                float g = union ? 0.95f : 0.35f + 0.25f * Mth.clamp((level - 50f) / 50f, 0f, 1f);
                g *= 0.85f + 0.15f * Mth.sin(age * 0.12f);
                wings.render(pose, buffers.getBuffer(SafeAdditive.additive(classic ? WINGS_GLOW : WINGS2_GLOW)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, FastColor.ARGB32.color(Mth.clamp((int) (g * 255f), 0, 255), 255, 255, 255));
                pose.popPose();
            }
        } finally {
            PerfProbe.end(SECTION, t0);
        }
    }

    /** The brightness of the glow cores: none under 50 %, a faint one from there, full and pulsing in the union. */
    private static float glowAmount(float level, boolean union, float age) {
        if (union) return 0.85f + 0.15f * Mth.sin(age * 0.2f);
        return level < DevilRules.WINGS_AT ? 0f : 0.45f * Mth.clamp((level - DevilRules.WINGS_AT) / 50f, 0f, 1f);
    }

    /** The devil's colour mixed a third of the way to white, so the markings keep their drawing. */
    private static int tintOf(int rgb) {
        int r = 255 - ((255 - (rgb >> 16 & 255)) * 2 / 3), g = 255 - ((255 - (rgb >> 8 & 255)) * 2 / 3), b = 255 - ((255 - (rgb & 255)) * 2 / 3);
        return r << 16 | g << 8 | b;
    }

    private void drawMarks(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer p, int tier, int s, float markA, float glow, int tint, boolean classic) {
        ResourceLocation[][][] weg = classic ? WEG : WEG2;
        int tr = tint >> 16 & 255, tg = tint >> 8 & 255, tb = tint & 255;
        PlayerModel<AbstractClientPlayer> parent = getParentModel();
        parent.copyPropertiesTo(overlay);
        overlay.setAllVisible(false);
        overlay.head.visible = overlay.body.visible = overlay.rightArm.visible = overlay.leftArm.visible = overlay.rightLeg.visible = overlay.leftLeg.visible = true;   // the base layer only: the marks are painted there
        overlay.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(weg[tier][s][0])), light, OverlayTexture.NO_OVERLAY, FastColor.ARGB32.color(Mth.clamp((int) (markA * 255f), 0, 255), tr, tg, tb));
        if (glow > 0.02f)
            overlay.renderToBuffer(pose, buffers.getBuffer(SafeAdditive.additive(weg[tier][s][1])), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, FastColor.ARGB32.color(Mth.clamp((int) (glow * 255f), 0, 255), tr, tg, tb));
    }
}
