package com.newuniverse.nusmp.skilltree.loot;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Why: the entry point of the unique-lines additions to the skill-book loot (1.82.0), in one class so the existing {@code ItemsSystem.init} needs ONE new line. It registers the loot
 * modifier type {@code nusmp:rift_books} on the mod bus (the shipped JSON and the entry in the global list switch it on), and on the game bus it keeps the two bits of state of this part
 * from outliving what they belong to: the pity memory of a player is dropped when that player logs out (one boolean per ONLINE player, never a leak), and both it and the cached
 * weight table are dropped when the server stops (the next world, or a connection to another server, starts from nothing). It also registers the self-test {@code tree.lines.loot}.
 * Safe to call twice.
 */
public final class LinesLoot {
    private LinesLoot() {}

    private static boolean done;

    public static synchronized void init(IEventBus modBus) {
        if (done) return;
        done = true;
        RiftBookLootModifier.register(modBus);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> BookPity.GLOBAL.forget(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
            BookPity.GLOBAL.clear();
            RouteTable.clear();
        });
        LinesLootSelfTests.register();
    }
}
