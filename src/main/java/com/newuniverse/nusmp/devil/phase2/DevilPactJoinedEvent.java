package com.newuniverse.nusmp.devil.phase2;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/**
 * Why: the HOOK for the owner's unfinished third sabotage rule ("if three players make a contract within the same Minecraft day ..."; the message ended there, docs/devil_contract_phase2.md). Nothing in the mod
 * acts on it. With the server setting {@code devilPhase2.sabotage.sameDayTripleHook = true} every new contract posts this event once (on the server, after the contract is in the ledger) with the devil, the
 * number of contracts it now has and the Minecraft day (game time / 24000), so a later rule, a datapack mod or another addon can count three in one day without scanning anything. Not cancellable.
 */
public class DevilPactJoinedEvent extends Event {
    private final ServerPlayer player;
    private final String devil;
    private final int contracts;
    private final long day;

    public DevilPactJoinedEvent(ServerPlayer player, String devil, int contracts, long day) {
        this.player = player;
        this.devil = devil;
        this.contracts = contracts;
        this.day = day;
    }

    public ServerPlayer getPlayer() { return player; }

    public String getDevil() { return devil; }

    /** How many contracts the devil has now, this one included. */
    public int getContracts() { return contracts; }

    /** The Minecraft day the contract was signed in (game time divided by 24000). */
    public long getDay() { return day; }
}
