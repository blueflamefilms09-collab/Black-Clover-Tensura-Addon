package com.newuniverse.nusmp.elite.nations.event;

import net.neoforged.bus.api.Event;

/**
 * Why: A nation was founded (post-creation, the founder is already its leader). Fired on the NeoForge GAME bus ({@code NeoForge.EVENT_BUS}) on the server thread, after the change is made; not cancellable. Carries plain values (ids and UUIDs) only, so a listener in
 * another subsystem needs no Minecraft types. Part of the public API of the Nations system (see {@code NationApi}).
 */
public class NationCreatedEvent extends Event {
    private final String nationId;
    private final String name;
    private final java.util.UUID founder;

    public NationCreatedEvent(String nationId, String name, java.util.UUID founder) {
        this.nationId = nationId;
        this.name = name;
        this.founder = founder;
    }

    public String nationId() { return nationId; }
    public String name() { return name; }
    public java.util.UUID founder() { return founder; }
}
