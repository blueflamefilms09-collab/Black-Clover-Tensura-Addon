package com.newuniverse.nusmp.client.relics.item;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The one registration call of this package, from the client section of the mod constructor (RelicClient.init is the natural place):
 * {@code com.newuniverse.nusmp.client.relics.item.ItemFxClient.init(modBus);}. It registers the bow and crossbow item properties, the holder tracking
 * that lets the renderer know who holds an item, and clears the synced holder flags on logout. WeaponRenderer clears {@link ItemStateVariants}'s
 * cache itself when the models are rebaked.
 */
public final class ItemFxClient {
    private ItemFxClient() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(RangedItemProperties::register);
        NeoForge.EVENT_BUS.addListener(HolderTracker::onPre);
        NeoForge.EVENT_BUS.addListener(HolderTracker::onPost);
        SocketFx.init(NeoForge.EVENT_BUS);                                           // 1.29.0: socket particles and tooltip lines
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            HolderFlags.clear();
            HolderTracker.reset();
            ItemStateVariants.clearCache();
        });
    }
}
