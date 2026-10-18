package com.newuniverse.nusmp.elite.nations.event;

import net.neoforged.bus.api.Event;

/**
 * Why: A player left a nation (left, kicked, or the nation was disbanded; reason is 'left', 'kicked' or 'disbanded'). Fired on the NeoForge GAME bus ({@code NeoForge.EVENT_BUS}) on the server thread, after the change is made; not cancellable. Carries plain values (ids and UUIDs) only, so a listener in
 * another subsystem needs no Minecraft types. Part of the public API of the Nations system (see {@code NationApi}).
 */
public class NationMemberLeftEvent extends Event {
    private final String nationId;
    private final java.util.UUID player;
    private final String reason;

    public NationMemberLeftEvent(String nationId, java.util.UUID player, String reason) {
        this.nationId = nationId;
        this.player = player;
        this.reason = reason;
    }

    public String nationId() { return nationId; }
    public java.util.UUID player() { return player; }
    public String reason() { return reason; }
}
