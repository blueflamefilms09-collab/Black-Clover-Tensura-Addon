package com.newuniverse.nusmp.elite.nations.event;

import net.neoforged.bus.api.Event;

/**
 * Why: A player's reputation with a nation (or faction id) changed. Fired on the NeoForge GAME bus ({@code NeoForge.EVENT_BUS}) on the server thread, after the change is made; not cancellable. Carries plain values (ids and UUIDs) only, so a listener in
 * another subsystem needs no Minecraft types. Part of the public API of the Nations system (see {@code NationApi}).
 */
public class ReputationChangedEvent extends Event {
    private final java.util.UUID player;
    private final String nationId;
    private final int oldValue;
    private final int newValue;
    private final String reason;

    public ReputationChangedEvent(java.util.UUID player, String nationId, int oldValue, int newValue, String reason) {
        this.player = player;
        this.nationId = nationId;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.reason = reason;
    }

    public java.util.UUID player() { return player; }
    public String nationId() { return nationId; }
    public int oldValue() { return oldValue; }
    public int newValue() { return newValue; }
    public String reason() { return reason; }
}
