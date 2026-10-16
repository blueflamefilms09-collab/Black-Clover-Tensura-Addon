package com.newuniverse.nusmp.client.relics.worn.lib;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.relics.fx.RelicFxOptions;
import com.newuniverse.nusmp.relics.RelicBearer;
import com.newuniverse.nusmp.relics.RelicSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Armour as an object: in the hand, on the ground and in a frame an armour item of the mod is its 3D worn model (the same pieces the wearer gets, at the rest pose of the
 * player model, scaled to fit), and in the inventory it keeps the old 16x16 sprite, loaded as the model {@code item/<id>_icon} (tools/relics/gen_armor_item_models.py turns an
 * item's model json into a {@code builtin/entity} model and moves the old one to that name; an item without the icon file keeps its old model and is not touched). A relic
 * helmet or pair of boots uses its worn models; a relic chest piece (only ornaments over a painted body) keeps its sprite. The robe sets are GeckoLib items and take this
 * renderer through their own provider ({@code RobeItem}); every other armour item takes it from {@link #registerExtensions}.
 */
public final class ArmorItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static ArmorItemRenderer instance;

    private ArmorItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static ArmorItemRenderer instance() {
        if (instance == null) instance = new ArmorItemRenderer();
        return instance;
    }

    static ModelResourceLocation icon(String path) { return ModelResourceLocation.standalone(WornModels.rl("item/" + path + "_icon")); }

    /** Every armour item that may carry the renderer: the mod's armour items and the relic armour. */
    private static boolean candidate(Item item) {
        return item instanceof ArmorItem && ("nusmp".equals(BuiltInRegistries.ITEM.getKey(item).getNamespace()));
    }

    /** Mod bus: the custom renderer of every armour item that is not a GeckoLib item (those hand it out themselves). */
    public static void registerExtensions(RegisterClientExtensionsEvent e) {
        IClientItemExtensions ext = new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() { return instance(); }
        };
        List<Item> items = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) if (candidate(item) && !(item instanceof GeoItem)) items.add(item);
        if (!items.isEmpty()) e.registerItem(ext, items.toArray(new Item[0]));
    }

    /** Mod bus: the old models of the converted items, so they can be drawn as the icon. */
    public static void registerModels(ModelEvent.RegisterAdditional e) {
        var rm = Minecraft.getInstance().getResourceManager();
        for (Item item : BuiltInRegistries.ITEM) {
            if (!candidate(item)) continue;
            String path = BuiltInRegistries.ITEM.getKey(item).getPath();
            if (rm.getResource(WornModels.rl("models/item/" + path + "_icon.json")).isPresent()) e.register(icon(path));
        }
    }

    /** The ensemble an item stands for as an object, or null (the icon is drawn). */
    private static WornLibrary.Ensemble ensembleOf(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof RelicBearer b) {
            RelicSlot slot = b.relic().slot();
            return slot == RelicSlot.HEAD || slot == RelicSlot.FEET ? WornLibrary.of(b.relic()) : null;
        }
        if (item instanceof ArmorItem a) return ArmorItems.of(stack, a.getEquipmentSlot());
        return null;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (stack.getItem() instanceof RelicBearer) {                              // 1.48.0: a relic armour item is held as the hand library says (hold styles, hand models)
            if (RelicFxOptions.handModels()) {
                com.newuniverse.nusmp.client.relics.hand.HandItemRenderer.instance().renderByItem(stack, ctx, pose, buffers, light, overlay);
                return;
            }
            pose.pushPose();                                                       // option off: the 1.34.0 object form below, with the display block that its item json no longer carries
            try {
                com.newuniverse.nusmp.client.relics.hand.HandItemRenderer.applySpritePreset(pose, ctx);
                renderOlder(stack, ctx, pose, buffers, light, overlay);
            } finally {
                pose.popPose();
            }
            return;
        }
        renderOlder(stack, ctx, pose, buffers, light, overlay);
    }

    /** The armour item as an object, as 1.34.0 made it (every armour item that is not a relic, and a relic when relicfx.handModels is off). */
    private void renderOlder(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        WornLibrary.Ensemble e = null;
        if (ctx != ItemDisplayContext.GUI && RelicFxOptions.models()) {
            try {
                e = ensembleOf(stack);
            } catch (RuntimeException ex) {
                LogUtils.getLogger().error("[nusmp] the 3D model of armour item {} failed; its icon is used", path, ex);
            }
        }
        if (e == null) {                                                           // the inventory, or no model: the old icon
            BakedModel icon = mc.getModelManager().getModel(icon(path));
            pose.pushPose();
            pose.translate(0.5f, 0.5f, 0.5f);                                      // render() translates by -0.5 again
            mc.getItemRenderer().render(stack, ItemDisplayContext.GUI, false, pose, buffers, light, overlay, icon);
            pose.popPose();
            return;
        }
        float time = mc.level == null ? 0f : mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(false);
        pose.pushPose();
        if (ctx == ItemDisplayContext.FIXED || ctx == ItemDisplayContext.HEAD) {   // the display transform of these turns a sprite round to face out; a model must be turned back
            pose.translate(0.5f, 0.5f, 0.5f);
            pose.mulPose(Axis.YP.rotationDegrees(180f));
            pose.translate(-0.5f, -0.5f, -0.5f);
        }
        WornLibDraw.itemFit(e, pose, 0.92f);
        WornLibDraw.drawItem(e, pose, buffers, light, time);
        pose.popPose();
    }
}
