package com.newuniverse.nusmp.celestial;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: one call wires Celestial Spirit Magic into the mod constructor (like the other systems): the items (before {@code NUItems.ITEMS} is registered on the bus), the
 * player attachment, the Spirit King's entity type and attributes, the key loot modifier, the sync packet, the game-bus listeners and the in-game self-tests. The prop
 * behaviours are registered by {@code prop.PropRegistry} (one line) and the renderers by {@code client.celestial.CelestialClient}.
 */
public final class CelestialSystem {
    private CelestialSystem() {}

    /** Call once from the mod constructor, before {@code NUItems.ITEMS.register(modEventBus)}. */
    public static void init(IEventBus modBus) {
        CelestialItems.init();
        CelestialAttachments.ATTACHMENTS.register(modBus);
        CelestialEntities.ENTITIES.register(modBus);
        modBus.addListener(CelestialEntities::attributes);
        CelestialLootModifier.register(modBus);
        modBus.addListener(CelestialNet::registerPayloads);
        CelestialEvents.register(NeoForge.EVENT_BUS);
        CelestialSelfTests.register();
    }
}
