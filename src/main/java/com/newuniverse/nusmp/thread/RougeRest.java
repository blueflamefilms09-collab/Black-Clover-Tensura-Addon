package com.newuniverse.nusmp.thread;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * Why: the head-cat layer draws on the PLAYER, so it needs a cheap answer to "is this player's Rouge resting on their head right now?"
 * without searching the world every frame. Each client-side Rouge writes herself here every tick while she rests (and removes herself when
 * she moves off or is removed), and the layer does one map lookup. It also holds the client hook that makes her pickable only while the local
 * player holds a name tag, so she can be named but never gets in the way of aiming or arrows. Client thread only; the server never writes it.
 */
public final class RougeRest {
    private RougeRest() {}

    private static final Map<UUID, RougeEntity> RESTING = new HashMap<>();

    /** Set by the client setup: true while the local player holds a name tag. The default (false) is what the server sees. */
    public static volatile BooleanSupplier namingHand = () -> false;

    static void put(UUID owner, RougeEntity cat) { RESTING.put(owner, cat); }

    static void clear(UUID owner, RougeEntity cat) { if (owner != null) RESTING.remove(owner, cat); }

    /** The cat resting on this player's head, drawn by the layer, or null. */
    public static RougeEntity on(UUID owner) {
        RougeEntity r = RESTING.get(owner);
        if (r == null) return null;
        if (r.isRemoved()) { RESTING.remove(owner); return null; }
        return r;
    }

    public static void clearAll() { RESTING.clear(); }
}
