package com.newuniverse.nusmp.elite.nations.event;

import net.neoforged.bus.api.Event;

/**
 * Why: A nation reached a level higher than any it has announced before. Fired on the NeoForge GAME bus ({@code NeoForge.EVENT_BUS}) on the server thread, after the change is made; not cancellable. Carries plain values (ids and UUIDs) only, so a listener in
 * another subsystem needs no Minecraft types. Part of the public API of the Nations system (see {@code NationApi}).
 */
public class NationLevelUpEvent extends Event {
    private final String nationId;
    private final int oldLevel;
    private final int newLevel;

    public NationLevelUpEvent(String nationId, int oldLevel, int newLevel) {
        this.nationId = nationId;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    public String nationId() { return nationId; }
    public int oldLevel() { return oldLevel; }
    public int newLevel() { return newLevel; }
}
