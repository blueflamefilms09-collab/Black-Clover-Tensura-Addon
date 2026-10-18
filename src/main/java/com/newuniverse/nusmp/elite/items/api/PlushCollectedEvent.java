package com.newuniverse.nusmp.elite.items.api;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/**
 * Why: the "soft hook" of the plushie collection counter. When a player holds a plushie they have never held before, the items subsystem posts this event on {@code NeoForge.EVENT_BUS}; the records
 * builder (achievements: "collect 10 plushies", "the whole set") listens for it, with no import of the items subsystem's classes beyond this one small event. Server side. Not cancellable.
 *
 * <p>{@code plushId} is the kind id ({@code ender_dragon}, not the item id), {@code distinct} how many different plushies the player now has, {@code total} how many exist.
 */
public class PlushCollectedEvent extends Event {
    private final ServerPlayer player;
    private final String plushId;
    private final int distinct, total;

    public PlushCollectedEvent(ServerPlayer player, String plushId, int distinct, int total) {
        this.player = player;
        this.plushId = plushId;
        this.distinct = distinct;
        this.total = total;
    }

    public ServerPlayer getPlayer() { return player; }

    public String getPlushId() { return plushId; }

    public int getDistinct() { return distinct; }

    public int getTotal() { return total; }

    /** True when this was the last one: the player has the whole collection. */
    public boolean completesCollection() { return distinct >= total; }
}
