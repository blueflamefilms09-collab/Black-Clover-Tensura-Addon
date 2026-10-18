package com.newuniverse.nusmp.elite.nations.event;

import net.neoforged.bus.api.Event;

/**
 * Why: A member completed a contract and was paid (coins are after tax). Fired on the NeoForge GAME bus ({@code NeoForge.EVENT_BUS}) on the server thread, after the change is made; not cancellable. Carries plain values (ids and UUIDs) only, so a listener in
 * another subsystem needs no Minecraft types. Part of the public API of the Nations system (see {@code NationApi}).
 */
public class ContractCompletedEvent extends Event {
    private final java.util.UUID player;
    private final String nationId;
    private final long contractUid;
    private final String templateId;
    private final int tier;
    private final long coins;
    private final long nationXp;
    private final int reputation;

    public ContractCompletedEvent(java.util.UUID player, String nationId, long contractUid, String templateId, int tier, long coins, long nationXp, int reputation) {
        this.player = player;
        this.nationId = nationId;
        this.contractUid = contractUid;
        this.templateId = templateId;
        this.tier = tier;
        this.coins = coins;
        this.nationXp = nationXp;
        this.reputation = reputation;
    }

    public java.util.UUID player() { return player; }
    public String nationId() { return nationId; }
    public long contractUid() { return contractUid; }
    public String templateId() { return templateId; }
    public int tier() { return tier; }
    public long coins() { return coins; }
    public long nationXp() { return nationXp; }
    public int reputation() { return reputation; }
}
