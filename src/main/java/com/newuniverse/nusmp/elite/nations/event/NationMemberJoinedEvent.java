package com.newuniverse.nusmp.elite.nations.event;

import net.neoforged.bus.api.Event;

/**
 * Why: A player became a member of a nation (founder excluded: see NationCreatedEvent). Fired on the NeoForge GAME bus ({@code NeoForge.EVENT_BUS}) on the server thread, after the change is made; not cancellable. Carries plain values (ids and UUIDs) only, so a listener in
 * another subsystem needs no Minecraft types. Part of the public API of the Nations system (see {@code NationApi}).
 */
public class NationMemberJoinedEvent extends Event {
    private final String nationId;
    private final java.util.UUID player;

    public NationMemberJoinedEvent(String nationId, java.util.UUID player) {
        this.nationId = nationId;
        this.player = player;
    }

    public String nationId() { return nationId; }
    public java.util.UUID player() { return player; }
}
