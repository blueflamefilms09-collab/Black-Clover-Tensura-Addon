package com.newuniverse.nusmp.elite.nations.event;

import net.neoforged.bus.api.Event;

/**
 * Why: Two nations became allies. Fired on the NeoForge GAME bus ({@code NeoForge.EVENT_BUS}) on the server thread, after the change is made; not cancellable. Carries plain values (ids and UUIDs) only, so a listener in
 * another subsystem needs no Minecraft types. Part of the public API of the Nations system (see {@code NationApi}).
 */
public class AllianceFormedEvent extends Event {
    private final String nationA;
    private final String nationB;

    public AllianceFormedEvent(String nationA, String nationB) {
        this.nationA = nationA;
        this.nationB = nationB;
    }

    public String nationA() { return nationA; }
    public String nationB() { return nationB; }
}
