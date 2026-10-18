package com.newuniverse.nusmp.heroweapon.weapon.glassworld;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Why: the runtime state of a hero weapon (the residue of the Fan, the souls of the Scythe, the tempo of the Katana, the read pages of the Book) belongs to a
 * wielder and lives only as long as the server does: it is saved nowhere, it must not grow without bound, and it must be forgotten when the wielder leaves or the
 * server stops (the owner's server rule: no static mutable state that outlives a server). This is that map, keyed by the player's UUID, created on first use,
 * bounded (the oldest entry goes when more than {@code cap} wielders have used the weapon), and cleared in one call. No game imports, so it is tested on its own.
 */
public final class PerPlayer<S> {
    private final Map<UUID, S> states = new LinkedHashMap<>();
    private final Supplier<S> fresh;
    private final int cap;

    /**
     * @param fresh makes the state of a wielder who has none yet
     * @param cap   the most wielders held at once (a server holds a few dozen at most; the cap only guards against a leak)
     */
    public PerPlayer(Supplier<S> fresh, int cap) {
        this.fresh = fresh;
        this.cap = Math.max(1, cap);
    }

    /** The wielder's state, made on first use. */
    public S of(UUID id) {
        S s = states.get(id);
        if (s == null) {
            if (states.size() >= cap) {
                UUID oldest = states.keySet().iterator().next();
                states.remove(oldest);
            }
            s = fresh.get();
            states.put(id, s);
        }
        return s;
    }

    /** The wielder's state if they have one, else null (does not make one). */
    public S peek(UUID id) { return states.get(id); }

    public void forget(UUID id) { states.remove(id); }

    public void clear() { states.clear(); }

    public int size() { return states.size(); }
}
