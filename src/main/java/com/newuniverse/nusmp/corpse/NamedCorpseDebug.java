package com.newuniverse.nusmp.corpse;

import net.minecraft.server.level.ServerPlayer;

/**
 * CI only (3.10.1): lets the client screenshot run ({@code client/debug/ClientShot}, started with {@code -Dnusmp.clientshot}) cast a numbered undead on the integrated server so the real
 * client can be asked whether the body arrived and is drawn. Server thread only; it does exactly what the page cast does ({@link Corpses#summonNamedChecked}) and nothing else.
 */
public final class NamedCorpseDebug {
    private NamedCorpseDebug() {}

    /** Summons the numbered undead with this registry id for the player; returns a one-line result (the body's entity id, or the refusal reason). */
    public static String summon(ServerPlayer p, String id) {
        for (NamedCorpses k : NamedCorpses.values()) {
            if (!k.id.equals(id)) continue;
            Corpses.NamedSummon s = Corpses.summonNamedChecked(p, k);
            return s.body() == null ? "REFUSED: " + s.why() : "entity " + s.body().getId() + " at " + Math.round(s.body().getX()) + " " + Math.round(s.body().getY()) + " " + Math.round(s.body().getZ());
        }
        return "unknown id " + id;
    }

    /** The ids of the numbered undead, in order. */
    public static String[] ids() {
        NamedCorpses[] all = NamedCorpses.values();
        String[] out = new String[all.length];
        for (int i = 0; i < all.length; i++) out[i] = all[i].id;
        return out;
    }
}
