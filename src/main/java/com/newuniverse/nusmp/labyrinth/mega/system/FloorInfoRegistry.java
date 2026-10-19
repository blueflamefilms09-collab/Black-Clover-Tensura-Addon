package com.newuniverse.nusmp.labyrinth.mega.system;

import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Why: the registration entry of the System's floor report (1.94.0), in the shape every builder of the Mega-Dungeon uses. {@code MegaSystem} calls {@link #init} (the config
 * file, the two payloads, the floor listener that sends the report on every arrival, the in-game self-test {@code system.floor_info}) and {@link #register} (the once-a-second
 * watcher that announces a mechanic waking or being overcome mid-floor, and the clean-up on logout and when the server stops). The command is {@code /dungeon floor}
 * ({@code DungeonCommand}); the client half is {@code client/mega/system/FloorCardClient}, which {@code SystemClient.init} starts.
 */
public final class FloorInfoRegistry {
    private FloorInfoRegistry() {}

    public static void init(IEventBus modBus) {
        FloorInfoConfig.register();
        modBus.addListener(FloorInfoNet::register);
        MegaDungeon.addFloorListener(FloorInfo.LISTENER);
        FloorInfoSelfTests.register();
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener((ServerTickEvent.Post e) -> FloorInfo.tick(e.getServer()));
        gameBus.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> FloorInfo.forget(e.getEntity().getUUID()));
        gameBus.addListener((ServerStoppedEvent e) -> FloorInfo.clear());
    }
}
