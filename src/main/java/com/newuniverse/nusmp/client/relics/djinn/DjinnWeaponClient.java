package com.newuniverse.nusmp.client.relics.djinn;

import com.newuniverse.nusmp.client.WeaponRenderer;
import com.newuniverse.nusmp.relics.djinn.weapon.DjinnWeaponItems;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Draws the 24 Djinn weapons in 3D with the mod's weapon renderer, without being in its fixed list: the same {@link WeaponRenderer} class is the custom
 * renderer of each of these items (it reads {@code weapon_meshes/<id>.txt} and the skin and glow in {@code textures/entity/weapon/}), and their 16 x 16
 * icon models ({@code item/<id>_icon}) are added to the models the game bakes. Registered from {@link DjinnClient#init}.
 */
public final class DjinnWeaponClient {
    private DjinnWeaponClient() {}

    private static WeaponRenderer instance;

    public static void init(IEventBus modBus) {
        modBus.addListener(DjinnWeaponClient::registerExtensions);
        modBus.addListener(DjinnWeaponClient::registerModels);
    }

    private static void registerExtensions(RegisterClientExtensionsEvent e) {
        IClientItemExtensions ext = new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (instance == null) instance = new WeaponRenderer();
                return instance;
            }
        };
        for (DeferredItem<Item> item : DjinnWeaponItems.all()) e.registerItem(ext, item.get());
    }

    private static void registerModels(ModelEvent.RegisterAdditional e) {
        for (DeferredItem<Item> item : DjinnWeaponItems.all()) {
            String path = BuiltInRegistries.ITEM.getKey(item.get()).getPath();
            e.register(ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("nusmp", "item/" + path + "_icon")));
        }
    }
}
