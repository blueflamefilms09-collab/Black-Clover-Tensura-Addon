package com.newuniverse.nusmp.client.elite.items;

import com.newuniverse.nusmp.client.WeaponRenderer;
import com.newuniverse.nusmp.client.relics.worn.lib.ArmorItems;
import com.newuniverse.nusmp.elite.items.EliteIds;
import com.newuniverse.nusmp.elite.items.armor.NightingaleArmorItem;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the client half of the elite items, started by {@code EliteItems.init} on the client only. (1) The six weapons are drawn in hand, on the ground and in frames by the SAME renderer as every other
 * 3D weapon of the mod ({@link WeaponRenderer}: the mesh {@code weapon_meshes/<id>.txt}, the skin, the pulsing glow, the void layer), so they are registered with it here instead of editing its list; its
 * mesh cache and class table are the ones the mod already resets on a resource reload. The inventory keeps the 16x16 icon, which is the model {@code item/<id>_icon} registered below (exactly the
 * pattern of {@code WeaponRenderer.registerModels}). (2) The Nightingale pieces tell the armour item that the model library draws a player wearing them, so the painted layer is swapped for a
 * transparent one on that player and the 3D worn model is not drawn on top of it ({@link NightingaleArmorItem#libraryDraws}). Everything else about the pieces (the object form, the worn model, the
 * icon) is done by the library's own registration for every armour item of the mod.
 */
public final class EliteItemsClient {
    private EliteItemsClient() {}

    private static WeaponRenderer renderer;

    public static void init(IEventBus modBus) {
        modBus.addListener(EliteItemsClient::registerExtensions);
        modBus.addListener(EliteItemsClient::registerModels);
        NightingaleArmorItem.libraryDraws = (entity, stack) -> {
            if (!(entity instanceof LivingEntity wearer) || !(stack.getItem() instanceof ArmorItem a)) return false;
            return ArmorItems.replaces(wearer, stack, a.getEquipmentSlot());
        };
    }

    private static List<Item> weapons() {
        List<Item> out = new ArrayList<>();
        for (String id : EliteIds.WEAPONS) BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("nusmp", id)).ifPresent(out::add);
        return out;
    }

    static void registerExtensions(RegisterClientExtensionsEvent e) {
        IClientItemExtensions ext = new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new WeaponRenderer();
                return renderer;
            }
        };
        List<Item> items = weapons();
        if (!items.isEmpty()) e.registerItem(ext, items.toArray(new Item[0]));
    }

    static void registerModels(ModelEvent.RegisterAdditional e) {
        for (String id : EliteIds.WEAPONS) e.register(ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("nusmp", "item/" + id + "_icon")));
    }
}
