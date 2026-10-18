package com.newuniverse.nusmp.elite.nations.event;

import net.neoforged.bus.api.Event;

/**
 * Why: A nation declared war; the war starts in the DECLARED phase. Fired on the NeoForge GAME bus ({@code NeoForge.EVENT_BUS}) on the server thread, after the change is made; not cancellable. Carries plain values (ids and UUIDs) only, so a listener in
 * another subsystem needs no Minecraft types. Part of the public API of the Nations system (see {@code NationApi}).
 */
public class WarDeclaredEvent extends Event {
    private final long warId;
    private final String attacker;
    private final String defender;
    private final String reason;

    public WarDeclaredEvent(long warId, String attacker, String defender, String reason) {
        this.warId = warId;
        this.attacker = attacker;
        this.defender = defender;
        this.reason = reason;
    }

    public long warId() { return warId; }
    public String attacker() { return attacker; }
    public String defender() { return defender; }
    public String reason() { return reason; }
}
