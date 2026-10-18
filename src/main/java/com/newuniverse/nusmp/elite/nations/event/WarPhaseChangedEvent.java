package com.newuniverse.nusmp.elite.nations.event;

import net.neoforged.bus.api.Event;

/**
 * Why: A war moved to another phase (preparation, active, ceasefire, ended). Fired on the NeoForge GAME bus ({@code NeoForge.EVENT_BUS}) on the server thread, after the change is made; not cancellable. Carries plain values (ids and UUIDs) only, so a listener in
 * another subsystem needs no Minecraft types. Part of the public API of the Nations system (see {@code NationApi}).
 */
public class WarPhaseChangedEvent extends Event {
    private final long warId;
    private final String attacker;
    private final String defender;
    private final String phase;

    public WarPhaseChangedEvent(long warId, String attacker, String defender, String phase) {
        this.warId = warId;
        this.attacker = attacker;
        this.defender = defender;
        this.phase = phase;
    }

    public long warId() { return warId; }
    public String attacker() { return attacker; }
    public String defender() { return defender; }
    public String phase() { return phase; }
}
