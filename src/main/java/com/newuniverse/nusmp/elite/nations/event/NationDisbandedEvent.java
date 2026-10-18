package com.newuniverse.nusmp.elite.nations.event;

import net.neoforged.bus.api.Event;

/**
 * Why: A nation was disbanded (its data is already gone; the id and name are what it was). Fired on the NeoForge GAME bus ({@code NeoForge.EVENT_BUS}) on the server thread, after the change is made; not cancellable. Carries plain values (ids and UUIDs) only, so a listener in
 * another subsystem needs no Minecraft types. Part of the public API of the Nations system (see {@code NationApi}).
 */
public class NationDisbandedEvent extends Event {
    private final String nationId;
    private final String name;

    public NationDisbandedEvent(String nationId, String name) {
        this.nationId = nationId;
        this.name = name;
    }

    public String nationId() { return nationId; }
    public String name() { return name; }
}
