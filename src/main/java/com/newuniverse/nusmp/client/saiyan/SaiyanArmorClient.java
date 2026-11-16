package com.newuniverse.nusmp.client.saiyan;

import com.newuniverse.nusmp.client.relics.worn.lib.ArmorItemRenderer;
import com.newuniverse.nusmp.client.relics.worn.lib.ArmorItems;
import com.newuniverse.nusmp.saiyan.armor.SaiyanArmorItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the craftable Saiyan alloy armour is drawn on a player by the worn model library (the Blender models of {@code relic_worn/saiyan_alloy_*.txt}) like the other armour of the mod, so the vanilla armour
 * box (the plain painted layer of the material) must not be drawn on top of it. Each piece gets one client extension (NeoForge refuses a second registration for the same item, which is why
 * {@code ArmorItemRenderer} leaves these pieces out of its own) whose armour model is the empty one exactly when the library replaces the piece (a player, the option on, a model found), so a mob or an
 * armour stand wearing it still shows the vanilla box and nothing is ever invisible. The object form (hand, ground, frame) is the library's renderer, the inventory keeps the sprite. Client thread
 * only; one call from the client block of the mod class.
 */
public final class SaiyanArmorClient {
    private SaiyanArmorClient() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(SaiyanArmorClient::registerExtensions);
    }

    static void registerExtensions(RegisterClientExtensionsEvent e) {
        IClientItemExtensions ext = new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity wearer, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                return ArmorItems.replaces(wearer, stack, slot) ? ArmorItems.emptyModel() : original;
            }

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() { return ArmorItemRenderer.instance(); }
        };
        List<Item> items = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) if (item instanceof SaiyanArmorItem && !e.isItemRegistered(item)) items.add(item);
        if (!items.isEmpty()) e.registerItem(ext, items.toArray(new Item[0]));
    }
}
