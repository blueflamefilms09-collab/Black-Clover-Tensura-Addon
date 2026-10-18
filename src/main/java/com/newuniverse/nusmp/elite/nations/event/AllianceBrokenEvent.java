package com.newuniverse.nusmp.elite.nations.event;

import net.neoforged.bus.api.Event;

/**
 * Why: An alliance was broken by 'breaker' (or ended because a nation was disbanded: breaker empty). Fired on the NeoForge GAME bus ({@code NeoForge.EVENT_BUS}) on the server thread, after the change is made; not cancellable. Carries plain values (ids and UUIDs) only, so a listener in
 * another subsystem needs no Minecraft types. Part of the public API of the Nations system (see {@code NationApi}).
 */
public class AllianceBrokenEvent extends Event {
    private final String nationA;
    private final String nationB;
    private final String breaker;

    public AllianceBrokenEvent(String nationA, String nationB, String breaker) {
        this.nationA = nationA;
        this.nationB = nationB;
        this.breaker = breaker;
    }

    public String nationA() { return nationA; }
    public String nationB() { return nationB; }
    public String breaker() { return breaker; }
}
