package com.newuniverse.nusmp.elite.nations.event;

import net.neoforged.bus.api.Event;

/**
 * Why: A bounty was claimed by a kill (victimKey is the player UUID or the entity type id). Fired on the NeoForge GAME bus ({@code NeoForge.EVENT_BUS}) on the server thread, after the change is made; not cancellable. Carries plain values (ids and UUIDs) only, so a listener in
 * another subsystem needs no Minecraft types. Part of the public API of the Nations system (see {@code NationApi}).
 */
public class BountyClaimedEvent extends Event {
    private final java.util.UUID killer;
    private final String victimKey;
    private final long coins;
    private final String posterNation;

    public BountyClaimedEvent(java.util.UUID killer, String victimKey, long coins, String posterNation) {
        this.killer = killer;
        this.victimKey = victimKey;
        this.coins = coins;
        this.posterNation = posterNation;
    }

    public java.util.UUID killer() { return killer; }
    public String victimKey() { return victimKey; }
    public long coins() { return coins; }
    public String posterNation() { return posterNation; }
}
