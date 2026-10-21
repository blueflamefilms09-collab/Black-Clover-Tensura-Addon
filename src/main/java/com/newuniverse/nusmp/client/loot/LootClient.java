package com.newuniverse.nusmp.client.loot;

import com.newuniverse.nusmp.client.relics.worn.lib.ArmorItemRenderer;
import com.newuniverse.nusmp.client.relics.worn.lib.ArmorItems;
import com.newuniverse.nusmp.labyrinth.loot.LootArmorItem;
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
 * Why: the dungeon armour of the loot is drawn on a player by the worn model library like every other armour of the mod, so the vanilla armour box (the plain pattern in the
 * set's colours) must not be drawn on top of it. This gives each piece one client extension (a second registration for the same item is refused by NeoForge, which is why
 * {@code ArmorItemRenderer} leaves the loot armour out of its own) whose armour model is the empty one exactly when the library replaces the piece (a player, the option on, a model
 * found), so a mob or an armour stand wearing it still shows the vanilla box and nothing is ever invisible. The object form is the library's renderer, as for the other armour.
 * Client thread only; one call from the client block of the mod class.
 */
public final class LootClient {
    private LootClient() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(LootClient::registerExtensions);
        modBus.addListener(LootClient::registerBackpackScreen);                                                    // 1.107.0: the Backpack window
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e) -> e.enqueueWork(LootClient::registerBackpackModels));
    }

    /** 1.107.0: the window of the Backpack for every tier. */
    static void registerBackpackScreen(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent e) {
        e.register(com.newuniverse.nusmp.labyrinth.loot.backpack.BackpackRegistry.MENU.get(), BackpackScreen::new);
    }

    /** 1.107.0: the item property that lets the Backpack's item model pick its tier model (the overrides of {@code models/item/backpack.json}). */
    static void registerBackpackModels() {
        net.minecraft.client.renderer.item.ItemProperties.register(com.newuniverse.nusmp.labyrinth.loot.backpack.BackpackRegistry.ITEM.get(),
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("nusmp", "backpack_tier"),
                (stack, level, entity, seed) -> com.newuniverse.nusmp.labyrinth.loot.backpack.BackpackItem.tierOf(stack).ordinal());
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
        for (Item item : BuiltInRegistries.ITEM) if (item instanceof LootArmorItem && !e.isItemRegistered(item)) items.add(item);
        if (!items.isEmpty()) e.registerItem(ext, items.toArray(new Item[0]));
    }
}
