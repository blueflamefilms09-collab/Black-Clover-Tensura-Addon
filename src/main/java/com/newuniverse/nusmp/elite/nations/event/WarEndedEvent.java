package com.newuniverse.nusmp.elite.nations.event;

import net.neoforged.bus.api.Event;

/**
 * Why: The fighting of a war is over (victory, draw, surrender, ceasefire, admin end). Spoils are already applied; 'winner' is empty for a draw or ceasefire; outcome is the War.Outcome name in lower case. Fired on the NeoForge GAME bus ({@code NeoForge.EVENT_BUS}) on the server thread, after the change is made; not cancellable. Carries plain values (ids and UUIDs) only, so a listener in
 * another subsystem needs no Minecraft types. Part of the public API of the Nations system (see {@code NationApi}).
 */
public class WarEndedEvent extends Event {
    private final long warId;
    private final String attacker;
    private final String defender;
    private final String winner;
    private final String outcome;
    private final long spoilsCoins;

    public WarEndedEvent(long warId, String attacker, String defender, String winner, String outcome, long spoilsCoins) {
        this.warId = warId;
        this.attacker = attacker;
        this.defender = defender;
        this.winner = winner;
        this.outcome = outcome;
        this.spoilsCoins = spoilsCoins;
    }

    public long warId() { return warId; }
    public String attacker() { return attacker; }
    public String defender() { return defender; }
    public String winner() { return winner; }
    public String outcome() { return outcome; }
    public long spoilsCoins() { return spoilsCoins; }
}
