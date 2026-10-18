package com.newuniverse.nusmp.elite.items.api;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/**
 * Why: a crate opening is something the records system counts ("open 10 crates"), so it is announced on {@code NeoForge.EVENT_BUS} after the rewards were given. Server side, not cancellable.
 * {@code tier} is {@code common}, {@code rare} or {@code elite}; {@code rewards} how many things were given.
 */
public class CrateOpenedEvent extends Event {
    private final ServerPlayer player;
    private final String tier;
    private final int rewards;

    public CrateOpenedEvent(ServerPlayer player, String tier, int rewards) {
        this.player = player;
        this.tier = tier;
        this.rewards = rewards;
    }

    public ServerPlayer getPlayer() { return player; }

    public String getTier() { return tier; }

    public int getRewards() { return rewards; }
}
