package com.newuniverse.nusmp.client.greed;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.greed.GreedHands;
import com.newuniverse.nusmp.greed.GreedItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the Black Sword Greed's gauntlet and dual-blade forms are drawn in BOTH hands ({@link GreedHands}). The main hand is the vanilla item draw (the item's own renderer picks the right glove / sword); this class
 * draws the second one, the other hand's: in third person a render layer on every player ({@link TwinHandsLayer}, seen by other players too: the main-hand stack is synced by the game), in first person the
 * off-hand's own hand draw ({@link RenderHandEvent} fires for an empty off hand too) with vanilla's own arm and swing transforms. It draws only while the main hand holds one of the two forms and the off hand is
 * EMPTY (a held item there is never covered), and only for the stack the game already syncs: no packet, no state, no allocation per frame (a layer, one item draw). The client setting {@code classicHands}
 * ({@code nusmp-greed-client.toml}) restores the old looks (one mesh, one draw). Looks only: the second blow is the server's ({@code GreedForms#offBlow}). Not seen in a running game by its author (see the doc).
 */
public final class GreedHandsClient {
    private GreedHandsClient() {}

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue CLASSIC_HANDS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("greed");
        CLASSIC_HANDS = b.comment("true = draw the Black Sword Greed's gauntlets and dual blades the old way (one mesh holding the pair / the double blade, in the main hand only). Only the look changes.").define("classicHands", false);
        b.pop();
        SPEC = b.build();
    }

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SPEC, "nusmp-greed-client.toml");
        modBus.addListener(GreedHandsClient::addLayers);
        NeoForge.EVENT_BUS.addListener(GreedHandsClient::onRenderHand);
    }

    /** The old looks, or the default while the file is not loaded yet. */
    public static boolean classic() {
        try {
            return CLASSIC_HANDS.get();
        } catch (IllegalStateException notLoaded) {
            return CLASSIC_HANDS.getDefault();
        }
    }

    /** Is this item path drawn with the new two-hand looks right now? */
    public static boolean twinLook(String itemPath) { return GreedHands.isTwin(itemPath) && !classic(); }

    /** Does this player hold a twin form in the main hand with the off hand empty (so the other hand draws its own)? */
    static boolean drawsTwin(AbstractClientPlayer p) {
        ItemStack main = p.getMainHandItem();
        String path = BuiltInRegistries.ITEM.getKey(main.getItem()).getPath();
        boolean twin = main.getItem() instanceof GreedItem && GreedHands.isTwin(path) || com.newuniverse.nusmp.sinweapons.SinRules.CHAKRAMS_ID.equals(path);      // the Greed twin forms and the Black Chakrams: one in each hand
        return !classic() && twin && p.getOffhandItem().isEmpty() && !p.isSpectator();
    }

    private static void addLayers(EntityRenderersEvent.AddLayers e) {
        for (PlayerSkin.Model skin : e.getSkins())
            if (e.getSkin(skin) instanceof PlayerRenderer r) r.addLayer(new TwinHandsLayer(r));
    }

    /** First person: the off hand's own draw, with vanilla's transforms of a held item (ItemInHandRenderer.renderArmWithItem for a plain item), the same stack as the main hand. */
    private static void onRenderHand(RenderHandEvent e) {
        if (e.getHand() != InteractionHand.OFF_HAND || !e.getItemStack().isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer p = mc.player;
        if (p == null || mc.level == null || !drawsTwin(p) || p.isScoping() || p.isUsingItem()) return;
        HumanoidArm arm = p.getMainArm().getOpposite();
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        float swing = e.getSwingProgress(), s = Mth.sqrt(swing);
        PoseStack pose = e.getPoseStack();
        pose.pushPose();
        pose.translate(i * -0.4f * Mth.sin(s * (float) Math.PI), 0.2f * Mth.sin(s * (float) Math.PI * 2f), -0.2f * Mth.sin(swing * (float) Math.PI));
        pose.translate(i * 0.56f, -0.52f + e.getEquipProgress() * -0.6f, -0.72f);                   // applyItemArmTransform
        float f = Mth.sin(swing * swing * (float) Math.PI), f1 = Mth.sin(s * (float) Math.PI);       // applyItemArmAttackTransform
        pose.mulPose(Axis.YP.rotationDegrees(i * (45f + f * -20f)));
        pose.mulPose(Axis.ZP.rotationDegrees(i * f1 * -20f));
        pose.mulPose(Axis.XP.rotationDegrees(f1 * -80f));
        pose.mulPose(Axis.YP.rotationDegrees(i * -45f));
        ItemDisplayContext ctx = i > 0 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        mc.getItemRenderer().renderStatic(p, p.getMainHandItem(), ctx, i < 0, pose, e.getMultiBufferSource(), mc.level, e.getPackedLight(), OverlayTexture.NO_OVERLAY, p.getId() + ctx.ordinal());
        pose.popPose();
    }
}
