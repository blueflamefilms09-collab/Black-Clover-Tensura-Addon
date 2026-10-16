package com.newuniverse.nusmp.client.relics.hand;

import com.newuniverse.nusmp.relics.RelicConsumableItem;
import com.newuniverse.nusmp.relics.RelicGearItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the hand renderer only works if three things are wired on the client: the item renderer of every Curios relic and consumable, the old sprite models the renderer
 * draws as the inventory icon, and the forgetting of everything it parsed when resources reload. One call from {@code WornLibClient.init}. (Relic armour already takes the
 * armour item renderer, which hands its relics to {@link HandItemRenderer}.)
 */
public final class HandClient {
    private HandClient() {}

    /** The items this renderer owns: the relics worn in Curios slots and the consumable relics. */
    private static boolean candidate(Item item) {
        return (item instanceof RelicGearItem || item instanceof RelicConsumableItem) && "nusmp".equals(BuiltInRegistries.ITEM.getKey(item).getNamespace());
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterClientReloadListenersEvent e) -> e.registerReloadListener((ResourceManagerReloadListener) mgr -> HandModels.clear()));
        modBus.addListener(HandClient::registerExtensions);
        modBus.addListener(HandClient::registerModels);
    }

    static void registerExtensions(RegisterClientExtensionsEvent e) {
        IClientItemExtensions ext = new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() { return HandItemRenderer.instance(); }
        };
        List<Item> items = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) if (candidate(item) && !e.isItemRegistered(item)) items.add(item);
        if (!items.isEmpty()) e.registerItem(ext, items.toArray(new Item[0]));
    }

    /** The old sprite models of the converted items, so the renderer can draw them as the icon (an item whose json was not converted has no icon file and is left alone). */
    static void registerModels(ModelEvent.RegisterAdditional e) {
        var rm = Minecraft.getInstance().getResourceManager();
        for (Item item : BuiltInRegistries.ITEM) {
            if (!candidate(item)) continue;
            String path = BuiltInRegistries.ITEM.getKey(item).getPath();
            if (rm.getResource(HandModels.rl("models/item/" + path + "_icon.json")).isPresent()) e.register(HandItemRenderer.icon(path));
        }
    }
}
