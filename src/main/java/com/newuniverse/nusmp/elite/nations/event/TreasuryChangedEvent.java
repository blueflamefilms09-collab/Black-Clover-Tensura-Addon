package com.newuniverse.nusmp.elite.nations.event;

import net.neoforged.bus.api.Event;

/**
 * Why: A nation's treasury changed by 'delta' coins (positive = in); balance is after the change. Fired on the NeoForge GAME bus ({@code NeoForge.EVENT_BUS}) on the server thread, after the change is made; not cancellable. Carries plain values (ids and UUIDs) only, so a listener in
 * another subsystem needs no Minecraft types. Part of the public API of the Nations system (see {@code NationApi}).
 */
public class TreasuryChangedEvent extends Event {
    private final String nationId;
    private final long delta;
    private final long balance;
    private final String reason;

    public TreasuryChangedEvent(String nationId, long delta, long balance, String reason) {
        this.nationId = nationId;
        this.delta = delta;
        this.balance = balance;
        this.reason = reason;
    }

    public String nationId() { return nationId; }
    public long delta() { return delta; }
    public long balance() { return balance; }
    public String reason() { return reason; }
}
