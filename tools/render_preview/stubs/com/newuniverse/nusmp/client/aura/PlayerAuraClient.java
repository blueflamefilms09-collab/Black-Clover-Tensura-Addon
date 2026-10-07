package com.newuniverse.nusmp.client.aura;

import com.newuniverse.nusmp.aura.Aura;
import net.minecraft.client.Minecraft;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Preview stub of PlayerAuraClient: the painters registered for each Aura and which player has which aura until when (the real
 * PlayerAuraLayer, compiled for real, reads running() and painter() from here). {@link #has} keeps its game semantics.
 */
public final class PlayerAuraClient {
    private PlayerAuraClient() {}

    /** entity id -> aura -> {start game time, end game time, style}. */
    private static final Map<Integer, EnumMap<Aura, long[]>> ACTIVE = new HashMap<>();
    private static final EnumMap<Aura, AuraPainter> PAINTERS = new EnumMap<>(Aura.class);

    public static void register(Aura a, AuraPainter p) { PAINTERS.put(a, p); }
    static AuraPainter painter(Aura a) { return PAINTERS.get(a); }
    static EnumMap<Aura, long[]> running(int entityId) { return ACTIVE.get(entityId); }

    /** True while the player has the aura (for particles and other client logic). */
    public static boolean has(int entityId, Aura a) {
        var m = ACTIVE.get(entityId);
        Minecraft mc = Minecraft.getInstance();
        long[] w = m == null ? null : m.get(a);
        return w != null && mc.level != null && mc.level.getGameTime() < w[1];
    }

    /** Preview helpers (not in the game). */
    public static void previewClear() { ACTIVE.clear(); PAINTERS.clear(); }
    public static boolean previewHasPainter(Aura a) { return PAINTERS.containsKey(a); }
    public static void previewSet(int entityId, Aura a, long start, long end, int style) {
        ACTIVE.computeIfAbsent(entityId, k -> new EnumMap<>(Aura.class)).put(a, new long[]{start, end, style});
    }
}
