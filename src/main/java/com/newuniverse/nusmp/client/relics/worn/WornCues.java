package com.newuniverse.nusmp.client.relics.worn;

import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;

/**
 * What this client knows about the beats of the boss relics: for each (player entity, relic) the kind of the last cue and the game time it arrived.
 * The renderers read it every frame; entries older than 10 seconds are dropped. Client thread only.
 */
public final class WornCues {
    private WornCues() {}

    /** A beat: its kind (see {@code WaveBossNet}) and the game time it began. */
    public record Cue(int kind, long start) {}

    private static final Map<Long, Cue> CUES = new HashMap<>();
    private static final long KEEP = 200L;

    private static long key(int entity, int which) { return ((long) entity << 4) | (which & 15); }

    /** From the packet: a relic of this player fired. */
    public static void receive(int entity, int which, int kind) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        if (CUES.size() > 64) CUES.values().removeIf(c -> now - c.start() > KEEP);
        CUES.put(key(entity, which), new Cue(kind, now));
    }

    /** The last cue of this relic on this player, or null if there is none or it is old. */
    public static Cue get(int entity, int which, long now) {
        Cue c = CUES.get(key(entity, which));
        if (c == null) return null;
        if (now - c.start() > KEEP || now < c.start()) {
            CUES.remove(key(entity, which));
            return null;
        }
        return c;
    }

    public static void clear() { CUES.clear(); }
}
