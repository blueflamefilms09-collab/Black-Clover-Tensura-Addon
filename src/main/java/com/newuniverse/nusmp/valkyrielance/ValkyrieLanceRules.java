package com.newuniverse.nusmp.valkyrielance;

import java.util.Map;
import java.util.UUID;

/**
 * Why: the numbers and decisions of the Valkyrie Drill Lance (the core weapon of Noelle's Valkyrie Dress, a creative-display item) in one pure class (no game class), so a test can pin every cap. The lance is an
 * honest spear-class weapon: a trident's melee numbers (9 damage with the wielder's own 1, 1.1 swings a second, iron's 250 uses), nothing else; no key, no packet, no throw, no special magic. A hit throws a
 * small burst of water drops at the target, drawn from the server with ordinary particle packets to the players near the spot, capped here: particles per burst, one burst per attacker every few ticks and a few
 * bursts per server tick over all attackers. Nothing here allocates per tick beyond the map entry of a new attacker.
 */
public final class ValkyrieLanceRules {
    private ValkyrieLanceRules() {}

    public static final String ID = "valkyrie_drill_lance";

    /** Attack damage modifier on top of iron's (6 + 2 = 8, shown 9 with the player's own 1: a trident's) and the attack speed modifier (-2.9 = 1.1 swings a second, a trident's). */
    public static final float BASE_DAMAGE = 6.0f, SPEED = -2.9f;
    /** Drops in one hit burst, bursts per server tick (all attackers together), and the least ticks between two bursts of one attacker. */
    public static final int HIT_PARTICLES = 8, HIT_BURSTS_PER_TICK = 4, HIT_GAP_TICKS = 4;
    /** The most attackers remembered (a map keyed by UUID, trimmed when it grows; also cleared when a player leaves). */
    public static final int MAX_REMEMBERED = 256;

    /** The worst number of hit particles the server can send in one tick: bursts per tick times particles per burst. */
    public static int worstHitParticlesPerTick() { return HIT_BURSTS_PER_TICK * HIT_PARTICLES; }

    /**
     * May this attacker throw a hit burst at game tick {@code now}? At least {@link #HIT_GAP_TICKS} after its last burst. Records the burst when yes. The map is bounded: past {@link #MAX_REMEMBERED} entries the
     * entries older than the gap are dropped (a full-map scan at most once per overflow, never per hit).
     */
    public static boolean hitBurstAllowed(Map<UUID, Long> last, UUID attacker, long now) {
        Long prev = last.get(attacker);
        if (prev != null && now - prev < HIT_GAP_TICKS && now >= prev) return false;
        if (last.size() >= MAX_REMEMBERED) last.values().removeIf(t -> now - t >= HIT_GAP_TICKS || t > now);
        if (last.size() >= MAX_REMEMBERED) return false;
        last.put(attacker, now);
        return true;
    }
}
