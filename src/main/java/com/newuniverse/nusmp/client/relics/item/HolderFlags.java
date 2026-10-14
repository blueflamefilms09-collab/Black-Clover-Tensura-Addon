package com.newuniverse.nusmp.client.relics.item;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What the client knows about other players' render flags, told by the server (relics/RelicHolderSync): the Genesis body flag and the leaves of the
 * player's grimoire. Entity persistent data never reaches a client, so the item layers read it from here. Keyed by entity id; no Minecraft types.
 */
public final class HolderFlags {
    private HolderFlags() {}

    private static final Map<Integer, Integer> FLAGS = new ConcurrentHashMap<>();

    /** Bit 0: Genesis body. Bits 8 to 15: grimoire leaves (0 = none). */
    public static int pack(boolean genesis, int leaves) { return (genesis ? 1 : 0) | (Math.max(0, Math.min(15, leaves)) << 8); }

    public static boolean genesis(int packed) { return (packed & 1) != 0; }

    public static int leaves(int packed) { return packed >> 8 & 15; }

    public static void put(int entityId, int packed) {
        if (packed == 0) {
            FLAGS.remove(entityId);
            return;
        }
        if (FLAGS.size() > 1024) FLAGS.clear();
        FLAGS.put(entityId, packed);
    }

    public static int get(int entityId) { return FLAGS.getOrDefault(entityId, 0); }

    public static void clear() { FLAGS.clear(); }
}
