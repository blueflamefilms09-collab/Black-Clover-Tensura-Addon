package com.newuniverse.nusmp.arc.tier;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the server must know, in one bounded place, which Death Note scenes are running (so a crowd of Lights can never run more than {@link #MAX_ACTIVE} at once, one player is never the
 * target of two, one Light never runs two) and must be able to cancel a player's scene from an event (logout, death, a change of dimension) without finding the boss. State is keyed by player
 * UUID (never a "current player"), cleaned up on logout, death, dimension change, the boss leaving the world and the server stopping. Pure: no Minecraft class, server thread only.
 *
 * <p>Worst case: {@link #MAX_ACTIVE} scenes and {@link #MAX_COOLDOWNS} cooldown entries, both fixed arrays of small objects; nothing grows with the player count.
 */
public final class LightScenes {
    private LightScenes() {}

    /** At most this many scenes run on the whole server at the same time (more Lights than this wait their turn). */
    public static final int MAX_ACTIVE = 4;
    /** How many players may have a cooldown remembered at once (the oldest is forgotten first). */
    public static final int MAX_COOLDOWNS = 256;
    /** Ticks before the same player can be written at again (a minute), and before the same Light starts another scene. */
    public static final int TARGET_COOLDOWN = 1200, LIGHT_COOLDOWN = 600;

    private static final Map<UUID, LightScene> BY_TARGET = new HashMap<>();
    private static final Map<UUID, LightScene> BY_LIGHT = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWN = new LinkedHashMap<>(64, 0.75f, false) {
        @Override protected boolean removeEldestEntry(Map.Entry<UUID, Long> eldest) { return size() > MAX_COOLDOWNS; }
    };

    /** Why a scene did or did not start. */
    public enum Start { STARTED, FULL, TARGET_BUSY, LIGHT_BUSY, TARGET_COOLING }

    /** Starts a scene of {@code light} at {@code target} if the caps allow; {@code out[0]} holds the scene when it started. */
    public static Start start(UUID light, UUID target, long now, LightScene[] out) {
        if (BY_LIGHT.containsKey(light)) return Start.LIGHT_BUSY;
        if (BY_TARGET.containsKey(target)) return Start.TARGET_BUSY;
        Long until = COOLDOWN.get(target);
        if (until != null && now < until) return Start.TARGET_COOLING;
        if (BY_LIGHT.size() >= MAX_ACTIVE) return Start.FULL;
        LightScene s = new LightScene(light, target);
        BY_LIGHT.put(light, s);
        BY_TARGET.put(target, s);
        if (out != null && out.length > 0) out[0] = s;
        return Start.STARTED;
    }

    /** Removes a finished or cancelled scene and starts the target's cooldown. */
    public static void end(LightScene s, long now) {
        if (s == null) return;
        BY_LIGHT.remove(s.light(), s);
        BY_TARGET.remove(s.target(), s);
        COOLDOWN.remove(s.target());
        COOLDOWN.put(s.target(), now + TARGET_COOLDOWN);
    }

    /** The scene running at this player, or null. */
    public static LightScene ofTarget(UUID target) { return BY_TARGET.get(target); }

    /** The scene this Light runs, or null. */
    public static LightScene ofLight(UUID light) { return BY_LIGHT.get(light); }

    public static int active() { return BY_LIGHT.size(); }

    public static int cooldowns() { return COOLDOWN.size(); }

    /** A player logged out, died or changed dimension: their scene is cancelled (the boss notices on its next tick and tells the viewers) and they are forgotten. */
    public static void playerGone(UUID player) {
        LightScene s = BY_TARGET.remove(player);
        if (s != null) {
            s.interrupt(LightScene.Cancel.TARGET_GONE);
            BY_LIGHT.remove(s.light(), s);
        }
        COOLDOWN.remove(player);
    }

    /** A Light left the world (died, unloaded, removed): his scene is cancelled and dropped. Returns the scene so the caller can tell the viewers. */
    public static LightScene lightGone(UUID light, LightScene.Cancel why) {
        LightScene s = BY_LIGHT.remove(light);
        if (s != null) {
            s.interrupt(why);
            BY_TARGET.remove(s.target(), s);
        }
        return s;
    }

    /** The server stopped (or another world was opened in the same game): nothing carries over. */
    public static void reset() {
        BY_LIGHT.clear();
        BY_TARGET.clear();
        COOLDOWN.clear();
    }

    /** Is this scene still the one in the registry (a boss holding an old reference after {@link #playerGone} still sees its own scene as cancelled, but the registry no longer lists it)? */
    public static boolean listed(LightScene s) { return s != null && BY_LIGHT.get(s.light()) == s; }
}
