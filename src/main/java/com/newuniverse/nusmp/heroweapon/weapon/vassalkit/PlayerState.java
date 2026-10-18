package com.newuniverse.nusmp.heroweapon.weapon.vassalkit;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Why: a weapon logic is ONE object for the whole game, but its techniques need a little memory per wielder (the panes of glass a Mirror stands, the beat of a
 * song, the needles planted). That memory is plain Java state that must not outlive its wielder or its server: it is dropped when the person logs out and
 * the whole map is cleared when the server stops, so a single-player world opened twice in one session never sees the first world's panes. Nothing in it is
 * saved; it holds timestamps of the game clock and the ids of other entities, never the entities themselves. Server thread only.
 */
public final class PlayerState<T> {
    private final Map<UUID, T> map = new HashMap<>();
    private final Supplier<T> factory;

    public PlayerState(Supplier<T> factory) {
        this.factory = factory;
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> map.remove(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> map.clear());
    }

    /** The memory of this person, made empty on first use. */
    public T of(Player p) {
        return map.computeIfAbsent(p.getUUID(), k -> factory.get());
    }

    /** The memory of this person if they have any, else null (does not create one). */
    public T peek(Player p) {
        return map.get(p.getUUID());
    }

    /** Forgets this person's memory now. */
    public void drop(Player p) {
        map.remove(p.getUUID());
    }

    /** How many people have a memory (for the self-test). */
    public int size() {
        return map.size();
    }
}
